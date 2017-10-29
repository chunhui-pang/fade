"""
Test for aggregated flow.
We test aggregated flow with a linear topology, and several host was attached to each edge switch.
Each switch in one edge would send packets to the corresponding another one at the another end of edge.
"""
import time
import datetime
import logging
from mininet.topo import Topo
from mininet.net import Mininet
from mininet.node import Controller, RemoteController, IVSSwitch, UserSwitch, OVSSwitch
from mininet.cli import CLI
import shlex, subprocess

from testcase import TestCase

class AggregatedFlowTestTopo (Topo):
    "linear topology for aggregated flow test"
    def __init__ (self, nodes = 5, hosts = 5, **kwargs):
        Topo.__init__(self)
        sws = []
        self.left_hosts = []
        self.right_hosts = []
        # create switches
        for i in range(nodes):
            sws.append(self.addSwitch('s{}'.format(i+1), protocols='OpenFlow10'))
            if i != 0:
                self.addLink(sws[i-1], sws[i])
        # add hosts
        for i in range(hosts):
            # construct left part of hosts
            tmp = self.addHost('h1{}'.format(i+1), ip='10.0.1.{}/16'.format(100+i+1))
            self.left_hosts.append(tmp)
            self.addLink(tmp, sws[0])
            # construct right part of hosts
            tmp = self.addHost('h2{}'.format(i+1), ip='10.0.2.{}/16'.format(100+i+1))
            self.right_hosts.append(tmp)
            self.addLink(tmp, sws[len(sws)-1])
                
    def get_left_hosts( self ):
        "get left part of hosts"
        return self.left_hosts

    def get_right_hosts( self ):
        "get right part of hosts"
        return self.right_hosts
    
class AggregatedTest( TestCase ):
    "test case for aggregated flow"
    def __init__( self, nodes=5, hosts=5, inject = False, **kwargs):
        super(AggregatedTest, self).__init__()
        self.nodes = int(nodes) if not isinstance(nodes, int) else nodes
        self.hosts = int(hosts) if not isinstance(hosts, int) else hosts
        self.topo = AggregatedFlowTestTopo(self.nodes, self.hosts, **kwargs)
        self.net = Mininet(self.topo, controller=lambda name: RemoteController( name, ip='127.0.0.1', port=6653 ), switch=OVSSwitch)
        # self.net = Mininet(self.topo)
        self.left_hosts = self.construct_hosts(self.topo.get_left_hosts())
        self.right_hosts = self.construct_hosts(self.topo.get_right_hosts())
        self.vhost = self.right_hosts[0]
        self.vip = '10.0.3.1'
        # ignore construct packet sniffing and anomaly injection
        # this is ONLY a test script, not the final experiment scripts

    def construct_hosts(self, host_names):
        hosts_node = []
        for name in host_names:
            hosts_node.append(self.net.getNodeByName(name))
        return hosts_node

    def post_start( self ):
        # leverage ping to construct the initial flow rule
        for i in range( len(self.left_hosts)):
            self.net.ping([self.left_hosts[i], self.right_hosts[i]])
        self.construct_vhost()
        
    def construct_vhost( self ):
        ip = self.vhost.IP()
        self.vhost.cmd('ifconfig {}-eth0 down'.format(self.vhost.name))
        self.vhost.cmd('ifconfig {}-eth0 0.0.0.0'.format(self.vhost.name))
        # add bridge
        self.vhost.cmd('brctl addbr br0')
        self.vhost.cmd('brctl addif br0 {}-eth0'.format(self.vhost.name))
        self.vhost.cmd('ifconfig br0 {}'.format(ip))
        # launch the network
        self.vhost.cmd('ifconfig {}-eth0 up'.format(self.vhost.name))
        self.vhost.cmd('ifconfig br0 up')
        # set up ebtables to send ARP reply automatically
        self.vhost.cmd('ebtables -t nat -A PREROUTING -p arp --arp-opcode Request --arp-ip-dst {} -j arpreply --arpreply-mac {} --arpreply-target ACCEPT'.format(self.vip, self.vhost.MAC()))
        # disable ICMP port unreachable message
        self.vhost.cmd('iptables -I OUTPUT -p icmp --icmp-type destination-unreachable -j DROP')

    def inject_malicious( self ):
        # do nothing
        sw_name = 's{}'.format(self.nodes/2)
        cmd = 'ovs-ofctl add-flow {} ip,nw_dst={},priority=255,idle_timeout=0,hard_timeout=0,actions=drop'.format(sw_name, self.vip)
        logging.info('inject rule on switch {}, cmd="{}"'.format(sw_name, cmd))
        cmds = shlex.split(cmd)
        subprocess.Popen(cmds)
        logging.info('malicious rule injected finished, current: {}'.format( datetime.datetime.now().strftime('%H:%M:%S.%f') ))

    
    def test( self ):
        sport = 2000
        dport_start = 3000
        dport_end = 6000
        count = 1000000
        delay = 10000
        dports = '{}-{}'.format(dport_start, dport_end + 1)
        time.sleep(1)
        # destination port is used as the packets count to be sent and the packet identity
        for i in range( len(self.left_hosts) ):
            for j in range( len(self.right_hosts)):
                left_name = self.left_hosts[i].name
                left_ip = self.left_hosts[i].IP()
                right_name = self.right_hosts[j].name
                right_ip = self.right_hosts[j].IP()
                # construct and inject commands
                left_command = './sendpkt -i {}-eth0 -g {} -p {} -w {}us -c {} {} &'.format(left_name, sport, dports, delay, count, right_ip)
                logging.info('send command to {}: {}'.format(left_name, left_command))
                right_command = './sendpkt -i {}-eth0 -g {} -p {} -w {}us -c {} {} &'.format(right_name, sport, dports, delay, count, left_ip)
                logging.info('send command to {}: {}'.format(right_name, right_command))
                self.left_hosts[i].cmd(left_command)
                self.right_hosts[i].cmd(right_command)
            # send traffic to virtual host
            v_command = './sendpkt -i {}-eth0 -g {} -p {} -w {}us -c {} {} &'.format(left_name, sport, dports, delay, count, self.vip)
            logging.info('send command to {}: {}'.format(left_name, v_command))
            self.left_hosts[i].cmd(v_command)
        logging.info('traffic injected, last for {} seconds...'.format(count*delay/1000/1000) )
        time.sleep(10)
        self.inject_malicious()

    def sleep_time_after_test_finish( self ):
        return 300

    def clean( self, exception = False ):
        for host in self.left_hosts:
            host.cmd('kill %./sendpkt')
        for host in self.right_hosts:
            host.cmd('kill %./sendpkt')
        time.sleep(3)
        
