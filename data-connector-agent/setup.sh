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
# Configures distribution for invocation on this machine.  If run as root it
# will add a user to the /etc/passwd.  If run as a normal user it will print
# out the line that needs to be appended to /etc/passwd.  Additionally, this
# script sets proper permissions on openssh files and sets config file path
# locations anchored to the cwd of this script.

# If running under Cygwin used sshd.exe
uname -a |grep -i cygwin >/dev/null
 if [ $? = 0 ]; then
   installmode=cygwin
  else
   installmode=linux
 fi

# Get next available UID after 1000.
return_highest_id() {
  HIGHEST_ID=0
  IDS=`cat $1 |awk -F: '{print $3}'` 
  for i in $IDS; do
    if [[ $i -gt $HIGHEST_ID ]]; then 
      if [[ $i -lt "60000" ]]; then
        HIGHEST_ID=$i
      fi
    fi
  done
  # force UIDs to be above 100 as these are reserved for system accounts. 
  if [[ $HIGHEST_ID -lt "100" ]]; then
    HIGHEST_ID=100
  fi

  return $HIGHEST_ID
}

# See if we already have a woodstock user.
check_for_user() {
  grep "woodstock" /etc/passwd
  return $?
}

check_for_user_cygwin() {
  grep "woodstock:unused_by_nt" /etc/passwd
  return $?
}

# Verify we have been run from our distribution directory.
check_invocation_location() {
  found=0
  for file in *; do
    if [ "$file" == "setup.sh" ]; then
      let found=$found+1
    elif [ "$file" == "runclient.sh" ]; then
      let found=$found+1
    elif [ "$file" == "third-party" ]; then
      let found=$found+1
    fi
  done

  if [ $found != 3 ]; then
    return 0
  else 
    return 1
  fi
}
# main

OPENSSH_HOME=`pwd`/third-party/openssh

check_invocation_location
if [ $? == 0 ]; then
  echo please run from directory where setup.sh is located
  exit 1
fi

if [ $installmode = "linux" ] ; then
  # GET LINUX PW LINE FOR WOODSTOCK USER
  return_highest_id "/etc/passwd"
  let HI_UID=$?+1
  return_highest_id "/etc/group"
  let HI_GID=$?+1
  echo $HI_GID
  LINUX_PWLINE="woodstock:$HI_UID:$HI_GID:100::$OPENSSH_HOME/home/woodstock:/bin/false"
  
  check_for_user
	if [ $? != 0 ]; then
	 if [ $UID != 0 ]; then
	   echo not run as root, please add this line to /etc/passwd
	   echo $LINUX_PWLINE
	 else
	   echo adding user woodstock to password file
	   echo $LINUX_PWLINE >> /etc/passwd
	 fi
	else
	 echo user woodstock already defined in /etc/passwd
	fi
	
    sed -i rules.properties -e  's^sshd=.*$^sshd='$OPENSSH_HOME'/bin/start_sshd.sh'	
	
fi

if [ $installmode = "cygwin" ] ; then
  check_for_user_cygwin
	if [ $? != 0 ]; then
	   echo "local user not found in /etc/passwd."
	   echo "please create the windows user account and run mkpasswd -l >/etc/passwd"
	  
	else
	 echo "local user woodstock found in /etc/passwd"

	sed -i /etc/passwd -e  's^:/home/woodstock:^:'$OPENSSH_HOME'/home/woodstock:^'

	fi
	
	sed -i rules.properties -e  's^sshd=.*$^sshd=c:\\\\cygwin\\\\bin\\\\bash.exe --login -i -c  "'$OPENSSH_HOME'/bin/start_sshd.sh"^'
	
fi

#SETUP SSHD RUNTIME
mkdir $OPENSSH_HOME/bin
sed -e "s:_WSCLIENT_HOME_:$PWD:g" $OPENSSH_HOME/dist/sshd_config.tmpl > $OPENSSH_HOME/etc/sshd_config
sed -e "s:_WSCLIENT_HOME_:$PWD:g" $OPENSSH_HOME/dist/start_sshd.sh.tmpl > $OPENSSH_HOME/bin/start_sshd.sh
chmod 755 $OPENSSH_HOME/bin/start_sshd.sh
chmod 600 $OPENSSH_HOME/etc/ssh_host_dsa_key
chmod 600 $OPENSSH_HOME/etc/ssh_host_rsa_key
chmod 755 start.sh
chmod 755 stop.sh
chmod 755 runclient.sh
