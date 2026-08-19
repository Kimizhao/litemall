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
      data: { orderId: parseInt(orderId) },
      noToast: true
    })

    uni.hideLoading()

    if (!payParam) {
      throw new Error('未获取到支付参数')
    }

    // 小程序原生支付唤起
    // #ifdef MP-WEIXIN
    return new Promise((resolve) => {
      const pkg = payParam.packageValue || payParam.package || (payParam.prepayId ? `prepay_id=${payParam.prepayId}` : '')
      uni.requestPayment({
        provider: 'wxpay',
        timeStamp: String(payParam.timeStamp),
        nonceStr: payParam.nonceStr,
        package: pkg,
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
            title: isCancel ? '已取消支付' : '支付未完成',
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

    // 非微信小程序环境（H5 或 调试环境降级逻辑）
    // #ifndef MP-WEIXIN
    return new Promise((resolve) => {
      uni.showModal({
        title: '在线支付提示',
        content: '当前为非微信小程序原生环境，无法调起微信支付，可查看订单详情。',
        confirmText: '查看订单',
        showCancel: false,
        success: () => {
          uni.redirectTo({
            url: `/pages/payResult/payResult?status=false&orderId=${orderId}`
          })
          resolve(false)
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
        title: '支付提示',
        content: `${errmsg}，请在订单详情页继续支付。`,
        confirmText: '查看订单',
        showCancel: false,
        success: () => {
          uni.redirectTo({
            url: `/pages/payResult/payResult?status=false&orderId=${orderId}`
          })
          resolve(false)
        }
      })
    })
  }
}
