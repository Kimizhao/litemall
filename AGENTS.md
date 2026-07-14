# Xiaoman/Litemall AI Development Rules (AGENTS.md)

This file defines the project architecture, naming conventions, build commands, and critical guidelines for the "小满食光" (Xiaoman Shiguang) brand ordering mini-program and backend.

---

## 1. Project Overview & Directories
* **Frontend**: `xiaoman-wx-uniapp/` (Uni-app Vue3 + Vite project)
* **Backend**: `litemall-db/`, `litemall-core/`, `litemall-wx-api/`, etc. (Spring Boot / Maven project)
* **Design Specification**: [docs/superpowers/specs/2026-06-17-xiaoman-ordering-design.md](file:///Users/zhaozh/Desktop/litemall/docs/superpowers/specs/2026-06-17-xiaoman-ordering-design.md)

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

---

## 4. Key Commands & Operations

### 4.1 Frontend Commands (`xiaoman-wx-uniapp/`)
* **H5 Dev Server**: `npm run dev:h5`
* **H5 Compilation**: `npm run build:h5`
* **WeChat MP Dev**: `npm run dev:mp-weixin`
* **WeChat MP Compilation**: `npm run build:mp-weixin`

### 4.2 Backend Commands
* **Clean & Package Jar**: `mvn clean package -Dmaven.test.skip=true`

### 4.3 Deployment (`deploy.sh` at root)
* **Helper Script**: `./deploy.sh` compiles the H5 static assets, syncs them to the remote server, packages the Java jar, uploads it, and restarts the remote Docker container.
* **CRITICAL**: Do **NOT** run `./deploy.sh` or sync changes to the remote server if the user specifies "先不部署" (do not deploy yet). Always compile and test changes locally first.
