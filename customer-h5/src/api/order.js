import request from '../utils/request'

/**
 * 订单接口。提交订单会一次性写入订单主表与明细并清空购物车，返回 { orderId }。
 */
export function submitOrder(data) {
  return request.post('/user/order/submit', data)
}

export function getOrderPage(params) {
  return request.get('/user/order/list', { params })
}

export function getOrderDetail(id) {
  return request.get(`/user/order/${id}`)
}

export function cancelOrder(id) {
  return request.put('/user/order/cancel', null, { params: { id } })
}

/**
 * 确认订单页读取购物车。接口本身是 C 模块的 /user/shoppingCart/list，
 * 但由 D 的 api 层发起调用，确认订单页不反向依赖 C 的前端模块。
 */
export function getCartList() {
  return request.get('/user/shoppingCart/list')
}
