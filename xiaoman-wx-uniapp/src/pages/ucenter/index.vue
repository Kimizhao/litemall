<template>
  <view class="container">
    <!-- Header profile block -->
    <view class="profile-header">
      <view class="header-bg"></view>
      
      <!-- Profile Card Container -->
      <view class="profile-card dark-glass-panel">
        <view class="avatar-wrapper" @tap="onProfileTap">
          <image :src="hasLogin ? (userInfo.avatarUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=200') : 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?q=80&w=200'" class="avatar-image" mode="aspectFill" />
          <view class="online-tag" v-if="hasLogin"></view>
        </view>
        
        <view class="user-meta" @tap="onProfileTap">
          <text class="username">{{ hasLogin ? userInfo.nickname : '点击登录账号' }}</text>
          <text class="user-id" v-if="hasLogin">小满食光尊享会员</text>
          <text class="user-id" v-else>享受更便捷的本地购物体验</text>
        </view>
        
        <view class="action-arrow" v-if="!hasLogin">➔</view>
      </view>
    </view>

    <!-- Content Area -->
    <view class="content-body">
      <!-- Order Management Container -->
      <view class="menu-box glass-panel">
        <view class="box-header">
          <text class="box-title">我的订单</text>
          <text class="box-subtitle" @tap="onViewAllOrders">查看全部</text>
        </view>
        
        <view class="order-grid">
          <view class="order-item" v-for="(item, index) in orderStates" :key="index" @tap="onOrderStateTap(item)">
            <text class="order-label">{{ item.label }}</text>
          </view>
        </view>
      </view>

      <!-- Services List Container -->
      <view class="menu-box glass-panel services-list">
        <view class="service-row" v-for="(item, index) in services" :key="index" @tap="onServiceTap(item)">
          <view class="service-left">
            <text class="service-label">{{ item.label }}</text>
          </view>
          <text class="service-arrow">›</text>
        </view>
        
        <!-- Log Out Row -->
        <view class="service-row logout-row" v-if="hasLogin" @tap="onLogout">
          <view class="service-left">
            <text class="service-label logout-text">退出账户登录</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import request from '../../utils/request'
import api from '../../config/api'

// Reactive state variables
const hasLogin = ref(false)
const userInfo = ref(null)

const orderStates = ref([
  { label: '待付款', icon: '💳', status: 1 },
  { label: '待发货', icon: '📦', status: 2 },
  { label: '待收货', icon: '🚚', status: 3 },
  { label: '待评价', icon: '⭐', status: 4 }
])

const services = ref([
  { label: '我的收藏', icon: '❤️', route: '/pages/ucenter/collect/collect' },
  { label: '我的足迹', icon: '👣', route: '/pages/ucenter/footprint/footprint' },
  { label: '收货地址', icon: '📍', route: '/pages/ucenter/address/address' },
  { label: '帮助与反馈', icon: '💬', route: '/pages/ucenter/feedback/feedback' }
])

// Auto reload login state upon view enter
onShow(() => {
  const token = uni.getStorageSync('token')
  const storedUser = uni.getStorageSync('userInfo')
  if (token && storedUser) {
    hasLogin.value = true
    userInfo.value = JSON.parse(storedUser)
  } else {
    hasLogin.value = false
    userInfo.value = null
  }
})

const onProfileTap = () => {
  if (!hasLogin.value) {
    uni.navigateTo({
      url: '/pages/auth/login'
    })
  }
}

const onLogout = () => {
  uni.showModal({
    title: '退出登录',
    content: '您确定要退出当前登录的账号吗？',
    confirmText: '确定',
    cancelText: '取消',
    confirmColor: '#6F4E37',
    success: async (res) => {
      if (res.confirm) {
        try {
          await request({
            url: api.AuthLogout,
            method: 'POST'
          })
        } catch (err) {
          console.error('Logout request failed:', err)
        }
        
        // Remove locally cached keys
        uni.removeStorageSync('token')
        uni.removeStorageSync('userInfo')
        hasLogin.value = false
        userInfo.value = null
        
        uni.showToast({
          title: '已安全退出',
          icon: 'success'
        })
      }
    }
  })
}

const onViewAllOrders = () => {
  if (!hasLogin.value) {
    uni.showToast({
      title: '请先登录账号',
      icon: 'none'
    })
    setTimeout(() => {
      uni.navigateTo({
        url: '/pages/auth/login'
      })
    }, 1000)
    return
  }
  uni.navigateTo({
    url: '/pages/ucenter/order/order?tab=0'
  })
}

const onOrderStateTap = (item) => {
  if (!hasLogin.value) {
    uni.showToast({
      title: '请先登录账号',
      icon: 'none'
    })
    setTimeout(() => {
      uni.navigateTo({
        url: '/pages/auth/login'
      })
    }, 1000)
    return
  }
  uni.navigateTo({
    url: `/pages/ucenter/order/order?tab=${item.status}`
  })
}

const onServiceTap = (item) => {
  if (!hasLogin.value) {
    uni.showToast({
      title: '请先登录账号',
      icon: 'none'
    })
    setTimeout(() => {
      uni.navigateTo({
        url: '/pages/auth/login'
      })
    }, 1000)
    return
  }
  if (item.route) {
    uni.navigateTo({
      url: item.route
    })
  }
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background-color: var(--bg-primary);
  display: flex;
  flex-direction: column;
}

/* Header profile styling */
.profile-header {
  position: relative;
  height: 480rpx;
  width: 100%;
  display: flex;
  align-items: flex-end;
  padding: 0 40rpx 48rpx 40rpx;
  box-sizing: border-box;
}

.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 80rpx;
  background: var(--dark-gradient);
  border-bottom-left-radius: 48rpx;
  border-bottom-right-radius: 48rpx;
}

.profile-card {
  width: 100%;
  height: 190rpx;
  border-radius: var(--radius-md);
  padding: 32rpx;
  display: flex;
  align-items: center;
  z-index: 10;
  box-sizing: border-box;
}

.avatar-wrapper {
  position: relative;
  width: 110rpx;
  height: 110rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.3);
  margin-right: 28rpx;
}

.avatar-image {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background-color: #E6D5B8;
}

.online-tag {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 24rpx;
  height: 24rpx;
  border-radius: 50%;
  background-color: #10b981;
  border: 4rpx solid #4A3525;
}

.user-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.username {
  font-size: 34rpx;
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 8rpx;
  letter-spacing: -0.5rpx;
}

.user-id {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.6);
  font-weight: 500;
}

.action-arrow {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.5);
  font-weight: bold;
}

/* Content Body */
.content-body {
  padding: 0 40rpx;
  display: flex;
  flex-direction: column;
  gap: 32rpx;
}

.menu-box {
  border-radius: var(--radius-md);
  padding: 32rpx;
  display: flex;
  flex-direction: column;
}

.box-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28rpx;
  border-bottom: 1rpx solid rgba(226, 232, 240, 0.4);
  padding-bottom: 16rpx;
}

.box-title {
  font-size: 28rpx;
  font-weight: 700;
  color: var(--text-main);
}

.box-subtitle {
  font-size: 22rpx;
  color: var(--text-muted);
  font-weight: 600;
}

/* Orders grid shortcut */
.order-grid {
  display: flex;
  justify-content: space-between;
}

.order-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 25%;
  transition: transform 0.2s ease;
}

.order-item:active {
  transform: scale(0.9);
}

.order-icon {
  font-size: 48rpx;
  margin-bottom: 12rpx;
}

.order-label {
  font-size: 26rpx;
  color: #6F4E37;
  font-weight: 700;
  text-align: center;
  padding: 10rpx 0;
}

/* Services settings rows */
.services-list {
  padding: 16rpx 32rpx;
}

.service-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 0;
  border-bottom: 1rpx solid rgba(226, 232, 240, 0.4);
}

.service-row:last-child {
  border-bottom: none;
}

.service-row:active {
  opacity: 0.7;
}

.service-left {
  display: flex;
  align-items: center;
}

.service-icon {
  font-size: 32rpx;
  margin-right: 20rpx;
}

.service-label {
  font-size: 26rpx;
  color: var(--text-main);
  font-weight: 600;
}

.service-arrow {
  font-size: 28rpx;
  color: var(--text-muted);
  font-weight: bold;
}

/* Log out action button */
.logout-row {
  border-top: 1rpx solid rgba(226, 232, 240, 0.4);
  margin-top: 16rpx;
  padding-top: 28rpx;
  padding-bottom: 12rpx;
  display: flex;
  justify-content: center;
  align-items: center;
}

.logout-icon {
  font-size: 32rpx;
}

.logout-text {
  color: #ef4444;
  font-weight: 700;
}
</style>
