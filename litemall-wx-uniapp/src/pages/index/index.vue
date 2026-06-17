<template>
  <view class="container">
    <!-- Top Search Bar -->
    <view class="search-header glass-panel">
      <view class="search-box">
        <text class="search-icon">🔍</text>
        <input type="text" placeholder="搜索商品、品牌、类目..." disabled placeholder-style="color: #94a3b8;" />
      </view>
    </view>

    <!-- Content Scroller -->
    <scroll-view scroll-y class="scroll-content">
      <!-- Banner Carousel -->
      <view class="banner-wrapper">
        <swiper class="banner-swiper" circular autoplay interval="4000" indicator-dots indicator-color="rgba(255,255,255,0.4)" indicator-active-color="#ffffff">
          <swiper-item v-for="item in banner" :key="item.id">
            <image :src="item.url" mode="aspectFill" class="banner-image" />
          </swiper-item>
          <swiper-item v-if="banner.length === 0">
            <view class="banner-placeholder">
              <text class="ph-title">LITEMALL 商城</text>
              <text class="ph-subtitle">正在加载精选好物...</text>
            </view>
          </swiper-item>
        </swiper>
      </view>

      <!-- Category Quick Links -->
      <view class="category-grid glass-panel">
        <view class="category-item" v-for="item in channel" :key="item.id" @tap="onChannelTap(item)">
          <view class="icon-circle">
            <image :src="item.iconUrl" mode="aspectFit" class="category-icon" />
          </view>
          <text class="category-label">{{ item.name }}</text>
        </view>
        <view class="category-item" v-if="channel.length === 0" v-for="i in 4" :key="i">
          <view class="icon-circle placeholder-circle"></view>
          <text class="category-label loading-text">加载中</text>
        </view>
      </view>

      <!-- Hot Goods Section -->
      <view class="section-container" v-if="hotGoods.length > 0">
        <view class="section-header">
          <view class="section-title-wrap">
            <text class="section-indicator"></text>
            <text class="section-title">人气推荐</text>
          </view>
          <text class="see-more">查看全部 →</text>
        </view>

        <view class="hot-list">
          <view class="hot-card" v-for="item in hotGoods" :key="item.id" @tap="onGoodsTap(item)">
            <image :src="item.picUrl" mode="aspectFill" class="hot-image" />
            <view class="hot-info">
              <text class="goods-name">{{ item.name }}</text>
              <text class="goods-brief">{{ item.brief }}</text>
              <view class="goods-price-wrap">
                <text class="price-symbol">¥</text>
                <text class="goods-price">{{ item.retailPrice }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- New Arrivals Section -->
      <view class="section-container" v-if="newGoods.length > 0">
        <view class="section-header">
          <view class="section-title-wrap">
            <text class="section-indicator accent-indicator"></text>
            <text class="section-title">新品首发</text>
          </view>
          <text class="see-more">查看全部 →</text>
        </view>

        <view class="goods-grid">
          <view class="goods-card" v-for="item in newGoods" :key="item.id" @tap="onGoodsTap(item)">
            <image :src="item.picUrl" mode="aspectFill" class="goods-image" />
            <view class="goods-info">
              <text class="goods-name">{{ item.name }}</text>
              <view class="price-row">
                <view class="price-wrap">
                  <text class="price-symbol">¥</text>
                  <text class="goods-price">{{ item.retailPrice }}</text>
                </view>
                <view class="add-btn">+</view>
              </view>
            </view>
          </view>
        </view>
      </view>
      
      <view class="footer-spacer"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import request from '../../utils/request'
import api from '../../config/api'

// Core state variables
const banner = ref([])
const channel = ref([])
const newGoods = ref([])
const hotGoods = ref([])

const loadHomeData = async () => {
  try {
    const data = await request({
      url: api.IndexUrl,
      method: 'GET'
    })
    if (data) {
      banner.value = data.banner || []
      channel.value = data.channel || []
      newGoods.value = data.newGoodsList || []
      hotGoods.value = data.hotGoodsList || []
    }
  } catch (err) {
    console.error('Failed to load Litemall home details:', err)
  }
}

onMounted(() => {
  loadHomeData()
})

onLoad(() => {
  uni.showShareMenu({
    withShareTicket: true
  })
})

onPullDownRefresh(async () => {
  await loadHomeData()
  uni.stopPullDownRefresh()
})

const onChannelTap = (item) => {
  uni.navigateTo({
    url: `/pages/category/category?id=${item.id}`
  })
}

const onGoodsTap = (item) => {
  uni.navigateTo({
    url: `/pages/goods/goods?id=${item.id}`
  })
}
</script>

<style scoped>
.container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: var(--bg-primary);
}

.search-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 110rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 32rpx;
  z-index: 100;
  border-bottom: 1rpx solid rgba(226,232,240,0.5);
}

.search-box {
  width: 100%;
  height: 76rpx;
  border-radius: 38rpx;
  background: rgba(241, 245, 249, 0.8);
  display: flex;
  align-items: center;
  padding: 0 32rpx;
}

.search-icon {
  margin-right: 16rpx;
  font-size: 28rpx;
}

.search-box input {
  font-size: 26rpx;
  color: var(--text-main);
  width: 100%;
}

.scroll-content {
  flex: 1;
  margin-top: 110rpx;
}

/* Banner swiper styles */
.banner-wrapper {
  padding: 24rpx 32rpx;
}

.banner-swiper {
  width: 100%;
  height: 340rpx;
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-md);
}

.banner-image {
  width: 100%;
  height: 100%;
}

.banner-placeholder {
  width: 100%;
  height: 100%;
  background: var(--primary-gradient);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.ph-title {
  font-size: 44rpx;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 4rpx;
  margin-bottom: 8rpx;
}

.ph-subtitle {
  font-size: 24rpx;
  color: rgba(255,255,255,0.8);
}

/* Category Grid */
.category-grid {
  margin: 0 32rpx 32rpx 32rpx;
  border-radius: var(--radius-md);
  padding: 32rpx 16rpx;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20rpx 0;
}

.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.icon-circle {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: rgba(59, 130, 246, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12rpx;
  transition: transform 0.2s ease;
}

.category-item:active .icon-circle {
  transform: scale(0.9);
}

.category-icon {
  width: 52rpx;
  height: 52rpx;
}

.category-label {
  font-size: 24rpx;
  color: var(--text-muted);
  font-weight: 500;
}

.placeholder-circle {
  background: rgba(148, 163, 184, 0.15);
  animation: pulse 1.5s infinite ease-in-out;
}

.loading-text {
  color: rgba(148, 163, 184, 0.5);
}

/* Common Section styles */
.section-container {
  margin-bottom: 40rpx;
}

.section-header {
  padding: 0 32rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.section-title-wrap {
  display: flex;
  align-items: center;
}

.section-indicator {
  width: 8rpx;
  height: 36rpx;
  border-radius: 4rpx;
  background: var(--primary-gradient);
  margin-right: 16rpx;
}

.accent-indicator {
  background: var(--accent-gradient);
}

.section-title {
  font-size: 34rpx;
  font-weight: 700;
  color: var(--text-main);
  letter-spacing: -0.5rpx;
}

.see-more {
  font-size: 24rpx;
  color: #4f46e5;
  font-weight: 600;
}

/* Hot Sale items */
.hot-list {
  padding: 0 32rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.hot-card {
  display: flex;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
}

.hot-image {
  width: 220rpx;
  height: 220rpx;
  background: #f1f5f9;
}

.hot-info {
  flex: 1;
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.goods-name {
  font-size: 28rpx;
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 8rpx;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.goods-brief {
  font-size: 24rpx;
  color: var(--text-muted);
  margin-bottom: 16rpx;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.goods-price-wrap {
  display: flex;
  align-items: baseline;
}

.price-symbol {
  font-size: 24rpx;
  font-weight: 700;
  color: #ef4444;
  margin-right: 2rpx;
}

.goods-price {
  font-size: 36rpx;
  font-weight: 700;
  color: #ef4444;
}

/* New Arrivals grid */
.goods-grid {
  padding: 0 32rpx;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24rpx;
}

.goods-card {
  background: var(--bg-secondary);
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
  background: #f1f5f9;
}

.goods-card .goods-info {
  padding: 20rpx;
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12rpx;
}

.price-wrap {
  display: flex;
  align-items: baseline;
}

.add-btn {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: var(--primary-gradient);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: bold;
}

.footer-spacer {
  height: 48rpx;
}

@keyframes pulse {
  0%, 100% {
    opacity: 0.6;
  }
  50% {
    opacity: 1;
  }
}
</style>
