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
