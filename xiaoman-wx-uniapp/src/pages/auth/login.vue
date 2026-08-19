<template>
  <view class="container">
    <!-- Close Button -->
    <view class="close-btn" @tap="onBackTap">✕</view>

    <!-- Logo & Title Section -->
    <view class="brand-section">
      <view class="brand-logo">
        <text class="brand-logo-char">满</text>
      </view>
      <text class="brand-title">欢迎登录小满食光</text>
    </view>

    <!-- TAB 1: WeChat One-Click & Other Number Login -->
    <block v-if="activeTab === 'main'">

      <!-- Action Buttons -->
      <view class="button-group">
        <button 
          class="action-btn primary"
          :open-type="agreed ? 'getPhoneNumber' : ''"
          @getphonenumber="handleWeixinPhoneLogin"
          @tap="onPhoneBtnTap"
          :disabled="loading"
        >
          手机号快捷登录
        </button>
        
        <!-- <button class="action-btn secondary" @tap="activeTab = 'sms'">
          验证码登录
        </button> -->

        <!-- <button class="action-btn secondary" @tap="activeTab = 'account'">
          账号密码登录
        </button> -->
      </view>

      <!-- Agreement Checkbox -->
      <view class="agreement-wrapper">
        <label class="checkbox-label" @tap="toggleAgreement">
          <checkbox :checked="agreed" color="#6F4E37" style="transform: scale(0.8);" />
          <view class="agreement-text">
            我已认真阅读、理解并同意
            <text class="agreement-link" @tap.stop="openDoc('service')">《用户协议》</text>
            和
            <text class="agreement-link" @tap.stop="openDoc('privacy')">《隐私协议》</text>
            ，登录后自动绑定当前微信
          </view>
        </label>
      </view>

      <!-- Footer action -->
      <view class="footer-btn" @tap="onBackTap">暂不登录</view>
    </block>

    <!-- TAB 2: SMS Verification Code Login -->
    <block v-else-if="activeTab === 'sms'">
      <view class="sms-form">
        <!-- Back Arrow -->
        <view class="back-arrow" @tap="activeTab = 'main'">‹ 其他登录方式</view>

        <view class="input-group">
          <input 
            type="number" 
            class="form-input" 
            v-model="smsPhone" 
            placeholder="请输入手机号" 
            maxlength="11"
            placeholder-style="color: #A18276;"
          />
          <view class="code-input-wrap">
            <input 
              type="number" 
              class="form-input code-input" 
              v-model="smsCode" 
              placeholder="输入验证码" 
              maxlength="6"
              placeholder-style="color: #A18276;"
            />
            <text 
              class="send-code-link" 
              :class="{ disabled: countdown > 0 }" 
              @tap="onSendCode"
            >
              {{ countdown > 0 ? `${countdown}s 后重试` : '获取验证码' }}
            </text>
          </view>
        </view>

        <!-- Agreement Checkbox (Shared) -->
        <view class="agreement-wrapper">
          <label class="checkbox-label" @tap="toggleAgreement">
            <checkbox :checked="agreed" color="#6F4E37" style="transform: scale(0.8);" />
            <view class="agreement-text">
              我已认真阅读、理解并同意
              <text class="agreement-link" @tap.stop="openDoc('service')">《用户协议》</text>
              和
              <text class="agreement-link" @tap.stop="openDoc('privacy')">《隐私协议》</text>
              ，登录后自动绑定当前微信
            </view>
          </label>
        </view>

        <button 
          class="action-btn primary sms-submit-btn" 
          @tap="handleSmsLogin"
          :disabled="loading"
        >
          登录
        </button>

        <view class="sms-help" @tap="onSmsHelp">遇到问题</view>
      </view>
    </block>

    <!-- TAB 3: Username & Password Login -->
    <block v-else-if="activeTab === 'account'">
      <view class="sms-form">
        <!-- Back Arrow -->
        <view class="back-arrow" @tap="activeTab = 'main'">‹ 其他登录方式</view>

        <view class="input-group">
          <input 
            type="text" 
            class="form-input" 
            v-model="accountUsername" 
            placeholder="请输入用户名或手机号" 
            placeholder-style="color: #A18276;"
          />
          <input 
            type="password" 
            class="form-input" 
            v-model="accountPassword" 
            placeholder="请输入密码" 
            placeholder-style="color: #A18276;"
          />
        </view>

        <!-- Agreement Checkbox (Shared) -->
        <view class="agreement-wrapper">
          <label class="checkbox-label" @tap="toggleAgreement">
            <checkbox :checked="agreed" color="#6F4E37" style="transform: scale(0.8);" />
            <view class="agreement-text">
              我已认真阅读、理解并同意
              <text class="agreement-link" @tap.stop="openDoc('service')">《用户协议》</text>
              和
              <text class="agreement-link" @tap.stop="openDoc('privacy')">《隐私协议》</text>
              ，登录后自动绑定当前微信
            </view>
          </label>
        </view>

        <button 
          class="action-btn primary sms-submit-btn" 
          @tap="handleAccountLogin"
          :disabled="loading"
        >
          登录
        </button>

        <view class="account-actions">
          <text class="account-action-link" @tap="onGoResetPassword">忘记密码？</text>
        </view>
      </view>
    </block>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import api from '../../config/api'

// View states
const activeTab = ref('main') // 'main', 'sms', or 'account'
const agreed = ref(false)
const loading = ref(false)

// Silent Login Credentials
const openid = ref('')
const sessionKey = ref('')

// SMS Form
const smsPhone = ref('')
const smsCode = ref('')
const countdown = ref(0)
let timer = null

// Account Form
const accountUsername = ref('')
const accountPassword = ref('')

const toggleAgreement = () => {
  agreed.value = !agreed.value
}

const openDoc = (docType) => {
  uni.navigateTo({
    url: `/pages/auth/doc?type=${docType}`
  })
}

const onPhoneBtnTap = () => {
  if (!agreed.value) {
    uni.showToast({
      title: '请先阅读并同意用户协议和隐私协议',
      icon: 'none'
    })
  }
}

const onSmsHelp = () => {
  uni.showModal({
    title: '遇到问题',
    content: '如果您无法收到验证码或有其他登录问题，请联系门店前台，或添加微信客服反馈。',
    showCancel: false,
    confirmText: '我知道了',
    confirmColor: '#6F4E37'
  })
}

const onGoResetPassword = () => {
  uni.navigateTo({
    url: '/pages/auth/reset'
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

// 60s countdown for SMS
const onSendCode = async () => {
  if (countdown.value > 0) return
  if (!smsPhone.value || smsPhone.value.length < 11) {
    uni.showToast({
      title: '请输入正确的手机号码',
      icon: 'none'
    })
    return
  }

  uni.showLoading({ title: '正在发送...' })
  try {
    // Call backend captcha send mapping
    await request({
      url: api.AuthRegisterCaptcha,
      method: 'POST',
      data: {
        mobile: smsPhone.value,
        type: 'login'
      }
    })

    uni.showToast({
      title: '验证码已发送至您的手机，测试环境验证码为123456',
      icon: 'none',
      duration: 3000
    })

    // Start timer
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (err) {
    console.error('发送验证码失败:', err)
  } finally {
    uni.hideLoading()
  }
}

// SMS Login Action
const handleSmsLogin = async () => {
  if (!agreed.value) {
    uni.showToast({
      title: '请先同意协议并勾选确认',
      icon: 'none'
    })
    return
  }
  if (!smsPhone.value || smsPhone.value.length < 11) {
    uni.showToast({
      title: '请输入有效的手机号',
      icon: 'none'
    })
    return
  }
  if (!smsCode.value) {
    uni.showToast({
      title: '请输入验证码',
      icon: 'none'
    })
    return
  }

  loading.value = true
  try {
    const data = await request({
      url: api.AuthLoginBySms,
      method: 'POST',
      data: {
        mobile: smsPhone.value,
        code: smsCode.value
      }
    })

    if (data && data.token) {
      uni.removeStorageSync('hasLoggedOut')
      uni.setStorageSync('token', data.token)
      uni.setStorageSync('userInfo', JSON.stringify(data.userInfo))

      uni.showToast({
        title: '登录成功！',
        icon: 'success'
      })

      setTimeout(() => {
        uni.switchTab({
          url: '/pages/index/index'
        })
      }, 1000)
    }
  } catch (err) {
    console.error('手机验证码登录失败:', err)
  } finally {
    loading.value = false
  }
}

// Account Username & Password Login Action
const handleAccountLogin = async () => {
  if (!agreed.value) {
    uni.showToast({
      title: '请先同意协议并勾选确认',
      icon: 'none'
    })
    return
  }
  if (!accountUsername.value.trim()) {
    uni.showToast({
      title: '请输入用户名或手机号',
      icon: 'none'
    })
    return
  }
  if (!accountPassword.value) {
    uni.showToast({
      title: '请输入密码',
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
        username: accountUsername.value.trim(),
        password: accountPassword.value
      }
    })

    if (data && data.token) {
      uni.removeStorageSync('hasLoggedOut')
      uni.setStorageSync('token', data.token)
      uni.setStorageSync('userInfo', JSON.stringify(data.userInfo))

      uni.showToast({
        title: '登录成功！',
        icon: 'success'
      })

      setTimeout(() => {
        uni.switchTab({
          url: '/pages/index/index'
        })
      }, 1000)
    }
  } catch (err) {
    console.error('账号密码登录失败:', err)
  } finally {
    loading.value = false
  }
}

// WeChat silent login
const checkSilentLogin = () => {
  // #ifdef MP-WEIXIN
  uni.login({
    provider: 'weixin',
    success: async (loginRes) => {
      try {
        const res = await request({
          url: api.AuthWeixinSilentLogin,
          method: 'POST',
          data: {
            code: loginRes.code
          }
        })

        if (res) {
          if (res.openid) openid.value = res.openid
          if (res.sessionKey) sessionKey.value = res.sessionKey

          if (res.isBound && res.token) {
            // Check if user manually logged out previously
            const hasLoggedOut = uni.getStorageSync('hasLoggedOut')
            if (hasLoggedOut) {
              console.log('用户曾手动退出登录，禁用本次自动静默登录跳转')
              return
            }

            uni.setStorageSync('token', res.token)
            uni.setStorageSync('userInfo', JSON.stringify(res.userInfo))
            uni.showToast({
              title: '自动登录成功',
              icon: 'success'
            })
            setTimeout(() => {
              uni.switchTab({
                url: '/pages/index/index'
              })
            }, 1000)
          }
        }
      } catch (err) {
        console.error('静默登录失败:', err)
      }
    },
    fail: (err) => {
      console.error('wx.login failed:', err)
    }
  })
  // #endif
}

onMounted(() => {
  checkSilentLogin()
})

const handleWeixinPhoneLogin = async (e) => {
  if (e.detail.errMsg !== 'getPhoneNumber:ok') {
    uni.showToast({
      title: '您取消了手机号授权，无法登录',
      icon: 'none'
    })
    return
  }

  if (!openid.value || !sessionKey.value) {
    uni.showToast({
      title: '正在初始化连接，请重试',
      icon: 'none'
    })
    checkSilentLogin()
    return
  }

  const { encryptedData, iv } = e.detail
  loading.value = true

  try {
    const data = await request({
      url: api.AuthWeixinPhoneLogin,
      method: 'POST',
      data: {
        openid: openid.value,
        sessionKey: sessionKey.value,
        encryptedData: encryptedData,
        iv: iv
      }
    })

    if (data && data.token) {
      uni.removeStorageSync('hasLoggedOut')
      uni.setStorageSync('token', data.token)
      uni.setStorageSync('userInfo', JSON.stringify(data.userInfo))

      uni.showToast({
        title: '登录成功！',
        icon: 'success'
      })

      setTimeout(() => {
        uni.switchTab({
          url: '/pages/index/index'
        })
      }, 1000)
    }
  } catch (err) {
    console.error('微信一键登录失败:', err)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background-color: #FAF6EE; /* Warm Cream-Beige */
  padding: 80rpx 48rpx;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  position: relative;
}

.close-btn {
  position: absolute;
  top: 60rpx;
  right: 48rpx;
  font-size: 40rpx;
  color: #A18276;
  padding: 10rpx;
  z-index: 10;
}

.brand-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 100rpx;
  margin-bottom: 80rpx;
}

.brand-logo {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  background-color: #6F4E37; /* Coffee Brown */
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(111, 78, 55, 0.15);
  margin-bottom: 28rpx;
}

.brand-logo-char {
  font-size: 64rpx;
  font-weight: 800;
  color: #ffffff;
}

.brand-title {
  font-size: 38rpx;
  font-weight: 800;
  color: #4A3525;
}

/* Agreement Checkbox Styling */
.agreement-wrapper {
  margin-bottom: 50rpx;
  display: flex;
  align-items: flex-start;
}

.checkbox-label {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
}

.agreement-text {
  font-size: 23rpx;
  color: #A18276;
  line-height: 1.6;
}

.agreement-link {
  color: #E07A5F; /* Terracotta highlight */
  font-weight: 700;
  display: inline;
}

/* Action Buttons */
.button-group {
  display: flex;
  flex-direction: column;
  gap: 28rpx;
  margin-bottom: 60rpx;
}

.action-btn {
  width: 100%;
  height: 96rpx;
  border-radius: 48rpx;
  font-size: 30rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.action-btn::after {
  border: none;
}

.action-btn.primary {
  background-color: #6F4E37; /* Theme Coffee Brown */
  color: #ffffff;
  box-shadow: 0 8px 24px rgba(111, 78, 55, 0.2);
}

.action-btn.primary:active {
  opacity: 0.9;
  transform: translateY(2rpx);
}

.action-btn.secondary {
  background-color: transparent;
  border: 2rpx solid #6F4E37;
  color: #6F4E37;
}

.action-btn.secondary:active {
  background-color: rgba(111, 78, 55, 0.05);
}

.footer-btn {
  text-align: center;
  font-size: 26rpx;
  color: #A18276;
  margin-top: auto;
  padding: 20rpx 0;
  font-weight: 600;
}

/* SMS & Account Verification View */
.sms-form {
  display: flex;
  flex-direction: column;
}

.back-arrow {
  font-size: 26rpx;
  color: #6F4E37;
  font-weight: 700;
  margin-bottom: 40rpx;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 28rpx;
  margin-bottom: 40rpx;
}

.form-input {
  width: 100%;
  height: 96rpx;
  background-color: #ffffff;
  border: 1rpx solid rgba(161, 130, 118, 0.2);
  border-radius: 20rpx;
  padding: 0 32rpx;
  box-sizing: border-box;
  font-size: 28rpx;
  color: #4A3525;
}

.code-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.code-input {
  padding-right: 200rpx;
}

.send-code-link {
  position: absolute;
  right: 32rpx;
  font-size: 26rpx;
  font-weight: 700;
  color: #E07A5F;
  z-index: 5;
}

.send-code-link.disabled {
  color: #A18276;
  font-weight: 500;
}

.sms-submit-btn {
  margin-top: 10rpx;
}

.sms-help {
  text-align: center;
  font-size: 24rpx;
  color: #A18276;
  margin-top: 40rpx;
  font-weight: 500;
}

.account-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 30rpx;
}

.account-action-link {
  font-size: 26rpx;
  color: #E07A5F;
  font-weight: 600;
}
</style>
