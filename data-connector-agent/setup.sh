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

# Setup Configuration 
OPENSSH_HOME=`pwd`/third-party/openssh
CONF_DIR=`pwd`/config  # Absolute path required
LOG_DIR=`pwd`/logs  # Absolute path required

LOCAL_CONF_FILE=$CONF_DIR/localConfig.xml
APACHE_ROOT=`pwd`/third-party/apache-httpd/root #  Absolute path required
APACHE_HTPASSWD=`pwd`/third-party/apache-httpd/root/bin/htpasswd #  Absolute path required
APACHE_CTL=`pwd`/third-party/apache-httpd/root/bin/apachectl #  Absolute path required
APACHE_CONF_DIST_FILE=$CONF_DIR/httpd.conf-dist
APACHE_CONF_DIR=$CONF_DIR/apache # Absolute path required
APACHE_CONF_GEN_FILENAME=$APACHE_CONF_DIR/httpd.conf-template

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

  # Update config/localConfig.xml sshd path  
  if [ -e $LOCAL_CONF_FILE ]; then
    echo $LOCAL_CONF_FILE file already in place, skipping.
  else 
    /bin/cp $LOCAL_CONF_FILE-dist $LOCAL_CONF_FILE
    sed -i $LOCAL_CONF_FILE -e  's^_SSHD_^'$OPENSSH_HOME'/bin/start_sshd.sh^'	
    sed -i $LOCAL_CONF_FILE -e  's^_APACHE_HTPASSWD_^'$APACHE_HTPASSWD'^'	
    sed -i $LOCAL_CONF_FILE -e  's^_APACHE_CTL_^'$APACHE_CTL'^'	
    sed -i $LOCAL_CONF_FILE -e  's^_APACHE_CONF_DIR^'$APACHE_CONF_DIR'^'	
  fi

  # Update http conf template
  /bin/cp $APACHE_CONF_DIST_FILE $APACHE_CONF_GEN_FILENAME
  sed -i $APACHE_CONF_GEN_FILENAME -e 's^_APACHE_ROOT_^'$APACHE_ROOT'^'
  sed -i $APACHE_CONF_GEN_FILENAME -e 's^_APACHE_CONF_^'$APACHE_CONF_DIR'^'
  sed -i $APACHE_CONF_GEN_FILENAME -e 's^_APACHE_LOG_DIR_^'$LOG_DIR'^'

  # Check Apache HTTP suitability
  if [ -e $APACHE_ROOT/bin/httpd ]; then
    echo -n "Checking apache version: "
    $APACHE_ROOT/bin/httpd -v |grep version |grep 2.2
    if [ $? != 0 ]; then
      echo not found
      echo "Secure Data Connector requires apache 2.2"
      exit 1
    fi
    echo success.
    echo -n "Checking for mod_proxy: "
    $APACHE_ROOT/bin/httpd -l |grep proxy
    static_link_result=$?
    ls $APACHE_ROOT/modules |grep proxy
    dynamic_link_result=$?
    if [ $static_link_result != 0 -a $dynamic_link_result != 0 ]; then
      echo not found
      echo "Secure Data Connector requires mod_proxy."
      exit 1
    fi
    echo success.
  else 
    echo Did not find httpd at $APACHE_ROOT/bin/httpd
    exit 1
  fi

fi

if [ $installmode = "cygwin" ] ; then
  check_for_user_cygwin

	if [ $? != 0 ]; then
	   echo "local user not found in /etc/passwd."
	   echo "please create the windows user account and run mkpasswd -l >/etc/passwd"
	else
	 echo "local user woodstock found in /etc/passwd"
	 # Fix user home directory path
	 sed -i /etc/passwd -e  's^:/home/woodstock:^:'$OPENSSH_HOME'/home/woodstock:^'
	fi

  # Update config/localConfig.xml sshd path for cygwin execution	
  if [ -e $LOCAL_CONF_FILE ]; then
    echo $LOCAL_CONF_FILE file already in place, skipping.
  else 
    /bin/cp config/localConfig.xml-dist config/localConfig.xml
    sed -i config/localConfig.xml -e  's^_SSHD_.*$^_SSHD_c:\\\\cygwin\\\\bin\\\\bash.exe --login -i -c  "'$OPENSSH_HOME'/bin/start_sshd.sh"^'
  fi
fi


# Create folders for apache
mkdir -p $CONF_DIR/apache/htdocs
mkdir logs

#SETUP SSHD RUNTIME
mkdir $OPENSSH_HOME/bin
sed -e "s:_WSCLIENT_HOME_:$PWD:g" $OPENSSH_HOME/dist/sshd_config.tmpl > $OPENSSH_HOME/etc/sshd_config
sed -e "s:_WSCLIENT_HOME_:$PWD:g" $OPENSSH_HOME/dist/start_sshd.sh.tmpl > $OPENSSH_HOME/bin/start_sshd.sh
chmod 755 $OPENSSH_HOME/bin/start_sshd.sh
chmod 600 $OPENSSH_HOME/etc/ssh_host_dsa_key
chmod 600 $OPENSSH_HOME/etc/ssh_host_rsa_key
chmod 644 $OPENSSH_HOME/home/woodstock/.ssh/authorized_keys
chmod 755 start.sh
chmod 755 stop.sh
chmod 755 runclient.sh
