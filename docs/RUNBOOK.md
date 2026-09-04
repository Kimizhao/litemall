# 小满食光运维与开发操作手册 (RUNBOOK.md)

本文档提供“小满食光”项目从本地开发调试、产物构建打包到生产环境部署与故障排查的完整运维操作指引。

---

## 1. 环境依赖与版本要求

| 组件 | 推荐版本 | 说明 |
|---|---|---|
| **JDK** | 1.8 / 11 / 17 | 需配置 `JAVA_HOME` 与 `PATH` |
| **Maven** | 3.6+ | 包含阿里云镜像配置更佳 |
| **Node.js** | 16.x / 18.x | 运行 Uni-app Vite 与 Vue-CLI |
| **MySQL** | 5.7 / 8.0 | 字符集 `utf8mb4` |
| **微信开发者工具** | 最新稳定版 | 小程序导入与真机预览 |

---

## 2. 本地开发与联调快速启动

### 2.1 一键启动脚本 (`./start.sh`)

项目根目录提供了自动化启动脚本 `./start.sh`：

```bash
./start.sh
```

**内部执行流程**：
1. 检测并自动清理端口 `8082`（Java 后端）与 `9527`（管理端前端）的遗留占用进程。
2. 后台启动 Java 后端服务（运行端口 `8082`，输出重定向至 `./backend.log`）。
3. 前台启动 `litemall-admin` 开发服务器（端口 `9527`）。
4. 捕获中断信号（`Ctrl+C`），在退出时自动终止后台 Java 进程，保持系统清洁。

### 2.2 小程序端开发 (`xiaoman-wx-uniapp/`)

```bash
cd xiaoman-wx-uniapp

# 安装依赖
npm install

# 启动微信小程序编译监听 (在微信开发者工具中导入 dist/dev/mp-weixin)
npm run dev:mp-weixin

# 或者启动 H5 调试模式 (默认端口 5173)
npm run dev:h5
```

### 2.3 商家后台管理端 (`litemall-admin/`)

```bash
cd litemall-admin

# 安装依赖
npm install --registry=https://registry.npmmirror.com

# 启动开发服务器 (端口 9527，自动代理 /admin 至 http://localhost:8082)
npm run dev
```

---

## 3. 构建打包与发布

### 3.1 编译管理后台静态资源至后端包

为使 Spring Boot 单 Jar 包直接托管商家管理后台，需先将后台静态资源构建至 `litemall-all/src/main/resources/static`：

```bash
cd litemall-admin
npm run build:dep
```

### 3.2 编译后端可执行 Jar 包

```bash
mvn clean package -Dmaven.test.skip=true
# 编译产物位于: litemall-all/target/litemall-all-0.1.0-exec.jar
```

### 3.3 自动化部署脚本 (`./deploy.sh`)

根目录脚本 `./deploy.sh` 实现了自动化集成与远端部署：

```bash
./deploy.sh
```

**部署步骤**：
1. 编译 `xiaoman-wx-uniapp` 前端 H5 产物。
2. 执行 `litemall-admin` 的 `npm run build:dep` 打包管理前端。
3. 编译 Java 后端 `litemall-all-0.1.0-exec.jar`。
4. 使用 `scp` 将 Jar 包同步至云服务器。
5. 远程执行 `ssh` 命令重启 Docker Compose 容器 `docker compose restart litemall`。

> [!CAUTION]
> **部署安全红线**：若用户明确指示“先不部署”，严禁执行 `./deploy.sh` 或向远端服务器推送任何文件。

---

## 4. 工具脚本与日常维护

### 4.1 图片批量压缩 (`./compress_images.py`)

在向静态资源目录添加新的高清图片或在服务器上归档时，可使用压缩脚本降低尺寸与文件大小：

```bash
# 扫描当前目录，生成压缩副本并统计节省体积
python3 compress_images.py .

# 直接就地覆盖原图 (保留原文件名)
python3 compress_images.py ./xiaoman-wx-uniapp/src/static/xiaoman --inplace --quality 85
```

### 4.2 线上数据库一键备份 (`./backup.sh`)

项目根目录提供了自动化远端数据库导出与本地同步脚本：

```bash
# 默认备份至 backup/YYYY-MM-DD.sql
./backup.sh

# 支持传入自定义文件名
./backup.sh custom_backup.sql
```

**执行流程**：
1. 远程在云服务器执行 `docker exec mysql-litemall mysqldump` 导出 `litemall` 数据库为 utf8mb4 SQL 文件。
2. 远端校验备份文件非空与大小完整性。
3. 通过 `scp` 自动拉取归档至本地 `backup/` 目录并输出统计信息。


---

## 5. 常见问题排查 (FAQ)

### Q1: 本地短信验证码无法收到？
* **解答**：开发与测试环境已内置后门通道，验证码统一固定为 `'123456'`，前端注册与重置页面已预填并隐藏验证码输入框，可直接提交。

### Q2: 自提订单提交报“收货地址为空”错误？
* **解答**：自提模式会在结算页静默创建 `areaCode` 为 `310115`、名称为 `到店自提(小满食光)` 的虚拟收货地址。如遇报错，请检查数据库 `litemall_address` 表是否能正常写入。

### Q3: 飞鹅打印机未配置 AppID 时后端报错？
* **解答**：`LitemallPrinterClient` 具有空配置安全降级保护（Bypass）。若未在 `application.yml` 配置密钥，相关增删改查将记录警告日志并安全返回 `true`，不阻塞主流程。
