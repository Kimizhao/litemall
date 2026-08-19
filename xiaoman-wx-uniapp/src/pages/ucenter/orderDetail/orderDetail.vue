<template>
  <view class="container" v-if="orderInfo">
    <scroll-view scroll-y class="detail-scroll">
      <!-- Order Status Banner -->
      <view class="status-banner" :class="'bg-' + orderInfo.orderStatus">
        <text class="status-text">{{ orderInfo.orderStatusText }}</text>
        <text class="status-desc" v-if="orderInfo.orderStatus === 101">订单已提交，请尽快完成支付</text>
        <text class="status-desc" v-else-if="orderInfo.orderStatus === 201">
          {{ pickupInfo ? '已付款，请凭取餐号到店自取，小满食光正在加紧制作中' : '已付款，正在加紧打包中，请耐心等待' }}
        </text>
        <text class="status-desc" v-else-if="orderInfo.orderStatus === 301">
          {{ pickupInfo ? '餐品已制作完成，请凭取餐号前往门店取餐' : '已发货配送中，请保持手机畅通' }}
        </text>
        <text class="status-desc" v-else-if="orderInfo.orderStatus === 401 || orderInfo.orderStatus === 402">
          订单已完成，感谢您的光临！
        </text>
        <text class="status-desc" v-else-if="orderInfo.orderStatus === 102 || orderInfo.orderStatus === 103">
          订单已取消
        </text>
      </view>

      <!-- 1. 自提订单：在详情页顶部展示到店自取凭证小票 -->
      <view class="receipt-card glass-panel" v-if="pickupInfo">
        <view class="receipt-header">
          <text class="receipt-shop-name">小满食光</text>
          <text class="receipt-type-tag">到店自取凭证</text>
        </view>

        <!-- 取餐号展示 -->
        <view class="pickup-code-section">
          <text class="code-label">取餐号</text>
          <text class="code-number">{{ pickupCode }}</text>
          <text class="code-status" :class="{ paid: isOrderPaid, unpaid: !isOrderPaid }">
            {{ getPickupStatusText(orderInfo.orderStatus) }}
          </text>
        </view>

        <view class="dashed-line"></view>

        <!-- 预约自取信息项 -->
        <view class="info-list">
          <view class="info-item" v-if="pickupInfo.mode">
            <text class="info-label">预订方式</text>
            <text class="info-value highlight">{{ pickupInfo.mode }}</text>
          </view>
          <view class="info-item" v-else-if="pickupInfo.time">
            <text class="info-label">预约时间</text>
            <text class="info-value highlight">{{ pickupInfo.time }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">联系姓名</text>
            <text class="info-value">{{ pickupInfo.name || (orderInfo && orderInfo.consignee) || '' }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">联系电话</text>
            <text class="info-value">{{ pickupInfo.phone || (orderInfo && orderInfo.mobile) || '' }}</text>
          </view>
          <view class="info-item align-start">
            <text class="info-label">备注要求</text>
            <text class="info-value remark-text">{{ pickupInfo.remark || '无' }}</text>
          </view>
        </view>
      </view>

      <!-- 2. 普通外卖配送订单：展示收货地址 -->
      <view class="info-card glass-panel" v-else>
        <view class="address-header">
          <text class="user-name">{{ orderInfo.consignee }}</text>
          <text class="user-phone">{{ orderInfo.mobile }}</text>
        </view>
        <text class="address-text">{{ orderInfo.address }}</text>
      </view>

      <!-- Goods Items List -->
      <view class="goods-card glass-panel">
        <view class="goods-list">
          <view 
            class="goods-item" 
            v-for="item in orderGoods" 
            :key="item.id"
            @tap="onGoodsTap(item.goodsId)"
          >
            <image :src="item.picUrl" mode="aspectFill" class="goods-img" />
            <view class="goods-info">
              <text class="goods-name">{{ item.goodsName }}</text>
              <text class="goods-spec">{{ item.specifications.join(' ') }}</text>
              <view class="price-num-row">
                <text class="goods-price">¥{{ item.price }}</text>
                <text class="goods-num">x{{ item.number }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- Bill cost details -->
      <view class="bill-card glass-panel">
        <view class="bill-row">
          <text class="bill-label">商品总额</text>
          <text class="bill-val">¥{{ orderInfo.goodsPrice }}</text>
        </view>
        <view class="bill-row">
          <text class="bill-label">运费</text>
          <text class="bill-val">¥{{ orderInfo.freightPrice }}</text>
        </view>
        <view class="bill-row" v-if="orderInfo.couponPrice > 0">
          <text class="bill-label">优惠券</text>
          <text class="bill-val text-red">-¥{{ orderInfo.couponPrice }}</text>
        </view>
        <view class="divider"></view>
        <view class="bill-row total-row">
          <text class="total-label">实付款</text>
          <text class="total-val">¥{{ orderInfo.actualPrice }}</text>
        </view>
      </view>

      <!-- Order Metadata logs -->
      <view class="meta-card glass-panel">
        <view class="meta-row">
          <text class="meta-label">订单编号</text>
          <text class="meta-val">{{ orderInfo.orderSn }}</text>
        </view>
        <view class="meta-row">
          <text class="meta-label">创建时间</text>
          <text class="meta-val">{{ orderInfo.addTime }}</text>
        </view>
        <view class="meta-row" v-if="orderInfo.payTime">
          <text class="meta-label">付款时间</text>
          <text class="meta-val">{{ orderInfo.payTime }}</text>
        </view>
        <view class="meta-row" v-if="orderInfo.message">
          <text class="meta-label">备注留言</text>
          <text class="meta-val">{{ orderInfo.message }}</text>
        </view>
      </view>

      <view class="footer-spacer"></view>
    </scroll-view>

    <!-- Sticky Footer buttons -->
    <view class="detail-footer glass-panel" v-if="hasActionButtons">
      <button 
        class="action-btn cancel-btn" 
        v-if="orderInfo.handleOption.cancel" 
        @tap="onCancelOrder"
      >
        取消订单
      </button>
      <button 
        class="action-btn pay-btn" 
        v-if="orderInfo.handleOption.pay" 
        @tap="onPayOrder"
      >
        去付款
      </button>
      <button 
        class="action-btn confirm-btn" 
        v-if="orderInfo.handleOption.confirm" 
        @tap="onConfirmReceipt"
      >
        {{ pickupInfo ? '确认取餐' : '确认收货' }}
      </button>
    </view>
  </view>

  <view class="loading-wrap" v-else>
    <text class="loading-icon">⏳</text>
    <text class="loading-text">正在查询订单信息...</text>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import request from '../../../utils/request'
import api from '../../../config/api'
import { parseOrderRemark, generatePickupCode } from '../../../utils/orderParser'
import { payOrder } from '../../../utils/pay'

const orderId = ref(0)
const orderInfo = ref(null)
const orderGoods = ref([])
const pickupInfo = ref(null)
const pickupCode = ref('')

const isOrderPaid = computed(() => {
  if (!orderInfo.value || !orderInfo.value.orderStatus) return false
  const s = Number(orderInfo.value.orderStatus)
  return s !== 101 && s !== 102 && s !== 103
})

const getPickupStatusText = (status) => {
  const s = Number(status)
  if (s === 101) return '待付款 · 待支付'
  if (s === 201) return '已付款 · 制作中'
  if (s === 301) return '已制作 · 待取餐'
  if (s === 401 || s === 402) return '已完成'
  if (s === 102 || s === 103) return '已取消'
  return '处理中'
}

const loadData = (options) => {
  const id = (options && (options.id || options.orderId)) || orderId.value
  if (id) {
    orderId.value = parseInt(id)
    getOrderDetail()
  }
}

onLoad((options) => {
  loadData(options)
})

onShow(() => {
  if (orderId.value) {
    getOrderDetail()
  }
})

const getOrderDetail = async () => {
  if (!orderId.value) return
  try {
    const data = await request({
      url: `${api.OrderDetail}?orderId=${orderId.value}`,
      method: 'GET'
    })
    
    if (data) {
      orderInfo.value = data.orderInfo || null
      orderGoods.value = data.orderGoods || []
      
      if (orderInfo.value) {
        // 解析备注
        const message = orderInfo.value.message || ''
        const parsed = parseOrderRemark(message)
        if (parsed) {
          pickupInfo.value = parsed
          pickupCode.value = generatePickupCode(orderId.value)
          if (orderInfo.value.orderStatus === 201 || orderInfo.value.orderStatus === 301) {
            if (!orderInfo.value.handleOption) {
              orderInfo.value.handleOption = {}
            }
            orderInfo.value.handleOption.confirm = true
          }
        }
      }
    }
  } catch (err) {
    console.error('Failed to get order details:', err)
  }
}

const hasActionButtons = computed(() => {
  if (!orderInfo.value || !orderInfo.value.handleOption) return false
  const opt = orderInfo.value.handleOption
  return opt.cancel || opt.pay || opt.confirm
})

const onGoodsTap = (id) => {
  uni.navigateTo({
    url: `/pages/goods/goods?id=${id}`
  })
}

const onCancelOrder = () => {
  if (!orderId.value) return
  uni.showModal({
    title: '取消订单',
    content: '您确定要取消订单吗？',
    confirmText: '取消订单',
    cancelText: '取消',
    confirmColor: '#ef4444',
    success: async (res) => {
      if (res.confirm) {
        try {
          await request({
            url: api.OrderCancel,
            method: 'POST',
            data: { orderId: orderId.value }
          })
          uni.showToast({ title: '订单已取消', icon: 'success' })
          getOrderDetail()
        } catch (err) {
          console.error('Failed to cancel order:', err)
        }
      }
    }
  })
}

const onPayOrder = async () => {
  if (!orderId.value) return
  const success = await payOrder(orderId.value)
  if (success) {
    getOrderDetail()
  }
}

const onConfirmReceipt = () => {
  if (!orderId.value) return
  const isPickup = !!pickupInfo.value
  uni.showModal({
    title: isPickup ? '确认取餐' : '确认收货',
    content: isPickup ? '您已拿到预订的商品了吗？确认后订单将完成。' : '您已收到商品了吗？',
    confirmText: isPickup ? '确认取餐' : '确认收货',
    cancelText: '取消',
    confirmColor: '#8B8E4B',
    success: async (res) => {
      if (res.confirm) {
        try {
          await request({
            url: api.OrderConfirm,
            method: 'POST',
            data: { orderId: orderId.value }
          })
          uni.showToast({ title: isPickup ? '取餐完成' : '确认收货成功', icon: 'success' })
          getOrderDetail()
        } catch (err) {
          console.error('Failed to confirm receipt:', err)
        }
      }
    }
  })
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background-color: var(--bg-primary);
  display: flex;
  flex-direction: column;
}

.detail-scroll {
  flex: 1;
}

/* Status Banner overlay */
.status-banner {
  height: 240rpx;
  padding: 48rpx;
  display: flex;
  flex-direction: column;
  color: var(--text-main);
  box-sizing: border-box;
  background-color: #F5EEDF;
}

.bg-101 { background: var(--accent-gradient); color: #ffffff; } /* unpaid */
.bg-201 { background: var(--primary-gradient); color: #ffffff; } /* paid/unshipped */
.bg-301 { background: var(--primary-gradient); color: #ffffff; } /* shipped */
.bg-401 { background: var(--secondary-gradient); color: #ffffff; } /* completed */

.status-text {
  font-size: 34rpx;
  font-weight: 800;
  margin-bottom: 8rpx;
}

.status-desc {
  font-size: 20rpx;
  font-weight: 600;
  opacity: 0.9;
}

/* Info card */
.info-card {
  margin: -40rpx 32rpx 32rpx 32rpx;
  border-radius: var(--radius-md);
  padding: 32rpx;
  box-shadow: var(--shadow-md);
  border: 1px solid var(--border-light);
  position: relative;
  z-index: 10;
}

.address-header {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 12rpx;
}

.user-name {
  font-size: 28rpx;
  font-weight: 800;
  color: var(--text-main);
}

.user-phone {
  font-size: 24rpx;
  color: var(--text-muted);
  font-weight: 600;
}

.address-text {
  font-size: 24rpx;
  color: var(--text-muted);
  line-height: 1.4;
  font-weight: 500;
}

/* Goods Card list */
.goods-card {
  margin: 0 32rpx 32rpx 32rpx;
  border-radius: var(--radius-md);
  padding: 32rpx;
  border: 1px solid var(--border-light);
}

.goods-list {
  display: flex;
  flex-direction: column;
  gap: 28rpx;
}

.goods-item {
  display: flex;
  gap: 20rpx;
}

.goods-img {
  width: 120rpx;
  height: 120rpx;
  border-radius: var(--radius-sm);
  background-color: #f1f5f9;
}

.goods-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.goods-name {
  font-size: 24rpx;
  font-weight: 700;
  color: var(--text-main);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.3;
}

.goods-spec {
  font-size: 20rpx;
  color: var(--text-muted);
  font-weight: 600;
  margin-top: 4rpx;
}

.price-num-row {
  display: flex;
  justify-content: space-between;
  margin-top: auto;
  align-items: baseline;
}

.goods-price {
  font-size: 26rpx;
  font-weight: 700;
  color: var(--text-main);
}

.goods-num {
  font-size: 22rpx;
  color: var(--text-muted);
  font-weight: 700;
}

/* Bill Cost card */
.bill-card {
  margin: 0 32rpx 32rpx 32rpx;
  border-radius: var(--radius-md);
  padding: 32rpx;
  border: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.bill-row {
  display: flex;
  justify-content: space-between;
  font-size: 24rpx;
}

.bill-label {
  color: var(--text-muted);
  font-weight: 600;
}

.bill-val {
  color: var(--text-main);
  font-weight: 700;
}

.text-red {
  color: #E07A5F;
}

.divider {
  height: 1px;
  background-color: var(--border-light);
  margin: 8rpx 0;
}

.total-row {
  align-items: baseline;
}

.total-label {
  font-size: 26rpx;
  color: var(--text-main);
  font-weight: 800;
}

.total-val {
  font-size: 36rpx;
  color: #E07A5F;
  font-weight: 800;
}

/* Metadata card */
.meta-card {
  margin: 0 32rpx 32rpx 32rpx;
  border-radius: var(--radius-md);
  padding: 32rpx;
  border: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.meta-row {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.meta-label {
  font-size: 20rpx;
  color: var(--text-muted);
  font-weight: 600;
}

.meta-val {
  font-size: 24rpx;
  color: var(--text-main);
  font-weight: 600;
}

.footer-spacer {
  height: 180rpx;
}

/* Sticky footer */
.detail-footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 140rpx;
  padding: 16rpx 32rpx;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16rpx;
  border-top: 1rpx solid rgba(226,232,240,0.5);
  box-shadow: 0 -8px 24px rgba(0, 0, 0, 0.04);
}

.action-btn {
  height: 80rpx;
  padding: 0 36rpx;
  border-radius: 40rpx;
  font-size: 24rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cancel-btn {
  background-color: #ffffff;
  color: var(--text-muted);
  border: 1px solid var(--border-light);
}

.pay-btn, .confirm-btn {
  background: var(--primary-gradient);
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

.action-btn:active {
  opacity: 0.85;
}

/* Loading Wrap */
.loading-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
}

.loading-icon {
  font-size: 60rpx;
  margin-bottom: 16rpx;
  animation: spinner 1.5s infinite linear;
}

.loading-text {
  font-size: 24rpx;
  color: var(--text-muted);
  font-weight: 500;
}

@keyframes spinner {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 日系木纹质感自提小票卡片 */
.receipt-card {
  margin: -40rpx 32rpx 32rpx 32rpx;
  border-radius: var(--radius-md);
  background-color: #FFFFFF;
  border: 1px solid #E6D5B8;
  box-shadow: var(--shadow-md);
  padding: 40rpx 32rpx;
  position: relative;
  z-index: 10;
  display: flex;
  flex-direction: column;
}

.receipt-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.receipt-shop-name {
  font-size: 32rpx;
  font-weight: 800;
  color: #6F4E37;
  letter-spacing: 1rpx;
}

.receipt-type-tag {
  font-size: 20rpx;
  background-color: #F5EEDF;
  color: #6F4E37;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
  font-weight: bold;
}

.pickup-code-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30rpx 0;
}

.code-label {
  font-size: 24rpx;
  color: #A18276;
}

.code-number {
  font-size: 80rpx;
  font-weight: 900;
  color: #E07A5F;
  margin: 10rpx 0;
  letter-spacing: 2rpx;
}

.code-status {
  font-size: 24rpx;
  font-weight: bold;
  color: #A18276;
  background-color: #FAF6EE;
  padding: 4rpx 24rpx;
  border-radius: 20rpx;
}

.code-status.paid {
  color: #FFFFFF;
  background-color: #6F4E37;
}

.code-status.unpaid {
  color: #FFFFFF;
  background-color: #E07A5F;
}

.dashed-line {
  width: 100%;
  height: 2rpx;
  border-bottom: 2rpx dashed #E6D5B8;
  margin: 24rpx 0;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.info-item {
  display: flex;
  justify-content: space-between;
  font-size: 24rpx;
  color: #A18276;
}

.info-item.align-start {
  align-items: flex-start;
}

.info-label {
  width: 140rpx;
}

.info-value {
  flex: 1;
  text-align: right;
  font-weight: bold;
  color: #4A3525;
}

.info-value.highlight {
  color: #E07A5F;
}

.remark-text {
  text-align: justify;
}
</style>
