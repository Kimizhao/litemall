<template>
  <view class="container">
    <view class="form-card glass-panel">
      <!-- Feedback Type Selector -->
      <view class="section">
        <text class="section-title">反馈类型</text>
        <view class="type-grid">
          <view 
            class="type-item" 
            v-for="t in feedbackTypes" 
            :key="t"
            :class="{ active: selectedType === t }"
            @tap="selectedType = t"
          >
            {{ t }}
          </view>
        </view>
      </view>

      <view class="divider"></view>

      <!-- Feedback Content Text -->
      <view class="section">
        <text class="section-title">反馈详情</text>
        <textarea 
          class="feedback-textarea" 
          v-model="content" 
          placeholder="请详细描述您遇到的问题或提出的精美手作改进意见（不少于10字）..." 
          maxlength="300"
        />
        <text class="char-count">{{ content.length }}/300</text>
      </view>

      <view class="divider"></view>

      <!-- Phone coordinates -->
      <view class="section no-gap">
        <text class="section-title">联系方式</text>
        <view class="phone-input-wrap">
          <input type="text" class="phone-input" v-model="mobile" placeholder="留下手机号/邮箱，方便我们回执（选填）" />
        </view>
      </view>

      <view class="divider"></view>

      <!-- Submit button -->
      <button class="submit-btn" @tap="onSubmitFeedback" :disabled="submitting">
        {{ submitting ? '正在提交...' : '提交反馈' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import request from '../../../utils/request'
import api from '../../../config/api'

const feedbackTypes = ['商品相关', '物流服务', '售后服务', '功能建议', '其他问题']
const selectedType = ref('商品相关')
const content = ref('')
const mobile = ref('')
const submitting = ref(false)

const onSubmitFeedback = async () => {
  const val = content.value.trim()
  if (val.length < 10) {
    uni.showToast({
      title: '反馈详情不能少于10个字哦',
      icon: 'none'
    })
    return
  }

  submitting.value = true
  try {
    await request({
      url: api.FeedbackAdd,
      method: 'POST',
      data: {
        feedType: selectedType.value,
        content: val,
        mobile: mobile.value
      }
    })
    
    uni.showToast({
      title: '感谢您的宝贵建议！',
      icon: 'success'
    })
    
    setTimeout(() => {
      uni.navigateBack()
    }, 1200)
  } catch (err) {
    console.error('Failed to submit feedback:', err)
  } finally {
    submitting.value = false
  }
}
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

.section {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.no-gap {
  gap: 12rpx;
}

.section-title {
  font-size: 26rpx;
  font-weight: 800;
  color: var(--text-main);
  border-left: 6rpx solid #4f46e5;
  padding-left: 12rpx;
}

/* Feedback Types Grid */
.type-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
}

.type-item {
  height: 64rpx;
  border-radius: 32rpx;
  background-color: #ffffff;
  border: 1px solid var(--border-light);
  font-size: 22rpx;
  color: var(--text-muted);
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.type-item.active {
  background: var(--primary-gradient);
  color: #ffffff;
  border-color: transparent;
  box-shadow: 0 4px 10px rgba(79, 70, 229, 0.2);
}

.feedback-textarea {
  width: 100%;
  height: 240rpx;
  font-size: 26rpx;
  color: var(--text-main);
  line-height: 1.5;
  font-weight: 500;
}

.char-count {
  text-align: right;
  font-size: 20rpx;
  color: var(--text-muted);
  font-weight: 600;
}

.phone-input-wrap {
  height: 88rpx;
  border-radius: var(--radius-sm);
  background-color: #ffffff;
  border: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  padding: 0 24rpx;
}

.phone-input {
  width: 100%;
  font-size: 24rpx;
  color: var(--text-main);
  font-weight: 500;
}

.divider {
  height: 1px;
  background-color: var(--border-light);
  margin: 32rpx 0;
}

/* Submit button */
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
}

.submit-btn:active {
  opacity: 0.85;
}
</style>
