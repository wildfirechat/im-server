set -e
rm -rf /opt/im-server
rm -rf /etc/im-server
rm -rf /var/log/im-server
rm -rf /usr/lib/systemd/system/im-server.service

if [ -d /var/lib/im-server/h2db ]; then
echo "IM embed db file not deleted in path /var/lib/im-server/h2db, if you don't need it anymore, please remove it manually"
fi

if [ -d /var/lib/im-server/media ]; then
echo "IM embed media files not deleted in path /var/lib/im-server/media, if you don't need it anymore, please remove it manually"
fi

systemctl daemon-reload

