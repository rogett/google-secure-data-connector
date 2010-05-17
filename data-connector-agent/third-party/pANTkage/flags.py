#!/usr/bin/python
#
# Copyright 2008 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#   

import sys
import re
import getopt
import pprint

class FlagsError(Exception):
  def __init__(self, args=['']):
    self.args = args
  def __str__(self):
    return repr(self.args)


class Flags:

  def __init__(self, argv=sys.argv):
    self._argv = argv
    self._flag_list = {'help': ['help', 'boolean', '0', 'help', 
      'Print Usage', 0]}
    self._getopt_options = []
    self._usage = []
    self.flags = {}

  def set_integer(self, longname, default=None, description=''):
    self._flag_list[longname] = ['%s=' % longname, 'integer', default, 
      description, 0]

  def set_string(self, longname, default=None, description=''):
    self._flag_list[longname] = ['%s=' % longname, 'string', default, 
      description, 0]

  def set_boolean(self, longname, default=None, description=''):
    self._flag_list[longname] = [longname, 'boolean', default, 
      description, 0]

  def usage(self):
    print '%s usage:' % self._argv[0]
    for key in self._flag_list.keys():
      print '--%s : %s : default=%s' % (self._flag_list[key][0], 
        self._flag_list[key][3], self._flag_list[key][2])

  def process_flags(self):
    # get arguments
    getopt_flags = []
    for key in self._flag_list.keys():
      getopt_flags.append(self._flag_list[key][0])
    try:
      opts, args = getopt.getopt(self._argv[1:], '', getopt_flags)
    except getopt.GetoptError, e:
      print 'error: %s' % e
      self.usage()
      sys.exit(1)
    # extract arguments from getopt
    for option, value in opts:
      option = option[2:]
      if option == 'help':
        self.usage()
        sys.exit(1)
      for key in self._flag_list.keys():
        if key == option:
          if self._flag_list[option][1] == 'boolean':
            self.flags[option] = 1 # set to true
            self._flag_list[key][4] = 1 # specified
          elif self._flag_list[key][1] == 'string':
            self.flags[option] = value # set to value
            self._flag_list[key][4] = 1 # specified
          elif self._flag_list[key][1] == 'integer':
            try:
              self.flags[key] = int(value) # set to integer
            except ValueError:
              print 'error: %s is not a valid integer' % option
              self.usage()
              sys.exit(1)
            self._flag_list[key][4] = 1 # specified
          break

    # set defaults if not specified with getopt
    for key in self._flag_list.keys():
      if not self._flag_list[key][4]:
        self.flags[key] = self._flag_list[key][2]

if __name__ == '__main__':
  print 'flags not meant to be invoked'
  sys.exit(1)
