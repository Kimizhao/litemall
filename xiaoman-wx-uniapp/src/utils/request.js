/**
 * Axios-like promise wrapper for uni.request
 */
export default function request(options) {
  return new Promise((resolve, reject) => {
    // Retrieve token from storage
    const token = uni.getStorageSync('token')
    const headers = {
      'Content-Type': 'application/json',
      ...options.headers
    }
    
    if (token) {
      headers['X-Litemall-Token'] = token
    }

    uni.request({
      url: options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: headers,
      success: (res) => {
        if (res.statusCode === 200) {
          const data = res.data
          // Litemall backend returns errno: 0 for success
          if (data.errno === 0) {
            resolve(data.data)
          } else if (data.errno === 501) {
            // 501 represents unauthorized/token expired
            uni.removeStorageSync('token')
            uni.removeStorageSync('userInfo')
            uni.navigateTo({
              url: '/pages/auth/login'
            })
            reject(data)
          } else {
            uni.showToast({
              title: data.errmsg || 'Request Error',
              icon: 'none',
              duration: 2000
            })
            reject(data)
          }
        } else {
          uni.showToast({
            title: `HTTP ${res.statusCode} Error`,
            icon: 'none'
          })
          reject(res)
        }
      },
      fail: (err) => {
        uni.showToast({
          title: 'Network Timeout',
          icon: 'none'
        })
        reject(err)
      }
    })
  })
}
