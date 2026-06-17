<template>
  <view class="container">
    <scroll-view 
      scroll-y 
      class="list-scroll"
      @scrolltolower="onReachBottom"
    >
      <view class="list-feed" v-if="aftersaleList.length > 0">
        <view 
          class="aftersale-card glass-panel" 
          v-for="item in aftersaleList" 
          :key="item.id"
          @tap="onAftersaleDetail(item.id)"
        >
          <!-- Card Header status -->
          <view class="card-header">
            <text class="apply-time">申请时间: {{ item.addTime }}</text>
            <text class="apply-status" :class="'status-' + item.status">{{ getStatusText(item.status) }}</text>
          </view>

          <!-- Refund cost -->
          <view class="refund-row">
            <text class="label">退款金额</text>
            <text class="value">¥{{ item.amount }}</text>
          </view>

          <!-- Refund explanation reason -->
          <view class="reason-row">
            <text class="label">退款原因</text>
            <text class="value">{{ item.reason }}</text>
          </view>
          
          <view class="divider"></view>
          <text class="detail-link">查看服务详情 →</text>
        </view>
      </view>

      <view class="empty-list" v-else-if="!loading">
        <text class="empty-icon">🛡️</text>
        <text class="empty-title">暂无售后申请记录</text>
        <text class="empty-desc">手作宝贝全程保障，有任何问题请联系客服</text>
      </view>

      <view class="feed-state">
        <text class="state-text" v-if="loading">正在同步售后单...</text>
        <text class="state-text" v-else-if="finished">已同步全部售后单</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../../utils/request'
import api from '../../../config/api'

const aftersaleList = ref([])
const page = ref(1)
const limit = ref(10)
const loading = ref(false)
const finished = ref(false)

const getAftersaleList = async (reset = false) => {
  if (loading.value) return
  if (reset) {
    page.value = 1
    finished.value = false
  }
  if (finished.value) return

  loading.value = true
  try {
    const data = await request({
      url: `${api.AftersaleList}?status=0&page=${page.value}&limit=${limit.value}`,
      method: 'GET'
    })
    
    if (data) {
      const list = data.list || []
      if (reset) {
        aftersaleList.value = list
      } else {
        aftersaleList.value = [...aftersaleList.value, ...list]
      }
      
      if (list.length < limit.value) {
        finished.value = true
      } else {
        page.value += 1
      }
    } else {
      finished.value = true
    }
  } catch (err) {
    console.error('Failed to get aftersale list:', err)
  } finally {
    loading.value = false
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

const onAftersaleDetail = (id) => {
  uni.navigateTo({
    url: `/pages/ucenter/aftersaleDetail/aftersaleDetail?id=${id}`
  })
}

const onReachBottom = () => {
  getAftersaleList()
}

onMounted(() => {
  getAftersaleList(true)
})
</script>

<style scoped>
.container {
  height: 100vh;
  background-color: var(--bg-primary);
}

.list-scroll {
  height: 100%;
}

.list-feed {
  padding: 32rpx;
  display: flex;
  flex-direction: column;
  gap: 28rpx;
}

.aftersale-card {
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
  padding: 32rpx;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16rpx;
  border-bottom: 1px solid var(--border-light);
}

.apply-time {
  font-size: 22rpx;
  color: var(--text-muted);
  font-weight: 500;
}

.apply-status {
  font-size: 24rpx;
  font-weight: 800;
}

.status-0 { color: #4f46e5; }
.status-1 { color: #3b82f6; }
.status-2 { color: #10b981; }
.status-3 { color: #ef4444; }

.refund-row, .reason-row {
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

.divider {
  height: 1px;
  background-color: var(--border-light);
}

.detail-link {
  font-size: 22rpx;
  color: #4f46e5;
  font-weight: 700;
  text-align: right;
}

/* Empty State */
.empty-list {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 60vh;
  padding: 0 64rpx;
}

.empty-icon {
  font-size: 100rpx;
  margin-bottom: 24rpx;
}

.empty-title {
  font-size: 32rpx;
  font-weight: 800;
  color: var(--text-main);
  margin-bottom: 8rpx;
}

.empty-desc {
  font-size: 24rpx;
  color: var(--text-muted);
  text-align: center;
}

.feed-state {
  text-align: center;
  padding: 32rpx 0 64rpx 0;
}

.state-text {
  font-size: 22rpx;
  color: var(--text-muted);
  font-weight: 600;
}
</style>
