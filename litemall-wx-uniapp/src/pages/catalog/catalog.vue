<template>
  <view class="container">
    <!-- Top Search Header -->
    <view class="search-header glass-panel">
      <view class="search-box">
        <text class="search-icon">🔍</text>
        <input type="text" placeholder="搜索分类商品..." disabled placeholder-style="color: #94a3b8;" />
      </view>
    </view>

    <!-- Dual Pane Content Layout -->
    <view class="pane-content">
      <!-- Left Category Scroll Menu -->
      <scroll-view scroll-y class="left-menu">
        <view class="menu-item" v-for="item in categoryList" :key="item.id" :class="{ active: currentCategory.id === item.id }" @tap="switchCategory(item)">
          <text class="item-text">{{ item.name }}</text>
          <view class="active-indicator" v-if="currentCategory.id === item.id"></view>
        </view>
        <view class="menu-item" v-if="categoryList.length === 0" v-for="i in 8" :key="i">
          <view class="skeleton-line"></view>
        </view>
      </scroll-view>

      <!-- Right Subcategory Scroll Pane -->
      <scroll-view scroll-y class="right-pane" :scroll-top="scrollTop">
        <view class="pane-container" v-if="currentCategory">
          <!-- Top Promotion Banner in Subcategory -->
          <view class="cat-banner-wrapper">
            <image :src="currentCategory.picUrl || 'https://images.unsplash.com/photo-1540959733332-eab4deceeaf7?q=80&w=600'" mode="aspectFill" class="cat-banner" />
            <view class="cat-banner-overlay">
              <text class="cat-banner-desc">{{ currentCategory.desc }}</text>
            </view>
          </view>

          <!-- Grid Title -->
          <view class="grid-title">
            <text class="title-line"></text>
            <text class="title-text">{{ currentCategory.name }} 子分类</text>
            <text class="title-line"></text>
          </view>

          <!-- Subcategories Grid -->
          <view class="subcat-grid">
            <view class="subcat-card" v-for="item in subCategoryList" :key="item.id" @tap="onSubcatTap(item)">
              <image :src="item.picUrl" mode="aspectFill" class="subcat-image" />
              <text class="subcat-name">{{ item.name }}</text>
            </view>
          </view>
          
          <view class="empty-state" v-if="subCategoryList.length === 0">
            <text class="empty-icon">📦</text>
            <text class="empty-text">该分类下暂无数据</text>
          </view>
        </view>
        
        <view class="loading-state" v-else>
          <text class="loading-icon">⏳</text>
          <text class="loading-text">分类正在加载中...</text>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import api from '../../config/api'

// Core reactive states
const categoryList = ref([])
const currentCategory = ref(null)
const subCategoryList = ref([])
const scrollTop = ref(0)

const fetchCatalog = async () => {
  try {
    const data = await request({
      url: api.CatalogList,
      method: 'GET'
    })
    if (data) {
      categoryList.value = data.categoryList || []
      currentCategory.value = data.currentCategory || null
      subCategoryList.value = data.currentSubCategory || []
    }
  } catch (err) {
    console.error('Failed to load Litemall categories:', err)
  }
}

const switchCategory = async (category) => {
  if (currentCategory.value.id === category.id) return
  currentCategory.value = category
  subCategoryList.value = [] // Reset right pane list during loading
  scrollTop.value = scrollTop.value === 0 ? -1 : 0 // Reset scroll position
  
  try {
    const data = await request({
      url: `${api.CatalogCurrent}?id=${category.id}`,
      method: 'GET'
    })
    if (data) {
      currentCategory.value = data.currentCategory || category
      subCategoryList.value = data.currentSubCategory || []
    }
  } catch (err) {
    console.error('Failed to switch category:', err)
  }
}

onMounted(() => {
  fetchCatalog()
})

const onSubcatTap = (item) => {
  uni.navigateTo({
    url: `/pages/category/category?id=${item.id}`
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

/* Dual Pane Layout */
.pane-content {
  flex: 1;
  margin-top: 110rpx;
  display: flex;
  overflow: hidden;
}

/* Left vertical menu scroll */
.left-menu {
  width: 190rpx;
  height: 100%;
  background-color: #ffffff;
  border-right: 1rpx solid var(--border-light);
}

.menu-item {
  height: 100rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  transition: all 0.2s ease;
}

.item-text {
  font-size: 26rpx;
  color: var(--text-muted);
  font-weight: 500;
  text-align: center;
  padding: 0 16rpx;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.menu-item.active {
  background-color: var(--bg-primary);
}

.menu-item.active .item-text {
  color: #4f46e5;
  font-weight: 700;
}

.active-indicator {
  position: absolute;
  left: 0;
  top: 30%;
  height: 40%;
  width: 6rpx;
  background: var(--primary-gradient);
  border-top-right-radius: 4rpx;
  border-bottom-right-radius: 4rpx;
}

.skeleton-line {
  width: 100rpx;
  height: 24rpx;
  border-radius: 6rpx;
  background: rgba(148, 163, 184, 0.1);
  animation: pulse 1.5s infinite ease-in-out;
}

/* Right scroll area */
.right-pane {
  flex: 1;
  height: 100%;
  background-color: var(--bg-primary);
}

.pane-container {
  padding: 24rpx;
}

.cat-banner-wrapper {
  position: relative;
  width: 100%;
  height: 190rpx;
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  margin-bottom: 32rpx;
}

.cat-banner {
  width: 100%;
  height: 100%;
}

.cat-banner-overlay {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  top: 0;
  background: linear-gradient(to bottom, rgba(15,23,42,0.1) 0%, rgba(15,23,42,0.65) 100%);
  display: flex;
  align-items: flex-end;
  padding: 16rpx 24rpx;
}

.cat-banner-desc {
  color: #ffffff;
  font-size: 24rpx;
  font-weight: 500;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* Category Grid Title */
.grid-title {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24rpx;
}

.title-line {
  flex: 1;
  height: 1rpx;
  background-color: var(--border-light);
}

.title-text {
  font-size: 24rpx;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 2rpx;
  padding: 0 20rpx;
}

/* Subcategories grid */
.subcat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20rpx;
}

.subcat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  background-color: #ffffff;
  border-radius: var(--radius-md);
  padding: 16rpx;
  border: 1px solid var(--border-light);
  transition: transform 0.2s ease;
}

.subcat-card:active {
  transform: scale(0.95);
}

.subcat-image {
  width: 100rpx;
  height: 100rpx;
  background: #f1f5f9;
  border-radius: 50%;
  margin-bottom: 12rpx;
}

.subcat-name {
  font-size: 22rpx;
  color: var(--text-main);
  font-weight: 600;
  text-align: center;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80rpx 0;
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: 16rpx;
}

.empty-text {
  font-size: 26rpx;
  color: var(--text-muted);
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 60%;
}

.loading-icon {
  font-size: 60rpx;
  margin-bottom: 16rpx;
  animation: spinner 1.5s infinite linear;
}

@keyframes spinner {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@keyframes pulse {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}
</style>
