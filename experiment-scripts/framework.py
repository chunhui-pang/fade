"""
The test framework that controls the progress of a single test.
You may refer to IoC (inverse of control)
"""
import logging
import datetime
import time
import shutil
import sys
import subprocess
import traceback
import os

from mininet.cli import CLI


class TestFramework ( object ):
    "the test framework"
    
    def __init__( self, controller, testcase, auto_clean = True, run_cli = False ):
        "initialize the framework"
        self.controller = controller
        self.testcase = testcase
        self.run_cli = run_cli
        self.auto_clean = auto_clean
        self.mininet_log = '/tmp/mininet.log'
        self.has_exception = False
        self.has_finished = False
        self.net = None
        
    def perform_test(  self ):
        "perform a single test"
        if self.has_finished:
            return
        self.has_finished = True
        logging.info('start performing test at time {}'.format(datetime.datetime.now().strftime('%H:%M:%S.%f')))
        self.net = self.testcase.get_net( )
        try:
            self.testcase.pre_start( )
            self.net.start()
            logging.info('do post start task after {} seconds, current: {}'.format( self.testcase.sleep_time_after_net_start(), datetime.datetime.now().strftime('%H:%M:%S.%f') ))
            time.sleep( self.testcase.sleep_time_after_net_start() )
            self.testcase.post_start( )

            logging.info('do test task after {} seconds, current: {}'.format( self.testcase.sleep_time_after_post_start(), datetime.datetime.now().strftime('%H:%M:%S.%f')))
            time.sleep( self.testcase.sleep_time_after_post_start() )
            self.testcase.test( )

            if self.run_cli:
                logging.info('run CLI')
                CLI( self.net )

            logging.info('do clean task after {} seconds, current: {}'.format( self.testcase.sleep_time_after_test_finish(), datetime.datetime.now().strftime('%H:%M:%S.%f')))
            time.sleep( self.testcase.sleep_time_after_test_finish() )
            self.testcase.post_test()
        except:
            self.has_exception = True
            logging.error('An error happens when performing tests')
            traceback.print_exc()
        logging.info('finish performing test at time {}'.format(datetime.datetime.now().strftime('%H:%M:%S.%f')))

    def get_exit_status( self ):
        "retrieve the execution exit status, true if it exits normally, otherwise false"
        return not self.has_exception
    
    def copy_controller_log( self, target_path ):
        "store the controller log"
        if self.has_finished == False:
            raise RuntimeError('You haven\'t performed the test yet!')
        if self.get_exit_status() or self.auto_clean == False:
            self.controller.retrieve_floodlight_log(target_path)
        else:
            raise RuntimeError('cannot retrieve controller log as you command as to clean all relics if any exception happens')

    def copy_controller_config( self, target_path ):
        "copy the configuration of the controller"
        if self.has_finished == False:
            raise RuntimeError('You haven\'t performed the test yet!')
        if self.get_exit_status() or self.auto_clean == False:
            self.controller.retrieve_floodlight_config(target_path)
        else:
            raise RuntimeError('cannot retrieve controller log as you command as to clean all relics if any exception happens')


    def copy_mininet_log( self, target_path ):
        "store the mininet log"
        if self.has_finished == False:
            raise RuntimeError('You haven\'t performed the test yet!')
        if self.get_exit_status() or self.auto_clean == False:
            shutil.copy(self.mininet_log, target_path)
        else:
            raise RuntimeError('cannot retrieve controller log as you command as to clean all relics if any exception happens')

    def clean_test( self ):
        "clean all related objects (logs, processes)"
        if not self.auto_clean:
            logging.info('auto clean option is disabled, please clean the testbed manually.')
            return
        try:
            self.controller.clean_floodlight()
            logging.info('clean controller finished.')
        except:
            logging.error('An error was encountered when clean the controller')
            traceback.print_exc()
        # wait to clean the controller
        time.sleep( 3 )
        try:
            self.testcase.clean(exception=self.has_exception)
        except:
            logging.error('An error was encountered when clean the test case')
            traceback.print_exc()
        logging.info('clean test case finished.')
        try:
            self.net.stop()
            if self.has_exception:
                subprocess.call(['mn', '-c'])
        except:
            logging.error('An error was encountered when clean mininet')
            traceback.print_exc()
        try:
            os.remove(self.mininet_log)
        except:
            pass
        logging.info('clean mininet finished')
