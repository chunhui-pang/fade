"""
manage floodlight instance
"""
import logging
import subprocess
import shutil
import os
import time
import signal

class FloodlightController ( object ):
    "manage floodlight controller"

    def __init__ (self, auto_start = False, start_shell=None):
        self.start_shell = start_shell
        self.auto_start = auto_start
        self.has_started = False
        self.process = None
        if self.auto_start:
            if start_shell == None:
                raise RuntimeError('we want the controller starting shell path if the controller is automatically delegated.')
            else:
                self.start_floodlight( )

    def start_floodlight ( self ):
        "start the floodlight"
        if self.has_started:
            return
        self.has_started = True
        cmds = self.start_shell.split()
        cwd = self.get_cwd_from_shell_path( )
        with open(os.devnull, 'w')  as DEVNULL:
            try:
                self.process = subprocess.Popen(cmds, cwd=cwd, stdout=DEVNULL, stderr=DEVNULL)
            except subprocess.CalledProcessError:
                logging.error('cannot start floodlight controller, exit')
                raise RuntimeError('cannot start floodlight controller')
        logging.info('sleep 5 seconds to wait for floodlight start')
        time.sleep(5)

    def get_cwd_from_shell_path( self ):
        "get the cwd from the shell scripts"
        idx = self.start_shell.rfind('/')
        return self.start_shell[:idx]

    def get_floodlight_pid( self ):
        "get the pid of floodlight"
        cmds = 'pgrep -f java.*floodlight'.split()
        p = subprocess.Popen(cmds, stdout=subprocess.PIPE)
        out, err = p.communicate()
        pid = None
        for line in out.splitlines():
            pid = int(line)
        return pid
    
    def check_status( self ):
        "is the controller is running?"
        pid = self.get_floodlight_pid( )
        return True if pid != None else False
        
    def stop_floodlight( self ):
        "just stop floodlight"
        pid = self.get_floodlight_pid()
        if pid != None:
            logging.info('killing floodlight process {}'.format(pid))
            os.kill(pid, signal.SIGTERM)
        else:
            logging.error('cannot find floodlight process')
        self.process = None
        self.has_started = None

    def get_log_path( self ):
        return '/tmp/floodlight.log'

    def get_config_path( self ):
        if self.start_shell:
            idx = self.start_shell.rfind('/')
            parent_path = self.start_shell[:idx]
            return parent_path + '/src/main/resources/floodlightdefault.properties'
        else:
            return None

    def clean_floodlight( self ):
        "stop process and clean logs"
        if self.has_started:
            self.stop_floodlight()
        # remove logs
        try:
            os.remove( self.get_log_path() )
        except OSError:
            pass
        
    def retrieve_floodlight_log( self, target_path ):
        "copy logs to another file"
        if self.auto_start:
            shutil.copy(self.get_log_path(), target_path)

    def retrieve_floodlight_config( self, target_path ):
        "copy configurations"
        shutil.copy(self.get_config_path(), target_path)
        
