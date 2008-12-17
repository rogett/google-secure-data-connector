#!/bin/bash
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
# Starts runclient.sh in background and log stdout and stderr to file.

if [ $UID != 0 ] ; then
 echo You should run this script as root or via sudo
else

 # Kill launcher before stopping java process so it is not restarted
   ps -ef |grep -v grep | grep runclient.sh >/dev/null
 
   if [ $? = 0 ] ; then
    echo 'Stopping Secure Data Connector Launcher'
    ps -ef |grep -v grep | grep runclient.sh \
    | awk '{ print $2 }' | xargs kill
   fi

  # Check for running java process and if found kill it.
   ps -ef |grep -v grep | grep .*java.*localConfig.xml >/dev/null
 
   if [ $? = 0 ] ; then
    echo 'Stopping Secure Data Connector'
    ps -ef |grep -v grep | grep .*java.*localConfig.xml \
    | awk '{ print $2 }' | xargs kill 
   fi

fi 
