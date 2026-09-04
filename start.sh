#!/bin/bash

# 获取脚本所在绝对路径
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR"

echo "============================================="
echo "   正在启动 小满食光 本地开发环境..."
echo "============================================="

# 检查 8082 端口占用
if lsof -i:8082 >/dev/null 2>&1; then
    echo "[!] 提示: 检测到端口 8082 已被占用，正在清理进程..."
    lsof -t -i:8082 | xargs kill -9 >/dev/null 2>&1
    sleep 1
fi

# 检查 9527 端口占用
if lsof -i:9527 >/dev/null 2>&1; then
    echo "[!] 提示: 检测到端口 9527 已被占用，正在清理进程..."
    lsof -t -i:9527 | xargs kill -9 >/dev/null 2>&1
    sleep 1
fi

echo "--> 1. 正在后台启动 Java 后端服务 (端口: 8082)..."
nohup java -Dfile.encoding=UTF-8 -jar litemall-all/target/litemall-all-0.1.0-exec.jar --server.port=8082 > backend.log 2>&1 &
BACKEND_PID=$!

# 等待后端初始化
sleep 3

# 检查后端进程是否仍存活
if kill -0 $BACKEND_PID >/dev/null 2>&1; then
    echo "[✓] 后端服务成功启动 (PID: $BACKEND_PID)"
    echo "    后端运行日志保存在: ./backend.log"
else
    echo "[✗] 后端服务启动失败，请检查 ./backend.log 日志内容！"
    exit 1
fi

# 当前端进程退出或按 Ctrl+C 时，自动清理后端进程
cleanup() {
    echo ""
    echo "============================================="
    echo "   正在清理并关闭本地开发服务..."
    echo "============================================="
    if kill -0 $BACKEND_PID >/dev/null 2>&1; then
        echo "--> 正在终止后端进程 (PID: $BACKEND_PID)..."
        kill -9 $BACKEND_PID >/dev/null 2>&1
    fi
    echo "[✓] 所有服务已成功停止！"
    exit 0
}

trap cleanup INT TERM EXIT

echo "--> 2. 正在启动前端开发服务 (端口: 9527)..."
cd litemall-admin
npm run dev
