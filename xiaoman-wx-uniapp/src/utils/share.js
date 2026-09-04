export default {
  onShareAppMessage(res) {
    return {
      title: '小满食光 - 三明治·紫米饭团·饮品',
      path: '/pages/index/index',
      imageUrl: '/static/xiaoman/share_cover.jpg'
    }
  },
  onShareTimeline() {
    return {
      title: '小满食光 - 三明治·紫米饭团·饮品',
      query: '',
      imageUrl: '/static/xiaoman/share_cover.jpg'
    }
  }
}
