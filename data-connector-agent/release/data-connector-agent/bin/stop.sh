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
# Stops secure-data-connector running in the background.
#
# $Id: stop.sh-dist 446 2009-08-13 22:34:06Z matt.proud $

# For local modifications, please set these variables here.
# OVERRIDE_RUNDIR=

# These variables are determined by configure.sh.
RUNDIR=${OVERRIDE_RUNDIR:-$(pwd)}

runfile="${RUNDIR}/agent.pid"

if [ ! -f "${runfile}" ]; then
  echo "The agent is not running right now." >&2
  exit 1
fi

runner_pid=$(< "${runfile}")

echo "Agent running at pid = ${runner_pid}"

if ps -p ${runner_pid} -o comm,args | tail -1 | grep -q runagent.sh; then
  java_pid=$(ps --ppid "${runner_pid}" -o pid | tail -1)
  
  for i in $(seq 1 3); do
    echo "Stopping parent process ${runner_pid}"
    kill -TERM ${runner_pid} 2>/dev/null
    echo "Stopping java process ${java_pid}"
    kill -TERM ${java_pid} 2>/dev/null
    sleep 2
  done
  
  kill -KILL ${runner_pid} 2>/dev/null
  kill -KILL ${java_pid} 2>/dev/null
fi

rm "${runfile}"
