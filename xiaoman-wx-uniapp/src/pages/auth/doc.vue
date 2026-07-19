<template>
  <view class="container">
    <view class="content-card">
      <text class="title">{{ title }}</text>
      <text class="update-time">更新日期：2026年7月18日</text>
      
      <scroll-view scroll-y class="scroll-body">
        <view class="doc-text">
          <block v-if="type === 'service'">
            <view class="section">
              <text class="h2">一、 协议的范围与确认</text>
              <text class="p">欢迎您使用“小满食光”小程序服务。本协议是您与小满食光（以下简称“我们”）之间关于您使用我们提供的在线点单、三明治/咖啡配送及会员账户服务所订立的法律协议。</text>
              <text class="p">当您点击同意本协议或使用我们的点餐服务时，即视为您已充分阅读、理解并接受本协议的全部内容。</text>
            </view>

            <view class="section">
              <text class="h2">二、 账户注册与微信绑定</text>
              <text class="p">1. 为正常使用小满食光的点单与配送服务，您需要授权您的微信手机号码进行一键登录与绑定。绑定后，您的优惠券、积分及历史订单将与该微信号深度关联。</text>
              <text class="p">2. 您应当保证提供给我们的手机号真实有效。若因手机号错误导致餐品无法配送或无法取得联系，相关损失将由您自行承担。</text>
            </view>

            <view class="section">
              <text class="h2">三、 在线点单与餐品服务</text>
              <text class="p">1. 我们提供新鲜手作三明治、现磨咖啡及饮品。由于餐品均为即时制作的食品，请您在下单前仔细核对商品规格、口味及备注信息。</text>
              <text class="p">2. 订单一旦开始制作，非因餐品质量问题，我们将无法受理您的取消或退款申请。</text>
            </view>

            <view class="section">
              <text class="h2">四、 配送与签收</text>
              <text class="p">1. 配送范围及配送费标准以小程序结算页面公示为准。由于高峰期、恶劣天气或不可抗力导致配送延迟的，我们将竭诚与您沟通并加急处理。</text>
              <text class="p">2. 请您在收到餐品时当面确认外包装完好无损，如餐品有倾洒或损坏，请立即通过客服与我们联系。</text>
            </view>

            <view class="section">
              <text class="h2">五、 免责声明与知识产权</text>
              <text class="p">我们拥有本小程序内所有文字、图片、界面设计及品牌标识的知识产权。未经授权，任何单位或个人不得进行商业目的的复制或传播。</text>
            </view>
          </block>

          <block v-else-if="type === 'privacy'">
            <view class="section">
              <text class="h2">一、 我们如何收集和使用您的信息</text>
              <text class="p">为向您提供安全、顺畅的点单及配送服务，我们会在获得您的明确授权后，收集以下必要信息：</text>
              <text class="p">1. **微信基本信息（头像、昵称）**：用于在个人中心展示您的个性化主页与会员昵称。</text>
              <text class="p">2. **手机号码**：用于绑定并创建您的会员账户，同时用于订单状态通知、取餐联系及配送联系。</text>
              <text class="p">3. **地理位置与配送地址**：用于定位您的当前位置以推荐最近的门店，或供配送员将餐品送达指定位置。</text>
            </view>

            <view class="section">
              <text class="h2">二、 我们如何保护和存储您的信息</text>
              <text class="p">1. 我们会采用行业主流的安全技术 and 加密手段存储您的个人信息，严防信息泄露、滥用或非授权修改。</text>
              <text class="p">2. 您的个人信息将严格保存在境内，且仅用于为您提供本小程序的点餐相关服务，除法律法规要求外，我们绝不会向任何第三方出售或公开您的隐私信息。</text>
            </view>

            <view class="section">
              <text class="h2">三、 您的权利与账号注销</text>
              <text class="p">您有权随时在“我的 -> 账号与设置”中修改您的头像和昵称。如果您需要注销账户或清除您的个人数据，可通过小程序内的帮助中心与我们的主理人取得联系，我们将在验证您的身份后及时处理您的注销申请。</text>
            </view>

            <view class="section">
              <text class="h2">四、 本政策的更新</text>
              <text class="p">随着国家法律法规的调整或小程序功能的升级，我们可能会适时修订本隐私政策，修订后的版本将在小程序内公示并生效。</text>
            </view>
          </block>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const type = ref('service')
const title = ref('用户服务协议')

onLoad((options) => {
  if (options.type) {
    type.value = options.type
    if (options.type === 'privacy') {
      title.value = '隐私政策协议'
      uni.setNavigationBarTitle({ title: '隐私政策协议' })
    } else {
      title.value = '用户服务协议'
      uni.setNavigationBarTitle({ title: '用户服务协议' })
    }
  }
})
</script>

<style scoped>
.container {
  min-height: 100vh;
  background-color: #FAF6EE;
  padding: 32rpx;
  box-sizing: border-box;
}

.content-card {
  background-color: #ffffff;
  border-radius: 24rpx;
  padding: 40rpx 32rpx;
  box-shadow: 0 4px 16px rgba(111, 78, 55, 0.05);
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64rpx);
  box-sizing: border-box;
}

.title {
  font-size: 38rpx;
  font-weight: 800;
  color: #4A3525;
  text-align: center;
  margin-bottom: 8rpx;
}

.update-time {
  font-size: 22rpx;
  color: #A18276;
  text-align: center;
  margin-bottom: 40rpx;
}

.scroll-body {
  flex: 1;
  overflow: hidden;
}

.doc-text {
  padding-bottom: 40rpx;
}

.section {
  margin-bottom: 40rpx;
}

.h2 {
  font-size: 28rpx;
  font-weight: 700;
  color: #4A3525;
  margin-bottom: 16rpx;
  display: block;
}

.p {
  font-size: 24rpx;
  color: #6F4E37;
  line-height: 1.8;
  margin-bottom: 12rpx;
  display: block;
}
</style>
