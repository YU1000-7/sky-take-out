import request from '../utils/request'

export function login(phone, password) {
  return request.post('/user/user/login', { phone, password })
}
