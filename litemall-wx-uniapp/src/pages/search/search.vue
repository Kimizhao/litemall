<template>
  <view class="container">
    <!-- Header Search Bar -->
    <view class="search-header glass-panel">
      <view class="search-input-wrap">
        <text class="search-icon">🔍</text>
        <input 
          type="text" 
          class="search-input" 
          v-model="keyword" 
          placeholder="搜索您喜欢的精美手作物..." 
          confirm-type="search"
          @confirm="onSearch(keyword)"
          @input="onInput"
        />
        <text class="clear-icon" v-if="keyword" @tap="onClearInput">✕</text>
      </view>
      <text class="cancel-btn" @tap="onCancel">取消</text>
    </view>

    <!-- Search Suggestions List -->
    <view class="suggestion-list glass-panel" v-if="keyword && !showResult && suggestions.length > 0">
      <view 
        class="suggestion-item" 
        v-for="(item, idx) in suggestions" 
        :key="idx"
        @tap="onSearch(item)"
      >
        <text class="suggest-icon">🔍</text>
        <text class="suggest-text">{{ item }}</text>
      </view>
    </view>

    <!-- Search History & Hot Keywords -->
    <scroll-view scroll-y class="search-body" v-if="!showResult">
      <!-- Search History -->
      <view class="search-section" v-if="historyList.length > 0">
        <view class="section-header">
          <text class="section-title">历史搜索</text>
          <text class="delete-icon" @tap="onClearHistory">🗑️</text>
        </view>
        <view class="tag-list">
          <view 
            class="tag-item" 
            v-for="(item, idx) in historyList" 
            :key="idx"
            @tap="onSearch(item)"
          >
            {{ item }}
          </view>
        </view>
      </view>

      <!-- Hot Searches -->
      <view class="search-section" v-if="hotList.length > 0">
        <view class="section-header">
          <text class="section-title">热门推荐</text>
        </view>
        <view class="tag-list">
          <view 
            class="tag-item hot-tag" 
            v-for="(item, idx) in hotList" 
            :key="item.id"
            @tap="onSearch(item.keyword)"
          >
            {{ item.keyword }}
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- Search Results Grid -->
    <view class="search-results" v-else>
      <!-- Sort Header -->
      <view class="sort-header glass-panel">
        <view 
          class="sort-tab" 
          :class="{ active: currentSort === 'default' }" 
          @tap="onSortTab('default')"
        >
          综合
        </view>
        <view 
          class="sort-tab price-tab" 
          :class="{ active: currentSort === 'price' }" 
          @tap="onSortTab('price')"
        >
          价格
          <view class="arrows">
            <text class="arrow up" :class="{ highlight: currentSort === 'price' && order === 'asc' }">▲</text>
            <text class="arrow down" :class="{ highlight: currentSort === 'price' && order === 'desc' }">▼</text>
          </view>
        </view>
        <view 
          class="sort-tab" 
          :class="{ active: currentSort === 'category' }" 
          @tap="onSortTab('category')"
        >
          分类
        </view>
      </view>

      <!-- Results List -->
      <scroll-view scroll-y class="results-scroll" v-if="goodsList.length > 0">
        <view class="goods-grid">
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
        <view class="no-more">已加载全部商品</view>
      </scroll-view>

      <!-- Empty Result -->
      <view class="empty-results" v-else>
        <text class="empty-icon">🔍</text>
        <text class="empty-title">未找到相关商品</text>
        <text class="empty-desc">换个词试试，或者看看我们的热门推荐吧</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import api from '../../config/api'

const keyword = ref('')
const suggestions = ref([])
const historyList = ref([])
const hotList = ref([])

// Results and sorting
const showResult = ref(false)
const goodsList = ref([])
const currentSort = ref('default') // default, price, category
const order = ref('desc')          // asc, desc
const categoryId = ref(0)

const getSearchIndex = async () => {
  try {
    const data = await request({
      url: api.SearchIndex,
      method: 'GET'
    })
    if (data) {
      historyList.value = data.historyKeywordList || []
      hotList.value = data.hotKeywordList || []
    }
  } catch (err) {
    console.error('Failed to load search index:', err)
  }
}

const onInput = async () => {
  if (!keyword.value.trim()) {
    suggestions.value = []
    return
  }
  try {
    const data = await request({
      url: `${api.SearchHelper}?keyword=${keyword.value}`,
      method: 'GET'
    })
    suggestions.value = data || []
  } catch (err) {
    console.error('Failed to autocomplete:', err)
  }
}

const onClearInput = () => {
  keyword.value = ''
  suggestions.value = []
  showResult.value = false
  goodsList.value = []
}

const onCancel = () => {
  uni.navigateBack({
    fail: () => {
      uni.switchTab({
        url: '/pages/index/index'
      })
    }
  })
}

const onSearch = async (val) => {
  if (!val.trim()) return
  keyword.value = val
  showResult.value = true
  suggestions.value = []
  fetchResults()
}

const fetchResults = async () => {
  let sortField = 'add_time'
  if (currentSort.value === 'price') {
    sortField = 'retail_price'
  } else if (currentSort.value === 'category') {
    sortField = 'name'
  }

  try {
    const data = await request({
      url: `${api.SearchResult}?keyword=${keyword.value}&page=1&limit=20&sort=${sortField}&order=${order.value}&categoryId=${categoryId.value}`,
      method: 'GET'
    })
    if (data) {
      goodsList.value = data.goodsList || []
      // Update history list reactively
      getSearchIndex()
    }
  } catch (err) {
    console.error('Failed to fetch search results:', err)
  }
}

const onSortTab = (tab) => {
  if (currentSort.value === tab) {
    if (tab === 'price') {
      order.value = order.value === 'asc' ? 'desc' : 'asc'
    } else {
      return
    }
  } else {
    currentSort.value = tab
    order.value = tab === 'price' ? 'asc' : 'desc'
  }
  fetchResults()
}

const onClearHistory = async () => {
  uni.showModal({
    title: '清除历史',
    content: '您确定要清空所有搜索历史吗？',
    confirmText: '清空',
    cancelText: '取消',
    confirmColor: '#ef4444',
    success: async (res) => {
      if (res.confirm) {
        try {
          await request({
            url: api.SearchClearHistory,
            method: 'POST'
          })
          historyList.value = []
        } catch (err) {
          console.error('Failed to clear search history:', err)
        }
      }
    }
  })
}

const onGoodsTap = (id) => {
  uni.navigateTo({
    url: `/pages/goods/goods?id=${id}`
  })
}

onMounted(() => {
  getSearchIndex()
})
</script>

<style scoped>
.container {
  min-height: 100vh;
  background-color: var(--bg-primary);
  display: flex;
  flex-direction: column;
}

/* Header bar */
.search-header {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  padding: 24rpx 32rpx;
  gap: 20rpx;
  border-bottom: 1px solid var(--border-light);
}

.search-input-wrap {
  flex: 1;
  height: 76rpx;
  border-radius: 38rpx;
  background-color: rgba(241, 245, 249, 0.6);
  border: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  padding: 0 28rpx;
}

.search-icon {
  font-size: 28rpx;
  color: var(--text-muted);
  margin-right: 16rpx;
}

.search-input {
  flex: 1;
  font-size: 26rpx;
  color: var(--text-main);
}

.clear-icon {
  font-size: 28rpx;
  color: var(--text-muted);
  padding: 10rpx;
}

.cancel-btn {
  font-size: 26rpx;
  font-weight: 700;
  color: var(--text-muted);
}

/* Suggestions dropdown */
.suggestion-list {
  position: absolute;
  top: 110rpx;
  left: 32rpx;
  right: 32rpx;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  z-index: 99;
  max-height: 400rpx;
  overflow-y: auto;
  border: 1px solid var(--border-light);
}

.suggestion-item {
  display: flex;
  align-items: center;
  padding: 28rpx 32rpx;
  border-bottom: 1px solid var(--border-light);
}

.suggestion-item:last-child {
  border-bottom: none;
}

.suggest-icon {
  font-size: 24rpx;
  color: var(--text-muted);
  margin-right: 18rpx;
}

.suggest-text {
  font-size: 26rpx;
  color: var(--text-main);
  font-weight: 500;
}

/* Search body */
.search-body {
  flex: 1;
  padding: 32rpx;
}

.search-section {
  margin-bottom: 56rpx;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 800;
  color: var(--text-main);
}

.delete-icon {
  font-size: 32rpx;
  padding: 4rpx 12rpx;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.tag-item {
  padding: 12rpx 28rpx;
  border-radius: 28rpx;
  background-color: var(--bg-secondary);
  font-size: 24rpx;
  color: var(--text-muted);
  font-weight: 600;
  border: 1px solid var(--border-light);
}

.hot-tag {
  color: #4f46e5;
  background-color: rgba(79, 70, 229, 0.04);
  border-color: rgba(79, 70, 229, 0.1);
}

/* Results layout */
.search-results {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.sort-header {
  display: flex;
  justify-content: space-around;
  height: 88rpx;
  align-items: center;
  border-bottom: 1px solid var(--border-light);
}

.sort-tab {
  font-size: 26rpx;
  font-weight: 700;
  color: var(--text-muted);
  position: relative;
  height: 100%;
  display: flex;
  align-items: center;
}

.sort-tab.active {
  color: #4f46e5;
}

.sort-tab.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 20%;
  right: 20%;
  height: 6rpx;
  background: var(--primary-gradient);
  border-radius: 3rpx;
}

.price-tab {
  gap: 6rpx;
}

.arrows {
  display: flex;
  flex-direction: column;
  line-height: 0.6;
  font-size: 14rpx;
}

.arrow {
  color: #cbd5e1;
}

.arrow.highlight {
  color: #4f46e5;
}

.results-scroll {
  flex: 1;
}

.goods-grid {
  padding: 32rpx;
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
  flex: 1;
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

.no-more {
  text-align: center;
  font-size: 22rpx;
  color: var(--text-muted);
  padding: 32rpx 0 64rpx 0;
  font-weight: 500;
}

/* Empty State */
.empty-results {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  padding: 0 64rpx;
  margin-top: 150rpx;
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
</style>
