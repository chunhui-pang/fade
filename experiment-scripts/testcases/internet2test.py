import time
import subprocess
import shlex
import os
import shutil
import datetime
import logging

from mininet.topo import Topo
from mininet.net import Mininet
from mininet.node import Controller, RemoteController, IVSSwitch, UserSwitch, OVSSwitch
from mininet.nodelib import LinuxBridge
from mininet.cli import CLI

from testcase import TestCase

class Internet2Topo( Topo ):
    "Topology of Internet2. We assume every router is connected with a host, and the IP address of the host should located in a given range (24b prefix)"
    
    def __init__( self ):
        # Initialize topology
        Topo.__init__( self )

        swNames = ['atla', 'chic', 'hous', 'kans', 'losa', 'newy32aoa', 'salt', 'seat', 'wash']
        links = [
            ['atla', 'chic'],
            ['atla', 'hous'],
            ['atla', 'wash'],
            ['chic', 'kans'],
            ['chic', 'newy32aoa'],
            ['chic', 'wash'],
            ['hous', 'kans'],
            ['hous', 'losa'],
            ['kans', 'salt'],
            ['losa', 'salt'],
            ['losa', 'seat'],
            ['newy32aoa', 'wash'],
            ['salt', 'seat']
        ]
        sws = {}
        hosts = {}
        for idx,name in enumerate(swNames, start=1):
            sws[name] = self.addSwitch(name, dpid=str(idx), protocols='OpenFlow10')
            hosts[name] = self.addHost('h1-{}'.format(name[0:4]), ip='10.0.{}.1'.format(idx))
            self.addLink(hosts[name], sws[name])
        for link in links:
            self.addLink(sws[link[0]], sws[link[1]])

class Internet2Test( TestCase ):
    def __init__( self ):
        super(Internet2Test, self).__init__()
        self.topo = Internet2Topo( )
        self.net = Mininet(self.topo, controller=lambda name: RemoteController( name, ip='127.0.0.1', port=6653 ), switch=OVSSwitch)
        self.popens = []
        
    def post_start( self ):
        self.net.pingAll()

    def inject_malicious_rules( self ):
        if str(self.net.switch).find('OVSSwitch') != -1:
            rules = (
                'ovs-ofctl add-flow kans ip,nw_dst=10.0.1.1,priority=255,actions=output:3',
                'ovs-ofctl add-flow losa ip,nw_dst=10.0.8.1,priority=255,actions=output:3'
            )
        elif str(self.net.switch).find('UserSwitch') != -1:
            rules = (
                'dpctl unix:/tmp/kans flow-mod cmd=add,table=0,hard=300,prio=255 eth_type=0x800,ip_dst=10.0.1.1 apply:output=3',
                'dpctl unix:/tmp/losa flow-mod cmd=add,table=0,hard=300,prio=255 eth_type=0x800,ip_dst=10.0.8.1 apply:output=3'
            )
        else:
            raise ValueError('switch unrecognized! cannot inject malicious rules')
        for rule in rules:
            cmds = shlex.split(rule)
            subprocess.Popen(cmds)
        logging.info('malicious rule injected finished, current: {}'.format( datetime.datetime.now().strftime('%H:%M:%S.%f') ))
        kans = None
        losa = None
        for sw in self.net.switches:
            if sw.name == 'kans':
                kans = sw.dpid
            elif sw.name == 'losa':
                losa = sw.dpid
        logging.info('inject malicious rule on kans({}), forwarding 10.0.1.1 erroneously....'.format(kans))
        logging.info('inject malicious rule on losa({}), forwarding 10.0.8.1 erroneously....'.format(losa))
    def test( self ):
        sport = 2000
        dport_start = 3000
        dport_end = 5000
        count = 100000
        delay = 50000
        dports = '{}-{}'.format(dport_start, dport_end)
        # for sw in self.net.switches:
        #     cmds = shlex.split('tcpdump -i {}-eth2 -w tcpdump-{}.pcap net 10.0.0.0/16 and ip'.format(sw.name, sw.name+'eth2'))
        #     popen = subprocess.Popen(cmds)
        #     self.popens.append(popen)
        for h in self.net.hosts:
            h.cmd('tcpdump -w tcpdump-{}.pcap net 10.0.0.0/8 and ip &'.format(h.name))

        for src in self.net.hosts:
            # logging.info('ip of host {} is {}'.format(src, src.IP()))
            dsts = ''
            for dst in self.net.hosts:
                if src != dst:
                    dsts += (' ' + dst.IP())
            src.cmd('./sendpkt -i {}-eth0 -g {} -p {} -w {}us -c {} {} &'.format(src.name, sport, dports, delay, count, dsts));
        logging.info('traffic injected for every pair of hosts, last for {} seconds...'.format(count*delay/1000/1000) )
        
        logging.info('inject malicious flow rules after 20 seconds...')
        time.sleep(20)
        self.inject_malicious_rules( )
        # CLI(self.net)
        
    def sleep_time_after_test_finish( self ):
        return 360
    
    def clean( self, exception = False ):
        super(Internet2Test, self).clean(exception)
        # kill all related process
        for host in self.net.hosts:
            host.cmd('kill %./sendpkt')
        time.sleep(3)
        for h in self.net.hosts:
            h.cmd('kill %tcpdump')
        for popen in self.popens:
            popen.terminate()

    def post_test( self, exception = False):
        super(Internet2Test, self).post_test(exception)
        # copy property files to log directory
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
                    logging.info('pcap file {} have been moved to output directory'.format(pcap))
                except IOError:
                    logging.warning('cannot move pcap file {} to output directory.'.format(pcap))

