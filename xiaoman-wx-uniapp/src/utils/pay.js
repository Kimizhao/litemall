import request from './request'
import api from '../config/api'

/**
 * 调起微信支付通用逻辑
 * @param {number|string} orderId 订单ID
 * @returns {Promise<boolean>} 是否支付成功
 */
export async function payOrder(orderId) {
  if (!orderId) {
    uni.showToast({ title: '订单号无效', icon: 'none' })
    return false
  }

  uni.showLoading({ title: '正在发起支付...' })

  try {
    const payParam = await request({
      url: api.OrderPrepay,
      method: 'POST',
      data: { orderId: parseInt(orderId) }
    })

    uni.hideLoading()

    if (!payParam) {
      throw new Error('未获取到支付参数')
    }

    // 小程序原生支付唤起
    // #ifdef MP-WEIXIN
    return new Promise((resolve) => {
      uni.requestPayment({
        provider: 'wxpay',
        timeStamp: payParam.timeStamp,
        nonceStr: payParam.nonceStr,
        package: payParam.packageValue || payParam.package,
        signType: payParam.signType || 'MD5',
        paySign: payParam.paySign,
        success: () => {
          uni.showToast({ title: '支付成功', icon: 'success' })
          setTimeout(() => {
            uni.redirectTo({
              url: `/pages/payResult/payResult?status=true&orderId=${orderId}`
            })
            resolve(true)
          }, 600)
        },
        fail: (res) => {
          console.warn('微信支付失败/取消:', res)
          const isCancel = res && (res.errMsg && (res.errMsg.indexOf('cancel') >= 0 || res.errMsg.indexOf('Cancel') >= 0))
          uni.showToast({
            title: isCancel ? '已取消支付' : (res.errMsg || '支付失败'),
            icon: 'none'
          })
          setTimeout(() => {
            uni.redirectTo({
              url: `/pages/payResult/payResult?status=false&orderId=${orderId}`
            })
            resolve(false)
          }, 800)
        }
      })
    })
    // #endif

    // 非微信小程序环境（H5 或 开发环境降级逻辑）
    // #ifndef MP-WEIXIN
    return new Promise((resolve) => {
      uni.showModal({
        title: '在线支付提示',
        content: '当前环境不是微信小程序原生环境，是否模拟完成支付？',
        confirmText: '模拟支付',
        cancelText: '取消',
        success: (res) => {
          if (res.confirm) {
            uni.setStorageSync('mock_paid_order_' + orderId, true)
            uni.showToast({ title: '模拟支付成功', icon: 'success' })
            setTimeout(() => {
              uni.redirectTo({
                url: `/pages/payResult/payResult?status=true&orderId=${orderId}`
              })
              resolve(true)
            }, 600)
          } else {
            uni.redirectTo({
              url: `/pages/payResult/payResult?status=false&orderId=${orderId}`
            })
            resolve(false)
          }
        }
      })
    })
    // #endif

  } catch (err) {
    uni.hideLoading()
    console.error('Prepay failed:', err)

    const errmsg = (err && (err.errmsg || err.message)) || '发起支付失败'
    
    return new Promise((resolve) => {
      uni.showModal({
        title: '支付发起提示',
        content: `${errmsg}\n\n（提示：若未配置真实微信商户号或OpenID，可使用模拟支付体验流程）`,
        confirmText: '模拟支付',
        cancelText: '稍后支付',
        success: (res) => {
          if (res.confirm) {
            uni.setStorageSync('mock_paid_order_' + orderId, true)
            uni.showToast({ title: '模拟支付成功', icon: 'success' })
            setTimeout(() => {
              uni.redirectTo({
                url: `/pages/payResult/payResult?status=true&orderId=${orderId}`
              })
              resolve(true)
            }, 600)
          } else {
            uni.redirectTo({
              url: `/pages/payResult/payResult?status=false&orderId=${orderId}`
            })
            resolve(false)
          }
        }
      })
    })
  }
}
