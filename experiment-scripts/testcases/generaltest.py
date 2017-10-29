import time
import subprocess
import shlex
import shutil
import random
import math
import datetime
import os
import logging


from mininet.net import Mininet
from mininet.node import Controller, RemoteController, IVSSwitch, UserSwitch, OVSSwitch
from mininet.nodelib import LinuxBridge
from mininet.cli import CLI

from testcase import TestCase
from utils import PathRetriever, TopoLoader

class GeneralTest( TestCase ):
    "A general test case which is based on customized topology build from adjacent matrix, and can randomly inject malicious rules"
    def __init__( self, topo_file=None, duration=360, hosts=2, **kwargs):
        "initialization"
        super(GeneralTest, self).__init__( )
        self.duration = duration if isinstance(duration, int) else int(duration)
        self.fake_host_per_switch = hosts if isinstance(hosts, int) else int(hosts)
        # initialization
        self.net = Mininet(controller=lambda name: RemoteController( name, ip='127.0.0.1', port=6653 ), switch=OVSSwitch)
        self.net.addController('c0')
        TopoLoader.create_topo_from_file( self.net, topo_file)
        # add hosts
        for i in range(1, len(self.net.switches)+1):
            s = self.net.get('s{}'.format(i))
            h = self.net.addHost('h{}'.format(i), ip='10.{}.{}.1'.format(i/255, i%255))
            self.net.addLink(h, s)
        if str(self.net.switch).find('UserSwitch') != -1:            
            self.path_retriever = PathRetriever(dpidbase=16)
        else:
            self.path_retriever = PathRetriever(dpidbase=10)
        self.inject_percent = [0.4, 0.5]
        self.inject_count = None

    def construct_fake_hosts( self ):
        "leverage ebtables to construct fake hosts"
        for i in range(1, len(self.net.switches)+1):
            # construct fake hosts, fake host ip: 10.0.sw_id.fake_id or 10.0.i.j
            host = self.net.get('h{}'.format(i))
            fake_ips = []
            for fake_id in range(2, self.fake_host_per_switch+1):
                fake_ips.append('10.0.{}.{}'.format(i, fake_id))
            # down the default interface and unset its ip addresses
            host.cmd('ifconfig {}-eth0 down'.format(host.name))
            host.cmd('ifconfig {}-eth0 0.0.0.0'.format(host.name))
            # add bridge
            host.cmd('brctl addbr br0')
            host.cmd('brctl addif br0 {}-eth0'.format(host.name))
            host.cmd('ifconfig br0 {}'.format(host.IP()))
            # launch the network
            host.cmd('ifconfig {}-eth0 up'.format(host.name))
            host.cmd('ifconfig br0 up')
            # set up ebtables to send ARP reply automatically
            for fake_ip in fake_ips:
                host.cmd('ebtables -t nat -A PREROUTING -p arp --arp-opcode Request --arp-ip-dst {} -j arpreply --arpreply-mac {} --arpreply-target ACCEPT'.format(fake_ip, host.MAC()))
            # disable ICMP port unreachable message
            host.cmd('iptables -I OUTPUT -p icmp --icmp-type destination-unreachable -j DROP')

    def init_fake_network( self ):
        "luanch the fake network, leverage ./sendpkt to wake it up"
        max_sw_id = len(self.net.switches)
        for i in range(1, max_sw_id+1):
            host = self.net.get('h{}'.format(i))
            for fake_id in range(1, self.fake_host_per_switch+1):
                fake_target = []
                for j in range(1, max_sw_id+1):
                    if j != i:
                        fake_target.append('10.0.{}.{}'.format(j, fake_id))
                src = '10.0.{}.{}'.format(i, fake_id)
                host.cmd('./sendpkt -u {} -w {}ms -c {} {}'.format(src, 10, 5*len(fake_target), ' '.join(fake_target)))
        
    def post_start( self ):
        "do what you want immediately after the start of the network, or the preparement of your test"
        if self.fake_host_per_switch > 1:
            self.construct_fake_hosts()
            self.init_fake_network()
        self.net.pingAll()

    
    def sleep_time_after_post_start( self ):
        "the interval between the finish of post start action and the begin of the test"
        return 10

    def inject_malicious_rules( self ):
        "inject malicious rules"
        # initialze essential variables
        hosts = []
        dpid2sw = {}
        for h in self.net.hosts:
            hosts.append( h )
        for sw in self.net.switches:
            dpid2sw[int(sw.defaultDpid(), 16)] = sw

        # retrieve paths
        percent = random.uniform( *self.inject_percent )
        random.shuffle( hosts )
        count = math.ceil( len( hosts ) * percent )
        count = len(hosts) if count > len(hosts) else int(count)
        dst_sel = hosts[:count]
        inject_points = []
        for dst in dst_sel:
            run = 0
            while run < len( hosts ):
                run += 1
                src = random.choice( hosts )
                if src != dst:
                    path = self.path_retriever.get_path( src.MAC(), dst.MAC() )
                    if path and len(path) >= 3:
                        inject_point = path[ random.randint( 1, len(path)-2 ) ]
                        sw = dpid2sw[ inject_point[0] ]
                        if sw != None:
                            sw_name = sw.name
                            outport = 2 if inject_point[1] == 1 else (inject_point[1] - 1)
                            inject_points.append( (dst.IP(), sw_name, outport ) )
                            break

        inject_points = inject_points[:4]
        self.inject_count = len(inject_points)
        # build inject commands
        inject_cmd = []
        for dst, sw_name, outport in inject_points:
            if str(self.net.switch).find('OVSSwitch') != -1:            
                cmd = 'ovs-ofctl add-flow {} ip,nw_dst={},priority=255,actions=output:{}'.format(sw_name, dst, outport )
                logging.info('inject rule on switch {}, cmd="{}"'.format(sw_name, cmd))
            elif str(self.net.switch).find('UserSwitch') != -1:
                cmd = 'dpctl unix:/tmp/{} flow-mod cmd=add,table=0,prio=255 eth_type=0x800,ip_dst={} apply:output={}'.format(sw_name, dst, outport )
                logging.info('inject rule on switch {}, cmd="{}"'.format(sw_name, cmd))
            else:
                raise ValueError('switch unrecognized! cannot inject malicious rules')
            inject_cmd.append(cmd)
        # inject malicious flows
        for cmd in inject_cmd:
            cmds = shlex.split(cmd)
            subprocess.Popen(cmds)
        logging.info('malicious rule injected finished, current: {}'.format( datetime.datetime.now().strftime('%H:%M:%S.%f') ))
        
    def test( self ):
        "do test here"
        sport = 2000
        dport_start = 3000
        dport_end = 5000
        count = 10000000
        delay = 3000
        dports = '{}-{}'.format(dport_start, dport_end)
        max_sw_id = len(self.net.switches)
        for i in range(1, max_sw_id+1):
            host = self.net.get('h{}'.format(i))
            for fake_id in range(1, self.fake_host_per_switch+1):
                fake_target = []
                for j in range(1, max_sw_id+1):
                    if j != i:
                        fake_target.append('10.0.{}.{}'.format(j, fake_id))
                src = '10.0.{}.{}'.format(i, fake_id)
                host.cmd('./sendpkt -u {} -g {} -p {} -w {}us -c {} {} &'.format(src, sport, dports, delay, count, ' '.join(fake_target)))
        logging.info('traffic injected for every pair of hosts, last for {} seconds...'.format(count*delay/1000/1000) )

        random.seed(datetime.datetime.now())
        intval = random.randint(5, 20)
        logging.info('inject malicious flow rules after {} seconds...'.format(intval))
        time.sleep(intval)
        self.inject_malicious_rules( )
        # CLI(self.net)

    def sleep_time_after_test_finish( self ):
        "the interval between finish of calling test and do clean task, or the test duration"
        return self.duration
    
    def clean( self, exception = False ):
        "do clean work, the network will shutdown immediately after this"
        super(GeneralTest, self).clean(exception)
        for host in self.net.hosts:
            host.cmd('kill %./sendpkt')


