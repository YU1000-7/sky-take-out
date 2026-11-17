import request from '../utils/request'

/**
 * 购物车接口。dishId 与 setmealId 二选一，口味仅菜品需要。
 */
export function addCart(data) {
  return request.post('/user/shoppingCart', data)
}

export function subCart(data) {
  return request.put('/user/shoppingCart/sub', data)
}

export function getCartList() {
  return request.get('/user/shoppingCart/list')
}

export function cleanCart() {
  return request.delete('/user/shoppingCart/clean')
}
