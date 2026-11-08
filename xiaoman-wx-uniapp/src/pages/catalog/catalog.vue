<template>
  <view class="container">
    <!-- 1. 顶部店铺卡片区域 -->
    <view class="shop-header">
      <view class="shop-header-bg">
        <image :src="shopHeaderBg" class="shop-header-bg-img" mode="widthFix"></image>
      </view>
      <view class="shop-header-bg-overlay"></view>
      <view class="shop-header-content">
        <!-- <view class="shop-avatar">满</view> -->
        <view class="shop-info">
          <!-- <text class="shop-name">小满食光</text> -->
          <text class="shop-subtitle">三明治 · 紫米饭团 · 饮品</text>
        </view>
      </view>
    </view>

    <!-- 2. 试营业滚动公告栏 -->
    <!-- <view class="announcement-bar">
      <text class="announcement-icon">🎉</text>
      <view class="announcement-text-wrap">
        <view class="announcement-text">新店试营业，全场 8.8 折！微信扫码免费领饭团！</view>
      </view>
    </view> -->

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
          <text class="category-label">{{ item.name }}</text>
        </view>
      </scroll-view>

      <!-- 3.2 右栏：内容展示区 -->
      <view class="right-content">
        <!-- 头部标题与二级类目胶囊栏 (常驻吸顶) -->
        <view class="right-header">
          <view class="section-title">
            <text class="section-indicator"></text>
            <text>{{ currentCategoryName }}</text>
          </view>

          <!-- 二级类目横向胶囊栏 (仅在有效二级类目 >= 2 时智能展示) -->
          <scroll-view 
            scroll-x 
            class="subcategory-scroll" 
            v-if="validSubCategories.length >= 2"
            :show-scrollbar="false"
            enhanced
          >
            <view class="subcategory-bar">
              <view 
                class="subcat-pill" 
                :class="{ active: activeSubCategoryId === null }"
                @tap="onSubCategorySelect(null)"
              >
                <text class="subcat-pill-text">全部</text>
              </view>
              <view 
                v-for="sub in validSubCategories" 
                :key="sub.id"
                class="subcat-pill"
                :class="{ active: activeSubCategoryId === sub.id }"
                @tap="onSubCategorySelect(sub.id)"
              >
                <text class="subcat-pill-text">{{ sub.name }}</text>
              </view>
            </view>
          </scroll-view>
        </view>

        <!-- 商品列表 -->
        <scroll-view scroll-y class="goods-list" :scroll-top="goodsScrollTop">
          <view 
            class="goods-item" 
            :class="{ 'sold-out': item.isSoldOut }"
            v-for="item in filteredGoods" 
            :key="item.id" 
            @tap="onGoodsTap(item)"
          >
            <view class="goods-img-wrapper">
              <image :src="item.picUrl" mode="aspectFill" class="goods-img" />
            </view>
            <view class="goods-info">
              <view class="goods-name-row">
                <text class="goods-name">{{ item.name }}</text>
                <text class="goods-desc">{{ item.brief }}</text>
              </view>

              <!-- 低库存微标 (仅在未售罄且 1 <= totalStock <= 3 时展示) -->
              <view class="stock-badge" v-if="!item.isSoldOut && item.totalStock && item.totalStock <= LOW_STOCK_THRESHOLD">
                <text class="stock-badge-text">仅剩{{ item.totalStock }}份</text>
              </view>

              <view class="goods-action-row">
                <view class="price-container">
                  <text class="price-discount">{{ item.retailPrice }}</text>
                  <text class="price-original" v-if="item.counterPrice && item.counterPrice > item.retailPrice">{{ item.counterPrice }}</text>
                </view>

                <!-- 已售罄标识 (参考截图居右展示) -->
                <view class="sold-out-tag" v-if="item.isSoldOut">
                  <text class="sold-out-text">已售罄</text>
                </view>
              </view>
            </view>
          </view>

          <view class="empty-state" v-if="filteredGoods.length === 0">
            <text class="empty-text">正在制作美味中...</text>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import request from '../../utils/request'
import api from '../../config/api'

const shopHeaderBg = `/static/xiaoman/shop_header.jpg`

// 核心状态变量
const hasLogin = ref(false)
const goodsList = ref([])

// 低库存紧张预警阈值 (<= 3 份触发微标展示)
const LOW_STOCK_THRESHOLD = 3

const activeCategoryId = ref(null) // 默认选中分类 ID，动态设定
const activeSubCategoryId = ref(null) // 选中的二级分类 ID (null 代表“全部”)
const goodsScrollTop = ref(0) // 控制商品列表滚动复位

const categories = ref([])

const currentCategoryName = computed(() => {
  const cat = categories.value.find(item => item.id === activeCategoryId.value)
  return cat ? cat.name : ''
})

// 计算当前一级分类下具有在售商品的有效二级分类列表 (过滤掉在售商品数为 0 的二级类目)
const validSubCategories = computed(() => {
  const activeCat = categories.value.find(item => item.id === activeCategoryId.value)
  if (!activeCat || !activeCat.l2SubCategories) return []

  return activeCat.l2SubCategories.filter(sub => {
    return goodsList.value.some(item => item.categoryId === sub.id)
  })
})

// 根据当前一级分类与选中的二级分类过滤商品，并执行“售罄商品自动沉底”
const filteredGoods = computed(() => {
  const activeCat = categories.value.find(item => item.id === activeCategoryId.value)
  if (!activeCat || !activeCat.l2SubCategories) return []

  let list = []
  // 若选定了具体二级分类，则精确过滤
  if (activeSubCategoryId.value !== null) {
    list = goodsList.value.filter(item => item.categoryId === activeSubCategoryId.value)
  } else {
    // 默认“全部”模式：展示该一级分类下所有二级子分类对应的商品
    const subIds = activeCat.l2SubCategories.map(sub => sub.id)
    list = goodsList.value.filter(item => subIds.includes(item.categoryId))
  }

  // 排序：有货商品在前，已售罄商品自动沉底至当前类目末尾
  return [...list].sort((a, b) => {
    const aSold = a.isSoldOut ? 1 : 0
    const bSold = b.isSoldOut ? 1 : 0
    return aSold - bSold
  })
})

// 动态从后台加载分类与对应的商品列表
const loadGoodsData = async () => {
  try {
    // 1. 获取所有分类目录
    const resCatalog = await request({ url: api.CatalogAll, method: 'GET' })
    const l1List = resCatalog.categoryList || []
    const allL2List = resCatalog.allList || {}
    
    // 2. 动态构建分类列表
    categories.value = l1List.map(l1 => {
      const l2Sub = allL2List[l1.id] || []
      const firstL2Id = l2Sub.length > 0 ? l2Sub[0].id : null
      return {
        id: l1.id,
        name: l1.name, // 直接使用包含 Emoji 的完整数据库类目名称
        l2Id: firstL2Id,
        l2SubCategories: l2Sub
      }
    })
    
    // 3. 设置默认选中的一级分类
    if (!activeCategoryId.value && categories.value.length > 0) {
      activeCategoryId.value = categories.value[0].id
    }
    
    // 4. 收集所有二级分类并加载对应的商品
    const l2Ids = []
    categories.value.forEach(cat => {
      if (cat.l2SubCategories) {
        cat.l2SubCategories.forEach(sub => {
          if (sub.id && !l2Ids.includes(sub.id)) {
            l2Ids.push(sub.id)
          }
        })
      }
    })
    
    if (l2Ids.length > 0) {
      const goodsPromises = l2Ids.map(id => 
        request({ url: `${api.GoodsList}?categoryId=${id}&page=1&limit=100`, method: 'GET' })
          .then(res => ((res && res.list) || []).map(item => ({ ...item, categoryId: id })))
          .catch(err => {
            console.error(`Failed to load goods for category ${id}:`, err)
            return []
          })
      )
      
      const results = await Promise.all(goodsPromises)
      goodsList.value = results.reduce((acc, curr) => acc.concat(curr), [])
    } else {
      goodsList.value = []
    }
  } catch (err) {
    console.error('Failed to load Xiaoman goods data dynamically:', err)
  }
}

// 检查登录状态并初始化页面
onShow(async () => {
  const token = uni.getStorageSync('token')
  if (token) {
    hasLogin.value = true
  } else {
    hasLogin.value = false
  }
  
  await loadGoodsData()
})

// 下拉刷新
onPullDownRefresh(async () => {
  await loadGoodsData()
  uni.stopPullDownRefresh()
})

const resetScrollTop = () => {
  goodsScrollTop.value = goodsScrollTop.value === 0 ? 0.01 : 0
}

const onCategorySelect = (id) => {
  if (activeCategoryId.value === id) return
  activeCategoryId.value = id
  activeSubCategoryId.value = null // 切换一级分类自动重置二级为“全部”
  resetScrollTop()
}

const onSubCategorySelect = (subId) => {
  if (activeSubCategoryId.value === subId) return
  activeSubCategoryId.value = subId
  resetScrollTop()
}

// 跳转商品详情页（若已售罄则拦截并轻提示）
const onGoodsTap = (item) => {
  if (item.isSoldOut) {
    uni.showToast({
      title: '该餐品今日已售罄',
      icon: 'none',
      duration: 1500
    })
    return
  }
  uni.navigateTo({
    url: `/pages/goods/goods?id=${item.id}`
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
  overflow: hidden;
  z-index: 1;
}

.shop-header-bg-img {
  width: 100%;
  display: block;
}

.shop-header-bg-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(to bottom, rgba(74, 53, 37, 0.1), rgba(74, 53, 37, 0.8));
  z-index: 2;
}

.shop-header-content {
  position: relative;
  display: flex;
  align-items: center;
  gap: 24rpx;
  z-index: 3;
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
  height: 100%;
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

/* 3.2 右栏：内容展示区 */
.right-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #FAF6EE;
  overflow: hidden;
  height: 100%;
}

.right-header {
  padding: 24rpx 24rpx 16rpx 24rpx;
  background-color: #FAF6EE;
  flex-shrink: 0;
  z-index: 10;
}

.section-title {
  font-size: 28rpx;
  font-weight: 800;
  color: #6F4E37;
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

/* 二级类目横向滑动胶囊栏 */
.subcategory-scroll {
  width: 100%;
  white-space: nowrap;
  margin-top: 16rpx;
}

.subcategory-bar {
  display: inline-flex;
  align-items: center;
  gap: 14rpx;
  padding-right: 24rpx;
}

.subcat-pill {
  padding: 8rpx 24rpx;
  border-radius: 999rpx;
  background-color: #F5EFE6;
  border: 1rpx solid rgba(230, 213, 184, 0.6);
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
}

.subcat-pill.active {
  background-color: #6F4E37;
  border-color: #6F4E37;
  box-shadow: 0 4rpx 10rpx rgba(111, 78, 55, 0.18);
}

.subcat-pill-text {
  font-size: 24rpx;
  color: #A18276;
  font-weight: 500;
  white-space: nowrap;
}

.subcat-pill.active .subcat-pill-text {
  color: #FFFFFF;
  font-weight: 700;
}

/* 3.3 右栏单品列表 */
.goods-list {
  flex: 1;
  background-color: #FAF6EE;
  padding: 8rpx 24rpx 24rpx 24rpx;
  box-sizing: border-box;
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
  transition: all 0.2s ease;
}

.goods-item:active {
  opacity: 0.9;
  background-color: #FAF6EE;
  transform: scale(0.99);
}

/* 售罄卡片弱化与交互样式 */
.goods-item.sold-out {
  opacity: 0.6;
}

.goods-item.sold-out:active {
  transform: none;
  background-color: #FFFFFF;
  opacity: 0.6;
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

/* 低库存紧张提醒微标 */
.stock-badge {
  display: inline-flex;
  align-items: center;
  align-self: flex-start;
  margin-top: 8rpx;
  background-color: rgba(224, 122, 95, 0.1);
  padding: 2rpx 12rpx;
  border-radius: 6rpx;
}

.stock-badge-text {
  font-size: 20rpx;
  color: #E07A5F;
  font-weight: 600;
  line-height: 1.2;
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

/* 已售罄标识 */
.sold-out-tag {
  display: flex;
  align-items: center;
}

.sold-out-text {
  font-size: 24rpx;
  color: #A18276;
  font-weight: 600;
  letter-spacing: 1rpx;
}

/* Empty state */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 120rpx;
  gap: 16rpx;
}

.empty-text {
  font-size: 26rpx;
  color: #A18276;
}
</style>
