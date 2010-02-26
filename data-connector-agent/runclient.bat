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

REM
REM ARGUMENT DEFINTION
REM 

set DEPLOY_JAR=build\sdc-agent.jar
set LOCALCONFIG_FILE=config\localConfig.xml
set RULES_FILE=config\resourceRules.xml
set LOGGING_PROPERTIES=config\log4j.properties

REM
REM AGENT INVOCATION LOOP
REM

:INVOKEAGENT

java ^
  -jar %DEPLOY_JAR% ^
  -localConfigFile %LOCALCONFIG_FILE% ^
  -rulesFile %RULES_FILE% ^
  -log4jPropertiesFile %LOGGING_PROPERTIES%

goto INVOKEAGENT


