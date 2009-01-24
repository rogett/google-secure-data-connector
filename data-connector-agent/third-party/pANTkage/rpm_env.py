#!/usr/bin/python
#
# * Copyright 2008 Google Inc.
# *
# * This program is free software; you can redistribute it and/or
# * modify it under the terms of the GNU General Public License
# * as published by the Free Software Foundation; either version 2
# * of the License, or (at your option) any later version.
# *
# * This program is distributed in the hope that it will be useful,
# * but WITHOUT ANY WARRANTY; without even the implied warranty of
# * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# * GNU General Public License for more details.
# *
# * You should have received a copy of the GNU General Public License
# * along with this program; if not, write to the Free Software
# * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
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

def delete_env():
  os.removedirs('./rpm/BUILD')
  os.removedirs('./rpm/RPMS/athlon')
  os.removedirs('./rpm/RPMS/i386')
  os.removedirs('./rpm/RPMS/i486')
  os.removedirs('./rpm/RPMS/i586')
  os.removedirs('./rpm/RPMS/i686')
  os.removedirs('./rpm/RPMS/noarch')
  os.removedirs('./rpm/SRPMS')


def main():
  if FLAGS.flags['clean']:
    delete_env()
  else:
    setup_env()


if __name__ == '__main__':
  main()
