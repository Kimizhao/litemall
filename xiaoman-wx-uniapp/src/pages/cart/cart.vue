<template>
  <view class="container">
    <!-- Non-logged in state -->
    <view class="no-login" v-if="!hasLogin">
      <view class="c">
        <text class="prompt-text">还没有登录</text>
        <button class="login-btn" @tap="onGoLogin">去登录</button>
      </view>
    </view>

    <!-- Logged in state -->
    <view class="login-workspace" v-else>
      <!-- Empty cart state -->
      <view class="no-cart" v-if="hasLoaded && cartList.length === 0">
        <view class="c">
          <text class="empty-title">空空如也~</text>
          <text class="empty-desc">去添加点什么吧</text>
        </view>
      </view>

      <!-- Cart view (Only rendered when there are items) -->
      <view class="cart-view" v-else-if="cartList.length > 0">
        <scroll-view scroll-y class="cart-scroll">
          <view class="list">
            <view class="group-item">
              <view class="goods">
                <view class="item" :class="{ edit: isEditCart }" v-for="(item, index) in cartList" :key="item.id">
                  <!-- Checkbox selector -->
                  <view class="checkbox-cell" @tap="onCheckToggle(item, index)">
                    <view class="custom-checkbox" :class="{ checked: item.checked }">
                      <text class="check-mark" v-if="item.checked">✓</text>
                    </view>
                  </view>

                  <!-- Product Card Details -->
                  <view class="cart-goods">
                    <image :src="item.picUrl" mode="aspectFill" class="img" />
                    <view class="info">
                      <!-- Normal Mode -->
                      <block v-if="!isEditCart">
                        <view class="t">
                          <text class="name">{{ item.goodsName }}</text>
                          <text class="num">x{{ item.number }}</text>
                        </view>
                        <view class="attr">{{ item.specifications ? item.specifications.join(' ') : '' }}</view>
                        <view class="b">
                          <text class="price">￥{{ item.price }}</text>
                        </view>
                      </block>

                      <!-- Edit Mode -->
                      <block v-else>
                        <view class="attr">已选择: {{ item.specifications ? item.specifications.join(' ') : '' }}</view>
                        <view class="b">
                          <text class="price">￥{{ item.price }}</text>
                          <view class="selnum">
                            <view class="cut" @tap="onQuantityChange(item, index, -1)">-</view>
                            <input type="number" class="number" :value="item.number" disabled />
                            <view class="add" @tap="onQuantityChange(item, index, 1)">+</view>
                          </view>
                        </view>
                      </block>
                    </view>
                  </view>
                </view>
              </view>
            </view>
          </view>
          <view class="footer-spacer"></view>
        </scroll-view>

        <!-- Sticky Bottom Bar -->
        <view class="cart-bottom">
          <view class="select-all-wrap" @tap="onCheckAllToggle">
            <view class="custom-checkbox" :class="{ checked: isAllChecked }">
              <text class="check-mark" v-if="isAllChecked">✓</text>
            </view>
            <text class="select-label">全选（{{ cartTotal.checkedGoodsCount }}）</text>
          </view>

          <view class="total" v-if="!isEditCart">￥{{ cartTotal.checkedGoodsAmount }}</view>
          
          <view class="action_btn_area">
            <view class="action-btn" :class="isEditCart ? 'sure' : 'edit'" @tap="onEditToggle">
              {{ isEditCart ? '完成' : '编辑' }}
            </view>
            <view class="action-btn delete" @tap="onDeleteCart" v-if="isEditCart">
              删除({{ cartTotal.checkedGoodsCount }})
            </view>
            <view class="action-btn checkout" @tap="onCheckout" v-else>
              下单
            </view>
          </view>
        </view>
      </view>

      <!-- Initial Loading state -->
      <view class="loading-wrap" v-else-if="loading || !hasLoaded">
        <text class="loading-text">正在同步购物车...</text>
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
  goodsCount: 0,
  goodsAmount: 0.00,
  checkedGoodsCount: 0,
  checkedGoodsAmount: 0.00
})
const loading = ref(false)
const hasLoaded = ref(false)

// Edit mode and select all helper states
const isEditCart = ref(false)
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
      cartTotal.value = data.cartTotal || { goodsCount: 0, goodsAmount: 0.00, checkedGoodsCount: 0, checkedGoodsAmount: 0.00 }
      isAllChecked.value = cartList.value.length > 0 && cartList.value.every(item => item.checked)
    }
  } catch (err) {
    console.error('Failed to sync Litemall cart:', err)
  } finally {
    loading.value = false
    hasLoaded.value = true
  }
}

// Check onShow lifecycle hook to dynamically reload items
onShow(() => {
  const token = uni.getStorageSync('token')
  if (token) {
    hasLogin.value = true
    isEditCart.value = false // reset edit mode when show
    getCartData()
  } else {
    hasLogin.value = false
    cartList.value = []
    cartTotal.value = { goodsCount: 0, goodsAmount: 0.00, checkedGoodsCount: 0, checkedGoodsAmount: 0.00 }
    isEditCart.value = false
  }
})

const onGoLogin = () => {
  uni.navigateTo({
    url: '/pages/auth/login'
  })
}

// Toggle individual check status
const onCheckToggle = async (item, index) => {
  if (!isEditCart.value) {
    // Normal mode: Sync directly with server
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
        cartTotal.value = data.cartTotal || { goodsCount: 0, goodsAmount: 0.00, checkedGoodsCount: 0, checkedGoodsAmount: 0.00 }
        isAllChecked.value = cartList.value.length > 0 && cartList.value.every(item => item.checked)
      }
    } catch (err) {
      console.error('Failed to toggle check status:', err)
    }
  } else {
    // Edit mode: Local selection change
    cartList.value[index].checked = !cartList.value[index].checked
    isAllChecked.value = cartList.value.length > 0 && cartList.value.every(item => item.checked)
    
    // Update local total checked count
    let count = 0
    cartList.value.forEach(v => {
      if (v.checked) {
        count += v.number
      }
    })
    cartTotal.value.checkedGoodsCount = count
  }
}

// Toggle select all status
const onCheckAllToggle = async () => {
  if (cartList.value.length === 0) return
  const nextChecked = !isAllChecked.value
  const productIds = cartList.value.map(item => item.productId)
  
  if (!isEditCart.value) {
    // Normal mode: Sync directly with server
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
        cartTotal.value = data.cartTotal || { goodsCount: 0, goodsAmount: 0.00, checkedGoodsCount: 0, checkedGoodsAmount: 0.00 }
        isAllChecked.value = nextChecked
      }
    } catch (err) {
      console.error('Failed to toggle select all status:', err)
    }
  } else {
    // Edit mode: Local selection change for all
    cartList.value = cartList.value.map(item => {
      item.checked = nextChecked
      return item
    })
    isAllChecked.value = nextChecked
    
    // Update local total checked count
    let count = 0
    cartList.value.forEach(v => {
      if (v.checked) {
        count += v.number
      }
    })
    cartTotal.value.checkedGoodsCount = count
  }
}

// Adjust quantity
const onQuantityChange = async (item, index, diff) => {
  const nextNum = item.number + diff
  if (nextNum < 1) return
  
  // Local preview change
  cartList.value[index].number = nextNum
  
  if (isEditCart.value) {
    let count = 0
    cartList.value.forEach(v => {
      if (v.checked) {
        count += v.number
      }
    })
    cartTotal.value.checkedGoodsCount = count
  }
  
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
    
    // If not editing, sync standard response
    if (!isEditCart.value && data) {
      cartList.value = data.cartList || []
      cartTotal.value = data.cartTotal || { goodsCount: 0, goodsAmount: 0.00, checkedGoodsCount: 0, checkedGoodsAmount: 0.00 }
    }
  } catch (err) {
    console.error('Failed to update quantity:', err)
  }
}

// Toggle edit/complete mode
const onEditToggle = () => {
  if (isEditCart.value) {
    // Save/complete mode -> re-fetch checked statuses from server
    isEditCart.value = false
    getCartData()
  } else {
    // Enter edit mode -> locally uncheck all items
    isEditCart.value = true
    cartList.value = cartList.value.map(item => {
      item.checked = false
      return item
    })
    isAllChecked.value = false
    cartTotal.value.checkedGoodsCount = 0
  }
}

// Delete checked items batch
const onDeleteCart = async () => {
  const checkedItems = cartList.value.filter(item => item.checked)
  if (checkedItems.length === 0) return
  
  const productIds = checkedItems.map(item => item.productId)
  
  uni.showModal({
    title: '提示',
    content: '确定要删除选中的商品吗？',
    confirmColor: '#E07A5F',
    success: async (res) => {
      if (res.confirm) {
        try {
          const data = await request({
            url: api.CartDelete,
            method: 'POST',
            data: {
              productIds: productIds
            }
          })
          if (data) {
            // Uncheck all remaining items to match litemall-wx behavior
            cartList.value = (data.cartList || []).map(v => {
              v.checked = false
              return v
            })
            cartTotal.value = data.cartTotal || { goodsCount: 0, goodsAmount: 0.00, checkedGoodsCount: 0, checkedGoodsAmount: 0.00 }
            isAllChecked.value = false
            uni.showToast({
              title: '已删除商品',
              icon: 'success'
            })
          }
        } catch (err) {
          console.error('Failed to delete cart items:', err)
        }
      }
    }
  })
}

const onCheckout = () => {
  if (cartTotal.value.checkedGoodsCount === 0) {
    uni.showToast({
      title: '请勾选需要结算的商品',
      icon: 'none'
    })
    return
  }
  
  try {
    uni.setStorageSync('cartId', 0)
    uni.navigateTo({
      url: '/pages/checkout/checkout'
    })
  } catch (e) {
    console.error('Checkout redirect error:', e)
  }
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background-color: var(--bg-primary);
  display: flex;
  flex-direction: column;
}

/* Unlogged in state view */
.no-login {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  padding: 0 64rpx;
  box-sizing: border-box;
}

.no-login .c {
  width: 100%;
  margin-top: 360rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 40rpx;
}

.prompt-text {
  font-size: 34rpx;
  color: var(--text-muted);
  font-weight: 700;
  text-align: center;
}

.login-btn {
  width: 90%;
  height: 96rpx;
  border-radius: 48rpx;
  background-color: #A18276; /* Muted theme gray-brown */
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  letter-spacing: 2rpx;
}

.login-btn:active {
  opacity: 0.85;
}

/* Logged in workspace */
.login-workspace {
  display: flex;
  flex-direction: column;
  flex: 1;
}

/* Empty cart state */
.no-cart {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
}

.no-cart .c {
  width: 100%;
  margin-top: 360rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}

.empty-title {
  font-size: 32rpx;
  font-weight: 800;
  color: var(--text-main);
}

.empty-desc {
  font-size: 26rpx;
  color: var(--text-muted);
  font-weight: 600;
}

/* Cart list container */
.cart-view {
  width: 100%;
  height: auto;
  overflow: hidden;
}

.cart-scroll {
  width: 100%;
  flex: 1;
}

.cart-view .list {
  height: auto;
  width: 100%;
  overflow: hidden;
}

.cart-view .group-item {
  height: auto;
  width: 100%;
  margin-bottom: 18rpx;
}

.cart-view .goods {
  padding: 32rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.cart-view .item {
  display: flex;
  align-items: center;
  padding: 24rpx;
  border-radius: var(--radius-md);
  background-color: #ffffff;
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
  box-sizing: border-box;
}

/* Checkbox alignment */
.checkbox-cell {
  padding: 20rpx 20rpx 20rpx 0;
}

.custom-checkbox {
  width: 42rpx;
  height: 42rpx;
  border-radius: 50%;
  border: 4rpx solid #E6D5B8;
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

/* Product Row info */
.cart-goods {
  flex: 1;
  display: flex;
  align-items: center;
}

.cart-view .item .img {
  width: 130rpx;
  height: 130rpx;
  border-radius: var(--radius-sm);
  background-color: var(--bg-primary);
  margin-right: 24rpx;
}

.cart-view .item .info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 130rpx;
}

.cart-view .item .t {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.cart-view .item .name {
  font-size: 26rpx;
  font-weight: 700;
  color: var(--text-main);
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  max-width: 320rpx;
}

.cart-view .item .num {
  font-size: 24rpx;
  color: var(--text-muted);
  font-weight: 700;
}

.cart-view .item .attr {
  font-size: 22rpx;
  color: var(--text-muted);
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cart-view .item .b {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.cart-view .item .price {
  font-size: 28rpx;
  font-weight: 800;
  color: #E07A5F;
}

/* Edit State adjustment */
.cart-view .item.edit .attr {
  font-weight: 700;
  color: var(--text-muted);
  text-align: right;
  height: 38rpx;
  line-height: 38rpx;
}

.cart-view .item.edit .b {
  height: 52rpx;
}

/* Stepper Quantity selection */
.selnum {
  width: 200rpx;
  height: 52rpx;
  border: 2rpx solid #E6D5B8;
  border-radius: 26rpx;
  overflow: hidden;
  display: flex;
  background-color: #ffffff;
}

.selnum .cut {
  width: 60rpx;
  height: 100%;
  text-align: center;
  line-height: 48rpx;
  font-size: 28rpx;
  font-weight: bold;
  background-color: #FAF6EE;
  color: var(--text-main);
}

.selnum .cut:active, .selnum .add:active {
  background-color: #E6D5B8;
}

.selnum .number {
  flex: 1;
  height: 100%;
  text-align: center;
  line-height: 48rpx;
  font-size: 24rpx;
  color: var(--text-main);
  font-weight: 700;
  border-left: 2rpx solid #E6D5B8;
  border-right: 2rpx solid #E6D5B8;
}

.selnum .add {
  width: 60rpx;
  height: 100%;
  text-align: center;
  line-height: 48rpx;
  font-size: 28rpx;
  font-weight: bold;
  background-color: #FAF6EE;
  color: var(--text-main);
}

.footer-spacer {
  height: 160rpx;
}

/* Sticky Bottom Bar */
.cart-bottom {
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 110rpx;
  background-color: #ffffff;
  border-top: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  padding: 0 32rpx;
  box-sizing: border-box;
  z-index: 100;
}

.select-all-wrap {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.select-label {
  font-size: 26rpx;
  color: var(--text-main);
  font-weight: 700;
}

.cart-bottom .total {
  font-size: 32rpx;
  font-weight: 800;
  color: #E07A5F;
  flex: 1;
  text-align: right;
  padding-right: 32rpx;
}

.action_btn_area {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-left: auto;
}

.action-btn {
  height: 72rpx;
  padding: 0 32rpx;
  border-radius: 36rpx;
  font-size: 24rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  letter-spacing: 2rpx;
  transition: opacity 0.2s ease;
}

.action-btn:active {
  opacity: 0.85;
}

.action-btn.edit {
  background-color: #FAF6EE;
  color: #6F4E37;
  border: 1px solid #E6D5B8;
}

.action-btn.sure {
  background-color: #6F4E37;
  color: #ffffff;
}

.action-btn.checkout {
  background: var(--primary-gradient);
  color: #ffffff;
  box-shadow: 0 4rpx 12rpx rgba(224, 122, 95, 0.25);
}

.action-btn.delete {
  background-color: #E07A5F;
  color: #ffffff;
  box-shadow: 0 4rpx 12rpx rgba(224, 122, 95, 0.25);
}

.loading-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 60vh;
}

.loading-text {
  font-size: 24rpx;
  color: var(--text-muted);
  font-weight: 500;
}
</style>
