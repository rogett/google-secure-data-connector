REM * Copyright 2008 Google Inc.
REM *
REM * This program is free software; you can redistribute it and/or
REM * modify it under the terms of the GNU General Public License
REM * as published by the Free Software Foundation; either version 2
REM * of the License, or (at your option) any later version.
REM *
REM * This program is distributed in the hope that it will be useful,
REM * but WITHOUT ANY WARRANTY; without even the implied warranty of
REM * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
REM * GNU General Public License for more details.
REM *
REM * You should have received a copy of the GNU General Public License
REM * along with this program; if not, write to the Free Software
REM * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

set WOODSTOCK_HOME=%CD%

set %JAVA_HOME%=C:\Program Files\java\jdk1.6.0_10
set %PATH%=%JAVA_HOME%\bin;%PATH%
set CLASSPATH=%WOODSTOCK_HOME%\third-party\jsocks\bin\jsocks-server.jar
set CLASSPATH=%CLASSPATH%;%WOODSTOCK_HOME%\third-party\wpg-proxy\bin\wpgproxy.jar
set CLASSPATH=%CLASSPATH%;%WOODSTOCK_HOME%\third-party\jline\jline-0.9.91.jar
set CLASSPATH=%CLASSPATH%;%WOODSTOCK_HOME%\third-party\commons-io\commons-io-1.3.2.jar
set CLASSPATH=%CLASSPATH%;%WOODSTOCK_HOME%\third-party\jsch\jsch-20071024.jar
set CLASSPATH=%CLASSPATH%;%WOODSTOCK_HOME%\third-party\apache-log4j\log4j-1.2.15.jar
set CLASSPATH=%CLASSPATH%;%WOODSTOCK_HOME%\third-party\commons-logging\commons-logging-1.1.jar
set CLASSPATH=%CLASSPATH%;%WOODSTOCK_HOME%\third-party\commons-cli\commons-cli-1.1.jar
set CLASSPATH=%CLASSPATH%;%WOODSTOCK_HOME%\third-party\json\json.jar
set CLASSPATH=%CLASSPATH%;%WOODSTOCK_HOME%\build\prod\classes\

set JVM_ARGS=-Djava.net.preferIPv4Stack=true

set SDC_OPTION=-configFile %WOODSTOCK_HOME%\rules.properties 
set SDC_OPTION=%SDC_OPTION% -secureLinkServerHost enterprise-apps-tls.sandbox.google.com
set SDC_OPTION=%SDC_OPTION% -secureLinkServerPort 443
set SDC_OPTION=%SDC_OPTION% -logPropertiesFile %WOODSTOCK_HOME%\logging.properties
set SDC_OPTION=%SDC_OPTION% -useSsl 
set SDC_OPTION=%SDC_OPTION% -sslKeyStoreFile %WOODSTOCK_HOME%\config\secureLinkClientTrustStore
set SDC_OPTION=%SDC_OPTION% -sslKeyStorePassword woodstock

java %JVM_ARGS% com.google.dataconnector.client.Client %SDC_OPTION%