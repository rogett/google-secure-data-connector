REM Copyright 2009 Google, Inc.
REM 
REM Licensed under the Apache License, Version 2.0 (the "License");
REM you may not use this file except in compliance with the License.
REM You may obtain a copy of the License at
REM 
REM http://www.apache.org/licenses/LICENSE-2.0
REM 
REM Unless required by applicable law or agreed to in writing, software
REM distributed under the License is distributed on an "AS IS" BASIS,
REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM See the License for the specific language governing permissions and
REM limitations under the License.
REM 
REM This is the IzPack installer generator.  It is currently used only for
REM Windows generation.
REM 
REM This invocation framework is embarrassingly nascent.  Please contribute
REM any portable improvements---e.g., better batch file, Windows Scripting
REM Host, Powershell---upstream!
REM
REM If you have Cygwin installed, you can easily run this UNIX agent scripts
REM in Windows instead of using of this incomplete batch script.
REM
REM $Id$

REM
REM CLASSPATH DEFINITION
REM

set CLASSPATH=%CLASSPATH%;lib\agent.jar
set CLASSPATH=%CLASSPATH%;lib\protocol-generated.jar
set CLASSPATH=%CLASSPATH%;lib\third-party\aopalliance\aopalliance.jar
set CLASSPATH=%CLASSPATH%;lib\third-party\apache-log4j\log4j-1.2.15.jar
set CLASSPATH=%CLASSPATH%;lib\third-party\commons-cli\commons-cli-1.1.jar
set CLASSPATH=%CLASSPATH%;lib\third-party\commons-logging\commons-logging-1.1.jar
set CLASSPATH=%CLASSPATH%;lib\third-party\google-collect-1.0-rc2\google-collect-1.0-rc2.jar
set CLASSPATH=%CLASSPATH%;lib\third-party\google-collect-1.0-rc2\google-collect-testfw-1.0-rc2.jar
set CLASSPATH=%CLASSPATH%;lib\third-party\google-feedserver\commons-beanutils-1.8.0.jar
set CLASSPATH=%CLASSPATH%;lib\third-party\google-feedserver\commons-beanutils-core-1.8.0.jar
set CLASSPATH=%CLASSPATH%;lib\third-party\google-feedserver\commons-lang-2.4.jar
set CLASSPATH=%CLASSPATH%;lib\third-party\google-feedserver\gdata-client-1.0.jar
set CLASSPATH=%CLASSPATH%;lib\third-party\google-feedserver\gdata-core-1.0.jar
set CLASSPATH=%CLASSPATH%;lib\third-party\google-feedserver\google-feedserver-java-client-2.0.jar
set CLASSPATH=%CLASSPATH%;lib\third-party\guice\guice-2.0.jar
set CLASSPATH=%CLASSPATH%;lib\third-party\jsocks\jsocks.jar
set CLASSPATH=%CLASSPATH%;lib\third-party\json\json.jar
set CLASSPATH=%CLASSPATH%;lib\third-party\protobuf\protobuf-java-2.2.0.jar

REM
REM ARGUMENT DEFINTION
REM 

set ENTRYPOINT_CLASS=com.google.dataconnector.client.Client
set LOCALCONFIG_FILE=etc\localConfig.xml
set RULES_FILE=etc\resourceRules.xml
set LOGGING_PROPERTIES=etc\log4j.properties

REM
REM AGENT INVOCATION LOOP
REM

:INVOKEAGENT

java ^
  -cp %CLASSPATH% ^
  %ENTRYPOINT_CLASS% ^
  -localConfigFile %LOCALCONFIG_FILE% ^
  -rulesFile %RULES_FILE% ^
  -log4jPropertiesFile %LOGGING_PROPERTIES%

goto INVOKEAGENT


