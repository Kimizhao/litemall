<template>
  <view class="container">
    <scroll-view 
      scroll-y 
      class="topic-scroll" 
      @scrolltolower="onReachBottom"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="topic-feed">
        <view 
          class="topic-card glass-panel" 
          v-for="item in topicList" 
          :key="item.id"
          @tap="onTopicTap(item.id)"
        >
          <image :src="item.picUrl" mode="aspectFill" class="topic-image" />
          <view class="topic-info">
            <text class="topic-title">{{ item.title }}</text>
            <text class="topic-subtitle">{{ item.subtitle }}</text>
            <view class="topic-meta">
              <text class="price-lead" v-if="item.price > 0">¥{{ item.price }} 起</text>
              <text class="read-more">阅读详情 →</text>
            </view>
          </view>
        </view>
      </view>

      <!-- Loading / End States -->
      <view class="feed-state">
        <text class="state-text" v-if="loading">正在加载中...</text>
        <text class="state-text" v-else-if="finished">已加载全部专题</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import api from '../../config/api'

const topicList = ref([])
const page = ref(1)
const limit = ref(10)
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)

const getTopicList = async (reset = false) => {
  if (loading.value) return
  if (reset) {
    page.value = 1
    finished.value = false
  }
  if (finished.value) return
  
  loading.value = true
  try {
    const data = await request({
      url: `${api.TopicList}?page=${page.value}&limit=${limit.value}`,
      method: 'GET'
    })
    
    if (data && data.limit) {
      const list = data.list || []
      if (reset) {
        topicList.value = list
      } else {
        topicList.value = [...topicList.value, ...list]
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
    console.error('Failed to get topics:', err)
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

const onReachBottom = () => {
  getTopicList()
}

const onRefresh = () => {
  refreshing.value = true
  getTopicList(true)
}

const onTopicTap = (id) => {
  uni.navigateTo({
    url: `/pages/topicDetail/topicDetail?id=${id}`
  })
}

onMounted(() => {
  getTopicList(true)
})
</script>

<style scoped>
.container {
  height: 100vh;
  background-color: var(--bg-primary);
}

.topic-scroll {
  height: 100%;
}

.topic-feed {
  padding: 32rpx;
  display: flex;
  flex-direction: column;
  gap: 32rpx;
}

.topic-card {
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
  transition: transform 0.2s ease;
}

.topic-card:active {
  transform: scale(0.98);
}

.topic-image {
  width: 100%;
  height: 380rpx;
  background-color: #f1f5f9;
}

.topic-info {
  padding: 32rpx;
  display: flex;
  flex-direction: column;
}

.topic-title {
  font-size: 30rpx;
  font-weight: 800;
  color: var(--text-main);
  margin-bottom: 8rpx;
  line-height: 1.4;
}

.topic-subtitle {
  font-size: 24rpx;
  color: var(--text-muted);
  font-weight: 500;
  line-height: 1.5;
  margin-bottom: 24rpx;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.topic-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
}

.price-lead {
  font-size: 26rpx;
  font-weight: 800;
  color: #ef4444;
}

.read-more {
  font-size: 24rpx;
  color: #4f46e5;
  font-weight: 700;
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
