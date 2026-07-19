<template>
  <view class="container">
    <!-- Header Tabs -->
    <view class="tabs-header glass-panel">
      <view 
        class="tab-item" 
        v-for="tab in tabs" 
        :key="tab.showType"
        :class="{ active: showType === tab.showType }"
        @tap="onTabChange(tab.showType)"
      >
        {{ tab.name }}
      </view>
    </view>

    <!-- Orders Feed -->
    <scroll-view 
      scroll-y 
      class="orders-scroll"
      @scrolltolower="onReachBottom"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="orders-list" v-if="orderList.length > 0">
        <view 
          class="order-card glass-panel" 
          v-for="order in orderList" 
          :key="order.id"
          @tap="onOrderDetail(order.id)"
        >
          <!-- Card Header status -->
          <view class="card-header">
            <text class="order-sn">订单号: {{ order.orderSn }}</text>
            <text class="order-status" :class="'status-' + order.orderStatus">{{ order.orderStatusText }}</text>
          </view>

          <!-- Goods Items list -->
          <view class="goods-list">
            <view class="goods-item" v-for="g in order.goodsList" :key="g.id">
              <image :src="g.picUrl" mode="aspectFill" class="goods-img" />
              <view class="goods-info">
                <text class="goods-name">{{ g.goodsName }}</text>
                <text class="goods-spec">{{ g.specifications.join(' ') }}</text>
                <view class="price-num-row">
                  <text class="price">¥{{ g.price }}</text>
                  <text class="num">x{{ g.number }}</text>
                </view>
              </view>
            </view>
          </view>

          <!-- Price & Summary -->
          <view class="card-summary">
            <text class="summary-text">共 {{ getOrderGoodsCount(order) }} 件宝贝，实付 </text>
            <text class="summary-price">¥{{ order.actualPrice }}</text>
          </view>

          <!-- Inline Actions -->
          <view class="card-actions" @tap.stop v-if="hasActionButtons(order)">
            <button 
              class="action-btn cancel-btn" 
              v-if="order.handleOption.cancel" 
              @tap="onCancelOrder(order.id)"
            >
              取消订单
            </button>
            <button 
              class="action-btn pay-btn" 
              v-if="order.handleOption.pay" 
              @tap="onPayOrder(order.id)"
            >
              去付款
            </button>
            <button 
              class="action-btn confirm-btn" 
              v-if="order.handleOption.confirm" 
              @tap="onConfirmReceipt(order.id, order.message)"
            >
              {{ (order.message && order.message.indexOf('[预约自取]') !== -1) ? '确认取餐' : '确认收货' }}
            </button>
            <button 
              class="action-btn comment-btn" 
              v-if="order.handleOption.comment" 
              @tap="onCommentOrder(order.id)"
            >
              去评价
            </button>
          </view>
        </view>
      </view>

      <view class="empty-orders" v-else-if="!loading">
        <text class="empty-title">还没有任何订单呢</text>
      </view>

      <view class="feed-state">
        <text class="state-text" v-if="loading">正在同步订单...</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import request from '../../../utils/request'
import api from '../../../config/api'

const tabs = [
  { name: '全部', showType: 0 },
  { name: '待付款', showType: 1 },
  { name: '待发货', showType: 2 },
  { name: '待收货', showType: 3 },
  { name: '待评价', showType: 4 }
]

const showType = ref(0)
const orderList = ref([])
const page = ref(1)
const limit = ref(10)
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)

onLoad((options) => {
  if (options.tab) {
    showType.value = parseInt(options.tab)
  }
})

const getOrderList = async (reset = false) => {
  if (loading.value) return
  if (reset) {
    page.value = 1
    finished.value = false
  }
  if (finished.value) return

  loading.value = true
  try {
    const data = await request({
      url: `${api.OrderList}?showType=${showType.value}&page=${page.value}&limit=${limit.value}`,
      method: 'GET'
    })
    
    if (data) {
      let list = data.list || []
      list = list.map(order => {
        const isPaid = uni.getStorageSync('mock_paid_order_' + order.id)
        if (isPaid === true && order.orderStatus === 101) {
          order.orderStatus = 201
          order.orderStatusText = '已付款'
          if (order.handleOption) {
            order.handleOption.pay = false
          }
        }
        
        const isPickup = order.message && order.message.indexOf('[预约自取]') !== -1
        if (isPickup && order.orderStatus === 201) {
          if (!order.handleOption) {
            order.handleOption = {}
          }
          order.handleOption.confirm = true
        }
        return order
      })

      if (reset) {
        orderList.value = list
      } else {
        orderList.value = [...orderList.value, ...list]
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
    console.error('Failed to get order list:', err)
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

const onTabChange = (type) => {
  showType.value = type
  getOrderList(true)
}

const onReachBottom = () => {
  getOrderList()
}

const onRefresh = () => {
  refreshing.value = true
  getOrderList(true)
}

const getOrderGoodsCount = (order) => {
  if (!order.goodsList) return 0
  return order.goodsList.reduce((acc, curr) => acc + curr.number, 0)
}

const hasActionButtons = (order) => {
  const opt = order.handleOption
  return opt.cancel || opt.pay || opt.confirm || opt.comment
}

const onOrderDetail = (id) => {
  uni.navigateTo({
    url: `/pages/ucenter/orderDetail/orderDetail?id=${id}`
  })
}

const onCancelOrder = (id) => {
  uni.showModal({
    title: '取消订单',
    content: '您确定要取消该手作订单吗？',
    confirmText: '取消订单',
    cancelText: '取消',
    confirmColor: '#E07A5F',
    success: async (res) => {
      if (res.confirm) {
        try {
          await request({
            url: api.OrderCancel,
            method: 'POST',
            data: { orderId: id }
          })
          uni.showToast({ title: '订单已取消', icon: 'success' })
          getOrderList(true)
        } catch (err) {
          console.error('Failed to cancel order:', err)
        }
      }
    }
  })
}

const onPayOrder = (id) => {
  uni.navigateTo({
    url: `/pages/payResult/payResult?status=true&orderId=${id}`
  })
}

const onConfirmReceipt = (id, message) => {
  const isPickup = message && message.indexOf('[预约自取]') !== -1
  uni.showModal({
    title: isPickup ? '确认取餐' : '确认收货',
    content: isPickup ? '您已拿到预订的手作美味了吗？确认后订单将完成。' : '您已收到心仪的手作商品了吗？确认后将打款给独立手艺人。',
    confirmText: isPickup ? '确认取餐' : '确认收货',
    cancelText: '取消',
    confirmColor: '#8B8E4B',
    success: async (res) => {
      if (res.confirm) {
        try {
          await request({
            url: api.OrderConfirm,
            method: 'POST',
            data: { orderId: id }
          })
          uni.showToast({ title: isPickup ? '取餐完成' : '确认收货成功', icon: 'success' })
          getOrderList(true)
        } catch (err) {
          console.error('Failed to confirm receipt:', err)
        }
      }
    }
  })
}

const onCommentOrder = (id) => {
  uni.navigateTo({
    url: `/pages/commentPost/commentPost?orderId=${id}`
  })
}

onShow(() => {
  getOrderList(true)
})
</script>

<style scoped>
.container {
  height: 100vh;
  background-color: var(--bg-primary);
  display: flex;
  flex-direction: column;
}

.tabs-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  justify-content: space-around;
  height: 88rpx;
  align-items: center;
  border-bottom: 1px solid var(--border-light);
}

.tab-item {
  font-size: 26rpx;
  font-weight: 700;
  color: var(--text-muted);
  position: relative;
  height: 100%;
  display: flex;
  align-items: center;
}

.tab-item.active {
  color: #6F4E37;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 20%;
  right: 20%;
  height: 6rpx;
  background: var(--primary-gradient);
  border-radius: 3rpx;
}

.orders-scroll {
  flex: 1;
}

.orders-list {
  padding: 32rpx;
  display: flex;
  flex-direction: column;
  gap: 28rpx;
}

.order-card {
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
  padding: 32rpx;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 20rpx;
  border-bottom: 1px solid var(--border-light);
}

.order-sn {
  font-size: 24rpx;
  color: var(--text-muted);
  font-weight: 600;
}

.order-status {
  font-size: 24rpx;
  font-weight: 800;
}

.status-101 { color: #E07A5F; } /* unpaid */
.status-201 { color: #6F4E37; } /* paid/unshipped */
.status-301 { color: #D86B4F; } /* shipped/unreceived */
.status-401 { color: #8B8E4B; } /* received/completed */

.goods-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  padding: 24rpx 0;
  border-bottom: 1px solid var(--border-light);
}

.goods-item {
  display: flex;
  gap: 20rpx;
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
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.3;
}

.goods-spec {
  font-size: 20rpx;
  color: var(--text-muted);
  font-weight: 600;
  margin-top: 4rpx;
}

.price-num-row {
  display: flex;
  justify-content: space-between;
  margin-top: auto;
  align-items: baseline;
}

.price {
  font-size: 24rpx;
  font-weight: 700;
  color: var(--text-main);
}

.num {
  font-size: 22rpx;
  color: var(--text-muted);
  font-weight: 700;
}

.card-summary {
  display: flex;
  justify-content: flex-end;
  align-items: baseline;
  padding: 24rpx 0 0 0;
}

.summary-text {
  font-size: 24rpx;
  color: var(--text-muted);
  font-weight: 500;
}

.summary-price {
  font-size: 32rpx;
  color: #E07A5F;
  font-weight: 800;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16rpx;
  margin-top: 24rpx;
  padding-top: 24rpx;
  border-top: 1px solid var(--border-light);
}

.action-btn {
  height: 64rpx;
  padding: 0 28rpx;
  border-radius: 32rpx;
  font-size: 22rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cancel-btn {
  background-color: #ffffff;
  color: var(--text-muted);
  border: 1px solid var(--border-light);
}

.pay-btn, .confirm-btn, .comment-btn {
  background: var(--primary-gradient);
  color: #ffffff;
  box-shadow: 0 4px 10px rgba(79, 70, 229, 0.2);
}

.action-btn:active {
  opacity: 0.85;
}

/* Empty State */
.empty-orders {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 60vh;
  padding: 0 64rpx;
}

.empty-title {
  font-size: 32rpx;
  font-weight: 800;
  color: var(--text-main);
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
