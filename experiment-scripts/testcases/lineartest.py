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

class LinearTopo( Topo ):
    "linear topology with host attaching on edge switches only, using parameter nodes to control the number of switches"
    def __init__( self, nodes = 5):
        Topo.__init__( self )
        sws = []
        if not isinstance(nodes, int):
            nodes = int(nodes)
        for i in range(nodes):
            sws.append(self.addSwitch('s{}'.format(i+1), protocols='OpenFlow10'))
            if(i != 0):
                self.addLink(sws[i-1], sws[i])
        h1 = self.addHost('h1', ip = '10.1.0.1/16')  # fake hosts from 10.1.0.2 -> 10.0.1.254.254
        h2 = self.addHost('h2', ip = '10.2.0.1/16')
        self.addLink(h1, sws[0])
        self.addLink(h2, sws[nodes-1])

class LinearTest( TestCase ):
    "test case for topology LinearTopo"
    "Linear topology, links are displayed following:"
    "h1-eth0<->s1-eth2"
    "h2-eth0<->s5-eth2"
    "s1-eth1<->s2-eth1"
    "s2-eth2<->s3-eth1"
    "s3-eth2<->s4-eth1"
    "s4-eth2<->s5-eth1"

    def __init__( self, nodes=5, fake_hosts=7, num_of_injects=0, duration=900, **kwargs ):
        super(LinearTest, self).__init__()
        self.topo = LinearTopo(nodes)
        self.net = Mininet(self.topo, controller=lambda name: RemoteController( name, ip='127.0.0.1', port=6653 ), switch=OVSSwitch)
        self.h1 = self.net.get('h1')
        self.h2 = self.net.get('h2')
        self.host_ips = [ ['10.1.0.1'], ['10.2.0.1'] ]
        self.nodes = int(nodes) if not isinstance(nodes, int) else nodes
        self.fake_hosts = fake_hosts if isinstance(fake_hosts, int) else int(fake_hosts)
        self.num_of_injects = num_of_injects if isinstance(num_of_injects, int) else int(num_of_injects)
        self.duration = duration if isinstance(duration, int) else int(duration)
        self.popens = []
        
    def post_start( self ):
        self.build_and_launch_fake_network()

    def inject_malicious( self ):
        # inject malicious rule to drop traffic from h1 to h2
        if self.nodes < 3:
            if self.num_of_injects != 0:
                logging.error('could not inject malicious rules in topology shorter than 3')
            return
        injected_idx = set()
        injected_idx.add(0)  # don't select the real ip
        injected_idx.add(1)  
        for inject_id in range(self.num_of_injects):
            # choose switch, we don't choose the first and the last switch
            inject_sw = random.randint(1, self.nodes-2) + 1
            sw_name = 's' + str(inject_sw)
            # choose ip
            ip_idx = None
            while not ip_idx or ip_idx in injected_idx:
                ip_idx = random.randint(0, 2*len(self.host_ips[0])-1)
            injected_idx.add(ip_idx)
            inject_ip = self.host_ips[ip_idx%2][ip_idx/2]
            real_outport = 2 if inject_ip.startswith('10.2') else 1
            cmd = None
            if str(self.net.switch).find('OVSSwitch') != -1:            
                cmd = 'ovs-ofctl add-flow {} ip,nw_dst={},priority=255,actions={}'.format(sw_name, inject_ip, 'drop')
            elif str(self.net.switch).find('UserSwitch') != -1:
                cmd = 'dpctl unix:/tmp/{} flow-mod cmd=add,table=0,hard=300,prio=255 eth_type=0x800,ip_dst={} apply:output={}'.format(sw_name, inject_ip, 'drop' )
            else:
                raise ValueError('switch unrecognized! cannot inject malicious rules')
            cmds = shlex.split(cmd)
            subprocess.Popen(cmds)
            logging.info('inject rule finished on switch {}, cmd="{}"'.format(sw_name, cmd))

    def test( self ):
        sport = 2000
        dport_start = 3000
        dport_end = 6000
        count = 100000000
        dports = '{}-{}'.format(dport_start, dport_end + 1)
        # assure every flow rules could receive 20 packet in per-seconds
        delay = 1000000/len(self.host_ips[1])/20
        time.sleep(1)
        # destination port is used as the packets count to be sent and the packet identity
        h1_send_targets = ' '.join(self.host_ips[1])
        h1_cmd = './sendpkt -u {} -g {} -p {} -w {}us -c {} {} &'.format(self.h1.IP(), sport, dports, delay, count, h1_send_targets)
        logging.info('send command to h1: {}'.format(h1_cmd))
        self.h1.cmd(h1_cmd)

        delay = 1000000/len(self.host_ips[0])/20
        h2_send_targets = ' '.join(self.host_ips[0])
        h2_cmd = './sendpkt -u {} -g {} -p {} -w {}us -c {} {} &'.format(self.h2.IP(), sport, dports, delay, count, h2_send_targets)
        logging.info('send command to h2: {}'.format(h2_cmd))
        self.h2.cmd(h2_cmd)
        # self.h2.cmd('nping -udp -g {} -p {} -N -delay {} -c 1 10.0.0.1 &'.format(sport, dports, delay))
        logging.info('traffic injected, last for {} seconds...'.format(count*delay/1000/1000) )
        if self.num_of_injects != 0:
            wt = random.randint(10,20)
            logging.info('waiting for {} seconds to inject malicious traffic'.format(wt))
            time.sleep(wt)
            self.inject_malicious()
        time.sleep(30)
        for i in range(6):
            result = self.net.iperf([self.h1, self.h2])
            logging.info('iperf between h1 and h2: ' + str(result))
            time.sleep(10)

    def sleep_time_after_test_finish( self ):
        time_remain = self.duration - 10*6 # 10*6, iperf
        return time_remain if time_remain > 10 else 10
    
    def clean( self, exception = False ):
        # kill all related process
        self.h1.cmd('kill %./sendpkt')
        self.h2.cmd('kill %./sendpkt')
        for popen in self.popens:
            popen.terminate()
            
    def setup_ebtables_to_hijack_arp (self, host):
        "setup the ebtables to hijack arp messages and send replies automatically"
        # down the default interface and unset its ip addresses
        ip = host.IP()
        name = host.name
        host.cmd('ifconfig {}-eth0 down'.format(name))
        host.cmd('ifconfig {}-eth0 0.0.0.0'.format(name))
        # add bridge
        host.cmd('brctl addbr br0')
        host.cmd('brctl addif br0 {}-eth0'.format(name))
        host.cmd('ifconfig br0 {}'.format(ip))
        # launch the network
        host.cmd('ifconfig {}-eth0 up'.format(name))
        host.cmd('ifconfig br0 up')
    
    def build_and_launch_fake_network( self ):
        "build the fake network and leverage sendpkt to intialize the network (install flow rules)"
        fake_id = 2
        added = 0
        while added != self.fake_hosts:
            if fake_id % 255 != 0:
                self.host_ips[0].append('10.1.{}.{}'.format(fake_id/255, fake_id%255))
                self.host_ips[1].append('10.2.{}.{}'.format(fake_id/255, fake_id%255))
                added += 1
            fake_id += 1
        # disable ICMP message
        self.h1.cmd('iptables -I OUTPUT -p icmp --icmp-type destination-unreachable -j DROP')
        self.h2.cmd('iptables -I OUTPUT -p icmp --icmp-type destination-unreachable -j DROP')
        # install ebtables
        self.setup_ebtables_to_hijack_arp(self.h1)
        self.setup_ebtables_to_hijack_arp(self.h2)
        # set up arp tables and use ebtables as a fallback to send ARP reply automatically
        for fake_ip in self.host_ips[0]:
            self.h2.cmd('arp -s {} {}'.format(fake_ip, self.h1.MAC()))
            if fake_ip != self.h1.IP():
                self.h1.cmd('ebtables -t nat -A PREROUTING -p arp --arp-opcode Request --arp-ip-dst {} -j arpreply --arpreply-mac {} --arpreply-target ACCEPT'.format(fake_ip, self.h1.MAC()))
        for fake_ip in self.host_ips[1]:
            self.h1.cmd('arp -s {} {}'.format(fake_ip, self.h2.MAC()))
            if fake_ip != self.h2.IP():
                self.h2.cmd('ebtables -t nat -A PREROUTING -p arp --arp-opcode Request --arp-ip-dst {} -j arpreply --arpreply-mac {} --arpreply-target ACCEPT'.format(fake_ip, self.h2.MAC()))
        # call sendpkt to trigger packet in
        h1_send_targets = ' '.join(self.host_ips[1])
        h2_send_targets = ' '.join(self.host_ips[0])
        self.h1.cmd('./sendpkt -u {} -w {}ms -c {} {}'.format(3, self.h1.IP(), 5*len(self.host_ips[1]), h1_send_targets));
        self.h2.cmd('./sendpkt -u {} -w {}ms -c {} {}'.format(3, self.h2.IP(), 5*len(self.host_ips[0]), h2_send_targets));
        
