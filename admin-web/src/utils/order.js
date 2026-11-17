/**
 * 订单状态口径，与后端 com.sky.common.enums.OrderStatus 保持一致。
 * B 端和 C 端页面共用同一份定义，避免各处散落魔法数字。
 */
export const ORDER_STATUS_OPTIONS = [
  { value: 1, label: '待接单', tag: 'warning' },
  { value: 2, label: '已接单', tag: '' },
  { value: 3, label: '派送中', tag: 'success' },
  { value: 4, label: '已完成', tag: 'info' },
  { value: 5, label: '已取消', tag: 'danger' }
]

export function statusLabel(status) {
  const matched = ORDER_STATUS_OPTIONS.find(item => item.value === status)
  return matched ? matched.label : '未知状态'
}

export function statusTag(status) {
  const matched = ORDER_STATUS_OPTIONS.find(item => item.value === status)
  return matched ? matched.tag : 'info'
}

/** 金额单位分，展示时换算成元 */
export function formatPrice(price) {
  if (price === null || price === undefined) {
    return '0.00'
  }
  return (Number(price) / 100).toFixed(2)
}

/** 商家端下一个可执行动作，返回 null 表示没有可执行动作 */
export function nextAction(status) {
  if (status === 1) {
    return { key: 'confirm', label: '接单' }
  }
  if (status === 2) {
    return { key: 'delivery', label: '派送' }
  }
  if (status === 3) {
    return { key: 'complete', label: '完成' }
  }
  return null
}
