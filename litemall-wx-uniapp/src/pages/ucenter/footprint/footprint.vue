<template>
  <view class="container">
    <scroll-view 
      scroll-y 
      class="foot-scroll"
      @scrolltolower="onReachBottom"
    >
      <view class="foot-list" v-if="footprintList.length > 0">
        <view 
          class="foot-row glass-panel" 
          v-for="item in footprintList" 
          :key="item.id"
          @tap="onGoodsTap(item.goodsId)"
        >
          <image :src="item.picUrl" mode="aspectFill" class="goods-img" />
          <view class="goods-info">
            <text class="goods-name">{{ item.name }}</text>
            <text class="goods-brief">{{ item.brief }}</text>
            <view class="price-row">
              <text class="price-symbol">¥</text>
              <text class="price-val">{{ item.retailPrice }}</text>
            </view>
          </view>
          <!-- Deletion trigger -->
          <text class="delete-btn" @tap.stop="onDeleteFootprint(item)">✕</text>
        </view>
      </view>

      <view class="empty-foot" v-else-if="!loading">
        <text class="empty-icon">👣</text>
        <text class="empty-title">暂无浏览足迹</text>
        <text class="empty-desc">多去逛逛商品，留下您的珍贵印记吧</text>
      </view>

      <view class="feed-state">
        <text class="state-text" v-if="loading">正在加载足迹...</text>
        <text class="state-text" v-else-if="finished">已同步全部足迹</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../../utils/request'
import api from '../../../config/api'

const footprintList = ref([])
const page = ref(1)
const limit = ref(10)
const loading = ref(false)
const finished = ref(false)

const getFootprintList = async (reset = false) => {
  if (loading.value) return
  if (reset) {
    page.value = 1
    finished.value = false
  }
  if (finished.value) return

  loading.value = true
  try {
    const data = await request({
      url: `${api.FootprintList}?page=${page.value}&limit=${limit.value}`,
      method: 'GET'
    })
    
    if (data) {
      const list = data.list || []
      if (reset) {
        footprintList.value = list
      } else {
        footprintList.value = [...footprintList.value, ...list]
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
    console.error('Failed to get footprints:', err)
  } finally {
    loading.value = false
  }
}

const onDeleteFootprint = (item) => {
  uni.showModal({
    title: '删除足迹',
    content: '确定要从足迹中删除此商品记录吗？',
    confirmText: '删除',
    cancelText: '取消',
    confirmColor: '#ef4444',
    success: async (res) => {
      if (res.confirm) {
        try {
          await request({
            url: api.FootprintDelete,
            method: 'POST',
            data: {
              id: item.id
            }
          })
          uni.showToast({
            title: '删除足迹成功',
            icon: 'success'
          })
          getFootprintList(true)
        } catch (err) {
          console.error('Failed to delete footprint:', err)
        }
      }
    }
  })
}

const onReachBottom = () => {
  getFootprintList()
}

const onGoodsTap = (id) => {
  uni.navigateTo({
    url: `/pages/goods/goods?id=${id}`
  })
}

onMounted(() => {
  getFootprintList(true)
})
</script>

<style scoped>
.container {
  height: 100vh;
  background-color: var(--bg-primary);
}

.foot-scroll {
  height: 100%;
}

.foot-list {
  padding: 32rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.foot-row {
  display: flex;
  align-items: center;
  padding: 24rpx;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
  position: relative;
}

.goods-img {
  width: 120rpx;
  height: 120rpx;
  border-radius: var(--radius-sm);
  background-color: #f1f5f9;
  margin-right: 20rpx;
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
  margin-bottom: 8rpx;
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
  font-size: 26rpx;
  font-weight: 700;
  color: #ef4444;
}

.delete-btn {
  font-size: 24rpx;
  color: #cbd5e1;
  padding: 16rpx;
  font-weight: bold;
}

/* Empty Foot State */
.empty-foot {
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
