<template>
  <view class="login-container">
    <view class="login-card">
      <view class="brand-header">
        <view class="brand-icon-wrapper">
          <text class="brand-icon">🌿</text>
        </view>
      </view>

      <view class="form-wrapper">
        <view class="input-group">
          <text class="input-label">管理账号</text>
          <input 
            class="input-field" 
            v-model="form.username" 
            placeholder="请输入管理员账号" 
            placeholder-class="placeholder-text"
          />
        </view>

        <view class="input-group">
          <text class="input-label">管理密码</text>
          <input 
            class="input-field" 
            v-model="form.password" 
            type="password" 
            placeholder="请输入管理密码" 
            placeholder-class="placeholder-text"
          />
        </view>

        <button class="submit-btn" :loading="submitting" @tap="onSubmit">
          验证并绑定
        </button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import request from '../../utils/request'
import api from '../../config/api'

const form = reactive({
  username: '',
  password: ''
})

const submitting = ref(false)

onMounted(async () => {
  try {
    const res = await request({ url: api.AdminAuthStatus, hideErrorToast: true })
    if (res && res.isBound) {
      uni.redirectTo({ url: '/pages/admin/workbench' })
    }
  } catch (e) {
    // ignore
  }
})

const onSubmit = async () => {
  if (!form.username.trim()) {
    uni.showToast({ title: '请输入管理员账号', icon: 'none' })
    return
  }
  if (!form.password.trim()) {
    uni.showToast({ title: '请输入管理员密码', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    const res = await request({
      url: api.AdminAuthLogin,
      method: 'POST',
      data: {
        username: form.username.trim(),
        password: form.password.trim()
      }
    })

    if (res && res.isBound) {
      uni.showToast({ title: '绑定成功', icon: 'success' })
      setTimeout(() => {
        uni.redirectTo({ url: '/pages/admin/workbench' })
      }, 500)
    } else {
      uni.showToast({ title: '绑定未完成，请重试', icon: 'none' })
    }
  } catch (err) {
    console.error('Login error:', err)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  background-color: #FAF6EE;
  padding: 40rpx 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
}

.login-card {
  width: 100%;
  background: #ffffff;
  border-radius: 28rpx;
  padding: 56rpx 40rpx 48rpx;
  box-shadow: 0 10rpx 36rpx rgba(111, 78, 55, 0.08);
  border: 2rpx solid #F0E6D8;
  box-sizing: border-box;
}

.brand-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 40rpx;
}

.brand-icon-wrapper {
  width: 104rpx;
  height: 104rpx;
  background: #6F4E37;
  border-radius: 26rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0;
  box-shadow: 0 8rpx 20rpx rgba(111, 78, 55, 0.2);
}

.brand-icon {
  font-size: 52rpx;
}

.form-wrapper {
  display: flex;
  flex-direction: column;
  gap: 32rpx;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.input-label {
  font-size: 26rpx;
  font-weight: 600;
  color: #4A3525;
}

.input-field {
  height: 88rpx;
  background: #FAF6EE;
  border: 2rpx solid #E6D7C3;
  border-radius: 16rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  color: #4A3525;
}

.placeholder-text {
  color: #B5A49B;
}

.submit-btn {
  height: 92rpx;
  line-height: 92rpx;
  background: #6F4E37;
  color: #FAF6EE;
  font-size: 30rpx;
  font-weight: 700;
  border-radius: 20rpx;
  margin-top: 16rpx;
  box-shadow: 0 6rpx 18rpx rgba(111, 78, 55, 0.25);
}

.submit-btn:active {
  opacity: 0.9;
}
</style>
