import request from '../utils/request'

export function listAddresses() {
  return request.get('/user/addressBook/list')
}

export function getAddress(id) {
  return request.get(`/user/addressBook/${id}`)
}

export function createAddress(data) {
  return request.post('/user/addressBook', data)
}

export function updateAddress(data) {
  return request.put('/user/addressBook', data)
}

export function deleteAddress(id) {
  return request.delete('/user/addressBook', { params: { id } })
}

export function setDefaultAddress(id) {
  return request.put('/user/addressBook/default', null, { params: { id } })
}
