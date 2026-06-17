<template>
  <view class="container">
    <!-- Back Navigation Icon -->
    <view class="nav-back" @tap="onBackTap">
      <text class="back-icon">←</text>
    </view>

    <!-- Branding Section -->
    <view class="brand-section">
      <view class="logo-circle">
        <text class="logo-text">LM</text>
      </view>
      <text class="brand-title">LITEMALL 官方旗舰店</text>
      <text class="brand-subtitle">使用您的商城账号进行登录</text>
    </view>

    <!-- Form Section -->
    <view class="form-section">
      <view class="input-card glass-panel">
        <!-- Username Input -->
        <view class="input-group">
          <text class="input-label">商城账号</text>
          <view class="input-wrapper">
            <text class="input-icon">👤</text>
            <input type="text" v-model="username" placeholder="请输入账号 (如 user123)" placeholder-style="color: #94a3b8;" />
          </view>
        </view>

        <!-- Password Input -->
        <view class="input-group">
          <text class="input-label">商城密码</text>
          <view class="input-wrapper">
            <text class="input-icon">🔒</text>
            <input :type="showPassword ? 'text' : 'password'" v-model="password" placeholder="请输入密码 (如 user123)" placeholder-style="color: #94a3b8;" />
            <text class="pass-toggle" @tap="togglePassword">{{ showPassword ? '👁️' : '🙈' }}</text>
          </view>
        </view>
      </view>

      <!-- Submit Button -->
      <button class="submit-btn" :disabled="loading" :class="{ 'btn-loading': loading }" @tap="handleLogin">
        <text v-if="!loading">立即登录</text>
        <text v-else class="btn-spinner">⌛ 正在登录...</text>
      </button>

      <!-- Helper Links -->
      <view class="helper-links">
        <text class="helper-link" @tap="onGoRegister">立即注册</text>
        <text class="helper-divider">|</text>
        <text class="helper-link" @tap="onGoReset">忘记密码</text>
      </view>

      <!-- Helper info for local testing -->
      <view class="login-tip">
        <text class="tip-title">本地开发测试账号：</text>
        <text class="tip-desc">账号：<text class="highlight">user123</text> | 密码：<text class="highlight">user123</text></text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import request from '../../utils/request'
import api from '../../config/api'

// Form states
const username = ref('')
const password = ref('')
const showPassword = ref(false)
const loading = ref(false)

const togglePassword = () => {
  showPassword.value = !showPassword.value
}

const onBackTap = () => {
  uni.navigateBack({
    fail: () => {
      // Fallback if there's no back history
      uni.switchTab({
        url: '/pages/index/index'
      })
    }
  })
}

const onGoRegister = () => {
  uni.navigateTo({
    url: '/pages/auth/register'
  })
}

const onGoReset = () => {
  uni.navigateTo({
    url: '/pages/auth/reset'
  })
}

const handleLogin = async () => {
  if (!username.value.trim() || !password.value.trim()) {
    uni.showToast({
      title: '请输入账号和密码！',
      icon: 'none'
    })
    return
  }

  loading.value = true
  try {
    const data = await request({
      url: api.AuthLoginByAccount,
      method: 'POST',
      data: {
        username: username.value,
        password: password.value
      }
    })

    if (data && data.token) {
      // Success - Save token & userInfo to LocalStorage
      uni.setStorageSync('token', data.token)
      uni.setStorageSync('userInfo', JSON.stringify(data.userInfo))

      uni.showToast({
        title: '登录成功，欢迎回来！',
        icon: 'success'
      })

      // Return back to user profile page
      setTimeout(() => {
        onBackTap()
      }, 1000)
    }
  } catch (err) {
    console.error('Failed to log in:', err)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background: var(--dark-gradient);
  display: flex;
  flex-direction: column;
  padding: 0 48rpx;
  box-sizing: border-box;
}

/* Custom back button */
.nav-back {
  margin-top: 80rpx;
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
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

/* Branding Section */
.brand-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 60rpx;
  margin-bottom: 80rpx;
}

.logo-circle {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  background: var(--primary-gradient);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 20px rgba(59, 130, 246, 0.3);
  margin-bottom: 32rpx;
}

.logo-text {
  font-size: 48rpx;
  font-weight: 800;
  color: #ffffff;
  letter-spacing: -2rpx;
}

.brand-title {
  font-size: 40rpx;
  font-weight: 800;
  color: #ffffff;
  letter-spacing: 2rpx;
  margin-bottom: 12rpx;
}

.brand-subtitle {
  font-size: 24rpx;
  color: #94a3b8;
  font-weight: 500;
  text-align: center;
}

/* Form Section */
.form-section {
  display: flex;
  flex-direction: column;
  gap: 40rpx;
}

.input-card {
  border-radius: var(--radius-md);
  padding: 40rpx 32rpx;
  background: rgba(30, 41, 59, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.input-group {
  display: flex;
  flex-direction: column;
  margin-bottom: 36rpx;
}

.input-group:last-child {
  margin-bottom: 0;
}

.input-label {
  font-size: 24rpx;
  color: #94a3b8;
  font-weight: 600;
  margin-bottom: 16rpx;
}

.input-wrapper {
  display: flex;
  align-items: center;
  height: 88rpx;
  border-radius: var(--radius-sm);
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.05);
  padding: 0 24rpx;
  box-sizing: border-box;
}

.input-icon {
  font-size: 28rpx;
  margin-right: 16rpx;
}

.input-wrapper input {
  flex: 1;
  font-size: 28rpx;
  color: #ffffff;
  height: 100%;
}

.pass-toggle {
  font-size: 28rpx;
  padding: 10rpx;
}

/* Submit primary action */
.submit-btn {
  width: 100%;
  height: 96rpx;
  border-radius: var(--radius-md);
  background: var(--primary-gradient);
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 20px rgba(79, 70, 229, 0.3);
  transition: all 0.2s ease;
}

.submit-btn:active {
  transform: translateY(2rpx);
  box-shadow: 0 5px 10px rgba(79, 70, 229, 0.2);
}

.submit-btn[disabled] {
  opacity: 0.7;
}

.btn-spinner {
  font-size: 28rpx;
}

/* Helper Links */
.helper-links {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16rpx;
  margin-top: 16rpx;
}

.helper-link {
  font-size: 24rpx;
  color: #94a3b8;
  font-weight: 600;
  padding: 10rpx 20rpx;
  transition: color 0.2s ease;
}

.helper-link:active {
  color: #3b82f6;
}

.helper-divider {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.1);
}

/* Local Testing Tip */
.login-tip {
  margin-top: 40rpx;
  background: rgba(255, 255, 255, 0.03);
  border-radius: var(--radius-sm);
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  border: 1rpx dashed rgba(255, 255, 255, 0.1);
}

.tip-title {
  font-size: 22rpx;
  color: #ef4444;
  font-weight: 700;
  margin-bottom: 6rpx;
}

.tip-desc {
  font-size: 22rpx;
  color: #94a3b8;
  font-weight: 500;
}

.highlight {
  color: #3b82f6;
  font-weight: 700;
}
</style>
