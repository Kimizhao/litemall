<template>
  <view class="container">
    <!-- 1. 顶部店铺卡片区域 -->
    <view class="shop-header">
      <view class="shop-header-bg"></view>
      <view class="shop-header-content">
        <view class="shop-avatar">满</view>
        <view class="shop-info">
          <text class="shop-name">小满食光手作</text>
          <text class="shop-subtitle">三明治 · 手作简餐 · 现榨饮品</text>
        </view>
      </view>
    </view>

    <!-- 2. 试营业滚动公告栏 -->
    <view class="announcement-bar">
      <text class="announcement-icon">🎉</text>
      <view class="announcement-text-wrap">
        <view class="announcement-text">新店试营业，全场 8.8 折！微信扫码免费领饭团！</view>
      </view>
    </view>

    <!-- 3. 主体内容区 -->
    <view class="main-content">
      <!-- 3.1 预订点餐大按钮卡片 -->
      <view class="order-hero-card" @tap="onOrderTap">
        <view class="order-hero-overlay"></view>
        <view class="order-hero-content">
          <view class="order-hero-left">
            <text class="order-hero-title">预订点餐</text>
            <text class="order-hero-desc">预约自取 / 外卖配送</text>
            <view class="order-badge">提前下单不用等</view>
          </view>
          <view class="order-hero-right">
            <text class="order-hero-icon">🥪</text>
            <view class="order-arrow-btn">➔</view>
          </view>
        </view>
      </view>

      <!-- 3.2 门店信息模块 -->
      <view class="info-card">
        <view class="info-card-title">门店信息</view>
        
        <view class="info-row">
          <text class="info-icon">🕒</text>
          <view class="info-text-wrap">
            <text class="info-label">营业时间</text>
            <text class="info-value">每天 08:00 - 21:00</text>
          </view>
        </view>

        <view class="info-row" @tap="onAddressTap">
          <text class="info-icon">📍</text>
          <div class="info-text-wrap">
            <text class="info-label">门店地址</text>
            <text class="info-value text-link">上海市浦东新区小满食光手作店</text>
          </div>
          <text class="arrow-right">›</text>
        </view>

        <view class="info-row" @tap="onMakeCall">
          <text class="info-icon">📞</text>
          <div class="info-text-wrap">
            <text class="info-label">联系电话</text>
            <text class="info-value text-link">138-0000-0000</text>
          </div>
          <view class="btn-call">呼叫</view>
        </view>
      </view>

      <!-- 3.3 关于我们介绍 -->
      <view class="about-card">
        <view class="about-header">
          <text class="about-title">关于小满食光</text>
          <text class="about-icon">🌱</text>
        </view>
        <text class="about-text">
          精选天然食材，坚持每日手作。每一个热乎乎的三明治、每一份清爽的果汁，都承载着我们对健康生活的向往和对美食的热爱。在这里，静享美食，不负时光。
        </text>
        
        <view class="featured-gallery">
          <view class="gallery-item">
            <text class="gallery-icon">🥪</text>
            <text class="gallery-name">招牌三明治</text>
          </view>
          <view class="gallery-item">
            <text class="gallery-icon">🍙</text>
            <text class="gallery-name">手作简餐</text>
          </view>
          <view class="gallery-item">
            <text class="gallery-icon">🍹</text>
            <text class="gallery-name">现榨饮品</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { onShow } from '@dcloudio/uni-app'

// 点击预订点餐跳转到“点单” tab
const onOrderTap = () => {
  uni.switchTab({
    url: '/pages/catalog/catalog'
  })
}

// 拨打门店电话
const onMakeCall = () => {
  uni.makePhoneCall({
    phoneNumber: '13800000000'
  })
}

// 地址点击提示
const onAddressTap = () => {
  uni.showToast({
    title: '已复制地址，期待您的光临！',
    icon: 'none'
  })
}
</script>

<style scoped>
.container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #FAF6EE;
  color: #4A3525;
}

/* 1. 店铺头部区域 */
.shop-header {
  position: relative;
  height: 260rpx;
  display: flex;
  align-items: flex-end;
  padding: 40rpx;
  overflow: hidden;
}

.shop-header-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image: linear-gradient(to bottom, rgba(74, 53, 37, 0.1), rgba(74, 53, 37, 0.8)), 
                    url('/static/xiaoman/shop_header.jpg');
  background-size: cover;
  background-position: center;
  z-index: 1;
}

.shop-header-content {
  position: relative;
  display: flex;
  align-items: center;
  gap: 24rpx;
  z-index: 2;
  color: #FFFFFF;
}

.shop-avatar {
  width: 110rpx;
  height: 110rpx;
  border-radius: 50%;
  background-color: #FFFFFF;
  color: #6F4E37;
  font-size: 56rpx;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 4rpx solid #E6D5B8;
  box-shadow: 0 6rpx 20rpx rgba(111, 78, 55, 0.25);
}

.shop-name {
  font-size: 42rpx;
  font-weight: 800;
  letter-spacing: 2rpx;
  text-shadow: 0 2rpx 6rpx rgba(0,0,0,0.5);
}

.shop-subtitle {
  font-size: 26rpx;
  opacity: 0.95;
  margin-top: 6rpx;
  text-shadow: 0 2rpx 4rpx rgba(0,0,0,0.4);
}

/* 2. 试营业滚动公告栏 */
.announcement-bar {
  background-color: #FFEFC6;
  color: #8C6430;
  padding: 12rpx 30rpx;
  font-size: 24rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 12rpx;
  border-bottom: 1rpx solid #E6D5B8;
  z-index: 10;
}

.announcement-text-wrap {
  flex: 1;
  overflow: hidden;
}

.announcement-text {
  white-space: nowrap;
  animation: marquee 18s linear infinite;
  display: inline-block;
}

@keyframes marquee {
  0% { transform: translateX(100%); }
  100% { transform: translateX(-100%); }
}

/* 3. 主体内容区 */
.main-content {
  padding: 40rpx;
  display: flex;
  flex-direction: column;
  gap: 36rpx;
}

/* 3.1 预订点餐大按钮卡片 */
.order-hero-card {
  position: relative;
  background-color: #6F4E37;
  color: #FFFFFF;
  border-radius: 36rpx;
  padding: 48rpx;
  overflow: hidden;
  box-shadow: 0 12rpx 36rpx rgba(111, 78, 55, 0.2);
  transition: all 0.2s ease;
}

.order-hero-card:active {
  transform: scale(0.98);
  box-shadow: 0 6rpx 20rpx rgba(111, 78, 55, 0.15);
}

.order-hero-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle at right bottom, rgba(224, 122, 95, 0.4), transparent 70%);
  z-index: 1;
}

.order-hero-content {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
  z-index: 2;
}

.order-hero-left {
  display: flex;
  flex-direction: column;
}

.order-hero-title {
  font-size: 52rpx;
  font-weight: 900;
  letter-spacing: 2rpx;
  text-shadow: 0 2rpx 4rpx rgba(0,0,0,0.15);
}

.order-hero-desc {
  font-size: 28rpx;
  opacity: 0.9;
  margin-top: 10rpx;
}

.order-badge {
  align-self: flex-start;
  margin-top: 24rpx;
  background-color: #E07A5F;
  font-size: 22rpx;
  font-weight: bold;
  padding: 6rpx 18rpx;
  border-radius: 100rpx;
  box-shadow: 0 4rpx 12rpx rgba(224, 122, 95, 0.3);
}

.order-hero-right {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16rpx;
}

.order-hero-icon {
  font-size: 80rpx;
  filter: drop-shadow(0 4rpx 8rpx rgba(0,0,0,0.1));
}

.order-arrow-btn {
  width: 60rpx;
  height: 60rpx;
  background-color: #FFFFFF;
  color: #6F4E37;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 28rpx;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.1);
}

/* 3.2 门店信息模块 */
.info-card {
  background-color: #FFFFFF;
  border-radius: 32rpx;
  padding: 36rpx;
  box-shadow: 0 6rpx 20rpx rgba(111, 78, 55, 0.04);
  border: 1rpx solid rgba(230, 213, 184, 0.2);
}

.info-card-title {
  font-size: 32rpx;
  font-weight: 800;
  color: #6F4E37;
  margin-bottom: 30rpx;
  padding-left: 12rpx;
  border-left: 6rpx solid #6F4E37;
}

.info-row {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid rgba(230, 213, 184, 0.15);
}

.info-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.info-row:first-child {
  padding-top: 0;
}

.info-icon {
  font-size: 38rpx;
  margin-right: 24rpx;
  width: 44rpx;
  text-align: center;
}

.info-text-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.info-label {
  font-size: 22rpx;
  color: #A18276;
  font-weight: 600;
}

.info-value {
  font-size: 28rpx;
  font-weight: 700;
  color: #4A3525;
}

.text-link {
  color: #6F4E37;
}

.arrow-right {
  font-size: 36rpx;
  color: #A18276;
}

.btn-call {
  font-size: 24rpx;
  font-weight: bold;
  color: #E07A5F;
  border: 2rpx solid #E07A5F;
  padding: 8rpx 20rpx;
  border-radius: 100rpx;
  transition: all 0.2s ease;
}

.btn-call:active {
  background-color: rgba(224, 122, 95, 0.1);
  transform: scale(0.95);
}

/* 3.3 关于我们介绍 */
.about-card {
  background-color: #FFFFFF;
  border-radius: 32rpx;
  padding: 36rpx;
  box-shadow: 0 6rpx 20rpx rgba(111, 78, 55, 0.04);
  border: 1rpx solid rgba(230, 213, 184, 0.2);
}

.about-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.about-title {
  font-size: 32rpx;
  font-weight: 800;
  color: #6F4E37;
}

.about-icon {
  font-size: 38rpx;
}

.about-text {
  font-size: 26rpx;
  color: #6F594B;
  line-height: 1.6;
}

.featured-gallery {
  display: flex;
  justify-content: space-between;
  margin-top: 36rpx;
  gap: 20rpx;
}

.gallery-item {
  flex: 1;
  background-color: #FAF6EE;
  border-radius: 20rpx;
  padding: 24rpx 12rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
  border: 1rpx solid rgba(230, 213, 184, 0.15);
}

.gallery-icon {
  font-size: 50rpx;
}

.gallery-name {
  font-size: 22rpx;
  font-weight: bold;
  color: #6F4E37;
}
</style>
