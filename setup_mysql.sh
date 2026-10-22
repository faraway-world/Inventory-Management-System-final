#!/bin/bash
set -e

echo "=== Installing MariaDB (MySQL) ==="
pacman -S --needed --noconfirm mariadb mariadb-clients

echo "=== Initializing Database Directory ==="
if [ ! -d "/var/lib/mysql/mysql" ]; then
    mariadb-install-db --user=mysql --basedir=/usr --datadir=/var/lib/mysql
fi

echo "=== Starting MariaDB Service ==="
systemctl enable --now mariadb

echo "=== Creating Database 'oop_inventory' ==="
mariadb -e "CREATE DATABASE IF NOT EXISTS oop_inventory; GRANT ALL PRIVILEGES ON oop_inventory.* TO 'root'@'localhost' IDENTIFIED BY ''; FLUSH PRIVILEGES;"

echo ""
echo "=== SUCCESS! MySQL / MariaDB is running and configured for oop_inventory ==="
