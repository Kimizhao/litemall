import { WxApiHost } from '../config/api'

export function fixImageUrl(val) {
  if (typeof val !== 'string') return val

  // If it's a full URL containing /wx/storage/fetch/ or /storage/fetch/, replace the domain with WxApiHost
  if (val.includes('/wx/storage/fetch/')) {
    const pathIndex = val.indexOf('/wx/storage/fetch/')
    return WxApiHost + val.substring(pathIndex)
  }
  if (val.includes('/storage/fetch/')) {
    const pathIndex = val.indexOf('/storage/fetch/')
    return WxApiHost + '/wx' + val.substring(pathIndex)
  }

  // Relative path starting with /wx/storage/fetch/
  if (val.startsWith('/wx/storage/fetch/')) {
    return WxApiHost + val
  }
  // Or starting with /storage/fetch/
  if (val.startsWith('/storage/fetch/')) {
    return WxApiHost + '/wx' + val
  }

  // HTML content containing src="/wx/storage/fetch/..." or src="/storage/fetch/..."
  let updated = val
  if (val.includes('/wx/storage/fetch/')) {
    updated = updated.replace(/(src=["'])\/wx\/storage\/fetch\//gi, `$1${WxApiHost}/wx/storage/fetch/`)
  }
  if (val.includes('/storage/fetch/')) {
    updated = updated.replace(/(src=["'])\/storage\/fetch\//gi, `$1${WxApiHost}/wx/storage/fetch/`)
  }
  return updated
}

function processResponseData(data) {
  if (!data) return data
  if (typeof data === 'string') {
    return fixImageUrl(data)
  }
  if (Array.isArray(data)) {
    return data.map(item => processResponseData(item))
  }
  if (typeof data === 'object') {
    const newData = {}
    for (const key in data) {
      if (Object.prototype.hasOwnProperty.call(data, key)) {
        newData[key] = processResponseData(data[key])
      }
    }
    return newData
  }
  return data
}

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
            resolve(processResponseData(data.data))
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
