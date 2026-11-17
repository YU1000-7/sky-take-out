import request from '../utils/request'

/**
 * 订单状态口径，与后端 com.sky.common.enums.OrderStatus 保持一致。
 */
export const ORDER_STATUS_LIST = [
  { value: 1, label: '待接单', tag: 'warning', tip: '等待商家接单' },
  { value: 2, label: '已接单', tag: 'primary', tip: '商家已接单，正在备餐' },
  { value: 3, label: '派送中', tag: 'success', tip: '骑手正在派送，请保持电话畅通' },
  { value: 4, label: '已完成', tag: 'default', tip: '订单已完成，感谢您的惠顾' },
  { value: 5, label: '已取消', tag: 'danger', tip: '订单已取消' }
]

export function statusOf(status) {
  return (
    ORDER_STATUS_LIST.find(item => item.value === status) || {
      value: status,
      label: '未知状态',
      tag: 'default',
      tip: '订单状态异常，请联系商家'
    }
  )
}

export function statusLabel(status) {
  return statusOf(status).label
}

export function statusTag(status) {
  return statusOf(status).tag
}

export function statusTip(status) {
  return statusOf(status).tip
}

/** 金额单位分，展示时换算成元 */
export function formatPrice(price) {
  return (Number(price || 0) / 100).toFixed(2)
}

/** 收货地址拼成一整行，省市区名称可能为空 */
export function joinAddress(address) {
  if (!address) {
    return ''
  }
  return [address.provinceName, address.cityName, address.districtName, address.detail]
    .filter(Boolean)
    .join(' ')
}
