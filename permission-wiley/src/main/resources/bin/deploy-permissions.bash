#!/bin/bash

# If this script does not work after FTP-ing to Linux, make sure to remove
# carriage returns (use dos2unix) command.
#
# This script is meant as a deploy utility for the permissions code base.
# It takes one argument: the location of the code to be deployed.
# I.e. where you ftp'd the files to.

if [ $# -lt 1 ] ; then
    echo "Usage: ${0##*/} [directory of code to deploy]"
    exit 1
fi

if [ -z $PERMISSIONS_HOME ]; then
    echo "ERROR: Permissions Home Not Set.  Please set PERMISSIONS_HOME environment variable."
    exit 1
fi

echo "Permissions Home: $PERMISSIONS_HOME"

SOURCE_DIR=$1

if [ ! -d $SOURCE_DIR ]; then
    echo "ERROR: Source Directory $SOURCE_DIR does not exist."
    exit 1
fi

echo "Source Directory $SOURCE_DIR exists.  Checking Validity."

if [ ! -d $SOURCE_DIR/tomcat ]; then
	echo "ERROR: There is no tomcat directory in $SOURCE_DIR"
	exit 1
fi

if [ ! -d $SOURCE_DIR/mule ]; then
	echo "ERROR: There is no mule directory in $SOURCE_DIR"
	exit 1
fi

# This check is not really part of deployment but good thing to check
if [ -z $MULE_HOME ]; then
  echo "ERROR: Mule Home not set. Please set MULE_HOME environment variable."
  exit 1
fi

if [ ! -L $MULE_HOME/lib/user ]; then
  echo "ERROR: $MULE_HOME/lib/user is expected to be a symbolic link to $PERMISSION_HOME/mule"
  exit 1
fi

# If we've gotten this far, then we're good to copy code.  Let's make a backup

echo "Backing up current release..."

if [ ! -d $PERMISSIONS_HOME/backup ]; then
	mkdir -p $PERMISSIONS_HOME/backup
fi

if [ -d $PERMISSIONS_HOME/mule ] && [ -d $PERMISSIONS_HOME/tomcat ]; then
	TMP_DIR=/tmp/permdeploytmp-$(date +%m-%d-%Y-%H-%M-%S)

	mkdir -p $TMP_DIR

	cp -R $PERMISSIONS_HOME/mule $TMP_DIR
	cp -R $PERMISSIONS_HOME/tomcat $TMP_DIR

	BACKUP_FILE=permissions-backup-$(date +%m-%d-%Y-%H-%M-%S).tar

	tar -cf $PERMISSIONS_HOME/backup/$BACKUP_FILE $TMP_DIR/*

	rm -fr $TMP_DIR

	echo "Created Backup: $BACKUP_FILE"
else
	echo "WARNING: There doesn't seem to be a full deployment in place to backup.  No Backup Made"
fi

rm -fr $PERMISSIONS_HOME/mule
rm -fr $PERMISSIONS_HOME/tomcat

cp -R $SOURCE_DIR/mule $PERMISSIONS_HOME
cp -R $SOURCE_DIR/tomcat $PERMISSIONS_HOME

echo "Source Code Deployed To: $PERMISSIONS_HOME"

# record keeping
response=
while [ -z $response ]; do
  echo -n "What tag is this (e.g. v3r9)? "
  read response
  if [ -n "$response" ]; then
    echo $response > $PERMISSIONS_HOME/version.txt
    echo $response written to $PERMISSIONS_HOME/version.txt
  fi
done
