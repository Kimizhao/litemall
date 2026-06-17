<template>
  <view class="container">
    <scroll-view scroll-y class="help-scroll">
      <view class="help-list" v-if="issueList.length > 0">
        <view 
          class="faq-card glass-panel" 
          v-for="(item, idx) in issueList" 
          :key="item.id"
          @tap="onToggleAnswer(idx)"
        >
          <view class="faq-question">
            <text class="q-icon">❓</text>
            <text class="q-text">{{ item.question }}</text>
            <text class="arrow-icon" :class="{ rotated: activeIndex === idx }">▼</text>
          </view>
          <view class="faq-answer-wrap" :class="{ show: activeIndex === idx }">
            <view class="faq-answer">
              <text class="a-text">{{ item.answer }}</text>
            </view>
          </view>
        </view>
      </view>
      
      <view class="empty-help" v-else-if="!loading">
        <text class="empty-icon">📂</text>
        <text class="empty-title">暂无相关帮助信息</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import api from '../../config/api'

const issueList = ref([])
const activeIndex = ref(-1)
const loading = ref(false)

const getIssues = async () => {
  loading.value = true
  try {
    const data = await request({
      url: `${api.IssueList}?page=1&limit=50`,
      method: 'GET'
    })
    if (data) {
      issueList.value = data.list || []
    }
  } catch (err) {
    console.error('Failed to get issues:', err)
  } finally {
    loading.value = false
  }
}

const onToggleAnswer = (index) => {
  if (activeIndex.value === index) {
    activeIndex.value = -1
  } else {
    activeIndex.value = index
  }
}

onMounted(() => {
  getIssues()
})
</script>

<style scoped>
.container {
  height: 100vh;
  background-color: var(--bg-primary);
}

.help-scroll {
  height: 100%;
}

.help-list {
  padding: 32rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.faq-card {
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
}

.faq-question {
  display: flex;
  align-items: center;
  padding: 32rpx;
  gap: 16rpx;
  cursor: pointer;
}

.q-icon {
  font-size: 26rpx;
}

.q-text {
  flex: 1;
  font-size: 26rpx;
  font-weight: 700;
  color: var(--text-main);
}

.arrow-icon {
  font-size: 20rpx;
  color: var(--text-muted);
  transition: transform 0.2s ease;
}

.arrow-icon.rotated {
  transform: rotate(180deg);
}

/* Accordion slide-down effect */
.faq-answer-wrap {
  height: 0;
  overflow: hidden;
  transition: height 0.25s ease-out;
}

.faq-answer-wrap.show {
  height: auto;
}

.faq-answer {
  padding: 0 32rpx 32rpx 72rpx;
}

.a-text {
  font-size: 24rpx;
  color: var(--text-muted);
  line-height: 1.5;
  font-weight: 500;
}

/* Empty Help */
.empty-help {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 60vh;
}

.empty-icon {
  font-size: 90rpx;
  margin-bottom: 16rpx;
}

.empty-title {
  font-size: 32rpx;
  font-weight: 800;
  color: var(--text-main);
}
</style>
