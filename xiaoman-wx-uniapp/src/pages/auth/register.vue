<template>
  <view class="container">
    <!-- Back Navigation Icon -->
    <view class="nav-back" @tap="onBackTap">
      <text class="back-icon">←</text>
    </view>

    <view class="register-card">
      <!-- Title -->
      <view class="header">
        <text class="title">加入 小满食光</text>
        <!-- <text class="subtitle">开启您的手作美食之旅</text> -->
      </view>

      <!-- Username -->
      <view class="input-wrap">
        <input type="text" class="form-input" v-model="username" placeholder="请输入用户名" placeholder-style="color: rgba(255, 255, 255, 0.45);" />
      </view>

      <!-- Mobile phone -->
      <view class="input-wrap">
        <input type="number" class="form-input" v-model="mobile" placeholder="请输入手机号码" maxlength="11" placeholder-style="color: rgba(255, 255, 255, 0.45);" />
      </view>

      <!-- SMS Captcha is bypassed and hidden for test environment -->

      <!-- Password -->
      <view class="input-wrap">
        <input type="password" class="form-input" v-model="password" placeholder="请输入密码" placeholder-style="color: rgba(255, 255, 255, 0.45);" />
      </view>

      <view class="input-wrap">
        <input type="password" class="form-input" v-model="confirmPassword" placeholder="请再次确认密码" placeholder-style="color: rgba(255, 255, 255, 0.45);" />
      </view>

      <!-- Register Submit -->
      <button class="submit-btn" @tap="onRegister" :disabled="submitting">
        {{ submitting ? '正在注册...' : '立即注册' }}
      </button>

      <text class="login-link" @tap="onGoLogin">已有账号？立即登录</text>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import request from '../../utils/request'
import api from '../../config/api'

const username = ref('')
const mobile = ref('')
const code = ref('123456')
const password = ref('')
const confirmPassword = ref('')

const submitting = ref(false)

const onRegister = async () => {
  if (!username.value.trim()) {
    uni.showToast({ title: '请输入用户名', icon: 'none' })
    return
  }
  if (!mobile.value.trim() || mobile.value.length < 11) {
    uni.showToast({ title: '请输入正确的手机号码', icon: 'none' })
    return
  }
  // Captcha is hidden and pre-filled with mock code '123456'
  if (!password.value.trim() || password.value.length < 6) {
    uni.showToast({ title: '密码长度不能少于6位', icon: 'none' })
    return
  }
  if (password.value !== confirmPassword.value) {
    uni.showToast({ title: '两次输入的密码不一致', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    const data = await request({
      url: api.AuthRegister,
      method: 'POST',
      data: {
        username: username.value,
        password: password.value,
        mobile: mobile.value,
        code: code.value
      }
    })
    
    if (data && data.token) {
      uni.setStorageSync('token', data.token)
      uni.setStorageSync('userInfo', data.userInfo)
      uni.showToast({
        title: '注册成功！',
        icon: 'success'
      })
      setTimeout(() => {
        uni.switchTab({
          url: '/pages/ucenter/index'
        })
      }, 1200)
    }
  } catch (err) {
    console.error('Failed to register:', err)
  } finally {
    submitting.value = false
  }
}

const onGoLogin = () => {
  uni.navigateTo({
    url: '/pages/auth/login'
  })
}

const onBackTap = () => {
  uni.navigateBack({
    fail: () => {
      uni.switchTab({
        url: '/pages/index/index'
      })
    }
  })
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background: var(--dark-gradient);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
  box-sizing: border-box;
  position: relative;
}

/* Custom back button */
.nav-back {
  position: absolute;
  top: 80rpx;
  left: 48rpx;
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  transition: background 0.2s ease;
}

.nav-back:active {
  background: rgba(255, 255, 255, 0.15);
}

.back-icon {
  font-size: 36rpx;
  color: #ffffff;
  font-weight: bold;
}

.register-card {
  width: 100%;
  padding: 64rpx 48rpx;
  border-radius: var(--radius-lg);
  background: rgba(74, 53, 37, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.05);
  box-shadow: var(--shadow-lg);
  display: flex;
  flex-direction: column;
}

.header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 56rpx;
  gap: 8rpx;
}

.title {
  font-size: 40rpx;
  font-weight: 800;
  color: #ffffff;
}

.subtitle {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.6);
  font-weight: 600;
}

.input-wrap {
  height: 96rpx;
  border-radius: var(--radius-sm);
  background-color: rgba(74, 53, 37, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  align-items: center;
  padding: 0 28rpx;
  margin-bottom: 24rpx;
}

.input-icon {
  font-size: 32rpx;
  margin-right: 20rpx;
}

.form-input {
  flex: 1;
  font-size: 26rpx;
  color: #ffffff;
}

/* Captcha row */
.captcha-row {
  display: flex;
  gap: 20rpx;
  margin-bottom: 24rpx;
}

.captcha-input-wrap {
  flex: 1;
  margin-bottom: 0;
}

.captcha-btn {
  width: 220rpx;
  height: 96rpx;
  border-radius: var(--radius-sm);
  background: var(--primary-gradient);
  color: #ffffff;
  font-size: 24rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12rpx rgba(224, 122, 95, 0.2);
}

.captcha-btn:active {
  opacity: 0.85;
}

/* Register button */
.submit-btn {
  width: 100%;
  height: 96rpx;
  border-radius: 48rpx;
  background: var(--primary-gradient);
  color: #ffffff;
  font-size: 28rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(224, 122, 95, 0.3);
  margin-top: 32rpx;
}

.submit-btn:active {
  opacity: 0.85;
}

.login-link {
  text-align: center;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.6);
  font-weight: 600;
  margin-top: 40rpx;
}
</style>
