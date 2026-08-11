<template>
  <view class="container">
    <view class="form-card glass-panel">
      <!-- Consignee name -->
      <view class="input-row">
        <text class="label">收货人</text>
        <input type="text" class="form-input" v-model="address.name" placeholder="请填写收货人姓名" />
      </view>

      <!-- Phone number -->
      <view class="input-row">
        <text class="label">手机号码</text>
        <input type="number" class="form-input" v-model="address.tel" placeholder="请填写手机号码" maxlength="11" />
      </view>

      <!-- Province / City / County -->
      <view class="input-row">
        <text class="label">省份</text>
        <input type="text" class="form-input" v-model="address.province" placeholder="如：浙江省" />
      </view>
      <view class="input-row">
        <text class="label">城市</text>
        <input type="text" class="form-input" v-model="address.city" placeholder="如：杭州市" />
      </view>
      <view class="input-row">
        <text class="label">区县</text>
        <input type="text" class="form-input" v-model="address.county" placeholder="如：西湖区" />
      </view>

      <!-- Detailed Address -->
      <view class="input-row textarea-row">
        <text class="label">详细地址</text>
        <textarea class="form-textarea" v-model="address.addressDetail" placeholder="街道、楼牌号等详细信息" />
      </view>

      <view class="divider"></view>

      <!-- Set Default switch -->
      <view class="switch-row">
        <view class="switch-info">
          <text class="switch-label">设为默认地址</text>
          <text class="switch-desc">每次下单时将首选使用此收货地址</text>
        </view>
        <switch :checked="address.isDefault" @change="onDefaultChange" color="#E07A5F" />
      </view>

      <view class="divider"></view>

      <!-- Action Buttons -->
      <button class="submit-btn" @tap="onSave" :disabled="submitting">
        {{ submitting ? '正在保存...' : '保存地址' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '../../../utils/request'
import api from '../../../config/api'

const addressId = ref(0)
const address = ref({
  name: '',
  tel: '',
  province: '',
  city: '',
  county: '',
  addressDetail: '',
  isDefault: false
})
const submitting = ref(false)

onLoad((options) => {
  if (options.id) {
    addressId.value = parseInt(options.id)
  }
})

const getAddressDetail = async () => {
  try {
    const data = await request({
      url: `${api.AddressDetail}?id=${addressId.value}`,
      method: 'GET'
    })
    if (data) {
      address.value = data
    }
  } catch (err) {
    console.error('Failed to get address detail:', err)
  }
}

const onDefaultChange = (e) => {
  address.value.isDefault = e.detail.value
}

const onSave = async () => {
  const addr = address.value
  if (!addr.name.trim()) {
    uni.showToast({ title: '请填写收货人姓名', icon: 'none' })
    return
  }
  if (!addr.tel.trim() || addr.tel.length < 11) {
    uni.showToast({ title: '请填写正确的手机号码', icon: 'none' })
    return
  }
  if (!addr.province.trim() || !addr.city.trim() || !addr.county.trim()) {
    uni.showToast({ title: '请完整填写省市区信息', icon: 'none' })
    return
  }
  if (!addr.addressDetail.trim()) {
    uni.showToast({ title: '请填写详细街道地址', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    const res = await request({
      url: api.AddressSave,
      method: 'POST',
      data: {
        id: addressId.value || undefined,
        name: addr.name,
        tel: addr.tel,
        province: addr.province,
        city: addr.city,
        county: addr.county,
        addressDetail: addr.addressDetail,
        isDefault: addr.isDefault,
        areaCode: '100000' // mock area code standard
      }
    })
    if (res) {
      uni.setStorageSync('selectedAddressId', res)
    }
    uni.showToast({
      title: '保存地址成功',
      icon: 'success'
    })
    setTimeout(() => {
      uni.navigateBack()
    }, 1000)
  } catch (err) {
    console.error('Failed to save address:', err)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  if (addressId.value) {
    getAddressDetail()
  }
})
</script>

<style scoped>
.container {
  min-height: 100vh;
  background-color: var(--bg-primary);
  padding: 32rpx;
  box-sizing: border-box;
}

.form-card {
  border-radius: var(--radius-md);
  padding: 40rpx 32rpx;
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
}

.input-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100rpx;
  border-bottom: 1px solid var(--border-light);
}

.label {
  font-size: 26rpx;
  font-weight: 700;
  color: var(--text-main);
  width: 140rpx;
}

.form-input {
  flex: 1;
  font-size: 26rpx;
  color: var(--text-main);
  font-weight: 500;
  text-align: right;
}

.textarea-row {
  height: 180rpx;
  align-items: flex-start;
  padding: 24rpx 0;
}

.form-textarea {
  flex: 1;
  height: 100%;
  font-size: 26rpx;
  color: var(--text-main);
  font-weight: 500;
  text-align: right;
}

.divider {
  height: 1px;
  background-color: var(--border-light);
  margin: 32rpx 0;
}

/* Switch styling */
.switch-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.switch-info {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.switch-label {
  font-size: 26rpx;
  font-weight: 700;
  color: var(--text-main);
}

.switch-desc {
  font-size: 20rpx;
  color: var(--text-muted);
  font-weight: 500;
}

/* Save button */
.submit-btn {
  width: 100%;
  height: 88rpx;
  border-radius: 44rpx;
  background: var(--primary-gradient);
  color: #ffffff;
  font-size: 26rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.2);
  margin-top: 32rpx;
}

.submit-btn:active {
  opacity: 0.85;
}
</style>
