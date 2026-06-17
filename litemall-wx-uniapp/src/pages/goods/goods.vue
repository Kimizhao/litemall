<template>
  <view class="container" v-if="goods">
    <!-- Floating Top Back Button -->
    <view class="nav-back glass-panel" @tap="onBackTap">
      <text class="back-icon">←</text>
    </view>

    <!-- Scrollable Detail Contents -->
    <scroll-view scroll-y class="detail-scroll">
      <!-- Gallery Swiper -->
      <view class="gallery-wrapper">
        <swiper class="gallery-swiper" circular autoplay interval="5000" indicator-dots indicator-color="rgba(255,255,255,0.4)" indicator-active-color="#ffffff">
          <swiper-item v-for="(img, idx) in gallery" :key="idx">
            <image :src="img" mode="aspectFill" class="gallery-image" @tap="onPreviewImage(idx)" />
          </swiper-item>
          <swiper-item v-if="gallery.length === 0">
            <image :src="goods.picUrl" mode="aspectFill" class="gallery-image" />
          </swiper-item>
        </swiper>
      </view>

      <!-- Goods Core Info -->
      <view class="goods-info-card glass-panel">
        <view class="price-row">
          <view class="price-block">
            <text class="price-symbol">¥</text>
            <text class="price-retail">{{ goods.retailPrice }}</text>
            <text class="price-counter" v-if="goods.counterPrice > goods.retailPrice">¥{{ goods.counterPrice }}</text>
          </view>
          <view class="tag-hot" v-if="goods.isHot">人气推荐</view>
          <view class="tag-new" v-else-if="goods.isNew">新品首发</view>
        </view>
        <text class="goods-name">{{ goods.name }}</text>
        <text class="goods-brief">{{ goods.brief }}</text>
      </view>

      <!-- Selection Spec Card Placeholder -->
      <view class="spec-card glass-panel">
        <view class="spec-row">
          <text class="spec-label">服务：</text>
          <text class="spec-val">🏷️ 7天无理由退货 · 📦 商家发货 & 售后</text>
        </view>
      </view>

      <!-- Parameters Attribute Table -->
      <view class="param-card glass-panel" v-if="attributeList.length > 0">
        <view class="card-header">
          <text class="indicator-line"></text>
          <text class="card-title">商品属性</text>
        </view>
        <view class="param-table">
          <view class="param-row" v-for="(item, idx) in attributeList" :key="idx">
            <view class="param-name-cell">
              <text class="param-name">{{ item.attribute }}</text>
            </view>
            <view class="param-value-cell">
              <text class="param-value">{{ item.value }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- HTML Rich-Text Content Details -->
      <view class="desc-card glass-panel" v-if="goods.detail">
        <view class="card-header">
          <text class="indicator-line accent-line"></text>
          <text class="card-title">商品详情</text>
        </view>
        <view class="rich-text-wrapper">
          <rich-text :nodes="htmlNodes" class="detail-rich-text"></rich-text>
        </view>
      </view>
      
      <view class="footer-spacer"></view>
    </scroll-view>

    <!-- Bottom Action Toolbar -->
    <view class="action-footer glass-panel">
      <view class="footer-navs">
        <view class="nav-item" @tap="onHomeTap">
          <text class="nav-icon">🏠</text>
          <text class="nav-label">首页</text>
        </view>
        <view class="nav-item" @tap="onCartTap">
          <text class="nav-icon">🛒</text>
          <text class="nav-label">购物车</text>
        </view>
      </view>
      <view class="footer-btns">
        <view class="btn-cart" @tap="onAddToCart">加入购物车</view>
        <view class="btn-buy" @tap="onBuyNow">立即购买</view>
      </view>
    </view>
  </view>

  <view class="loading-container" v-else>
    <text class="loading-icon">⏳</text>
    <text class="loading-text">正在加载详情...</text>
  </view>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '../../utils/request'
import api from '../../config/api'

// Route dynamic ID
const goodsId = ref(0)

// Product detail data states
const goods = ref(null)
const gallery = ref([])
const attributeList = ref([])
const productList = ref([])

onLoad((options) => {
  if (options.id) {
    goodsId.value = parseInt(options.id)
  }
})

const getGoodsDetail = async () => {
  try {
    const data = await request({
      url: `${api.GoodsDetail}?id=${goodsId.value}`,
      method: 'GET'
    })
    
    if (data) {
      goods.value = data.info || null
      gallery.value = data.info ? data.info.gallery : []
      attributeList.value = data.attribute || []
      productList.value = data.productList || []
    }
  } catch (err) {
    console.error('Failed to load product details:', err)
  }
}

// Format backend HTML content to adjust image sizes dynamically
const htmlNodes = computed(() => {
  if (!goods.value || !goods.value.detail) return ''
  // Fix styling issues for responsive images inside rich-text
  return goods.value.detail
    .replace(/<img[^>]*>/gi, (match) => {
      return match.replace(/style="[^"]*"/gi, '').replace(/width="[^"]*"/gi, '').replace(/height="[^"]*"/gi, '')
    })
    .replace(/<img/gi, '<img style="width:100%;height:auto;display:block;margin:0 auto;"')
})

const onBackTap = () => {
  uni.navigateBack({
    fail: () => {
      uni.switchTab({
        url: '/pages/index/index'
      })
    }
  })
}

const onPreviewImage = (index) => {
  uni.previewImage({
    current: gallery.value[index],
    urls: gallery.value
  })
}

const onHomeTap = () => {
  uni.switchTab({
    url: '/pages/index/index'
  })
}

const onCartTap = () => {
  uni.switchTab({
    url: '/pages/cart/cart'
  })
}

const onAddToCart = async () => {
  // Check login status first
  const token = uni.getStorageSync('token')
  if (!token) {
    uni.showModal({
      title: '提示',
      content: '请先登录以使用购物车功能',
      confirmText: '去登录',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          uni.navigateTo({
            url: '/pages/auth/login'
          })
        }
      }
    })
    return
  }

  // Ensure products list (SKUs) is available
  if (!productList.value || productList.value.length === 0) {
    uni.showToast({
      title: '该商品暂无可售规格',
      icon: 'none'
    })
    return
  }

  // Use the first product configuration (SKU) as default
  const defaultProduct = productList.value[0]

  try {
    const data = await request({
      url: api.CartAdd,
      method: 'POST',
      data: {
        goodsId: goods.value.id,
        number: 1,
        productId: defaultProduct.id
      }
    })
    
    uni.showToast({
      title: '已加入购物车',
      icon: 'success'
    })
  } catch (err) {
    console.error('Failed to add商品到购物车:', err)
  }
}

const onBuyNow = async () => {
  // Check login status first
  const token = uni.getStorageSync('token')
  if (!token) {
    uni.showModal({
      title: '提示',
      content: '请先登录以购买商品',
      confirmText: '去登录',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          uni.navigateTo({
            url: '/pages/auth/login'
          })
        }
      }
    })
    return
  }

  // Ensure products list is available
  if (!productList.value || productList.value.length === 0) {
    uni.showToast({
      title: '该商品暂无可售规格',
      icon: 'none'
    })
    return
  }

  const defaultProduct = productList.value[0]

  // Add to cart first, then navigate to checkout or display success
  try {
    await request({
      url: api.CartAdd,
      method: 'POST',
      data: {
        goodsId: goods.value.id,
        number: 1,
        productId: defaultProduct.id
      }
    })
    
    uni.showToast({
      title: '已加购，正在结算...',
      icon: 'none'
    })
    setTimeout(() => {
      uni.switchTab({
        url: '/pages/cart/cart'
      })
    }, 800)
  } catch (err) {
    console.error('Failed to buy now:', err)
  }
}

onMounted(() => {
  getGoodsDetail()
})
</script>

<style scoped>
.container {
  height: 100vh;
  background-color: var(--bg-primary);
  position: relative;
}

/* Custom navigation button */
.nav-back {
  position: absolute;
  top: 80rpx;
  left: 40rpx;
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.7);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.nav-back:active {
  background: rgba(255, 255, 255, 0.9);
}

.back-icon {
  font-size: 34rpx;
  color: var(--text-main);
  font-weight: bold;
}

/* Scroll area */
.detail-scroll {
  height: 100%;
}

/* Carousel */
.gallery-wrapper {
  width: 100%;
  height: 750rpx;
  background-color: #f1f5f9;
}

.gallery-swiper {
  width: 100%;
  height: 100%;
}

.gallery-image {
  width: 100%;
  height: 100%;
}

/* Primary info details */
.goods-info-card {
  margin: -40rpx 32rpx 32rpx 32rpx;
  border-radius: var(--radius-md);
  padding: 40rpx 32rpx;
  position: relative;
  z-index: 10;
  box-shadow: var(--shadow-md);
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.price-block {
  display: flex;
  align-items: baseline;
}

.price-symbol {
  font-size: 32rpx;
  font-weight: 700;
  color: #ef4444;
  margin-right: 4rpx;
}

.price-retail {
  font-size: 52rpx;
  font-weight: 800;
  color: #ef4444;
  margin-right: 16rpx;
  letter-spacing: -1rpx;
}

.price-counter {
  font-size: 26rpx;
  color: var(--text-muted);
  text-decoration: line-through;
  font-weight: 500;
}

.tag-hot {
  padding: 6rpx 20rpx;
  border-radius: 20rpx;
  font-size: 20rpx;
  font-weight: 700;
  color: #ffffff;
  background: var(--accent-gradient);
  box-shadow: 0 4px 8px rgba(244, 63, 94, 0.2);
}

.tag-new {
  padding: 6rpx 20rpx;
  border-radius: 20rpx;
  font-size: 20rpx;
  font-weight: 700;
  color: #ffffff;
  background: var(--primary-gradient);
  box-shadow: 0 4px 8px rgba(59, 130, 246, 0.2);
}

.goods-name {
  font-size: 34rpx;
  font-weight: 800;
  color: var(--text-main);
  margin-bottom: 12rpx;
  line-height: 1.4;
  letter-spacing: -0.5rpx;
}

.goods-brief {
  font-size: 26rpx;
  color: var(--text-muted);
  line-height: 1.5;
}

/* Service specification row */
.spec-card {
  margin: 0 32rpx 32rpx 32rpx;
  border-radius: var(--radius-md);
  padding: 32rpx;
}

.spec-row {
  display: flex;
  font-size: 26rpx;
}

.spec-label {
  color: var(--text-muted);
  font-weight: 600;
  margin-right: 16rpx;
}

.spec-val {
  color: var(--text-main);
  font-weight: 600;
}

/* Attribute list card */
.param-card {
  margin: 0 32rpx 32rpx 32rpx;
  border-radius: var(--radius-md);
  padding: 32rpx;
}

.card-header {
  display: flex;
  align-items: center;
  margin-bottom: 28rpx;
}

.indicator-line {
  width: 6rpx;
  height: 30rpx;
  border-radius: 4rpx;
  background: var(--primary-gradient);
  margin-right: 12rpx;
}

.accent-line {
  background: var(--accent-gradient);
}

.card-title {
  font-size: 28rpx;
  font-weight: 700;
  color: var(--text-main);
}

.param-table {
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.param-row {
  display: flex;
  border-bottom: 1px solid var(--border-light);
}

.param-row:last-child {
  border-bottom: none;
}

.param-name-cell {
  width: 180rpx;
  background-color: rgba(248, 250, 252, 0.8);
  padding: 20rpx 24rpx;
  display: flex;
  align-items: center;
  border-right: 1px solid var(--border-light);
}

.param-name {
  font-size: 24rpx;
  color: var(--text-muted);
  font-weight: 600;
}

.param-value-cell {
  flex: 1;
  padding: 20rpx 24rpx;
  background-color: #ffffff;
}

.param-value {
  font-size: 24rpx;
  color: var(--text-main);
  font-weight: 500;
  line-height: 1.4;
}

/* Rich-text detail contents */
.desc-card {
  margin: 0 32rpx 32rpx 32rpx;
  border-radius: var(--radius-md);
  padding: 32rpx;
}

.rich-text-wrapper {
  margin-top: 16rpx;
}

.detail-rich-text {
  font-size: 28rpx;
  color: var(--text-main);
}

.footer-spacer {
  height: 180rpx;
}

/* Sticky footer controllers */
.action-footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 140rpx;
  padding: 16rpx 32rpx;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1rpx solid rgba(226,232,240,0.5);
  box-shadow: 0 -8px 24px rgba(0, 0, 0, 0.04);
}

.footer-navs {
  display: flex;
  gap: 36rpx;
  padding-left: 12rpx;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.nav-icon {
  font-size: 38rpx;
  margin-bottom: 4rpx;
}

.nav-label {
  font-size: 20rpx;
  color: var(--text-muted);
  font-weight: 600;
}

.footer-btns {
  display: flex;
  gap: 16rpx;
  flex: 1;
  justify-content: flex-end;
  padding-left: 36rpx;
}

.btn-cart {
  height: 84rpx;
  padding: 0 36rpx;
  border-radius: 42rpx;
  background: rgba(59, 130, 246, 0.08);
  border: 1px solid rgba(59, 130, 246, 0.15);
  color: #3b82f6;
  font-size: 26rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-buy {
  height: 84rpx;
  padding: 0 44rpx;
  border-radius: 42rpx;
  background: var(--primary-gradient);
  color: #ffffff;
  font-size: 26rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.25);
}

.btn-cart:active, .btn-buy:active {
  opacity: 0.85;
}

/* Loading container */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background-color: var(--bg-primary);
}

.loading-icon {
  font-size: 70rpx;
  margin-bottom: 24rpx;
  animation: spinner 1.5s infinite linear;
}

.loading-text {
  font-size: 26rpx;
  color: var(--text-muted);
  font-weight: 500;
}

@keyframes spinner {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>
