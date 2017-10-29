import datetime
import time
import os, signal
import sys
import logging
import traceback

from floodlight import FloodlightController
from arguments import ExperimentArguments
from framework import TestFramework
from casemanager import TestCaseManager
               

def init_logger( ):
    "initalize logger"
    logging.basicConfig(level=logging.DEBUG, format='%(asctime)s,%(msecs)d %(name)-8s %(levelname)-4s %(message)s', datefmt='%Y-%m-%d %H:%M:%S', filename='/tmp/mininet.log', filemode='w')
    console = logging.StreamHandler()
    console.setLevel(logging.INFO)
    formatter = logging.Formatter('%(name)-8s %(levelname)-4s %(message)s')
    console.setFormatter(formatter)
    logging.getLogger('').addHandler(console)

if __name__ == '__main__':
    init_logger()
    arguments = ExperimentArguments()
    floodlightInst = FloodlightController(arguments.get_controller_start_mode(), arguments.get_controller_shell_path())
    case_manager = TestCaseManager()
    
    if floodlightInst.check_status() == False:
        logging.error('cannot find running floodlight instance.')
        sys.exit(1)
    case = case_manager.create_test_case( arguments.get_testcase_name(), arguments.get_testcase_arguments() )
    framework = TestFramework(floodlightInst, case, arguments.get_auto_clean(), arguments.get_run_cli())
    try:
        framework.perform_test()
        # copy logs
        if arguments.get_log_directory() and framework.get_exit_status():
            logging.info('copy log files to log directory...')
            framework.copy_controller_log( arguments.get_log_directory() + 'floodlight.log')
            framework.copy_mininet_log( arguments.get_log_directory() + 'mininet.log')
            framework.copy_controller_config( arguments.get_log_directory() + 'floodlight.properties')
    except:
        traceback.print_exc()
    finally:
        # clean
        logging.info('performing clean operations.')
        framework.clean_test()
    
