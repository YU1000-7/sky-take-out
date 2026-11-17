import request from '../utils/request'

/**
 * C 端套餐接口。分类复用 B 模块的 api/menu.js，不在此重复定义。
 */
export function getSetmealList(categoryId) {
  return request.get('/user/setmeal/list', {
    params: categoryId ? { categoryId } : {}
  })
}

export function getSetmealDishes(id) {
  return request.get(`/user/setmeal/dish/${id}`)
}
