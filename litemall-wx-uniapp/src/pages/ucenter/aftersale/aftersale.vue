<template>
  <view class="container">
    <view class="form-card glass-panel" v-if="orderId">
      <!-- Refund Reason Selection -->
      <view class="section">
        <text class="section-title">退款原因</text>
        <view class="reason-list">
          <view 
            class="reason-item" 
            v-for="r in reasons" 
            :key="r"
            :class="{ active: selectedReason === r }"
            @tap="selectedReason = r"
          >
            {{ r }}
          </view>
        </view>
      </view>

      <view class="divider"></view>

      <!-- Refund Amount -->
      <view class="section">
        <text class="section-title">退款金额</text>
        <view class="amount-wrap">
          <text class="currency">¥</text>
          <input type="digit" class="amount-input" v-model="amount" placeholder="退款金额" />
        </view>
        <text class="amount-tip">最多可退款 ¥{{ maxAmount }} 元</text>
      </view>

      <view class="divider"></view>

      <!-- Explanation details -->
      <view class="section">
        <text class="section-title">退款说明</text>
        <textarea 
          class="form-textarea" 
          v-model="comment" 
          placeholder="请详细说明您的退款退货原因..." 
          maxlength="200"
        />
        <text class="char-count">{{ comment.length }}/200</text>
      </view>

      <view class="divider"></view>

      <!-- Submit button -->
      <button class="submit-btn" @tap="onSubmit" :disabled="submitting">
        {{ submitting ? '正在提交...' : '提交申请' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '../../../utils/request'
import api from '../../../config/api'

const orderId = ref(0)
const maxAmount = ref(0.00)
const amount = ref('')
const selectedReason = ref('不喜欢/不想要')
const comment = ref('')
const submitting = ref(false)

const reasons = ['不喜欢/不想要', '做工材质不满意', '尺寸/规格不符', '快递配送延迟', '手作商品有损坏']

onLoad((options) => {
  if (options.orderId) {
    orderId.value = parseInt(options.orderId)
  }
  if (options.amount) {
    maxAmount.value = parseFloat(options.amount)
    amount.value = options.amount
  }
})

const onSubmit = async () => {
  const refundAmount = parseFloat(amount.value)
  if (isNaN(refundAmount) || refundAmount <= 0 || refundAmount > maxAmount.value) {
    uni.showToast({
      title: '请输入合法的退款金额',
      icon: 'none'
    })
    return
  }

  submitting.value = true
  try {
    await request({
      url: api.AftersaleSubmit,
      method: 'POST',
      data: {
        orderId: orderId.value,
        type: 0, // standard return
        reason: selectedReason.value,
        amount: refundAmount,
        comment: comment.value.trim(),
        pictures: []
      }
    })

    uni.showToast({
      title: '售后申请已提交！',
      icon: 'success'
    })

    setTimeout(() => {
      uni.navigateTo({
        url: '/pages/ucenter/aftersaleList/aftersaleList'
      })
    }, 1200)
  } catch (err) {
    console.error('Failed to submit aftersale:', err)
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

.section-title {
  font-size: 26rpx;
  font-weight: 800;
  color: var(--text-main);
  border-left: 6rpx solid #4f46e5;
  padding-left: 12rpx;
}

/* Reasons List */
.reason-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.reason-item {
  padding: 12rpx 28rpx;
  border-radius: 28rpx;
  background-color: #ffffff;
  border: 1px solid var(--border-light);
  font-size: 22rpx;
  color: var(--text-muted);
  font-weight: 600;
}

.reason-item.active {
  background: var(--primary-gradient);
  color: #ffffff;
  border-color: transparent;
  box-shadow: 0 4px 10px rgba(79, 70, 229, 0.2);
}

/* Refund Amount */
.amount-wrap {
  display: flex;
  align-items: center;
  gap: 16rpx;
  border-bottom: 1px solid var(--border-light);
  padding-bottom: 16rpx;
}

.currency {
  font-size: 40rpx;
  font-weight: 800;
  color: var(--text-main);
}

.amount-input {
  flex: 1;
  font-size: 36rpx;
  font-weight: 800;
  color: var(--text-main);
}

.amount-tip {
  font-size: 20rpx;
  color: var(--text-muted);
  font-weight: 600;
}

.form-textarea {
  width: 100%;
  height: 200rpx;
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
