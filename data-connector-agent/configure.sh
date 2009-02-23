#!/bin/bash
#
# Copyright 2008 Google Inc. All Rights Reserved.
# Author: rayc@google.com (Ray Colline)

# OPTIONS
# -prefix Secure Data Connector install location
# -httpd Apache2 httpd location (Must have mod_proxy).
# -opensshd OpenSSH daemon binary.

PACKAGE="google-secure-data-connector"
PREFIX="/usr/local"
ETCPREFIX=
VARPREFIX=
APACHEVERSION="2.2"
APACHECTL=
MODULESDIR=
OPENSSHD=
JAVAHOME=${JAVAHOME}  # Get it from the environment
USER=daemon
GROUP=daemon
USE_SUPPLIED_APACHE="false"
LSB="false"
NOVERIFY="false"
APACHE_20_MODULES="access proxy proxy_http mime mime_magic"
APACHE_22_MODULES="authz_host proxy proxy_http mime mime_magic"

# Save last run config options to config.status
echo $0 $* > config.status
chmod 755 config.status

# Check for getopt gnu util
[ -x "$(which getopt)" ] || { echo "gnu getopt binary not found." ; exit 1; }

# Command line arguments
OPTS=$(getopt -o h --long lsb,noverify,prefix:,etcprefix::,varprefix::,binprefix::,apachectl::,opensshd:,apache_modules_dir::,javahome::,use_supplied_apache,user::,group:: -n 'configure' -- "$@") 
if [ $? != 0 ] || [ $# = 0 ]; then 
  echo -e "\nUsage:
    --lsb) use LSB defaults no other PREFIX options are neccessary
    --prefix) binary prefix
    --etcprefix) etc prefix.  defaults to $PREFIX/etc
    --varprefix) var prefix. defaults to $PREFIX/var
    --use_supplied_apache) no other apache/ht options are needed.
    --apachectl) location of apachectl binary.
    --apache_modules_dir) location of apache modules dir.
    --opensshd) location of openssh's sshd binary.
    --user) user to run woodstock as. Default is 'daemon'
    --group) group to run woodstock as. Default is 'daemon'
    --javahome) system java location.
    --noverify) do not perform configure validation steps.
  " >&2
  exit 1 
fi

eval set -- "$OPTS"

while true; do
  case "$1" in
    --lsb) LSB="true" ; shift 1 ;;
    --noverify) NOVERIFY="true" ; shift 1 ;; 
    --use_supplied_apache) USE_SUPPLIED_APACHE="true" ; shift 1 ;;
    --prefix) PREFIX=$2 ; shift 2 ;;
    --etcprefix) ETCPREFIX=$2; shift 2 ;;
    --varprefix) VARPREFIX=$2; shift 2 ;;
    --binprefix) BINPREFIX=$2; shift 2 ;;
    --apachectl) APACHECTL=$2 ; shift 2 ;;
    --apache_modules_dir) MODULESDIR=$2 ; shift 2 ;;
    --opensshd) OPENSSHD=$2 ; shift 2 ;;
    --javahome) JAVAHOME=$2 ; shift 2 ;;
    --user) USER=$2 ; shift 2 ;;
    --group) GROUP=$2 ; shift 2 ;;
    --) shift ; break ;;
    *) echo "Error!" ; exit 1 ;;
  esac
done

#
### Argument logic - set other prefixes
#

if [ -z ${ETCPREFIX} ]; then
  ETCPREFIX=${PREFIX}/etc
fi

if [ -z ${VARPREFIX} ]; then
  VARPREFIX=${PREFIX}/var
fi

# Set LSB. 
if [ ${LSB} = "true" ]; then
  PREFIX=/opt/${PACKAGE}
  ETCPREFIX=/etc/opt/${PACKAGE}
  VARPREFIX=/var/opt/${PACKAGE}
fi

# Check supplied apache and check.
if [ ${USE_SUPPLIED_APACHE} = "true" ]; then
  APACHECTL=${PREFIX}/lib/apache/bin/apachectl
  APACHEMODULES=""  # we dont use modules in the supplied apache.
fi

# Infer java binary location from JAVA_HOME env, JAVAHOME env 
# or --javabin setting.
if [ ${JAVA_HOME} ]; then
  JAVABIN=${JAVA_HOME}/bin/java
elif [ ${JAVAHOME} ]; then
  JAVABIN=${JAVAHOME}/bin/java
else # Try to figure it out.
  JAVABIN=$(which java)
fi


#
### Verification Checks
#

if [ ${NOVERIFY} = "false" ]; then

  # verify opensshd
  if [ ! -x "${OPENSSHD}" ]; then
    echo "opensshd: ${OPENSSHD} not found"
    exit 1
  fi

  # verify user and group for existence
  getent=$(which getent)
  if [ ! -x "${getent}" ]; then
    echo "getent missing, cant check group and user. Assuming you entered it right"
  else
    $getent passwd $USER 2>&1 > /dev/null
    if [ $? != 0 ]; then
      echo "user $USER does not exist"
      exit 1
    fi

    $getent group $GROUP 2>&1 > /dev/null
    if [ $? != 0 ]; then
      echo "group $GROUP does not exist"
      exit 1
    fi
  fi

  # verify non supplied apache.
  if [ ${USE_SUPPLIED_APACHE} = "false" ]; then

    # Check version
    echo -n "checking apache version: " 
    APACHEVERSION=$(${APACHECTL} -v | grep version | awk -F/ '{print $2}' \
        | cut -c 1-3)
    if [ ${APACHEVERSION} != "2.2" -a ${APACHEVERSION} != "2.0" ]; then
      echo Apache version not correct - must be 2.0 or 2.2
      exit 1
    else 
      echo ${APACHEVERSION}
    fi

    if [ -z ${APACHECTL} ]; then
     echo "--apachectl option is missing!"
     exit 1
    fi

    if [ ! -x "${APACHECTL}" ]; then
      echo "httpd: ${APACHECTL} not found"
      exit 1
    fi

  fi

  # verify woodstock user
  HOMEDIR=~woodstock
  if [ ${HOMEDIR} = '~woodstock' ]; then
    # if the user doesnt exist, the string '~woodstock' will be present
    echo "'woodstock' user does not exist."
    echo "Create woodstock user with homedir as ${ETCPREFIX}/woodstock-user"
    echo "To create on most linux systems run:"
    echo "useradd --home-dir=${ETCPREFIX}/woodstock-user" \
        "--comment='Woodstock User' --shell=/bin/false" 
    exit 1
  elif [ ${HOMEDIR} != ${ETCPREFIX}/woodstock-user ]; then
    echo "'woodstock' home directory is incorrect."
    echo It should be \"${ETCPREFIX}/woodstock-user\"
    exit 1
  fi

  # verify java binary.
  if [ -x "${JAVABIN}" ]; then
    ${JAVABIN} -version 2>&1 | grep 'version' |grep -q '1.6'
    if [ $? != 0 ]; then
      echo "Java found at ${JAVABIN} not suitable."
      echo "Secure Data Connector requires JDK 1.6"
      exit 1
    else
      echo "Found java at ${JAVABIN}"
    fi
  else
    echo "Java could not be found at $JAVABIN"
    exit 1
  fi

fi

#
### Getting Apache Module list for config files.
#

if [ ${APACHEVERSION} = "2.2" ]; then
  APACHE_MODULES=${APACHE_22_MODULES}
else
  APACHE_MODULES=${APACHE_20_MODULES}
fi

#
# Checks MODULESDIR for both apache 2.0 and 2.2 style module names and
# echos the results (for use with eval).
#
# $1 module name.
# 
function getPathToModuleFile {

  module=$1
  if [ -e "${MODULESDIR}/mod_${module}.so" ]; then
    echo "${MODULESDIR}/mod_${module}.so"
  else 
    echo "NOTFOUND"
  fi

  return
}

if [ ${USE_SUPPLIED_APACHE} = "false" ]; then

  FOUND_MODULES=""
  for module in ${APACHE_MODULES}; do

    echo -n checking for statically compiled module: ${module}:
    ${APACHECTL} -l |grep -q ${module}
    if [ $? = 0 ]; then
      continue
    fi
    echo " no"

    echo -n checking dynamically compiled apache for: ${module}:
    modulepath=$(getPathToModuleFile ${module})
    if [ $modulepath = "NOTFOUND" ]; then
      echo " no"
      echo "Please install module \"${module}\"."
      exit 1
    fi
    echo " yes"

    FOUND_MODULES="${FOUND_MODULES} ${module}"
  done
fi

#
### Setup files
#

# Edit build.properties
template=build.properties
cp build.properties-dist ${template}
echo Generating ${template}
sed -i ${template} -e 's^__PREFIX__^'${PREFIX}'^'
sed -i ${template} -e 's^__ETCPREFIX__^'${ETCPREFIX}'^'
sed -i ${template} -e 's^__VARPREFIX__^'${VARPREFIX}'^'
sed -i ${template} -e 's^__USER__^'${USER}'^'
sed -i ${template} -e 's^__GROUP__^'${GROUP}'^'

# Edit install-sdc.sh
template="install-sdc.sh"
cp "install-sdc.sh-dist" ${template}
echo Generating ${template}
sed -i ${template} -e 's^__PREFIX__^'${PREFIX}'^'
sed -i ${template} -e 's^__ETCPREFIX__^'${ETCPREFIX}'^'
sed -i ${template} -e 's^__VARPREFIX__^'${VARPREFIX}'^'
sed -i ${template} -e 's^__USER__^'${USER}'^'
sed -i ${template} -e 's^__GROUP__^'${GROUP}'^'
chmod 755 $template

# Edit build.xml
template=build.xml
cp build.xml-dist ${template}
echo Generating ${template}
if [ ${USE_SUPPLIED_APACHE} = "true" ]; then
  sed -i ${template} -e 's^__BUILDHTTPD__^,compile-httpd^'
  sed -i ${template} -e 's^__INSTALLHTTPD__^,install-httpd^'
  sed -i ${template} -e 's^__CLEANHTTPD__^clean-httpd^'
  sed -i ${template} -e 's^__DISTCLEANHTTPD__^,dist-clean-httpd^'
else 
  sed -i ${template} -e 's^__BUILDHTTPD__^^'
  sed -i ${template} -e 's^__INSTALLHTTPD__^^'
  sed -i ${template} -e 's^__CLEANHTTPD__^^'
  sed -i ${template} -e 's^__DISTCLEANHTTPD__^^'
fi

# Create resourceRules.xml since we don't need to edit this file.
cp config/resourceRules.xml-dist config/resourceRules.xml

# Edit httpd.conf-dist
template=config/apache/httpd.conf-template
cp config/apache/httpd.conf-dist ${template}
echo Generating ${template}

if [ ! -z "${FOUND_MODULES}" ]; 
then
  echo ${FOUND_MODULES}
  for module in ${FOUND_MODULES}; do  # Add modules to template
    echo Configuring load for ${module}
    moduledir=$(getPathToModuleFile ${module})
    echo "LoadModule ${module}_module ${moduledir}" >> ${template}
  done
fi

sed -i ${template} -e 's^_APACHE_ROOT_^'${ETCPREFIX}'/apache^'
sed -i ${template} -e 's^_APACHE_LOG_DIR_^'${VARPREFIX}'/log^'
sed -i ${template} -e 's^_USER_^'${USER}'^'
sed -i ${template} -e 's^_GROUP_^'${GROUP}'^'

# Edit localConf.xml-dist
template=config/localConfig.xml
cp config/localConfig.xml-dist ${template}
echo Generating ${template}
sed -i ${template} -e 's^_APACHE_CTL_^'${APACHECTL}'^'
sed -i ${template} -e 's^_APACHE_CONF_DIR_^'${ETCPREFIX}'/apache^'
sed -i ${template} -e 's^_START_SSHD_^'${ETCPREFIX}'/openssh/start_sshd.sh^'

# Openssh start_ssh.sh-dist
template=config/openssh/start_sshd.sh
cp config/openssh/start_sshd.sh-dist ${template}
echo Generating ${template}
sed -i ${template} -e 's^_SSHD_^'${OPENSSHD}'^'
sed -i ${template} -e 's^_OPENSSHCONF_^'${ETCPREFIX}'/openssh^'

# Openssh sshd_config-dist.
template=config/openssh/sshd_config
cp config/openssh/sshd_config-dist ${template}
echo Generating ${template}
sed -i ${template} -e 's^_OPENSSHCONF_^'${ETCPREFIX}'/openssh^'

# Runclient
template=runclient.sh
cp runclient.sh-dist runclient.sh
echo Generating ${template}
sed -i ${template} -e 's^_PREFIX_^'${PREFIX}'^'
sed -i ${template} -e 's^_ETCPREFIX_^'${ETCPREFIX}'^'
sed -i ${template} -e 's^_JAVABIN_^'${JAVABIN}'^'
sed -i ${template} -e 's^_USER_^'${USER}'^'
sed -i ${template} -e 's^_GROUP_^'${GROUP}'^'

# Supplied Apache build.xml
if [ ${USE_SUPPLIED_APACHE} = "true" ]; then
  template=third-party/apache-httpd/build.xml
  cp third-party/apache-httpd/build.xml-dist ${template}
  echo Generating ${template}
  sed -i ${template} -e 's^_APACHE_ROOT_^'${PREFIX}'/lib/apache^'
fi


