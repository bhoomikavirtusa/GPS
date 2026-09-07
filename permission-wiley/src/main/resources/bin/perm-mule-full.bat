@echo off

set OLD_DIR=%CD%

cd %MULE_HOME%

set JTA_LOG_PATH=%MULE_HOME%/logs/full
set PIDDIR=%JTA_LOG_PATH%
set MULE_APP_NAME=PERM_MULE_FULL

set CONFIG_STR=conf/mule/web-mule-config.xml
set CONFIG_STR=%CONFIG_STR%, conf/mule/services-mule-config.xml
set CONFIG_STR=%CONFIG_STR%, conf/mule/pesearch-mule-config.xml

set CONFIG_STR=%CONFIG_STR%, conf/mule/peclient-mule-config.xml
set CONFIG_STR=%CONFIG_STR%, conf/mule/cmsclient-mule-config.xml
set CONFIG_STR=%CONFIG_STR%, conf/mule/message-mule-config.xml
set CONFIG_STR=%CONFIG_STR%, conf/mule/imports-mule-config.xml
set CONFIG_STR=%CONFIG_STR%, conf/mule/cron-mule-config.xml
set CONFIG_STR=%CONFIG_STR%, conf/mule/rest-services-mule-config.xml

bin\mule %1 -config "%CONFIG_STR%"

cd %OLD_DIR%