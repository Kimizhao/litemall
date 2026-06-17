<template>
  <view class="container">
    <!-- Header Summary -->
    <view class="rating-header glass-panel" v-if="commentCount > 0">
      <view class="rating-num-wrap">
        <text class="rating-score">5.0</text>
        <text class="rating-tip">好评率 100%</text>
      </view>
      <text class="rating-count">已有 {{ commentCount }} 位顾客留下心得评价</text>
    </view>

    <!-- Comments List -->
    <scroll-view 
      scroll-y 
      class="comment-scroll"
      @scrolltolower="onReachBottom"
    >
      <view class="comment-list" v-if="commentList.length > 0">
        <view class="comment-item glass-panel" v-for="item in commentList" :key="item.id">
          <view class="comment-user">
            <image :src="item.userInfo.avatarUrl || '/static/tabbar/user.png'" class="avatar" />
            <view class="user-meta">
              <text class="nickname">{{ item.userInfo.nickName || '精选手作粉' }}</text>
              <view class="stars-row">
                <text class="star" v-for="s in item.star" :key="s">⭐</text>
              </view>
            </view>
            <text class="date">{{ item.addTime }}</text>
          </view>
          <text class="comment-content">{{ item.content }}</text>
          
          <!-- Pictures list -->
          <view class="comment-pics" v-if="item.picList && item.picList.length > 0">
            <image 
              v-for="(pic, pIdx) in item.picList" 
              :key="pIdx" 
              :src="pic" 
              mode="aspectFill" 
              class="comment-pic"
              @tap="onPreviewImage(item.picList, pIdx)"
            />
          </view>
        </view>
      </view>

      <view class="empty-comments" v-else-if="!loading">
        <text class="empty-icon">✍️</text>
        <text class="empty-title">暂无商品评价</text>
        <text class="empty-desc">购买过此宝贝的顾客尚未留下评价心得</text>
      </view>

      <view class="feed-state">
        <text class="state-text" v-if="loading">正在同步评价...</text>
        <text class="state-text" v-else-if="finished">已同步全部评价</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '../../utils/request'
import api from '../../config/api'

const valueId = ref(0)
const commentList = ref([])
const commentCount = ref(0)
const page = ref(1)
const limit = ref(10)
const loading = ref(false)
const finished = ref(false)

onLoad((options) => {
  if (options.valueId) {
    valueId.value = parseInt(options.valueId)
  }
})

const getComments = async (reset = false) => {
  if (loading.value) return
  if (reset) {
    page.value = 1
    finished.value = false
  }
  if (finished.value) return

  loading.value = true
  try {
    const data = await request({
      url: `${api.CommentList}?valueId=${valueId.value}&type=0&showType=0&page=${page.value}&limit=${limit.value}`,
      method: 'GET'
    })
    
    if (data) {
      const list = data.list || []
      commentCount.value = data.limit ? data.count : 0
      if (reset) {
        commentList.value = list
      } else {
        commentList.value = [...commentList.value, ...list]
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
    console.error('Failed to get comments list:', err)
  } finally {
    loading.value = false
  }
}

const onReachBottom = () => {
  getComments()
}

const onPreviewImage = (urls, index) => {
  uni.previewImage({
    current: urls[index],
    urls: urls
  })
}

onMounted(() => {
  if (valueId.value) {
    getComments(true)
  }
})
</script>

<style scoped>
.container {
  height: 100vh;
  background-color: var(--bg-primary);
  display: flex;
  flex-direction: column;
}

.rating-header {
  margin: 32rpx;
  border-radius: var(--radius-md);
  padding: 32rpx 40rpx;
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.rating-num-wrap {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.rating-score {
  font-size: 52rpx;
  font-weight: 800;
  color: #ef4444;
}

.rating-tip {
  font-size: 20rpx;
  color: var(--text-muted);
  font-weight: 700;
}

.rating-count {
  font-size: 24rpx;
  color: var(--text-muted);
  font-weight: 600;
}

.comment-scroll {
  flex: 1;
}

.comment-list {
  padding: 0 32rpx 32rpx 32rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.comment-item {
  border-radius: var(--radius-md);
  padding: 32rpx;
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.comment-user {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background-color: #cbd5e1;
}

.user-meta {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.nickname {
  font-size: 24rpx;
  font-weight: 700;
  color: var(--text-main);
}

.stars-row {
  display: flex;
  gap: 2rpx;
  font-size: 16rpx;
}

.date {
  font-size: 18rpx;
  color: var(--text-muted);
  margin-left: auto;
  font-weight: 500;
}

.comment-content {
  font-size: 24rpx;
  color: var(--text-main);
  line-height: 1.4;
  font-weight: 500;
}

.comment-pics {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 8rpx;
}

.comment-pic {
  width: 140rpx;
  height: 140rpx;
  border-radius: var(--radius-sm);
  background-color: #f1f5f9;
}

/* Empty comments state */
.empty-comments {
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
