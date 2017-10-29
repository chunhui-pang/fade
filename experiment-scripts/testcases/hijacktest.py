import time
import subprocess
import shlex
import os
import shutil
import logging
import random

from mininet.topo import Topo
from mininet.net import Mininet
from mininet.node import Controller, RemoteController, IVSSwitch, UserSwitch, OVSSwitch
from mininet.nodelib import LinuxBridge
from mininet.cli import CLI

from testcase import TestCase
from utils import PathRetriever

class HijackTopo( Topo ):
    "linear topology with host attaching on edge switches only, using parameter nodes to control the number of switches"
    def __init__( self, straight = 5, hstart=2, hend=4):
        Topo.__init__( self )
        sws = []
        straight = straight if isinstance(straight, int) else int(straight)
        hstart = hstart if isinstance(hstart, int) else int(hstart)
        hend = hend if isinstance(hend, int) else int(hend)
        for i in range(straight):
            sws.append( self.addSwitch('s{}'.format(i+1), protocols='OpenFlow10'))
            if i != 0:
                self.addLink( sws[i-1], sws[i] )
        side_path = []
        for i in range(hend-hstart):
            side_path.append( self.addSwitch('s{}'.format(straight + i + 1)))
            if i != 0:
                self.addLink( side_path[i-1], side_path[i])
        self.addLink(sws[hstart-1], side_path[0])
        self.addLink(side_path[len(side_path)-1], sws[hend-1])
        h1 = self.addHost('h1', ip = '10.0.0.1')
        h2 = self.addHost('h2', ip = '10.0.0.2')
        self.addLink(h1, sws[0])
        self.addLink(h2, sws[straight-1])

        idx = 3
        for sw in self.switches():
            if sw != sws[0] and sw != sws[straight-1]:
                h = self.addHost('h{}'.format(idx))
                self.addLink(h, sw)
                idx+=1
        
class HijackTest( TestCase ):
    "test case for topology HijackTopo, test the performance and ability of detect traffic hijack"

    def __init__( self, straight=5, hstart=2, hend=4, inject=False ):
        TestCase.__init__( self )
        self.straight = straight if isinstance(straight, int) else int(straight)
        self.hstart = hstart if isinstance(hstart, int) else int(hstart)
        self.hend = hend if isinstance(hend, int) else int(hend)
        self.inject = inject if isinstance(inject, bool) else ('true' == inject.lower())
        self.topo = HijackTopo( straight, hstart, hend )
        self.net = Mininet(self.topo, controller=lambda name: RemoteController( name, ip='127.0.0.1', port=6653 ), switch=UserSwitch)
        self.h1 = self.net.get('h1')
        self.h2 = self.net.get('h2')
        self.popens = []
        if str(self.net.switch).find('UserSwitch') != -1:            
            self.path_retriever = PathRetriever(dpidbase=16)
        else:
            self.path_retriever = PathRetriever(dpidbase=10)
        
    def post_start( self ):
        self.net.pingAll()
        # self.net.ping([self.h1, self.h2])

    def inject_malicious( self ):
        # inject malicious rule to drop traffic from h1 to h2
        path = self.path_retriever.get_raw_path( self.h1.MAC(), self.h2.MAC() )
        print path
        if len(path) < 6:
            logging.warning('path shorten than 3, give up malicious injecting')
        else:
            sw = self.net.get('s{}'.format(self.hstart))
            has_ports = [1,2,3]
            for dpid, port in path:
                if dpid == int(sw.defaultDpid(), 16):
                    has_ports.remove(port)
            if len(has_ports) == 0:
                logging.warn('all port used, inject malicious rule fail...')
                return
            outport = has_ports[0]
            cmd = None
            if str(self.net.switch).find('OVSSwitch') != -1:            
                cmd = 'ovs-ofctl add-flow {} ip,nw_dst={},priority=255,actions=output:{}'.format(sw.name, self.h2.IP(), outport )
            elif str(self.net.switch).find('UserSwitch') != -1:
                cmd = 'dpctl unix:/tmp/{} flow-mod cmd=add,table=0,hard=300,prio=255 eth_type=0x800,ip_dst={} apply:output={}'.format(sw.name, self.h2.IP(), outport )
            else:
                raise ValueError('switch unrecognized! cannot inject malicious rules')
            cmds = shlex.split(cmd)
            subprocess.Popen(cmds)
            logging.info('inject rule finished on switch {}, cmd="{}"'.format(sw.name, cmd))

    def test( self ):
        sport = 2000
        dport_start = 3000
        dport_end = 6000
        count = 1000000
        delay = 10000
        dports = '{}-{}'.format(dport_start, dport_end + 1)
        time.sleep(1)
        # destination port is used as the packets count to be sent and the packet identity
        logging.info('send command to h1: {}'.format('./sendpkt -i h1-eth0 -g {} -p {} -w {}us -c {} 10.0.0.2 &'.format(sport, dports, delay, count)))
        self.h1.cmd('./sendpkt -i h1-eth0 -g {} -p {} -w {}us -c {} 10.0.0.2 &'.format(sport, dports, delay, count));
        self.h2.cmd('./sendpkt -i h2-eth0 -g {} -p {} -w {}us -c {} 10.0.0.1 &'.format(sport, dports, delay, count));
        # floodlight cannot find a route from hijacked point to h2 if there is no traffic between those two nodes
        # those codes are used to generate slow traffic
        for h in self.net.hosts:
            if h != self.h1 and h != self.h2:
                h.cmd('./sendpkt -i {}-eth0 -g 2000 -p 3000 -w 1s -c 100000 10.0.0.2 &'.format(h.name))
        logging.info('traffic injected, last for {} seconds...'.format(count*delay/1000/1000) )
        if self.inject:
            wt = random.randint(10,20)
            logging.info('waiting for {} seconds to inject malicious traffic'.format(wt))
            time.sleep(wt)
            self.inject_malicious()
        
    def sleep_time_after_test_finish( self ):
        return 360
    
    def clean( self, exception = False ):
        # kill all related process
        self.h1.cmd('kill %./sendpkt')
        self.h2.cmd('kill %./sendpkt')
        time.sleep(3)
        self.h1.cmd('kill %tcpdump')
        self.h2.cmd('kill %tcpdump')
        for popen in self.popens:
            popen.terminate()

    def post_test( self, exception = False ):
        TestCase.post_test( self, exception )
        pcaps = [f for f in os.listdir('.') if os.path.isfile(f) and f.endswith('.pcap')]
        for pcap in pcaps:
            if exception == True:
                try:
                    os.remove(pcap)
                    logging.info('pcap file have been deleted!')
                except OSError:
                    logging.warning('Cannot delete pcap files')
            else:
                try:
                    shutil.move(pcap, self.get_output_dir())
                    logging.info('pcap files {} have been moved to output directory'.format(pcap))
                except IOError:
                    logging.warning('cannot move pcap file {} to output directory.'.format(pcap))
