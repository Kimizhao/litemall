<template>
  <view class="container" v-if="checkoutData">
    <!-- 1. 到店自取/外卖配送方式切换 -->
    <view class="type-selector-card glass-panel">
      <view 
        class="type-tab" 
        :class="{ active: deliveryType === '自取' }" 
        @tap="onTypeChange('自取')"
      >到店自取 (预约)</view>
      <view 
        class="type-tab" 
        :class="{ active: deliveryType === '外卖' }" 
        @tap="onTypeChange('外卖')"
      >外卖配送</view>
    </view>

    <!-- 2. 到店自取专属表单 -->
    <view class="pickup-card glass-panel" v-if="deliveryType === '自取'">
      <view class="card-title">预约取餐信息</view>
      
      <!-- 姓名与性别 -->
      <view class="form-row">
        <text class="form-label">联络人</text>
        <view class="name-gender-wrap">
          <input 
            type="text" 
            class="form-input name-input" 
            v-model="pickupName" 
            placeholder="填写取餐姓名" 
            maxlength="10"
          />
          <view class="gender-selector">
            <view 
              class="gender-btn" 
              :class="{ active: pickupGender === '女士' }" 
              @tap="pickupGender = '女士'"
            >女士</view>
            <view 
              class="gender-btn" 
              :class="{ active: pickupGender === '先生' }" 
              @tap="pickupGender = '先生'"
            >先生</view>
          </view>
        </view>
      </view>

      <!-- 手机号 -->
      <view class="form-row">
        <text class="form-label">联系电话</text>
        <input 
          type="number" 
          class="form-input" 
          v-model="pickupPhone" 
          placeholder="接收取餐通知的手机号" 
          maxlength="11"
        />
      </view>

      <!-- 预约时间 -->
      <view class="form-row">
        <text class="form-label">取餐时间</text>
        <view class="time-picker-wrap">
          <picker mode="selector" :range="dateOptions" :value="0" @change="onDateChange">
            <view class="picker-trigger">{{ pickupDate }}</view>
          </picker>
          <text class="picker-separator">-</text>
          <picker mode="selector" :range="timeOptions" :value="selectedTimeIndex" @change="onTimeChange">
            <view class="picker-trigger highlight">{{ timeOptions[selectedTimeIndex] || '选择时间' }}</view>
          </picker>
        </view>
      </view>

      <!-- 特殊备注 -->
      <view class="form-row align-start">
        <text class="form-label">备注需求</text>
        <textarea 
          class="form-textarea" 
          v-model="pickupRemark" 
          placeholder="不要葱/少糖/打包等要求（选填）" 
          maxlength="50"
        />
      </view>
    </view>

    <!-- 3. 外卖配送地址选择器 -->
    <view class="address-card glass-panel" v-if="deliveryType === '外卖'" @tap="onSelectAddress">
      <view class="address-info-wrap" v-if="address.id && address.name !== '到店自提(小满食光)'">
        <view class="address-title">
          <text class="user-name">{{ address.name }}</text>
          <text class="user-phone">{{ address.tel }}</text>
          <text class="tag-default" v-if="address.isDefault">默认</text>
        </view>
        <text class="address-text">{{ address.province }}{{ address.city }}{{ address.county }}{{ address.addressDetail }}</text>
      </view>
      <view class="address-empty" v-else>
        <text class="empty-icon">📍</text>
        <text class="empty-text">选择外卖配送收货地址</text>
      </view>
      <text class="arrow-right">></text>
    </view>

    <!-- 4. 商品明细列表 -->
    <view class="goods-card glass-panel">
      <view class="card-title">购买明细</view>
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

    <!-- 5. 优惠券选择 -->
    <view class="options-card glass-panel">
      <view class="option-row" @tap="onSelectCoupon">
        <text class="option-label">优惠券</text>
        <view class="option-right">
          <text class="option-val coupon-val" v-if="couponPrice > 0">-¥{{ couponPrice }}</text>
          <text class="option-val" v-else-if="availableCouponLength > 0">{{ availableCouponLength }}张可用</text>
          <text class="option-val text-muted" v-else>无可用优惠券</text>
          <text class="arrow-right">></text>
        </view>
      </view>
      <!-- 仅外卖配送模式展示常规备注留言输入 -->
      <view class="option-row no-arrow" v-if="deliveryType === '外卖'">
        <text class="option-label">备注留言</text>
        <input type="text" class="note-input" v-model="message" placeholder="请填写外卖备注留言（选填）" />
      </view>
    </view>

    <!-- 6. 结算账单详情 -->
    <view class="bill-card glass-panel">
      <view class="bill-row">
        <text class="bill-label">商品合计</text>
        <text class="bill-val">¥{{ goodsTotalPrice }}</text>
      </view>
      <view class="bill-row" v-if="deliveryType === '外卖'">
        <text class="bill-label">配送运费</text>
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
        <text class="total-val">¥{{ computedTotalPrice }}</text>
      </view>
    </view>

    <!-- 7. 底部常驻付款栏 -->
    <view class="checkout-footer glass-panel">
      <view class="footer-left">
        <text class="pay-label">实付款：</text>
        <text class="pay-symbol">¥</text>
        <text class="pay-price">{{ computedTotalPrice }}</text>
      </view>
      <button class="pay-btn" @tap="onSubmitOrder" :disabled="submitting || isSavingAddress">
        {{ submitting ? '正在提交...' : '提交订单' }}
      </button>
    </view>
  </view>

  <view class="loading-wrap" v-else>
    <text class="loading-icon">⏳</text>
    <text class="loading-text">正在计算结算单数据...</text>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
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

// 购物车与传参
const cartId = ref(0)
const addressId = ref(0)
const couponId = ref(0)
const userCouponId = ref(0)
const grouponLinkId = ref(0)
const grouponRulesId = ref(0)

// 预约点餐逻辑字段
const deliveryType = ref('自取') // '自取' 或 '外卖'
const pickupName = ref('')
const pickupGender = ref('女士') // '先生' / '女士'
const pickupPhone = ref('')
const pickupRemark = ref('')

const dateOptions = ref(['今日', '明日'])
const pickupDate = ref('今日')
const selectedTimeIndex = ref(0)

const isSavingAddress = ref(false)
const virtualAddressId = ref(0)
const savedAddresses = ref([])

// 到店自取应收运费强制为 0
const computedTotalPrice = computed(() => {
  if (deliveryType.value === '自取') {
    // 减去配送运费
    const amt = actualPrice.value - freightPrice.value
    return amt > 0 ? amt.toFixed(2) : '0.00'
  }
  return actualPrice.value.toFixed(2)
})

// 预约取餐小时和分钟区间生成（08:00 - 21:00，间隔 15 分钟）
const timeOptions = computed(() => {
  const options = []
  const startHour = 8
  const endHour = 21
  
  const now = new Date()
  const currentHour = now.getHours()
  const currentMinute = now.getMinutes()
  
  for (let h = startHour; h <= endHour; h++) {
    for (let m = 0; m < 60; m += 15) {
      if (h === endHour && m > 0) break // 到 21:00 截止
      
      const hourStr = String(h).padStart(2, '0')
      const minStr = String(m).padStart(2, '0')
      const timeStr = `${hourStr}:${minStr}`
      
      if (pickupDate.value === '今日') {
        const optionTime = new Date()
        optionTime.setHours(h, m, 0, 0)
        // 限定取餐时间至少比现在晚 15 分钟
        const diffMins = (optionTime - now) / (1000 * 60)
        if (diffMins < 15) {
          continue
        }
      }
      options.push(timeStr)
    }
  }
  return options
})

// 准备与创建到店自取专用的虚拟地址，防止后端验证报错与商家业务误判
const checkAndPrepareVirtualAddress = async () => {
  if (isSavingAddress.value) return
  isSavingAddress.value = true
  
  try {
    const res = await request({
      url: api.AddressList,
      method: 'GET'
    })
    
    const list = res || []
    savedAddresses.value = list
    
    // 唯一匹配名称为 '到店自提(小满食光)' 的地址
    const found = list.find(item => item.name === '到店自提(小满食光)')
    if (found) {
      virtualAddressId.value = found.id
      addressId.value = found.id
    } else {
      // 携带 areaCode 静默创建虚拟自提地址
      const newAddr = await request({
        url: api.AddressSave,
        method: 'POST',
        data: {
          name: '到店自提(小满食光)',
          tel: '13800000000',
          province: '虚拟自提',
          city: '门店自取',
          county: '小满食光手作店',
          areaCode: '310115', // 上海浦东新区编码，防后端必填拦截
          addressDetail: '到店自取专属地址',
          isDefault: false
        }
      })
      if (newAddr && newAddr.id) {
        virtualAddressId.value = newAddr.id
        addressId.value = newAddr.id
      }
    }
    
    // 重新获取结算账单
    await getCheckoutInfo()
  } catch (err) {
    console.error('Failed to prepare virtual address:', err)
    uni.showToast({
      title: '创建自提凭证失败，请重试',
      icon: 'none'
    })
  } finally {
    isSavingAddress.value = false
  }
}

// 切换配送与自取
const onTypeChange = async (type) => {
  if (isSavingAddress.value) return;
  deliveryType.value = type
  
  if (type === '自取') {
    if (virtualAddressId.value) {
      addressId.value = virtualAddressId.value
      await getCheckoutInfo()
    } else {
      await checkAndPrepareVirtualAddress()
    }
  } else {
    // 还原外卖地址，选择第一个非自提的地址
    const selectedAddressId = uni.getStorageSync('selectedAddressId')
    if (selectedAddressId) {
      addressId.value = parseInt(selectedAddressId)
    } else {
      const normal = savedAddresses.value.find(item => item.name !== '到店自提(小满食光)')
      addressId.value = normal ? normal.id : 0
    }
    await getCheckoutInfo()
  }
}

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
      
      if (data.checkedAddress && data.checkedAddress.id) {
        addressId.value = data.checkedAddress.id
      }
    }
  } catch (err) {
    console.error('Failed to get checkout info:', err)
  }
}

onShow(async () => {
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

  // 页面进入时自启动地址准备工作
  if (deliveryType.value === '自取') {
    await checkAndPrepareVirtualAddress()
  } else {
    try {
      const res = await request({
        url: api.AddressList,
        method: 'GET'
      })
      savedAddresses.value = res || []
      if (!addressId.value) {
        const normal = savedAddresses.value.find(item => item.name !== '到店自提(小满食光)')
        addressId.value = normal ? normal.id : 0
      }
    } catch (e) {
      console.error(e)
    }
    await getCheckoutInfo()
  }
})

const onDateChange = (e) => {
  pickupDate.value = dateOptions.value[e.detail.value]
  selectedTimeIndex.value = 0
}

const onTimeChange = (e) => {
  selectedTimeIndex.value = parseInt(e.detail.value)
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

// 提交订单，进行表单验证并打包备注信息
const onSubmitOrder = async () => {
  if (deliveryType.value === '外卖') {
    if (!addressId.value || addressId.value === virtualAddressId.value) {
      uni.showToast({
        title: '请先配置您的收货地址',
        icon: 'none'
      })
      return
    }
  } else {
    // 自提参数校验
    if (!pickupName.value.trim()) {
      uni.showToast({
        title: '请填写预约姓名',
        icon: 'none'
      })
      return
    }
    if (pickupName.value.trim().length > 10) {
      uni.showToast({
        title: '姓名过长(限10字内)',
        icon: 'none'
      })
      return
    }
    if (!pickupPhone.value) {
      uni.showToast({
        title: '请填写取餐电话',
        icon: 'none'
      })
      return
    }
    if (!/^1[3-9]\d{9}$/.test(pickupPhone.value)) {
      uni.showToast({
        title: '请输入合规的11位手机号',
        icon: 'none'
      })
      return
    }
    
    const times = timeOptions.value
    if (times.length === 0) {
      uni.showToast({
        title: '该日期没有可选时间段',
        icon: 'none'
      })
      return
    }
    const selectedTime = times[selectedTimeIndex.value]
    if (!selectedTime) {
      uni.showToast({
        title: '请选择预约取餐时间',
        icon: 'none'
      })
      return
    }
    
    // 拼接留言参数，屏蔽特殊字符与换行
    const cleanRemark = pickupRemark.value.trim() ? pickupRemark.value.replace(/[\r\n|]/g, ' ').trim() : '无'
    message.value = `[预约自取] 时间:${pickupDate.value} ${selectedTime} | 称呼:${pickupName.value.trim()}${pickupGender.value} | 电话:${pickupPhone.value} | 备注:${cleanRemark}`
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
  background-color: #FAF6EE;
  padding: 32rpx 32rpx 180rpx 32rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  box-sizing: border-box;
  color: #4A3525;
}

/* 切换卡片 */
.type-selector-card {
  display: flex;
  background-color: #F5EEDF;
  border-radius: 40rpx;
  padding: 8rpx;
  border: 1rpx solid #E6D5B8;
}

.type-tab {
  flex: 1;
  text-align: center;
  height: 70rpx;
  line-height: 70rpx;
  font-size: 28rpx;
  font-weight: bold;
  color: #A18276;
  border-radius: 35rpx;
  transition: all 0.2s ease;
}

.type-tab.active {
  background-color: #6F4E37;
  color: #FFFFFF;
  box-shadow: 0 4rpx 12rpx rgba(111, 78, 55, 0.15);
}

/* 到店自取预约卡片 */
.pickup-card {
  background-color: #FFFFFF;
  padding: 32rpx;
  border-radius: 24rpx;
  border: 1rpx solid rgba(230, 213, 184, 0.3);
  box-shadow: 0 4rpx 16rpx rgba(111, 78, 55, 0.03);
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.card-title {
  font-size: 28rpx;
  font-weight: 800;
  color: #6F4E37;
  border-bottom: 2rpx solid #E6D5B8;
  padding-bottom: 12rpx;
}

.form-row {
  display: flex;
  align-items: center;
  border-bottom: 1rpx solid rgba(230, 213, 184, 0.25);
  padding-bottom: 16rpx;
}

.form-row.align-start {
  align-items: flex-start;
  border-bottom: none;
  padding-bottom: 0;
}

.form-label {
  width: 140rpx;
  font-size: 26rpx;
  font-weight: 700;
  color: #4A3525;
}

.name-gender-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.form-input {
  flex: 1;
  height: 50rpx;
  font-size: 26rpx;
  color: #4A3525;
}

.name-input {
  border-right: 1rpx solid rgba(230, 213, 184, 0.3);
  padding-right: 12rpx;
}

.gender-selector {
  display: flex;
  background-color: #F5EEDF;
  border-radius: 24rpx;
  padding: 4rpx;
}

.gender-btn {
  padding: 4rpx 20rpx;
  font-size: 22rpx;
  color: #A18276;
  border-radius: 20rpx;
  font-weight: bold;
}

.gender-btn.active {
  background-color: #E07A5F;
  color: #FFFFFF;
}

.time-picker-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.picker-trigger {
  font-size: 26rpx;
  font-weight: bold;
  color: #6F4E37;
  background-color: #F5EEDF;
  padding: 6rpx 20rpx;
  border-radius: 8rpx;
  border: 1rpx solid #E6D5B8;
}

.picker-trigger.highlight {
  color: #FFFFFF;
  background-color: #E07A5F;
  border-color: #E07A5F;
}

.picker-separator {
  font-size: 24rpx;
  color: #A18276;
}

.form-textarea {
  flex: 1;
  height: 120rpx;
  font-size: 26rpx;
  color: #4A3525;
  background-color: #FAF6EE;
  border-radius: 12rpx;
  padding: 16rpx;
  border: 1rpx solid rgba(230, 213, 184, 0.4);
}

/* 外卖地址卡片 */
.address-card {
  display: flex;
  align-items: center;
  padding: 32rpx;
  background-color: #FFFFFF;
  border-radius: 24rpx;
  border: 1rpx solid rgba(230, 213, 184, 0.3);
  box-shadow: 0 4rpx 16rpx rgba(111, 78, 55, 0.03);
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
  font-weight: bold;
  color: #4A3525;
}

.user-phone {
  font-size: 26rpx;
  color: #A18276;
}

.tag-default {
  padding: 2rpx 10rpx;
  background-color: #6F4E37;
  color: #ffffff;
  font-size: 18rpx;
  font-weight: bold;
  border-radius: 4rpx;
}

.address-text {
  font-size: 24rpx;
  color: #A18276;
  line-height: 1.4;
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
  color: #A18276;
  font-weight: bold;
}

.arrow-right {
  font-size: 28rpx;
  color: #E6D5B8;
  font-weight: bold;
}

/* 购买商品列表 */
.goods-card {
  background-color: #FFFFFF;
  padding: 32rpx;
  border-radius: 24rpx;
  border: 1rpx solid rgba(230, 213, 184, 0.3);
}

.goods-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  margin-top: 20rpx;
}

.goods-item {
  display: flex;
  gap: 20rpx;
}

.goods-img {
  width: 120rpx;
  height: 120rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
  border: 1rpx solid rgba(230, 213, 184, 0.2);
}

.goods-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.goods-name {
  font-size: 26rpx;
  font-weight: bold;
  color: #4A3525;
}

.goods-spec {
  font-size: 22rpx;
  color: #A18276;
}

.price-num-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.goods-price {
  font-size: 26rpx;
  font-weight: 800;
  color: #E07A5F;
}

.goods-num {
  font-size: 24rpx;
  color: #A18276;
}

/* 优惠券选择 */
.options-card {
  background-color: #FFFFFF;
  padding: 12rpx 32rpx;
  border-radius: 24rpx;
  border: 1rpx solid rgba(230, 213, 184, 0.3);
  display: flex;
  flex-direction: column;
}

.option-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 1rpx solid rgba(230, 213, 184, 0.2);
}

.option-row:last-child {
  border-bottom: none;
}

.option-row.no-arrow {
  justify-content: flex-start;
  gap: 20rpx;
}

.option-label {
  font-size: 26rpx;
  font-weight: 700;
  color: #4A3525;
}

.option-right {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.option-val {
  font-size: 24rpx;
  color: #6F4E37;
}

.coupon-val {
  color: #E07A5F;
  font-weight: bold;
}

.note-input {
  flex: 1;
  font-size: 26rpx;
  color: #4A3525;
  height: 50rpx;
}

/* 费用明细 */
.bill-card {
  background-color: #FFFFFF;
  padding: 32rpx;
  border-radius: 24rpx;
  border: 1rpx solid rgba(230, 213, 184, 0.3);
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.bill-row {
  display: flex;
  justify-content: space-between;
  font-size: 24rpx;
  color: #A18276;
}

.bill-val {
  font-weight: bold;
  color: #4A3525;
}

.text-red {
  color: #E07A5F;
}

.divider {
  height: 2rpx;
  background-color: rgba(230, 213, 184, 0.5);
  margin: 10rpx 0;
}

.total-row {
  font-size: 28rpx;
  color: #4A3525;
}

.total-label {
  font-weight: 800;
}

.total-val {
  font-size: 34rpx;
  font-weight: 800;
  color: #E07A5F;
}

/* 底部操作栏 */
.checkout-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 140rpx;
  background-color: #FFFFFF;
  border-top: 1rpx solid #E6D5B8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40rpx 20rpx 40rpx;
  z-index: 100;
}

.footer-left {
  display: flex;
  align-items: baseline;
}

.pay-label {
  font-size: 24rpx;
  color: #A18276;
}

.pay-symbol {
  font-size: 24rpx;
  font-weight: bold;
  color: #E07A5F;
}

.pay-price {
  font-size: 40rpx;
  font-weight: 800;
  color: #E07A5F;
}

.pay-btn {
  height: 80rpx;
  line-height: 80rpx;
  background-color: #E07A5F;
  color: #FFFFFF;
  font-size: 28rpx;
  font-weight: bold;
  border-radius: 40rpx;
  padding: 0 60rpx;
  border: none;
  box-shadow: 0 6rpx 16rpx rgba(224, 122, 95, 0.2);
}

.pay-btn:active {
  transform: scale(0.95);
}

.loading-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  gap: 20rpx;
}

.loading-icon {
  font-size: 64rpx;
}

.loading-text {
  font-size: 26rpx;
  color: #A18276;
}
</style>
