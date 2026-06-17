<template>
  <view class="container">
    <scroll-view 
      scroll-y 
      class="collect-scroll"
      @scrolltolower="onReachBottom"
    >
      <view class="goods-grid" v-if="collectList.length > 0">
        <view 
          class="goods-card glass-panel" 
          v-for="item in collectList" 
          :key="item.id"
          @tap="onGoodsTap(item.valueId)"
        >
          <!-- Deletion Action overlay -->
          <text class="uncollect-btn" @tap.stop="onUncollect(item)">✕</text>
          
          <image :src="item.picUrl" mode="aspectFill" class="goods-image" />
          <view class="goods-info">
            <text class="goods-name">{{ item.name }}</text>
            <text class="goods-brief">{{ item.brief }}</text>
            <view class="price-row">
              <text class="price-symbol">¥</text>
              <text class="price-val">{{ item.retailPrice }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="empty-collect" v-else-if="!loading">
        <text class="empty-icon">⭐</text>
        <text class="empty-title">暂无收藏宝贝</text>
        <text class="empty-desc">在商品详情页点击星星，即可收藏心仪宝贝</text>
      </view>

      <view class="feed-state">
        <text class="state-text" v-if="loading">正在加载收藏...</text>
        <text class="state-text" v-else-if="finished">已同步全部收藏</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../../utils/request'
import api from '../../../config/api'

const collectList = ref([])
const page = ref(1)
const limit = ref(10)
const loading = ref(false)
const finished = ref(false)

const getCollectList = async (reset = false) => {
  if (loading.value) return
  if (reset) {
    page.value = 1
    finished.value = false
  }
  if (finished.value) return

  loading.value = true
  try {
    const data = await request({
      url: `${api.CollectList}?type=0&page=${page.value}&limit=${limit.value}`,
      method: 'GET'
    })
    
    if (data) {
      const list = data.list || []
      if (reset) {
        collectList.value = list
      } else {
        collectList.value = [...collectList.value, ...list]
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
    console.error('Failed to get collect list:', err)
  } finally {
    loading.value = false
  }
}

const onUncollect = (item) => {
  uni.showModal({
    title: '取消收藏',
    content: '确定要取消收藏该宝贝吗？',
    confirmText: '取消收藏',
    cancelText: '取消',
    confirmColor: '#ef4444',
    success: async (res) => {
      if (res.confirm) {
        try {
          await request({
            url: api.CollectAddOrDelete,
            method: 'POST',
            data: {
              type: 0,
              valueId: item.valueId
            }
          })
          uni.showToast({
            title: '已取消收藏',
            icon: 'success'
          })
          getCollectList(true)
        } catch (err) {
          console.error('Failed to uncollect:', err)
        }
      }
    }
  })
}

const onReachBottom = () => {
  getCollectList()
}

const onGoodsTap = (id) => {
  uni.navigateTo({
    url: `/pages/goods/goods?id=${id}`
  })
}

onMounted(() => {
  getCollectList(true)
})
</script>

<style scoped>
.container {
  height: 100vh;
  background-color: var(--bg-primary);
}

.collect-scroll {
  height: 100%;
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
  position: relative;
}

.uncollect-btn {
  position: absolute;
  top: 16rpx;
  right: 16rpx;
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background-color: rgba(15, 23, 42, 0.6);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20rpx;
  font-weight: bold;
  z-index: 10;
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
  margin-bottom: 4rpx;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.3;
}

.goods-brief {
  font-size: 18rpx;
  color: var(--text-muted);
  font-weight: 500;
  margin-bottom: 12rpx;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
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

/* Empty State */
.empty-collect {
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
