<template>
  <view class="container" v-if="brand">
    <scroll-view scroll-y class="detail-scroll">
      <!-- Creator Cover Header -->
      <image :src="brand.picUrl" mode="aspectFill" class="brand-banner" />

      <!-- Story Info Card -->
      <view class="story-card glass-panel">
        <text class="brand-title">{{ brand.name }}</text>
        <view class="divider"></view>
        <text class="brand-desc">{{ brand.desc }}</text>
      </view>

      <!-- Products Grid under Brand -->
      <view class="goods-grid" v-if="goodsList.length > 0">
        <view 
          class="goods-card glass-panel" 
          v-for="item in goodsList" 
          :key="item.id"
          @tap="onGoodsTap(item.id)"
        >
          <image :src="item.picUrl" mode="aspectFill" class="goods-image" />
          <view class="goods-info">
            <text class="goods-name">{{ item.name }}</text>
            <view class="price-row">
              <text class="price-symbol">¥</text>
              <text class="price-val">{{ item.retailPrice }}</text>
            </view>
          </view>
        </view>
      </view>
      <view class="empty-goods" v-else>
        <text class="empty-icon">🎨</text>
        <text class="empty-text">该品牌暂未发布手作宝贝</text>
      </view>

      <view class="footer-spacer"></view>
    </scroll-view>
  </view>

  <view class="loading-wrap" v-else>
    <text class="loading-icon">⏳</text>
    <text class="loading-text">正在同步设计师故事...</text>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onLoad, onShareAppMessage, onShareTimeline } from '@dcloudio/uni-app'
import request from '../../utils/request'
import api from '../../config/api'

const brandId = ref(0)
const brand = ref(null)
const goodsList = ref([])

onShareAppMessage((res) => {
  return {
    title: brand.value && brand.value.name ? `${brand.value.name} - 小满食光` : '小满食光品牌故事',
    path: `/pages/brandDetail/brandDetail?id=${brandId.value}`,
    imageUrl: brand.value && brand.value.picUrl ? brand.value.picUrl : ''
  }
})

onShareTimeline(() => {
  return {
    title: brand.value && brand.value.name ? `${brand.value.name} - 小满食光` : '小满食光品牌故事',
    query: `id=${brandId.value}`,
    imageUrl: brand.value && brand.value.picUrl ? brand.value.picUrl : ''
  }
})

onLoad((options) => {
  if (options.id) {
    brandId.value = parseInt(options.id)
  }
})

const getBrandDetail = async () => {
  try {
    const data = await request({
      url: `${api.BrandDetail}?id=${brandId.value}`,
      method: 'GET'
    })
    if (data) {
      brand.value = data.brand || null
    }
  } catch (err) {
    console.error('Failed to get brand detail:', err)
  }
}

const getGoodsList = async () => {
  try {
    const data = await request({
      url: `${api.GoodsList}?brandId=${brandId.value}&page=1&limit=20`,
      method: 'GET'
    })
    if (data) {
      goodsList.value = data.goodsList || []
    }
  } catch (err) {
    console.error('Failed to get brand goods:', err)
  }
}

const onGoodsTap = (id) => {
  uni.navigateTo({
    url: `/pages/goods/goods?id=${id}`
  })
}

onMounted(() => {
  if (brandId.value) {
    getBrandDetail()
    getGoodsList()
  }
})
</script>

<style scoped>
.container {
  height: 100vh;
  background-color: var(--bg-primary);
}

.detail-scroll {
  height: 100%;
}

.brand-banner {
  width: 100%;
  height: 480rpx;
  background-color: #f1f5f9;
}

/* Story Card */
.story-card {
  margin: -60rpx 32rpx 32rpx 32rpx;
  border-radius: var(--radius-md);
  padding: 40rpx 32rpx;
  position: relative;
  z-index: 10;
  box-shadow: var(--shadow-md);
  border: 1px solid var(--border-light);
}

.brand-title {
  font-size: 34rpx;
  font-weight: 800;
  color: var(--text-main);
}

.divider {
  height: 1px;
  background-color: var(--border-light);
  margin: 24rpx 0;
}

.brand-desc {
  font-size: 24rpx;
  color: var(--text-muted);
  line-height: 1.6;
  font-weight: 500;
}

/* Goods Grid */
.goods-grid {
  padding: 0 32rpx;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24rpx;
}

.goods-card {
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
}

.goods-image {
  width: 100%;
  height: 320rpx;
  background-color: #f1f5f9;
}

.goods-info {
  padding: 20rpx;
  display: flex;
  flex-direction: column;
}

.goods-name {
  font-size: 24rpx;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 12rpx;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  height: 64rpx;
  line-height: 1.3;
}

.price-row {
  display: flex;
  align-items: baseline;
}

.price-symbol {
  font-size: 20rpx;
  font-weight: 700;
  color: #ef4444;
}

.price-val {
  font-size: 28rpx;
  font-weight: 700;
  color: #ef4444;
}

.empty-goods {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 0;
}

.empty-icon {
  font-size: 60rpx;
  margin-bottom: 12rpx;
}

.empty-text {
  font-size: 24rpx;
  color: var(--text-muted);
  font-weight: 600;
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
