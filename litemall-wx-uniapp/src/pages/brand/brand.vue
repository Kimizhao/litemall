<template>
  <view class="container">
    <scroll-view 
      scroll-y 
      class="brand-scroll"
      @scrolltolower="onReachBottom"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="brand-list">
        <view 
          class="brand-card glass-panel" 
          v-for="item in brandList" 
          :key="item.id"
          @tap="onBrandTap(item.id)"
        >
          <image :src="item.picUrl" mode="aspectFill" class="brand-image" />
          <view class="brand-info">
            <view class="brand-title-row">
              <text class="brand-name">{{ item.name }}</text>
              <text class="brand-price" v-if="item.floorPrice > 0">¥{{ item.floorPrice }}起</text>
            </view>
            <text class="brand-desc">{{ item.simpleDesc }}</text>
          </view>
        </view>
      </view>

      <view class="feed-state">
        <text class="state-text" v-if="loading">正在加载品牌...</text>
        <text class="state-text" v-else-if="finished">已加载全部品牌商</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import api from '../../config/api'

const brandList = ref([])
const page = ref(1)
const limit = ref(10)
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)

const getBrandList = async (reset = false) => {
  if (loading.value) return
  if (reset) {
    page.value = 1
    finished.value = false
  }
  if (finished.value) return

  loading.value = true
  try {
    const data = await request({
      url: `${api.BrandList}?page=${page.value}&limit=${limit.value}`,
      method: 'GET'
    })
    
    if (data) {
      const list = data.list || []
      if (reset) {
        brandList.value = list
      } else {
        brandList.value = [...brandList.value, ...list]
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
    console.error('Failed to get brand list:', err)
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

const onReachBottom = () => {
  getBrandList()
}

const onRefresh = () => {
  refreshing.value = true
  getBrandList(true)
}

const onBrandTap = (id) => {
  uni.navigateTo({
    url: `/pages/brandDetail/brandDetail?id=${id}`
  })
}

onMounted(() => {
  getBrandList(true)
})
</script>

<style scoped>
.container {
  height: 100vh;
  background-color: var(--bg-primary);
}

.brand-scroll {
  height: 100%;
}

.brand-list {
  padding: 32rpx;
  display: flex;
  flex-direction: column;
  gap: 32rpx;
}

.brand-card {
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
  transition: transform 0.2s ease;
}

.brand-card:active {
  transform: scale(0.98);
}

.brand-image {
  width: 100%;
  height: 360rpx;
  background-color: #f1f5f9;
}

.brand-info {
  padding: 32rpx;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.brand-title-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.brand-name {
  font-size: 30rpx;
  font-weight: 800;
  color: var(--text-main);
}

.brand-price {
  font-size: 26rpx;
  color: #ef4444;
  font-weight: 800;
}

.brand-desc {
  font-size: 24rpx;
  color: var(--text-muted);
  line-height: 1.4;
  font-weight: 500;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.feed-state {
  text-align: center;
  padding: 40rpx 0 80rpx 0;
}

.state-text {
  font-size: 22rpx;
  color: var(--text-muted);
  font-weight: 600;
}
</style>
