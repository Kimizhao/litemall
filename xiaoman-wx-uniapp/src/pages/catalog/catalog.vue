<template>
  <view class="container">
    <!-- 1. 顶部店铺卡片区域 -->
    <view class="shop-header">
      <view class="shop-header-bg"></view>
      <view class="shop-header-content">
        <!-- <view class="shop-avatar">满</view> -->
        <view class="shop-info">
          <!-- <text class="shop-name">小满食光</text> -->
          <text class="shop-subtitle">三明治 · 简餐 · 饮品</text>
        </view>
      </view>
    </view>

    <!-- 2. 试营业滚动公告栏 -->
    <view class="announcement-bar">
      <text class="announcement-icon">🎉</text>
      <view class="announcement-text-wrap">
        <view class="announcement-text">新店试营业，全场 8.8 折！微信扫码免费领饭团！</view>
      </view>
    </view>

    <!-- 3. 双栏自适应点餐区 -->
    <view class="menu-container">
      <!-- 3.1 左栏：分类导航 -->
      <scroll-view scroll-y class="sidebar">
        <view 
          v-for="item in categories" 
          :key="item.id"
          class="category-tab"
          :class="{ active: activeCategoryId === item.id }"
          @tap="onCategorySelect(item.id)"
        >
          <text class="category-icon">{{ item.icon }}</text>
          <text class="category-label">{{ item.name }}</text>
        </view>
      </scroll-view>

      <!-- 3.2 右栏：商品列表 -->
      <scroll-view scroll-y class="goods-list">
        <view class="section-title">
          <text class="section-indicator"></text>
          <text>{{ currentCategoryName }}</text>
        </view>

        <view class="goods-item" v-for="item in filteredGoods" :key="item.id">
          <view class="goods-img-wrapper">
            <image :src="item.picUrl" mode="aspectFill" class="goods-img" />
          </view>
          <view class="goods-info">
            <view class="goods-name-row">
              <text class="goods-name">{{ item.name }}</text>
              <text class="goods-desc">{{ item.brief }}</text>
            </view>
            <view class="goods-action-row">
              <view class="price-container">
                <text class="price-discount">{{ item.retailPrice }}</text>
                <text class="price-original" v-if="item.counterPrice && item.counterPrice > item.retailPrice">{{ item.counterPrice }}</text>
              </view>
              <!-- 数量加减组件 -->
              <view class="quantity-selector">
                <view 
                  class="btn-minus" 
                  v-if="getGoodsCountInCart(item.id) > 0" 
                  @tap.stop="decreaseCart(item)"
                >-</view>
                <text 
                  class="quantity-count" 
                  v-if="getGoodsCountInCart(item.id) > 0"
                >{{ getGoodsCountInCart(item.id) }}</text>
                <view class="btn-add" @tap.stop="addToCart(item)">+</view>
              </view>
            </view>
          </view>
        </view>

        <view class="empty-state" v-if="filteredGoods.length === 0">
          <text class="empty-icon">🥪</text>
          <text class="empty-text">正在制作美味中...</text>
        </view>
      </scroll-view>
    </view>

    <!-- 4. 底部常驻购物车栏 -->
    <view class="cart-bar-container" :class="{ show: cartTotal.checkedGoodsCount > 0 }">
      <view class="cart-bar">
        <view class="cart-left" @tap="toggleCartDrawer">
          <view class="cart-icon-wrapper">
            <text class="cart-icon">🛒</text>
            <view class="cart-badge">{{ cartTotal.checkedGoodsCount }}</view>
          </view>
          <view class="cart-price-info">
            <text class="cart-price">¥{{ cartTotal.checkedGoodsAmount }}</text>
            <text class="cart-original-price" v-if="cartOriginalTotal > cartTotal.checkedGoodsAmount">¥{{ cartOriginalTotal.toFixed(1) }}</text>
          </view>
        </view>
        <button class="btn-checkout" @tap="onCheckout">立即预订</button>
      </view>
    </view>

    <!-- 5. 购物车抽屉详情弹窗 -->
    <view class="cart-drawer-mask" v-if="showCartDrawer" @tap="toggleCartDrawer"></view>
    <view class="cart-drawer" :class="{ open: showCartDrawer }">
      <view class="drawer-header">
        <text class="drawer-title">已选商品</text>
        <view class="btn-clear" @tap="clearCart">
          <text class="clear-icon">🗑️</text>
          <text>清空购物车</text>
        </view>
      </view>
      <scroll-view scroll-y class="drawer-list">
        <view class="drawer-item" v-for="item in cartList" :key="item.id">
          <text class="drawer-goods-name">{{ item.goodsName }}</text>
          <view class="drawer-goods-right">
            <text class="drawer-goods-price">¥{{ (item.price * item.number).toFixed(1) }}</text>
            <view class="quantity-selector">
              <view class="btn-minus" @tap.stop="decreaseCart(item)">-</view>
              <text class="quantity-count">{{ item.number }}</text>
              <view class="btn-add" @tap.stop="addToCart(item)">+</view>
            </view>
          </view>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import request from '../../utils/request'
import api from '../../config/api'

// 核心状态变量
const hasLogin = ref(false)
const goodsList = ref([])
const cartList = ref([])
const cartTotal = ref({
  checkedGoodsCount: 0,
  checkedGoodsAmount: 0.00
})
const activeCategoryId = ref(1001000) // 默认选中三明治
const showCartDrawer = ref(false)

// 分类硬编码定义（关联二级分类ID）
const categories = ref([
  { id: 1001000, name: '招牌三明治', icon: '🥪', l2Id: 1001001 },
  { id: 1002000, name: '手作简餐', icon: '🍙', l2Id: 1002001 },
  { id: 1003000, name: '现榨饮品', icon: '🍹', l2Id: 1003001 }
])

const currentCategoryName = computed(() => {
  const cat = categories.value.find(item => item.id === activeCategoryId.value)
  return cat ? cat.name : ''
})

// 计算购物车原始总金额（未折价前原价的总和，从 goodsList 中匹配 counterPrice）
const cartOriginalTotal = computed(() => {
  let total = 0
  cartList.value.forEach(cartItem => {
    const goods = goodsList.value.find(g => g.id === cartItem.goodsId)
    if (goods && goods.counterPrice) {
      total += goods.counterPrice * cartItem.number
    } else {
      total += cartItem.price * cartItem.number
    }
  })
  return total
})

// 根据当前一级分类对应的二级分类过滤商品
const filteredGoods = computed(() => {
  let targetL2Id = 1001001
  if (activeCategoryId.value === 1002000) targetL2Id = 1002001
  else if (activeCategoryId.value === 1003000) targetL2Id = 1003001
  
  return goodsList.value.filter(item => item.categoryId === targetL2Id)
})

// 一次性从后台精准加载小满食光的 10 款商品
const loadGoodsData = async () => {
  try {
    const [res1, res2, res3] = await Promise.all([
      request({ url: `${api.GoodsList}?categoryId=1001001&page=1&limit=100`, method: 'GET' }),
      request({ url: `${api.GoodsList}?categoryId=1002001&page=1&limit=100`, method: 'GET' }),
      request({ url: `${api.GoodsList}?categoryId=1003001&page=1&limit=100`, method: 'GET' })
    ])
    
    const list1 = ((res1 && res1.list) || []).map(item => ({ ...item, categoryId: 1001001 }))
    const list2 = ((res2 && res2.list) || []).map(item => ({ ...item, categoryId: 1002001 }))
    const list3 = ((res3 && res3.list) || []).map(item => ({ ...item, categoryId: 1003001 }))
    
    goodsList.value = [...list1, ...list2, ...list3]
  } catch (err) {
    console.error('Failed to load Xiaoman goods data:', err)
  }
}

// 同步获取购物车接口数据
const getCartData = async () => {
  if (!hasLogin.value) return
  try {
    const data = await request({
      url: api.CartList,
      method: 'GET'
    })
    if (data) {
      cartList.value = data.cartList || []
      cartTotal.value = data.cartTotal || { checkedGoodsCount: 0, checkedGoodsAmount: 0.00 }
      
      // 如果购物车里数量为空，则自动关闭购物车抽屉
      if (cartTotal.value.checkedGoodsCount === 0) {
        showCartDrawer.value = false
      }
    }
  } catch (err) {
    console.error('Failed to get cart list:', err)
  }
}

// 检查登录状态并初始化页面
onShow(async () => {
  const token = uni.getStorageSync('token')
  if (token) {
    hasLogin.value = true
  } else {
    hasLogin.value = false
    cartList.value = []
    cartTotal.value = { checkedGoodsCount: 0, checkedGoodsAmount: 0.00 }
  }
  
  await loadGoodsData()
  if (hasLogin.value) {
    await getCartData()
  }
})

// 下拉刷新
onPullDownRefresh(async () => {
  await loadGoodsData()
  if (hasLogin.value) {
    await getCartData()
  }
  uni.stopPullDownRefresh()
})

const onCategorySelect = (id) => {
  activeCategoryId.value = id
}

// 查询某件商品在购物车中的加购数量
const getGoodsCountInCart = (goodsId) => {
  const item = cartList.value.find(i => i.goodsId === goodsId)
  return item ? item.number : 0
}

// 添加至购物车 logic
const addToCart = async (goods) => {
  if (!hasLogin.value) {
    uni.navigateTo({
      url: '/pages/auth/login'
    })
    return
  }

  // 区分传入的是 GoodsList 的商品还是 CartList 的商品字段（如果是 CartList，其商品ID放在 goodsId 中）
  const goodsId = goods.goodsId || goods.id
  const cartItem = cartList.value.find(item => item.goodsId === goodsId)
  
  try {
    if (cartItem) {
      await request({
        url: api.CartUpdate,
        method: 'POST',
        data: {
          id: cartItem.id,
          goodsId: goodsId,
          productId: goodsId,
          number: cartItem.number + 1
        }
      })
    } else {
      await request({
        url: api.CartAdd,
        method: 'POST',
        data: {
          goodsId: goodsId,
          productId: goodsId,
          number: 1
        }
      })
    }
    await getCartData()
  } catch (err) {
    console.error('Failed to add cart item:', err)
  }
}

// 从购物车中递减 logic
const decreaseCart = async (goods) => {
  if (!hasLogin.value) return
  
  const goodsId = goods.goodsId || goods.id
  const cartItem = cartList.value.find(item => item.goodsId === goodsId)
  if (!cartItem) return

  try {
    if (cartItem.number === 1) {
      await request({
        url: api.CartDelete,
        method: 'POST',
        data: {
          productIds: [goodsId]
        }
      })
    } else {
      await request({
        url: api.CartUpdate,
        method: 'POST',
        data: {
          id: cartItem.id,
          goodsId: goodsId,
          productId: goodsId,
          number: cartItem.number - 1
        }
      })
    }
    await getCartData()
  } catch (err) {
    console.error('Failed to decrease cart item:', err)
  }
}

const toggleCartDrawer = () => {
  if (cartTotal.value.checkedGoodsCount > 0) {
    showCartDrawer.value = !showCartDrawer.value
  }
}

// 清空购物车 logic
const clearCart = async () => {
  if (cartList.value.length === 0) return
  const productIds = cartList.value.map(item => item.productId)
  try {
    await request({
      url: api.CartDelete,
      method: 'POST',
      data: {
        productIds: productIds
      }
    })
    showCartDrawer.value = false
    await getCartData()
  } catch (err) {
    console.error('Failed to clear cart:', err)
  }
}

// 立即预订跳转
const onCheckout = () => {
  if (!hasLogin.value) {
    uni.navigateTo({
      url: '/pages/auth/login'
    })
    return
  }
  uni.navigateTo({
    url: '/pages/checkout/checkout'
  })
}
</script>

<style scoped>
.container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #FAF6EE;
  color: #4A3525;
}

/* 1. 店铺头部区域 */
.shop-header {
  position: relative;
  height: 260rpx;
  display: flex;
  align-items: flex-end;
  padding: 40rpx;
  overflow: hidden;
}

.shop-header-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image: linear-gradient(to bottom, rgba(74, 53, 37, 0.1), rgba(74, 53, 37, 0.8)), 
                    url('/static/xiaoman/shop_header.jpg');
  background-size: cover;
  background-position: center top;
  z-index: 1;
}

.shop-header-content {
  position: relative;
  display: flex;
  align-items: center;
  gap: 24rpx;
  z-index: 2;
  color: #FFFFFF;
}

.shop-avatar {
  width: 110rpx;
  height: 110rpx;
  border-radius: 50%;
  background-color: #FFFFFF;
  color: #6F4E37;
  font-size: 56rpx;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 4rpx solid #E6D5B8;
  box-shadow: 0 6rpx 20rpx rgba(111, 78, 55, 0.25);
}

.shop-name {
  font-size: 42rpx;
  font-weight: 800;
  letter-spacing: 2rpx;
  text-shadow: 0 2rpx 6rpx rgba(0,0,0,0.5);
}

.shop-subtitle {
  font-size: 26rpx;
  opacity: 0.95;
  margin-top: 6rpx;
  text-shadow: 0 2rpx 4rpx rgba(0,0,0,0.4);
}

/* 2. 试营业滚动公告栏 */
.announcement-bar {
  background-color: #FFEFC6;
  color: #8C6430;
  padding: 12rpx 30rpx;
  font-size: 24rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 12rpx;
  border-bottom: 1rpx solid #E6D5B8;
  z-index: 10;
}

.announcement-text-wrap {
  flex: 1;
  overflow: hidden;
}

.announcement-text {
  white-space: nowrap;
  animation: marquee 18s linear infinite;
  display: inline-block;
}

@keyframes marquee {
  0% { transform: translateX(100%); }
  100% { transform: translateX(-100%); }
}

/* 3. 双栏自适应点餐区 */
.menu-container {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* 3.1 左栏品类 */
.sidebar {
  width: 170rpx;
  background-color: #F5EEDF;
  border-right: 1rpx solid #E6D5B8;
}

.category-tab {
  padding: 36rpx 12rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
  border-bottom: 1rpx solid rgba(230, 213, 184, 0.2);
  transition: all 0.2s ease;
}

.category-icon {
  font-size: 40rpx;
  transition: transform 0.2s ease;
}

.category-label {
  font-size: 24rpx;
  font-weight: 600;
  color: #A18276;
}

.category-tab.active {
  background-color: #FAF6EE;
  position: relative;
}

.category-tab.active .category-icon {
  transform: scale(1.15);
}

.category-tab.active .category-label {
  color: #6F4E37;
  font-weight: 800;
}

.category-tab.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 25%;
  height: 50%;
  width: 8rpx;
  background-color: #6F4E37;
  border-radius: 0 4rpx 4rpx 0;
}

/* 3.2 右栏单品列表 */
.goods-list {
  flex: 1;
  background-color: #FAF6EE;
  padding: 24rpx 24rpx 140rpx 24rpx; /* 底部预留空间给购物车 */
}

.section-title {
  font-size: 28rpx;
  font-weight: 800;
  color: #6F4E37;
  margin-bottom: 24rpx;
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.section-indicator {
  width: 6rpx;
  height: 28rpx;
  background-color: #6F4E37;
  border-radius: 3rpx;
}

.goods-item {
  display: flex;
  gap: 20rpx;
  background-color: #FFFFFF;
  padding: 24rpx;
  border-radius: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(111, 78, 55, 0.04);
  border: 1rpx solid rgba(230, 213, 184, 0.2);
}

.goods-img-wrapper {
  width: 144rpx;
  height: 144rpx;
  border-radius: 16rpx;
  overflow: hidden;
  border: 1rpx solid rgba(230, 213, 184, 0.3);
  background-color: #FAF6EE;
  flex-shrink: 0;
}

.goods-img {
  width: 100%;
  height: 100%;
}

.goods-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.goods-name-row {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.goods-name {
  font-size: 28rpx;
  font-weight: 800;
  color: #4A3525;
}

.goods-desc {
  font-size: 22rpx;
  color: #A18276;
  line-height: 1.3;
}

.goods-action-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10rpx;
}

.price-container {
  display: flex;
  align-items: baseline;
  gap: 8rpx;
}

.price-discount {
  font-size: 34rpx;
  font-weight: 800;
  color: #E07A5F;
}

.price-discount::before {
  content: '¥';
  font-size: 22rpx;
  font-weight: bold;
}

.price-original {
  font-size: 22rpx;
  color: #A18276;
  text-decoration: line-through;
}

.price-original::before {
  content: '¥';
}

/* 数量选择加减组件 */
.quantity-selector {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.btn-minus {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  border: 2rpx solid #E6D5B8;
  background-color: #FFFFFF;
  color: #6F4E37;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  font-weight: bold;
  box-shadow: 0 2rpx 6rpx rgba(111, 78, 55, 0.05);
}

.btn-minus:active {
  transform: scale(0.95);
}

.quantity-count {
  font-size: 28rpx;
  font-weight: bold;
  color: #4A3525;
  min-width: 30rpx;
  text-align: center;
}

.btn-add {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background-color: #E07A5F;
  color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  font-weight: bold;
  box-shadow: 0 4rpx 10rpx rgba(224, 122, 95, 0.3);
}

.btn-add:active {
  transform: scale(0.95);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 120rpx;
  gap: 16rpx;
}

.empty-icon {
  font-size: 80rpx;
}

.empty-text {
  font-size: 26rpx;
  color: #A18276;
}

/* 4. 底部悬浮购物车栏 */
.cart-bar-container {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 30rpx 40rpx;
  pointer-events: none;
  z-index: 90;
}

.cart-bar {
  height: 100rpx;
  background-color: rgba(74, 53, 37, 0.96);
  backdrop-filter: blur(10rpx);
  border-radius: 50rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 10rpx 0 32rpx;
  pointer-events: auto;
  box-shadow: 0 12rpx 36rpx rgba(74, 53, 37, 0.25);
  border: 1rpx solid rgba(230, 213, 184, 0.15);
  transform: translateY(180rpx);
  opacity: 0;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.cart-bar-container.show .cart-bar {
  transform: translateY(0);
  opacity: 1;
}

.cart-left {
  display: flex;
  align-items: center;
  gap: 24rpx;
  color: #FFFFFF;
}

.cart-icon-wrapper {
  position: relative;
}

.cart-icon {
  font-size: 44rpx;
}

.cart-badge {
  position: absolute;
  top: -12rpx;
  right: -12rpx;
  background-color: #E07A5F;
  color: #FFFFFF;
  font-size: 20rpx;
  font-weight: bold;
  padding: 2rpx 10rpx;
  border-radius: 20rpx;
  border: 2rpx solid #4A3525;
}

.cart-price-info {
  display: flex;
  align-items: baseline;
  gap: 12rpx;
}

.cart-price {
  font-size: 38rpx;
  font-weight: 800;
  color: #FFFFFF;
}

.cart-original-price {
  font-size: 22rpx;
  color: rgba(255,255,255,0.5);
  text-decoration: line-through;
}

.btn-checkout {
  height: 80rpx;
  line-height: 80rpx;
  background-color: #E07A5F;
  color: #FFFFFF;
  font-size: 28rpx;
  font-weight: 800;
  border-radius: 40rpx;
  padding: 0 44rpx;
  border: none;
  box-shadow: 0 4rpx 12rpx rgba(224, 122, 95, 0.2);
  margin: 0 !important;
  width: auto !important;
}

.btn-checkout:active {
  transform: scale(0.95);
}

/* 5. 购物车详情弹窗抽屉 */
.cart-drawer-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(74, 53, 37, 0.4);
  backdrop-filter: blur(4rpx);
  z-index: 80;
}

.cart-drawer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: #FAF6EE;
  border-radius: 40rpx 40rpx 0 0;
  z-index: 85;
  box-shadow: 0 -8rpx 32rpx rgba(74, 53, 37, 0.15);
  transform: translateY(100%);
  transition: transform 0.35s cubic-bezier(0.16, 1, 0.3, 1);
  padding: 40rpx 40rpx 300rpx 40rpx;
  border-top: 1rpx solid #E6D5B8;
  box-sizing: border-box;
}

.cart-drawer.open {
  transform: translateY(0);
}

.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 2rpx solid #E6D5B8;
  padding-bottom: 20rpx;
  margin-bottom: 20rpx;
}

.drawer-title {
  font-size: 30rpx;
  font-weight: 800;
  color: #6F4E37;
}

.btn-clear {
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-size: 24rpx;
  color: #A18276;
}

.clear-icon {
  font-size: 26rpx;
}

.drawer-list {
  max-height: 480rpx;
}

.drawer-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid rgba(230, 213, 184, 0.3);
}

.drawer-goods-name {
  font-size: 28rpx;
  font-weight: bold;
  color: #4A3525;
}

.drawer-goods-right {
  display: flex;
  align-items: center;
  gap: 30rpx;
}

.drawer-goods-price {
  font-size: 28rpx;
  font-weight: 800;
  color: #E07A5F;
  min-width: 100rpx;
  text-align: right;
}
</style>
