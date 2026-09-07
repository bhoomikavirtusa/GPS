#!/bin/bash

if [[ -z "$CATALINA_HOME" ]]; then
  echo "CATALINA_HOME must be set!"
  exit 1
fi

if [[ -z "$PERMISSIONS_HOME" ]]; then
  echo "PERMISSIONS_HOME must be set!"
  exit 1
fi

if ! [[ -e "$CATALINA_HOME/webapps/internal-web.war" ]]; then
  echo "Symbolic link for internal-web.war missing - creating..."
  ln -s $PERMISSIONS_HOME/tomcat/internal-web.war $CATALINA_HOME/webapps/
fi

if ! [[ -e "$CATALINA_HOME/webapps/author-web.war" ]]; then
  echo "Symbolic link for author-web.war missing - creating..."
  ln -s $PERMISSIONS_HOME/tomcat/author-web.war $CATALINA_HOME/webapps/
fi

OLD_DIR=$(pwd)

cd $CATALINA_HOME

if ! [[ -d endorsed ]]; then
  echo "endorsed dir does not exist - copying..."
  cp -r ~smarkoff/endorsed .
fi

chmod u+x bin/*.sh

if ! [[ -f webapps/ROOT/index.jsp.orig ]]; then
  echo "Setting up webapps/ROOT/index.jsp to redirect..."
  mv webapps/ROOT/index.jsp webapps/ROOT/index.jsp.orig
  # Tomcat 7 no longer has index.html
  #mv webapps/ROOT/index.html webapps/ROOT/index.html.orig
  echo "<%
    String ssoHeader = request.getHeader(\"user_dn\");
    if (ssoHeader != null && ssoHeader.contains(\"Wiley Customers\"))
      response.sendRedirect(\"/author-web/\");
    else response.sendRedirect(\"/internal-web/\");
    %>" > webapps/ROOT/index.jsp
fi


if [ "$1" == "start" ]; then
    rm -rf $CATALINA_HOME/work/*
fi

# regular Tomcat has catalina.sh
# Covalent has tomcat_startup.sh
if [ -f bin/catalina.sh ]; then
    bin/catalina.sh $1
elif [ -f bin/tomcat_startup.sh ]; then
    bin/tomcat_startup.sh $1
fi

cd $OLD_DIR