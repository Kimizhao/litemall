<template>
  <view class="container" v-if="checkoutData">
    <!-- Shipping Address Selector -->
    <view class="address-card glass-panel" @tap="onSelectAddress">
      <view class="address-info-wrap" v-if="address.id">
        <view class="address-title">
          <text class="user-name">{{ address.name }}</text>
          <text class="user-phone">{{ address.tel }}</text>
          <text class="tag-default" v-if="address.isDefault">默认</text>
        </view>
        <text class="address-text">{{ address.province }}{{ address.city }}{{ address.county }}{{ address.addressDetail }}</text>
      </view>
      <view class="address-empty" v-else>
        <text class="empty-icon">📍</text>
        <text class="empty-text">您尚未配置收货地址，立即添加</text>
      </view>
      <text class="arrow-right">></text>
    </view>

    <!-- Products List -->
    <view class="goods-card glass-panel">
      <view class="card-title">购买商品</view>
      <view class="goods-list">
        <view class="goods-item" v-for="item in checkedGoodsList" :key="item.id">
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

    <!-- Coupons and Message Options -->
    <view class="options-card glass-panel">
      <!-- Coupon Selection -->
      <view class="option-row" @tap="onSelectCoupon">
        <text class="option-label">优惠券</text>
        <view class="option-right">
          <text class="option-val coupon-val" v-if="couponPrice > 0">-¥{{ couponPrice }}</text>
          <text class="option-val" v-else-if="availableCouponLength > 0">{{ availableCouponLength }}张可用</text>
          <text class="option-val text-muted" v-else>无可用优惠券</text>
          <text class="arrow-right">></text>
        </view>
      </view>

      <!-- Message/Note input -->
      <view class="option-row no-arrow">
        <text class="option-label">备注留言</text>
        <input type="text" class="note-input" v-model="message" placeholder="请填写留言（选填）" />
      </view>
    </view>

    <!-- Cost Bill Calculator Breakdown -->
    <view class="bill-card glass-panel">
      <view class="bill-row">
        <text class="bill-label">商品合计</text>
        <text class="bill-val">¥{{ goodsTotalPrice }}</text>
      </view>
      <view class="bill-row">
        <text class="bill-label">运费</text>
        <text class="bill-val">¥{{ freightPrice }}</text>
      </view>
      <view class="bill-row" v-if="couponPrice > 0">
        <text class="bill-label">优惠券扣减</text>
        <text class="bill-val text-red">-¥{{ couponPrice }}</text>
      </view>
      <view class="bill-row" v-if="grouponPrice > 0">
        <text class="bill-label">团购折扣</text>
        <text class="bill-val text-red">-¥{{ grouponPrice }}</text>
      </view>
      <view class="divider"></view>
      <view class="bill-row total-row">
        <text class="total-label">应付总额</text>
        <text class="total-val">¥{{ actualPrice }}</text>
      </view>
    </view>

    <!-- Sticky Pay Bar -->
    <view class="checkout-footer glass-panel">
      <view class="footer-left">
        <text class="pay-label">实付款：</text>
        <text class="pay-symbol">¥</text>
        <text class="pay-price">{{ actualPrice }}</text>
      </view>
      <button class="pay-btn" @tap="onSubmitOrder" :disabled="submitting">
        {{ submitting ? '正在提交...' : '提交订单' }}
      </button>
    </view>
  </view>

  <view class="loading-wrap" v-else>
    <text class="loading-icon">⏳</text>
    <text class="loading-text">正在加载结算单数据...</text>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import request from '../../utils/request'
import api from '../../config/api'

const checkoutData = ref(null)
const address = ref({})
const checkedGoodsList = ref([])
const couponPrice = ref(0)
const grouponPrice = ref(0)
const freightPrice = ref(0)
const goodsTotalPrice = ref(0)
const actualPrice = ref(0)
const availableCouponLength = ref(0)

const message = ref('')
const submitting = ref(false)

// URL / Query parameters
const cartId = ref(0)
const addressId = ref(0)
const couponId = ref(0)
const userCouponId = ref(0)
const grouponLinkId = ref(0)
const grouponRulesId = ref(0)

onShow(() => {
  // Read selected address / coupon from storage if redirected
  const selectedAddressId = uni.getStorageSync('selectedAddressId')
  if (selectedAddressId) {
    addressId.value = parseInt(selectedAddressId)
    uni.removeStorageSync('selectedAddressId')
  }
  
  const selectedCouponId = uni.getStorageSync('selectedCouponId')
  const selectedUserCouponId = uni.getStorageSync('selectedUserCouponId')
  if (selectedCouponId) {
    couponId.value = parseInt(selectedCouponId)
    userCouponId.value = parseInt(selectedUserCouponId)
    uni.removeStorageSync('selectedCouponId')
    uni.removeStorageSync('selectedUserCouponId')
  }

  getCheckoutInfo()
})

const getCheckoutInfo = async () => {
  try {
    const data = await request({
      url: `${api.CartCheckout}?cartId=${cartId.value}&addressId=${addressId.value}&couponId=${couponId.value}&userCouponId=${userCouponId.value}&grouponRulesId=${grouponRulesId.value}`,
      method: 'GET'
    })
    
    if (data) {
      checkoutData.value = data
      address.value = data.checkedAddress || {}
      checkedGoodsList.value = data.checkedGoodsList || []
      couponPrice.value = data.couponPrice || 0
      grouponPrice.value = data.grouponPrice || 0
      freightPrice.value = data.freightPrice || 0
      goodsTotalPrice.value = data.goodsTotalPrice || 0
      actualPrice.value = data.actualPrice || 0
      availableCouponLength.value = data.availableCouponLength || 0
      
      // Update variables if returned
      if (data.checkedAddress && data.checkedAddress.id) {
        addressId.value = data.checkedAddress.id
      }
    }
  } catch (err) {
    console.error('Failed to get checkout info:', err)
  }
}

const onSelectAddress = () => {
  uni.navigateTo({
    url: '/pages/ucenter/address/address?isSelect=true'
  })
}

const onSelectCoupon = () => {
  if (availableCouponLength.value === 0) return
  uni.navigateTo({
    url: '/pages/ucenter/couponSelect/couponSelect'
  })
}

const onSubmitOrder = async () => {
  if (!addressId.value) {
    uni.showToast({
      title: '请先配置您的收货地址',
      icon: 'none'
    })
    return
  }

  submitting.value = true
  try {
    const data = await request({
      url: api.OrderSubmit,
      method: 'POST',
      data: {
        cartId: cartId.value,
        addressId: addressId.value,
        couponId: couponId.value,
        userCouponId: userCouponId.value,
        grouponRulesId: grouponRulesId.value,
        grouponLinkId: grouponLinkId.value,
        message: message.value
      }
    })
    
    if (data && data.orderId) {
      // Clean selected storage cache items
      uni.navigateTo({
        url: `/pages/payResult/payResult?status=true&orderId=${data.orderId}`
      })
    } else {
      uni.navigateTo({
        url: `/pages/payResult/payResult?status=false`
      })
    }
  } catch (err) {
    console.error('Order submit failed:', err)
    uni.navigateTo({
      url: `/pages/payResult/payResult?status=false`
    })
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background-color: var(--bg-primary);
  padding: 32rpx 32rpx 180rpx 32rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  box-sizing: border-box;
}

/* Shipping Address Card */
.address-card {
  display: flex;
  align-items: center;
  padding: 32rpx;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  position: relative;
}

.address-info-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.address-title {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.user-name {
  font-size: 30rpx;
  font-weight: 800;
  color: var(--text-main);
}

.user-phone {
  font-size: 26rpx;
  color: var(--text-muted);
  font-weight: 600;
}

.tag-default {
  padding: 2rpx 10rpx;
  background: var(--primary-gradient);
  color: #ffffff;
  font-size: 18rpx;
  font-weight: 700;
  border-radius: 4rpx;
}

.address-text {
  font-size: 24rpx;
  color: var(--text-muted);
  line-height: 1.4;
  font-weight: 500;
}

.address-empty {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.empty-icon {
  font-size: 36rpx;
}

.empty-text {
  font-size: 26rpx;
  color: var(--text-muted);
  font-weight: 700;
}

.arrow-right {
  font-size: 28rpx;
  color: #cbd5e1;
  font-weight: bold;
}

/* Goods list card */
.goods-card {
  padding: 32rpx;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
}

.card-title {
  font-size: 26rpx;
  font-weight: 800;
  color: var(--text-main);
  margin-bottom: 24rpx;
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

/* Options / Coupons Card */
.options-card {
  padding: 16rpx 32rpx;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
}

.option-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 96rpx;
  border-bottom: 1px solid var(--border-light);
}

.option-row:last-child {
  border-bottom: none;
}

.option-label {
  font-size: 26rpx;
  font-weight: 700;
  color: var(--text-main);
}

.option-right {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.option-val {
  font-size: 24rpx;
  color: var(--text-main);
  font-weight: 600;
}

.coupon-val {
  color: #ef4444;
}

.note-input {
  flex: 1;
  text-align: right;
  font-size: 24rpx;
  color: var(--text-main);
  font-weight: 600;
  padding-left: 32rpx;
}

.no-arrow {
  gap: 20rpx;
}

/* Bill breakdown card */
.bill-card {
  padding: 32rpx;
  border-radius: var(--radius-md);
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
  color: #ef4444;
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
  color: #ef4444;
  font-weight: 800;
}

/* Sticky footer */
.checkout-footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 140rpx;
  padding: 0 32rpx 0 40rpx;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1rpx solid rgba(226,232,240,0.5);
  box-shadow: 0 -8px 24px rgba(0, 0, 0, 0.04);
}

.footer-left {
  display: flex;
  align-items: baseline;
}

.pay-label {
  font-size: 24rpx;
  color: var(--text-muted);
  font-weight: 600;
}

.pay-symbol {
  font-size: 24rpx;
  color: #ef4444;
  font-weight: 700;
}

.pay-price {
  font-size: 38rpx;
  font-weight: 800;
  color: #ef4444;
}

.pay-btn {
  height: 88rpx;
  padding: 0 52rpx;
  border-radius: 44rpx;
  background: var(--primary-gradient);
  color: #ffffff;
  font-size: 26rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.25);
}

.pay-btn:active {
  opacity: 0.85;
}

/* Loading state */
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
</style>
