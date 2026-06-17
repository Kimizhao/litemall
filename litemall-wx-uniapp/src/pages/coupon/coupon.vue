<template>
  <view class="container">
    <scroll-view 
      scroll-y 
      class="coupon-scroll"
      @scrolltolower="onReachBottom"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="coupon-list" v-if="couponList.length > 0">
        <view class="coupon-ticket glass-panel" v-for="item in couponList" :key="item.id">
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
            <text class="coupon-time">有效期: {{ item.days ? '领券后 ' + item.days + ' 天内有效' : item.startTime + ' - ' + item.endTime }}</text>
            <button class="claim-btn" @tap="onClaimCoupon(item)">立即领取</button>
          </view>
        </view>
      </view>

      <view class="empty-coupons" v-else-if="!loading">
        <text class="empty-icon">🎫</text>
        <text class="empty-title">暂无可领取的优惠券</text>
      </view>

      <view class="feed-state">
        <text class="state-text" v-if="loading">正在加载优惠券...</text>
        <text class="state-text" v-else-if="finished">已加载全部优惠券</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import api from '../../config/api'

const couponList = ref([])
const page = ref(1)
const limit = ref(10)
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)

const getCoupons = async (reset = false) => {
  if (loading.value) return
  if (reset) {
    page.value = 1
    finished.value = false
  }
  if (finished.value) return

  loading.value = true
  try {
    const data = await request({
      url: `${api.CouponList}?page=${page.value}&limit=${limit.value}`,
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
    console.error('Failed to get coupon list:', err)
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

const onClaimCoupon = async (item) => {
  // Check login first
  const token = uni.getStorageSync('token')
  if (!token) {
    uni.navigateTo({
      url: '/pages/auth/login'
    })
    return
  }

  uni.showLoading({ title: '正在领券...' })
  try {
    await request({
      url: api.CouponReceive,
      method: 'POST',
      data: {
        couponId: item.id
      }
    })
    uni.hideLoading()
    uni.showToast({
      title: '领券成功！',
      icon: 'success'
    })
  } catch (err) {
    uni.hideLoading()
    console.error('Failed to claim coupon:', err)
  }
}

const onReachBottom = () => {
  getCoupons()
}

const onRefresh = () => {
  refreshing.value = true
  getCoupons(true)
}

onMounted(() => {
  getCoupons(true)
})
</script>

<style scoped>
.container {
  height: 100vh;
  background-color: var(--bg-primary);
}

.coupon-scroll {
  height: 100%;
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
  height: 220rpx;
}

.ticket-left {
  width: 220rpx;
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
  font-size: 56rpx;
  font-weight: 800;
}

.use-cond {
  font-size: 20rpx;
  font-weight: 600;
  opacity: 0.9;
  margin-top: 4rpx;
}

.ticket-right {
  flex: 1;
  padding: 24rpx 28rpx;
  display: flex;
  flex-direction: column;
  position: relative;
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

.claim-btn {
  position: absolute;
  right: 28rpx;
  top: 50%;
  transform: translateY(-50%);
  width: 140rpx;
  height: 56rpx;
  border-radius: 28rpx;
  background: var(--primary-gradient);
  color: #ffffff;
  font-size: 20rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 10px rgba(79, 70, 229, 0.2);
}

.claim-btn:active {
  opacity: 0.85;
}

/* Empty Coupons State */
.empty-coupons {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 60vh;
}

.empty-icon {
  font-size: 100rpx;
  margin-bottom: 16rpx;
}

.empty-title {
  font-size: 32rpx;
  font-weight: 800;
  color: var(--text-main);
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
