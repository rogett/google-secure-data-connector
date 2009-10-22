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

set -e  # bail out on any errors
set -x

. configuration.sh

TIMESTAMP=$(date +'%Y%m%d_%H%M%S')

# Check root.
if [ $(id -u) != 0 ]; then
  echo install must be run as user root.
  exit 1
fi

# Create directories
for dir in \
    "${PREFIX}" \
    "${LIBDIR}" \
    "${BINDIR}" \
    "${SYSCONFDIR}" \
    "${LOCALSTATEDIR}" \
    "${LOCALSTATEDIR}/log" \
    "${SYSV_INIT_SCRIPT_DIRECTORY}"
do echo making dir ${dir}
  mkdir -p ${dir}
done

# binary
install -m 0755 -d "${BINDIR}"

install -m 0755 -t "${BINDIR}" runclient.sh
install -m 0755 -t "${BINDIR}" start.sh
install -m 0755 -t "${BINDIR}" stop.sh

# lib
for jar in "agent.jar" "protocol-generated.jar"; do
  echo installing ${jar}
  install -m 0644 -t "${LIBDIR}" build/${jar}
done

THIRD_PARTY_FILES=$(find third-party/ -type f | grep -v .svn)
for file in ${THIRD_PARTY_FILES}; do
  echo installing ${file}
  install -m 644 -D ${file} ${LIBDIR}/${file}
done


# backup old config files
if [ -e ${SYSCONFDIR}/localConfig.xml ]; then
  echo backing up old localConfig.xml file
  install -g ${GROUP} -o root -m 640 ${SYSCONFDIR}/localConfig.xml \
      ${SYSCONFDIR}/localConfig.xml-existing-${TIMESTAMP}
fi

if [ -e ${SYSCONFDIR}/resourceRules.xml ]; then
  echo backing up old resourceRules.xml file
  install -g ${GROUP} -o root -m 640 ${SYSCONFDIR}/resourceRules.xml \
      ${SYSCONFDIR}/resourceRules.xml-existing-${TIMESTAMP}
fi

# localConfig.xml
install -g ${GROUP} -o ${USER} -m 640 -t ${SYSCONFDIR} ./config/localConfig.xml

# resourceRules.xml-dist
install -g ${GROUP} -o ${USER} -m 640 -t ${SYSCONFDIR} \
  ./config/resourceRules.xml-dist

# log4j.properties
file="${SYSCONFDIR}/log4j.properties"
echo installing ${file}
install -g ${GROUP} -o ${USER} -m 640 -t ${SYSCONFDIR} ./config/log4j.properties

install -o root -m 0755 initscript "${INITSCRIPT}"
