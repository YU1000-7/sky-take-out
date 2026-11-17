import request from '../utils/request'

export function getSetmealPage(params) {
  return request.get('/admin/setmeal/page', { params })
}

export function getSetmealById(id) {
  return request.get(`/admin/setmeal/${id}`)
}

export function createSetmeal(data) {
  return request.post('/admin/setmeal', data)
}

export function updateSetmeal(data) {
  return request.put('/admin/setmeal', data)
}

export function deleteSetmeal(ids) {
  return request.delete('/admin/setmeal', { params: { ids } })
}

export function changeSetmealStatus(id, status) {
  return request.put(`/admin/setmeal/status/${status}`, null, {
    params: { id }
  })
}
