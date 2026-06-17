<template>
  <view class="container">
    <!-- Horizontal Sibling Category Nav -->
    <scroll-view scroll-x class="cate-nav-bar" :scroll-into-view="'cat-item-' + currentCategory.id" scroll-with-animation>
      <view class="cate-nav-list">
        <view 
          v-for="item in navList" 
          :key="item.id" 
          :id="'cat-item-' + item.id"
          class="nav-item" 
          :class="{ active: id === item.id }" 
          @tap="onNavTab(item)"
        >
          <text class="nav-text">{{ item.name }}</text>
          <view class="active-pill" v-if="id === item.id"></view>
        </view>
      </view>
    </scroll-view>

    <!-- Sorting Filter Toolbar -->
    <view class="filter-toolbar glass-panel">
      <view class="filter-item" :class="{ active: currentSortType === 'default' }" @tap="onSortTab('default')">
        <text class="filter-text">综合</text>
      </view>
      <view class="filter-item price-filter" :class="{ active: currentSortType === 'price' }" @tap="onSortTab('price')">
        <text class="filter-text">价格</text>
        <view class="price-arrows">
          <text class="arrow up" :class="{ active: currentSortType === 'price' && currentSortOrder === 'asc' }">▲</text>
          <text class="arrow down" :class="{ active: currentSortType === 'price' && currentSortOrder === 'desc' }">▼</text>
        </view>
      </view>
      <view class="filter-item" :class="{ active: currentSortType === 'category' }" @tap="onSortTab('category')">
        <text class="filter-text">分类</text>
      </view>
    </view>

    <!-- Scrollable Goods Grid -->
    <scroll-view scroll-y class="goods-scroll" @scrolltolower="onReachBottom">
      <view class="goods-container">
        <!-- Grid Items -->
        <view class="goods-grid" v-if="goodsList.length > 0">
          <view class="goods-card" v-for="item in goodsList" :key="item.id" @tap="onGoodsCardTap(item)">
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

        <!-- Empty State -->
        <view class="empty-state" v-if="goodsList.length === 0 && !loading">
          <text class="empty-icon">📦</text>
          <text class="empty-text">此分类下暂无相关商品</text>
        </view>

        <!-- Loading / No More states -->
        <view class="state-footer" v-if="goodsList.length > 0">
          <text class="footer-text" v-if="loading">加载中...</text>
          <text class="footer-text" v-else-if="finished">—— 已经到底啦 ——</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '../../utils/request'
import api from '../../config/api'

// Query routing states
const id = ref(0)
const navList = ref([])
const currentCategory = ref({})

// Goods list states
const goodsList = ref([])
const page = ref(1)
const limit = ref(10)
const loading = ref(false)
const finished = ref(false)

// Sorting states
const currentSortType = ref('default')
const currentSortOrder = ref('desc')

onLoad((options) => {
  if (options.id) {
    id.value = parseInt(options.id)
  }
})

const getCategoryInfo = async () => {
  try {
    const data = await request({
      url: `${api.GoodsCategory}?id=${id.value}`,
      method: 'GET'
    })
    if (data) {
      navList.value = data.brotherCategory || []
      currentCategory.value = data.currentCategory || {}
      if (data.currentCategory && data.currentCategory.id) {
        id.value = data.currentCategory.id
      }
    }
  } catch (err) {
    console.error('Failed to load sibling categories:', err)
  }
}

const getGoodsList = async (reset = false) => {
  if (loading.value) return
  if (reset) {
    page.value = 1
    goodsList.value = []
    finished.value = false
  }
  
  loading.value = true
  try {
    const sortMap = {
      default: 'add_time',
      price: 'retail_price',
      category: 'name'
    }
    const apiSort = sortMap[currentSortType.value] || 'add_time'
    
    const data = await request({
      url: `${api.GoodsList}?categoryId=${id.value}&page=${page.value}&limit=${limit.value}&sort=${apiSort}&order=${currentSortOrder.value}`,
      method: 'GET'
    })
    
    if (data && data.list) {
      const list = data.list
      goodsList.value = [...goodsList.value, ...list]
      if (list.length < limit.value) {
        finished.value = true
      } else {
        page.value++
      }
    } else {
      finished.value = true
    }
  } catch (err) {
    console.error('Failed to load goods list:', err)
  } finally {
    loading.value = false
  }
}

// Switching nav category tab
const onNavTab = async (item) => {
  if (id.value === item.id) return
  id.value = item.id
  currentCategory.value = item
  await getGoodsList(true)
}

// Handling sorting tabs
const onSortTab = async (sortType) => {
  if (sortType === 'price') {
    if (currentSortType.value === 'price') {
      // Toggle order
      currentSortOrder.value = currentSortOrder.value === 'asc' ? 'desc' : 'asc'
    } else {
      currentSortType.value = 'price'
      currentSortOrder.value = 'asc'
    }
  } else {
    if (currentSortType.value === sortType) return
    currentSortType.value = sortType
    currentSortOrder.value = 'desc'
  }
  await getGoodsList(true)
}

// Infinite scroll triggers
const onReachBottom = () => {
  if (!finished.value && !loading.value) {
    getGoodsList()
  }
}

onMounted(async () => {
  uni.showLoading({ title: '加载中...' })
  await getCategoryInfo()
  await getGoodsList(true)
  uni.hideLoading()
})

const onGoodsCardTap = (item) => {
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

/* Horizontal top nav */
.cate-nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 84rpx;
  background-color: #ffffff;
  z-index: 100;
  border-bottom: 1rpx solid var(--border-light);
}

.cate-nav-list {
  display: flex;
  white-space: nowrap;
  height: 100%;
  padding: 0 16rpx;
}

.nav-item {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 0 32rpx;
  height: 100%;
  position: relative;
}

.nav-text {
  font-size: 28rpx;
  color: var(--text-muted);
  font-weight: 500;
  transition: color 0.2s ease;
}

.nav-item.active .nav-text {
  color: #4f46e5;
  font-weight: 700;
}

.active-pill {
  position: absolute;
  bottom: 0;
  height: 6rpx;
  width: 40rpx;
  background: var(--primary-gradient);
  border-radius: 4rpx;
}

/* Sorting toolbar */
.filter-toolbar {
  position: fixed;
  top: 84rpx;
  left: 0;
  right: 0;
  height: 80rpx;
  display: flex;
  align-items: center;
  z-index: 100;
  border-bottom: 1rpx solid rgba(226,232,240,0.5);
  box-shadow: var(--shadow-sm);
}

.filter-item {
  flex: 1;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.filter-text {
  font-size: 26rpx;
  color: var(--text-muted);
  font-weight: 600;
}

.filter-item.active .filter-text {
  color: #4f46e5;
}

.price-filter {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.price-arrows {
  display: flex;
  flex-direction: column;
  justify-content: center;
  line-height: 1;
}

.arrow {
  font-size: 14rpx;
  color: #cbd5e1;
}

.arrow.active {
  color: #4f46e5;
}

/* Scroll area */
.goods-scroll {
  flex: 1;
  margin-top: 164rpx;
}

.goods-container {
  padding: 32rpx;
}

/* Products grid */
.goods-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 28rpx;
}

.goods-card {
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
  transition: transform 0.2s ease;
}

.goods-card:active {
  transform: scale(0.98);
}

.goods-image {
  width: 100%;
  height: 330rpx;
  background: #f1f5f9;
}

.goods-info {
  padding: 20rpx;
}

.goods-name {
  font-size: 26rpx;
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 8rpx;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  height: 72rpx;
  line-height: 1.4;
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16rpx;
}

.price-wrap {
  display: flex;
  align-items: baseline;
}

.price-symbol {
  font-size: 22rpx;
  font-weight: 700;
  color: #ef4444;
  margin-right: 2rpx;
}

.goods-price {
  font-size: 32rpx;
  font-weight: 700;
  color: #ef4444;
}

.add-btn {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: var(--primary-gradient);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  font-weight: bold;
}

/* Footer status indicators */
.state-footer {
  width: 100%;
  display: flex;
  justify-content: center;
  padding: 40rpx 0;
}

.footer-text {
  font-size: 24rpx;
  color: var(--text-muted);
  font-weight: 500;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 0;
}

.empty-icon {
  font-size: 90rpx;
  margin-bottom: 24rpx;
}

.empty-text {
  font-size: 26rpx;
  color: var(--text-muted);
}
</style>
