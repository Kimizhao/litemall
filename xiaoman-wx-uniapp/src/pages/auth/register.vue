<template>
  <view class="container"></view>
</template>

<script setup>
import { onMounted } from 'vue'

onMounted(() => {
  uni.redirectTo({
    url: '/pages/auth/login'
  })
})
</script>

<style scoped>
.container {
  min-height: 100vh;
  background-color: #FAF6EE;
}
</style>
