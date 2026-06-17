/**
 * 解析订单备注中的预约自取信息
 * @param {String} message 订单备注留言
 * @returns {Object|null} 解析成功返回预约对象，匹配失败返回null
 */
export function parseOrderRemark(message) {
  if (!message || typeof message !== 'string') return null;
  try {
    if (message.indexOf('[预约自取]') === -1) {
      return null;
    }
    const reg = /\[预约自取\] 时间:(.*?) \| 称呼:(.*?) \| 电话:(.*?) \| 备注:([\s\S]*?)$/;
    const matches = message.match(reg);
    if (matches && matches.length >= 5) {
      return {
        isPickup: true,
        time: matches[1].trim(),
        name: matches[2].trim(),
        phone: matches[3].trim(),
        remark: matches[4].trim()
      };
    }
  } catch (e) {
    console.error('解析订单备注异常:', e);
  }
  return null;
}

/**
 * 根据订单ID生成取餐号（后三位余数，限制最大 1000 单每日防碰撞）
 * @param {Number|String} orderId 订单ID
 */
export function generatePickupCode(orderId) {
  const num = parseInt(orderId) || 0;
  return 'A' + String(num % 1000).padStart(3, '0');
}
