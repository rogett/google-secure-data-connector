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




"""Interctive Spec file generator 

A script to aid in the generation of a spec file.  It prompts user for need
information, and then builds the rest on it's own.
"""

__author__ = 'kaeli@google.com (Kaeli Chambers)'

import flags

FLAGS=flags.Flags()
FLAGS.set_string('summary', None, 'short summary')
FLAGS.set_string('name', None, 'project name')
FLAGS.set_string('version', None, 'version number')
FLAGS.set_string('release', None, 'release number')
FLAGS.set_string('license', None, 'license type')
FLAGS.set_string('description', None, 'description of the package')
FLAGS.set_string('sourceloc', None, 'location of source tar')
FLAGS.set_string('group', None, 'package group')
FLAGS.set_string('type', 'bin', 'src or bin package')
FLAGS.set_string('buildarch', 'noarch', 'build architecture')
FLAGS.set_string('deps', None, 'rpm dependencies')
FLAGS.process_flags()

def get_Template():
  spec_Template = open("third-party/pANTkage/spec_template", 'r')
  template = spec_Template.read()
  spec_Template.close()
  return template
  

def make_Spec(template):
  specFile = str(FLAGS.flags['name']) + ".spec"
  saveSpec = open(specFile, 'w+')
  saveSpec.write("Summary: " + str(FLAGS.flags['summary']) + '\n')
  saveSpec.write("Name: " + str(FLAGS.flags['name']) + '\n')
  saveSpec.write("Version: " + str(FLAGS.flags['version']) + '\n')
  saveSpec.write("Release: " + str(FLAGS.flags['release']) + '\n')
  saveSpec.write("Source0: %{name}-%{version}-%{release}-" + str(FLAGS.flags['type']) + ".tar.gz \n")
  saveSpec.write("License: " + str(FLAGS.flags['license']) + '\n')
  saveSpec.write("Group: " + str(FLAGS.flags['group']) + '\n')
  if str(FLAGS.flags['deps']) != 'None':
    saveSpec.write('Requires: ' +str(FLAGS.flags['deps']) + '\n')
  saveSpec.write("BuildArch: " +str(FLAGS.flags['buildarch']) + '\n')
  saveSpec.write("BuildRoot: %_topdir/BUILD/%{name}-root" + '\n\n')
  saveSpec.write("%description" + '\n' + str(FLAGS.flags['description']) + '\n\n')
  #saveSpec.write("%prep \n")
  saveSpec.write(template + '\n')
  saveSpec.close()

def main():
  template = get_Template()
  make_Spec(template)


if __name__ == '__main__':
  main()
