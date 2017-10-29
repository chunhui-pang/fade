"""
manage all test cases
"""
import os
import logging
import inspect



    
class TestCaseManager (object):
    "test case manager, dynamically load test cases from directory 'testcase'"

    def __init__ (self, directory = './testcases/'):
        self.filename_suffix = 'test.py'
        self.class_suffix = 'Test'
        self.module_suffix = 'test'
        self.case_names = [str(f)[:-len(self.filename_suffix)] for f in os.listdir(directory) if str(f).endswith(self.filename_suffix)]
        self.case_mod = self.get_module_from_directory(directory, self.case_names)
        logging.info('find testcases: {}'.format(self.case_names))

    def get_module_from_directory( self, directory, case_names ):
        "construct a module from directory"
        idx1 = directory.find('/')
        idx1 = 0 if idx1 == 0 else idx1 + 1
        idx2 = directory.find('/', idx1)
        idx2 = len(directory) if idx2 == -1 else idx2
        module_name = directory[idx1:idx2]
        load_mods = [mod + self.module_suffix for mod in self.case_names]
        return __import__(module_name, globals(), locals(), load_mods, -1)

    def get_case_names( self ):
        "get all test case names"
        return self.case_names

    def create_test_case( self, casename, kwargs ):
        "get a specific test case"
        submodule_name = casename.lower() + self.module_suffix
        class_module = getattr(self.case_mod, submodule_name)
        testcase_obj = None
        for name,obj in inspect.getmembers(class_module, inspect.isclass):
            if name.lower() == submodule_name:
                testcase_obj = obj
                break
        if not testcase_obj:
            raise RuntimeError('cannot find the test class {}'.format(submodule_name))
        return testcase_obj() if not kwargs else testcase_obj(**kwargs)
    
