# Xiaoman/Litemall AI Development Rules (AGENTS.md)

This file defines the project architecture, naming conventions, build commands, and critical guidelines for the "小满食光" (Xiaoman Shiguang) brand ordering mini-program and backend.

---

## 1. Project Overview & Directories
* **Ordering Frontend**: `xiaoman-wx-uniapp/` (Uni-app Vue3 + Vite project)
* **Admin Frontend**: `litemall-admin/` (Vue2 + Element-UI, Dev Port: `9527`)
* **Backend**: `litemall-all/`, `litemall-db/`, `litemall-core/`, `litemall-wx-api/`, `litemall-admin-api/` (Spring Boot / Maven, Dev Port: `8082`, Prod Port: `8080`)
* **Documentation Pointers**:
  * Architecture & State Machine: [docs/ARCHITECTURE.md](file:///Users/zhaozh/Desktop/litemall/docs/ARCHITECTURE.md)
  * Operations & Runbook: [docs/RUNBOOK.md](file:///Users/zhaozh/Desktop/litemall/docs/RUNBOOK.md)
  * System Design Spec: [docs/superpowers/specs/2026-06-17-xiaoman-ordering-design.md](file:///Users/zhaozh/Desktop/litemall/docs/superpowers/specs/2026-06-17-xiaoman-ordering-design.md)

---

## 2. Brand & Theme Guidelines (Must Adhere)
* **Style Tone**: Warm day-style handmade coffee/sandwich theme (warm brown, beige, terracotta).
* **Color System (App.vue)**:
  * Primary Theme / Accent Dark: `#6F4E37` (Warm Coffee Brown)
  * Page Background: `#FAF6EE` (Warm Cream-Beige)
  * Text Secondary / Inactive: `#A18276`
  * Primary Text: `#4A3525`
  * Action Highlight: `#E07A5F` (Terracotta)
* **Forbidden Colors**: Do NOT introduce raw blues (`#3b82f6`), cool indigos (`#4f46e5`), bright emerald greens, or generic alarm reds.

---

## 3. Architecture & Code Conventions

### 3.1 Mock SMS & Verification Bypass
* **SMS Verification back-door**: In development/testing, the verification code is pre-filled and hardcoded to `'123456'`.
* **Frontend (`register.vue`, `reset.vue`)**: The SMS input row is hidden; the code ref is initialized to `'123456'` and submitted automatically.
* **Backend (`WxAuthController.java`)**: The `register`, `reset`, and `resetPhone` endpoints explicitly check if the code is `'123456'`. If yes, it bypasses verification.

### 3.2 Mobile Phone Login Fallback
* **Backend**: If a login request (`/wx/auth/login`) fails to query the user by username (`queryByUsername`), it automatically falls back to querying by mobile phone number (`queryByMobile`).
* **Frontend**: The login input placeholder is `"请输入用户名或手机号"`.

### 3.3 Address Copy & Navigation
* **Homepage (`index.vue`)**:
  * Tapping the address copies `福州市仓山区公园路43号小满食光` to the clipboard first, and triggers a toast.
  * Map opening uses conditional compilation:
    * `#ifdef H5`: Redirects to Tencent Map Web Marker URI.
    * `#ifndef H5`: Calls `uni.openLocation` using coordinates `(latitude: 26.0438, longitude: 119.3144)`.

### 3.4 WeChat QR Code Long-press Recognition
* **Homepage (`index.vue`)**: Tapping the owner WeChat QR code wrapper invokes `onPreviewQR` which calls `uni.previewImage`. This allows the WeChat client to show its native long-press "Scan QR Code" menu.

### 3.5 Product Detail Viewing
* **Catalog/Ordering Page (`catalog.vue`)**: Tapping anywhere on a product card container `.goods-item` (excluding the quantity minus/plus selector buttons which have stop propagation) redirects to the detailed view page `/pages/goods/goods?id=${item.id}`.

### 3.6 Cloud Printer & WeChat Shipping Integration
* **Cloud Thermal Printer (`LitemallPrinterClient`)**: Automatically triggers print receipt jobs on paid status `201`. Safe bypass if credentials are not configured.
* **WeChat Order Shipping API (`WxShippingService`)**: Synchronizes virtual/express/pickup shipping info to the official WeChat Mini-Program Order Management API.

---

## 4. Key Commands & Operations

### 4.1 Local Development (`start.sh` at root)
* **One-Click Local Dev**: `./start.sh` (cleans occupied ports, starts backend on `8082` and admin dashboard on `9527`).

### 4.2 Frontend Commands
* **Xiaoman Mini-Program (`xiaoman-wx-uniapp/`)**:
  * H5: `npm run dev:h5` / `npm run build:h5`
  * WeChat MP: `npm run dev:mp-weixin` / `npm run build:mp-weixin`
* **Admin Portal (`litemall-admin/`)**:
  * Dev Server: `npm run dev` (Port 9527, proxies to `8082`)
  * Build into Backend Jar: `npm run build:dep`

### 4.3 Backend Commands
* **Clean & Package Jar**: `mvn clean package -Dmaven.test.skip=true`

### 4.4 Deployment (`deploy.sh` at root)
* **Helper Script**: `./deploy.sh` builds admin frontend, packages the Java jar, syncs to remote server, and restarts Docker container.
* **CRITICAL**: Do **NOT** run `./deploy.sh` or sync changes to the remote server if the user specifies "先不部署" (do not deploy yet). Always compile and test changes locally first.

### 4.5 Image Compression Helper (`compress_images.py` at root)
* **Helper Script**: `./compress_images.py` provides batch image resizing (max dimension 800px) and quality compression (default 85%) for local and server storage images (JPEG/PNG). Overwriting original files in-place requires `--inplace` flag.

### 4.6 Online Database Backup (`backup.sh` at root)
* **Helper Script**: `./backup.sh` connects to remote Docker MySQL, dumps the `litemall` database, and automatically synchronizes the `.sql` snapshot to the local `backup/` directory. Supports optional custom filename argument (default: `YYYY-MM-DD.sql`).

