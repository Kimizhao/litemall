<template>
  <view class="container">
    <view class="form-card glass-panel">
      <!-- Text Area -->
      <textarea 
        class="comment-textarea" 
        v-model="content" 
        placeholder="写下您对该手作专题的心得与感悟，字数在5-200字之间..."
        maxlength="200"
      />
      <text class="char-count">{{ content.length }}/200</text>

      <view class="divider"></view>

      <!-- Submit button -->
      <button class="submit-btn" @tap="onSubmit" :disabled="submitting">
        {{ submitting ? '正在提交...' : '发表留言' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '../../utils/request'
import api from '../../config/api'

const valueId = ref(0)
const content = ref('')
const submitting = ref(false)

onLoad((options) => {
  if (options.valueId) {
    valueId.value = parseInt(options.valueId)
  }
})

const onSubmit = async () => {
  const val = content.value.trim()
  if (val.length < 5) {
    uni.showToast({
      title: '留言内容不能少于5个字哦',
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
        type: 1, // Topic type
        valueId: valueId.value,
        content: val,
        star: 5,
        hasPicture: false,
        picUrls: []
      }
    })
    
    uni.showToast({
      title: '发表留言成功',
      icon: 'success'
    })
    
    setTimeout(() => {
      uni.navigateBack()
    }, 1000)
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

.comment-textarea {
  width: 100%;
  height: 300rpx;
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
  margin-top: 12rpx;
}

.divider {
  height: 1px;
  background-color: var(--border-light);
  margin: 32rpx 0;
}

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
