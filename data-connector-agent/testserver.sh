#!/bin/bash
#
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
# Runs client with appropriate class path.

WOODSTOCK_HOME=`pwd`

export PATH=$JAVA_HOME/bin/:$PATH 
export CLASSPATH=\
$WOODSTOCK_HOME/third-party/jsocks/bin/jsocks-server.jar:\
$WOODSTOCK_HOME/third-party//wpg-proxy/bin/wpgproxy.jar:\
$WOODSTOCK_HOME/third-party/jline/jline-0.9.91.jar:\
$WOODSTOCK_HOME/third-party/commons-io/commons-io-1.3.2.jar:\
$WOODSTOCK_HOME/third-party/jsch/jsch-20071024.jar:\
$WOODSTOCK_HOME/third-party/apache-log4j/log4j-1.2.15.jar:\
$WOODSTOCK_HOME/third-party/commons-logging/commons-logging-1.1.jar:\
$WOODSTOCK_HOME/third-party/commons-cli/commons-cli-1.1.jar:\
$WOODSTOCK_HOME/third-party/aopalliance/aopalliance.jar:\
$WOODSTOCK_HOME/third-party/guice-snapshot20081016/guice-assistedinject-snapshot20081016.jar:\
$WOODSTOCK_HOME/third-party/guice-snapshot20081016/guice-snapshot20081016.jar:\
$WOODSTOCK_HOME/third-party/json/json.jar:\
$WOODSTOCK_HOME/third-party/oauth/core.jar:\
$WOODSTOCK_HOME/third-party/oauth/commons-codec-1.3.jar:\
$WOODSTOCK_HOME/third-party/google-feedserver/commons-beanutils-1.8.0.jar:\
$WOODSTOCK_HOME/third-party/google-feedserver/commons-beanutils-core-1.8.0.jar:\
$WOODSTOCK_HOME/third-party/google-feedserver/gdata-client-1.0.jar:\
$WOODSTOCK_HOME/third-party/google-feedserver/gdata-core-1.0.jar:\
$WOODSTOCK_HOME/third-party/google-feedserver/google-feedserver-java-client-1.0.jar:\
$WOODSTOCK_HOME/third-party/google-feedserver/commons-lang-2.4.jar:\
$WOODSTOCK_HOME/build/prod/classes/:\
$WOODSTOCK_HOME/third-party/jsch/jsch-0.1.39.jar:\
$WOODSTOCK_HOME/build/testClasses/
JVM_ARGS="-Djava.net.preferIPv4Stack=true"   

CMD="java com.google.dataconnector.testserver.TestServerFailures \
 -localConfigFile ./config/localConfig.xml \
 -rulesFile ./config/resourceRules.xml \
 -sshPrivateKeyFile ./config/id_rsa_no_password \
 -testServerListenPort 9009"

# uncomment this line to get more debugging output
#CMD="${CMD} -verbose"

${CMD} -exitpoint AFTER_CLIENT_CONNECTS 
${CMD} -exitpoint AFTER_AUTHZ_REQ_RECVD
${CMD} -exitpoint AFTER_AUTHZ_RESPONSE_SENT
${CMD} -exitpoint AFTER_REG_REQ_RECVD
${CMD} -exitpoint AFTER_REG_RESPONSE_SENT
${CMD} -exitpoint AFTER_SSHD_START
${CMD} -exitpoint NORMAL_EXIT
