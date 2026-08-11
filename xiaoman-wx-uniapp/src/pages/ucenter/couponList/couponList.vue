<template>
  <view class="container">
    <!-- Header Tabs -->
    <view class="tabs-header glass-panel">
      <view 
        class="tab-item" 
        v-for="tab in tabs" 
        :key="tab.status"
        :class="{ active: currentStatus === tab.status }"
        @tap="onStatusChange(tab.status)"
      >
        {{ tab.name }}
      </view>
    </view>

    <!-- Coupons Feed list -->
    <scroll-view 
      scroll-y 
      class="coupon-scroll"
      @scrolltolower="onReachBottom"
    >
      <view class="coupon-list" v-if="couponList.length > 0">
        <view 
          class="coupon-ticket glass-panel" 
          v-for="item in couponList" 
          :key="item.id"
          :class="{ 'coupon-inactive': currentStatus > 0 }"
        >
          <view class="ticket-left">
            <view class="value-wrap">
              <text class="currency">¥</text>
              <text class="value-num">{{ item.discount }}</text>
            </view>
            <text class="use-cond">满 {{ item.min }} 元可用</text>
          </view>
          <view class="ticket-right">
            <text class="coupon-name">{{ item.name }}</text>
            <text class="coupon-desc">{{ item.desc }}</text>
            <text class="coupon-time">有效期: {{ item.startTime }} 至 {{ item.endTime }}</text>
          </view>
        </view>
      </view>

      <view class="empty-coupons" v-else-if="hasLoaded && !loading">
        <text class="empty-icon">🎫</text>
        <text class="empty-title">无优惠券记录</text>
        <text class="empty-desc">赶紧去领券中心看看有无合适的宝贝券吧</text>
      </view>

      <view class="feed-state">
        <text class="state-text" v-if="loading || !hasLoaded">正在加载优惠券...</text>
        <text class="state-text" v-else-if="finished">已加载全部优惠券</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../../utils/request'
import api from '../../../config/api'

const tabs = [
  { name: '未使用', status: 0 },
  { name: '已使用', status: 1 },
  { name: '已过期', status: 2 }
]

const currentStatus = ref(0)
const couponList = ref([])
const page = ref(1)
const limit = ref(10)
const loading = ref(false)
const hasLoaded = ref(false)
const finished = ref(false)

const getMyCoupons = async (reset = false) => {
  if (loading.value) return
  if (reset) {
    page.value = 1
    finished.value = false
  }
  if (finished.value) return

  loading.value = true
  try {
    const data = await request({
      url: `${api.CouponMyList}?status=${currentStatus.value}&page=${page.value}&limit=${limit.value}`,
      method: 'GET'
    })
    
    if (data) {
      const list = data.list || []
      if (reset) {
        couponList.value = list
      } else {
        couponList.value = [...couponList.value, ...list]
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
    console.error('Failed to get my coupons:', err)
  } finally {
    loading.value = false
    hasLoaded.value = true
  }
}

const onStatusChange = (status) => {
  if (currentStatus.value === status) return
  currentStatus.value = status
  couponList.value = []
  hasLoaded.value = false
  getMyCoupons(true)
}

const onReachBottom = () => {
  getMyCoupons()
}

onMounted(() => {
  getMyCoupons(true)
})
</script>

<style scoped>
.container {
  height: 100vh;
  background-color: var(--bg-primary);
  display: flex;
  flex-direction: column;
}

.tabs-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  justify-content: space-around;
  height: 88rpx;
  min-height: 88rpx;
  flex-shrink: 0;
  align-items: center;
  border-bottom: 1px solid var(--border-light);
}

.tab-item {
  font-size: 26rpx;
  font-weight: 700;
  color: var(--text-muted);
  position: relative;
  height: 100%;
  display: flex;
  align-items: center;
}

.tab-item.active {
  color: #6F4E37;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 20%;
  right: 20%;
  height: 6rpx;
  background: var(--primary-gradient);
  border-radius: 3rpx;
}

.coupon-scroll {
  flex: 1;
}

.coupon-list {
  padding: 32rpx;
  display: flex;
  flex-direction: column;
  gap: 28rpx;
}

.coupon-ticket {
  display: flex;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  height: 200rpx;
}

.ticket-left {
  width: 200rpx;
  background: var(--primary-gradient);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  border-right: 2px dashed rgba(255, 255, 255, 0.4);
}

.value-wrap {
  display: flex;
  align-items: baseline;
}

.currency {
  font-size: 24rpx;
  font-weight: 700;
}

.value-num {
  font-size: 52rpx;
  font-weight: 800;
}

.use-cond {
  font-size: 18rpx;
  font-weight: 600;
  opacity: 0.9;
  margin-top: 4rpx;
}

.ticket-right {
  flex: 1;
  padding: 24rpx 28rpx;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.coupon-name {
  font-size: 26rpx;
  font-weight: 800;
  color: var(--text-main);
  margin-bottom: 6rpx;
}

.coupon-desc {
  font-size: 20rpx;
  color: var(--text-muted);
  font-weight: 500;
  margin-bottom: 12rpx;
}

.coupon-time {
  font-size: 16rpx;
  color: var(--text-muted);
  font-weight: 500;
}

/* Inactive coupons grayscale */
.coupon-inactive .ticket-left {
  background: #cbd5e1;
}

.coupon-inactive {
  opacity: 0.7;
}

/* Empty Coupons State */
.empty-coupons {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 60vh;
  padding: 0 64rpx;
}

.empty-icon {
  font-size: 100rpx;
  margin-bottom: 16rpx;
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
