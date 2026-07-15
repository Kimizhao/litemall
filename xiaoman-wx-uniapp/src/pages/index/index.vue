<template>
  <view class="container">
    <!-- 1. 顶部店铺卡片区域 -->
    <view class="shop-header">
      <view class="shop-header-bg"></view>
      <view class="shop-header-content">
        <!-- <view class="shop-avatar">满</view> -->
        <view class="shop-info">
          <!-- <text class="shop-name">小满食光</text> -->
          <text class="shop-subtitle">三明治 · 简餐 · 饮品</text>
        </view>
      </view>
    </view>

    <!-- 2. 滚动公告栏 -->
    <view class="announcement-bar" v-if="announcement">
      <text class="announcement-icon">🎉</text>
      <view class="announcement-text-wrap">
        <view class="announcement-text">{{ announcement }}</view>
      </view>
    </view>

    <!-- 3. 主体内容区 -->
    <view class="main-content">
      <!-- 3.1 预订点餐大按钮卡片 -->
      <view class="order-hero-card" @tap="onOrderTap">
        <view class="order-hero-bg"></view>
        <view class="order-hero-overlay"></view>
        <view class="order-hero-content">
          <text class="order-hero-title">预订点餐</text>
          <view class="order-arrow-btn">➔</view>
        </view>
      </view>

      <!-- 3.2 门店信息模块 -->
      <view class="info-card">
        <view class="info-card-title">门店信息</view>
        
        <!-- <view class="info-row">
          <text class="info-icon">🕒</text>
          <view class="info-text-wrap">
            <text class="info-label">营业时间</text>
            <text class="info-value">每天 08:00 - 21:00</text>
          </view>
        </view> -->

        <view class="info-row" @tap="onAddressTap">
          <text class="info-icon">📍</text>
          <div class="info-text-wrap">
            <!-- <text class="info-label">导航地址</text> -->
            <text class="info-value text-link">福州市仓山区公园路43号小满食光</text>
          </div>
          <text class="arrow-right">›</text>
        </view>

        <view class="info-row" @tap="onMakeCall">
          <text class="info-icon">📞</text>
          <div class="info-text-wrap">
            <!-- <text class="info-label">联系电话</text> -->
            <text class="info-value text-link">182-5903-3410</text>
          </div>
          <view class="btn-call">呼叫</view>
        </view>
      </view>

      <!-- 3.3 添加微信二维码 -->
      <view class="owner-card">
        <view class="owner-card-title">添加微信</view>
        <view class="owner-card-body">
          <view class="owner-info">
            <text class="owner-desc">扫码或长按保存二维码添加。</text>
          </view>
          <view class="owner-qr-wrapper" @tap="onPreviewQR">
            <image src="/static/xiaoman/contact_qr.png?v=20260714_cropped" :show-menu-by-longpress="true" mode="aspectFit" class="owner-qr-img" />
          </view>
        </view>
      </view>

      <!-- 3.4 关于我们介绍 -->
      <!-- <view class="about-card">
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
      </view> -->
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import request from '../../utils/request'
import api from '../../config/api'

const announcement = ref('')

// 获取首页数据
const loadHomeData = () => {
  request({
    url: api.IndexUrl,
    method: 'GET'
  }).then(res => {
    if (res && res.announcement) {
      announcement.value = res.announcement
    } else {
      announcement.value = ''
    }
  }).catch(err => {
    console.error('获取首页数据失败:', err)
  })
}

onShow(() => {
  loadHomeData()
})

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

// 地址点击导航并复制
const onAddressTap = () => {
  uni.setClipboardData({
    data: '福州市仓山区公园路43号小满食光',
    showToast: false,
    success: () => {
      // Show copy success toast immediately
      uni.showToast({
        title: '地址已复制',
        icon: 'none',
        duration: 1500
      });
      
      // Delay map opening by 1 second to allow toast visibility
      // setTimeout(() => {
      //   // #ifdef H5
      //   window.location.href = 'https://apis.map.qq.com/uri/v1/marker?marker=coord:26.0438,119.3144;title:小满食光;addr:福州市仓山区公园路43号&referer=litemall';
      //   // #endif
        
      //   // #ifndef H5
      //   uni.openLocation({
      //     latitude: 26.0438,
      //     longitude: 119.3144,
      //     name: '小满食光',
      //     address: '福州市仓山区公园路43号',
      //     scale: 18,
      //     success: () => {
      //       console.log('Location opened successfully');
      //     },
      //     fail: (err) => {
      //       console.error('Failed to open location:', err);
      //       uni.showToast({
      //         title: '已复制地址，期待您的光临！',
      //         icon: 'none'
      //       });
      //     }
      //   });
      //   // #endif
      // }, 1000);
    }
  });
}

// 预览主理人微信二维码（支持长按识别）
const onPreviewQR = () => {
  uni.previewImage({
    urls: ['/static/xiaoman/contact_qr.png?v=20260714_cropped'],
    current: '/static/xiaoman/contact_qr.png?v=20260714_cropped'
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
  background-position: center top;
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
  height: 160rpx;
  color: #FFFFFF;
  border-radius: 36rpx;
  overflow: hidden;
  box-shadow: 0 12rpx 36rpx rgba(111, 78, 55, 0.15);
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  padding: 0 48rpx;
}

.order-hero-card:active {
  transform: scale(0.98);
  box-shadow: 0 6rpx 20rpx rgba(111, 78, 55, 0.1);
}

.order-hero-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image: url('/static/xiaoman/sandwich_4.jpg');
  background-size: cover;
  background-position: center;
  z-index: 1;
}

.order-hero-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(to right, rgba(74, 53, 37, 0.8), rgba(74, 53, 37, 0.35));
  z-index: 2;
}

.order-hero-content {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
  z-index: 3;
  width: 100%;
}

.order-hero-title {
  font-size: 48rpx;
  font-weight: 900;
  letter-spacing: 2rpx;
  text-shadow: 0 2rpx 8rpx rgba(0,0,0,0.3);
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
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.15);
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

/* 3.3 主理人微信二维码 */
.owner-card {
  background-color: #FFFFFF;
  border-radius: 32rpx;
  padding: 36rpx;
  box-shadow: 0 6rpx 20rpx rgba(111, 78, 55, 0.04);
  border: 1rpx solid rgba(230, 213, 184, 0.2);
}

.owner-card-title {
  font-size: 32rpx;
  font-weight: 800;
  color: #6F4E37;
  margin-bottom: 24rpx;
  padding-left: 12rpx;
  border-left: 6rpx solid #6F4E37;
}

.owner-card-body {
  display: flex;
  align-items: center;
  gap: 30rpx;
}

.owner-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.owner-desc {
  font-size: 24rpx;
  color: #A18276;
  line-height: 1.5;
  font-weight: 600;
}

.owner-wechat-id {
  display: flex;
  align-items: center;
  font-size: 26rpx;
  font-weight: bold;
}

.wechat-label {
  color: #A18276;
}

.wechat-value {
  color: #4A3525;
  background-color: #FAF6EE;
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
  border: 1rpx dashed rgba(111, 78, 55, 0.3);
}

.owner-qr-wrapper {
  width: 160rpx;
  height: 160rpx;
  border-radius: 16rpx;
  overflow: hidden;
  border: 2rpx solid #FAF6EE;
  box-shadow: 0 4rpx 12rpx rgba(111, 78, 55, 0.08);
  flex-shrink: 0;
  background-color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
}

.owner-qr-img {
  width: 150rpx;
  height: 150rpx;
}

/* 3.4 关于我们介绍 */
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
