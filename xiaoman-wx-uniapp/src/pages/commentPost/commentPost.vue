<template>
  <view class="container">
    <view class="form-card glass-panel" v-if="orderId">
      <!-- Stars Selector -->
      <view class="section">
        <text class="section-title">评价得分</text>
        <view class="stars-selector">
          <text 
            class="star-item" 
            v-for="index in 5" 
            :key="index"
            :class="{ highlight: index <= star }"
            @tap="star = index"
          >
            ★
          </text>
        </view>
        <text class="rating-text">{{ getRatingDesc(star) }}</text>
      </view>

      <view class="divider"></view>

      <!-- Review content description -->
      <view class="section">
        <text class="section-title">评价心得</text>
        <textarea 
          class="comment-textarea" 
          v-model="content" 
          placeholder="分享您收到手作宝贝后的心得与喜悦吧（不少于5字）..."
          maxlength="200"
        />
        <text class="char-count">{{ content.length }}/200</text>
      </view>

      <view class="divider"></view>

      <!-- Submit button -->
      <button class="submit-btn" @tap="onSubmit" :disabled="submitting">
        {{ submitting ? '正在提交...' : '发表评价' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '../../utils/request'
import api from '../../config/api'

const orderId = ref(0)
const star = ref(5)
const content = ref('')
const submitting = ref(false)

onLoad((options) => {
  if (options.orderId) {
    orderId.value = parseInt(options.orderId)
  }
})

const getRatingDesc = (score) => {
  const map = {
    1: '非常差，很不满意',
    2: '较差，有些瑕疵',
    3: '一般，还有改进空间',
    4: '满意，做工精美',
    5: '非常满意，精雕细琢！'
  }
  return map[score] || '非常满意！'
}

const onSubmit = async () => {
  const val = content.value.trim()
  if (val.length < 5) {
    uni.showToast({
      title: '评价内容不能少于5个字哦',
      icon: 'none'
    })
    return
  }

  submitting.value = true
  try {
    await request({
      url: api.CommentPost,
      method: 'POST',
      data: {
        type: 0, // Product type
        valueId: orderId.value, // valueId binds to order/goods context
        content: val,
        star: star.value,
        hasPicture: false,
        picUrls: []
      }
    })

    uni.showToast({
      title: '发表评价成功',
      icon: 'success'
    })

    setTimeout(() => {
      uni.navigateBack()
    }, 1200)
  } catch (err) {
    console.error('Failed to post comment:', err)
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
  align-items: center;
}

.section-title {
  font-size: 26rpx;
  font-weight: 800;
  color: var(--text-main);
  border-left: 6rpx solid #4f46e5;
  padding-left: 12rpx;
  align-self: flex-start;
}

/* Stars Selector */
.stars-selector {
  display: flex;
  gap: 16rpx;
}

.star-item {
  font-size: 72rpx;
  color: #e2e8f0;
  cursor: pointer;
  transition: color 0.15s ease;
}

.star-item.highlight {
  color: #fbbf24;
}

.rating-text {
  font-size: 22rpx;
  color: var(--text-muted);
  font-weight: 700;
}

.comment-textarea {
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
  align-self: flex-end;
}

.divider {
  height: 1px;
  background-color: var(--border-light);
  margin: 32rpx 0;
  width: 100%;
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
