<template>
  <view class="container">
    <scroll-view 
      scroll-y 
      class="groupon-scroll"
      @scrolltolower="onReachBottom"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="groupon-list" v-if="grouponList.length > 0">
        <view 
          class="groupon-card glass-panel" 
          v-for="item in grouponList" 
          :key="item.id"
          @tap="onGoodsTap(item.id)"
        >
          <image :src="item.picUrl" mode="aspectFill" class="goods-img" />
          <view class="goods-info">
            <text class="goods-name">{{ item.name }}</text>
            <text class="goods-brief">{{ item.brief }}</text>
            
            <view class="metrics-row">
              <text class="tag-groupon">{{ item.grouponMember }} 人成团</text>
              <text class="tag-discount">立省 ¥{{ item.counterPrice - item.grouponPrice }}</text>
            </view>

            <view class="price-action-row">
              <view class="price-block">
                <text class="currency">¥</text>
                <text class="groupon-price">{{ item.grouponPrice }}</text>
                <text class="counter-price">¥{{ item.counterPrice }}</text>
              </view>
              <button class="buy-btn">去拼团</button>
            </view>
          </view>
        </view>
      </view>

      <view class="empty-groupon" v-else-if="!loading">
        <text class="empty-icon">👥</text>
        <text class="empty-title">暂无团购活动</text>
        <text class="empty-desc">敬请期待我们下一期的拼团好物吧</text>
      </view>

      <view class="feed-state">
        <text class="state-text" v-if="loading">正在同步拼团宝贝...</text>
        <text class="state-text" v-else-if="finished">已同步全部拼团商品</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import api from '../../config/api'

const grouponList = ref([])
const page = ref(1)
const limit = ref(10)
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)

const getGroupons = async (reset = false) => {
  if (loading.value) return
  if (reset) {
    page.value = 1
    finished.value = false
  }
  if (finished.value) return

  loading.value = true
  try {
    const data = await request({
      url: `${api.GroupOnList}?page=${page.value}&limit=${limit.value}`,
      method: 'GET'
    })
    
    if (data) {
      const list = data.list || []
      if (reset) {
        grouponList.value = list
      } else {
        grouponList.value = [...grouponList.value, ...list]
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
    console.error('Failed to get groupon list:', err)
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

const onReachBottom = () => {
  getGroupons()
}

const onRefresh = () => {
  refreshing.value = true
  getGroupons(true)
}

const onGoodsTap = (id) => {
  uni.navigateTo({
    url: `/pages/goods/goods?id=${id}`
  })
}

onMounted(() => {
  getGroupons(true)
})
</script>

<style scoped>
.container {
  height: 100vh;
  background-color: var(--bg-primary);
}

.groupon-scroll {
  height: 100%;
}

.groupon-list {
  padding: 32rpx;
  display: flex;
  flex-direction: column;
  gap: 32rpx;
}

.groupon-card {
  display: flex;
  padding: 24rpx;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
  gap: 24rpx;
  transition: transform 0.2s ease;
}

.groupon-card:active {
  transform: scale(0.98);
}

.goods-img {
  width: 180rpx;
  height: 180rpx;
  border-radius: var(--radius-sm);
  background-color: #f1f5f9;
}

.goods-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.goods-name {
  font-size: 26rpx;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 4rpx;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.3;
}

.goods-brief {
  font-size: 20rpx;
  color: var(--text-muted);
  font-weight: 500;
  margin-bottom: 12rpx;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.metrics-row {
  display: flex;
  gap: 12rpx;
  margin-bottom: auto;
}

.tag-groupon {
  padding: 2rpx 12rpx;
  background-color: rgba(224, 122, 95, 0.08);
  color: #E07A5F;
  font-size: 16rpx;
  font-weight: 700;
  border-radius: 4rpx;
}

.tag-discount {
  padding: 2rpx 12rpx;
  background-color: rgba(224, 122, 95, 0.12);
  color: #D86B4F;
  font-size: 16rpx;
  font-weight: 700;
  border-radius: 4rpx;
}

.price-action-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12rpx;
}

.price-block {
  display: flex;
  align-items: baseline;
  gap: 4rpx;
}

.currency {
  font-size: 20rpx;
  font-weight: 700;
  color: #ef4444;
}

.groupon-price {
  font-size: 32rpx;
  font-weight: 800;
  color: #ef4444;
}

.counter-price {
  font-size: 20rpx;
  color: var(--text-muted);
  text-decoration: line-through;
  font-weight: 500;
  margin-left: 6rpx;
}

.buy-btn {
  height: 56rpx;
  padding: 0 28rpx;
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

/* Empty State */
.empty-groupon {
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
