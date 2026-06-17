# 小满食光手作点餐小程序系统设计规范 (Spec)

本文档定义了“小满食光手作”点餐小程序（基于 Uni-app）与现有的 Litemall Spring Boot 后端集成的设计与开发实现规范。

---

## 1. 目标与定位 (Goal & Position)

*   **店铺名称**：小满食光手作（主打手作三明治、简餐、现榨生椰饮品）
*   **核心功能**：实现扫码点单、外卖配送、到店自取、预约点餐四合一，第一阶段重点落地**“预约到店点餐”**，第二阶段迭代“团体报餐”。
*   **后端方案**：集成现有 Litemall 后端 API（无须修改 Java 源码与编译后端，参数通过订单留言 `message` 字段做结构化封装）。
*   **视觉风格**：暖日系手作风格（配色温馨、圆润边角、微交互缩放动效）。

---

## 2. 数据库配置与商品数据 SQL (Database SQL)

为了兼容 Litemall 的二级分类（L2）过滤规则以及防止未付款订单在 30 分钟内被后台定时任务自动取消，我们需要在数据库中执行以下 SQL 进行系统配置修改和商品类目插入。

```sql
-- 1. 系统配置：将未付款订单超时取消时间修改为 1440 分钟（24 小时），防止预约订单被提前取消
UPDATE `litemall_system` SET `key_value` = '1440' WHERE `key_name` = 'litemall_order_unpaid';

-- 2. 软删除已有的冲突或旧数据（防止直接删除产生外键或审计问题，设 deleted = 1）
UPDATE `litemall_category` SET `deleted` = 1, `update_time` = NOW() WHERE `id` IN (1001000, 1001001, 1002000, 1002001, 1003000, 1003001);
UPDATE `litemall_goods` SET `deleted` = 1, `update_time` = NOW() WHERE `id` BETWEEN 1181001 AND 1181010;
UPDATE `litemall_goods_product` SET `deleted` = 1, `update_time` = NOW() WHERE `id` BETWEEN 1181001 AND 1181010;
UPDATE `litemall_goods_specification` SET `deleted` = 1, `update_time` = NOW() WHERE `goods_id` BETWEEN 1181001 AND 1181010;
UPDATE `litemall_goods_attribute` SET `deleted` = 1, `update_time` = NOW() WHERE `goods_id` BETWEEN 1181001 AND 1181010;

-- 3. 插入或恢复一级分类 (L1) 与对应的二级分类 (L2) （若已存在则更新属性并恢复 deleted = 0）
INSERT INTO `litemall_category` (`id`, `name`, `keywords`, `desc`, `pid`, `icon_url`, `pic_url`, `level`, `sort_order`, `add_time`, `update_time`, `deleted`) VALUES
(1001000, '招牌三明治 🥪', '三明治', '经典美味手作三明治', 0, '', '', 'L1', 1, NOW(), NOW(), 0),
(1001001, '手作三明治', '三明治', '美味三明治二级分类', 1001000, '', '', 'L2', 1, NOW(), NOW(), 0),
(1002000, '手作简餐 🍙', '简餐,饭团', '暖心手作简餐饭团', 0, '', '', 'L1', 2, NOW(), NOW(), 0),
(1002001, '手作饭团', '饭团,简餐', '美味饭团二级分类', 1002000, '', '', 'L2', 1, NOW(), NOW(), 0),
(1003000, '现榨饮品 🍹', '饮品,椰汁,生椰', '天然现榨清凉饮品', 0, '', '', 'L1', 3, NOW(), NOW(), 0),
(1003001, '椰风饮品', '椰汁,饮品', '椰乳饮品二级分类', 1003000, '', '', 'L2', 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `keywords`=VALUES(`keywords`), `desc`=VALUES(`desc`), `pid`=VALUES(`pid`), `icon_url`=VALUES(`icon_url`), `pic_url`=VALUES(`pic_url`), `level`=VALUES(`level`), `sort_order`=VALUES(`sort_order`), `update_time`=NOW(), `deleted`=0;

-- 4. 插入或恢复商品基本信息 (Goods - 关联二级分类 ID)
INSERT INTO `litemall_goods` (`id`, `goods_sn`, `name`, `category_id`, `brand_id`, `gallery`, `keywords`, `brief`, `is_on_sale`, `sort_order`, `pic_url`, `share_url`, `is_new`, `is_hot`, `unit`, `counter_price`, `retail_price`, `detail`, `add_time`, `update_time`, `deleted`) VALUES
(1181001, 'XM-001', '烟熏培根三明治', 1001001, 0, '["/static/xiaoman/sandwich_4.jpg"]', '培根,三明治', '精选培根、溏心煎蛋、新鲜番茄、生菜、玉米沙拉', 1, 1, '/static/xiaoman/sandwich_4.jpg', '', 1, 1, '份', 10.00, 8.80, '手作烟熏培根三明治', NOW(), NOW(), 0),
(1181002, 'XM-002', '芝士牛肉饼三明治', 1001001, 0, '["/static/xiaoman/sandwich_2.jpg"]', '牛肉,三明治', '手工牛肉饼、芝士片、酸黄瓜、紫甘蓝、玉米沙拉', 1, 2, '/static/xiaoman/sandwich_2.jpg', '', 1, 1, '份', 12.00, 10.60, '手作芝士牛肉饼三明治', NOW(), NOW(), 0),
(1181003, 'XM-003', '紫薯里脊三明治', 1001001, 0, '["/static/xiaoman/sandwich_12.jpg"]', '紫薯,里脊', '手作紫薯泥、香煎里脊肉、流心煎蛋、新鲜生菜', 1, 3, '/static/xiaoman/sandwich_12.jpg', '', 1, 1, '份', 12.00, 10.60, '手作紫薯里脊三明治', NOW(), NOW(), 0),
(1181004, 'XM-004', '活力热狗三明治', 1001001, 0, '["/static/xiaoman/sandwich_13.jpg"]', '热狗,三明治', '珍味烤肠、脆青瓜片、香甜玉米粒、特调番茄酱', 1, 4, '/static/xiaoman/sandwich_13.jpg', '', 1, 0, '份', 9.00, 7.90, '手作活力热狗三明治', NOW(), NOW(), 0),
(1181005, 'XM-005', '小满紫米饭团 (基础款)', 1002001, 0, '["/static/xiaoman/rice_1.jpg"]', '紫米,饭团', '糯香紫米、脆萝卜丁、黄金肉松、香脆油条、沙拉酱', 1, 5, '/static/xiaoman/rice_1.jpg', '', 1, 1, '个', 6.00, 5.30, '手作小满紫米饭团', NOW(), NOW(), 0),
(1181006, 'XM-006', '烤肠紫米饭团', 1002001, 0, '["/static/xiaoman/rice_1.jpg"]', '烤肠,饭团', '基础款饭团配料，特别加入一整根脆皮烤肠', 1, 6, '/static/xiaoman/rice_1.jpg', '', 1, 0, '个', 8.00, 7.00, '手作烤肠紫米饭团', NOW(), NOW(), 0),
(1181007, 'XM-007', '咸蛋黄紫米饭团', 1002001, 0, '["/static/xiaoman/rice_1.jpg"]', '咸蛋黄,饭团', '基础款饭团配料，特别加入两颗起沙咸蛋黄', 1, 7, '/static/xiaoman/rice_1.jpg', '', 1, 1, '个', 9.00, 7.90, '手作咸蛋黄紫米饭团', NOW(), NOW(), 0),
(1181008, 'XM-008', '翡翠椰子汁', 1003001, 0, '["/static/xiaoman/rice_1.jpg"]', '椰子汁', '原榨清甜椰子水，健康天然无添加，清热消暑', 1, 8, '/static/xiaoman/rice_1.jpg', '', 1, 0, '杯', 12.00, 10.60, '现榨清甜翡翠椰子汁', NOW(), NOW(), 0),
(1181009, 'XM-009', '柠檬黄椰汁', 1003001, 0, '["/static/xiaoman/rice_1.jpg"]', '柠檬,生椰', '清爽香水柠檬原汁，融入纯厚生椰乳', 1, 9, '/static/xiaoman/rice_1.jpg', '', 1, 1, '杯', 10.00, 8.80, '现榨柠檬生椰乳', NOW(), NOW(), 0),
(1181010, 'XM-010', '草莓椰椰', 1003001, 0, '["/static/xiaoman/rice_1.jpg"]', '草莓,生椰', '手捣新鲜草莓果肉，加入浓郁椰子乳', 1, 10, '/static/xiaoman/rice_1.jpg', '', 1, 1, '杯', 8.00, 7.00, '手捣草莓生椰乳', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE `goods_sn`=VALUES(`goods_sn`), `name`=VALUES(`name`), `category_id`=VALUES(`category_id`), `brand_id`=VALUES(`brand_id`), `gallery`=VALUES(`gallery`), `keywords`=VALUES(`keywords`), `brief`=VALUES(`brief`), `is_on_sale`=VALUES(`is_on_sale`), `sort_order`=VALUES(`sort_order`), `pic_url`=VALUES(`pic_url`), `share_url`=VALUES(`share_url`), `is_new`=VALUES(`is_new`), `is_hot`=VALUES(`is_hot`), `unit`=VALUES(`unit`), `counter_price`=VALUES(`counter_price`), `retail_price`=VALUES(`retail_price`), `detail`=VALUES(`detail`), `update_time`=NOW(), `deleted`=0;

-- 5. 插入或恢复销售产品条目 (Product)
INSERT INTO `litemall_goods_product` (`id`, `goods_id`, `specifications`, `price`, `number`, `url`, `add_time`, `update_time`, `deleted`) VALUES
(1181001, 1181001, '["标准"]', 8.80, 200, '', NOW(), NOW(), 0),
(1181002, 1181002, '["标准"]', 10.60, 200, '', NOW(), NOW(), 0),
(1181003, 1181003, '["标准"]', 10.60, 200, '', NOW(), NOW(), 0),
(1181004, 1181004, '["标准"]', 7.90, 200, '', NOW(), NOW(), 0),
(1181005, 1181005, '["标准"]', 5.30, 200, '', NOW(), NOW(), 0),
(1181006, 1181006, '["标准"]', 7.00, 200, '', NOW(), NOW(), 0),
(1181007, 1181007, '["标准"]', 7.90, 200, '', NOW(), NOW(), 0),
(1181008, 1181008, '["标准"]', 10.60, 200, '', NOW(), NOW(), 0),
(1181009, 1181009, '["标准"]', 8.80, 200, '', NOW(), NOW(), 0),
(1181010, 1181010, '["标准"]', 7.00, 200, '', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE `goods_id`=VALUES(`goods_id`), `specifications`=VALUES(`specifications`), `price`=VALUES(`price`), `number`=VALUES(`number`), `url`=VALUES(`url`), `update_time`=NOW(), `deleted`=0;

-- 6. 插入或恢复规格与属性
INSERT INTO `litemall_goods_specification` (`goods_id`, `specification`, `value`, `pic_url`, `add_time`, `update_time`, `deleted`) VALUES
(1181001, '规格', '标准', '', NOW(), NOW(), 0),
(1181002, '规格', '标准', '', NOW(), NOW(), 0),
(1181003, '规格', '标准', '', NOW(), NOW(), 0),
(1181004, '规格', '标准', '', NOW(), NOW(), 0),
(1181005, '规格', '标准', '', NOW(), NOW(), 0),
(1181006, '规格', '标准', '', NOW(), NOW(), 0),
(1181007, '规格', '标准', '', NOW(), NOW(), 0),
(1181008, '规格', '标准', '', NOW(), NOW(), 0),
(1181009, '规格', '标准', '', NOW(), NOW(), 0),
(1181010, '规格', '标准', '', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE `specification`=VALUES(`specification`), `value`=VALUES(`value`), `pic_url`=VALUES(`pic_url`), `update_time`=NOW(), `deleted`=0;

INSERT INTO `litemall_goods_attribute` (`goods_id`, `attribute`, `value`, `add_time`, `update_time`, `deleted`) VALUES
(1181001, '用料', '精选培根、溏心煎蛋、新鲜番茄、生菜、玉米沙拉', NOW(), NOW(), 0),
(1181002, '用料', '手工牛肉饼、芝士片、酸黄瓜、紫甘蓝、玉米沙拉', NOW(), NOW(), 0),
(1181003, '用料', '手作紫薯泥、香煎里脊肉、流心煎蛋、新鲜生菜', NOW(), NOW(), 0),
(1181004, '用料', '珍味烤肠、脆青瓜片、香甜玉米粒、特调番茄酱', NOW(), NOW(), 0),
(1181005, '用料', '糯香紫米、脆萝卜丁、黄金肉松、香脆油条、沙拉酱', NOW(), NOW(), 0),
(1181006, '用料', '基础款饭团配料，特别加入一整根脆皮烤肠', NOW(), NOW(), 0),
(1181007, '用料', '基础款饭团配料，特别加入两颗起沙咸蛋黄', NOW(), NOW(), 0),
(1181008, '用料', '原榨清甜椰子水，健康天然无添加，清热消暑', NOW(), NOW(), 0),
(1181009, '用料', '清爽香水柠檬原汁，融入纯厚生椰乳', NOW(), NOW(), 0),
(1181010, '用料', '手捣新鲜草莓果肉，加入浓郁椰子乳', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE `attribute`=VALUES(`attribute`), `value`=VALUES(`value`), `update_time`=NOW(), `deleted`=0;
```

---

## 3. 前端修改方案 (Frontend Development Plan)

### 3.1 资源导入
在开发前，必须先将 `/Users/zhaozh/Desktop/little-full` 下的图片物理复制到小程序目录中：
*   复制目标目录：`litemall-wx-uniapp/src/static/xiaoman/`
*   背景及缩略图对应：
    *   `shop_header.jpg` <- `小满食光...1...jpg` (店铺横幅)
    *   `sandwich_4.jpg` <- `小满食光...4...jpg` (培根三明治)
    *   `sandwich_2.jpg` <- `小满食光...2...jpg` (牛肉三明治)
    *   `sandwich_12.jpg` <- `小满食光...12...jpg` (紫薯三明治)
    *   `sandwich_13.jpg` <- `小满食光...13...jpg` (热狗三明治)
    *   `rice_1.jpg` <- `人间烟火里的三明治_1...jpg` (饭团/饮品默认图)

### 3.2 路由及主页替换 (`pages/index/index`)
重写 `pages/index/index`，实现经典的暖系双栏点餐布局：
*   **店铺信息区块**：引入日系背景图、店铺头像、店名、副标题和高亮黄滚动条：“🎉 新店试营业，全场 8.8 折！”
*   **商品数据加载逻辑**：
    *   **精准分类加载机制**：由于后端做的是等于匹配（`andCategoryIdEqualTo`），传入 L1 父类 ID 无法获取任何商品，所以前端必须**传入实际商品关联的二级分类 (L2) 类目 ID** 过滤，发起三个并行的商品请求：
        1. `/wx/goods/list?categoryId=1001001&limit=100` (加载三明治二级分类下商品)
        2. `/wx/goods/list?categoryId=1002001&limit=100` (加载饭团二级分类下商品)
        3. `/wx/goods/list?categoryId=1003001&limit=100` (加载饮品二级分类下商品)
        请求返回后，前端将结果合并为一个统一的商品列表 `goodsList` 缓存在页面内存中。
    *   **内存过滤机制**：当用户在左侧侧边栏切换不同的一级分类（如三明治 `1001000`、简餐 `1002000`、饮品 `1003000`）时，前端在内存中对应筛选二级分类 ID 匹配的商品进行渲染（例如：点击招牌三明治 `1001000`，在右侧商品列表过滤出并渲染 `category_id === 1001001` 的商品）。
*   **本地图片路径适配**：若商品 `pic_url` 包含 `/static/xiaoman/`，前端直接使用该相对路径，绕过域名拼接逻辑。
*   **价格渲染**：原价显示划线价 `counter_price`，折后价作为销售价 `retail_price`。支持点击 `+` / `-` 调用 `/wx/cart/add` 等接口管理本地购物车。
*   **底部购物车抽屉**：购物车中有物品时，悬浮滑出黑色半透明质感底栏，显示总金额。点击可向上展开已加购商品列表，支持数量加减或一键清空。

### 3.3 结算页改造 (`pages/checkout/checkout`)
在确认订单页增加“取餐模式”的互斥 UI 切换逻辑：
1.  **取餐模式切换**：提供 Tabs/滑块组件：“到店自取” 与 “外卖配送”。
    *   **选择“外卖配送”时**：展示 Litemall 原生收货地址选择器（点击可选择/修改地址），隐藏预约信息表单。
    *   **选择“到店自取”时**：隐藏收货地址模块，并展示预约信息收集表单（包含姓名、性别称呼单选、电话、取餐时间选择、备注需求）。
2.  **到店自取时的地址参数处理 (专属虚拟自提地址机制)**：
    *   为了防止后端因为 `addressId` 字段非法而拦截订单，到店自提模式下前端**强制且唯一**使用一个专属自提虚拟地址：
        1.  进入结算页时，首先请求收货地址列表接口 `/wx/address/list`。
        2.  遍历地址数组，查找 `name === '到店自提(小满食光)'` 的记录：
            *   **若已存在**：直接使用该条记录的 `id` 作为提交的 `addressId`。
            *   **若不存在**：在调起保存前将页面锁定（置位布尔值锁 `isSavingAddress = true`），静默调用 `/wx/address/save` 接口保存一条专用门店虚拟地址，参数中**必须包含有效的 `areaCode` 字段**以绕过后台参数拦截：
                *   `name`: "到店自提(小满食光)"
                *   `tel`: "13800000000"
                *   `province`: "虚拟自提"
                *   `city`: "门店自取"
                *   `county`: "小满食光手作店"
                *   `areaCode`: "310115" (门店所在行政区域代码，防止后端校验报错)
                *   `addressDetail`: "到店自取专属地址"
                *   `isDefault`: false
                保存成功后，释放锁并将返回的有效自提 `id` 设为提交的 `addressId`。
        3.  **异常拦截与报错**：若静默创建/获取自提地址接口失败，捕获异常并释放锁，同时强制拦截订单提交，调用 `uni.showToast` 提示：“创建自提凭证失败，请重试”。
3.  **表单校验规则**：
    *   **姓名校验**：不能为空，去空格后字符长度必须在 `1 - 10` 位之间。
    *   **电话校验**：使用正则匹配 `/^1[3-9]\d{9}$/`，必须为合法的 11 位手机号。
    *   **取餐时间**：仅限选择“今天/明天”的 `08:00 - 21:00` 营业时间段。且若选择的是今天，预约时间必须大于当前时间至少 `15` 分钟。若不符，拦截提交并弹出 toast 提示。
4.  **订单留言转换**：
    *   若是自取，在提交订单时，将取餐时间、称呼、电话、以及备注需求（过滤多行换行）拼接为结构化字符串，传入请求的 `message` 字段中：
        `[预约自取] 时间:今日 12:30 | 称呼:张女士 | 电话:13800138000 | 备注:少酱，打包` (若无备注需求，则显示 `无`)
    *   如果是外卖，照常传入收货地址 ID 且 `message` 为用户的普通备注留言。

### 3.4 支付与订单状态流转 (Payment & Order Flow)
*   **支付状态模拟与冲突覆写**：
    *   用户提交自取订单后，数据库中该订单的支付状态为 `101`（未付款）。前端提交订单成功后，直接携带参数 `status=true&orderId=XXX` 跳转至凭证小票页。
    *   **测试环境模拟付款机制**：在凭证页小票的底部提供一个仅用于演示目的的“模拟在线付款”按钮。点击后前端会向本地写入缓存：`uni.setStorageSync('mock_paid_order_' + orderId, true)`。
    *   **状态同步冲突解决（覆写机制）**：为防止凭证页和订单详情页、以及**订单列表页（`pages/ucenter/order/order.vue`）**请求后台时被返回的 `101` 真实状态覆盖，前端在读取任意位置的订单状态时，增加一层覆写逻辑：
        `if (order.orderStatus === 101 && uni.getStorageSync('mock_paid_order_' + order.id) === true) { order.orderStatus = 201; // 强行将对象状态更新为201已付款进行前台渲染 }`
        这样确保整个系统（列表页、详情页、小票页）对自提虚拟支付订单的流转感知绝对一致。
*   **超时控制机制**：在后台中我们已将未付款订单超时取消时间改为了 24 小时（1440分钟），防范了自动取消任务。

### 3.5 统一解析工具类与详情页改造
为了实现优雅降级并避免冗余代码，我们引入解析模块：

#### 统一解析工具类 (`src/utils/orderParser.js`)
在前端新建该文件，封装以下解析逻辑，**正则中“备注”字段采用多行模式正则组 `([\s\S]*?)` 以防换行失败**：
```javascript
/**
 * 解析订单备注中的预约自取信息
 * @param {String} message 订单备注留言
 * @returns {Object|null} 解析成功返回预约对象，匹配失败返回null
 */
export function parseOrderRemark(message) {
  if (!message || typeof message !== 'string') return null;
  try {
    if (message.indexOf('[预约自取]') === -1) {
      return null;
    }
    const reg = /\[预约自取\] 时间:(.*?) \| 称呼:(.*?) \| 电话:(.*?) \| 备注:([\s\S]*?)$/;
    const matches = message.match(reg);
    if (matches && matches.length >= 5) {
      return {
        isPickup: true,
        time: matches[1].trim(),
        name: matches[2].trim(),
        phone: matches[3].trim(),
        remark: matches[4].trim()
      };
    }
  } catch (e) {
    console.error('解析订单备注异常:', e);
  }
  return null;
}

/**
 * 根据订单ID生成取餐号（后三位余数，限制最大 1000 单每日防碰撞）
 * @param {Number|String} orderId 订单ID
 */
export function generatePickupCode(orderId) {
  const num = parseInt(orderId) || 0;
  return 'A' + String(num % 1000).padStart(3, '0');
}
```

#### 凭证页与订单详情页的调用及降级 (`pages/payResult` & `orderDetail`)
*   **凭证小票页 (`pages/payResult/payResult`)**：
    *   在页面加载时，获取传入的 `orderId`，主动发起 `/wx/order/detail?id=orderId` 请求加载详细订单数据，获取订单 `message` 留言内容与支付状态。
    *   执行上述“状态同步冲突解决覆写机制”判断支付展示状态。
    *   调用 `parseOrderRemark(message)` 进行解析。
    *   解析成功则渲染展示温暖日系“手作凭证小票”卡片样式。
    *   匹配失败则直接显示传统的“在线订单支付结果”界面（外卖订单普通视图）。
*   **订单详情页 (`pages/ucenter/orderDetail/orderDetail`)**：
    *   改造 `orderDetail.vue` 模板。
    *   在生命周期内执行“状态同步冲突解决覆写机制”纠正付款状态。
    *   顶部引入 `parseOrderRemark` 并在页面渲染时执行解析。
    *   若返回值非空，则在顶部新增醒目的“到店自提凭证”区块。展示利用 `generatePickupCode(order.id)` 生成的取餐号（如 `A012`），自取时间与客户联系电话、备注要求。
    *   若返回为空，则走原有的外卖地址配送物流追踪卡片，实现丝滑降级，确保常规订单详情页不会崩溃。

---

## 4. 验证方案 (Verification Plan)

### 4.1 数据一致性验证
1.  **菜单加载与二级分类过滤**：打开小程序首页，验证分类及三明治、简餐、饮料等 10 款商品是否能正常显示且图片加载无阻（传入 L2 分类过滤确保数据正常回显）。
2.  **折后价展示**：原价显示划线价，折后价作为现价。

### 4.2 购物车与 UI 切换验证
1.  点击商品列表的 `+`，验证底部购物车栏是否滑出，且金额正确。
2.  进入结算页，点击“到店自取”和“外卖配送”，验证收货地址栏与预约表单是否能够正确互斥显示。
3.  测试输入非法姓名、11位不合规手机号、包含换行的过去时间等，检验表单拦截提示与防重锁逻辑。

### 4.3 预约自提订单提交与凭证解析验证
1.  在用户无收货地址情况下，选择“到店自取”，填写正确信息。
2.  点击提交，验证是否在后台成功静默创建了包含 `areaCode` 为 `310115` 且 `name` 为“到店自提(小满食光)”的专用虚拟地址，成功创建订单。
3.  验证跳转至 `payResult` 页面是否成功发起详情查询请求，并在点击“模拟付款”后本地缓存标记成功。再次进入、刷新页面或返回**订单列表页（`order.vue`）**与**订单详情页（`orderDetail.vue`）**，验证解析订单备注后是否输出 `A` 开头的三位取餐号及自取凭证卡片，且三处状态展示均为“已付款”（状态 201）。
4.  在“我的订单”中进入 `orderDetail` 页面，验证是否能成功解析并以卡片形式展示取餐小票（包含备注信息与模拟支付成功状态），其余常规外卖订单能否正常加载。
