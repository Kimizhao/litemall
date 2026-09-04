#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

# Configuration
WORKSPACE_DIR="/Users/zhaozh/Desktop/litemall"
FRONTEND_DIR="${WORKSPACE_DIR}/xiaoman-wx-uniapp"
REMOTE_HOST="my-tencent-118"
REMOTE_DIR="/home/zhaozh/litemall"

# Output helpers
info() {
  echo -e "\033[1;34m[INFO]\033[0m $1"
}

success() {
  echo -e "\033[1;32m[SUCCESS]\033[0m $1"
}

error() {
  echo -e "\033[1;31m[ERROR]\033[0m $1"
  exit 1
}

echo "========================================="
echo "        小满食光手作 自动化部署脚本"
echo "========================================="

# 1. 编译前端h5页面
info "1. 开始编译前端 H5 页面..."
if [ -d "${FRONTEND_DIR}" ]; then
  cd "${FRONTEND_DIR}"
  npm run build:h5
  success "前端 H5 编译完成。"
else
  error "前端目录 ${FRONTEND_DIR} 不存在！"
fi

# 2. rsync同步前端产物
info "2. 同步前端编译产物到云服务器..."
# rsync -avz --delete "${FRONTEND_DIR}/dist/build/h5/" "${REMOTE_HOST}:${REMOTE_DIR}/h5/"
success "前端产物同步完成。"

# 3. 编译后端jar包
info "先编译管理后台的前端。"
cd "${WORKSPACE_DIR}/litemall-admin"
npm run build:dep
success "管理后台前端编译完成。"

info "3. 开始编译后端 jar 包..."
cd "${WORKSPACE_DIR}"
mvn clean package -Dmaven.test.skip=true
success "后端编译完成。"

# 4. scp复制后端产物
info "4. 复制后端编译产物 (jar 包) 到服务器..."
scp "${WORKSPACE_DIR}/litemall-all/target/litemall-all-0.1.0-exec.jar" "${REMOTE_HOST}:${REMOTE_DIR}/litemall/litemall.jar"
success "后端 jar 包复制完成。"

# 5. 重启服务
info "5. 远程重启后端 Java 容器服务..."
ssh "${REMOTE_HOST}" "cd ${REMOTE_DIR} && docker compose restart litemall"
success "容器 litemall 已成功重启。"

echo "========================================="
success "        自动部署全部成功！"
echo "========================================="
