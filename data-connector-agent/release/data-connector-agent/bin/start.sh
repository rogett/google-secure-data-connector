#!/bin/bash
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
#
# Starts runclient.sh in background and log stdout and stderr to file.
#
# $Id: start.sh-dist 447 2009-08-14 01:39:08Z matt.proud $

# For local modifications, please set these variables here.
# OVERRIDE_BINDIR=
# OVERRIDE_LOGDIR=
# OVERRIDE_RUNDIR=

# These variables are determined by configure.sh.
BINDIR=${OVERRIDE_BINDIR:-$(pwd)/bin}
RUNDIR=${OVERRIDE_RUNDIR:-$(pwd)/}
LOGDIR=${OVERRIDE_LOGDIR:-$(pwd)/log}


# Arguments:
# - $1 - The variable name that holds the directory to be made.
create_directory_if_absent() {
  directory="${!1}"

  if [ ! -d "${directory}" ]; then
    mkdir -p "${directory}"
  fi
}

create_directory_if_absent LOGDIR
create_directory_if_absent RUNDIR

pidfile="${RUNDIR}/agent.pid"

if [ -f "${pidfile}" ]; then
  other_pid=$(< "${pidfile}")
  if ps -p ${other_pid} -o comm,args 2>/dev/null | tail -1 | grep -q runagent.sh; then
    echo "Another agent is running at PID ${other_pid}."
    exit 1
  fi
fi

# Ensure that no race condition shenanigans have occurred.
rm -f "${pidfile}"

runner_pid=$("${BINDIR}/runagent.sh" "${@}" >>"${LOGDIR}/agent.log" 2>&1 & echo $!)

echo ${runner_pid} > "${pidfile}"
echo -e "Agent running PID=${runner_pid}. Please review log \"${LOGDIR}/agent.log\" for details." >&2


