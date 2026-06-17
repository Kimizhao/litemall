<template>
  <view class="container">
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
              <text class="date">{{ item.addTime }}</text>
            </view>
          </view>
          <text class="comment-content">{{ item.content }}</text>
          
          <!-- Image attachments if any -->
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
        <text class="empty-title">暂无留言</text>
        <text class="empty-desc">赶紧去专题详情写下您的精美留言吧</text>
      </view>

      <view class="feed-state" v-if="loading || finished">
        <text class="state-text" v-if="loading">正在加载留言...</text>
        <text class="state-text" v-else-if="finished">已显示全部留言</text>
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
      url: `${api.CommentList}?valueId=${valueId.value}&type=1&showType=0&page=${page.value}&limit=${limit.value}`,
      method: 'GET'
    })
    
    if (data) {
      const list = data.list || []
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
}

.comment-scroll {
  height: 100%;
}

.comment-list {
  padding: 32rpx;
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

.date {
  font-size: 18rpx;
  color: var(--text-muted);
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
  font-size: 90rpx;
  margin-bottom: 16rpx;
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
