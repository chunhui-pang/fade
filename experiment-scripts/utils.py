"utils for testcases"

import json
import requests
import logging
import re

class TopoLoader( object ):
    """load topology from file"""
    def __init__ (self):
        pass

    @staticmethod
    def load_matrix_from_file (topo_file):
        "load a topology matrix from a given file"
        matrix = None
        name = None
        size = None
        with open(topo_file, 'r') as tf:
            for line in tf.readlines():
                if line.isspace():
                    if name != None:
                        if len(matrix) != size[0]:
                            raise ValueError('the data size is not consistent with the row value in {}'.format(name))
                        port_assign_mode = TopoLoader.validate_and_analyze_matrix(matrix)
                        return (port_assign_mode, matrix)
                elif name == None:
                    name = line[:-1]
                elif size == None:
                    size = [int(i) for i in line.split()]
                    matrix = []
                else:
                    row = [int(i) for i in line.split()]
                    if len(row) != size[0]:
                        raise ValueError('the row size is not consistent with the row value in {}'.format(name))
                    matrix.append(row)
        return None
    @staticmethod
    def validate_and_analyze_matrix(matrix):
        "validate loaded matrix"
        if matrix == None:
            raise ValueError('parameter matrix cannot be none.')
        # port assign mode: auto/manual
        port_assign_mode = None
        matrix_len = len(matrix)
        for row in matrix:
            if matrix_len != len(row):
                raise ValueError('parameter matrix is illegal.')
            uniq_port = set()
            list_port = []
            for val in row:
                if val < 0:
                    raise ValueError('parameter matrix is illegal.')
                elif val > 0:
                    uniq_port.add(val)
                    list_port.append(val)
            # determine port assign mode
            if len(uniq_port) != len(list_port):
                if port_assign_mode == 'manual':
                    raise ValueError('illegal matrix, some rows indicate it has assign port manually, and others indicate we should assign ports automatically.')
                else:
                    port_assign_mode = 'auto'
            elif len(uniq_port) != 0  and len(uniq_port) != 1:
                if port_assign_mode == 'auto':
                    raise ValueError('illegal matrix, some rows indicate it has assign port manually, and others indicate we should assign ports automatically.')
                else:
                    port_assign_mode = 'manual'
        return port_assign_mode

    @staticmethod
    def create_topo_from_file(net, topo_file):
        "create topology from file"
        port_assign_mode, matrix = TopoLoader.load_matrix_from_file(topo_file)
        for i in range(1, len(matrix)+1):
            s = net.addSwitch('s{}'.format(i), dpid=str(i), protocols='OpenFlow10')
        # add links between switches
        for i in range(len(matrix)):
            for j in range(len(matrix)):
                if i < j and matrix[i][j] != 0:
                    s1 = net.get('s{}'.format(i+1))
                    s2 = net.get('s{}'.format(j+1))
                    if port_assign_mode == 'manual':
                        net.addLink(s1, s2, port1=matrix[i][j], port2=matrix[j][i])
                    else:
                        net.addLink(s1, s2)
        return net


class PathRetriever( object ):
    """retrieving path from floodlight controller. The communication channel is restful"""
    def __init__( self, base_uri='http://127.0.0.1:8080/wm/dumproute/dump/', dpidbase=10):
        "In UserSwitch, the dpidbase should be 10, and in ovs, it should be 16"
        logging.getLogger("requests").setLevel(logging.WARNING)
        logging.getLogger("urllib3").setLevel(logging.WARNING)
        self.base_uri = base_uri
        self.dpidbase=dpidbase

    def get_path ( self, src, dst, addr_type='mac' ):
        "return the forwarding path only include the outport"
        data = {'src': src, 'dst': dst}
        target_uri = self.construct_target_uri(addr_type)
        response = requests.post( target_uri, data = json.dumps(data) )
        if response.status_code == 200:
            txt_path = json.loads( response.text )
            path = []
            for node in txt_path:
                path.append( ( int( node['dpid'].replace(':', ''), self.dpidbase ), int(node['port']) ) )
            return path[1::2]
        else:
            logging.error('receive response error: {}'.format(response.text))
            return None

    def get_raw_path( self, src, dst, addr_type='mac'):
        "return the forwarding path include both the inport and the outport"
        data = {'src': src, 'dst': dst}
        target_uri = self.construct_target_uri(addr_type)
        response = requests.post( target_uri, data = json.dumps(data) )
        if response.status_code == 200:
            txt_path = json.loads( response.text )
            path = []
            for node in txt_path:
                path.append( ( int( node['dpid'].replace(':', ''), self.dpidbase ), int(node['port']) ) )
            return path
        else:
            logging.error('receive response error: {}'.format(response.text))
            return None

    def construct_target_uri( self, addr_type ):
        "construct a complete uri from the address type (mac and ip are allowed)"
        if not isinstance(addr_type, str):
            raise RuntimeError('addr_type must be str')
        if addr_type.lower() == 'mac':
            return self.base_uri + 'byMAC'
        elif addr_type.lower() == 'ip':
            return self.base_uri + 'byIP'
        else:
            raise RuntimeError('addr_type must in mac and ip')
        
class HostConf (object):
    "The Configuration of experiment hosts in AggregatedFlow"

    def __init__ ( self, name, attach_switch, attach_port, ip, mac, arp_table, delegate_ips = [], send_dsts = {}):
        "initialize all properties"
        self.name = name
        self.attach_switch = attach_switch
        self.attach_port = attach_port
        self.ip = ip
        self.mac = mac
        self.arp_table = arp_table
        self.delegate_ips = delegate_ips
        self.send_dsts = send_dsts

    def get_name( self ):
        "get the unique name of the host"
        return self.name

    def get_attach_switch( self ):
        "get the attached switch"
        return self.attach_switch

    def get_attach_port( self ):
        "get the attach point on the switch"
        return self.attach_port

    def get_ip( self ):
        "get the ip address of the host"
        return self.ip

    def get_mac( self ):
        "get the mac address of the hosts"
        return self.mac

    def get_arp_table( self ):
        "get the arp table, IP - MAC"
        return self.arp_table

    def get_delegate_ips( self ):
        "get the list of ip to which this host should send packets"
        return self.delegate_ips

    def get_send_dsts( self ):
        "get the ip addresses for which this host may receive packets"
        return self.send_dsts

    @staticmethod
    def parse_json( json_file ):
        objs = json.load(json_file)
        hosts = []
        for obj in objs:
            if not obj['name']:
                print 'host name must be specified.'
                return False
            if not obj['attach_switch']:
                print 'attach_switch must be specified'
                return False
            if not obj['attach_port']:
                print 'attach_port must be specified'
                return False
            if obj['ip'] and not HostConf.validate_ip(obj['ip']):
                print obj['ip'] + ' is not an valid ip'
                return False
            if obj['mac'] and not HostConf.validate_mac(obj['mac']):
                print obj['mac'] + ' is not a valid mac'
                return False
            if obj['arp_table'] and not isinstance(obj['arp_table'], dict):
                print 'arp_table must be a dict'
                return False
            if obj['arp_table']:
                for ip,mac in obj['arp_table'].iteritems():
                    if (not HostConf.validate_ip(ip)) or (not HostConf.validate_mac(mac)):
                        print 'arp_table entry "{}": "{}" is illegal'.format(ip, mac)
                        return False
            if obj['delegate_ips'] and not isinstance(obj['delegate_ips'], list):
                print 'delegate_ips must be a list'
                return False
            if obj['delegate_ips']:
                for ip in obj['delegate_ips']:
                    if not HostConf.validate_ip(ip):
                        print 'delegate_ips ' + ip + ' is not a valid ip'
                        return False
            if obj['send_dsts'] and not isinstance(obj['send_dsts'], dict):
                print 'send_dsts must be a dict'
                return False
            if obj['send_dsts']:
                for src in obj['send_dsts']:
                    dsts = obj['send_dsts'][src]
                    if not HostConf.validate_ip(src):
                        print 'send_dsts ' + src + ' is not a valid ip'
                        return False
                    for dst in dsts:
                        if not HostConf.validate_ip(dst):
                            print 'send_dsts ' + dst + ' is not a valid ip'
                            return False
            host_conf = HostConf(obj['name'], obj['attach_switch'], obj['attach_port'], obj['ip'], obj['mac'], obj['arp_table'], obj['delegate_ips'], obj['send_dsts'])
            hosts.append(host_conf)
        return hosts

    @staticmethod
    def validate_ip( ip ):
        parts = ip.split('.')
        if len(parts) != 4:
            return False
        for i in range( len(parts) ):
            if not parts[i].isdigit():
                return False
            else:
                n = int(parts[i])
                if n < 0 or n > 255:
                    return False
        return True

    @staticmethod
    def validate_mac( mac ):
        "validate MAC address"
        mac_regex = re.compile('([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}')
        return mac_regex.match(mac)
            
