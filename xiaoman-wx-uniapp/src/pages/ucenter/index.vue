<template>
  <view class="container">
    <!-- Header profile block -->
    <view class="profile-header">
      <view class="header-bg"></view>
      
      <!-- Profile Card Container -->
      <view class="profile-card dark-glass-panel">
        <view class="avatar-wrapper" @tap="onProfileTap">
          <!-- Show normal avatar if logged in, otherwise show silhouette placeholder -->
          <image v-if="hasLogin" :src="userInfo.avatarUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=200'" class="avatar-image" mode="aspectFill" />
          <view v-else class="default-avatar-placeholder">
            <text class="default-avatar-icon">👤</text>
          </view>
          <view class="online-tag" v-if="hasLogin"></view>
        </view>
        
        <view class="user-meta" @tap="onProfileTap">
          <block v-if="hasLogin">
            <text class="username">{{ userInfo.nickname }}</text>
            <!-- <text class="user-id" v-if="userInfo.mobile">{{ userInfo.mobile }}</text> -->
            <!-- <text class="user-id" v-else>小满食光尊享会员</text> -->
          </block>
          <block v-else>
            <view class="login-prompt-row">
              <text class="login-prompt-text">登录/注册</text>
              <text class="login-prompt-arrow">›</text>
            </view>
          </block>
        </view>
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
            <view class="order-icon-wrapper">
              <image class="order-icon-img" :src="item.icon" mode="aspectFit" />
              <text class="order-badge" v-if="order[item.badgeKey] > 0">{{ order[item.badgeKey] }}</text>
            </view>
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
        
        <!-- Bind Phone Number (Only show when logged in and phone not bound) -->
        <button 
          v-if="hasLogin && !userInfo.mobile" 
          class="service-row bind-phone-btn" 
          open-type="getPhoneNumber" 
          @getphonenumber="onGetPhoneNumber"
        >
          <view class="service-left">
            <text class="service-label bind-phone-text">绑定手机号</text>
          </view>
          <text class="service-arrow">›</text>
        </button>

        <!-- Log Out Row -->
        <view class="service-row logout-row" v-if="hasLogin" @tap="onLogout">
          <view class="service-left">
            <text class="service-label logout-text">退出登录</text>
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

const order = ref({
  unpaid: 0,
  unship: 0,
  unrecv: 0,
  uncomment: 0
})

const orderStates = ref([
  { label: '待付款', icon: '/static/images/pendpay.png', status: 1, badgeKey: 'unpaid' },
  { label: '待发货', icon: '/static/images/send.png', status: 2, badgeKey: 'unship' },
  { label: '待收货', icon: '/static/images/receive.png', status: 3, badgeKey: 'unrecv' },
  { label: '待评价', icon: '/static/images/comment.png', status: 4, badgeKey: 'uncomment' }
])

const services = ref([
  { label: '我的收藏', icon: '❤️', route: '/pages/ucenter/collect/collect' },
  { label: '我的足迹', icon: '👣', route: '/pages/ucenter/footprint/footprint' },
  { label: '收货地址', icon: '📍', route: '/pages/ucenter/address/address' },
  { label: '帮助与反馈', icon: '💬', route: '/pages/ucenter/feedback/feedback' }
])

// Auto reload login state upon view enter
onShow(async () => {
  const token = uni.getStorageSync('token')
  const storedUser = uni.getStorageSync('userInfo')
  if (token && storedUser) {
    hasLogin.value = true
    userInfo.value = JSON.parse(storedUser)
    
    // Async refresh profile from server
    try {
      const res = await request({
        url: api.AuthInfo,
        method: 'GET'
      })
      if (res) {
        userInfo.value = {
          nickname: res.nickName || '',
          avatarUrl: res.avatar || '',
          mobile: res.mobile || ''
        }
        
        const cached = JSON.parse(uni.getStorageSync('userInfo') || '{}')
        cached.nickName = res.nickName
        cached.nickname = res.nickName
        cached.avatarUrl = res.avatar
        cached.avatar = res.avatar
        cached.mobile = res.mobile
        uni.setStorageSync('userInfo', JSON.stringify(cached))
      }
    } catch (err) {
      console.error('Failed to sync user info onShow:', err)
    }

    // Async refresh order counts
    try {
      const res = await request({
        url: api.UserIndex,
        method: 'GET'
      })
      if (res && res.order) {
        order.value = res.order
      }
    } catch (err) {
      console.error('Failed to sync order counts onShow:', err)
    }
  } else {
    hasLogin.value = false
    userInfo.value = null
    order.value = {
      unpaid: 0,
      unship: 0,
      unrecv: 0,
      uncomment: 0
    }
  }
})

const onProfileTap = () => {
  if (hasLogin.value) {
    uni.navigateTo({
      url: '/pages/ucenter/settings/settings'
    })
  } else {
    uni.navigateTo({
      url: '/pages/auth/login'
    })
  }
}

const onGetPhoneNumber = async (e) => {
  if (e.detail.errMsg !== 'getPhoneNumber:ok') {
    uni.showToast({
      title: '您取消了手机号授权',
      icon: 'none'
    })
    return
  }

  const { encryptedData, iv } = e.detail
  uni.showLoading({
    title: '正在绑定手机号...'
  })

  try {
    await request({
      url: api.AuthBindPhone,
      method: 'POST',
      data: {
        encryptedData: encryptedData,
        iv: iv
      }
    })

    uni.showToast({
      title: '绑定成功！',
      icon: 'success'
    })

    // Fetch the updated user profile to store
    const fullUserInfo = await request({
      url: api.AuthInfo,
      method: 'GET'
    })
    userInfo.value = fullUserInfo
    uni.setStorageSync('userInfo', JSON.stringify(fullUserInfo))
  } catch (err) {
    console.error('手机号绑定失败:', err)
  } finally {
    uni.hideLoading()
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
        order.value = {
          unpaid: 0,
          unship: 0,
          unrecv: 0,
          uncomment: 0
        }
        
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

.order-icon-wrapper {
  position: relative;
  width: 56rpx;
  height: 56rpx;
  margin-bottom: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.order-icon-img {
  width: 100%;
  height: 100%;
}

.order-badge {
  position: absolute;
  top: -12rpx;
  right: -12rpx;
  min-width: 28rpx;
  height: 28rpx;
  line-height: 28rpx;
  background-color: #E07A5F;
  color: #ffffff;
  border-radius: 14rpx;
  font-size: 18rpx;
  text-align: center;
  padding: 0 6rpx;
  box-sizing: border-box;
  font-weight: 700;
  border: 2rpx solid #ffffff;
  z-index: 2;
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

/* Bind Phone Number button row reset styling */
.bind-phone-btn {
  background: none;
  border: none;
  padding: 0;
  margin: 0;
  line-height: normal;
  text-align: left;
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.bind-phone-btn::after {
  border: none;
}

.bind-phone-text {
  color: #E07A5F;
  font-weight: 700;
}

/* Unlogged-in Default Avatar & Prompt Styling */
.default-avatar-placeholder {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1rpx solid rgba(255, 255, 255, 0.1);
}

.default-avatar-icon {
  font-size: 52rpx;
  color: rgba(255, 255, 255, 0.95);
  display: inline-block;
  line-height: 1;
}

.login-prompt-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.login-prompt-text {
  font-size: 34rpx;
  font-weight: 700;
  color: #ffffff;
}

.login-prompt-arrow {
  font-size: 36rpx;
  color: rgba(255, 255, 255, 0.65);
  font-weight: 600;
  line-height: 1;
  position: relative;
  top: -2rpx;
}
</style>
