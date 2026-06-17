<template>
  <view class="container">
    <!-- Non-logged in state -->
    <view class="login-prompt" v-if="!hasLogin">
      <text class="prompt-icon">🔒</text>
      <text class="prompt-title">您尚未登录账号</text>
      <text class="prompt-desc">登录后即可同步和结账购物车中的精选商品</text>
      <button class="login-btn" @tap="onGoLogin">立即登录</button>
    </view>

    <!-- Logged in state -->
    <view class="cart-workspace" v-else>
      <!-- Scrollable list of items -->
      <scroll-view scroll-y class="cart-scroll" v-if="cartList.length > 0">
        <view class="cart-list">
          <view class="cart-row glass-panel" v-for="item in cartList" :key="item.id">
            <!-- Checkbox selector -->
            <view class="check-box-cell" @tap="onCheckToggle(item)">
              <view class="custom-checkbox" :class="{ checked: item.checked }">
                <text class="check-mark" v-if="item.checked">✓</text>
              </view>
            </view>

            <!-- Product Card Info -->
            <image :src="item.picUrl" mode="aspectFill" class="product-image" />
            <view class="product-details">
              <view class="title-row">
                <text class="product-name">{{ item.goodsName }}</text>
                <text class="delete-icon" @tap="onDeleteRow(item)">🗑️</text>
              </view>
              <text class="product-spec">{{ item.specifications.join(' ') }}</text>
              
              <view class="action-row">
                <view class="price-wrap">
                  <text class="price-symbol">¥</text>
                  <text class="price-val">{{ item.price }}</text>
                </view>
                <!-- Number adjuster -->
                <view class="quantity-adjuster">
                  <view class="adjust-btn btn-minus" @tap="onQuantityChange(item, -1)">-</view>
                  <input type="number" class="qty-input" v-model="item.number" disabled />
                  <view class="adjust-btn btn-plus" @tap="onQuantityChange(item, 1)">+</view>
                </view>
              </view>
            </view>
          </view>
        </view>
        <view class="footer-spacer"></view>
      </scroll-view>

      <!-- Empty State -->
      <view class="empty-cart" v-else-if="!loading">
        <text class="empty-icon">🛒</text>
        <text class="empty-title">购物车空空如也</text>
        <text class="empty-desc">赶紧去挑选几件心仪的手作美物吧</text>
        <button class="shop-btn" @tap="onGoShopping">去逛逛</button>
      </view>

      <!-- Loading State -->
      <view class="loading-cart" v-else>
        <text class="loading-icon">⏳</text>
        <text class="loading-text">正在同步购物车...</text>
      </view>

      <!-- Sticky Footer Checkout Panel -->
      <view class="checkout-footer glass-panel" v-if="cartList.length > 0">
        <view class="select-all-wrap" @tap="onCheckAllToggle">
          <view class="custom-checkbox" :class="{ checked: isAllChecked }">
            <text class="check-mark" v-if="isAllChecked">✓</text>
          </view>
          <text class="select-label">全选</text>
        </view>

        <view class="checkout-info">
          <view class="price-row">
            <text class="total-label">合计：</text>
            <text class="total-price-symbol">¥</text>
            <text class="total-price-val">{{ cartTotal.checkedGoodsAmount }}</text>
          </view>
          <button class="checkout-btn" @tap="onCheckout">
            结算 ({{ cartTotal.checkedGoodsCount }})
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import request from '../../utils/request'
import api from '../../config/api'

// Core authentication & cart states
const hasLogin = ref(false)
const cartList = ref([])
const cartTotal = ref({
  checkedGoodsCount: 0,
  checkedGoodsAmount: 0.00
})
const loading = ref(false)

// Select all helper state
const isAllChecked = ref(false)

const getCartData = async () => {
  if (!hasLogin.value) return
  loading.value = true
  try {
    const data = await request({
      url: api.CartList,
      method: 'GET'
    })
    if (data) {
      cartList.value = data.cartList || []
      cartTotal.value = data.cartTotal || { checkedGoodsCount: 0, checkedGoodsAmount: 0.00 }
      
      // Update select all state
      isAllChecked.value = cartList.value.length > 0 && cartList.value.every(item => item.checked)
    }
  } catch (err) {
    console.error('Failed to sync Litemall cart:', err)
  } finally {
    loading.value = false
  }
}

// Check onShow lifecycle hook to dynamically reload items
onShow(() => {
  const token = uni.getStorageSync('token')
  if (token) {
    hasLogin.value = true
    getCartData()
  } else {
    hasLogin.value = false
    cartList.value = []
    cartTotal.value = { checkedGoodsCount: 0, checkedGoodsAmount: 0.00 }
  }
})

const onGoLogin = () => {
  uni.navigateTo({
    url: '/pages/auth/login'
  })
}

const onGoShopping = () => {
  uni.switchTab({
    url: '/pages/index/index'
  })
}

// Toggle individual check status
const onCheckToggle = async (item) => {
  try {
    const data = await request({
      url: api.CartChecked,
      method: 'POST',
      data: {
        productIds: [item.productId],
        isChecked: item.checked ? 0 : 1
      }
    })
    if (data) {
      cartList.value = data.cartList || []
      cartTotal.value = data.cartTotal || { checkedGoodsCount: 0, checkedGoodsAmount: 0.00 }
      isAllChecked.value = cartList.value.length > 0 && cartList.value.every(item => item.checked)
    }
  } catch (err) {
    console.error('Failed to toggle check status:', err)
  }
}

// Toggle select all status
const onCheckAllToggle = async () => {
  if (cartList.value.length === 0) return
  const nextChecked = !isAllChecked.value
  const productIds = cartList.value.map(item => item.productId)
  
  try {
    const data = await request({
      url: api.CartChecked,
      method: 'POST',
      data: {
        productIds: productIds,
        isChecked: nextChecked ? 1 : 0
      }
    })
    if (data) {
      cartList.value = data.cartList || []
      cartTotal.value = data.cartTotal || { checkedGoodsCount: 0, checkedGoodsAmount: 0.00 }
      isAllChecked.value = nextChecked
    }
  } catch (err) {
    console.error('Failed to toggle select all status:', err)
  }
}

// Adjust quantity
const onQuantityChange = async (item, diff) => {
  const nextNum = item.number + diff
  if (nextNum < 1) return
  
  try {
    const data = await request({
      url: api.CartUpdate,
      method: 'POST',
      data: {
        id: item.id,
        number: nextNum,
        goodsId: item.goodsId,
        productId: item.productId
      }
    })
    if (data) {
      cartList.value = data.cartList || []
      cartTotal.value = data.cartTotal || { checkedGoodsCount: 0, checkedGoodsAmount: 0.00 }
    }
  } catch (err) {
    console.error('Failed to update product quantity:', err)
  }
}

// Delete item
const onDeleteRow = (item) => {
  uni.showModal({
    title: '删除宝贝',
    content: '您确定要从购物车中删除该手作物吗？',
    confirmText: '删除',
    cancelText: '取消',
    confirmColor: '#ef4444',
    success: async (res) => {
      if (res.confirm) {
        try {
          const data = await request({
            url: api.CartDelete,
            method: 'POST',
            data: {
              productIds: [item.productId]
            }
          })
          if (data) {
            cartList.value = data.cartList || []
            cartTotal.value = data.cartTotal || { checkedGoodsCount: 0, checkedGoodsAmount: 0.00 }
            isAllChecked.value = cartList.value.length > 0 && cartList.value.every(item => item.checked)
            uni.showToast({
              title: '已删除宝贝',
              icon: 'success'
            })
          }
        } catch (err) {
          console.error('Failed to delete cart item:', err)
        }
      }
    }
  })
}

const onCheckout = () => {
  if (cartTotal.value.checkedGoodsCount === 0) {
    uni.showToast({
      title: '您尚未勾选商品呢',
      icon: 'none'
    })
    return
  }
  uni.showToast({
    title: '正在前往结算收银台...',
    icon: 'none'
  })
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background-color: var(--bg-primary);
  display: flex;
  flex-direction: column;
}

/* Prompt for not logged in */
.login-prompt {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  padding: 0 64rpx;
  box-sizing: border-box;
  margin-top: 180rpx;
}

.prompt-icon {
  font-size: 100rpx;
  margin-bottom: 24rpx;
}

.prompt-title {
  font-size: 34rpx;
  font-weight: 800;
  color: var(--text-main);
  margin-bottom: 12rpx;
}

.prompt-desc {
  font-size: 24rpx;
  color: var(--text-muted);
  text-align: center;
  line-height: 1.5;
  margin-bottom: 52rpx;
}

.login-btn {
  width: 280rpx;
  height: 84rpx;
  border-radius: 42rpx;
  background: var(--primary-gradient);
  color: #ffffff;
  font-size: 28rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.2);
}

.login-btn:active {
  opacity: 0.85;
}

/* Cart Workspace */
.cart-workspace {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.cart-scroll {
  flex: 1;
}

.cart-list {
  padding: 32rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.cart-row {
  display: flex;
  align-items: center;
  padding: 24rpx;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
}

/* Custom Checkbox */
.check-box-cell {
  padding: 20rpx 16rpx 20rpx 0;
}

.custom-checkbox {
  width: 42rpx;
  height: 42rpx;
  border-radius: 50%;
  border: 4rpx solid #cbd5e1;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #ffffff;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.custom-checkbox.checked {
  background: var(--primary-gradient);
  border-color: transparent;
}

.check-mark {
  color: #ffffff;
  font-size: 24rpx;
  font-weight: bold;
}

/* Product Info */
.product-image {
  width: 140rpx;
  height: 140rpx;
  border-radius: var(--radius-sm);
  background: #f1f5f9;
  margin-right: 20rpx;
}

.product-details {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 4rpx;
}

.product-name {
  font-size: 26rpx;
  font-weight: 700;
  color: var(--text-main);
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
  padding-right: 16rpx;
}

.delete-icon {
  font-size: 30rpx;
  padding: 4rpx 10rpx;
}

.product-spec {
  font-size: 22rpx;
  color: var(--text-muted);
  font-weight: 500;
  margin-bottom: 12rpx;
}

.action-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.price-val {
  font-size: 30rpx;
  font-weight: 700;
  color: #ef4444;
}

/* Quantity Adjuster */
.quantity-adjuster {
  display: flex;
  align-items: center;
  border: 1px solid var(--border-light);
  border-radius: 28rpx;
  overflow: hidden;
  height: 52rpx;
  background-color: #ffffff;
}

.adjust-btn {
  width: 52rpx;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  font-weight: bold;
  color: var(--text-main);
  background-color: rgba(241, 245, 249, 0.5);
}

.adjust-btn:active {
  background-color: #e2e8f0;
}

.qty-input {
  width: 60rpx;
  text-align: center;
  font-size: 24rpx;
  color: var(--text-main);
  font-weight: 700;
}

.footer-spacer {
  height: 180rpx;
}

/* Empty State */
.empty-cart {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  padding: 0 64rpx;
  margin-top: 150rpx;
}

.empty-icon {
  font-size: 110rpx;
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
  margin-bottom: 48rpx;
}

.shop-btn {
  width: 240rpx;
  height: 80rpx;
  border-radius: 40rpx;
  background: var(--primary-gradient);
  color: #ffffff;
  font-size: 26rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Loading Cart */
.loading-cart {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 60vh;
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

/* Sticky Footer Checkout panel */
.checkout-footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 140rpx;
  padding: 0 32rpx 0 40rpx;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1rpx solid rgba(226,232,240,0.5);
  box-shadow: 0 -8px 24px rgba(0, 0, 0, 0.04);
}

.select-all-wrap {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.select-label {
  font-size: 24rpx;
  color: var(--text-main);
  font-weight: 700;
}

.checkout-info {
  display: flex;
  align-items: center;
  gap: 28rpx;
}

.checkout-info .price-row {
  display: flex;
  align-items: baseline;
}

.total-label {
  font-size: 24rpx;
  color: var(--text-muted);
  font-weight: 600;
}

.total-price-symbol {
  font-size: 24rpx;
  font-weight: 700;
  color: #ef4444;
  margin-right: 2rpx;
}

.total-price-val {
  font-size: 38rpx;
  font-weight: 800;
  color: #ef4444;
  letter-spacing: -0.5rpx;
}

.checkout-btn {
  height: 88rpx;
  padding: 0 44rpx;
  border-radius: 44rpx;
  background: var(--primary-gradient);
  color: #ffffff;
  font-size: 26rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.25);
}

.checkout-btn:active {
  opacity: 0.85;
}

@keyframes spinner {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>
