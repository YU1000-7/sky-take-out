<template>
  <section class="dashboard">
    <div class="page-heading">
      <div>
        <h2>仪表盘</h2>
        <p>门店今日经营概览：营业额、订单量、待接单提醒和近 7 天趋势都来自订单与报表接口。</p>
      </div>
      <el-button icon="el-icon-refresh" :loading="loading" @click="load">刷新</el-button>
    </div>

    <div class="stat-grid">
      <article class="stat-card">
        <p class="stat-label">今日营业额</p>
        <strong class="stat-value">￥{{ formatPrice(todayTurnover) }}</strong>
        <p class="stat-foot">
          较昨日
          <span :class="trendClass">{{ trendText }}</span>
        </p>
      </article>
      <article class="stat-card">
        <p class="stat-label">今日有效订单</p>
        <strong class="stat-value">{{ todayValidOrders }}</strong>
        <p class="stat-foot">今日下单 {{ todayOrderCount }} 单</p>
      </article>
      <article class="stat-card">
        <p class="stat-label">待接单</p>
        <strong class="stat-value warning">{{ pendingTotal }}</strong>
        <p class="stat-foot">
          <router-link class="link" to="/order?status=1">去处理</router-link>
        </p>
      </article>
      <article class="stat-card">
        <p class="stat-label">近 7 日营业额</p>
        <strong class="stat-value">￥{{ formatPrice(weekTurnover) }}</strong>
        <p class="stat-foot">已完成订单口径</p>
      </article>
    </div>

    <div class="main-grid">
      <div class="page-card chart-card">
        <header class="card-head">
          <div>
            <h3>近 7 日营业额</h3>
            <p>只统计已完成订单，区间内无订单的日期补 0</p>
          </div>
          <router-link class="link" to="/report">查看完整报表</router-link>
        </header>
        <div ref="turnoverChart" class="chart" />
      </div>

      <div class="page-card pending-card">
        <header class="card-head">
          <div>
            <h3>待接单提醒</h3>
            <p>新订单请尽快接单，客户只能在待接单时取消</p>
          </div>
        </header>

        <div v-if="!pendingRecords.length" class="empty">暂无待接单订单</div>

        <ul v-else class="pending-list">
          <li v-for="item in pendingRecords" :key="item.id">
            <div class="pending-info">
              <strong>{{ item.number }}</strong>
              <small>{{ item.orderTime }}</small>
            </div>
            <div class="pending-meta">
              <span>￥{{ formatPrice(item.amount) }}</span>
              <el-button type="text" @click="handleConfirm(item)">接单</el-button>
            </div>
          </li>
        </ul>
      </div>
    </div>

    <div class="page-card quick-card">
      <header class="card-head">
        <div>
          <h3>快捷入口</h3>
          <p>日常运营高频操作</p>
        </div>
      </header>
      <div class="quick-links">
        <router-link class="quick-item" to="/order">
          <i class="el-icon-tickets" />
          <span>订单管理</span>
        </router-link>
        <router-link class="quick-item" to="/report">
          <i class="el-icon-data-line" />
          <span>营业报表</span>
        </router-link>
        <router-link class="quick-item" to="/dish">
          <i class="el-icon-dish" />
          <span>菜品管理</span>
        </router-link>
        <router-link class="quick-item" to="/setmeal">
          <i class="el-icon-food" />
          <span>套餐管理</span>
        </router-link>
      </div>
    </div>
  </section>
</template>

<script>
import * as echarts from 'echarts'
import { confirmOrder, getOrderPage } from '../../api/order'
import { getOrderStatistics, getTurnover } from '../../api/report'
import { formatPrice } from '../../utils/order'

const THEME_COLOR = '#f28a2e'

function toDateString(date) {
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

export default {
  name: 'DashboardPage',
  data() {
    return {
      loading: false,
      turnoverList: [],
      statistics: {
        orderCountList: [],
        validOrderCountList: []
      },
      pendingRecords: [],
      pendingTotal: 0,
      chart: null
    }
  },
  computed: {
    todayTurnover() {
      return this.turnoverList.length
        ? this.turnoverList[this.turnoverList.length - 1].turnover
        : 0
    },
    yesterdayTurnover() {
      return this.turnoverList.length > 1
        ? this.turnoverList[this.turnoverList.length - 2].turnover
        : 0
    },
    weekTurnover() {
      return this.turnoverList.reduce((sum, item) => sum + (item.turnover || 0), 0)
    },
    trendText() {
      const yesterday = this.yesterdayTurnover || 0
      const today = this.todayTurnover || 0
      if (!yesterday) {
        return today > 0 ? '新增营业额' : '持平'
      }
      const rate = ((today - yesterday) / yesterday) * 100
      const sign = rate > 0 ? '+' : ''
      return `${sign}${rate.toFixed(1)}%`
    },
    trendClass() {
      if (!this.yesterdayTurnover) {
        return 'trend flat'
      }
      return this.todayTurnover >= this.yesterdayTurnover ? 'trend up' : 'trend down'
    },
    todayOrderCount() {
      const list = this.statistics.orderCountList || []
      return list.length ? list[list.length - 1].orderCount || 0 : 0
    },
    todayValidOrders() {
      const list = this.statistics.validOrderCountList || []
      return list.length ? list[list.length - 1].validOrderCount || 0 : 0
    }
  },
  mounted() {
    this.initChart()
    window.addEventListener('resize', this.handleResize)
    this.load()
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
    if (this.chart) {
      this.chart.dispose()
      this.chart = null
    }
  },
  methods: {
    formatPrice,
    lastSevenDays() {
      const end = new Date()
      const begin = new Date()
      begin.setDate(end.getDate() - 6)
      return {
        begin: toDateString(begin),
        end: toDateString(end)
      }
    },
    async load() {
      this.loading = true
      const range = this.lastSevenDays()
      try {
        // 三个接口互不阻塞：任何一个失败时其他卡片仍然可以展示。
        const [turnover, statistics, pending] = await Promise.all([
          getTurnover(range).catch(() => []),
          getOrderStatistics(range).catch(() => ({
            orderCountList: [],
            validOrderCountList: []
          })),
          getOrderPage({ page: 1, pageSize: 5, status: 1 }).catch(() => ({
            records: [],
            total: 0
          }))
        ])
        this.turnoverList = turnover || []
        this.statistics = statistics || { orderCountList: [], validOrderCountList: [] }
        this.pendingRecords = pending.records || []
        this.pendingTotal = pending.total || 0
        this.renderChart()
      } catch (error) {
        this.$message.error(error.message)
      } finally {
        this.loading = false
      }
    },
    initChart() {
      if (!this.$refs.turnoverChart) {
        return
      }
      this.chart = echarts.init(this.$refs.turnoverChart)
    },
    renderChart() {
      if (!this.chart) {
        this.initChart()
      }
      if (!this.chart) {
        return
      }
      const dates = this.turnoverList.map(item => (item.date || '').slice(5))
      const values = this.turnoverList.map(item => Number(item.turnover || 0) / 100)
      this.chart.setOption({
        grid: { left: 8, right: 16, top: 24, bottom: 8, containLabel: true },
        tooltip: {
          trigger: 'axis',
          valueFormatter: value => `￥${Number(value).toFixed(2)}`
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: dates,
          axisLine: { lineStyle: { color: '#e5e7eb' } },
          axisLabel: { color: '#8a94a3' }
        },
        yAxis: {
          type: 'value',
          splitLine: { lineStyle: { color: '#f2f4f7' } },
          axisLabel: { color: '#8a94a3' }
        },
        series: [
          {
            name: '营业额',
            type: 'line',
            smooth: true,
            symbolSize: 6,
            data: values,
            itemStyle: { color: THEME_COLOR },
            lineStyle: { width: 3, color: THEME_COLOR },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(242, 138, 46, 0.35)' },
                { offset: 1, color: 'rgba(242, 138, 46, 0.02)' }
              ])
            }
          }
        ]
      })
    },
    handleResize() {
      if (this.chart) {
        this.chart.resize()
      }
    },
    async handleConfirm(row) {
      try {
        await this.$confirm(`确定接下订单 ${row.number} 吗？`, '接单确认', { type: 'warning' })
      } catch (error) {
        return
      }
      try {
        await confirmOrder(row.id)
        this.$message.success('已接单')
        this.load()
      } catch (error) {
        this.$message.error(error.message || '接单失败')
      }
    }
  }
}
</script>

<style scoped>
.dashboard {
  max-width: 1420px;
  margin: 0 auto;
}

.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 22px;
}

.page-heading h2 {
  margin: 0;
  font-size: 22px;
}

.page-heading p {
  max-width: 720px;
  margin: 8px 0 0;
  color: #7b8492;
  font-size: 13px;
  line-height: 1.7;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.stat-card {
  padding: 20px 22px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
}

.stat-label {
  margin: 0;
  color: #8a94a3;
  font-size: 12px;
}

.stat-value {
  display: block;
  margin: 10px 0 6px;
  font-size: 26px;
  font-weight: 700;
}

.stat-value.warning {
  color: #d97706;
}

.stat-foot {
  margin: 0;
  color: #9ba7b8;
  font-size: 12px;
}

.trend.up {
  color: #2f9e44;
}

.trend.down {
  color: #d14343;
}

.trend.flat {
  color: #8a94a3;
}

.main-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(0, 1fr);
  gap: 16px;
  margin-bottom: 18px;
}

.chart-card,
.pending-card,
.quick-card {
  padding: 20px 22px;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.card-head h3 {
  margin: 0;
  font-size: 15px;
}

.card-head p {
  margin: 6px 0 0;
  color: #8a94a3;
  font-size: 12px;
}

.chart {
  width: 100%;
  height: 260px;
}

.link {
  color: #354154;
  font-size: 12px;
  text-decoration: none;
}

.link:hover {
  color: #f28a2e;
}

.empty {
  padding: 46px 0;
  color: #9ba7b8;
  font-size: 13px;
  text-align: center;
}

.pending-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.pending-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px dashed #eef1f5;
}

.pending-list li:last-child {
  border-bottom: none;
}

.pending-info strong,
.pending-info small {
  display: block;
}

.pending-info strong {
  font-size: 13px;
}

.pending-info small {
  margin-top: 4px;
  color: #8a94a3;
  font-size: 11px;
}

.pending-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #d14343;
  font-size: 13px;
  font-weight: 600;
}

.quick-links {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.quick-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px;
  color: #354154;
  background: #f8fafb;
  border: 1px solid #eef1f5;
  border-radius: 10px;
  text-decoration: none;
  font-size: 13px;
}

.quick-item i {
  font-size: 17px;
}

.quick-item:hover {
  border-color: #f28a2e;
  color: #f28a2e;
}

@media (max-width: 1080px) {
  .main-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
