"General aggregated flow test"
import time
import logging
import requests
import json
from mininet.topo import Topo
from mininet.net import Mininet
from mininet.node import Controller, RemoteController, IVSSwitch, UserSwitch, OVSSwitch
from mininet.cli import CLI

from testcase import TestCase
from utils import HostConf
from utils import PathRetriever
from utils import TopoLoader

class GeneralAggregatedTest ( TestCase ):
    """
    general test for aggregated flows. 
    It support several features:
    1. customized topology (loaded from file)
    2. customized host attach points (loaded from file)
    3. customized send/recv packets (loaded from file)
    """
    flow_mod_prefix = 'general-aggregate-test'
    next_flow_mod_id = 1
    def __init__ (self, topo_file, host_file, rule_file, duration=1000, **kwargs):
        super(GeneralAggregatedTest, self).__init__()
        self.topo_file = topo_file
        self.net = None
        self.host_file = host_file
        self.host_confs = None
        self.rule_file = rule_file
        self.duration = duration if isinstance(duration, int) else int(duration)
        # consruct topo
        self.construct_topo_from_file()
        # construct hosts
        self.construct_hosts_from_file()
        if str(self.net.switch).find('UserSwitch') != -1:            
            self.path_retriever = PathRetriever(dpidbase=16)
        else:
            self.path_retriever = PathRetriever(dpidbase=10)
        self.inject_percent = [0.4, 0.5]
        self.inject_count = None

    def post_start( self ):
        "setup delegate ips and start to send traffic"
        self.inject_controller_rules_from_file()
        self.setup_arp_table()
        self.setup_recv_delegate()
        self.setup_send_delegate()

    def test( self ):
        "do test: inject malicious rules"
        pass

    def sleep_time_after_test_finish( self ):
        "test interval"
        return self.duration

    def clean( self, exception=False ):
        "clean all commands"
        self.clean_send_delegate()
        logging.info('sleep 5 seconds to wait for all packet senders to be killed...')
        time.sleep(5)
        self.clean_recv_delegate()

    def construct_topo_from_file( self ):
        "construct the topology file from the matrix"
        self.net = Mininet(controller=lambda name: RemoteController( name, ip='127.0.0.1', port=6653 ), switch=OVSSwitch)
        self.net.addController('c0')
        # add switches
        self.net = TopoLoader.create_topo_from_file(self.net, self.topo_file)

    def construct_hosts_from_file( self ):
        "construct hosts from the host configuration file"
        with open(self.host_file, 'r') as json_file:
            self.host_confs = HostConf.parse_json( json_file )
        if not self.host_confs:
            raise RuntimeError('cannot parse host configuration from file {}'.format(self.host_file))
        for host_conf in self.host_confs:
            name = host_conf.get_name()
            ip = host_conf.get_ip()
            mac = host_conf.get_mac()
            self.net.addHost(name, ip=ip, mac=mac)
            host = self.net.get(name)
            sw = self.net.get(host_conf.get_attach_switch())
            port = host_conf.get_attach_port()
            self.net.addLink(host, sw, port1=None, port2=port if isinstance(port, int) else int(port))
            
    def inject_controller_rules_from_file( self ):
        "load flow rules from file self.rule_file, and inject these rules into switches through controller"
        staticflowpusher_url = 'http://127.0.0.1:8080/wm/staticflowpusher/json'
        with open(self.rule_file, 'r') as rf:
            for entry in rf.readlines():
                entry = entry.strip(' \r\n')
                switch_name, ip_dst, out_port = entry.split()
                hex_dpid = hex(int(switch_name[1:]))[2:]
                dpid = '0' * (12-len(hex_dpid)) + hex_dpid
                dpid = dpid[0:2] + ':' + dpid[2:4] + ':' + dpid[4:6] + ':' + dpid[6:8] + ':' + dpid[8:10] + ':' + dpid[10:12]
                json_data = json.dumps({
                    'name': 'flow-mod-' + GeneralAggregatedTest.flow_mod_prefix + '-' + str(GeneralAggregatedTest.next_flow_mod_id),
                    'entry_type': 'flow',
                    'switch': dpid,
                    'cookie': '0',
                    'priority': '1',
                    'eth_type': '0x0800',
                    'ipv4_dst':   ip_dst,
                    'actions':  'output=' + out_port,
                    'active': 'true',
                })
                GeneralAggregatedTest.next_flow_mod_id += 1
                result = requests.post(staticflowpusher_url, data = json_data)
                if not result.status_code == requests.codes.ok:
                    logging.error('fail to inject rule: {}, error message: {}', entry, result.text)
        logging.info('rules in file {} has been injected.', self.rule_file)
        
    def setup_arp_table( self ):
        "setup arp table for every host"
        for host_conf in self.host_confs:
            if not host_conf.get_arp_table() and len(host_conf.get_arp_table()) != 0:
                host = self.net.get(host_conf.get_name())
                for ip, mac in host_conf.get_arp_table().iteritems():
                    host.setARP(ip, mac)
        logging.info('setup arp table finished')
        
    def setup_recv_delegate( self ):
        """
        construct receive agent, we add a Linux Bridge to the host, and install an ebtables on the bridge to send arp reply automatically.
        As for the default interface (hi-eth0) has to be binded on the bridge and its IP address should be unset, 
        the bridge would be used as the default interface and the IP address of the origin default interface would be assigned to it.
        """
        for host_conf in self.host_confs:
            recv_ips = host_conf.get_delegate_ips()
            if recv_ips and len(recv_ips) != 0:
                host = self.net.get(host_conf.get_name())
                # down the default interface and unset its ip addresses
                host.cmd('ifconfig {}-eth0 down'.format(host_conf.get_name()))
                host.cmd('ifconfig {}-eth0 0.0.0.0'.format(host_conf.get_name()))
                # add bridge
                host.cmd('brctl addbr br0')
                host.cmd('brctl addif br0 {}-eth0'.format(host_conf.get_name()))
                host.cmd('ifconfig br0 {}'.format(host_conf.get_ip()))
                # launch the network
                host.cmd('ifconfig {}-eth0 up'.format(host_conf.get_name()))
                host.cmd('ifconfig br0 up')
                # set up ebtables to send ARP reply automatically
                for ip in recv_ips:
                    host.cmd('ebtables -t nat -A PREROUTING -p arp --arp-opcode Request --arp-ip-dst {} -j arpreply --arpreply-mac {} --arpreply-target ACCEPT'.format(ip, host.MAC()))
                # disable ICMP port unreachable message
                host.cmd('iptables -I OUTPUT -p icmp --icmp-type destination-unreachable -j DROP')

    def clean_recv_delegate( self ):
        "clean utilities launched for receive delegate"
        for host_conf in self.host_confs:
            recv_ips = host_conf.get_delegate_ips()
            if recv_ips and len(recv_ips) != 0:
                host = self.net.get(host_conf.get_name())
                host.cmd('ebtables -t nat -D PREROUTING 1:')
                host.cmd('ifconfig br0 down')
                host.cmd('brctl delif br0 {}-eth0'.format(host_conf.get_name()))
                host.cmd('brctl delbr br0')

    def setup_send_delegate( self ):
        "start to send packets"
        sport = 2000
        dport_start = 3000
        dport_end = 5000
        count = 1000000
        delay = 10000
        dports = '{}-{}'.format(dport_start, dport_end)
        for host_conf in self.host_confs:
            send_dsts = host_conf.get_send_dsts()
            if send_dsts and len(send_dsts) != 0:
                host = self.net.get(host_conf.get_name())
                for ip_src in send_dsts:
                    ip_dsts = send_dsts[ip_src]
                    dsts = ''
                    for ip in ip_dsts:
                        dsts += (' ' + ip)
                    command = './sendpkt -u {} -g {} -p {} -w {}us -c {} {} &'.format(ip_src, sport, dports, delay, count, dsts)
                    logging.info('{} command: {}'.format(host_conf.get_name(), command))
                    host.cmd(command)
        logging.info('traffic injected for every pair of hosts, last for {} seconds...'.format(count*delay/1000/1000) )
                    
    def clean_send_delegate( self ):
        "kill all packet senders"
        for host_conf in self.host_confs:
            send_dsts = host_conf.get_send_dsts()
            if send_dsts and len(send_dsts) != 0:
                host = self.net.get(host_conf.get_name())
                host.cmd('kill %./sendpkt')
        
