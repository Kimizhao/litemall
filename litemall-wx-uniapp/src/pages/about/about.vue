<template>
  <view class="container">
    <view class="about-card glass-panel">
      <!-- Profile Header -->
      <view class="brand-header">
        <text class="brand-logo">🎨</text>
        <text class="brand-name">Litemall 手作工坊</text>
        <text class="brand-version">v1.0.0 Port</text>
      </view>

      <view class="divider"></view>

      <!-- Shop Narrative -->
      <view class="section">
        <text class="section-title">品牌故事</text>
        <text class="section-text">
          在快节奏的现代生活中，我们始终坚信，带有手艺人温度与执念的作品具有不可替代的价值。Litemall 手作工坊精选全国独立手艺人、陶艺师与木雕工匠的匠心杰作，为您呈现充满自然、素雅与意境的生活器物。
        </text>
      </view>

      <!-- Coordinates -->
      <view class="section">
        <text class="section-title">联系我们</text>
        <view class="contact-row">
          <text class="label">🏢 地址</text>
          <text class="val">浙江省杭州市西湖区手工艺术产业园 3 栋</text>
        </view>
        <view class="contact-row">
          <text class="label">📞 电话</text>
          <text class="val">400-888-9999</text>
        </view>
        <view class="contact-row">
          <text class="label">✉️ 邮箱</text>
          <text class="val">artisan@litemall.com</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
// Static presentation
</script>

<style scoped>
.container {
  min-height: 100vh;
  background-color: var(--bg-primary);
  padding: 32rpx;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
}

.about-card {
  width: 100%;
  padding: 64rpx 48rpx;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-md);
  display: flex;
  flex-direction: column;
}

.brand-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}

.brand-logo {
  font-size: 100rpx;
  margin-bottom: 12rpx;
}

.brand-name {
  font-size: 34rpx;
  font-weight: 800;
  color: var(--text-main);
}

.brand-version {
  font-size: 20rpx;
  color: var(--text-muted);
  font-weight: 600;
  background-color: rgba(241, 245, 249, 0.8);
  padding: 4rpx 16rpx;
  border-radius: 12rpx;
}

.divider {
  height: 1px;
  background-color: var(--border-light);
  margin: 40rpx 0;
}

.section {
  margin-bottom: 40rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: 26rpx;
  font-weight: 800;
  color: var(--text-main);
  border-left: 6rpx solid #4f46e5;
  padding-left: 12rpx;
}

.section-text {
  font-size: 24rpx;
  color: var(--text-muted);
  line-height: 1.6;
  font-weight: 500;
}

.contact-row {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
  margin-bottom: 12rpx;
}

.contact-row:last-child {
  margin-bottom: 0;
}

.label {
  font-size: 22rpx;
  font-weight: 700;
  color: var(--text-main);
}

.val {
  font-size: 24rpx;
  color: var(--text-muted);
  font-weight: 500;
  line-height: 1.4;
}
</style>
