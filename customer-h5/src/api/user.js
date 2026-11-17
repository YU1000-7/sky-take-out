import request from '../utils/request'

export function login(phone) {
  return request.post('/user/user/login', { phone })
}
