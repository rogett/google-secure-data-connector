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

"""One-line documentation for rpm_env module.

A detailed description of rpm_env.
"""

__author__ = 'kaeli@google.com (Kaeli Chambers)'

import flags
import os

FLAGS = flags.Flags()

FLAGS.set_string('clean', None , 'clean build directory')
FLAGS.process_flags()

def setup_env():
  os.makedirs('./rpm/BUILD')
  os.makedirs('./rpm/RPMS/athlon')
  os.makedirs('./rpm/RPMS/i386')
  os.makedirs('./rpm/RPMS/i486')
  os.makedirs('./rpm/RPMS/i586')
  os.makedirs('./rpm/RPMS/i686')
  os.makedirs('./rpm/RPMS/noarch')
  os.makedirs('./rpm/SOURCES')
  os.makedirs('./rpm/SPECS')
  os.makedirs('./rpm/SRPMS')
  os.makedirs('./rpm/noarch')

def delete_env():
  os.removedirs('./rpm/BUILD')
  os.removedirs('./rpm/RPMS/athlon')
  os.removedirs('./rpm/RPMS/i386')
  os.removedirs('./rpm/RPMS/i486')
  os.removedirs('./rpm/RPMS/i586')
  os.removedirs('./rpm/RPMS/i686')
  os.removedirs('./rpm/RPMS/noarch')
  os.removedirs('./rpm/SRPMS')
  os.removedirs('./rpm/noarch')


def main():
  if FLAGS.flags['clean']:
    delete_env()
  else:
    setup_env()


if __name__ == '__main__':
  main()
