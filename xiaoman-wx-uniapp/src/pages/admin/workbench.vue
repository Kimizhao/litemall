<template>
  <view class="workbench-container">
    <!-- Top Bar (扫码核销与手输码暂隐藏)
    <view class="top-nav">
      <view class="nav-actions">
        <view class="scan-btn" @tap="onScanCode">
          <text class="scan-icon">📷</text>
          <text class="scan-text">扫码核销</text>
        </view>
        <view class="input-verify-btn" @tap="showManualVerifyModal = true">
          <text class="verify-btn-text">手输码</text>
        </view>
      </view>
    </view>
    -->

    <!-- Main Navigation Tabs -->
    <view class="main-tabs">
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 0 }" 
        @tap="switchMainTab(0)"
      >
        <text class="tab-label">订单流水</text>
        <text class="tab-badge" v-if="orderCounts.pendingTotal > 0">{{ orderCounts.pendingTotal }}</text>
      </view>
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 1 }" 
        @tap="switchMainTab(1)"
      >
        <text class="tab-label">餐品与类目</text>
      </view>
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 2 }" 
        @tap="switchMainTab(2)"
      >
        <text class="tab-label">门店与硬件</text>
      </view>
    </view>

    <!-- TAB 1: 订单流水 -->
    <view v-if="currentTab === 0" class="tab-content order-module">
      <!-- Order Sub-Tabs -->
      <view class="sub-tabs">
        <view 
          class="sub-tab-item" 
          :class="{ active: orderFilterType === 1 }" 
          @tap="switchOrderFilter(1)"
        >
          <text>待制作</text>
          <text class="sub-badge" v-if="orderCounts.making > 0">{{ orderCounts.making }}</text>
        </view>
        <view 
          class="sub-tab-item" 
          :class="{ active: orderFilterType === 2 }" 
          @tap="switchOrderFilter(2)"
        >
          <text>待核销</text>
          <text class="sub-badge" v-if="orderCounts.pickup > 0">{{ orderCounts.pickup }}</text>
        </view>
        <view 
          class="sub-tab-item" 
          :class="{ active: orderFilterType === 4 }" 
          @tap="switchOrderFilter(4)"
        >
          <text>待退款</text>
          <text class="sub-badge refund-badge" v-if="orderCounts.refund > 0">{{ orderCounts.refund }}</text>
        </view>
        <view 
          class="sub-tab-item" 
          :class="{ active: orderFilterType === 3 }" 
          @tap="switchOrderFilter(3)"
        >
          <text>已完成</text>
        </view>
        <view 
          class="sub-tab-item" 
          :class="{ active: orderFilterType === 0 }" 
          @tap="switchOrderFilter(0)"
        >
          <text>全部</text>
        </view>
      </view>

      <!-- 待制作子分类开关: [今日出餐] vs [明日预订] (备料模式) -->
      <view class="scope-toggle-bar" v-if="orderFilterType === 1">
        <view 
          class="scope-chip" 
          :class="{ active: fulfillmentScope === 'today' }" 
          @tap="switchFulfillmentScope('today')"
        >
          <text class="scope-icon">🍳</text>
          <text class="scope-title">今日出餐</text>
          <text class="scope-badge" v-if="orderCounts.todayMaking > 0">{{ orderCounts.todayMaking }}</text>
        </view>
        <view 
          class="scope-chip chip-tomorrow" 
          :class="{ active: fulfillmentScope === 'tomorrow' }" 
          @tap="switchFulfillmentScope('tomorrow')"
        >
          <text class="scope-icon">🌙</text>
          <text class="scope-title">明日预订</text>
          <text class="scope-badge" v-if="orderCounts.tomorrowMaking > 0">{{ orderCounts.tomorrowMaking }}</text>
        </view>
        <view 
          class="scope-chip" 
          :class="{ active: fulfillmentScope === 'all' }" 
          @tap="switchFulfillmentScope('all')"
        >
          <text class="scope-title">全部待制</text>
        </view>
      </view>

      <!-- Batch Action Bar (待制作/待核销，仅今日出餐与待核销展示) -->
      <view class="order-batch-bar" v-if="(orderFilterType === 1 && fulfillmentScope === 'today') || orderFilterType === 2">
        <view class="batch-bar-left">
          <text class="batch-bar-hint">
            {{ orderFilterType === 1 ? '今日待制作工单' : '待核销订单' }} ({{ orderFilterType === 1 ? orderList.length : orderCounts.pickup }})
          </text>
        </view>
        <view class="batch-bar-right">
          <button 
            class="batch-action-btn btn-ready" 
            v-if="orderFilterType === 1" 
            @tap="onBatchReady"
          >
            一键出餐
          </button>
          <button 
            class="batch-action-btn btn-verify" 
            v-if="orderFilterType === 2" 
            @tap="onBatchVerify"
          >
            一键核销
          </button>
        </view>
      </view>

      <!-- Order List -->
      <scroll-view scroll-y class="order-list-scroll" refresher-enabled :refresher-triggered="isRefreshing" @refresherrefresh="onRefreshOrders">
        <view v-if="orderList.length === 0" class="empty-state">
          <text class="empty-icon">☕</text>
          <text class="empty-text">当前暂无对应订单</text>
        </view>

        <view class="order-card" v-for="order in orderList" :key="order.id" @tap="openOrderDetail(order)" @longpress="onOrderLongPress(order)">
          <view class="card-header">
            <view class="order-sn-col">
              <text class="order-sn-text">单号：{{ order.orderSn }}</text>
            </view>
            <view class="card-header-right">
              <text class="fulfillment-badge" v-if="order.fulfillmentDate">📅 {{ order.fulfillmentDate }}</text>
              <text class="status-badge" :class="'status-' + order.orderStatus">{{ order.orderStatusText }}</text>
              <text class="view-detail-arrow">›</text>
            </view>
          </view>

          <!-- Goods preview list -->
          <view class="order-goods-list">
            <view class="goods-row" v-for="g in order.goodsList" :key="g.id">
              <image class="goods-thumb" :src="g.picUrl || '/static/images/default_goods.png'" mode="aspectFill" />
              <view class="goods-info">
                <text class="goods-title">{{ g.goodsName }}</text>
                <text class="goods-spec" v-if="g.specifications && g.specifications.length">{{ g.specifications.join(' / ') }}</text>
              </view>
              <view class="goods-meta">
                <text class="goods-price">¥{{ g.price }}</text>
                <text class="goods-num">x{{ g.number }}</text>
              </view>
            </view>
          </view>

          <!-- Order remarks -->
          <view class="order-remark-box" v-if="order.message">
            <text class="remark-label">顾客备注：</text>
            <text class="remark-val">{{ order.message }}</text>
          </view>

          <!-- Footer details and actions -->
          <view class="card-footer">
            <view class="price-time-box">
              <text class="order-time">{{ formatTime(order.addTime) }}</text>
              <text class="total-price">实付: <text class="price-val">¥{{ order.actualPrice }}</text></text>
            </view>

            <view class="card-actions">
              <!-- If Pending Making (201) -> Out/Ready Button -->
              <button 
                class="action-btn primary-btn" 
                v-if="order.orderStatus === 201" 
                @tap.stop="onMarkReady(order.id)"
              >
                出餐
              </button>

              <!-- If Ready for Pickup (301) -> Verify Button -->
              <button 
                class="action-btn primary-btn highlight" 
                v-if="order.orderStatus === 301" 
                @tap.stop="onVerifyOrder(order.id)"
              >
                核销完成
              </button>

              <!-- If Pending Refund (202) -> Review Refund -->
              <button 
                class="action-btn refund-btn" 
                v-if="order.orderStatus === 202" 
                @tap.stop="openRefundModal(order)"
              >
                审核退款
              </button>
            </view>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- TAB 2: 餐品与类目 -->
    <view v-if="currentTab === 1" class="tab-content goods-module">
      <!-- 1. Dedicated Action Bar (始终置顶显示新增餐品与类目管理，高对比度防挤压) -->
      <view class="goods-action-bar">
        <view class="bar-left">
          <text class="bar-title">餐品与类目</text>
          <text class="bar-count" v-if="goodsList.length">共 {{ goodsList.length }} 款餐品</text>
        </view>
        <view class="bar-right">
          <view class="action-btn-pill secondary" @tap="openCategoryModal">
            <text class="btn-text">类目管理</text>
          </view>
          <view class="action-btn-pill primary" @tap="openGoodsFormModal(null)">
            <text class="btn-text">新增餐品</text>
          </view>
        </view>
      </view>

      <!-- 2. 一级类目横向栏 (Level 1 Categories) -->
      <view class="l1-category-bar">
        <scroll-view scroll-x class="l1-scroll" :show-scrollbar="false">
          <view class="l1-scroll-inner">
            <view 
              class="l1-pill" 
              :class="{ active: selectedL1Id === null }" 
              @tap="onSelectL1Category(null)"
            >
              全部
            </view>
            <view 
              class="l1-pill" 
              v-for="cat in categoryTree" 
              :key="cat.id" 
              :class="{ active: selectedL1Id === cat.id }" 
              @tap="onSelectL1Category(cat.id)"
            >
              {{ cat.name }}
              <text class="l1-badge" v-if="cat.children && cat.children.length">{{ cat.children.length }}</text>
            </view>
            <view class="l1-add-pill" @tap="openEditCategoryModal(null, 'L1')">
              <text>➕ 新增一级</text>
            </view>
          </view>
        </scroll-view>
      </view>

      <!-- 3. 二级类目细分栏 (Level 2 Sub-categories - 当选中具体一级分类时展示) -->
      <view class="l2-category-bar" v-if="selectedL1Id !== null">
        <view class="l2-header">
          <view class="l2-header-left">
            <text class="l2-label">二级细分：</text>
            <text class="l2-parent-name">{{ activeL1Category ? activeL1Category.name : '' }}</text>
          </view>
          <view class="l2-header-right">
            <view class="l2-action-link" @tap="openEditCategoryModal(activeL1Category, 'L1')">
              <text>✏️ 编辑一级类目</text>
            </view>
            <view class="l2-action-link highlight" v-if="selectedL2Id" @tap="openEditSelectedL2Modal">
              <text>✏️ 编辑当前子类</text>
            </view>
          </view>
        </view>

        <scroll-view scroll-x class="l2-scroll" :show-scrollbar="false">
          <view class="l2-scroll-inner">
            <view 
              class="l2-pill" 
              :class="{ active: selectedL2Id === null }" 
              @tap="onSelectL2Category(null)"
            >
              全部
            </view>
            <view 
              class="l2-pill" 
              v-for="sub in activeSubCategories" 
              :key="sub.id" 
              :class="{ active: selectedL2Id === sub.id }" 
              @tap="onSelectL2Category(sub.id)"
            >
              {{ sub.name }}
            </view>
            <view class="l2-add-pill" @tap="openAddSubCategoryModal(activeL1Category)">
              <text>➕ 新增子类</text>
            </view>
          </view>
        </scroll-view>
      </view>

      <!-- 4. 商品列表 (Goods List) -->
      <scroll-view scroll-y class="goods-list-scroll" refresher-enabled :refresher-triggered="isRefreshingGoods" @refresherrefresh="onRefreshGoods">
        <view v-if="goodsList.length === 0" class="empty-state">
          <text class="empty-icon">🥪</text>
          <text class="empty-text">当前分类暂无餐品</text>
          <button class="empty-add-btn" @tap="openGoodsFormModal(null)">➕ 立即添加餐品</button>
        </view>

        <view class="goods-card" v-for="item in goodsList" :key="item.id" @tap="openGoodsFormModal(item)">
          <image class="goods-cover" :src="item.picUrl || '/static/images/default_goods.png'" mode="aspectFill" />
          <view class="goods-main-info">
            <view class="goods-head-row">
              <text class="goods-name">{{ item.name }}</text>
              <text class="soldout-tag" v-if="item.isSoldOut">已售罄</text>
            </view>
            <view class="goods-cat-tag-box" v-if="getCategoryNameById(item.categoryId)">
              <text class="goods-cat-tag">🏷️ {{ getCategoryNameById(item.categoryId) }}</text>
            </view>
            <text class="goods-brief" v-if="item.brief">{{ item.brief }}</text>
            
            <view class="goods-price-stock-row">
              <text class="price-tag">¥{{ item.retailPrice }}</text>
              <text class="stock-tag">剩余库存: {{ item.totalStock }}</text>
            </view>
          </view>

          <!-- Quick Actions on Goods Card -->
          <view class="goods-card-actions">
            <view class="toggle-group">
              <view class="toggle-item" @tap.stop="onToggleSale(item)">
                <text class="toggle-label">{{ item.isOnSale ? '在售' : '已下架' }}</text>
                <switch :checked="item.isOnSale" color="#6F4E37" style="transform: scale(0.65)" />
              </view>
            </view>
            <view class="action-btn-row">
              <button class="mini-btn edit-btn" @tap.stop="openGoodsFormModal(item)">编辑</button>
              <button class="mini-btn del-btn" @tap.stop="onDeleteGoods(item)">删除</button>
            </view>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- TAB 3: 门店与硬件 -->
    <view v-if="currentTab === 2" class="tab-content dashboard-module">
      <!-- Performance Cards -->
      <view class="perf-section">
        <!-- 上层: 今日履约出餐 (后厨运营) -->
        <text class="section-title">🍳 今日履约出餐</text>
        <view class="perf-grid">
          <view class="perf-card highlight-card">
            <text class="perf-val highlight-val">¥{{ dashboardData.stats.todayFulfillmentTurnover || dashboardData.stats.todayTurnover || 0 }}</text>
            <text class="perf-label">今日出餐货值</text>
          </view>
          <view class="perf-card">
            <text class="perf-val">{{ dashboardData.stats.todayFulfillmentOrderCount || dashboardData.stats.todayOrderCount || 0 }}</text>
            <text class="perf-label">今日计划出餐单</text>
          </view>
          <view class="perf-card">
            <text class="perf-val" style="color: #E07A5F;">{{ dashboardData.stats.todayMakingCount || 0 }}</text>
            <text class="perf-label">待制作餐品</text>
          </view>
          <view class="perf-card">
            <text class="perf-val">{{ dashboardData.stats.todayPickupCount || 0 }}</text>
            <text class="perf-label">待提货订单</text>
          </view>
        </view>

        <!-- 明日预订备料提示条 -->
        <view class="tomorrow-preorder-card" v-if="dashboardData.stats.tomorrowPreorderCount > 0">
          <view class="preorder-card-left">
            <text class="preorder-card-icon">🌙</text>
            <view class="preorder-card-info">
              <text class="preorder-card-title">明日预订单 (夜间备料)</text>
              <text class="preorder-card-desc">已有 {{ dashboardData.stats.tomorrowPreorderCount }} 笔预定明日出餐</text>
            </view>
          </view>
          <text class="preorder-card-val">¥{{ dashboardData.stats.tomorrowPreorderTurnover || 0 }}</text>
        </view>

        <!-- 下层: 今日收银流水 (对账财务) -->
        <text class="section-title cashflow-title">💰 今日收银流水</text>
        <view class="perf-grid">
          <view class="perf-card">
            <text class="perf-val">¥{{ dashboardData.stats.todayCashTurnover || 0 }}</text>
            <text class="perf-label">今日微信实收</text>
          </view>
          <view class="perf-card">
            <text class="perf-val">{{ dashboardData.stats.todayPaidOrderCount || 0 }}</text>
            <text class="perf-label">今日付款单数</text>
          </view>
          <view class="perf-card">
            <text class="perf-val" :style="{ color: dashboardData.stats.todayRefundAmount > 0 ? '#C0392B' : '#6F4E37' }">¥{{ dashboardData.stats.todayRefundAmount || 0 }}</text>
            <text class="perf-label">今日退款金额</text>
          </view>
          <view class="perf-card">
            <text class="perf-val" :style="{ color: dashboardData.stats.pendingRefundCount > 0 ? '#E07A5F' : '#6F4E37' }">{{ dashboardData.stats.pendingRefundCount || 0 }}</text>
            <text class="perf-label">待处理退款</text>
          </view>
        </view>
      </view>

      <!-- Cloud Printer Status Card -->
      <view class="printer-section">
        <text class="section-title">🖨️ 云打印机</text>
        <view class="printer-card">
          <view class="printer-header">
            <view class="printer-info">
              <text class="printer-name">{{ dashboardData.printer.name || '前台出票机' }}</text>
              <text class="printer-sn" v-if="dashboardData.printer.sn">SN: {{ dashboardData.printer.sn }}</text>
            </view>
            <view class="printer-status-badge" :class="dashboardData.printer.online === 1 ? 'online' : 'offline'">
              <text class="status-dot"></text>
              <text class="status-text">{{ dashboardData.printer.online === 1 ? '在线' : '离线' }}</text>
            </view>
          </view>

          <view class="printer-actions">
            <button class="test-print-btn" :loading="isPrintingTest" @tap="onTestPrint">
              测试打印
            </button>
          </view>
        </view>
      </view>

      <!-- Operator & Security Section -->
      <view class="operator-section">
        <text class="section-title">👤 当前账号</text>
        <view class="operator-card">
          <view class="operator-info-row">
            <text class="op-label">登录账号：</text>
            <text class="op-val">xiaoman</text>
          </view>
          <view class="operator-info-row">
            <text class="op-label">绑定状态：</text>
            <text class="op-val">已绑定</text>
          </view>
          
          <button class="unbind-btn" @tap="onUnbindAdmin">
            退出工作台
          </button>
        </view>
      </view>
    </view>

    <!-- MODAL 1: 手输提货码核销弹窗 -->
    <view class="modal-mask" v-if="showManualVerifyModal" @tap="showManualVerifyModal = false">
      <view class="modal-card" @tap.stop>
        <view class="modal-header">
          <text class="modal-title">手输提货码核销</text>
          <text class="modal-close" @tap="showManualVerifyModal = false">×</text>
        </view>
        <view class="modal-body">
          <text class="input-desc">请输入顾客订单号后 6 位取餐码或完整订单号：</text>
          <input 
            class="modal-input" 
            v-model="manualVerifyCode" 
            type="number" 
            placeholder="例如: 231094" 
            maxlength="32" 
          />
        </view>
        <view class="modal-footer">
          <button class="cancel-modal-btn" @tap="showManualVerifyModal = false">取消</button>
          <button class="confirm-modal-btn" @tap="onConfirmManualVerify">确认核销</button>
        </view>
      </view>
    </view>

    <!-- MODAL 2: 审核退款弹窗 -->
    <view class="modal-mask" v-if="showRefundModal" @tap="showRefundModal = false">
      <view class="modal-card" @tap.stop>
        <view class="modal-header">
          <text class="modal-title">审核订单退款</text>
          <text class="modal-close" @tap="showRefundModal = false">×</text>
        </view>
        <view class="modal-body">
          <view class="refund-detail" v-if="currentRefundOrder">
            <text class="refund-item-text">订单编号：{{ currentRefundOrder.orderSn }}</text>
            <text class="refund-item-text">退款金额：¥{{ currentRefundOrder.actualPrice }}</text>
            <text class="refund-item-text" v-if="currentRefundOrder.message">备注：{{ currentRefundOrder.message }}</text>
          </view>
          <input 
            class="modal-input" 
            v-model="refundRejectReason" 
            placeholder="若拒绝退款，请输入拒绝原因" 
          />
        </view>
        <view class="modal-footer">
          <button class="reject-btn" @tap="onSubmitRefund(false)">拒绝退款</button>
          <button class="agree-btn" @tap="onSubmitRefund(true)">同意原路退款</button>
        </view>
      </view>
    </view>

    <!-- MODAL 3: 餐品轻量新增/编辑弹窗 -->
    <view class="modal-mask" v-if="showGoodsModal" @tap="showGoodsModal = false">
      <view class="modal-card large-modal" @tap.stop>
        <view class="modal-header">
          <text class="modal-title">{{ goodsForm.id ? '编辑餐品' : '新增餐品' }}</text>
          <text class="modal-close" @tap="showGoodsModal = false">×</text>
        </view>
        <scroll-view scroll-y class="modal-scroll-body">
          <view class="form-row">
            <text class="form-label">餐品主图</text>
            <view class="image-uploader" @tap="onChooseGoodsImage">
              <template v-if="goodsForm.picUrl">
                <image :src="goodsForm.picUrl" class="upload-preview" mode="aspectFill" @tap.stop="onChooseGoodsImage" />
                <view class="upload-mask" @tap.stop="onChooseGoodsImage">
                  <text class="mask-text">点击更换</text>
                </view>
              </template>
              <view v-else class="upload-placeholder" @tap.stop="onChooseGoodsImage">
                <text class="upload-icon">📷</text>
                <text class="upload-hint">上传图片</text>
              </view>
              <view class="uploader-tap-overlay" @tap.stop="onChooseGoodsImage"></view>
            </view>
          </view>

          <view class="form-row">
            <text class="form-label">餐品名称 *</text>
            <input class="modal-input" v-model="goodsForm.name" placeholder="例如: 烟熏培根三明治" />
          </view>

          <view class="form-row">
            <text class="form-label">所属分类 *</text>
            <picker :range="selectableCategories" range-key="name" :value="categoryPickerIndex" @change="onCategoryPickerChange">
              <view class="picker-display">
                <text>{{ selectedCategoryName || '点击选择分类' }}</text>
                <text class="picker-arrow">▾</text>
              </view>
            </picker>
          </view>

          <!-- 规格模式切换 (标准规格 vs 多规格) -->
          <view class="form-row">
            <text class="form-label">规格模式 *</text>
            <view class="spec-type-tabs">
              <view 
                class="spec-type-tab" 
                :class="{ active: goodsForm.specType === 'single' }" 
                @tap="switchSpecType('single')"
              >
                <text class="tab-radio-circle"></text>
                <text class="tab-text">标准规格 (单规格)</text>
              </view>
              <view 
                class="spec-type-tab" 
                :class="{ active: goodsForm.specType === 'multi' }" 
                @tap="switchSpecType('multi')"
              >
                <text class="tab-radio-circle"></text>
                <text class="tab-text">多规格配置</text>
              </view>
            </view>
          </view>

          <!-- 模式一：标准单规格 -->
          <template v-if="goodsForm.specType === 'single'">
            <view class="form-row">
              <text class="form-label">餐品售价 (元) *</text>
              <input class="modal-input" v-model="goodsForm.retailPrice" type="digit" placeholder="例如: 12.80" />
            </view>
            <view class="form-row">
              <text class="form-label">当前总库存 *</text>
              <input class="modal-input" v-model="goodsForm.stock" type="number" placeholder="例如: 100" />
            </view>
          </template>

          <!-- 模式二：多规格配置 -->
          <template v-else>
            <view class="multi-spec-container">
              <view class="multi-spec-header">
                <text class="multi-spec-title">规格明细与价格库存</text>
                <text class="multi-spec-tip">每个规格拥有独立的售价与库存</text>
              </view>

              <!-- 快捷预设标签 -->
              <view class="preset-tag-bar">
                <text class="preset-label">快捷添加：</text>
                <scroll-view scroll-x class="preset-scroll" :show-scrollbar="false">
                  <view class="preset-scroll-inner">
                    <view 
                      class="preset-pill" 
                      v-for="preset in specPresets" 
                      :key="preset" 
                      @tap="addPresetSpec(preset)"
                    >
                      + {{ preset }}
                    </view>
                  </view>
                </scroll-view>
              </view>

              <!-- 规格 SKU 列表 -->
              <view class="spec-items-list">
                <view class="spec-sku-card" v-for="(sku, index) in multiSpecList" :key="index">
                  <view class="sku-card-top">
                    <text class="sku-seq-num">规格项 {{ index + 1 }}</text>
                    <view class="sku-del-btn" @tap="removeSpecItem(index)" v-if="multiSpecList.length > 1">
                      <text class="del-icon">✕</text>
                      <text class="del-text">删除</text>
                    </view>
                  </view>
                  <view class="sku-fields-row">
                    <view class="sku-field name-field">
                      <text class="field-sublabel">规格名称 *</text>
                      <input 
                        class="sku-modal-input" 
                        v-model="sku.specName" 
                        placeholder="如: 大杯 / 全麦" 
                      />
                    </view>
                    <view class="sku-field price-field">
                      <text class="field-sublabel">售价 (元) *</text>
                      <input 
                        class="sku-modal-input price" 
                        v-model="sku.price" 
                        type="digit" 
                        placeholder="0.00" 
                      />
                    </view>
                    <view class="sku-field num-field">
                      <text class="field-sublabel">库存 *</text>
                      <input 
                        class="sku-modal-input stock" 
                        v-model="sku.number" 
                        type="number" 
                        placeholder="50" 
                      />
                    </view>
                  </view>
                </view>
              </view>

              <!-- 添加规格项按钮 -->
              <view class="add-spec-row-btn" @tap="addEmptySpecItem">
                <text class="add-icon">➕</text>
                <text class="add-text">添加一个规格</text>
              </view>

              <!-- 汇总统计提示 -->
              <view class="multi-spec-summary" v-if="multiSpecList.length > 0">
                <text class="summary-text">
                  共 {{ multiSpecList.length }} 个规格，起步价: 
                  <text class="summary-val">¥{{ computedMinPrice }}</text>
                  ，合计库存: 
                  <text class="summary-val">{{ computedTotalStock }}</text> 件
                </text>
              </view>
            </view>
          </template>

          <view class="form-row">
            <text class="form-label">简短描述</text>
            <input class="modal-input" v-model="goodsForm.brief" placeholder="例如: 现烤全麦面包夹厚切培根" />
          </view>

          <view class="form-row toggle-row">
            <text class="form-label">是否上架销售</text>
            <switch :checked="goodsForm.isOnSale" color="#6F4E37" @change="e => goodsForm.isOnSale = e.detail.value" />
          </view>
        </scroll-view>

        <view class="modal-footer">
          <button class="cancel-modal-btn" @tap="showGoodsModal = false">取消</button>
          <button class="confirm-modal-btn" :loading="isSavingGoods" @tap="onSubmitGoodsForm">保存餐品</button>
        </view>
      </view>
    </view>

    <!-- MODAL 4: 类目管理弹窗 -->
    <view class="modal-mask" v-if="showCategoryModal" @tap="showCategoryModal = false">
      <view class="modal-card large-modal" @tap.stop>
        <view class="modal-header">
          <text class="modal-title">类目管理与维护</text>
          <text class="modal-close" @tap="showCategoryModal = false">×</text>
        </view>
        
        <scroll-view scroll-y class="modal-scroll-body">
          <view class="add-cat-bar">
            <button class="small-add-btn" @tap="openEditCategoryModal(null, 'L1')">➕ 新增一级主类目</button>
          </view>

          <view class="cat-empty-hint" v-if="categoryTree.length === 0">
            <text class="empty-icon">📁</text>
            <text class="empty-text">暂无商品类目，请点击上方按钮新增</text>
          </view>

          <view class="category-tree" v-else>
            <view class="tree-node" v-for="l1 in categoryTree" :key="l1.id">
              <view class="l1-row">
                <view class="node-left">
                  <text class="level-tag l1">一级</text>
                  <text class="node-name">{{ l1.name }}</text>
                  <text class="sort-order-badge">排序: {{ l1.sortOrder != null ? l1.sortOrder : 50 }}</text>
                </view>
                <view class="node-actions">
                  <text class="node-action-btn add-sub" @tap="openAddSubCategoryModal(l1)">+子类</text>
                  <text class="node-action-btn" @tap="openEditCategoryModal(l1, 'L1')">编辑</text>
                  <text class="node-action-btn del" @tap="onDeleteCategory(l1)">删除</text>
                </view>
              </view>

              <!-- L2 Children -->
              <view class="l2-container" v-if="l1.children && l1.children.length">
                <view class="l2-row" v-for="l2 in l1.children" :key="l2.id">
                  <view class="node-left">
                    <text class="level-tag l2">二级</text>
                    <text class="node-name sub-name">{{ l2.name }}</text>
                    <text class="sort-order-badge">排序: {{ l2.sortOrder != null ? l2.sortOrder : 50 }}</text>
                  </view>
                  <view class="node-actions">
                    <text class="node-action-btn" @tap="openEditCategoryModal(l2, 'L2')">编辑</text>
                    <text class="node-action-btn del" @tap="onDeleteCategory(l2)">删除</text>
                  </view>
                </view>
              </view>
              <view class="l2-empty-container" v-else>
                <text class="l2-empty-tip">暂无二级子类，可点击上方「+子类」添加</text>
              </view>
            </view>
          </view>
        </scroll-view>

        <view class="modal-footer">
          <button class="confirm-modal-btn" @tap="showCategoryModal = false">完成关闭</button>
        </view>
      </view>
    </view>

    <!-- MODAL 5: 单个类目新建/编辑小弹窗 -->
    <view class="modal-mask" v-if="showSingleCatModal" @tap="showSingleCatModal = false">
      <view class="modal-card" @tap.stop>
        <view class="modal-header">
          <text class="modal-title">{{ catModalTitle }}</text>
          <text class="modal-close" @tap="showSingleCatModal = false">×</text>
        </view>
        <view class="modal-body">
          <view class="form-row">
            <text class="form-label">类目级别</text>
            <text class="cat-level-desc">{{ catForm.level === 'L1' ? '一级主类目' : '二级细分类目' }}</text>
          </view>
          <view class="form-row" v-if="catForm.level === 'L2'">
            <text class="form-label">所属主类目</text>
            <text class="cat-parent-desc">{{ getParentCategoryName(catForm.pid) }}</text>
          </view>
          <view class="form-row">
            <text class="form-label">类目名称 *</text>
            <input class="modal-input" v-model="catForm.name" placeholder="请输入分类名称，如: 手作三明治" />
          </view>
          <view class="form-row">
            <text class="form-label">显示排序</text>
            <input class="modal-input" v-model="catForm.sortOrder" type="number" placeholder="数字越小越靠前 (默认50)" />
          </view>
        </view>
        <view class="modal-footer">
          <button class="cancel-modal-btn" @tap="showSingleCatModal = false">取消</button>
          <button class="confirm-modal-btn" :loading="isSavingCategory" @tap="onSubmitCategoryForm">保存</button>
        </view>
      </view>
    </view>

    <!-- MODAL 6: 订单详情弹窗 -->
    <view class="modal-mask" v-if="showOrderDetailModal" @tap="showOrderDetailModal = false">
      <view class="modal-card large-modal order-detail-modal" @tap.stop>
        <view class="modal-header">
          <view class="header-left">
            <text class="modal-title">订单详情</text>
            <text 
              class="status-badge" 
              :class="'status-' + (orderDetailData && orderDetailData.order ? orderDetailData.order.orderStatus : '')"
              v-if="orderDetailData && orderDetailData.orderStatusText"
            >
              {{ orderDetailData.orderStatusText }}
            </text>
          </view>
          <text class="modal-close" @tap="showOrderDetailModal = false">×</text>
        </view>

        <scroll-view scroll-y class="modal-scroll-body order-detail-scroll" v-if="orderDetailData && orderDetailData.order">
          <!-- 取餐码横幅 -->
          <view class="detail-pickup-banner">
            <view class="banner-code-col">
              <text class="banner-label">取餐码</text>
              <text class="banner-code">#{{ orderDetailData.pickupCode || (orderDetailData.order.orderSn ? orderDetailData.order.orderSn.slice(-6) : '') }}</text>
            </view>
            <view class="banner-status-col">
              <text class="banner-order-type">{{ orderDetailData.order.shipChannel || '到店自提' }}</text>
            </view>
          </view>

          <!-- 顾客备注 (如果存在) -->
          <view class="detail-section" v-if="orderDetailData.order.message">
            <text class="section-title">顾客备注</text>
            <view class="remark-content-box">
              <text class="remark-text">{{ orderDetailData.order.message }}</text>
            </view>
          </view>

          <!-- 顾客信息 -->
          <view class="detail-section">
            <text class="section-title">顾客信息</text>
            <view class="detail-info-card">
              <view class="info-line" v-if="orderDetailData.order.consignee || (orderDetailData.user && orderDetailData.user.nickname)">
                <text class="info-k">顾客姓名</text>
                <text class="info-v">{{ orderDetailData.order.consignee || (orderDetailData.user ? orderDetailData.user.nickname : '') || '微信顾客' }}</text>
              </view>
              <view class="info-line" v-if="orderDetailData.order.mobile || (orderDetailData.user && orderDetailData.user.mobile)">
                <text class="info-k">联系电话</text>
                <view class="phone-box">
                  <text class="info-v">{{ orderDetailData.order.mobile || orderDetailData.user.mobile }}</text>
                  <text class="call-btn" @tap="callPhone(orderDetailData.order.mobile || orderDetailData.user.mobile)">拨打</text>
                </view>
              </view>
              <view class="info-line" v-if="orderDetailData.order.address">
                <text class="info-k">提货/地址</text>
                <text class="info-v">{{ orderDetailData.order.address }}</text>
              </view>
            </view>
          </view>

          <!-- 商品明细 -->
          <view class="detail-section">
            <view class="section-header-row">
              <text class="section-title">商品明细</text>
              <text class="section-subtitle">共 {{ orderDetailData.orderGoods ? orderDetailData.orderGoods.length : 0 }} 种</text>
            </view>
            <view class="detail-goods-list">
              <view class="detail-goods-item" v-for="g in orderDetailData.orderGoods" :key="g.id">
                <image class="detail-goods-thumb" :src="g.picUrl || '/static/images/default_goods.png'" mode="aspectFill" />
                <view class="detail-goods-info">
                  <text class="detail-goods-name">{{ g.goodsName }}</text>
                  <text class="detail-goods-spec" v-if="g.specifications && g.specifications.length">
                    {{ Array.isArray(g.specifications) ? g.specifications.join(' / ') : g.specifications }}
                  </text>
                </view>
                <view class="detail-goods-calc">
                  <text class="detail-goods-price">¥{{ g.price }}</text>
                  <text class="detail-goods-num">x{{ g.number }}</text>
                  <text class="detail-goods-subtotal">¥{{ (g.price * g.number).toFixed(2) }}</text>
                </view>
              </view>
            </view>
          </view>

          <!-- 费用结算 -->
          <view class="detail-section">
            <text class="section-title">结算明细</text>
            <view class="detail-info-card fee-card">
              <view class="fee-line">
                <text class="fee-k">商品总额</text>
                <text class="fee-v">¥{{ orderDetailData.order.goodsPrice }}</text>
              </view>
              <view class="fee-line" v-if="orderDetailData.order.couponPrice && orderDetailData.order.couponPrice > 0">
                <text class="fee-k">优惠抵扣</text>
                <text class="fee-v discount">-¥{{ orderDetailData.order.couponPrice }}</text>
              </view>
              <view class="fee-line" v-if="orderDetailData.order.freightPrice && orderDetailData.order.freightPrice > 0">
                <text class="fee-k">打包/配送费</text>
                <text class="fee-v">¥{{ orderDetailData.order.freightPrice }}</text>
              </view>
              <view class="fee-line total-line">
                <text class="total-k">实付金额</text>
                <text class="total-v">¥{{ orderDetailData.order.actualPrice }}</text>
              </view>
            </view>
          </view>

          <!-- 订单基本信息 -->
          <view class="detail-section">
            <text class="section-title">订单信息</text>
            <view class="detail-info-card">
              <view class="info-line">
                <text class="info-k">订单编号</text>
                <view class="sn-box">
                  <text class="info-v">{{ orderDetailData.order.orderSn }}</text>
                  <text class="copy-sn-btn" @tap="copyText(orderDetailData.order.orderSn)">复制</text>
                </view>
              </view>
              <view class="info-line">
                <text class="info-k">下单时间</text>
                <text class="info-v">{{ formatFullTime(orderDetailData.order.addTime) }}</text>
              </view>
              <view class="info-line" v-if="orderDetailData.order.payTime">
                <text class="info-k">支付时间</text>
                <text class="info-v">{{ formatFullTime(orderDetailData.order.payTime) }}</text>
              </view>
              <view class="info-line" v-if="orderDetailData.order.shipTime">
                <text class="info-k">出餐时间</text>
                <text class="info-v">{{ formatFullTime(orderDetailData.order.shipTime) }}</text>
              </view>
              <view class="info-line" v-if="orderDetailData.order.confirmTime">
                <text class="info-k">完成时间</text>
                <text class="info-v">{{ formatFullTime(orderDetailData.order.confirmTime) }}</text>
              </view>
            </view>
          </view>
        </scroll-view>

        <!-- 底部快捷动作 -->
        <view class="modal-footer detail-modal-footer">
          <button class="footer-btn secondary" @tap="onReprintOrder(orderDetailData.order)">补打小票</button>
          
          <button 
            class="footer-btn primary" 
            v-if="orderDetailData && orderDetailData.order && orderDetailData.order.orderStatus === 201" 
            @tap="onMarkReady(orderDetailData.order.id)"
          >
            出餐
          </button>

          <button 
            class="footer-btn primary highlight" 
            v-if="orderDetailData && orderDetailData.order && orderDetailData.order.orderStatus === 301" 
            @tap="onVerifyOrder(orderDetailData.order.id)"
          >
            核销完成
          </button>

          <button 
            class="footer-btn refund" 
            v-if="orderDetailData && orderDetailData.order && orderDetailData.order.orderStatus === 202" 
            @tap="openRefundModalFromDetail"
          >
            审核退款
          </button>

          <button class="footer-btn close" @tap="showOrderDetailModal = false">关闭</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { onShow, onHide, onPullDownRefresh } from '@dcloudio/uni-app'
import request, { fixImageUrl } from '../../utils/request'
import api from '../../config/api'

// Navigation Tab State
const currentTab = ref(0) // 0: 订单流水, 1: 商品与类目, 2: 门店与硬件

// Order Sub-Tabs & Filter
const orderFilterType = ref(1) // 1: 待制作, 2: 待取餐, 4: 待退款, 3: 已完成, 0: 全部
const fulfillmentScope = ref('today') // 'today': 今日出餐, 'tomorrow': 明日预订, 'all': 全部
const orderList = ref([])
const isRefreshing = ref(false)
const orderCounts = reactive({
  making: 0,
  todayMaking: 0,
  tomorrowMaking: 0,
  pickup: 0,
  refund: 0,
  pendingTotal: 0
})

// Goods & Category State
const goodsList = ref([])
const isRefreshingGoods = ref(false)
const selectedL1Id = ref(null) // null = 全部
const selectedL2Id = ref(null) // null = 该一级类目下的全部
const selectedCategoryId = ref(null)
const categoryTree = ref([])
const flatCategories = ref([])
const isSavingCategory = ref(false)

// Active L1 category object
const activeL1Category = computed(() => {
  if (!selectedL1Id.value) return null
  return categoryTree.value.find(c => c.id === selectedL1Id.value) || null
})

// Active L2 sub-categories of active L1
const activeSubCategories = computed(() => {
  if (!activeL1Category.value || !activeL1Category.value.children) return []
  return activeL1Category.value.children
})

// Dynamic title for Category Form Modal
const catModalTitle = computed(() => {
  if (catForm.id) {
    return catForm.level === 'L1' ? '编辑一级主类目' : '编辑二级细分类目'
  }
  return catForm.level === 'L1' ? '新增一级主类目' : '新增二级细分类目'
})

// Helper to look up full category name by ID
function getCategoryNameById(catId) {
  if (!catId) return ''
  for (const l1 of categoryTree.value) {
    if (l1.id === catId) {
      return l1.name
    }
    if (l1.children && l1.children.length) {
      for (const l2 of l1.children) {
        if (l2.id === catId) {
          return `${l1.name} · ${l2.name}`
        }
      }
    }
  }
  return ''
}

// Helper to look up parent category name by PID
function getParentCategoryName(pid) {
  if (!pid) return '无'
  const parent = categoryTree.value.find(c => c.id === pid)
  return parent ? parent.name : `ID: ${pid}`
}

// Selectable categories for goods create/edit
const selectableCategories = computed(() => {
  const result = []
  for (const l1 of categoryTree.value) {
    if (l1.children && l1.children.length > 0) {
      result.push({
        id: l1.id,
        name: `【主】${l1.name}`,
        l1Id: l1.id
      })
      for (const l2 of l1.children) {
        result.push({
          id: l2.id,
          name: `　└ ${l2.name}`,
          l1Id: l1.id
        })
      }
    } else {
      result.push({
        id: l1.id,
        name: l1.name,
        l1Id: l1.id
      })
    }
  }
  return result
})

// Dashboard State
const dashboardData = reactive({
  stats: {
    todayFulfillmentTurnover: 0,
    todayFulfillmentOrderCount: 0,
    todayMakingCount: 0,
    todayPickupCount: 0,
    todayCompletedCount: 0,
    tomorrowPreorderCount: 0,
    tomorrowPreorderTurnover: 0,
    todayCashTurnover: 0,
    todayPaidOrderCount: 0,
    todayRefundAmount: 0,
    todayRefundCount: 0,
    todayTurnover: 0,
    todayOrderCount: 0,
    pendingMakingCount: 0,
    pendingPickupCount: 0,
    pendingRefundCount: 0
  },
  printer: {
    name: '前台出票机',
    sn: '',
    online: 0,
    status: 0
  }
})
const isPrintingTest = ref(false)

// Modals State
const showOrderDetailModal = ref(false)
const orderDetailData = ref(null)

const showManualVerifyModal = ref(false)
const manualVerifyCode = ref('')

const showRefundModal = ref(false)
const currentRefundOrder = ref(null)
const refundRejectReason = ref('')

const showGoodsModal = ref(false)
const isSavingGoods = ref(false)
const goodsDetailProducts = ref([])
const goodsForm = reactive({
  id: null,
  name: '',
  categoryId: null,
  retailPrice: '',
  counterPrice: '',
  stock: 100,
  brief: '',
  picUrl: '',
  isOnSale: true,
  specType: 'single' // 'single' 或 'multi'
})

// 多规格配置列表
const multiSpecList = ref([])
const specPresets = [
  '中杯', '大杯', '超大杯', '标准份', '大份', '全麦吐司', '经典白面包', '贝果', '冰', '热', '微辣'
]

// 计算多规格最低售价作为基础售价
const computedMinPrice = computed(() => {
  if (!multiSpecList.value || multiSpecList.value.length === 0) return '0.00'
  const prices = multiSpecList.value
    .map(s => parseFloat(s.price))
    .filter(p => !isNaN(p) && p > 0)
  if (prices.length === 0) return '0.00'
  return Math.min(...prices).toFixed(2)
})

// 计算多规格总库存
const computedTotalStock = computed(() => {
  if (!multiSpecList.value || multiSpecList.value.length === 0) return 0
  return multiSpecList.value.reduce((acc, s) => acc + (parseInt(s.number) || 0), 0)
})

function switchSpecType(type) {
  goodsForm.specType = type
  if (type === 'multi') {
    if (!multiSpecList.value || multiSpecList.value.length === 0) {
      const defaultPrice = goodsForm.retailPrice || '15.00'
      const sPrice = parseFloat(defaultPrice) || 15
      multiSpecList.value = [
        { id: null, specName: '中杯', price: defaultPrice, number: goodsForm.stock || 50 },
        { id: null, specName: '大杯', price: String(sPrice + 3), number: goodsForm.stock || 50 }
      ]
    }
  }
}

function addPresetSpec(presetName) {
  const exists = multiSpecList.value.some(s => s.specName === presetName)
  if (exists) {
    uni.showToast({ title: `已包含「${presetName}」规格`, icon: 'none' })
    return
  }
  const defaultPrice = goodsForm.retailPrice || (multiSpecList.value[0] ? multiSpecList.value[0].price : '15.00')
  multiSpecList.value.push({
    id: null,
    specName: presetName,
    price: defaultPrice,
    number: 50
  })
}

function addEmptySpecItem() {
  const defaultPrice = goodsForm.retailPrice || (multiSpecList.value[0] ? multiSpecList.value[0].price : '15.00')
  multiSpecList.value.push({
    id: null,
    specName: '',
    price: defaultPrice,
    number: 50
  })
}

function removeSpecItem(index) {
  if (multiSpecList.value.length <= 1) {
    uni.showToast({ title: '多规格模式下至少保留一个规格项', icon: 'none' })
    return
  }
  multiSpecList.value.splice(index, 1)
}

const showCategoryModal = ref(false)
const showSingleCatModal = ref(false)
const catForm = reactive({
  id: null,
  name: '',
  level: 'L1',
  pid: 0,
  sortOrder: 50
})

// Polling Timer
let pollTimer = null

// Computed category picker index and name
const categoryPickerIndex = computed(() => {
  if (!goodsForm.categoryId) return 0
  const idx = selectableCategories.value.findIndex(c => c.id === goodsForm.categoryId)
  return idx >= 0 ? idx : 0
})

const selectedCategoryName = computed(() => {
  if (!goodsForm.categoryId) return ''
  const cat = selectableCategories.value.find(c => c.id === goodsForm.categoryId)
  return cat ? cat.name : ''
})

// Lifecycle
onShow(async () => {
  try {
    const statusRes = await request({ url: api.AdminAuthStatus, hideErrorToast: true })
    if (!statusRes || !statusRes.isBound) {
      uni.redirectTo({ url: '/pages/admin/login' })
      return
    }
  } catch (e) {
    uni.redirectTo({ url: '/pages/admin/login' })
    return
  }

  await loadOrders()
  await loadCategories()
  await loadGoods()
  await loadDashboard()
  startOrderPolling()
})

onHide(() => {
  stopOrderPolling()
})

onUnmounted(() => {
  stopOrderPolling()
})

onPullDownRefresh(async () => {
  if (currentTab.value === 0) {
    await loadOrders()
  } else if (currentTab.value === 1) {
    await loadGoods()
    await loadCategories()
  } else {
    await loadDashboard()
  }
  uni.stopPullDownRefresh()
})

// Polling implementation (15s lightweight poll)
function startOrderPolling() {
  stopOrderPolling()
  pollTimer = setInterval(async () => {
    try {
      const res = await request({
        url: api.AdminOrderList,
        data: { showType: orderFilterType.value, limit: 10 },
        hideErrorToast: true
      })
      if (res) {
        const prevMaking = orderCounts.making
        updateOrderCounts(res)

        // If there's new making order, vibrate and alert
        if (res.pendingMakingCount > prevMaking && prevMaking !== 0) {
          uni.vibrateShort()
          uni.showToast({ title: '收到新订单！请及时制作', icon: 'none' })
        }
        orderList.value = res.list || []
      }
    } catch (e) {
      // Silent error during background poll
    }
  }, 15000)
}

function stopOrderPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function switchMainTab(idx) {
  currentTab.value = idx
  if (idx === 0) {
    loadOrders()
  } else if (idx === 1) {
    loadCategories()
    loadGoods()
  } else if (idx === 2) {
    loadDashboard()
  }
}

// ----------------- ORDER MODULE -----------------
async function loadOrders() {
  isRefreshing.value = true
  try {
    const params = {
      showType: orderFilterType.value,
      page: 1,
      limit: 50
    }
    if (orderFilterType.value === 1) {
      params.fulfillmentScope = fulfillmentScope.value
    }
    const res = await request({
      url: api.AdminOrderList,
      data: params
    })
    if (res) {
      orderList.value = res.list || []
      updateOrderCounts(res)
    }
  } catch (err) {
    console.error('加载订单失败', err)
  } finally {
    isRefreshing.value = false
  }
}

function updateOrderCounts(data) {
  if (!data) return
  orderCounts.todayMaking = data.todayMakingCount || 0
  orderCounts.tomorrowMaking = data.tomorrowMakingCount || 0
  orderCounts.making = data.pendingMakingCount || 0
  orderCounts.pickup = data.pendingPickupCount || 0
  orderCounts.refund = data.pendingRefundCount || 0
  orderCounts.pendingTotal = orderCounts.making + orderCounts.pickup + orderCounts.refund
}

function switchOrderFilter(type) {
  orderFilterType.value = type
  loadOrders()
}

function switchFulfillmentScope(scope) {
  fulfillmentScope.value = scope
  loadOrders()
}

async function onRefreshOrders() {
  await loadOrders()
}

// Batch Mark Ready (一键出餐)
async function onBatchReady() {
  if (fulfillmentScope.value === 'tomorrow') {
    uni.showToast({ title: '当前为明日预订模式，不可提前出餐', icon: 'none' })
    return
  }
  const targetOrders = orderList.value.filter(o => o.orderStatus === 201)
  if (targetOrders.length === 0) {
    uni.showToast({ title: '当前没有待制作订单', icon: 'none' })
    return
  }

  uni.showModal({
    title: '一键出餐',
    content: `确定将列表中的 ${targetOrders.length} 笔今日待制作订单全部标记出餐吗？`,
    confirmColor: '#6F4E37',
    success: async (mRes) => {
      if (mRes.confirm) {
        uni.showLoading({ title: '正在批量出餐...' })
        try {
          try {
            await request({
              url: api.AdminOrderBatchReady,
              method: 'POST',
              data: { orderIds: targetOrders.map(o => o.id) }
            })
          } catch (apiErr) {
            // 降级循环并发调用单个接口
            await Promise.all(targetOrders.map(o =>
              request({
                url: api.AdminOrderReady,
                method: 'POST',
                data: { orderId: o.id }
              })
            ))
          }
          uni.showToast({ title: `成功出餐 ${targetOrders.length} 笔`, icon: 'success' })
          loadOrders()
        } catch (e) {
          console.error('一键出餐失败', e)
          uni.showToast({ title: '部分出餐失败，请刷新', icon: 'none' })
          loadOrders()
        } finally {
          uni.hideLoading()
        }
      }
    }
  })
}

// Batch Verify (一键核销)
async function onBatchVerify() {
  const targetOrders = orderList.value.filter(o => o.orderStatus === 301 || o.orderStatus === 201)
  if (targetOrders.length === 0) {
    uni.showToast({ title: '当前没有待核销订单', icon: 'none' })
    return
  }

  uni.showModal({
    title: '一键核销',
    content: `确定将列表中的 ${targetOrders.length} 笔订单全部完成提货核销吗？`,
    confirmColor: '#6F4E37',
    success: async (mRes) => {
      if (mRes.confirm) {
        uni.showLoading({ title: '正在批量核销...' })
        try {
          try {
            await request({
              url: api.AdminOrderBatchVerify,
              method: 'POST',
              data: { orderIds: targetOrders.map(o => o.id) }
            })
          } catch (apiErr) {
            // 降级循环并发调用单个接口
            await Promise.all(targetOrders.map(o =>
              request({
                url: api.AdminOrderVerify,
                method: 'POST',
                data: { orderId: o.id }
              })
            ))
          }
          uni.showToast({ title: `成功核销 ${targetOrders.length} 笔`, icon: 'success' })
          loadOrders()
        } catch (e) {
          console.error('一键核销失败', e)
          uni.showToast({ title: '部分核销失败，请刷新', icon: 'none' })
          loadOrders()
        } finally {
          uni.hideLoading()
        }
      }
    }
  })
}

// Mark Order as Ready for Pickup (301)
async function onMarkReady(orderId) {
  uni.showLoading({ title: '正在出餐...' })
  try {
    await request({
      url: api.AdminOrderReady,
      method: 'POST',
      data: { orderId }
    })
    uni.showToast({ title: '已出餐', icon: 'success' })
    if (orderDetailData.value && orderDetailData.value.order && orderDetailData.value.order.id === orderId) {
      orderDetailData.value.order.orderStatus = 301
      orderDetailData.value.orderStatusText = '待取餐'
    }
    loadOrders()
  } catch (e) {
    console.error('出餐操作失败', e)
  } finally {
    uni.hideLoading()
  }
}

// Verify Order via Button click
async function onVerifyOrder(orderId) {
  uni.showModal({
    title: '提货核销',
    content: '确认顾客已提货？',
    confirmColor: '#6F4E37',
    success: async (mRes) => {
      if (mRes.confirm) {
        uni.showLoading({ title: '核销中...' })
        try {
          await request({
            url: api.AdminOrderVerify,
            method: 'POST',
            data: { orderId }
          })
          uni.showToast({ title: '核销成功', icon: 'success' })
          if (orderDetailData.value && orderDetailData.value.order && orderDetailData.value.order.id === orderId) {
            orderDetailData.value.order.orderStatus = 401
            orderDetailData.value.orderStatusText = '已完成'
          }
          loadOrders()
        } catch (e) {
          console.error('核销失败', e)
        } finally {
          uni.hideLoading()
        }
      }
    }
  })
}

// Scan Code Verification
function onScanCode() {
  uni.scanCode({
    success: async (res) => {
      if (res && res.result) {
        uni.showLoading({ title: '正在核销...' })
        try {
          await request({
            url: api.AdminOrderVerify,
            method: 'POST',
            data: { code: res.result }
          })
          uni.showToast({ title: '核销成功！', icon: 'success' })
          loadOrders()
        } catch (e) {
          console.error('扫码核销失败', e)
        } finally {
          uni.hideLoading()
        }
      }
    },
    fail: () => {
      // User cancelled camera scan
    }
  })
}

// Manual Verify Confirmation
async function onConfirmManualVerify() {
  if (!manualVerifyCode.value.trim()) {
    uni.showToast({ title: '请输入提货码', icon: 'none' })
    return
  }

  uni.showLoading({ title: '正在核销...' })
  try {
    await request({
      url: api.AdminOrderVerify,
      method: 'POST',
      data: { code: manualVerifyCode.value.trim() }
    })
    uni.showToast({ title: '核销成功', icon: 'success' })
    showManualVerifyModal.value = false
    manualVerifyCode.value = ''
    loadOrders()
  } catch (e) {
    console.error('核销失败', e)
  } finally {
    uni.hideLoading()
  }
}

let isLongPressing = false

// Open Order Detail Modal
async function openOrderDetail(order) {
  if (isLongPressing) return
  // Immediately populate base fields from list item
  orderDetailData.value = {
    order: { ...order },
    orderGoods: order.goodsList || [],
    orderStatusText: order.orderStatusText,
    pickupCode: order.pickupCode,
    user: null
  }
  showOrderDetailModal.value = true

  // Fetch complete details from backend
  try {
    const res = await request({
      url: api.AdminOrderDetail,
      data: { orderId: order.id }
    })
    if (res && res.order) {
      orderDetailData.value = res
    }
  } catch (e) {
    console.error('获取订单详情失败', e)
  }
}

function openRefundModalFromDetail() {
  if (orderDetailData.value && orderDetailData.value.order) {
    const o = orderDetailData.value.order
    showOrderDetailModal.value = false
    openRefundModal(o)
  }
}

function copyText(str) {
  if (!str) return
  uni.setClipboardData({
    data: String(str),
    success: () => {
      uni.showToast({ title: '已复制', icon: 'success' })
    }
  })
}

function callPhone(phone) {
  if (!phone) {
    uni.showToast({ title: '暂无电话号码', icon: 'none' })
    return
  }
  uni.makePhoneCall({ phoneNumber: String(phone) })
}

function formatFullTime(str) {
  if (!str) return '-'
  return str.replace('T', ' ').substring(0, 19)
}

// Order Card Long Press Menu
function onOrderLongPress(order) {
  isLongPressing = true
  setTimeout(() => {
    isLongPressing = false
  }, 400)
  // #ifndef H5
  try {
    uni.vibrateShort({ type: 'light' })
  } catch (e) {}
  // #endif

  const pickupTitle = order && order.pickupCode ? `取餐码 #${order.pickupCode}` : '订单操作'
  uni.showActionSheet({
    title: pickupTitle,
    itemList: ['补打小票'],
    success: (res) => {
      if (res.tapIndex === 0) {
        onReprintOrder(order)
      }
    }
  })
}

// Reprint Order Ticket
function onReprintOrder(orderOrId) {
  const orderId = typeof orderOrId === 'object' ? orderOrId.id : orderOrId
  const codeInfo = typeof orderOrId === 'object' && orderOrId.pickupCode ? `（取餐码 #${orderOrId.pickupCode}）` : ''
  uni.showModal({
    title: '补打小票',
    content: `确定重新打印该订单${codeInfo}小票吗？`,
    confirmColor: '#6F4E37',
    success: async (mRes) => {
      if (mRes.confirm) {
        uni.showLoading({ title: '发送补打指令...' })
        try {
          await request({
            url: api.AdminOrderReprint,
            method: 'POST',
            data: { orderId }
          })
          uni.showToast({ title: '小票指令已发送', icon: 'success' })
        } catch (e) {
          console.error('补打小票失败', e)
        } finally {
          uni.hideLoading()
        }
      }
    }
  })
}

// Refund Modal and Process
function openRefundModal(order) {
  currentRefundOrder.value = order
  refundRejectReason.value = ''
  showRefundModal.value = true
}

async function onSubmitRefund(agree) {
  if (!currentRefundOrder.value) return

  uni.showLoading({ title: agree ? '退款处理中...' : '提交拒绝中...' })
  try {
    await request({
      url: api.AdminOrderRefund,
      method: 'POST',
      data: {
        orderId: currentRefundOrder.value.id,
        agree,
        reason: refundRejectReason.value.trim()
      }
    })
    uni.showToast({ title: agree ? '退款已同意并返还' : '已拒绝退款申请', icon: 'success' })
    if (orderDetailData.value && orderDetailData.value.order && orderDetailData.value.order.id === currentRefundOrder.value.id) {
      orderDetailData.value.order.orderStatus = 203
      orderDetailData.value.orderStatusText = agree ? '已退款' : '已拒绝退款'
    }
    showRefundModal.value = false
    loadOrders()
  } catch (e) {
    console.error('退款审核失败', e)
  } finally {
    uni.hideLoading()
  }
}

// ----------------- GOODS & CATEGORY MODULE -----------------
async function loadCategories() {
  try {
    const res = await request({ url: api.AdminCategoryList })
    if (res && Array.isArray(res)) {
      categoryTree.value = res
      const flat = []
      for (const l1 of res) {
        flat.push({ id: l1.id, name: l1.name, level: 'L1' })
        if (l1.children && l1.children.length) {
          for (const l2 of l1.children) {
            flat.push({ id: l2.id, name: `└ ${l2.name}`, level: 'L2' })
          }
        }
      }
      flatCategories.value = flat
    }
  } catch (err) {
    console.error('加载类目失败:', err)
  }
}

async function loadGoods() {
  isRefreshingGoods.value = true
  try {
    const data = {
      page: 1,
      limit: 100
    }
    if (selectedCategoryId.value !== null && selectedCategoryId.value !== undefined && selectedCategoryId.value !== '') {
      data.categoryId = selectedCategoryId.value
    }
    const res = await request({
      url: api.AdminGoodsList,
      data
    })
    if (res) {
      goodsList.value = res.list || []
    }
  } catch (e) {
    console.error('加载商品失败', e)
  } finally {
    isRefreshingGoods.value = false
  }
}

function onSelectL1Category(catId) {
  selectedL1Id.value = catId
  selectedL2Id.value = null
  selectedCategoryId.value = catId
  loadGoods()
}

function onSelectL2Category(subId) {
  selectedL2Id.value = subId
  selectedCategoryId.value = subId !== null ? subId : selectedL1Id.value
  loadGoods()
}

function onSelectCategory(catId) {
  onSelectL1Category(catId)
}

async function onRefreshGoods() {
  await loadGoods()
}

// Toggle Goods On/Off Sale
async function onToggleSale(item) {
  const newStatus = !item.isOnSale
  try {
    await request({
      url: api.AdminGoodsQuickUpdate,
      method: 'POST',
      data: {
        goodsId: item.id,
        isOnSale: newStatus
      }
    })
    item.isOnSale = newStatus
    uni.showToast({ title: newStatus ? '餐品已上架' : '餐品已下架', icon: 'none' })
  } catch (e) {
    console.error('修改上架状态失败', e)
  }
}

// Open Goods Form Modal (Create or Edit)
async function openGoodsFormModal(item) {
  if (item) {
    // Edit existing goods
    goodsForm.id = item.id
    goodsForm.name = item.name
    goodsForm.categoryId = item.categoryId
    goodsForm.retailPrice = String(item.retailPrice != null ? item.retailPrice : '')
    goodsForm.counterPrice = String(item.counterPrice || item.retailPrice || '')
    goodsForm.stock = item.totalStock != null ? item.totalStock : 100
    goodsForm.brief = item.brief || ''
    goodsForm.picUrl = item.picUrl || ''
    goodsForm.isOnSale = !!item.isOnSale
    goodsForm.specType = 'single'
    multiSpecList.value = []

    // Fetch full SKU products
    try {
      uni.showLoading({ title: '加载中...' })
      const res = await request({
        url: api.AdminGoodsDetail,
        data: { goodsId: item.id }
      })
      if (res && res.products && res.products.length > 0) {
        goodsDetailProducts.value = res.products
        const isMulti = res.products.length > 1 || 
          (res.products.length === 1 && res.products[0].specifications && res.products[0].specifications[0] !== '标准')
        
        if (isMulti) {
          goodsForm.specType = 'multi'
          multiSpecList.value = res.products.map(p => ({
            id: p.id,
            specName: (p.specifications || []).join(' / ') || '标准',
            specifications: p.specifications,
            price: String(p.price != null ? p.price : goodsForm.retailPrice),
            number: Number(p.number != null ? p.number : 50)
          }))
        } else {
          goodsForm.specType = 'single'
          goodsForm.retailPrice = String(res.products[0].price != null ? res.products[0].price : goodsForm.retailPrice)
          goodsForm.stock = res.products[0].number != null ? res.products[0].number : 100
          multiSpecList.value = []
        }
      } else {
        goodsDetailProducts.value = []
      }
    } catch (e) {
      goodsDetailProducts.value = []
    } finally {
      uni.hideLoading()
    }
  } else {
    // Create new goods
    goodsForm.id = null
    goodsForm.name = ''
    goodsForm.specType = 'single'
    multiSpecList.value = []
    
    // Intelligently preselect category based on currently active filter
    let defaultCatId = null
    if (selectedL2Id.value) {
      defaultCatId = selectedL2Id.value
    } else if (selectedL1Id.value) {
      const l1 = categoryTree.value.find(c => c.id === selectedL1Id.value)
      if (l1) {
        if (l1.children && l1.children.length > 0) {
          defaultCatId = l1.children[0].id
        } else {
          defaultCatId = l1.id
        }
      }
    }
    if (!defaultCatId && selectableCategories.value.length > 0) {
      defaultCatId = selectableCategories.value[0].id
    }

    goodsForm.categoryId = defaultCatId
    goodsForm.retailPrice = ''
    goodsForm.counterPrice = ''
    goodsForm.stock = 100
    goodsForm.brief = ''
    goodsForm.picUrl = ''
    goodsForm.isOnSale = true
    goodsDetailProducts.value = []
  }
  showGoodsModal.value = true
}

function onCategoryPickerChange(e) {
  const index = Number(e.detail.value)
  if (selectableCategories.value[index]) {
    goodsForm.categoryId = selectableCategories.value[index].id
  }
}

// Choose and Upload Goods Image
function onChooseGoodsImage() {
  startPickImage()
}

function showPrivacyAgreementModal() {
  uni.showModal({
    title: '需开通相册权限声明',
    content: '微信提示：当前小程序在公众平台尚未声明“相册与相机”权限。\n\n【开通步骤】：\n登录 mp.weixin.qq.com ->【设置】->【用户隐私保护指引】-> 补充勾选“收集你选中的照片或视频信息”并提交即可生效。',
    showCancel: false,
    confirmText: '我知道了',
    confirmColor: '#6F4E37'
  })
}

function startPickImage() {
  const doUpload = (tempFilePath) => {
    if (!tempFilePath) return
    uni.showLoading({ title: '上传图片中...' })

    uni.uploadFile({
      url: api.StorageUpload,
      filePath: tempFilePath,
      name: 'file',
      header: {
        'X-Litemall-Token': uni.getStorageSync('token') || ''
      },
      success: (uploadRes) => {
        try {
          const data = JSON.parse(uploadRes.data)
          if (data.errno === 0 && data.data && data.data.url) {
            goodsForm.picUrl = fixImageUrl(data.data.url)
            uni.showToast({ title: '图片上传成功', icon: 'success' })
          } else {
            uni.showToast({ title: data.errmsg || '上传失败', icon: 'none' })
          }
        } catch (err) {
          uni.showToast({ title: '解析图片结果失败', icon: 'none' })
        }
      },
      fail: (err) => {
        console.error('上传图片网络异常:', err)
        uni.showToast({ title: '图片上传网络异常', icon: 'none' })
      },
      complete: () => {
        uni.hideLoading()
      }
    })
  }

  // 微信小程序优先使用现代化 chooseMedia，降级回退至 chooseImage
  if (typeof uni.chooseMedia === 'function') {
    uni.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      camera: 'back',
      success: (res) => {
        if (res.tempFiles && res.tempFiles.length > 0) {
          doUpload(res.tempFiles[0].tempFilePath)
        }
      },
      fail: (err) => {
        console.warn('chooseMedia fail:', err)
        if (err && err.errMsg && err.errMsg.includes('cancel')) {
          return
        }
        if (err && err.errMsg && err.errMsg.includes('privacy agreement')) {
          showPrivacyAgreementModal()
          return
        }
        fallbackChooseImage(doUpload)
      }
    })
  } else {
    fallbackChooseImage(doUpload)
  }
}

function fallbackChooseImage(doUpload) {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (chooseRes) => {
      if (chooseRes.tempFilePaths && chooseRes.tempFilePaths.length > 0) {
        doUpload(chooseRes.tempFilePaths[0])
      }
    },
    fail: (err) => {
      console.error('chooseImage fail:', err)
      if (err && err.errMsg && !err.errMsg.includes('cancel')) {
        if (err.errMsg.includes('privacy agreement')) {
          showPrivacyAgreementModal()
        } else {
          uni.showToast({ title: '选择图片失败: ' + (err.errMsg || ''), icon: 'none' })
        }
      }
    }
  })
}


// Submit Goods Form
async function onSubmitGoodsForm() {
  const name = goodsForm.name ? goodsForm.name.trim() : ''
  if (!name) {
    uni.showToast({ title: '请输入餐品名称', icon: 'none' })
    return
  }
  if (!goodsForm.categoryId) {
    uni.showToast({ title: '请选择所属分类', icon: 'none' })
    return
  }

  let finalPrice = '0.00'
  let finalStock = 100
  let payloadProducts = []

  if (goodsForm.specType === 'single') {
    const priceNum = parseFloat(goodsForm.retailPrice)
    if (isNaN(priceNum) || priceNum <= 0) {
      uni.showToast({ title: '请输入有效售价', icon: 'none' })
      return
    }
    finalPrice = priceNum.toFixed(2)
    finalStock = goodsForm.stock ? parseInt(goodsForm.stock) : 100
    payloadProducts = [{
      specName: '标准',
      specifications: ['标准'],
      price: finalPrice,
      number: finalStock
    }]
  } else {
    // 多规格校验
    if (!multiSpecList.value || multiSpecList.value.length === 0) {
      uni.showToast({ title: '请至少添加一个规格项', icon: 'none' })
      return
    }
    for (let i = 0; i < multiSpecList.value.length; i++) {
      const sku = multiSpecList.value[i]
      if (!sku.specName || !sku.specName.trim()) {
        uni.showToast({ title: `请填写规格项 ${i + 1} 的名称`, icon: 'none' })
        return
      }
      const pNum = parseFloat(sku.price)
      if (isNaN(pNum) || pNum <= 0) {
        uni.showToast({ title: `请填写规格「${sku.specName}」的有效售价`, icon: 'none' })
        return
      }
      const sNum = parseInt(sku.number)
      if (isNaN(sNum) || sNum < 0) {
        uni.showToast({ title: `请填写规格「${sku.specName}」的有效库存`, icon: 'none' })
        return
      }
    }

    finalPrice = computedMinPrice.value
    finalStock = computedTotalStock.value
    payloadProducts = multiSpecList.value.map(s => ({
      id: s.id || null,
      specName: s.specName.trim(),
      specifications: s.specName.trim().split('/').map(v => v.trim()).filter(Boolean),
      price: parseFloat(s.price).toFixed(2),
      number: parseInt(s.number) || 0
    }))
  }

  isSavingGoods.value = true
  try {
    const payload = {
      name,
      categoryId: goodsForm.categoryId,
      retailPrice: finalPrice,
      counterPrice: (goodsForm.counterPrice ? parseFloat(goodsForm.counterPrice) : parseFloat(finalPrice)).toFixed(2),
      stock: finalStock,
      brief: goodsForm.brief ? goodsForm.brief.trim() : '',
      picUrl: goodsForm.picUrl || '',
      isOnSale: goodsForm.isOnSale,
      specType: goodsForm.specType,
      products: payloadProducts
    }
    if (goodsForm.id) {
      payload.id = goodsForm.id
    }

    await request({
      url: api.AdminGoodsSave,
      method: 'POST',
      data: payload
    })

    uni.showToast({ title: '保存成功', icon: 'success' })
    showGoodsModal.value = false
    await loadGoods()
  } catch (err) {
    console.error('保存商品失败', err)
  } finally {
    isSavingGoods.value = false
  }
}

// Delete Goods
function onDeleteGoods(item) {
  uni.showModal({
    title: '删除餐品',
    content: `确定删除「${item.name}」吗？`,
    confirmColor: '#E07A5F',
    success: async (mRes) => {
      if (mRes.confirm) {
        uni.showLoading({ title: '删除中...' })
        try {
          await request({
            url: api.AdminGoodsDelete,
            method: 'POST',
            data: { id: item.id }
          })
          uni.showToast({ title: '餐品已删除', icon: 'success' })
          loadGoods()
        } catch (e) {
          console.error('删除商品失败', e)
        } finally {
          uni.hideLoading()
        }
      }
    }
  })
}

// Category Management Handlers
function openCategoryModal() {
  showCategoryModal.value = true
}

function openEditCategoryModal(cat, level) {
  if (cat) {
    catForm.id = cat.id
    catForm.name = cat.name
    catForm.level = cat.level || level
    catForm.pid = cat.pid || 0
    catForm.sortOrder = cat.sortOrder || 50
  } else {
    catForm.id = null
    catForm.name = ''
    catForm.level = level
    catForm.pid = 0
    catForm.sortOrder = 50
  }
  showSingleCatModal.value = true
}

function openAddSubCategoryModal(parentL1) {
  catForm.id = null
  catForm.name = ''
  catForm.level = 'L2'
  catForm.pid = parentL1.id
  catForm.sortOrder = 50
  showSingleCatModal.value = true
}

function openEditSelectedL2Modal() {
  if (!selectedL2Id.value) return
  const sub = activeSubCategories.value.find(s => s.id === selectedL2Id.value)
  if (sub) {
    openEditCategoryModal(sub, 'L2')
  }
}

async function onSubmitCategoryForm() {
  if (!catForm.name || !catForm.name.trim()) {
    uni.showToast({ title: '请输入分类名称', icon: 'none' })
    return
  }

  isSavingCategory.value = true
  try {
    await request({
      url: api.AdminCategorySave,
      method: 'POST',
      data: {
        id: catForm.id,
        name: catForm.name.trim(),
        level: catForm.level,
        pid: catForm.pid,
        sortOrder: Number(catForm.sortOrder != null && catForm.sortOrder !== '' ? catForm.sortOrder : 50)
      }
    })
    uni.showToast({ title: '类目保存成功', icon: 'success' })
    showSingleCatModal.value = false
    await loadCategories()
    await loadGoods()
  } catch (err) {
    console.error('类目保存失败', err)
  } finally {
    isSavingCategory.value = false
  }
}

function onDeleteCategory(cat) {
  uni.showModal({
    title: '删除分类',
    content: `确定删除分类「${cat.name}」吗？如果类目下仍有子分类或关联商品，系统将拒绝删除。`,
    confirmColor: '#E07A5F',
    success: async (mRes) => {
      if (mRes.confirm) {
        uni.showLoading({ title: '校验并删除中...' })
        try {
          await request({
            url: api.AdminCategoryDelete,
            method: 'POST',
            data: { id: cat.id }
          })
          uni.showToast({ title: '分类已删除', icon: 'success' })
          if (selectedL1Id.value === cat.id || selectedL2Id.value === cat.id) {
            selectedL1Id.value = null
            selectedL2Id.value = null
            selectedCategoryId.value = null
          }
          await loadCategories()
          await loadGoods()
        } catch (e) {
          console.error('删除分类失败', e)
        } finally {
          uni.hideLoading()
        }
      }
    }
  })
}

// ----------------- DASHBOARD & PRINTER MODULE -----------------
async function loadDashboard() {
  try {
    const res = await request({ url: api.AdminDashboard })
    if (res) {
      if (res.stats) {
        dashboardData.stats = res.stats
      }
      if (res.printer) {
        dashboardData.printer = res.printer
      }
    }
  } catch (err) {
    console.error('加载仪表盘失败:', err)
  }
}

async function onTestPrint() {
  isPrintingTest.value = true
  try {
    await request({
      url: api.AdminPrintTest,
      method: 'POST'
    })
    uni.showToast({ title: '测试打印指令已发送', icon: 'success' })
  } catch (err) {
    console.error('测试出票失败', err)
  } finally {
    isPrintingTest.value = false
  }
}

// Unbind and Logout from Workbench
function onUnbindAdmin() {
  uni.showModal({
    title: '退出工作台',
    content: '确定退出并解除绑定吗？',
    confirmColor: '#E07A5F',
    success: async (mRes) => {
      if (mRes.confirm) {
        uni.showLoading({ title: '退出中...' })
        try {
          await request({
            url: api.AdminAuthLogout,
            method: 'POST'
          })
          uni.showToast({ title: '已退出', icon: 'success' })
          setTimeout(() => {
            uni.switchTab({ url: '/pages/ucenter/index' })
          }, 400)
        } catch (e) {
          uni.showToast({ title: '退出异常', icon: 'none' })
        } finally {
          uni.hideLoading()
        }
      }
    }
  })
}

function formatTime(str) {
  if (!str) return ''
  return str.replace('T', ' ').substring(5, 16)
}
</script>

<style scoped>
.workbench-container {
  min-height: 100vh;
  background-color: #FAF6EE;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

/* Top Nav */
.top-nav {
  background: #6F4E37;
  padding: 20rpx 28rpx;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.scan-btn {
  background: rgba(255, 255, 255, 0.2);
  padding: 8rpx 18rpx;
  border-radius: 28rpx;
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.scan-icon {
  font-size: 26rpx;
}

.scan-text {
  color: #FAF6EE;
  font-size: 24rpx;
  font-weight: 600;
}

.input-verify-btn {
  background: #E07A5F;
  padding: 8rpx 18rpx;
  border-radius: 28rpx;
}

.verify-btn-text {
  color: #ffffff;
  font-size: 24rpx;
  font-weight: 600;
}

/* Main Navigation Tabs */
.main-tabs {
  background: #ffffff;
  display: flex;
  border-bottom: 2rpx solid #EFE4D6;
  position: sticky;
  top: 0;
  z-index: 10;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 26rpx 0;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
}

.tab-label {
  font-size: 28rpx;
  color: #8C7063;
  font-weight: 600;
}

.tab-item.active .tab-label {
  color: #6F4E37;
  font-weight: 700;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 25%;
  right: 25%;
  height: 6rpx;
  background: #6F4E37;
  border-radius: 6rpx;
}

.tab-badge {
  background: #E07A5F;
  color: #ffffff;
  font-size: 20rpx;
  font-weight: 700;
  padding: 2rpx 12rpx;
  border-radius: 16rpx;
}

/* Tab Content */
.tab-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

/* Sub Tabs for Orders */
.sub-tabs {
  display: flex;
  background: #FAF4EB;
  padding: 16rpx 20rpx;
  gap: 16rpx;
  overflow-x: auto;
  border-bottom: 1rpx solid #EAE0D3;
}

.sub-tab-item {
  padding: 10rpx 24rpx;
  border-radius: 24rpx;
  font-size: 24rpx;
  color: #8C7063;
  background: #ffffff;
  border: 1rpx solid #E6D7C3;
  display: flex;
  align-items: center;
  gap: 8rpx;
  white-space: nowrap;
}

.sub-tab-item.active {
  background: #6F4E37;
  color: #FAF6EE;
  font-weight: 700;
  border-color: #6F4E37;
}

.sub-badge {
  background: #E07A5F;
  color: #ffffff;
  font-size: 18rpx;
  font-weight: 700;
  padding: 0 10rpx;
  border-radius: 12rpx;
}

.refund-badge {
  background: #D9534F;
}

/* Order Scope Toggle Bar */
.scope-toggle-bar {
  display: flex;
  gap: 14rpx;
  padding: 12rpx 20rpx 4rpx;
}

.scope-chip {
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 10rpx 22rpx;
  background: #ffffff;
  border-radius: 32rpx;
  border: 1rpx solid #EFE4D6;
  font-size: 24rpx;
  color: #8C7063;
  font-weight: 600;
  box-shadow: 0 2rpx 6rpx rgba(111, 78, 55, 0.04);
}

.scope-chip.active {
  background: #6F4E37;
  color: #FAF6EE;
  border-color: #6F4E37;
  box-shadow: 0 4rpx 10rpx rgba(111, 78, 55, 0.2);
}

.scope-chip.chip-tomorrow.active {
  background: #E07A5F;
  border-color: #E07A5F;
  box-shadow: 0 4rpx 10rpx rgba(224, 122, 95, 0.25);
}

.scope-badge {
  background: rgba(224, 122, 95, 0.15);
  color: #E07A5F;
  font-size: 20rpx;
  padding: 2rpx 10rpx;
  border-radius: 14rpx;
  font-weight: 700;
}

.scope-chip.active .scope-badge {
  background: rgba(255, 255, 255, 0.25);
  color: #FAF6EE;
}

.tomorrow-locked-tag {
  display: flex;
  align-items: center;
  padding: 10rpx 18rpx;
  background: #F5EEDF;
  border-radius: 20rpx;
}

.locked-text {
  font-size: 22rpx;
  color: #8C7063;
  font-weight: 600;
}

.fulfillment-badge {
  font-size: 20rpx;
  padding: 4rpx 12rpx;
  border-radius: 10rpx;
  background: #F5EEDF;
  color: #6F4E37;
  font-weight: 700;
  margin-right: 10rpx;
}

/* Order Batch Bar */
.order-batch-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  margin: 16rpx 20rpx 4rpx;
  padding: 14rpx 24rpx;
  border-radius: 18rpx;
  border: 1rpx solid #EFE4D6;
  box-shadow: 0 2rpx 8rpx rgba(111, 78, 55, 0.04);
}

.batch-bar-hint {
  font-size: 24rpx;
  color: #8C7063;
  font-weight: 600;
}

.batch-bar-right {
  display: flex;
  align-items: center;
}

.batch-action-btn {
  height: 56rpx;
  line-height: 56rpx;
  padding: 0 28rpx;
  font-size: 24rpx;
  font-weight: 700;
  border-radius: 28rpx;
  margin: 0;
}

.batch-action-btn.btn-ready {
  background: #6F4E37;
  color: #FAF6EE;
  box-shadow: 0 4rpx 10rpx rgba(111, 78, 55, 0.2);
}

.batch-action-btn.btn-verify {
  background: #E07A5F;
  color: #ffffff;
  box-shadow: 0 4rpx 10rpx rgba(224, 122, 95, 0.25);
}

/* Order List */
.order-list-scroll {
  flex: 1;
  height: calc(100vh - 220rpx);
  padding: 20rpx;
  box-sizing: border-box;
}

.order-card {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 24rpx 28rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 16rpx rgba(111, 78, 55, 0.06);
  border: 1rpx solid #EFE4D6;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1rpx dashed #EFE4D6;
  padding-bottom: 16rpx;
  margin-bottom: 16rpx;
}

.order-sn-col {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex: 1;
  min-width: 0;
  margin-right: 12rpx;
}

.order-sn-text {
  font-size: 24rpx;
  color: #8C7063;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.status-badge {
  font-size: 24rpx;
  font-weight: 700;
  padding: 4rpx 14rpx;
  border-radius: 12rpx;
}

.status-201 { background: rgba(224, 122, 95, 0.15); color: #E07A5F; }
.status-301 { background: rgba(111, 78, 55, 0.15); color: #6F4E37; }
.status-401, .status-402 { background: rgba(140, 112, 99, 0.15); color: #8C7063; }
.status-202 { background: rgba(217, 83, 79, 0.15); color: #D9534F; }

.order-goods-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-bottom: 16rpx;
}

.goods-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.goods-thumb {
  width: 90rpx;
  height: 90rpx;
  border-radius: 12rpx;
  background: #FAF6EE;
}

.goods-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.goods-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #4A3525;
}

.goods-spec {
  font-size: 22rpx;
  color: #A18276;
}

.goods-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4rpx;
}

.goods-price {
  font-size: 26rpx;
  font-weight: 600;
  color: #4A3525;
}

.goods-num {
  font-size: 22rpx;
  color: #8C7063;
}

.order-remark-box {
  background: #FAF4EB;
  padding: 12rpx 18rpx;
  border-radius: 12rpx;
  margin-bottom: 16rpx;
  font-size: 24rpx;
  line-height: 1.4;
}

.remark-label {
  font-weight: 700;
  color: #6F4E37;
}

.remark-val {
  color: #4A3525;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1rpx solid #F5ECE0;
  padding-top: 16rpx;
}

.price-time-box {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.order-time {
  font-size: 22rpx;
  color: #A18276;
}

.total-price {
  font-size: 24rpx;
  color: #4A3525;
}

.price-val {
  font-size: 30rpx;
  font-weight: 700;
  color: #E07A5F;
}

.card-actions {
  display: flex;
  gap: 12rpx;
}

.action-btn {
  height: 64rpx;
  line-height: 64rpx;
  padding: 0 20rpx;
  font-size: 24rpx;
  border-radius: 14rpx;
  font-weight: 600;
  margin: 0;
}

.secondary-btn {
  background: #FAF4EB;
  color: #6F4E37;
  border: 1rpx solid #E6D7C3;
}

.primary-btn {
  background: #6F4E37;
  color: #FAF6EE;
}

.primary-btn.highlight {
  background: #E07A5F;
  color: #ffffff;
}

.refund-btn {
  background: #D9534F;
  color: #ffffff;
}

/* TAB 2: GOODS & CATEGORY */
.goods-module {
  display: flex;
  flex-direction: column;
  flex: 1;
  height: calc(100vh - 180rpx);
  overflow: hidden;
}

.goods-action-bar {
  background: #ffffff;
  padding: 16rpx 24rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1rpx solid #EAE0D3;
  flex-shrink: 0;
}

.goods-action-bar .bar-left {
  display: flex;
  align-items: baseline;
  gap: 12rpx;
}

.goods-action-bar .bar-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #4A3525;
}

.goods-action-bar .bar-count {
  font-size: 22rpx;
  color: #A18276;
}

.goods-action-bar .bar-right {
  display: flex;
  align-items: center;
  gap: 14rpx;
  flex-shrink: 0;
}

.action-btn-pill {
  padding: 10rpx 22rpx;
  border-radius: 30rpx;
  font-size: 24rpx;
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-weight: 600;
  transition: all 0.2s ease;
}

.action-btn-pill.primary {
  background: #6F4E37;
  color: #FAF6EE;
  box-shadow: 0 4rpx 10rpx rgba(111, 78, 55, 0.25);
}

.action-btn-pill.primary:active {
  transform: scale(0.96);
  background: #5C402D;
}

.action-btn-pill.secondary {
  background: #FAF4EB;
  color: #6F4E37;
  border: 1rpx solid #D6C2B4;
}

.action-btn-pill.secondary:active {
  background: #F0E6D8;
}

/* L1 Category Bar */
.l1-category-bar {
  background: #FAF4EB;
  padding: 14rpx 20rpx;
  border-bottom: 1rpx solid #EAE0D3;
  flex-shrink: 0;
}

.l1-scroll {
  white-space: nowrap;
  width: 100%;
}

.l1-scroll-inner {
  display: inline-flex;
  align-items: center;
  gap: 12rpx;
}

.l1-pill {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  padding: 10rpx 24rpx;
  border-radius: 28rpx;
  font-size: 24rpx;
  color: #8C7063;
  background: #ffffff;
  border: 1rpx solid #E6D7C3;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.l1-pill.active {
  background: #6F4E37;
  color: #FAF6EE;
  border-color: #6F4E37;
  font-weight: 700;
}

.l1-badge {
  font-size: 18rpx;
  background: rgba(161, 130, 118, 0.2);
  color: inherit;
  padding: 0 8rpx;
  border-radius: 12rpx;
}

.l1-pill.active .l1-badge {
  background: rgba(255, 255, 255, 0.3);
  color: #FAF6EE;
}

.l1-add-pill {
  display: inline-flex;
  align-items: center;
  gap: 4rpx;
  padding: 10rpx 20rpx;
  border-radius: 28rpx;
  font-size: 22rpx;
  color: #6F4E37;
  background: transparent;
  border: 1rpx dashed #A18276;
  flex-shrink: 0;
}

/* L2 Category Bar */
.l2-category-bar {
  background: #F4ECE0;
  padding: 12rpx 20rpx;
  border-bottom: 1rpx solid #E3D5C3;
  flex-shrink: 0;
}

.l2-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10rpx;
  font-size: 22rpx;
}

.l2-header-left {
  display: flex;
  align-items: center;
}

.l2-label {
  font-weight: 700;
  color: #6F4E37;
}

.l2-parent-name {
  color: #8C7063;
  font-weight: 600;
}

.l2-header-right {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.l2-action-link {
  font-size: 20rpx;
  color: #6F4E37;
  text-decoration: underline;
}

.l2-action-link.highlight {
  color: #E07A5F;
  font-weight: 600;
}

.l2-scroll {
  white-space: nowrap;
  width: 100%;
}

.l2-scroll-inner {
  display: inline-flex;
  align-items: center;
  gap: 12rpx;
}

.l2-pill {
  display: inline-flex;
  align-items: center;
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  font-size: 22rpx;
  color: #8C7063;
  background: #ffffff;
  border: 1rpx solid #DECFC0;
  flex-shrink: 0;
}

.l2-pill.active {
  background: #E07A5F;
  color: #ffffff;
  border-color: #E07A5F;
  font-weight: 700;
}

.l2-add-pill {
  display: inline-flex;
  align-items: center;
  gap: 4rpx;
  padding: 8rpx 18rpx;
  border-radius: 20rpx;
  font-size: 22rpx;
  color: #E07A5F;
  background: #ffffff;
  border: 1rpx dashed #E07A5F;
  flex-shrink: 0;
}

/* Goods List & Cards */
.goods-list-scroll {
  flex: 1;
  height: 0;
  padding: 20rpx;
  box-sizing: border-box;
}

.goods-cat-tag-box {
  margin: 6rpx 0;
}

.goods-cat-tag {
  display: inline-block;
  font-size: 20rpx;
  color: #6F4E37;
  background: #FAF4EB;
  border: 1rpx solid #E6D7C3;
  padding: 2rpx 10rpx;
  border-radius: 6rpx;
}

.empty-add-btn {
  margin-top: 24rpx;
  background: #6F4E37;
  color: #FAF6EE;
  font-size: 26rpx;
  font-weight: 700;
  padding: 12rpx 36rpx;
  border-radius: 30rpx;
}

.goods-card {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 20rpx;
  margin-bottom: 20rpx;
  display: flex;
  align-items: center;
  gap: 20rpx;
  border: 1rpx solid #EFE4D6;
  box-shadow: 0 4rpx 16rpx rgba(111, 78, 55, 0.06);
}

.goods-cover {
  width: 140rpx;
  height: 140rpx;
  border-radius: 16rpx;
  background: #FAF6EE;
  flex-shrink: 0;
}

.goods-main-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.goods-head-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.goods-name {
  font-size: 30rpx;
  font-weight: 700;
  color: #4A3525;
}

.soldout-tag {
  background: #FAF4EB;
  color: #A18276;
  font-size: 20rpx;
  padding: 2rpx 10rpx;
  border-radius: 8rpx;
  border: 1rpx solid #E6D7C3;
}

.goods-brief {
  font-size: 22rpx;
  color: #A18276;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 300rpx;
}

.goods-price-stock-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.price-tag {
  font-size: 30rpx;
  font-weight: 700;
  color: #E07A5F;
}

.stock-tag {
  font-size: 22rpx;
  color: #8C7063;
}

.goods-card-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12rpx;
}

.toggle-group {
  display: flex;
  align-items: center;
}

.toggle-item {
  display: flex;
  align-items: center;
  gap: 4rpx;
}

.toggle-label {
  font-size: 22rpx;
  color: #8C7063;
}

.action-btn-row {
  display: flex;
  gap: 10rpx;
}

.mini-btn {
  height: 52rpx;
  line-height: 52rpx;
  padding: 0 16rpx;
  font-size: 22rpx;
  border-radius: 12rpx;
  font-weight: 600;
  margin: 0;
}

.edit-btn {
  background: #FAF4EB;
  color: #6F4E37;
  border: 1rpx solid #E6D7C3;
}

.del-btn {
  background: #FFF1F0;
  color: #D9534F;
  border: 1rpx solid #FFCCC7;
}

/* TAB 3: DASHBOARD & HARDWARE */
.dashboard-module {
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 28rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #4A3525;
  margin-bottom: 16rpx;
  display: block;
}

.cashflow-title {
  margin-top: 24rpx;
}

.highlight-card {
  background: linear-gradient(135deg, #FAF6EE 0%, #FFFFFF 100%);
  border: 1rpx solid #E6D5B8;
}

.highlight-val {
  color: #E07A5F;
}

.tomorrow-preorder-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #FFFDF9;
  border: 1rpx dashed #E07A5F;
  border-radius: 18rpx;
  padding: 18rpx 24rpx;
  margin-top: 14rpx;
}

.preorder-card-left {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.preorder-card-icon {
  font-size: 32rpx;
}

.preorder-card-info {
  display: flex;
  flex-direction: column;
  gap: 2rpx;
}

.preorder-card-title {
  font-size: 24rpx;
  font-weight: 700;
  color: #4A3525;
}

.preorder-card-desc {
  font-size: 20rpx;
  color: #A18276;
}

.preorder-card-val {
  font-size: 32rpx;
  font-weight: 800;
  color: #E07A5F;
}

.perf-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18rpx;
}

.perf-card {
  background: #ffffff;
  padding: 28rpx;
  border-radius: 20rpx;
  border: 1rpx solid #EFE4D6;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  box-shadow: 0 4rpx 16rpx rgba(111, 78, 55, 0.06);
}

.perf-val {
  font-size: 38rpx;
  font-weight: 700;
  color: #6F4E37;
}

.perf-label {
  font-size: 22rpx;
  color: #A18276;
}

.printer-card, .operator-card {
  background: #ffffff;
  padding: 28rpx;
  border-radius: 20rpx;
  border: 1rpx solid #EFE4D6;
  box-shadow: 0 4rpx 16rpx rgba(111, 78, 55, 0.06);
}

.printer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.printer-info {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.printer-name {
  font-size: 30rpx;
  font-weight: 700;
  color: #4A3525;
}

.printer-sn {
  font-size: 22rpx;
  color: #A18276;
}

.printer-status-badge {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  font-size: 22rpx;
  font-weight: 600;
}

.status-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
}

.printer-status-badge.online {
  background: #EBF8EC;
  color: #388E3C;
}

.printer-status-badge.online .status-dot {
  background: #388E3C;
}

.printer-status-badge.offline {
  background: #FFF1F0;
  color: #D9534F;
}

.printer-status-badge.offline .status-dot {
  background: #D9534F;
}

.test-print-btn {
  height: 80rpx;
  line-height: 80rpx;
  background: #FAF4EB;
  color: #6F4E37;
  border: 2rpx solid #E6D7C3;
  font-size: 26rpx;
  font-weight: 700;
  border-radius: 16rpx;
}

.operator-info-row {
  display: flex;
  font-size: 26rpx;
  margin-bottom: 12rpx;
}

.op-label {
  color: #A18276;
  width: 200rpx;
}

.op-val {
  color: #4A3525;
  font-weight: 600;
}

.unbind-btn {
  height: 80rpx;
  line-height: 80rpx;
  background: #FFF1F0;
  color: #D9534F;
  border: 1rpx solid #FFCCC7;
  font-size: 26rpx;
  font-weight: 700;
  border-radius: 16rpx;
  margin-top: 24rpx;
}

/* Modals */
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  padding: 30rpx;
  box-sizing: border-box;
}

.modal-card {
  width: 100%;
  max-width: 640rpx;
  background: #ffffff;
  border-radius: 24rpx;
  padding: 36rpx;
  box-sizing: border-box;
}

.large-modal {
  max-height: 85vh;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.modal-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #4A3525;
}

.modal-close {
  font-size: 40rpx;
  color: #A18276;
  line-height: 1;
}

.modal-body {
  margin-bottom: 30rpx;
}

.modal-scroll-body {
  max-height: 60vh;
  padding-right: 10rpx;
}

.input-desc {
  font-size: 24rpx;
  color: #8C7063;
  margin-bottom: 16rpx;
  display: block;
}

.modal-input {
  height: 80rpx;
  background: #FAF6EE;
  border: 2rpx solid #E6D7C3;
  border-radius: 14rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  color: #4A3525;
}

.modal-footer {
  display: flex;
  gap: 16rpx;
  margin-top: 24rpx;
}

.cancel-modal-btn, .confirm-modal-btn, .agree-btn, .reject-btn {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 16rpx;
  font-size: 26rpx;
  font-weight: 700;
}

.cancel-modal-btn {
  background: #FAF4EB;
  color: #8C7063;
}

.confirm-modal-btn {
  background: #6F4E37;
  color: #FAF6EE;
}

.agree-btn {
  background: #6F4E37;
  color: #FAF6EE;
}

.reject-btn {
  background: #FFF1F0;
  color: #D9534F;
  border: 1rpx solid #FFCCC7;
}

/* Forms inside modal */
.form-row {
  margin-bottom: 20rpx;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.form-label {
  font-size: 24rpx;
  font-weight: 600;
  color: #4A3525;
}

.picker-display {
  height: 80rpx;
  background: #FAF6EE;
  border: 2rpx solid #E6D7C3;
  border-radius: 14rpx;
  padding: 0 20rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 28rpx;
  color: #4A3525;
}

.picker-arrow {
  color: #A18276;
}

.toggle-row {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
}

.image-uploader-box {
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.image-uploader {
  position: relative;
  width: 160rpx;
  height: 160rpx;
  border-radius: 16rpx;
  background: #FAF6EE;
  border: 2rpx dashed #E6D7C3;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.uploader-tap-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 10;
  opacity: 0;
  cursor: pointer;
}

.upload-preview {
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.upload-mask {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 40rpx;
  background: rgba(74, 53, 37, 0.72);
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.mask-text {
  color: #FAF6EE;
  font-size: 18rpx;
  pointer-events: none;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
  pointer-events: none;
}

.upload-icon {
  font-size: 40rpx;
}

.upload-hint {
  font-size: 18rpx;
  color: #A18276;
}

/* 规格模式切换 Tab */
.spec-type-tabs {
  display: flex;
  gap: 16rpx;
  width: 100%;
}

.spec-type-tab {
  flex: 1;
  height: 76rpx;
  border: 2rpx solid #E6D7C3;
  border-radius: 14rpx;
  background: #FAF6EE;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  transition: all 0.2s ease;
  font-size: 24rpx;
  color: #8C7063;
}

.spec-type-tab.active {
  border-color: #6F4E37;
  background: #FAF4EB;
  font-weight: 700;
  color: #6F4E37;
}

.tab-radio-circle {
  width: 24rpx;
  height: 24rpx;
  border-radius: 50%;
  border: 2rpx solid #A18276;
  box-sizing: border-box;
}

.spec-type-tab.active .tab-radio-circle {
  border-color: #6F4E37;
  background: #6F4E37;
  box-shadow: inset 0 0 0 4rpx #FAF4EB;
}

/* 多规格容器与卡片 */
.multi-spec-container {
  background: #FAF4EB;
  border-radius: 16rpx;
  padding: 20rpx;
  border: 1rpx solid #E6D7C3;
  margin-bottom: 20rpx;
}

.multi-spec-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.multi-spec-title {
  font-size: 26rpx;
  font-weight: 700;
  color: #6F4E37;
}

.multi-spec-tip {
  font-size: 20rpx;
  color: #A18276;
}

/* 快捷预设 */
.preset-tag-bar {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
  font-size: 22rpx;
}

.preset-label {
  color: #8C7063;
  flex-shrink: 0;
  font-size: 22rpx;
}

.preset-scroll {
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.preset-scroll-inner {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
}

.preset-pill {
  display: inline-flex;
  align-items: center;
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  font-size: 20rpx;
  background: #ffffff;
  color: #6F4E37;
  border: 1rpx solid #DCCDBA;
  flex-shrink: 0;
}

.preset-pill:active {
  background: #6F4E37;
  color: #FAF6EE;
}

/* 规格细项列表 */
.spec-items-list {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
  margin-bottom: 16rpx;
}

.spec-sku-card {
  background: #ffffff;
  border-radius: 14rpx;
  padding: 16rpx;
  border: 1rpx solid #EAE0D3;
  box-shadow: 0 2rpx 8rpx rgba(111, 78, 55, 0.04);
}

.sku-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}

.sku-seq-num {
  font-size: 22rpx;
  font-weight: 700;
  color: #6F4E37;
  background: #FAF4EB;
  padding: 2rpx 12rpx;
  border-radius: 8rpx;
}

.sku-del-btn {
  display: flex;
  align-items: center;
  gap: 4rpx;
  font-size: 22rpx;
  color: #D9534F;
}

.sku-fields-row {
  display: flex;
  gap: 12rpx;
  align-items: flex-start;
}

.sku-field {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.sku-field.name-field {
  flex: 1.6;
}

.sku-field.price-field {
  flex: 1.2;
}

.sku-field.num-field {
  flex: 1.2;
}

.field-sublabel {
  font-size: 20rpx;
  color: #8C7063;
  font-weight: 600;
}

.sku-modal-input {
  height: 64rpx;
  background: #FAF6EE;
  border: 1rpx solid #E6D7C3;
  border-radius: 10rpx;
  padding: 0 12rpx;
  font-size: 24rpx;
  color: #4A3525;
  box-sizing: border-box;
}

.sku-modal-input.price {
  color: #E07A5F;
  font-weight: 700;
}

.add-spec-row-btn {
  height: 68rpx;
  border-radius: 12rpx;
  border: 2rpx dashed #6F4E37;
  color: #6F4E37;
  background: rgba(111, 78, 55, 0.04);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  font-size: 24rpx;
  font-weight: 600;
}

.add-spec-row-btn:active {
  background: rgba(111, 78, 55, 0.1);
}

.multi-spec-summary {
  margin-top: 14rpx;
  font-size: 22rpx;
  color: #8C7063;
  text-align: right;
}

.summary-val {
  color: #E07A5F;
  font-weight: 700;
  font-size: 24rpx;
}

/* Category Tree in modal */
.add-cat-bar {
  margin-bottom: 16rpx;
}

.small-add-btn {
  height: 64rpx;
  line-height: 64rpx;
  background: #FAF4EB;
  color: #6F4E37;
  border: 2rpx solid #E6D7C3;
  font-size: 24rpx;
  font-weight: 700;
  border-radius: 12rpx;
}

.category-tree {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.tree-node {
  background: #FAF6EE;
  border-radius: 16rpx;
  padding: 16rpx;
  border: 1rpx solid #E6D7C3;
}

.l1-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.node-left {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.level-tag {
  font-size: 18rpx;
  font-weight: 700;
  padding: 2rpx 8rpx;
  border-radius: 6rpx;
}

.level-tag.l1 {
  background: #6F4E37;
  color: #FAF6EE;
}

.level-tag.l2 {
  background: #A18276;
  color: #FAF6EE;
}

.node-name {
  font-size: 26rpx;
  font-weight: 700;
  color: #4A3525;
}

.node-actions {
  display: flex;
  gap: 12rpx;
}

.node-action-btn {
  font-size: 22rpx;
  color: #6F4E37;
  font-weight: 600;
}

.node-action-btn.add-sub {
  color: #2E7D32;
}

.node-action-btn.del {
  color: #D9534F;
}

.l2-container {
  margin-top: 12rpx;
  padding-left: 20rpx;
  border-left: 2rpx dashed #DCCDBA;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.l2-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  padding: 8rpx 14rpx;
  border-radius: 10rpx;
  border: 1rpx solid #EFE4D6;
}

.sub-name {
  font-size: 24rpx;
  font-weight: 500;
}

.cat-level-desc {
  font-size: 26rpx;
  color: #6F4E37;
  font-weight: 700;
}

.cat-parent-desc {
  font-size: 26rpx;
  color: #E07A5F;
  font-weight: 700;
}

.sort-order-badge {
  font-size: 20rpx;
  color: #A18276;
  background: rgba(161, 130, 118, 0.12);
  padding: 2rpx 10rpx;
  border-radius: 6rpx;
}

.l2-empty-container {
  margin-top: 10rpx;
  padding: 8rpx 14rpx;
}

.l2-empty-tip {
  font-size: 22rpx;
  color: #A18276;
  font-style: italic;
}

.cat-empty-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60rpx 0;
  gap: 12rpx;
}

.refund-detail {
  background: #FAF4EB;
  padding: 16rpx;
  border-radius: 12rpx;
  margin-bottom: 20rpx;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.refund-item-text {
  font-size: 24rpx;
  color: #4A3525;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100rpx 0;
  gap: 16rpx;
}

.empty-icon {
  font-size: 64rpx;
}

.empty-text {
  font-size: 26rpx;
  color: #A18276;
}

/* Card Header Right & View Detail Arrow */
.card-header-right {
  display: flex;
  align-items: center;
  gap: 10rpx;
  flex-shrink: 0;
}

.view-detail-arrow {
  font-size: 32rpx;
  color: #A18276;
  font-weight: 600;
  line-height: 1;
}

/* Order Detail Modal */
.order-detail-modal {
  max-height: 85vh;
  display: flex;
  flex-direction: column;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.order-detail-scroll {
  max-height: 65vh;
  margin-bottom: 20rpx;
}

.detail-pickup-banner {
  background: linear-gradient(135deg, #6F4E37, #8C7063);
  border-radius: 16rpx;
  padding: 24rpx 28rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.banner-code-col {
  display: flex;
  flex-direction: column;
}

.banner-label {
  font-size: 22rpx;
  color: rgba(250, 246, 238, 0.8);
  margin-bottom: 4rpx;
}

.banner-code {
  font-size: 40rpx;
  font-weight: 800;
  color: #FAF6EE;
  letter-spacing: 2rpx;
}

.banner-order-type {
  background: rgba(255, 255, 255, 0.2);
  color: #FAF6EE;
  font-size: 24rpx;
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
}

.detail-section {
  margin-bottom: 24rpx;
}

.section-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}

.section-title {
  font-size: 26rpx;
  font-weight: 700;
  color: #4A3525;
  margin-bottom: 12rpx;
  display: block;
}

.section-subtitle {
  font-size: 22rpx;
  color: #8C7063;
}

.remark-content-box {
  background: #FFF9E6;
  border-left: 6rpx solid #E07A5F;
  border-radius: 8rpx;
  padding: 16rpx 20rpx;
}

.remark-text {
  font-size: 26rpx;
  color: #6F4E37;
  line-height: 1.4;
  font-weight: 500;
}

.detail-info-card {
  background: #FAF6EE;
  border-radius: 14rpx;
  padding: 18rpx 22rpx;
}

.info-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8rpx 0;
  font-size: 24rpx;
}

.info-k {
  color: #8C7063;
  flex-shrink: 0;
  min-width: 140rpx;
}

.info-v {
  color: #4A3525;
  font-weight: 500;
  text-align: right;
  word-break: break-all;
}

.phone-box, .sn-box {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.call-btn, .copy-sn-btn {
  font-size: 22rpx;
  color: #6F4E37;
  background: #EFE4D6;
  padding: 4rpx 14rpx;
  border-radius: 6rpx;
  font-weight: 600;
}

/* Goods List in Detail */
.detail-goods-list {
  background: #FAF6EE;
  border-radius: 14rpx;
  padding: 14rpx 20rpx;
}

.detail-goods-item {
  display: flex;
  align-items: center;
  padding: 14rpx 0;
  border-bottom: 1rpx dashed #EFE4D6;
}

.detail-goods-item:last-child {
  border-bottom: none;
}

.detail-goods-thumb {
  width: 90rpx;
  height: 90rpx;
  border-radius: 12rpx;
  margin-right: 16rpx;
  flex-shrink: 0;
  background: #ffffff;
}

.detail-goods-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  margin-right: 16rpx;
}

.detail-goods-name {
  font-size: 26rpx;
  color: #4A3525;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-goods-spec {
  font-size: 22rpx;
  color: #8C7063;
  margin-top: 4rpx;
}

.detail-goods-calc {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  flex-shrink: 0;
}

.detail-goods-price {
  font-size: 22rpx;
  color: #8C7063;
}

.detail-goods-num {
  font-size: 22rpx;
  color: #8C7063;
}

.detail-goods-subtotal {
  font-size: 26rpx;
  font-weight: 700;
  color: #4A3525;
  margin-top: 4rpx;
}

/* Fee Card */
.fee-card {
  padding: 16rpx 22rpx;
}

.fee-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6rpx 0;
  font-size: 24rpx;
}

.fee-k {
  color: #8C7063;
}

.fee-v {
  color: #4A3525;
  font-weight: 500;
}

.fee-v.discount {
  color: #388E3C;
}

.total-line {
  margin-top: 10rpx;
  padding-top: 12rpx;
  border-top: 1rpx solid #EFE4D6;
}

.total-k {
  font-size: 28rpx;
  font-weight: 700;
  color: #4A3525;
}

.total-v {
  font-size: 34rpx;
  font-weight: 800;
  color: #E07A5F;
}

/* Detail Modal Footer */
.detail-modal-footer {
  display: flex;
  gap: 12rpx;
  padding-top: 12rpx;
}

.footer-btn {
  flex: 1;
  height: 72rpx;
  line-height: 72rpx;
  font-size: 26rpx;
  font-weight: 600;
  border-radius: 16rpx;
  text-align: center;
  padding: 0;
  margin: 0;
}

.footer-btn.close {
  background: #FAF6EE;
  color: #6F4E37;
  border: 1rpx solid #EFE4D6;
}

.footer-btn.secondary {
  background: #FAF4EB;
  color: #6F4E37;
  border: 1rpx solid #E6D7C3;
}

.footer-btn.primary {
  background: #6F4E37;
  color: #ffffff;
}

.footer-btn.highlight {
  background: #E07A5F;
  color: #ffffff;
}

.footer-btn.refund {
  background: rgba(217, 83, 79, 0.15);
  color: #D9534F;
}
</style>
