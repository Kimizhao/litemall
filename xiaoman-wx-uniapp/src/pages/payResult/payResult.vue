<template>
  <view class="container">
    <!-- 1. 自提订单凭证小票 -->
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
          {{ isOrderPaid ? '已付款 · 待取餐' : '待付款 · 待支付' }}
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

      <view class="dashed-line"></view>

      <!-- 商品明细与价格 -->
      <view class="goods-breakdown">
        <view class="goods-row" v-for="g in orderGoods" :key="g.id">
          <text class="goods-title">{{ g.goodsName }} x{{ g.number }}</text>
          <text class="goods-amt">¥{{ (g.price * g.number).toFixed(1) }}</text>
        </view>
        <view class="total-row">
          <text>实付总额</text>
          <text class="total-price">¥{{ actualPrice }}</text>
        </view>
      </view>

      <view class="dashed-line"></view>

      <text class="receipt-footer-tip" v-if="isOrderPaid">下单并支付成功！小满食光正在加紧备餐，请凭取餐号到店取餐。</text>
      <text class="receipt-footer-tip unpaid-tip" v-else>订单已生成但尚未完成支付，请尽快付款以便门店为您制作。</text>

      <!-- 操作按钮区域 -->
      <view class="action-wrap">
        <button class="primary-btn pay-now-btn" v-if="!isOrderPaid" @tap="onPayRetry">立即付款</button>
        <button :class="isOrderPaid ? 'primary-btn' : 'secondary-btn'" @tap="onViewOrder">查看订单详情</button>
        <button class="secondary-btn" @tap="onGoShopping">返回首页</button>
      </view>
    </view>

    <!-- 2. 普通外卖配送订单支付结果页 (优雅降级) -->
    <view class="result-card glass-panel" v-else>
      <view class="icon-wrap" :class="isOrderPaid ? 'success-icon' : 'error-icon'">
        <text class="state-icon">{{ isOrderPaid ? '✓' : '✕' }}</text>
      </view>

      <text class="result-title">{{ isOrderPaid ? '下单成功' : '支付未完成' }}</text>
      <text class="result-desc">
        {{ isOrderPaid ? '您的外卖配送订单已提交成功，我们将尽快为您送达！' : '订单已生成但尚未完成支付，请前往订单中心或点击下方付款。' }}
      </text>

      <view class="divider"></view>

      <view class="action-wrap">
        <button class="primary-btn fail-btn" @tap="onPayRetry" v-if="!isOrderPaid">立即付款</button>
        <button :class="isOrderPaid ? 'primary-btn' : 'secondary-btn'" @tap="onViewOrder">查看订单</button>
        <button class="secondary-btn" @tap="onGoShopping">继续逛逛</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '../../utils/request'
import api from '../../config/api'
import { parseOrderRemark, generatePickupCode } from '../../utils/orderParser'
import { payOrder } from '../../utils/pay'

const payStatus = ref(false)
const orderId = ref(0)
const actualPrice = ref('0.00')

const orderInfo = ref(null)
const orderGoods = ref([])
const pickupInfo = ref(null)
const pickupCode = ref('')

const isOrderPaid = computed(() => {
  if (orderInfo.value && orderInfo.value.orderStatus) {
    const s = Number(orderInfo.value.orderStatus)
    return s !== 101 && s !== 102 && s !== 103
  }
  return payStatus.value
})

// 获取后台订单详情并做备注解析
const getOrderDetail = async () => {
  if (!orderId.value) return
  try {
    const data = await request({
      url: `${api.OrderDetail}?orderId=${orderId.value}`,
      method: 'GET'
    })
    if (data) {
      orderInfo.value = data.orderInfo || {}
      orderGoods.value = data.orderGoods || []
      actualPrice.value = (data.orderInfo && data.orderInfo.actualPrice) ? Number(data.orderInfo.actualPrice).toFixed(2) : '0.00'
      
      const message = data.orderInfo ? data.orderInfo.message : ''
      const parsed = parseOrderRemark(message)
      
      // 成功解析则启动自提小票渲染
      if (parsed) {
        pickupInfo.value = parsed
        pickupCode.value = generatePickupCode(orderId.value)
      }
    }
  } catch (err) {
    console.error('Failed to get order detail in payResult:', err)
  }
}

onLoad((options) => {
  if (options && options.status !== undefined) {
    payStatus.value = options.status === 'true' || options.status === '1'
  }
  const id = options && (options.orderId || options.id)
  if (id) {
    orderId.value = parseInt(id)
    getOrderDetail()
  }
})

const onViewOrder = () => {
  if (!orderId.value) {
    uni.navigateTo({
      url: '/pages/ucenter/order/order'
    })
    return
  }
  uni.navigateTo({
    url: `/pages/ucenter/orderDetail/orderDetail?id=${orderId.value}`
  })
}

const onPayRetry = async () => {
  if (!orderId.value) return
  const success = await payOrder(orderId.value)
  if (success) {
    payStatus.value = true
    await getOrderDetail()
  }
}

const onGoShopping = () => {
  uni.switchTab({
    url: '/pages/index/index'
  })
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background-color: #FAF6EE;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
  box-sizing: border-box;
  color: #4A3525;
}

/* 日系木纹质感自提小票卡片 */
.receipt-card {
  width: 100%;
  background-color: #FFFFFF;
  border-radius: 24rpx;
  border: 1rpx solid #E6D5B8;
  box-shadow: 0 12rpx 36rpx rgba(111, 78, 55, 0.08);
  padding: 48rpx;
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

.receipt-footer-tip.unpaid-tip {
  color: #E07A5F;
  font-weight: bold;
}

.pay-now-btn {
  background-color: #E07A5F;
  box-shadow: 0 6rpx 16rpx rgba(224, 122, 95, 0.25);
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

.goods-breakdown {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.goods-row {
  display: flex;
  justify-content: space-between;
  font-size: 24rpx;
  color: #A18276;
}

.goods-title {
  font-weight: 500;
}

.goods-amt {
  font-weight: bold;
  color: #4A3525;
}

.total-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16rpx;
  font-size: 26rpx;
  font-weight: 800;
}

.total-price {
  font-size: 34rpx;
  color: #E07A5F;
}

.receipt-footer-tip {
  font-size: 22rpx;
  color: #A18276;
  text-align: center;
  line-height: 1.4;
  margin: 20rpx 0;
}

/* 统一动作按钮 */
.action-wrap {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
  margin-top: 20rpx;
}

.mock-pay-btn {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 40rpx;
  background-color: #E07A5F;
  color: #FFFFFF;
  font-size: 26rpx;
  font-weight: bold;
  box-shadow: 0 6rpx 16rpx rgba(224, 122, 95, 0.25);
  border: none;
}

.primary-btn {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 40rpx;
  background-color: #6F4E37;
  color: #FFFFFF;
  font-size: 26rpx;
  font-weight: bold;
  box-shadow: 0 6rpx 16rpx rgba(111, 78, 55, 0.15);
  border: none;
}

.fail-btn {
  background-color: #E07A5F;
  box-shadow: 0 6rpx 16rpx rgba(224, 122, 95, 0.2);
}

.primary-btn:active, .mock-pay-btn:active {
  transform: scale(0.98);
}

.secondary-btn {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 40rpx;
  background-color: #F5EEDF;
  color: #6F4E37;
  font-size: 26rpx;
  font-weight: bold;
  border: 1rpx solid #E6D5B8;
}

.secondary-btn:active {
  background-color: #E6D5B8;
}

/* 普通结果卡片 */
.result-card {
  width: 100%;
  padding: 64rpx 48rpx;
  border-radius: 24rpx;
  border: 1rpx solid #E6D5B8;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 8rpx 24rpx rgba(111, 78, 55, 0.05);
  background-color: #FFFFFF;
}

.icon-wrap {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 32rpx;
}

.success-icon {
  background-color: #6F4E37;
  box-shadow: 0 8rpx 20rpx rgba(111, 78, 55, 0.2);
}

.error-icon {
  background-color: #E07A5F;
  box-shadow: 0 8rpx 20rpx rgba(224, 122, 95, 0.2);
}

.state-icon {
  color: #ffffff;
  font-size: 56rpx;
  font-weight: bold;
}

.result-title {
  font-size: 38rpx;
  font-weight: 800;
  color: #4A3525;
  margin-bottom: 16rpx;
}

.result-desc {
  font-size: 24rpx;
  color: #A18276;
  text-align: center;
  line-height: 1.5;
  margin-bottom: 48rpx;
}

.divider {
  width: 100%;
  height: 2rpx;
  background-color: #E6D5B8;
  margin-bottom: 48rpx;
}
</style>
