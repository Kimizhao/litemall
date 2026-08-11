export default {
  onShareAppMessage(res) {
    return {
      title: '小满食光 - 手作美食与温度咖啡',
      path: '/pages/index/index'
    }
  },
  onShareTimeline() {
    return {
      title: '小满食光 - 手作美食与温度咖啡',
      query: ''
    }
  }
}
