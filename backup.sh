#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

# Base directory
WORKSPACE_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
LOCAL_BACKUP_DIR="${WORKSPACE_DIR}/backup"

# Remote configuration
REMOTE_HOST="my-tencent-118"
REMOTE_DIR="/home/zhaozh/litemall"
REMOTE_BACKUP_DIR="${REMOTE_DIR}/backup"
DB_CONTAINER="mysql-litemall"
DB_USER="root"
DB_PASS="litemall123456"
DB_NAME="litemall"

# Backup filename (Default: YYYY-MM-DD.sql, or pass as first argument)
BACKUP_DATE=$(date +%Y-%m-%d)
BACKUP_FILENAME="${1:-${BACKUP_DATE}.sql}"
REMOTE_FILE="${REMOTE_BACKUP_DIR}/${BACKUP_FILENAME}"
LOCAL_FILE="${LOCAL_BACKUP_DIR}/${BACKUP_FILENAME}"

# Output formatting helpers
info() {
  echo -e "\033[1;34m[INFO]\033[0m $1"
}

success() {
  echo -e "\033[1;32m[SUCCESS]\033[0m $1"
}

warn() {
  echo -e "\033[1;33m[WARN]\033[0m $1"
}

error() {
  echo -e "\033[1;31m[ERROR]\033[0m $1"
  exit 1
}

echo "========================================="
echo "       小满食光 线上数据库备份脚本"
echo "========================================="
info "目标备份文件: ${BACKUP_FILENAME}"
info "远程服务器: ${REMOTE_HOST}"

# 1. 确保本地与远程备份目录存在
mkdir -p "${LOCAL_BACKUP_DIR}"

# 2. 在远端执行 mysqldump 导出
info "1. 正在远程导出数据库 (${DB_NAME})..."
ssh "${REMOTE_HOST}" "mkdir -p '${REMOTE_BACKUP_DIR}' && docker exec -e MYSQL_PWD='${DB_PASS}' '${DB_CONTAINER}' mysqldump -u'${DB_USER}' --default-character-set=utf8mb4 '${DB_NAME}' > '${REMOTE_FILE}'"

# 3. 校验远端备份文件
info "2. 校验远程备份文件完整性..."
REMOTE_SIZE=$(ssh "${REMOTE_HOST}" "stat -c%s '${REMOTE_FILE}' 2>/dev/null || stat -f%z '${REMOTE_FILE}' 2>/dev/null || wc -c < '${REMOTE_FILE}'")

if [ -z "${REMOTE_SIZE}" ] || [ "${REMOTE_SIZE}" -lt 1024 ]; then
  error "远程备份文件生成异常或体积过小 (${REMOTE_SIZE} bytes)！"
fi
success "远程备份成功生成: ${REMOTE_FILE} (大小: ${REMOTE_SIZE} 字节)"

# 4. 同步至本地
info "3. 正在将备份文件拉取到本地..."
scp "${REMOTE_HOST}:${REMOTE_FILE}" "${LOCAL_FILE}"

# 5. 校验本地文件
if [ ! -f "${LOCAL_FILE}" ]; then
  error "本地备份文件拉取失败，文件不存在！"
fi

LOCAL_SIZE=$(wc -c < "${LOCAL_FILE}" | tr -d ' ')
LOCAL_LINES=$(wc -l < "${LOCAL_FILE}" | tr -d ' ')

echo "========================================="
success "        线上数据库备份完成！"
echo "========================================="
echo "  * 本地路径: ${LOCAL_FILE}"
echo "  * 远端路径: ${REMOTE_FILE}"
echo "  * 文件大小: ${LOCAL_SIZE} 字节"
echo "  * SQL 行数: ${LOCAL_LINES} 行"
echo "========================================="
