# Phase 11 Completed Commands

## GPS-D Backup, 2026-10-07

```bash
cd /tmp

PERMISSIONS_HOME=/spare/permissions
MULE_HOME=/spare/mule/current
BACKUP="$PERMISSIONS_HOME/backup/cutover-20261007-022347"

mkdir -p "$BACKUP"/{wars,tomcat,indexes,bin,config}

cp -aL /opt/software/GPS/webapps/internal-web.war "$BACKUP/wars/"
cp -aL /opt/software/GPS/webapps/author-web.warb "$BACKUP/wars/"
cp -a /opt/software/GPS/webapps/author-web-temp.war "$BACKUP/wars/"

if [ -d /opt/software/GPS/webapps/internal-web ]; then
  cp -aL /opt/software/GPS/webapps/internal-web "$BACKUP/wars/"
fi

cp -aL /opt/software/GPS/conf "$BACKUP/tomcat/conf"
cp -aL "$PERMISSIONS_HOME/productIndex" "$BACKUP/indexes/"
cp -aL "$PERMISSIONS_HOME/assetUseIndex" "$BACKUP/indexes/"
cp -aL "$PERMISSIONS_HOME/mule" "$BACKUP/permissions-mule"
cp -aL "$MULE_HOME" "$BACKUP/mule-home"
cp -aL "$PERMISSIONS_HOME/permissions.properties" "$BACKUP/config/"
cp -aL /spare/permissions/bin/perm-mule-web.sh "$BACKUP/bin/"
cp -aL /spare/permissions/bin/perm-mule-proc.sh "$BACKUP/bin/"
```

## Section 6 — enable Camel WEB and stop Mule WEB (GPS-D 2026-10-12)

Exact steps that worked. Leave Mule PROC running. Do not start Mule WEB again after this.

### 1. Set flag true and restart Tomcat

```bash
sudo -u perm vi /spare/permissions/development.properties
# set: gps.camel.web.enabled=true
# (permissions.properties -> development.properties on GPS-D)

/opt/software/GPS/bin/shutdown.sh
/opt/software/GPS/bin/startup.sh
```

### 2. Stop Mule WEB

```bash
sudo -E -u perm bash
cd /spare/permissions/bin
source ./environment.bash

export MULE_HOME=/spare/mule/mule-standalone-3.8.1
export MULE_BASE=$MULE_HOME
cd /tmp
/spare/permissions/bin/perm-mule-web.sh stop
```

If that reports `Mule was not running` but WEB is still up (missing `.mule.pid` — GPS-D case):

```bash
ps -ef | grep -i '[m]ule' | grep -i web
# confirm PERM_MULE_WEB / web-mule-config.xml — not PROC
# GPS-D: wrapper 99320, JVM 99322

kill -TERM 99320
sleep 8
kill -TERM 99322
```

### 3. Restart Tomcat (Camel binds 63082)

```bash
/opt/software/GPS/bin/shutdown.sh
/opt/software/GPS/bin/startup.sh
```
