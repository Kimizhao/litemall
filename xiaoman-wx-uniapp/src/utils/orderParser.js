/**
 * 解析订单备注中的预订/自取信息
 * @param {String} message 订单备注留言
 * @returns {Object|null} 解析成功返回预约对象，匹配失败返回null
 */
export function parseOrderRemark(message) {
  if (!message || typeof message !== 'string') return null;
  try {
    if (!message.startsWith('[')) {
      return null;
    }
    
    // 提取主要字段
    const tagMatch = message.match(/^\[(.*?)\]/);
    const tag = tagMatch ? tagMatch[1] : '';
    
    const modeMatch = message.match(/方式:(.*?)(?=\s*\|)/);
    const timeMatch = message.match(/时间:(.*?)(?=\s*\|)/);
    const nameMatch = message.match(/称呼:(.*?)(?=\s*\|)/);
    const phoneMatch = message.match(/电话:(.*?)(?=\s*\|)/);
    const remarkMatch = message.match(/备注:([\s\S]*?)$/);

    const remarkText = remarkMatch ? remarkMatch[1].trim() : '';

    return {
      isPickup: true,
      type: tag,
      mode: modeMatch ? modeMatch[1].trim() : tag,
      time: timeMatch ? timeMatch[1].trim() : '',
      name: nameMatch ? nameMatch[1].trim() : '',
      phone: phoneMatch ? phoneMatch[1].trim() : '',
      remark: (remarkText === '无' || remarkText === '无备注') ? '' : remarkText
    };
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
