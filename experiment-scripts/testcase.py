"""Test cases for project"""
import shutil
import os
import logging

class TestCase(object):
    def __init__( self ):
        "initialization"
        self.topo = None
        self.net = None

    def pre_start( self ):
        "do preparement for network start"
        pass

    def get_net( self ):
        "return the mininet object or network object here"
        return self.net

    def sleep_time_after_net_start( self ):
        "the interval between the start of network and do post start action"
        return 1
    
    def post_start( self ):
        "do what you want immediately after the start of the network, or the preparement of your test"
        pass

    def sleep_time_after_post_start( self ):
        "the interval between the finish of post start action and the begin of the test"
        return 1
    
    def test( self ):
        "do test here"
        pass

    def sleep_time_after_test_finish( self ):
        "the interval between finish of calling test and do clean task, or the test duration"
        return 10
    
    def clean( self, exception = False ):
        "do clean work, the network will shutdown immediately after this"
        pass

    def post_test( self, exception = False ):
        "do extra works after correct executions"
        pass

        
