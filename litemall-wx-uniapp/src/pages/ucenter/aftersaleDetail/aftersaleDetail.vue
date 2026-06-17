<template>
  <view class="container" v-if="detail">
    <scroll-view scroll-y class="detail-scroll">
      <!-- Status Banner -->
      <view class="status-banner" :class="'bg-' + detail.status">
        <text class="status-text">{{ getStatusText(detail.status) }}</text>
        <text class="status-desc">服务单号: {{ detail.aftersaleSn }}</text>
      </view>

      <!-- Cost breakdown card -->
      <view class="info-card glass-panel">
        <view class="info-row">
          <text class="label">退款金额</text>
          <text class="value text-red">¥{{ detail.amount }}</text>
        </view>
        <view class="info-row">
          <text class="label">申请原因</text>
          <text class="value">{{ detail.reason }}</text>
        </view>
        <view class="info-row">
          <text class="label">申请说明</text>
          <text class="value">{{ detail.comment || '无' }}</text>
        </view>
      </view>

      <!-- Service metadata log -->
      <view class="meta-card glass-panel">
        <view class="meta-row">
          <text class="meta-label">申请时间</text>
          <text class="meta-val">{{ detail.addTime }}</text>
        </view>
        <view class="meta-row" v-if="detail.handleTime">
          <text class="meta-label">处理时间</text>
          <text class="meta-val">{{ detail.handleTime }}</text>
        </view>
        <view class="meta-row" v-if="detail.handleComment">
          <text class="meta-label">商家回执</text>
          <text class="meta-val text-indigo">{{ detail.handleComment }}</text>
        </view>
      </view>

      <view class="footer-spacer"></view>
    </scroll-view>
  </view>

  <view class="loading-wrap" v-else>
    <text class="loading-icon">⏳</text>
    <text class="loading-text">正在同步服务详情...</text>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '../../../utils/request'
import api from '../../../config/api'

const aftersaleId = ref(0)
const detail = ref(null)

onLoad((options) => {
  if (options.id) {
    aftersaleId.value = parseInt(options.id)
  }
})

const getAftersaleDetail = async () => {
  try {
    const data = await request({
      url: `${api.AftersaleDetail}?id=${aftersaleId.value}`,
      method: 'GET'
    })
    if (data) {
      detail.value = data
    }
  } catch (err) {
    console.error('Failed to get aftersale detail:', err)
  }
}

const getStatusText = (status) => {
  const map = {
    0: '审核中',
    1: '退款中',
    2: '退款成功',
    3: '已拒绝',
    4: '已取消'
  }
  return map[status] || '处理中'
}

onMounted(() => {
  if (aftersaleId.value) {
    getAftersaleDetail()
  }
})
</script>

<style scoped>
.container {
  min-height: 100vh;
  background-color: var(--bg-primary);
  display: flex;
  flex-direction: column;
}

.detail-scroll {
  flex: 1;
}

/* Status Banner */
.status-banner {
  height: 240rpx;
  padding: 48rpx;
  display: flex;
  flex-direction: column;
  color: #ffffff;
  box-sizing: border-box;
}

.bg-0 { background: var(--primary-gradient); }
.bg-1 { background: var(--primary-gradient); }
.bg-2 { background: var(--secondary-gradient); }
.bg-3 { background: var(--accent-gradient); }

.status-text {
  font-size: 34rpx;
  font-weight: 800;
  margin-bottom: 8rpx;
}

.status-desc {
  font-size: 20rpx;
  font-weight: 600;
  opacity: 0.9;
}

/* Cost card */
.info-card {
  margin: -40rpx 32rpx 32rpx 32rpx;
  border-radius: var(--radius-md);
  padding: 32rpx;
  box-shadow: var(--shadow-md);
  border: 1px solid var(--border-light);
  position: relative;
  z-index: 10;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: 24rpx;
}

.label {
  color: var(--text-muted);
  font-weight: 600;
}

.value {
  color: var(--text-main);
  font-weight: 700;
}

.text-red {
  color: #ef4444;
}

/* Metadata card */
.meta-card {
  margin: 0 32rpx 32rpx 32rpx;
  border-radius: var(--radius-md);
  padding: 32rpx;
  border: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.meta-row {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.meta-label {
  font-size: 20rpx;
  color: var(--text-muted);
  font-weight: 600;
}

.meta-val {
  font-size: 24rpx;
  color: var(--text-main);
  font-weight: 600;
}

.text-indigo {
  color: #4f46e5;
}

.footer-spacer {
  height: 120rpx;
}

/* Loading Wrap */
.loading-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
}

.loading-icon {
  font-size: 60rpx;
  margin-bottom: 16rpx;
  animation: spinner 1.5s infinite linear;
}

.loading-text {
  font-size: 24rpx;
  color: var(--text-muted);
  font-weight: 500;
}

@keyframes spinner {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>
