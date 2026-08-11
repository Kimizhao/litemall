<template>
  <view class="container" v-if="goods">
    <!-- Custom Navigation Bar -->
    <view class="nav-bar">
      <view class="status-bar" :style="{ height: statusBarHeight + 'px' }"></view>
      <view class="nav-content">
        <view class="nav-left" @tap="onBackTap">
          <view class="back-icon"></view>
        </view>
        <view class="nav-title">商品详情</view>
        <view class="nav-right"></view>
      </view>
    </view>
    <!-- Spacer to push page content below fixed navigation bar -->
    <view :style="{ height: (statusBarHeight + navBarHeight) + 'px' }"></view>

    <!-- Gallery Swiper -->
    <swiper class="goodsimgs" circular autoplay interval="3000" duration="1000" indicator-dots indicator-color="rgba(255,255,255,0.4)" indicator-active-color="#6F4E37">
      <swiper-item v-for="(img, idx) in gallery" :key="idx">
        <image :src="img" mode="aspectFill" class="gallery-image" @tap="onPreviewImage(idx)" />
      </swiper-item>
      <swiper-item v-if="gallery.length === 0">
        <image :src="goods.picUrl" mode="aspectFill" class="gallery-image" />
      </swiper-item>
    </swiper>

    <!-- Title Row -->
    <view class="goods_name">
      <view class="goods_name_left">{{ goods.name }}</view>
      <button class="goods_name_right" open-type="share">分享</button>
    </view>

    <!-- Goods Core Info Description & Price -->
    <view class="goods-info">
      <view class="c">
        <text class="desc">{{ goods.brief }}</text>
        <view class="price">
          <view class="counterPrice" v-if="goods.counterPrice > goods.retailPrice">原价：￥{{ goods.counterPrice }}</view>
          <view class="retailPrice">现价：￥{{ checkedSpecPrice }}</view>
        </view>
        <view class="brand" v-if="brand && brand.name">
          <text>{{ brand.name }}</text>
        </view>
      </view>
    </view>

    <!-- Spec selection switcher panel -->
    <view class="section-nav section-attr" @tap="onAddToCart">
      <view class="t">{{ tmpSpecText }}</view>
      <view class="arrow-right"></view>
    </view>

    <!-- Comments Section -->
    <view class="comments" v-if="commentCount > 0">
      <view class="h">
        <text class="t">评价({{ commentCount }})</text>
        <view class="i" @tap="onViewAllComments">
          <text class="i-text">查看全部</text>
          <view class="arrow-right-small"></view>
        </view>
      </view>
      <view class="b">
        <view class="item" v-for="item in commentList" :key="item.id">
          <view class="info">
            <view class="user">
              <image :src="item.avatar || '/static/images/default_avatar.png'"></image>
              <text>{{ item.nickname }}</text>
            </view>
            <view class="time">{{ item.addTime }}</view>
          </view>
          <view class="content">
            {{ item.content }}
          </view>
          <view class="imgs" v-if="item.picList && item.picList.length > 0">
            <image class="img" v-for="(pimg, pidx) in item.picList" :key="pidx" :src="pimg" @tap="onPreviewCommentImg(item.picList, pidx)"></image>
          </view>
          <view class="customer-service" v-if="item.adminContent">
            <text class="u">商家回复：</text>
            <text class="c">{{ item.adminContent }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- Product Parameters Table -->
    <view class="goods-attr" v-if="attributeList.length > 0">
      <view class="t">商品参数</view>
      <view class="l">
        <view class="item" v-for="(item, idx) in attributeList" :key="idx">
          <text class="left">{{ item.attribute }}</text>
          <text class="right">{{ item.value }}</text>
        </view>
      </view>
    </view>

    <!-- HTML Rich-Text Details -->
    <view class="detail" v-if="goods.detail">
      <rich-text :nodes="htmlNodes" class="detail-rich-text"></rich-text>
    </view>

    <!-- Common Problems Section -->
    <view class="common-problem" v-if="issueList.length > 0">
      <view class="h">
        <view class="line"></view>
        <text class="title">常见问题</text>
      </view>
      <view class="b">
        <view class="item" v-for="item in issueList" :key="item.id">
          <view class="question-box">
            <text class="spot"></text>
            <text class="question">{{ item.question }}</text>
          </view>
          <view class="answer">{{ item.answer }}</view>
        </view>
      </view>
    </view>

    <!-- Related Goods Section -->
    <view class="related-goods" v-if="relatedGoods.length > 0">
      <view class="h">
        <view class="line"></view>
        <text class="title">大家都在看</text>
      </view>
      <view class="b">
        <view class="item" v-for="item in relatedGoods" :key="item.id" @tap="onGoodsTap(item.id)">
          <image class="img" :src="item.picUrl" mode="aspectFill"></image>
          <text class="name">{{ item.name }}</text>
          <text class="price">￥{{ item.retailPrice }}</text>
        </view>
      </view>
    </view>

    <!-- Specification Selection Sheet Popup -->
    <view class="attr-pop-box" v-if="openAttr" @tap="onCloseAttr">
      <view class="attr-pop" @tap.stop>
        <view class="close-btn" @tap="onCloseAttr">×</view>
        <view class="img-info">
          <image :src="tmpPicUrl || goods.picUrl" mode="aspectFill" class="img" />
          <view class="info">
            <view class="c">
              <text class="price-val">价格：￥{{ checkedSpecPrice }}</text>
              <text class="spec-summary">{{ tmpSpecText }}</text>
            </view>
          </view>
        </view>

        <view class="spec-con">
          <view class="spec-item" v-for="spec in specificationList" :key="spec.name">
            <text class="name">{{ spec.name }}</text>
            <view class="values">
              <view 
                class="value" 
                :class="{ selected: vitem.checked }" 
                v-for="vitem in spec.valueList" 
                :key="vitem.id"
                @tap="clickSkuValue(spec.name, vitem.id)"
              >
                {{ vitem.value }}
              </view>
            </view>
          </view>

          <!-- Quantity Selection -->
          <view class="number-item">
            <text class="name">数量</text>
            <view class="selnum">
              <view class="cut" @tap="cutNumber">-</view>
              <input type="number" class="number" :value="number" disabled />
              <view class="add" @tap="addNumber">+</view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- Bottom Action Toolbar -->
    <view class="bottom-btn">
      <view class="l l-collect" @tap="addCollectOrNot">
        <text class="star-char">{{ userHasCollect === 1 ? '★' : '☆' }}</text>
        <text class="btn-label">收藏</text>
      </view>
      <view class="l l-cart" @tap="onCartTap">
        <view class="box">
          <text class="cart-count" v-if="cartGoodsCount > 0">{{ cartGoodsCount }}</text>
          <image src="/static/tabbar/cart.png" class="cart-icon-img" mode="aspectFit" />
        </view>
        <text class="btn-label">购物车</text>
      </view>
      <view class="r" @tap="onAddToCart" v-if="!soldout">加入购物车</view>
      <view class="c" @tap="onBuyNow" v-if="!soldout">立即购买</view>
      <view class="n" v-if="soldout">商品已售空</view>
    </view>
  </view>

  <view class="loading-container" v-else>
    <text class="loading-text">正在加载详情...</text>
  </view>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { onLoad, onShow, onShareAppMessage, onShareTimeline } from '@dcloudio/uni-app'
import request from '../../utils/request'
import api from '../../config/api'

// Route dynamic ID
const goodsId = ref(0)

onShareAppMessage((res) => {
  return {
    title: goods.value && goods.value.name ? `${goods.value.name} - 小满食光` : '小满食光 - 三明治·紫米饭团·饮品',
    path: `/pages/goods/goods?id=${goodsId.value}`,
    imageUrl: goods.value && goods.value.picUrl ? goods.value.picUrl : ''
  }
})

onShareTimeline(() => {
  return {
    title: goods.value && goods.value.name ? `${goods.value.name} - 小满食光` : '小满食光 - 三明治·紫米饭团·饮品',
    query: `id=${goodsId.value}`,
    imageUrl: goods.value && goods.value.picUrl ? goods.value.picUrl : ''
  }
})

// Navigation bar heights
const statusBarHeight = ref(20)
const navBarHeight = ref(44)

try {
  const sysInfo = uni.getSystemInfoSync()
  if (sysInfo.statusBarHeight) {
    statusBarHeight.value = sysInfo.statusBarHeight
  }
} catch (e) {
  console.error('Failed to get system info:', e)
}

// Product detail data states
const goods = ref(null)
const gallery = ref([])
const attributeList = ref([])
const productList = ref([])
const cartGoodsCount = ref(0)

// Specification Selection popup variables
const openAttr = ref(false)
const specificationList = ref([])
const checkedSpecPrice = ref(0)
const tmpSpecText = ref('请选择规格数量')
const tmpPicUrl = ref('')
const number = ref(1)
const soldout = ref(false)

// Additional states from litemall-wx
const brand = ref(null)
const issueList = ref([])
const commentCount = ref(0)
const commentList = ref([])
const relatedGoods = ref([])
const userHasCollect = ref(0)

onLoad((options) => {
  if (options.id) {
    goodsId.value = parseInt(options.id)
  }
})

const getGoodsDetail = async () => {
  try {
    const data = await request({
      url: `${api.GoodsDetail}?id=${goodsId.value}`,
      method: 'GET'
    })
    
    if (data) {
      goods.value = data.info || null
      gallery.value = data.info ? data.info.gallery : []
      attributeList.value = data.attribute || []
      productList.value = data.productList || []
      specificationList.value = data.specificationList || []
      checkedSpecPrice.value = data.info ? data.info.retailPrice : 0
      tmpPicUrl.value = data.info ? data.info.picUrl : ''
      
      // Load additional states
      brand.value = data.brand || null
      issueList.value = data.issue || []
      commentCount.value = data.comment ? data.comment.count : 0
      commentList.value = data.comment ? data.comment.data : []
      userHasCollect.value = data.userHasCollect || 0
      
      // Initialize checked property to false
      specificationList.value.forEach(spec => {
        if (spec.valueList) {
          spec.valueList.forEach(val => {
            val.checked = false
          })
        }
      })
      
      // If only one SKU exists, auto-select it
      if (specificationList.value.length === 1) {
        if (specificationList.value[0].valueList.length === 1) {
          specificationList.value[0].valueList[0].checked = true
          changeSpecInfo()
        }
      }
    }
  } catch (err) {
    console.error('Failed to load product details:', err)
  }
}

const getCartGoodsCount = async () => {
  const token = uni.getStorageSync('token')
  if (!token) {
    cartGoodsCount.value = 0
    return
  }
  try {
    const res = await request({
      url: api.CartGoodsCount,
      method: 'GET'
    })
    if (res !== undefined && res !== null) {
      cartGoodsCount.value = res
    }
  } catch (err) {
    console.error('Failed to get cart goods count:', err)
  }
}

const getRelatedGoods = async () => {
  try {
    const res = await request({
      url: `${api.GoodsRelated}?id=${goodsId.value}`,
      method: 'GET'
    })
    if (res && res.list) {
      relatedGoods.value = res.list
    }
  } catch (err) {
    console.error('Failed to get related goods:', err)
  }
}

const clickSkuValue = (specName, specValueId) => {
  specificationList.value.forEach(spec => {
    if (spec.name === specName) {
      spec.valueList.forEach(val => {
        if (val.id === specValueId) {
          val.checked = !val.checked
        } else {
          val.checked = false
        }
      })
    }
  })
  changeSpecInfo()
}

const getCheckedSpecValue = () => {
  const checkedValues = []
  specificationList.value.forEach(spec => {
    const checkedObj = {
      name: spec.name,
      valueId: 0,
      valueText: ''
    }
    if (spec.valueList) {
      spec.valueList.forEach(val => {
        if (val.checked) {
          checkedObj.valueId = val.id
          checkedObj.valueText = val.value
        }
      })
    }
    checkedValues.push(checkedObj)
  })
  return checkedValues
}

const isCheckedAllSpec = () => {
  return !getCheckedSpecValue().some(v => v.valueId === 0)
}

const getCheckedSpecKey = () => {
  return getCheckedSpecValue().map(v => v.valueText)
}

const getCheckedProductItem = (key) => {
  return productList.value.filter(v => {
    return v.specifications.toString() === key.toString()
  })
}

const changeSpecInfo = () => {
  const checkedNameValue = getCheckedSpecValue()
  const checkedValue = checkedNameValue
    .filter(v => v.valueId !== 0)
    .map(v => v.valueText)
    
  if (checkedValue.length > 0) {
    tmpSpecText.value = '已选择：' + checkedValue.join(' ')
  } else {
    tmpSpecText.value = '请选择规格数量'
  }
  
  if (isCheckedAllSpec()) {
    const checkedProductArray = getCheckedProductItem(getCheckedSpecKey())
    if (!checkedProductArray || checkedProductArray.length === 0) {
      soldout.value = true
      return
    }
    
    const checkedProduct = checkedProductArray[0]
    if (checkedProduct.number > 0) {
      checkedSpecPrice.value = checkedProduct.price
      if (checkedProduct.url) {
        tmpPicUrl.value = checkedProduct.url
      }
      soldout.value = false
    } else {
      checkedSpecPrice.value = goods.value.retailPrice
      soldout.value = true
    }
  } else {
    checkedSpecPrice.value = goods.value.retailPrice
    soldout.value = false
  }
}

const cutNumber = () => {
  if (number.value > 1) {
    number.value -= 1
  }
}

const addNumber = () => {
  number.value += 1
}

const onCloseAttr = () => {
  openAttr.value = false
}

// Format backend HTML content to adjust image sizes dynamically
const htmlNodes = computed(() => {
  if (!goods.value || !goods.value.detail) return ''
  // Fix styling issues for responsive images inside rich-text
  return goods.value.detail
    .replace(/<img[^>]*>/gi, (match) => {
      return match.replace(/style="[^"]*"/gi, '').replace(/width="[^"]*"/gi, '').replace(/height="[^"]*"/gi, '')
    })
    .replace(/<img/gi, '<img style="width:100%;height:auto;display:block;margin:0 auto;"')
})

const onBackTap = () => {
  uni.navigateBack({
    fail: () => {
      uni.switchTab({
        url: '/pages/index/index'
      })
    }
  })
}

const onPreviewImage = (index) => {
  uni.previewImage({
    current: gallery.value[index],
    urls: gallery.value
  })
}

const onPreviewCommentImg = (picList, index) => {
  uni.previewImage({
    current: picList[index],
    urls: picList
  })
}

const onViewAllComments = () => {
  uni.navigateTo({
    url: `/pages/comment/comment?valueId=${goodsId.value}&type=0`
  })
}

const onGoodsTap = (id) => {
  uni.redirectTo({
    url: `/pages/goods/goods?id=${id}`
  })
}

const onHomeTap = () => {
  uni.switchTab({
    url: '/pages/index/index'
  })
}

const onCartTap = () => {
  uni.switchTab({
    url: '/pages/cart/cart'
  })
}

const addCollectOrNot = async () => {
  const token = uni.getStorageSync('token')
  if (!token) {
    uni.showModal({
      title: '提示',
      content: '请先登录以使用收藏功能',
      confirmText: '去登录',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          uni.navigateTo({
            url: '/pages/auth/login'
          })
        }
      }
    })
    return
  }
  try {
    await request({
      url: api.CollectAddOrDelete,
      method: 'POST',
      data: {
        type: 0,
        valueId: goodsId.value
      }
    })
    if (userHasCollect.value === 1) {
      userHasCollect.value = 0
      uni.showToast({
        title: '已取消收藏',
        icon: 'none'
      })
    } else {
      userHasCollect.value = 1
      uni.showToast({
        title: '收藏成功',
        icon: 'success'
      })
    }
  } catch (err) {
    console.error('Failed to toggle collect:', err)
  }
}

const onAddToCart = async () => {
  // Check login status first
  const token = uni.getStorageSync('token')
  if (!token) {
    uni.showModal({
      title: '提示',
      content: '请先登录以使用购物车功能',
      confirmText: '去登录',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          uni.navigateTo({
            url: '/pages/auth/login'
          })
        }
      }
    })
    return
  }

  // Ensure products list (SKUs) is available
  if (!productList.value || productList.value.length === 0) {
    uni.showToast({
      title: '该商品暂无可售规格',
      icon: 'none'
    })
    return
  }

  if (!openAttr.value) {
    openAttr.value = true
    return
  }

  if (!isCheckedAllSpec()) {
    uni.showToast({
      title: '请选择完整规格',
      icon: 'none'
    })
    return
  }

  const checkedProductArray = getCheckedProductItem(getCheckedSpecKey())
  if (!checkedProductArray || checkedProductArray.length === 0) {
    uni.showToast({
      title: '没有库存',
      icon: 'none'
    })
    return
  }

  const checkedProduct = checkedProductArray[0]
  if (checkedProduct.number <= 0) {
    uni.showToast({
      title: '没有库存',
      icon: 'none'
    })
    return
  }

  try {
    const data = await request({
      url: api.CartAdd,
      method: 'POST',
      data: {
        goodsId: goods.value.id,
        number: number.value,
        productId: checkedProduct.id
      }
    })
    
    if (data !== undefined && data !== null) {
      cartGoodsCount.value = data
    }
    
    uni.showToast({
      title: '已加入购物车',
      icon: 'success'
    })
    openAttr.value = false
  } catch (err) {
    console.error('Failed to add商品到购物车:', err)
  }
}

const onBuyNow = async () => {
  // Check login status first
  const token = uni.getStorageSync('token')
  if (!token) {
    uni.showModal({
      title: '提示',
      content: '请先登录以购买商品',
      confirmText: '去登录',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          uni.navigateTo({
            url: '/pages/auth/login'
          })
        }
      }
    })
    return
  }

  // Ensure products list is available
  if (!productList.value || productList.value.length === 0) {
    uni.showToast({
      title: '该商品暂无可售规格',
      icon: 'none'
    })
    return
  }

  if (!openAttr.value) {
    openAttr.value = true
    return
  }

  if (!isCheckedAllSpec()) {
    uni.showToast({
      title: '请选择完整规格',
      icon: 'none'
    })
    return
  }

  const checkedProductArray = getCheckedProductItem(getCheckedSpecKey())
  if (!checkedProductArray || checkedProductArray.length === 0) {
    uni.showToast({
      title: '没有库存',
      icon: 'none'
    })
    return
  }

  const checkedProduct = checkedProductArray[0]
  if (checkedProduct.number <= 0) {
    uni.showToast({
      title: '没有库存',
      icon: 'none'
    })
    return
  }

  try {
    const data = await request({
      url: api.CartFastAdd,
      method: 'POST',
      data: {
        goodsId: goods.value.id,
        number: number.value,
        productId: checkedProduct.id
      }
    })
    
    uni.showToast({
      title: '已加购，正在结算...',
      icon: 'none'
    })
    
    openAttr.value = false
    
    setTimeout(() => {
      uni.navigateTo({
        url: `/pages/checkout/checkout?cartId=${data}`
      })
    }, 800)
  } catch (err) {
    console.error('Failed to buy now:', err)
  }
}

onMounted(() => {
  getGoodsDetail()
  getRelatedGoods()
})

onShow(() => {
  getCartGoodsCount()
})
</script>

<style scoped>
.container {
  background-color: #FAF6EE;
  padding-bottom: 120rpx;
  min-height: 100vh;
}

/* Gallery Swiper */
.goodsimgs {
  width: 750rpx;
  height: 750rpx;
  background-color: #ffffff;
}

.goodsimgs .gallery-image {
  width: 750rpx;
  height: 750rpx;
  display: block;
}

/* Title & Share Row */
.goods_name {
  background-color: #ffffff;
  padding: 30rpx 32rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1rpx solid rgba(230, 213, 184, 0.3);
}

.goods_name_left {
  font-size: 36rpx;
  font-weight: 700;
  color: #4A3525;
  flex: 1;
  padding-right: 20rpx;
}

.goods_name_right {
  width: 120rpx;
  height: 56rpx;
  line-height: 56rpx;
  padding: 0;
  margin: 0;
  font-size: 24rpx;
  color: #ffffff;
  background-color: #E07A5F;
  border-radius: 28rpx;
  text-align: center;
  border: none;
  font-weight: 600;
}

.goods_name_right::after {
  border: none;
}

/* Goods Core Info */
.goods-info {
  background-color: #ffffff;
  padding: 0 32rpx 32rpx 32rpx;
  margin-bottom: 20rpx;
  border-bottom: 1rpx solid rgba(230, 213, 184, 0.3);
}

.goods-info .desc {
  font-size: 26rpx;
  color: #A18276;
  line-height: 1.5;
  display: block;
  margin-bottom: 24rpx;
}

.goods-info .price {
  display: flex;
  align-items: baseline;
  gap: 20rpx;
}

.goods-info .counterPrice {
  font-size: 26rpx;
  color: #A18276;
  text-decoration: line-through;
}

.goods-info .retailPrice {
  font-size: 36rpx;
  font-weight: 800;
  color: #E07A5F;
}

.goods-info .brand {
  margin-top: 24rpx;
}

.goods-info .brand text {
  display: inline-block;
  padding: 4rpx 16rpx;
  font-size: 22rpx;
  color: #6F4E37;
  border: 1rpx solid #6F4E37;
  border-radius: 6rpx;
  background-color: rgba(111, 78, 55, 0.05);
}

/* Spec Selection Nav */
.section-nav {
  background-color: #ffffff;
  padding: 32rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
  border-top: 1rpx solid rgba(230, 213, 184, 0.3);
  border-bottom: 1rpx solid rgba(230, 213, 184, 0.3);
}

.section-nav .t {
  font-size: 28rpx;
  color: #4A3525;
  font-weight: 600;
}

.section-nav .arrow-right {
  width: 14rpx;
  height: 14rpx;
  border-top: 3rpx solid #A18276;
  border-right: 3rpx solid #A18276;
  transform: rotate(45deg);
  margin-right: 8rpx;
}

/* Comments Section */
.comments {
  background-color: #ffffff;
  padding: 32rpx;
  margin-bottom: 20rpx;
  border-top: 1rpx solid rgba(230, 213, 184, 0.3);
  border-bottom: 1rpx solid rgba(230, 213, 184, 0.3);
}

.comments .h {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 24rpx;
  border-bottom: 1rpx solid rgba(230, 213, 184, 0.2);
  margin-bottom: 24rpx;
}

.comments .h .t {
  font-size: 28rpx;
  font-weight: 700;
  color: #4A3525;
}

.comments .h .i {
  display: flex;
  align-items: center;
  font-size: 24rpx;
  color: #A18276;
}

.comments .h .i .i-text {
  font-size: 24rpx;
  color: #A18276;
}

.arrow-right-small {
  width: 10rpx;
  height: 10rpx;
  border-top: 2rpx solid #A18276;
  border-right: 2rpx solid #A18276;
  transform: rotate(45deg);
  margin-left: 8rpx;
}

.comments .item {
  margin-bottom: 24rpx;
}

.comments .info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.comments .user {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.comments .user image {
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  background-color: #FAF6EE;
}

.comments .user text {
  font-size: 26rpx;
  font-weight: 600;
  color: #4A3525;
}

.comments .time {
  font-size: 22rpx;
  color: #A18276;
}

.comments .content {
  font-size: 26rpx;
  color: #4A3525;
  line-height: 1.5;
  margin-bottom: 16rpx;
}

.comments .imgs {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-bottom: 16rpx;
}

.comments .imgs .img {
  width: 150rpx;
  height: 150rpx;
  border-radius: 8rpx;
}

.comments .customer-service {
  background-color: #FAF6EE;
  padding: 16rpx 20rpx;
  border-radius: 12rpx;
  font-size: 24rpx;
  line-height: 1.5;
  border-left: 6rpx solid #6F4E37;
}

.comments .customer-service .u {
  font-weight: 700;
  color: #6F4E37;
}

.comments .customer-service .c {
  color: #4A3525;
}

/* Product Parameters */
.goods-attr {
  background-color: #ffffff;
  padding: 32rpx;
  margin-bottom: 20rpx;
  border-top: 1rpx solid rgba(230, 213, 184, 0.3);
  border-bottom: 1rpx solid rgba(230, 213, 184, 0.3);
}

.goods-attr .t {
  font-size: 28rpx;
  font-weight: 700;
  color: #4A3525;
  margin-bottom: 24rpx;
}

.goods-attr .l {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.goods-attr .item {
  display: flex;
  background-color: #FAF6EE;
  padding: 16rpx 24rpx;
  border-radius: 8rpx;
  align-items: center;
}

.goods-attr .left {
  width: 180rpx;
  font-size: 24rpx;
  color: #A18276;
  font-weight: 600;
}

.goods-attr .right {
  flex: 1;
  font-size: 24rpx;
  color: #4A3525;
  font-weight: 500;
}

/* HTML Rich text detail */
.detail {
  background-color: #ffffff;
  padding: 20rpx 0;
  margin-bottom: 20rpx;
}

.detail-rich-text {
  font-size: 28rpx;
  color: #4A3525;
}

/* Common Problems */
.common-problem {
  background-color: #ffffff;
  padding: 32rpx;
  margin-bottom: 20rpx;
}

.common-problem .h {
  display: flex;
  align-items: center;
  gap: 16rpx;
  justify-content: center;
  margin-bottom: 32rpx;
}

.common-problem .h .line {
  width: 40rpx;
  height: 2rpx;
  background-color: rgba(230, 213, 184, 0.8);
}

.common-problem .h .title {
  font-size: 28rpx;
  font-weight: 700;
  color: #4A3525;
}

.common-problem .item {
  margin-bottom: 28rpx;
}

.common-problem .question-box {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 8rpx;
}

.common-problem .spot {
  width: 10rpx;
  height: 10rpx;
  background-color: #E07A5F;
  border-radius: 50%;
}

.common-problem .question {
  font-size: 26rpx;
  font-weight: 700;
  color: #4A3525;
}

.common-problem .answer {
  font-size: 24rpx;
  color: #A18276;
  line-height: 1.5;
  padding-left: 22rpx;
}

/* Related Goods */
.related-goods {
  background-color: #ffffff;
  padding: 32rpx;
  margin-bottom: 40rpx;
}

.related-goods .h {
  display: flex;
  align-items: center;
  gap: 16rpx;
  justify-content: center;
  margin-bottom: 32rpx;
}

.related-goods .h .line {
  width: 40rpx;
  height: 2rpx;
  background-color: rgba(230, 213, 184, 0.8);
}

.related-goods .h .title {
  font-size: 28rpx;
  font-weight: 700;
  color: #4A3525;
}

.related-goods .b {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24rpx;
}

.related-goods .item {
  display: flex;
  flex-direction: column;
  background-color: #FAF6EE;
  padding: 16rpx;
  border-radius: 16rpx;
  border: 1rpx solid rgba(230, 213, 184, 0.3);
}

.related-goods .img {
  width: 100%;
  height: 260rpx;
  border-radius: 12rpx;
  background-color: #ffffff;
  margin-bottom: 16rpx;
}

.related-goods .name {
  font-size: 26rpx;
  font-weight: 700;
  color: #4A3525;
  margin-bottom: 8rpx;
  text-align: left;
}

.related-goods .price {
  font-size: 26rpx;
  font-weight: 800;
  color: #E07A5F;
  text-align: left;
}

/* Bottom Action Bar */
.bottom-btn {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: calc(100rpx + env(safe-area-inset-bottom));
  padding-bottom: env(safe-area-inset-bottom);
  box-sizing: border-box;
  background-color: #ffffff;
  border-top: 1rpx solid rgba(230, 213, 184, 0.4);
  display: flex;
  z-index: 160;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.bottom-btn .l {
  width: 110rpx;
  height: 100rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-right: 1rpx solid rgba(230, 213, 184, 0.2);
}

.bottom-btn .star-char {
  font-size: 38rpx;
  color: #A18276;
  line-height: 1.1;
}

.bottom-btn .star-char.active {
  color: #E07A5F;
}

.bottom-btn .box {
  position: relative;
  width: 44rpx;
  height: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bottom-btn .cart-icon-img {
  width: 40rpx;
  height: 40rpx;
}

.bottom-btn .cart-count {
  position: absolute;
  top: -10rpx;
  right: -10rpx;
  background-color: #E07A5F;
  color: #ffffff;
  font-size: 16rpx;
  font-weight: 700;
  padding: 2rpx 8rpx;
  border-radius: 12rpx;
  border: 2rpx solid #ffffff;
  z-index: 2;
  min-width: 20rpx;
  text-align: center;
}

.bottom-btn .btn-label {
  font-size: 18rpx;
  color: #A18276;
  font-weight: 600;
  margin-top: 4rpx;
}

.bottom-btn .r, .bottom-btn .c, .bottom-btn .n {
  flex: 1;
  height: 100rpx;
  line-height: 100rpx;
  text-align: center;
  font-size: 28rpx;
  font-weight: 700;
}

.bottom-btn .r {
  background-color: rgba(111, 78, 55, 0.08);
  color: #6F4E37;
  border-left: 1rpx solid rgba(230, 213, 184, 0.2);
  transition: background-color 0.15s ease;
}

.bottom-btn .r:active {
  background-color: rgba(111, 78, 55, 0.16);
}

.bottom-btn .c {
  background-image: linear-gradient(to right, #6F4E37 0%, #E07A5F 100%);
  color: #ffffff;
  transition: opacity 0.15s ease;
}

.bottom-btn .c:active {
  opacity: 0.85;
}

.bottom-btn .n {
  background-color: #e5e5e5;
  color: #999;
}

/* Specification Selection Modal Popup styles */
.attr-pop-box {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  background-color: rgba(74, 53, 37, 0.4);
  z-index: 150;
  animation: fadeIn 0.25s ease-out;
}

.attr-pop {
  width: 100%;
  background-color: #ffffff;
  border-radius: 32rpx 32rpx 0 0;
  padding: 40rpx 40rpx 60rpx 40rpx;
  box-sizing: border-box;
  position: fixed;
  left: 0;
  bottom: calc(100rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid rgba(230, 213, 184, 0.5);
  animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes fadeIn {
  0% { opacity: 0; }
  100% { opacity: 1; }
}

@keyframes slideUp {
  0% { transform: translateY(100%); }
  100% { transform: translateY(0); }
}

.close-btn {
  position: absolute;
  top: 32rpx;
  right: 32rpx;
  font-size: 48rpx;
  color: #A18276;
  line-height: 1;
  font-weight: 300;
  cursor: pointer;
  z-index: 10;
  transition: transform 0.15s ease;
}

.close-btn:active {
  transform: scale(0.85);
}

.img-info {
  display: flex;
  gap: 28rpx;
  border-bottom: 1rpx solid rgba(230, 213, 184, 0.3);
  padding-bottom: 32rpx;
  margin-bottom: 32rpx;
}

.img-info .img {
  width: 160rpx;
  height: 160rpx;
  border-radius: 16rpx;
  background-color: #ffffff;
  border: 1rpx solid rgba(230, 213, 184, 0.3);
  flex-shrink: 0;
}

.img-info .info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.img-info .price-val {
  font-size: 36rpx;
  font-weight: 800;
  color: #E07A5F;
  margin-bottom: 8rpx;
  display: block;
}

.img-info .spec-summary {
  font-size: 24rpx;
  color: #A18276;
  font-weight: 500;
}

.spec-con {
  max-height: 60vh;
  overflow-y: auto;
}

.spec-item {
  margin-bottom: 36rpx;
}

.spec-item .name {
  font-size: 26rpx;
  color: #4A3525;
  font-weight: 700;
  margin-bottom: 20rpx;
  display: block;
}

.spec-item .values {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.spec-item .value {
  padding: 12rpx 28rpx;
  background-color: #ffffff;
  border: 1rpx solid rgba(230, 213, 184, 0.4);
  border-radius: 28rpx;
  font-size: 24rpx;
  color: #4A3525;
  font-weight: 500;
  transition: all 0.2s ease;
}

.spec-item .value:active {
  transform: scale(0.95);
}

.spec-item .value.selected {
  background-color: rgba(111, 78, 55, 0.08);
  border-color: #6F4E37;
  color: #6F4E37;
  font-weight: 700;
}

.number-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12rpx;
  margin-bottom: 24rpx;
}

.number-item .name {
  font-size: 26rpx;
  color: #4A3525;
  font-weight: 700;
}

.number-item .selnum {
  display: flex;
  align-items: center;
  border: 1rpx solid rgba(230, 213, 184, 0.5);
  border-radius: 28rpx;
  overflow: hidden;
  background-color: #ffffff;
}

.number-item .cut, .number-item .add {
  width: 64rpx;
  height: 56rpx;
  line-height: 56rpx;
  text-align: center;
  font-size: 32rpx;
  color: #6F4E37;
  font-weight: 700;
  transition: background-color 0.15s ease;
}

.number-item .cut:active, .number-item .add:active {
  background-color: rgba(111, 78, 55, 0.08);
}

.number-item .cut {
  border-right: 1rpx solid rgba(230, 213, 184, 0.3);
}

.number-item .add {
  border-left: 1rpx solid rgba(230, 213, 184, 0.3);
}

.number-item .number {
  width: 80rpx;
  height: 56rpx;
  text-align: center;
  font-size: 24rpx;
  color: #4A3525;
  font-weight: 700;
}

/* Loading container */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background-color: #FAF6EE;
}

.loading-text {
  font-size: 26rpx;
  color: #A18276;
  font-weight: 500;
}

/* Custom Navigation Bar Styles */
.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  background-color: #ffffff;
  z-index: 1000;
  border-bottom: 1rpx solid rgba(230, 213, 184, 0.3);
}

.nav-content {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32rpx;
  position: relative;
}

.nav-left {
  width: 80rpx;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
}

.nav-left:active {
  opacity: 0.6;
}

.back-icon {
  display: inline-block;
  width: 20rpx;
  height: 20rpx;
  border-top: 4rpx solid #4A3525;
  border-left: 4rpx solid #4A3525;
  transform: rotate(-45deg);
}

.nav-title {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  font-size: 32rpx;
  font-weight: 700;
  color: #4A3525;
}

.nav-right {
  width: 80rpx;
}
</style>
