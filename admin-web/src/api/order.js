import request from '../utils/request'

/** B 端订单分页，参数：page、pageSize、number、status、beginTime、endTime */
export function getOrderPage(params) {
  return request.get('/admin/order/page', { params })
}

/** 订单详情，返回 { order, orderDetailList } */
export function getOrderDetail(id) {
  return request.get(`/admin/order/details/${id}`)
}

/** 商家接单：1 待接单 → 2 已接单 */
export function confirmOrder(id) {
  return request.put('/admin/order/confirm', null, { params: { id } })
}

/** 商家派送：2 已接单 → 3 派送中 */
export function deliveryOrder(id) {
  return request.put('/admin/order/delivery', null, { params: { id } })
}

/** 商家完成：3 派送中 → 4 已完成 */
export function completeOrder(id) {
  return request.put('/admin/order/complete', null, { params: { id } })
}

/**
 * 商家拒单：1 待接单 → 5 已取消，原因写入 rejection_reason。
 * rejectionReason 为空时后端记「商家拒单」。
 */
export function rejectOrder(id, rejectionReason) {
  return request.put('/admin/order/rejection', null, {
    params: { id, rejectionReason }
  })
}

/** 统一状态流转入口，body：{ id, status } */
export function changeOrderStatus(data) {
  return request.put('/admin/order/status', data)
}
