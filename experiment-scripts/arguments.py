"""
experiment arguments
"""
import argparse
import os
import casemanager
import logging

def KeyValuePair(v):
    fields = v.split('=')
    if len(fields) != 2:
        raise argparse.ArgumentTypeError("String must have the format of key=value")
    else:
        return v

def CaseName( v ):
    case_manager = casemanager.TestCaseManager()
    case_names = case_manager.get_case_names()
    for tc in case_names:
        if v == tc:
            return v
    raise argparse.ArgumentTypeError("casenames must be in {}".format(case_names))

def AutoSelect( v ):
    casenames = {'auto': True, 'man': False}
    for name,value in casenames.iteritems():
        if v.lower() == name:
            return value
    raise argparse.ArgumentTypeError("autocontroller must be set in {}".format(casenames.keys()))

def WritableDir( v ):
    if not os.path.isdir(v) or not os.access(v, os.W_OK):
        raise argparse.ArgumentError('a writable directory is wanted as log output directory.')
    if v.endswith('/'):
        return v
    return v + '/'

class ExperimentArguments (object):
    "experiment arguments"

    def __init__(self):
        "initialization"
        self.test_case_params = {}
        self.parser = argparse.ArgumentParser("FADE experiment's argument parser")
        self.add_arguments()
        self.options = self.parser.parse_args()
        logging.info('options are: {}'.format(self.options))
        self.parse_testcase_arguments()

        
    def add_arguments( self ):
        "add arguments"
        self.parser.add_argument('casename', metavar='casename', type=CaseName, help='the test case name')
        self.parser.add_argument('-c', '--controller-mode', metavar='controller_mode', type=AutoSelect, default='man',
                                 help='Start floodlight controller manually or automatically: auto = automatically, man = manually')
        self.parser.add_argument('-p', '--controller-shell-path', metavar='controller_shell_path', type=str,
                                 help='the path of the shell that starts the controller')
        self.parser.add_argument('-t', '--testcase-arguments', metavar='testcase_argument', type=KeyValuePair, nargs='*',
                                 help='the extra arguments delivered to the test case, in the format of key=value')
        self.parser.add_argument('-o', '--log-directory', metavar='log_dir', type=WritableDir,
                                 help='the directory to output logs')
        self.parser.add_argument('-a', '--auto-clean', metavar='auto_clean', type=bool, default=True,
                                 help='clean all relics automatically?')
        self.parser.add_argument('-r', '--run-cli', metavar='run_cli', type=bool, default=False,
                                 help='Run CLI after test is running?')

    def parse_testcase_arguments( self ):
        "parse testcase specific arguments"
        if not self.options.testcase_arguments:
            return
        for param in self.options.testcase_arguments:
            pair = param.split('=')
            if len(pair) == 2:
                self.test_case_params[pair[0]] = pair[1]

    def get_testcase_name( self ):
        "get the name of the executed test case"
        return self.options.casename
    
    def get_controller_start_mode( self ):
        "get the strategy of starting controller"
        return self.options.controller_mode

    def get_controller_shell_path( self ):
        "get the path of the shell that starts the controller"
        return self.options.controller_shell_path

    def get_testcase_arguments( self ):
        "return the test case specific arguments"
        return self.test_case_params
        
    def get_log_directory( self ):
        "return the log directory"
        if not self.options.log_directory:
            return None
        if self.options.log_directory.endswith('/'):
            return self.options.log_directory
        return self.options.log_directory + '/'

    def get_auto_clean( self ):
        "return if the test framework should clean the testbed automatically"
        return self.options.auto_clean

    def get_run_cli( self ):
        "return if a Mininet CLI should be launched after the test"
        return self.options.run_cli
    
