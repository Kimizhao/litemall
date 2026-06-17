<template>
  <view class="container">
    <view class="register-card glass-panel">
      <!-- Title -->
      <view class="header">
        <text class="title">密码重置</text>
        <text class="subtitle">安全更新您的会员账户密码</text>
      </view>

      <!-- Mobile phone -->
      <view class="input-wrap">
        <text class="input-icon">📱</text>
        <input type="number" class="form-input" v-model="mobile" placeholder="请输入绑定的手机号码" maxlength="11" />
      </view>

      <!-- Captcha -->
      <view class="captcha-row">
        <view class="input-wrap captcha-input-wrap">
          <text class="input-icon">🔑</text>
          <input type="number" class="form-input" v-model="code" placeholder="短信验证码" maxlength="6" />
        </view>
        <button class="captcha-btn" @tap="onGetCaptcha" :disabled="countdown > 0">
          {{ countdown > 0 ? countdown + 's' : '获取验证码' }}
        </button>
      </view>

      <!-- Password -->
      <view class="input-wrap">
        <text class="input-icon">🔒</text>
        <input type="password" class="form-input" v-model="password" placeholder="请输入新密码" />
      </view>

      <view class="input-wrap">
        <text class="input-icon">🔒</text>
        <input type="password" class="form-input" v-model="confirmPassword" placeholder="请再次确认新密码" />
      </view>

      <!-- Submit -->
      <button class="submit-btn" @tap="onResetPassword" :disabled="submitting">
        {{ submitting ? '正在重置...' : '重置密码' }}
      </button>

      <text class="login-link" @tap="onGoLogin">返回登录</text>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import request from '../../utils/request'
import api from '../../config/api'

const mobile = ref('')
const code = ref('')
const password = ref('')
const confirmPassword = ref('')

const countdown = ref(0)
const submitting = ref(false)

const onGetCaptcha = async () => {
  if (!mobile.value.trim() || mobile.value.length < 11) {
    uni.showToast({ title: '请输入正确的手机号码', icon: 'none' })
    return
  }

  try {
    await request({
      url: api.AuthRegisterCaptcha,
      method: 'POST',
      data: { mobile: mobile.value }
    })
    uni.showToast({ title: '验证码已发送', icon: 'success' })
    
    // Countdown timer
    countdown.value = 60
    const timer = setInterval(() => {
      if (countdown.value > 0) {
        countdown.value -= 1
      } else {
        clearInterval(timer)
      }
    }, 1000)
  } catch (err) {
    console.error('Failed to get captcha:', err)
  }
}

const onResetPassword = async () => {
  if (!mobile.value.trim() || mobile.value.length < 11) {
    uni.showToast({ title: '请输入正确的手机号码', icon: 'none' })
    return
  }
  if (!code.value.trim()) {
    uni.showToast({ title: '请输入短信验证码', icon: 'none' })
    return
  }
  if (!password.value.trim() || password.value.length < 6) {
    uni.showToast({ title: '新密码长度不能少于6位', icon: 'none' })
    return
  }
  if (password.value !== confirmPassword.value) {
    uni.showToast({ title: '两次输入的密码不一致', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    await request({
      url: api.AuthReset,
      method: 'POST',
      data: {
        mobile: mobile.value,
        code: code.value,
        password: password.value
      }
    })
    
    uni.showToast({
      title: '密码重置成功',
      icon: 'success'
    })
    setTimeout(() => {
      uni.navigateTo({
        url: '/pages/auth/login'
      })
    }, 1200)
  } catch (err) {
    console.error('Failed to reset password:', err)
  } finally {
    submitting.value = false
  }
}

const onGoLogin = () => {
  uni.navigateTo({
    url: '/pages/auth/login'
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
}

.register-card {
  width: 100%;
  padding: 64rpx 48rpx;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 255, 255, 0.15);
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
  background-color: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
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
  box-shadow: 0 4px 12rpx rgba(79, 70, 229, 0.2);
}

.captcha-btn:active {
  opacity: 0.85;
}

/* Submit button */
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
  box-shadow: 0 8px 24px rgba(79, 70, 229, 0.3);
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
