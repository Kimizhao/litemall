<template>
  <view class="container">
    <scroll-view scroll-y class="address-scroll">
      <view class="address-list" v-if="addressList.length > 0">
        <view 
          class="address-card glass-panel" 
          v-for="item in addressList" 
          :key="item.id"
          @tap="onAddressTap(item)"
        >
          <view class="card-left">
            <view class="user-row">
              <text class="name">{{ item.name }}</text>
              <text class="phone">{{ item.tel }}</text>
              <text class="tag-default" v-if="item.isDefault">默认</text>
            </view>
            <text class="address-text">{{ item.province }}{{ item.city }}{{ item.county }}{{ item.addressDetail }}</text>
          </view>
          <view class="card-actions" @tap.stop>
            <text class="action-btn edit-btn" @tap="onEditAddress(item.id)">✏️</text>
            <text class="action-btn delete-btn" @tap="onDeleteAddress(item.id)">🗑️</text>
          </view>
        </view>
      </view>

      <view class="empty-address" v-else-if="!loading">
        <text class="empty-icon">📍</text>
        <text class="empty-title">暂无收货地址</text>
        <text class="empty-desc">配置收货地址，让心仪宝贝安全送达</text>
      </view>
    </scroll-view>

    <!-- Sticky Bottom Add Button -->
    <view class="footer-wrap">
      <button class="add-btn" @tap="onAddAddress">+ 添加新地址</button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import request from '../../../utils/request'
import api from '../../../config/api'

const addressList = ref([])
const isSelectMode = ref(false)
const loading = ref(false)

onLoad((options) => {
  if (options.isSelect) {
    isSelectMode.value = options.isSelect === 'true'
  }
})

onShow(() => {
  getAddressList()
})

const getAddressList = async () => {
  loading.value = true
  try {
    const data = await request({
      url: api.AddressList,
      method: 'GET'
    })
    addressList.value = data || []
  } catch (err) {
    console.error('Failed to get address list:', err)
  } finally {
    loading.value = false
  }
}

const onAddressTap = (item) => {
  if (isSelectMode.value) {
    // Cache selection for checkout
    uni.setStorageSync('selectedAddressId', item.id)
    uni.navigateBack()
  } else {
    onEditAddress(item.id)
  }
}

const onAddAddress = () => {
  uni.navigateTo({
    url: '/pages/ucenter/addressAdd/addressAdd'
  })
}

const onEditAddress = (id) => {
  uni.navigateTo({
    url: `/pages/ucenter/addressAdd/addressAdd?id=${id}`
  })
}

const onDeleteAddress = (id) => {
  uni.showModal({
    title: '删除地址',
    content: '确定要删除此收货地址吗？',
    confirmText: '删除',
    cancelText: '取消',
    confirmColor: '#ef4444',
    success: async (res) => {
      if (res.confirm) {
        try {
          await request({
            url: api.AddressDelete,
            method: 'POST',
            data: { id }
          })
          uni.showToast({
            title: '删除成功',
            icon: 'success'
          })
          getAddressList()
        } catch (err) {
          console.error('Failed to delete address:', err)
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

.address-scroll {
  flex: 1;
}

.address-list {
  padding: 32rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.address-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
}

.card-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.user-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.name {
  font-size: 28rpx;
  font-weight: 800;
  color: var(--text-main);
}

.phone {
  font-size: 24rpx;
  color: var(--text-muted);
  font-weight: 600;
}

.tag-default {
  padding: 2rpx 10rpx;
  background: var(--primary-gradient);
  color: #ffffff;
  font-size: 16rpx;
  font-weight: 700;
  border-radius: 4rpx;
}

.address-text {
  font-size: 24rpx;
  color: var(--text-muted);
  line-height: 1.4;
  font-weight: 500;
}

.card-actions {
  display: flex;
  gap: 24rpx;
  align-items: center;
  padding-left: 24rpx;
}

.action-btn {
  font-size: 32rpx;
  padding: 10rpx;
}

/* Empty Address State */
.empty-address {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 60vh;
  padding: 0 64rpx;
}

.empty-icon {
  font-size: 100rpx;
  margin-bottom: 24rpx;
}

.empty-title {
  font-size: 32rpx;
  font-weight: 800;
  color: var(--text-main);
  margin-bottom: 8rpx;
}

.empty-desc {
  font-size: 24rpx;
  color: var(--text-muted);
  text-align: center;
}

/* Sticky Footer Button */
.footer-wrap {
  padding: 24rpx 32rpx 48rpx 32rpx;
  background-color: var(--bg-primary);
}

.add-btn {
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
}

.add-btn:active {
  opacity: 0.85;
}
</style>
