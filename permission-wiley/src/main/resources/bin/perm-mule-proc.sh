#!/bin/bash

OLD_DIR=`pwd`

cd $MULE_HOME

export JTA_LOG_PATH=$MULE_HOME/logs/proc
export PIDDIR=$JTA_LOG_PATH
export MULE_APP_NAME=PERM_MULE_PROC

if ! [ -d "$JTA_LOG_PATH" ]; then
  mkdir "$JTA_LOG_PATH"
fi

CONFIG_STR=conf/mule/processor-mule-config.xml
CONFIG_STR=$CONFIG_STR,conf/mule/services-mule-config.xml
CONFIG_STR=$CONFIG_STR,conf/mule/peclient-mule-config.xml
CONFIG_STR=$CONFIG_STR,conf/mule/cmsclient-mule-config.xml
CONFIG_STR=$CONFIG_STR,conf/mule/message-mule-config.xml
CONFIG_STR=$CONFIG_STR,conf/mule/imports-mule-config.xml
CONFIG_STR=$CONFIG_STR,conf/mule/cron-mule-config.xml

bin/mule $1 -config "$CONFIG_STR"

cd $OLD_DIR