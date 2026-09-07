#!/bin/bash

export PS1='${PWD#$HOME/} $ '
alias ls='ls -F'

export PERMISSIONS_HOME=/spare/permissions

export JAVA_HOME=/spare/java/current
export CATALINA_OPTS="-Djava.awt.headless=true"
export CATALINA_HOME=/spare/tomcat/current
export JAVA_OPTS="-Xms256m -Xmx1024m -XX:MaxPermSize=256m -server"
export MULE_HOME=/spare/mule/current
export MULE_BASE=$MULE_HOME

PATH=$PATH:$HOME/bin:$JAVA_HOME/bin:$PERMISSIONS_HOME/bin

export PATH
