<template>
  <view class="container">
    <view class="settings-list">
      <!-- Avatar row (using button styled as list row for native WeChat chooseAvatar) -->
      <button class="settings-item avatar-row" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
        <text class="label">头像</text>
        <view class="value-wrap">
          <image :src="userInfo.avatarUrl || 'https://yanxuan.nosdn.127.net/80841d741d7fa3073e0ae27bf487339f.jpg?imageView&quality=90&thumbnail=64x64'" class="avatar-img" mode="aspectFill"></image>
          <text class="arrow">›</text>
        </view>
      </button>

      <!-- Nickname row -->
      <view class="settings-item" @tap="editNickname">
        <text class="label">昵称</text>
        <view class="value-wrap">
          <text class="value">{{ userInfo.nickname || '微信用户' }}</text>
          <text class="action-text">修改</text>
          <text class="arrow">›</text>
        </view>
      </view>

      <!-- Phone Number row (immutable) -->
      <view class="settings-item disabled-item" @tap="onPhoneTap">
        <text class="label">手机号</text>
        <view class="value-wrap">
          <text class="value">{{ userInfo.mobile || '未绑定' }}</text>
        </view>
      </view>
    </view>

    <!-- Documents Section -->
    <view class="settings-list docs-list">
      <view class="settings-item" @tap="openDoc('service')">
        <text class="label">用户服务协议</text>
        <view class="value-wrap">
          <text class="arrow">›</text>
        </view>
      </view>

      <view class="settings-item" @tap="openDoc('privacy')">
        <text class="label">隐私政策政策</text>
        <view class="value-wrap">
          <text class="arrow">›</text>
        </view>
      </view>
    </view>

    <!-- Logout Button -->
    <button class="logout-btn" @tap="onLogout">退出登录</button>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request, { fixImageUrl } from '../../../utils/request'
import api from '../../../config/api'

const userInfo = ref({
  nickname: '',
  avatarUrl: '',
  mobile: ''
})

const loadUserInfo = async () => {
  try {
    const cached = uni.getStorageSync('userInfo')
    if (cached) {
      const parsed = JSON.parse(cached)
      userInfo.value = {
        nickname: parsed.nickName || parsed.nickname || '',
        avatarUrl: parsed.avatarUrl || parsed.avatar || '',
        mobile: parsed.mobile || ''
      }
    }
  } catch (err) {
    console.error('Failed to load user info', err)
  }

  // Fetch fresh data from backend
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

      // Sync local storage cache
      const cached = JSON.parse(uni.getStorageSync('userInfo') || '{}')
      cached.nickName = res.nickName
      cached.nickname = res.nickName
      cached.avatarUrl = res.avatar
      cached.avatar = res.avatar
      cached.mobile = res.mobile
      uni.setStorageSync('userInfo', JSON.stringify(cached))
    }
  } catch (err) {
    console.error('Failed to fetch user info from server', err)
  }
}

onMounted(() => {
  loadUserInfo()
})

const onChooseAvatar = (e) => {
  const avatarUrl = e.detail.avatarUrl
  if (!avatarUrl) return

  uni.showLoading({ title: '正在上传头像...' })
  
  // Upload temporary avatar file to backend storage
  uni.uploadFile({
    url: api.StorageUpload,
    filePath: avatarUrl,
    name: 'file',
    header: {
      'X-Litemall-Token': uni.getStorageSync('token')
    },
    success: async (uploadRes) => {
      try {
        const resData = JSON.parse(uploadRes.data)
        if (resData.errno === 0 && resData.data.url) {
          // Process via fixImageUrl to ensure correct domain and Https is loaded
          const uploadedUrl = fixImageUrl(resData.data.url)
          
          // Save to user profile via AuthProfile api
          const updateRes = await request({
            url: api.AuthProfile,
            method: 'POST',
            data: {
              avatar: uploadedUrl
            }
          })

          if (updateRes) {
            userInfo.value.avatarUrl = uploadedUrl
            
            // Sync local storage cache
            const cached = JSON.parse(uni.getStorageSync('userInfo') || '{}')
            cached.avatarUrl = uploadedUrl
            cached.avatar = uploadedUrl
            uni.setStorageSync('userInfo', JSON.stringify(cached))
            
            uni.showToast({ title: '头像上传成功', icon: 'success' })
          }
        } else {
          uni.showToast({ title: '上传失败，请重试', icon: 'none' })
        }
      } catch (err) {
        console.error(err)
        uni.showToast({ title: '服务器响应解析错误', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    },
    fail: (err) => {
      console.error(err)
      uni.hideLoading()
      uni.showToast({ title: '头像上传失败', icon: 'none' })
    }
  })
}

const editNickname = () => {
  uni.showModal({
    title: '修改昵称',
    placeholderText: '请输入新昵称',
    editable: true,
    content: userInfo.value.nickname,
    success: async (res) => {
      if (res.confirm && res.content.trim()) {
        const newName = res.content.trim()
        try {
          uni.showLoading({ title: '保存中...' })
          const updateRes = await request({
            url: api.AuthProfile,
            method: 'POST',
            data: {
              nickname: newName
            }
          })
          if (updateRes) {
            userInfo.value.nickname = newName
            
            const cached = JSON.parse(uni.getStorageSync('userInfo') || '{}')
            cached.nickname = newName
            cached.nickName = newName
            uni.setStorageSync('userInfo', JSON.stringify(cached))
            
            uni.showToast({ title: '修改成功', icon: 'success' })
          }
        } catch (err) {
          console.error(err)
        } finally {
          uni.hideLoading()
        }
      }
    }
  })
}

const onPhoneTap = () => {
  uni.showToast({
    title: '手机号不可修改',
    icon: 'none'
  })
}

const openDoc = (docType) => {
  uni.navigateTo({
    url: `/pages/auth/doc?type=${docType}`
  })
}

const onLogout = () => {
  uni.showModal({
    title: '退出登录',
    content: '您确定要退出当前登录账号吗？',
    confirmColor: '#E07A5F',
    success: async (res) => {
      if (res.confirm) {
        try {
          await request({
            url: api.AuthLogout,
            method: 'POST'
          })
        } catch (err) {
          console.error('登出失败:', err)
        }
        
        // Clear caches and redirect
        uni.removeStorageSync('token')
        uni.removeStorageSync('userInfo')
        
        uni.showToast({
          title: '已退出登录',
          icon: 'success'
        })
        
        setTimeout(() => {
          uni.switchTab({
            url: '/pages/ucenter/index'
          })
        }, 1000)
      }
    }
  })
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background-color: #FAF6EE;
  padding: 24rpx;
  box-sizing: border-box;
}

.settings-list {
  background-color: #ffffff;
  border-radius: 20rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4px 16px rgba(111, 78, 55, 0.03);
  overflow: hidden;
}

.settings-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx;
  border-bottom: 1rpx solid rgba(161, 130, 118, 0.1);
  background-color: #ffffff;
  line-height: normal;
  text-align: left;
  border-radius: 0;
}

.settings-item::after {
  border: none;
}

.settings-item:last-child {
  border-bottom: none;
}

.avatar-row {
  height: auto;
  margin: 0;
}

.label {
  font-size: 28rpx;
  color: #4A3525;
  font-weight: 600;
}

.value-wrap {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.avatar-img {
  width: 90rpx;
  height: 90rpx;
  border-radius: 50%;
  border: 2rpx solid rgba(111, 78, 55, 0.15);
}

.value {
  font-size: 28rpx;
  color: #A18276;
}

.action-text {
  font-size: 24rpx;
  color: #E07A5F;
  font-weight: 700;
}

.arrow {
  font-size: 32rpx;
  color: #A18276;
  font-weight: 600;
}

.disabled-item {
  opacity: 0.85;
}

.logout-btn {
  width: 100%;
  height: 96rpx;
  border-radius: 48rpx;
  background-color: #ffffff;
  color: #E07A5F;
  font-size: 30rpx;
  font-weight: 700;
  border: 2rpx solid #E07A5F;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 60rpx;
  box-shadow: 0 4px 16px rgba(224, 122, 95, 0.08);
}

.logout-btn::after {
  border: none;
}

.logout-btn:active {
  background-color: rgba(224, 122, 95, 0.05);
}
</style>
