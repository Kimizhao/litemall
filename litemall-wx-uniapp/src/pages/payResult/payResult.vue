<template>
  <view class="container">
    <view class="result-card glass-panel">
      <!-- Success Icon -->
      <view class="icon-wrap success-icon" v-if="payStatus">
        <text class="state-icon">✓</text>
      </view>
      <!-- Failure Icon -->
      <view class="icon-wrap error-icon" v-else>
        <text class="state-icon">✕</text>
      </view>

      <text class="result-title">{{ payStatus ? '下单成功' : '支付失败' }}</text>
      <text class="result-desc">
        {{ payStatus ? '您的宝贝手作订单已提交成功，感谢您的支持！' : '订单已生成，但支付尚未完成，请尽快前往订单中心进行付款。' }}
      </text>

      <!-- cost or action values -->
      <view class="divider"></view>

      <!-- Action Buttons -->
      <view class="action-wrap">
        <button class="primary-btn" @tap="onViewOrder" v-if="payStatus">查看订单</button>
        <button class="primary-btn fail-btn" @tap="onPayRetry" v-else>重新支付</button>
        <button class="secondary-btn" @tap="onGoShopping">继续逛逛</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '../../utils/request'
import api from '../../config/api'

const payStatus = ref(false)
const orderId = ref(0)

onLoad((options) => {
  if (options.status) {
    payStatus.value = options.status === 'true'
  }
  if (options.orderId) {
    orderId.value = parseInt(options.orderId)
  }
})

const onViewOrder = () => {
  uni.navigateTo({
    url: `/pages/ucenter/orderDetail/orderDetail?id=${orderId.value}`
  })
}

const onPayRetry = async () => {
  uni.showLoading({ title: '正在发起支付...' })
  try {
    const data = await request({
      url: api.OrderPrepay,
      method: 'POST',
      data: {
        orderId: orderId.value
      }
    })
    
    // Simulate payment response
    uni.hideLoading()
    uni.showToast({
      title: '模拟支付成功',
      icon: 'success'
    })
    
    setTimeout(() => {
      payStatus.value = true
    }, 1000)
  } catch (err) {
    uni.hideLoading()
    console.error('Payment retry failed:', err)
  }
}

const onGoShopping = () => {
  uni.switchTab({
    url: '/pages/index/index'
  })
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background-color: var(--bg-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
  box-sizing: border-box;
}

.result-card {
  width: 100%;
  padding: 64rpx 48rpx;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: var(--shadow-md);
}

.icon-wrap {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 32rpx;
}

.success-icon {
  background: var(--secondary-gradient);
  box-shadow: 0 8px 20px rgba(16, 185, 129, 0.25);
}

.error-icon {
  background: var(--accent-gradient);
  box-shadow: 0 8px 20px rgba(239, 68, 68, 0.25);
}

.state-icon {
  color: #ffffff;
  font-size: 56rpx;
  font-weight: bold;
}

.result-title {
  font-size: 38rpx;
  font-weight: 800;
  color: var(--text-main);
  margin-bottom: 16rpx;
}

.result-desc {
  font-size: 24rpx;
  color: var(--text-muted);
  text-align: center;
  line-height: 1.5;
  margin-bottom: 48rpx;
}

.divider {
  width: 100%;
  height: 1px;
  background-color: var(--border-light);
  margin-bottom: 48rpx;
}

.action-wrap {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.primary-btn {
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

.fail-btn {
  background: var(--accent-gradient);
  box-shadow: 0 6px 16px rgba(236, 72, 153, 0.2);
}

.primary-btn:active {
  opacity: 0.85;
}

.secondary-btn {
  width: 100%;
  height: 88rpx;
  border-radius: 44rpx;
  background-color: #ffffff;
  color: var(--text-main);
  font-size: 26rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-light);
}

.secondary-btn:active {
  background-color: #f8fafc;
}
</style>
