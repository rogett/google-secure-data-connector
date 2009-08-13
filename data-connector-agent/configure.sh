#!/bin/bash
#
# Author: rayc@google.com (Ray Colline)
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
# $Id$

VENDOR="google"
PACKAGE="secure-data-connector"
VERSION=1.1
FULL_PACKAGE="${VENDOR}-${PACKAGE}"
FULLNAME="${VENDOR}-${PACKAGE}-${VERSION}"

# Instructions for managing ${RELEASE}:
# - If ${VERSION} changes, reset this to zero.
# - If something in the packaging or build infrastructure changes, please
#   increment this value.
RELEASE=2

# Install into locations ascertained by ${PREFIX} only.
BY_PREFIX_ONLY="false"


# The pace where the runtime binaries for the package are to be installed.
BINDIR=

# Used to hardcode the initscript path into a few targets.
INITSCRIPT=
# Get it from the environment
JAVAHOME=${JAVAHOME}
# The place where vendor and third-party JARs are to be installed.
LIBDIR=
# The place where local state information---e.g., data spools or generated
# information---should reside.  This defaults to /var, since according to the
# Linux FHS it is to be writable at all times, unlike /opt which makes no
# guarantees---hence the why this is not assumed from ${PREFIX} by default.
LOCALSTATEDIR="/var/opt/${VENDOR}/${PACKAGE}/${VERSION}/lib"
# See definition for ${LOCALSTATEDIR}.
LOGDIR="/var/opt/${VENDOR}/${PACKAGE}/${VERSION}/log"
MODULESDIR=
PREFIX="/opt/${VENDOR}/${PACKAGE}/${VERSION}"
PROTOC=$(which protoc)
RUNDIR="/var/opt/${VENDOR}/${PACKAGE}/${VERSION}/run"
SYSCONFDIR="/etc/opt/${VENDOR}/${PACKAGE}/${VERSION}"
if [ -d "/etc/rc.d" ]; then
  # Who uses rc.d anymore?
  SYSV_INIT_SCRIPT_DIRECTORY=/etc/rc.d
else
  SYSV_INIT_SCRIPT_DIRECTORY=/etc/init.d
fi

USER=daemon
GROUP=daemon

NOVERIFY="false"

CONFIGURATION_SHLIB="configuration.sh"

SUFFIX_VERSION_TO_DEBIAN_PACKAGE_NAME="false"
DEBIAN_PACKAGE_NAME="${VENDOR}-${PACKAGE}"

# Save last run config options to config.status
echo $(pwd)/configure.sh "${*}" > config.status
chmod 755 config.status

# Check for getopt gnu util
[ -x "$(which getopt)" ] || { echo "gnu getopt binary not found." ; exit 1; }

# Command line arguments
OPTS=$(getopt -o h --long noverify,by_prefix_only,suffix_version_to_debian_package_name,prefix:,sysconfdir::,localstatedir::,bindir::,protoc::,javahome::,user::,group::,libdir::,bindir:: -n 'configure' -- "$@")
if [ $? != 0 ]; then
  echo -e "\nUsage:
    --prefix) installation prefix.
    --by_prefix_only) Use for all paths to be derived from --prefix.
    --sysconfdir) etc prefix.  defaults to ${SYSCONFDIR}
    --localstatedir) var prefix. defaults to ${LOCALSTATEDIR}
    --protoc) location of protocol buffer compiler.
    --user) user to run SDC as. Default is 'daemon'
    --group) group to run SDC as. Default is 'daemon'
    --javahome) system java location.
    --noverify) do not perform configure validation steps.
    --suffix_version_to_debian_package_name) Append the version to the Debian
                                             package name to prevent namespace
                                             collisions.

    See README for more information.
  " >&2
  exit 1
fi

eval set -- "${OPTS}"

while true; do
  case "${1}" in
    --by_prefix_only) BY_PREFIX_ONLY="true" ; shift 1 ;;
    --noverify) NOVERIFY="true" ; shift 1 ;;
    --prefix) PREFIX=$2 ; shift 2 ;;
    --sysconfdir) SYSCONFDIR=$2; shift 2 ;;
    --localstatedir) LOCALSTATEDIR=$2; shift 2 ;;
    --bindir) BINDIR=$2; shift 2 ;;
    --protoc) PROTOC=$2 ; shift 2 ;;
    --javahome) JAVAHOME=$2 ; shift 2 ;;
    --user) USER=$2 ; shift 2 ;;
    --group) GROUP=$2 ; shift 2 ;;
    --libdir) LIBDIR=$2 ; shift 2 ;;
    --suffix_version_to_debian_package_name)
      SUFFIX_VERSION_TO_DEBIAN_PACKAGE_NAME="true"
      shift 1
      ;;
    --) shift ; break ;;
    *) echo "Error!" ; exit 1 ;;
  esac
done

#
### Argument logic - set other prefixes
#

if [ -z "${PREFIX}" ]; then
  echo '${PREFIX} is undefined; you probably do not want this!' >&2
  echo "This may be useful for rootless installs that run from a local" >&2
  echo "directory.  It shall be set to './'" >&2

  sleep 5
fi

# If instructed, use the ${PREFIX} if the desired variable is undefined
# or if instructed to forcibly use ${PREFIX}.
#
# Arguments:
# - $1 --- Variable name.
# - $2 --- Default suffix.
set_by_prefix_if_undefined() {
  if [ "${BY_PREFIX_ONLY}" = "true" ] || [ -z "${!1}" ]; then
    if [ -n "${PREFIX}" ]; then
      eval "${1}=${PREFIX}/${2}"
    else
      eval "${1}=./${2}"
    fi
  fi
}

# Override if the user wants the PREFIX to govern.
set_by_prefix_if_undefined "SYSV_INIT_SCRIPT_DIRECTORY" "etc/init.d"

INITSCRIPT="${SYSV_INIT_SCRIPT_DIRECTORY}/${FULLNAME}"

set_by_prefix_if_undefined "SYSCONFDIR" "etc"
set_by_prefix_if_undefined "LOCALSTATEDIR" "var"
set_by_prefix_if_undefined "LIBDIR" "lib"
set_by_prefix_if_undefined "BINDIR" "bin"
set_by_prefix_if_undefined "RUNDIR" "var/run"
set_by_prefix_if_undefined "LOGDIR" "var/log"

# Since by default the package installs files into locations partitioned by
# package version, it makes sense to enable packages that do not conflict with
# one another to exist.
if [ "${SUFFIX_VERSION_TO_DEBIAN_PACKAGE_NAME}" = "true" ]; then
  DEBIAN_PACKAGE_NAME="${DEBIAN_PACKAGE_NAME}${VERSION}"
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

# look for protoc
if [ ! -x "${PROTOC}" ]; then
  echo "You do not have protoc installed, however, we ship pregenerated sources"
  echo "but you will not be able to edit any .proto files"
  PROTOC="$(pwd)/src/no-op-protoc.sh"
else
  echo "Found protoc in your path, using that: ${PROTOC}."
fi

#
### Verification Checks
#

if [ ${NOVERIFY} = "false" ]; then

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

    # verify user account is enabled
    getent passwd $USER | grep bin/false
    if [ $? = 0 ]; then
      echo "User ${USER} is not enabled. Either enable the user or specify a different user account."
      exit 1
    fi

    getent passwd ${USER} |grep ${USER} | grep bin/nologin
    if [ $? = 0 ]; then
      echo "User ${USER} is not enabled. Either enable the user or specify a different user account."
      exit 1
    fi
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
### Setup files
#

echo "Generating configuration shlib..."

if [ -f "${CONFIGURATION_SHLIB}" ]; then
  rm "${CONFIGURATION_SHLIB}"
fi

echo "# This file is auto-generated and will be deleted when " >> "${CONFIGURATION_SHLIB}"
echo "# configure.sh is invoked."  >> "${CONFIGURATION_SHLIB}"

# Arguments:
# $1 --- The variable name to dump.
dump_variable_to_shlib() {
  # http://tldp.org/LDP/abs/html/bashver2.html#EX78
  # x=y
  # y=1
  # echo ${!x}
  # => 1
  echo "${1}=${!1}" >> "${CONFIGURATION_SHLIB}"
}

dump_variable_to_shlib BINDIR
dump_variable_to_shlib DEBIAN_PACKAGE_NAME
dump_variable_to_shlib FULLNAME
dump_variable_to_shlib FULL_PACKAGE
dump_variable_to_shlib GROUP
dump_variable_to_shlib INITSCRIPT
dump_variable_to_shlib JAVABIN
dump_variable_to_shlib LIBDIR
dump_variable_to_shlib LOCALSTATEDIR
dump_variable_to_shlib LOGDIR
dump_variable_to_shlib PACKAGE
dump_variable_to_shlib PREFIX
dump_variable_to_shlib PROTOC
dump_variable_to_shlib RELEASE
dump_variable_to_shlib RUNDIR
dump_variable_to_shlib SYSCONFDIR
dump_variable_to_shlib SYSV_INIT_SCRIPT_DIRECTORY
dump_variable_to_shlib USER
dump_variable_to_shlib VERSION

# Arguments:
# $1 --- The file to edit.
# $2 --- The variable to dump.
substitute_macro() {
  sed -i "${1}" -e 's^__'${2}'__^'${!2}'^'
}

# Arguments:
# $1 --- File to edit.
rewrite_template() {
  cp "${1}-dist" "${1}"

  substitute_macro "${1}" BINDIR
  substitute_macro "${1}" DEBIAN_PACKAGE_NAME
  substitute_macro "${1}" FULLNAME
  substitute_macro "${1}" FULL_PACKAGE
  substitute_macro "${1}" GROUP
  substitute_macro "${1}" INITSCRIPT
  substitute_macro "${1}" JAVABIN
  substitute_macro "${1}" LIBDIR
  substitute_macro "${1}" LOCALSTATEDIR
  substitute_macro "${1}" LOGDIR
  substitute_macro "${1}" PACKAGE
  substitute_macro "${1}" PREFIX
  substitute_macro "${1}" PROTOC
  substitute_macro "${1}" RELEASE
  substitute_macro "${1}" RUNDIR
  substitute_macro "${1}" SYSCONFDIR
  substitute_macro "${1}" SYSV_INIT_SCRIPT_DIRECTORY
  substitute_macro "${1}" USER
  substitute_macro "${1}" VERSION
}

rewrite_template "build.properties"
rewrite_template "build.xml"
rewrite_template "distribute/build.xml"
rewrite_template "distribute/debian/changelog.Debian"
rewrite_template "distribute/debian/securedataconnector.postinst"
rewrite_template "distribute/debian/securedataconnector.postrm"
rewrite_template "initscript"
rewrite_template "runclient.sh"
rewrite_template "start.sh"
rewrite_template "stop.sh"

cp "initscript" "distribute/debian/${FULLNAME}"

# Create resourceRules.xml since we don't need to edit this file.
cp config/resourceRules.xml{-dist,}

# Edit localConf.xml-dist
cp config/localConfig.xml{-dist,}

echo "Done!"

exit 0
