import request from '../utils/request'

/**
 * 营业报表接口，参数：begin、end，格式 yyyy-MM-dd。
 * 三个接口的统计区间口径一致，页面统一用同一个日期范围。
 */
export function getTurnover(params) {
  return request.get('/admin/report/turnover', { params })
}

export function getOrderStatistics(params) {
  return request.get('/admin/report/orderStatistics', { params })
}

export function getSalesTop10(params) {
  return request.get('/admin/report/top10', { params })
}
