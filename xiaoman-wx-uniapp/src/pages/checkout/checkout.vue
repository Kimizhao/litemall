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

    <!-- 2. 预订核心表单卡片 -->
    <view class="booking-card glass-panel">
      <view class="card-title">预订时间</view>
      
      <!-- 日期选择 (今天 / 明天) -->
      <view class="form-row date-row">
        <text class="form-label">预订日期</text>
        <view class="segmented-control">
          <view 
            class="segment-item" 
            :class="{ active: bookingDate === '今天', disabled: !isTodayAvailable }" 
            @tap="selectDate('今天')"
          >今天</view>
          <view 
            class="segment-item" 
            :class="{ active: bookingDate === '明天' }" 
            @tap="selectDate('明天')"
          >明天</view>
        </view>
      </view>

      <!-- 时间选择 (早餐 8:00 / 午餐 12:00 / 晚餐 18:00) -->
      <view class="form-row time-row">
        <text class="form-label">送达时间</text>
        <view class="segmented-control">
          <view 
            class="segment-item" 
            :class="{ active: bookingTime === '早餐 8:00', disabled: !isTimeSlotAvailable(bookingDate, '早餐 8:00') }" 
            @tap="selectTime('早餐 8:00')"
          >早餐 8:00</view>
          <view 
            class="segment-item" 
            :class="{ active: bookingTime === '午餐 12:00', disabled: !isTimeSlotAvailable(bookingDate, '午餐 12:00') }" 
            @tap="selectTime('午餐 12:00')"
          >午餐 12:00</view>
          <view 
            class="segment-item" 
            :class="{ active: bookingTime === '晚餐 18:00', disabled: !isTimeSlotAvailable(bookingDate, '晚餐 18:00') }" 
            @tap="selectTime('晚餐 18:00')"
          >晚餐 18:00</view>
        </view>
      </view>
    </view>

    <!-- 3. 配送与联系人表单卡片 -->
    <view class="delivery-card glass-panel">
      <view class="card-title">配送信息</view>

      <!-- 联系人 -->
      <view class="form-row">
        <text class="form-label">联系人</text>
        <input 
          type="text" 
          class="form-input" 
          v-model="contactName" 
          placeholder="请输入联系人姓名" 
          maxlength="15"
        />
      </view>

      <!-- 联系电话 -->
      <view class="form-row">
        <text class="form-label">联系电话</text>
        <input 
          type="number" 
          class="form-input" 
          v-model="contactPhone" 
          placeholder="请输入联系电话" 
          maxlength="11"
        />
      </view>

      <!-- 配送地址 -->
      <view class="form-row align-start">
        <text class="form-label address-label">配送地址</text>
        <textarea 
          class="form-textarea address-textarea" 
          v-model="deliveryAddress" 
          placeholder="请输入详细的配送地址" 
          maxlength="100"
        />
      </view>

      <!-- 备注 -->
      <view class="form-row align-start no-border">
        <text class="form-label">备注说明</text>
        <textarea 
          class="form-textarea" 
          v-model="remark" 
          placeholder="口味偏好/忌口要求/配送叮嘱（选填）" 
          maxlength="60"
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

const bookingDate = ref('今天')
const bookingTime = ref('午餐 12:00')

const isTimeSlotAvailable = (dateOption, timeOption) => {
  if (dateOption === '明天') {
    return true
  }
  const now = new Date()
  const currentHour = now.getHours()
  const currentMinute = now.getMinutes()
  const currentTotalMinutes = currentHour * 60 + currentMinute
  
  let targetTotalMinutes = 0
  if (timeOption === '早餐 8:00') {
    targetTotalMinutes = 8 * 60
  } else if (timeOption === '午餐 12:00') {
    targetTotalMinutes = 12 * 60
  } else if (timeOption === '晚餐 18:00') {
    targetTotalMinutes = 18 * 60
  }
  
  return (currentTotalMinutes + 30) <= targetTotalMinutes
}

const isTodayAvailable = computed(() => {
  return isTimeSlotAvailable('今天', '早餐 8:00') || 
         isTimeSlotAvailable('今天', '午餐 12:00') || 
         isTimeSlotAvailable('今天', '晚餐 18:00')
})

const selectDate = (date) => {
  if (date === '今天' && !isTodayAvailable.value) {
    uni.showToast({ title: '今天已无可预订的时间段', icon: 'none' })
    return
  }
  bookingDate.value = date
  
  // When switching date, make sure bookingTime is still valid for the selected date
  if (!isTimeSlotAvailable(date, bookingTime.value)) {
    // Select first available slot
    if (isTimeSlotAvailable(date, '早餐 8:00')) {
      bookingTime.value = '早餐 8:00'
    } else if (isTimeSlotAvailable(date, '午餐 12:00')) {
      bookingTime.value = '午餐 12:00'
    } else {
      bookingTime.value = '晚餐 18:00'
    }
  }
}

const selectTime = (time) => {
  if (!isTimeSlotAvailable(bookingDate.value, time)) {
    uni.showToast({ title: '该送餐时间需提前半小时预订', icon: 'none' })
    return
  }
  bookingTime.value = time
}

const initBookingDateTime = () => {
  if (isTodayAvailable.value) {
    bookingDate.value = '今天'
    if (isTimeSlotAvailable('今天', '早餐 8:00')) {
      bookingTime.value = '早餐 8:00'
    } else if (isTimeSlotAvailable('今天', '午餐 12:00')) {
      bookingTime.value = '午餐 12:00'
    } else {
      bookingTime.value = '晚餐 18:00'
    }
  } else {
    bookingDate.value = '明天'
    bookingTime.value = '早餐 8:00'
  }
}
const contactName = ref('')
const contactPhone = ref('')
const deliveryAddress = ref('')
const remark = ref('')
const submitting = ref(false)
const savedAddresses = ref([])

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
  initBookingDateTime()
  try {
    const res = await request({
      url: api.AddressList,
      method: 'GET'
    })
    savedAddresses.value = (res && res.list) || []
    
    // Auto populate default address details (excluding virtual self-pickup address)
    const normal = savedAddresses.value.find(item => item.name !== '到店自提(小满食光)')
    if (normal) {
      addressId.value = normal.id
      contactName.value = normal.name || ''
      contactPhone.value = normal.tel || ''
      deliveryAddress.value = normal.addressDetail || ''
    }
  } catch (e) {
    console.error(e)
  }
  await getCheckoutInfo()
})

const onSubmitOrder = async () => {
  // Form validations
  if (!isTimeSlotAvailable(bookingDate.value, bookingTime.value)) {
    uni.showToast({ title: '所选的送餐时间已失效，请重新选择', icon: 'none' })
    return
  }
  if (!contactName.value.trim()) {
    uni.showToast({ title: '请填写联系人姓名', icon: 'none' })
    return
  }
  if (!contactPhone.value.trim()) {
    uni.showToast({ title: '请填写联系电话', icon: 'none' })
    return
  }
  if (!/^1[3-9]\d{9}$/.test(contactPhone.value.trim())) {
    uni.showToast({ title: '请输入合规 of 11位手机号', icon: 'none' })
    return
  }
  if (!deliveryAddress.value.trim()) {
    uni.showToast({ title: '请填写详细的配送地址', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    // Determine whether current addressId is the virtual one
    const isVirtual = savedAddresses.value.some(item => item.id === addressId.value && item.name === '到店自提(小满食光)')
    const saveId = (isVirtual || !addressId.value) ? 0 : addressId.value

    // 1. Silent-save user's inputted address to the database (so we get a valid address ID)
    const addressData = await request({
      url: api.AddressSave,
      method: 'POST',
      data: {
        id: saveId,
        name: contactName.value.trim(),
        tel: contactPhone.value.trim(),
        province: '福建省',
        city: '福州市',
        county: '仓山区',
        areaCode: '350104', // Fuzhou Cangshan Area Code
        addressDetail: deliveryAddress.value.trim(),
        isDefault: true
      }
    })
    
    if (!addressData) {
      throw new Error('地址保存失败，请检查输入后重试')
    }
    
    addressId.value = addressData

    // 2. Format custom booking info inside order message remark
    const cleanRemark = remark.value.trim() ? remark.value.replace(/[\r\n|]/g, ' ').trim() : '无'
    const messageValue = `[预约送餐] 时间:${bookingDate.value} ${bookingTime.value} | 称呼:${contactName.value.trim()} | 电话:${contactPhone.value.trim()} | 备注:${cleanRemark}`
    
    // 3. Submit order
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
      // Mock payment result direct redirect
      uni.navigateTo({
        url: `/pages/payResult/payResult?status=true&orderId=${data.orderId}`
      })
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

.form-input {
  flex: 1;
  height: 50rpx;
  font-size: 26rpx;
  color: #4A3525;
  font-weight: 600;
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

.address-label {
  margin-top: 12rpx;
}

/* Segmented Control (Date & Time Selector) */
.segmented-control {
  flex: 1;
  display: flex;
  background-color: #F5EEDF;
  border-radius: 24rpx;
  padding: 6rpx;
  gap: 6rpx;
}

.segment-item {
  flex: 1;
  text-align: center;
  height: 60rpx;
  line-height: 60rpx;
  font-size: 24rpx;
  font-weight: bold;
  color: #A18276;
  border-radius: 20rpx;
  transition: all 0.2s ease;
}

.segment-item.active {
  background-color: #6F4E37;
  color: #FFFFFF;
  box-shadow: 0 4rpx 10rpx rgba(111, 78, 55, 0.15);
}

.segment-item.disabled {
  color: #D2C2B9;
  background-color: #EDE6D8;
  opacity: 0.65;
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
