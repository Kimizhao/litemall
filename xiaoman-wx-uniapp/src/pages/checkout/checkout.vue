<template>
  <view class="container" v-if="checkoutData">
    <!-- 1. 商品明细卡片 -->
    <view class="goods-card glass-panel">
      <view class="card-title">预订明细</view>
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

    <!-- 2. 预订方式选择卡片 -->
    <view class="booking-card glass-panel">
      <view class="card-title">预订方式</view>
      
      <view class="booking-option-list">
        <view 
          class="booking-option-item" 
          :class="{ active: selectedMode === item.id }"
          v-for="item in bookingOptions" 
          :key="item.id"
          @tap="selectBookingMode(item.id)"
        >
          <view class="option-left">
            <view class="option-radio">
              <view class="radio-dot" v-if="selectedMode === item.id"></view>
            </view>
            <view class="option-info">
              <text class="option-name">{{ item.name }}</text>
              <text class="option-tip" v-if="item.tip">{{ item.tip }}</text>
            </view>
          </view>
          <view class="option-right" v-if="item.time">
            <text class="option-time-badge">{{ item.time }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 3. 联系与配送信息卡片 -->
    <view class="delivery-card glass-panel">
      <view class="card-header">
        <view class="card-title">{{ selectedMode === 'pickup' ? '取餐联系信息' : '配送联系信息' }}</view>
        <view class="switch-address-btn" @tap="onSelectAddress" v-if="checkedAddress">
          <text class="switch-text">切换地址</text>
          <text class="arrow-icon">❯</text>
        </view>
      </view>

      <!-- 已选择/默认收货地址 -->
      <view class="address-box" v-if="checkedAddress" @tap="onSelectAddress">
        <view class="address-user-info">
          <text class="user-name">{{ checkedAddress.name }}</text>
          <text class="user-phone">{{ checkedAddress.tel }}</text>
          <text class="tag-default" v-if="checkedAddress.isDefault">默认</text>
        </view>
        <view class="address-detail-text">
          <text class="location-icon">📍</text>
          <text class="detail-content">{{ fullAddressText }}</text>
        </view>
      </view>

      <!-- 暂无收货地址状态 -->
      <view class="no-address-box" v-else @tap="onSelectAddress">
        <view class="no-address-left">
          <text class="no-address-icon">📍</text>
          <view class="no-address-text-group">
            <text class="no-address-title">暂无联系地址</text>
            <text class="no-address-desc">点击前往地址管理添加联系人与地址</text>
          </view>
        </view>
        <view class="add-address-btn">+ 添加地址</view>
      </view>

      <!-- 备注 -->
      <view class="form-row align-start no-border remark-row">
        <text class="form-label">备注说明</text>
        <textarea 
          class="form-textarea" 
          v-model="remark" 
          :placeholder="remarkPlaceholder" 
          maxlength="80"
        />
      </view>
    </view>

    <!-- 4. 预订合计详情卡片 -->
    <view class="bill-card glass-panel">
      <view class="bill-row">
        <text class="bill-label">商品总额</text>
        <text class="bill-val">¥{{ goodsTotalPrice }}</text>
      </view>
      <view class="bill-row" v-if="couponPrice > 0">
        <text class="bill-label">优惠折扣</text>
        <text class="bill-val text-red">-¥{{ couponPrice }}</text>
      </view>
      <view class="divider"></view>
      <view class="bill-row total-row">
        <text class="total-label">预订总额</text>
        <text class="total-val">¥{{ computedTotalPrice }}</text>
      </view>
    </view>

    <!-- 5. 底部立即预订提交栏 -->
    <view class="checkout-footer glass-panel">
      <view class="footer-left">
        <text class="pay-label">总计：</text>
        <text class="pay-symbol">¥</text>
        <text class="pay-price">{{ computedTotalPrice }}</text>
      </view>
      <button class="pay-btn" @tap="onSubmitOrder" :disabled="submitting">
        {{ submitting ? '正在预订...' : '立即预订' }}
      </button>
    </view>
  </view>

  <view class="loading-wrap" v-else>
    <text class="loading-icon">⏳</text>
    <text class="loading-text">正在同步商品数据...</text>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onLoad } from '@dcloudio/uni-app'
import request from '../../utils/request'
import api from '../../config/api'
import { payOrder } from '../../utils/pay'

// Form state variables
const checkoutData = ref(null)
const checkedGoodsList = ref([])
const couponPrice = ref(0)
const goodsTotalPrice = ref(0)
const actualPrice = ref(0)

const cartId = ref(0)
const addressId = ref(0)
const couponId = ref(0)
const userCouponId = ref(0)
const grouponRulesId = ref(0)
const grouponLinkId = ref(0)

// 预订方式选项列表
const bookingOptions = [
  {
    id: 'shier',
    name: '市二',
    time: '07:00-08:00',
    tip: ''
  },
  {
    id: 'shida',
    name: '师大',
    time: '17:00-18:00',
    tip: ''
  },
  {
    id: 'pickup',
    name: '到店自取',
    time: '',
    tip: '请备注时间，至少提前半天'
  }
]

const selectedMode = ref('shier')

const selectBookingMode = (modeId) => {
  selectedMode.value = modeId
}

const checkedAddress = ref(null)
const contactName = ref('')
const contactPhone = ref('')
const deliveryAddress = ref('')
const remark = ref('')
const submitting = ref(false)
const savedAddresses = ref([])
const hasRedirectedForNoAddress = ref(false)

const remarkPlaceholder = computed(() => {
  if (selectedMode.value === 'pickup') {
    return '请在此备注您预计到店自取的时间（至少提前半天）'
  } else if (selectedMode.value === 'shier') {
    return '请在此备注具体几区/几号楼'
  } else if (selectedMode.value === 'shida') {
    return '请在此备注具体哪栋楼/宿舍号'
  }
})

const fullAddressText = computed(() => {
  if (!checkedAddress.value) return ''
  const addr = checkedAddress.value
  const region = [addr.province, addr.city, addr.county].filter(Boolean).join('')
  if (region && addr.addressDetail && addr.addressDetail.startsWith(region)) {
    return addr.addressDetail
  }
  return (region ? region + ' ' : '') + (addr.addressDetail || '')
})

const onSelectAddress = () => {
  uni.navigateTo({
    url: '/pages/ucenter/address/address?isSelect=true'
  })
}

onLoad((options) => {
  if (options.cartId) cartId.value = parseInt(options.cartId)
  if (options.grouponRulesId) grouponRulesId.value = parseInt(options.grouponRulesId)
  if (options.grouponLinkId) grouponLinkId.value = parseInt(options.grouponLinkId)
})

const computedTotalPrice = computed(() => {
  return actualPrice.value.toFixed(2)
})

const getCheckoutInfo = async () => {
  try {
    const data = await request({
      url: `${api.CartCheckout}?cartId=${cartId.value}&addressId=${addressId.value}&couponId=${couponId.value}&userCouponId=${userCouponId.value}&grouponRulesId=${grouponRulesId.value}`,
      method: 'GET'
    })
    
    if (data) {
      checkoutData.value = data
      checkedGoodsList.value = data.checkedGoodsList || []
      couponPrice.value = data.couponPrice || 0
      goodsTotalPrice.value = data.goodsTotalPrice || 0
      actualPrice.value = data.actualPrice || 0
      
      if (data.checkedAddress && data.checkedAddress.id) {
        addressId.value = data.checkedAddress.id
      }
    }
  } catch (err) {
    console.error('Failed to get checkout info:', err)
  }
}

onShow(async () => {
  // 1. Read cached selected address ID if user selected from address management page
  const cachedAddressId = uni.getStorageSync('selectedAddressId')
  if (cachedAddressId) {
    uni.removeStorageSync('selectedAddressId')
  }

  // 2. Fetch address list
  try {
    const res = await request({
      url: api.AddressList,
      method: 'GET'
    })
    savedAddresses.value = (res && res.list) || []
    
    const validAddresses = savedAddresses.value.filter(item => item.name !== '到店自提(小满食光)')
    
    let target = null
    if (cachedAddressId) {
      target = validAddresses.find(item => item.id === parseInt(cachedAddressId))
    }
    if (!target) {
      target = validAddresses.find(item => item.isDefault)
    }
    if (!target && validAddresses.length > 0) {
      target = validAddresses[0]
    }
    
    if (target) {
      checkedAddress.value = target
      addressId.value = target.id
      contactName.value = target.name || ''
      contactPhone.value = target.tel || ''
      deliveryAddress.value = target.addressDetail || ''
    } else {
      checkedAddress.value = null
      addressId.value = 0
      contactName.value = ''
      contactPhone.value = ''
      deliveryAddress.value = ''

      // If user has no address, redirect to address management page to add one
      if (!hasRedirectedForNoAddress.value) {
        hasRedirectedForNoAddress.value = true
        uni.showToast({ title: '请先添加联系地址', icon: 'none' })
        setTimeout(() => {
          onSelectAddress()
        }, 500)
      }
    }
  } catch (e) {
    console.error('Failed to load address list:', e)
  }
  await getCheckoutInfo()
})

const onSubmitOrder = async () => {
  // Form validations
  if (!checkedAddress.value || !addressId.value) {
    uni.showToast({ title: '请先添加并选择联系地址', icon: 'none' })
    setTimeout(() => {
      onSelectAddress()
    }, 500)
    return
  }

  if (selectedMode.value === 'pickup' && !remark.value.trim()) {
    uni.showToast({ title: '到店自取请在备注中填写取餐时间', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    // Format booking mode info inside order message remark
    const cleanRemark = remark.value.trim() ? remark.value.replace(/[\r\n|]/g, ' ').trim() : '无'
    let messageValue = ''
    if (selectedMode.value === 'pickup') {
      messageValue = `[到店自取] 备注:${cleanRemark}`
    } else {
      const opt = bookingOptions.find(o => o.id === selectedMode.value) || bookingOptions[0]
      messageValue = `[${opt.name} ${opt.time}] 备注:${cleanRemark}`
    }
    
    // Submit order using chosen addressId
    const data = await request({
      url: api.OrderSubmit,
      method: 'POST',
      data: {
        cartId: cartId.value,
        addressId: addressId.value,
        couponId: couponId.value,
        userCouponId: userCouponId.value,
        grouponRulesId: grouponRulesId.value,
        grouponLinkId: 0,
        message: messageValue
      }
    })
    
    if (data && data.orderId) {
      await payOrder(data.orderId)
    } else {
      uni.navigateTo({
        url: `/pages/payResult/payResult?status=false`
      })
    }
  } catch (err) {
    console.error('Submit order failed:', err)
    uni.showToast({
      title: err.message || '提交失败，请重试',
      icon: 'none'
    })
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background-color: #FAF6EE;
  padding: 32rpx 32rpx 180rpx 32rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  box-sizing: border-box;
  color: #4A3525;
}

.goods-card, .booking-card, .delivery-card, .bill-card {
  background-color: #FFFFFF;
  padding: 36rpx;
  border-radius: 32rpx;
  border: 1rpx solid rgba(230, 213, 184, 0.2);
  box-shadow: 0 6rpx 20rpx rgba(111, 78, 55, 0.04);
  display: flex;
  flex-direction: column;
}

.card-title {
  font-size: 28rpx;
  font-weight: 800;
  color: #6F4E37;
  border-left: 6rpx solid #6F4E37;
  padding-left: 12rpx;
  margin-bottom: 24rpx;
  line-height: 1;
}

/* Goods list */
.goods-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.goods-item {
  display: flex;
  gap: 20rpx;
}

.goods-img {
  width: 110rpx;
  height: 110rpx;
  border-radius: var(--radius-sm);
  background-color: #FAF6EE;
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
  font-size: 24rpx;
  font-weight: 700;
  color: var(--text-main);
}

.goods-num {
  font-size: 22rpx;
  color: var(--text-muted);
  font-weight: 700;
}

/* Booking Options List */
.booking-option-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.booking-option-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 28rpx;
  background-color: #FAF6EE;
  border-radius: 20rpx;
  border: 2rpx solid rgba(230, 213, 184, 0.4);
  transition: all 0.25s ease;
  box-sizing: border-box;
}

.booking-option-item.active {
  background-color: #FFFDF9;
  border-color: #6F4E37;
  box-shadow: 0 4rpx 14rpx rgba(111, 78, 55, 0.08);
}

.option-left {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.option-radio {
  width: 38rpx;
  height: 38rpx;
  border-radius: 50%;
  border: 3rpx solid #C4AFA5;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #FFFFFF;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.booking-option-item.active .option-radio {
  border-color: #6F4E37;
}

.radio-dot {
  width: 20rpx;
  height: 20rpx;
  border-radius: 50%;
  background-color: #6F4E37;
}

.option-info {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.option-name {
  font-size: 28rpx;
  font-weight: 800;
  color: #4A3525;
}

.option-tip {
  font-size: 22rpx;
  color: #E07A5F;
  font-weight: 600;
}

.option-right {
  display: flex;
  align-items: center;
}

.option-time-badge {
  font-size: 24rpx;
  font-weight: 800;
  color: #6F4E37;
  background-color: rgba(111, 78, 55, 0.1);
  padding: 6rpx 18rpx;
  border-radius: 14rpx;
}

.booking-option-item.active .option-time-badge {
  background-color: #6F4E37;
  color: #FFFFFF;
}

.reminder-icon {
  font-size: 26rpx;
}

.reminder-text {
  font-size: 22rpx;
  color: #E07A5F;
  font-weight: 600;
  line-height: 1.4;
}

/* Delivery Card Header & Switch Button */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.switch-address-btn {
  display: flex;
  align-items: center;
  gap: 4rpx;
  padding: 6rpx 16rpx;
  background-color: #FAF6EE;
  border-radius: 20rpx;
  border: 1rpx solid rgba(230, 213, 184, 0.4);
}

.switch-text {
  font-size: 22rpx;
  font-weight: 700;
  color: #6F4E37;
}

.arrow-icon {
  font-size: 18rpx;
  color: #A18276;
}

/* Address Box & Content */
.address-box {
  background-color: #FAF6EE;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-top: 12rpx;
  border: 1rpx solid rgba(230, 213, 184, 0.3);
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.address-user-info {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.user-name {
  font-size: 28rpx;
  font-weight: 800;
  color: #4A3525;
}

.user-phone {
  font-size: 24rpx;
  font-weight: 600;
  color: #A18276;
}

.tag-default {
  padding: 2rpx 12rpx;
  background: var(--primary-gradient);
  color: #ffffff;
  font-size: 18rpx;
  font-weight: 700;
  border-radius: 6rpx;
}

.address-detail-text {
  display: flex;
  align-items: flex-start;
  gap: 8rpx;
}

.location-icon {
  font-size: 24rpx;
}

.detail-content {
  font-size: 24rpx;
  color: #4A3525;
  font-weight: 500;
  line-height: 1.4;
}

/* No Address State Box */
.no-address-box {
  background-color: #FAF6EE;
  border-radius: 20rpx;
  padding: 28rpx 24rpx;
  margin-top: 12rpx;
  border: 1rpx dashed #E07A5F;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.no-address-left {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.no-address-icon {
  font-size: 40rpx;
}

.no-address-text-group {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.no-address-title {
  font-size: 26rpx;
  font-weight: 800;
  color: #E07A5F;
}

.no-address-desc {
  font-size: 20rpx;
  color: #A18276;
}

.add-address-btn {
  padding: 10rpx 24rpx;
  background-color: #E07A5F;
  color: #FFFFFF;
  font-size: 22rpx;
  font-weight: 700;
  border-radius: 24rpx;
}

.remark-row {
  margin-top: 16rpx;
}

/* Form Row & Inputs */
.form-row {
  display: flex;
  align-items: center;
  border-bottom: 1rpx solid rgba(230, 213, 184, 0.15);
  padding: 24rpx 0;
}

.form-row.align-start {
  align-items: flex-start;
}

.form-row.no-border {
  border-bottom: none;
  padding-bottom: 0;
}

.form-label {
  width: 140rpx;
  font-size: 26rpx;
  font-weight: 700;
  color: #4A3525;
}

.form-textarea {
  flex: 1;
  height: 120rpx;
  font-size: 26rpx;
  color: #4A3525;
  background-color: #FAF6EE;
  border-radius: 16rpx;
  padding: 16rpx;
  border: 1rpx solid rgba(230, 213, 184, 0.3);
  font-weight: 600;
}

/* Bill Cost card */
.bill-row {
  display: flex;
  justify-content: space-between;
  font-size: 24rpx;
  margin-bottom: 16rpx;
}

.bill-row:last-child {
  margin-bottom: 0;
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
  margin: 16rpx 0;
}

.total-row {
  align-items: baseline;
  margin-bottom: 0;
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

/* Sticky Footer */
.checkout-footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 140rpx;
  padding: 16rpx 40rpx;
  z-index: 99;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1rpx solid rgba(230, 213, 184, 0.25);
  box-shadow: 0 -8rpx 24rpx rgba(111, 78, 55, 0.05);
}

.footer-left {
  display: flex;
  align-items: baseline;
}

.pay-label {
  font-size: 26rpx;
  color: #4A3525;
  font-weight: 700;
}

.pay-symbol {
  font-size: 28rpx;
  color: #E07A5F;
  font-weight: bold;
  margin-right: 4rpx;
}

.pay-price {
  font-size: 44rpx;
  color: #E07A5F;
  font-weight: 900;
  letter-spacing: -1rpx;
}

.pay-btn {
  height: 88rpx;
  line-height: 88rpx;
  background: var(--primary-gradient);
  color: #FFFFFF;
  font-size: 28rpx;
  font-weight: 800;
  border-radius: 44rpx;
  padding: 0 56rpx;
  box-shadow: 0 6rpx 20rpx rgba(224, 122, 95, 0.3);
  border: none;
}

.pay-btn:active {
  transform: scale(0.97);
  box-shadow: 0 4rpx 12rpx rgba(224, 122, 95, 0.2);
}

.pay-btn[disabled] {
  opacity: 0.6;
}

/* Loading Wrap */
.loading-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  color: var(--text-muted);
}

.loading-icon {
  font-size: 60rpx;
  margin-bottom: 16rpx;
  animation: spinner 1.5s infinite linear;
}

.loading-text {
  font-size: 24rpx;
  font-weight: 500;
}

@keyframes spinner {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>
