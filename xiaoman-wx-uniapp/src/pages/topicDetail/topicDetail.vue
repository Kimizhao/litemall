<template>
  <view class="container" v-if="topic">
    <!-- Scrollable content -->
    <scroll-view scroll-y class="detail-scroll">
      <!-- Cover Banner -->
      <image :src="topic.picUrl" mode="aspectFill" class="topic-banner" />

      <!-- Article Title & Info -->
      <view class="article-header glass-panel">
        <text class="article-title">{{ topic.title }}</text>
        <text class="article-subtitle">{{ topic.subtitle }}</text>
        <view class="divider"></view>
        <text class="read-count">📖 阅读数: {{ topic.readCount || 0 }}次</text>
      </view>

      <!-- HTML Rich Text content -->
      <view class="article-content glass-panel" v-if="topic.content">
        <rich-text :nodes="htmlNodes" class="detail-rich-text"></rich-text>
      </view>

      <!-- Related Goods -->
      <view class="goods-section glass-panel" v-if="goodsList.length > 0">
        <view class="section-title-row">
          <text class="section-title">专题商品</text>
        </view>
        <view class="goods-list">
          <view 
            class="goods-item" 
            v-for="item in goodsList" 
            :key="item.id"
            @tap="onGoodsTap(item.id)"
          >
            <image :src="item.picUrl" mode="aspectFill" class="goods-img" />
            <view class="goods-info">
              <text class="goods-name">{{ item.name }}</text>
              <text class="goods-brief">{{ item.brief }}</text>
              <text class="goods-price">¥{{ item.retailPrice }}</text>
            </view>
            <text class="buy-now-arrow">></text>
          </view>
        </view>
      </view>

      <!-- Comments Section -->
      <view class="comment-section glass-panel">
        <view class="section-title-row">
          <text class="section-title">精选留言 ({{ commentCount }})</text>
          <text class="btn-post" @tap="onPostComment">写留言</text>
        </view>

        <view class="comment-list" v-if="commentList.length > 0">
          <view class="comment-item" v-for="item in commentList" :key="item.id">
            <view class="comment-user">
              <image :src="item.userInfo.avatarUrl || '/static/tabbar/user.png'" class="avatar" />
              <text class="nickname">{{ item.userInfo.nickName || '精选手作粉' }}</text>
              <text class="date">{{ item.addTime }}</text>
            </view>
            <text class="comment-content">{{ item.content }}</text>
          </view>
          <view class="more-comments" v-if="commentCount > 5" @tap="onViewAllComments">
            查看全部 {{ commentCount }} 条留言
          </view>
        </view>
        <view class="empty-comments" v-else>
          <text class="empty-icon">✍️</text>
          <text class="empty-text">成为第一个留下足迹的人吧</text>
        </view>
      </view>

      <view class="footer-spacer"></view>
    </scroll-view>
  </view>

  <view class="loading-wrap" v-else>
    <text class="loading-icon">⏳</text>
    <text class="loading-text">正在加载专题详情...</text>
  </view>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import request from '../../utils/request'
import api from '../../config/api'

const topicId = ref(0)
const topic = ref(null)
const goodsList = ref([])
const commentList = ref([])
const commentCount = ref(0)

onLoad((options) => {
  if (options.id) {
    topicId.value = parseInt(options.id)
  }
})

onShow(() => {
  if (topicId.value) {
    getTopicDetail()
    getComments()
  }
})

const getTopicDetail = async () => {
  try {
    const data = await request({
      url: `${api.TopicDetail}?id=${topicId.value}`,
      method: 'GET'
    })
    if (data) {
      topic.value = data.topic || null
      
      // Load related goods
      if (data.goods && data.goods.length > 0) {
        // Retrieve details of goods if given as string IDs list
        goodsList.value = data.goods
      } else {
        // Fallback or empty
        goodsList.value = []
      }
    }
  } catch (err) {
    console.error('Failed to get topic detail:', err)
  }
}

const getComments = async () => {
  try {
    const data = await request({
      url: `${api.CommentList}?valueId=${topicId.value}&type=1&showType=0&page=1&limit=5`,
      method: 'GET'
    })
    if (data) {
      commentList.value = data.list || []
      commentCount.value = data.limit ? data.count : 0
    }
  } catch (err) {
    console.error('Failed to load topic comments:', err)
  }
}

// Adjust rich-text HTML styling for H5/MP
const htmlNodes = computed(() => {
  if (!topic.value || !topic.value.content) return ''
  return topic.value.content
    .replace(/<img[^>]*>/gi, (match) => {
      return match.replace(/style="[^"]*"/gi, '').replace(/width="[^"]*"/gi, '').replace(/height="[^"]*"/gi, '')
    })
    .replace(/<img/gi, '<img style="width:100%;height:auto;display:block;margin:16rpx auto;border-radius:12rpx;"')
})

const onGoodsTap = (id) => {
  uni.navigateTo({
    url: `/pages/goods/goods?id=${id}`
  })
}

const onPostComment = () => {
  // Check login first
  const token = uni.getStorageSync('token')
  if (!token) {
    uni.navigateTo({
      url: '/pages/auth/login'
    })
    return
  }
  uni.navigateTo({
    url: `/pages/topicCommentPost/topicCommentPost?valueId=${topicId.value}`
  })
}

const onViewAllComments = () => {
  uni.navigateTo({
    url: `/pages/topicComment/topicComment?valueId=${topicId.value}`
  })
}
</script>

<style scoped>
.container {
  height: 100vh;
  background-color: var(--bg-primary);
}

.detail-scroll {
  height: 100%;
}

.topic-banner {
  width: 100%;
  height: 480rpx;
  background-color: #f1f5f9;
}

/* Article Header info */
.article-header {
  margin: -60rpx 32rpx 32rpx 32rpx;
  border-radius: var(--radius-md);
  padding: 40rpx 32rpx;
  position: relative;
  z-index: 10;
  box-shadow: var(--shadow-md);
  border: 1px solid var(--border-light);
}

.article-title {
  font-size: 34rpx;
  font-weight: 800;
  color: var(--text-main);
  line-height: 1.4;
  margin-bottom: 12rpx;
}

.article-subtitle {
  font-size: 26rpx;
  color: var(--text-muted);
  line-height: 1.5;
  font-weight: 500;
}

.divider {
  height: 1px;
  background-color: var(--border-light);
  margin: 24rpx 0;
}

.read-count {
  font-size: 22rpx;
  color: var(--text-muted);
  font-weight: 600;
}

/* Article rich text content wrapper */
.article-content {
  margin: 0 32rpx 32rpx 32rpx;
  border-radius: var(--radius-md);
  padding: 40rpx 32rpx;
  border: 1px solid var(--border-light);
}

.detail-rich-text {
  font-size: 28rpx;
  color: var(--text-main);
  line-height: 1.6;
}

/* Related Goods list */
.goods-section {
  margin: 0 32rpx 32rpx 32rpx;
  border-radius: var(--radius-md);
  padding: 32rpx;
  border: 1px solid var(--border-light);
}

.section-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 800;
  color: var(--text-main);
}

.goods-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.goods-item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding-bottom: 24rpx;
  border-bottom: 1px solid var(--border-light);
}

.goods-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.goods-img {
  width: 110rpx;
  height: 110rpx;
  border-radius: var(--radius-sm);
  background-color: #f1f5f9;
}

.goods-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.goods-name {
  font-size: 24rpx;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 4rpx;
}

.goods-brief {
  font-size: 20rpx;
  color: var(--text-muted);
  font-weight: 500;
  margin-bottom: 8rpx;
}

.goods-price {
  font-size: 24rpx;
  color: #ef4444;
  font-weight: 800;
}

.buy-now-arrow {
  font-size: 26rpx;
  color: #cbd5e1;
  font-weight: bold;
}

/* Comments list Section */
.comment-section {
  margin: 0 32rpx 32rpx 32rpx;
  border-radius: var(--radius-md);
  padding: 32rpx;
  border: 1px solid var(--border-light);
}

.btn-post {
  font-size: 24rpx;
  font-weight: 700;
  color: #6F4E37;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 32rpx;
}

.comment-item {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.comment-user {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.avatar {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background-color: #E6D5B8;
}

.nickname {
  font-size: 24rpx;
  font-weight: 700;
  color: var(--text-main);
}

.date {
  font-size: 20rpx;
  color: var(--text-muted);
  margin-left: auto;
  font-weight: 500;
}

.comment-content {
  font-size: 24rpx;
  color: var(--text-main);
  line-height: 1.4;
  font-weight: 500;
  padding-left: 60rpx;
}

.more-comments {
  text-align: center;
  font-size: 24rpx;
  color: #6F4E37;
  font-weight: 700;
  padding-top: 24rpx;
  border-top: 1px solid var(--border-light);
}

.empty-comments {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40rpx 0;
}

.empty-icon {
  font-size: 48rpx;
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
