<template>
  <section class="report-page">
    <div class="page-heading">
      <div>
        <h2>营业报表</h2>
        <p>
          营业额与销量只统计已完成订单，订单总数含已取消订单。
          统计区间最长 90 天，区间内没有订单的日期按 0 展示，图表不会出现断点。
        </p>
      </div>
      <div class="range-actions">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="yyyy-MM-dd"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          unlink-panels
        />
        <el-button @click="setRecentDays(7)">近 7 天</el-button>
        <el-button @click="setRecentDays(30)">近 30 天</el-button>
        <el-button type="primary" icon="el-icon-search" :loading="loading" @click="load">
          查询
        </el-button>
      </div>
    </div>

    <el-alert
      v-if="loadError"
      class="report-alert"
      title="报表接口暂时不可用，请确认后端已启动并完成登录"
      type="warning"
      :closable="false"
      show-icon
    />

    <div class="summary-grid">
      <article class="summary-card">
        <p>营业额合计</p>
        <strong>￥{{ formatPrice(totalTurnover) }}</strong>
        <small>{{ rangeText }}</small>
      </article>
      <article class="summary-card">
        <p>有效订单</p>
        <strong>{{ totalValidOrders }}</strong>
        <small>已完成订单数</small>
      </article>
      <article class="summary-card">
        <p>订单总数</p>
        <strong>{{ totalOrders }}</strong>
        <small>含已取消订单</small>
      </article>
      <article class="summary-card">
        <p>平均客单价</p>
        <strong>￥{{ formatPrice(avgOrderValue) }}</strong>
        <small>营业额 / 有效订单</small>
      </article>
    </div>

    <div class="page-card chart-card">
      <header class="card-head">
        <h3>营业额趋势</h3>
        <span>单位：元</span>
      </header>
      <div ref="turnoverChart" v-loading="loading" class="chart" />
    </div>

    <div class="page-card chart-card">
      <header class="card-head">
        <h3>订单统计</h3>
        <span>订单总数 / 有效订单数</span>
      </header>
      <div ref="countChart" v-loading="loading" class="chart" />
    </div>

    <div class="page-card chart-card">
      <header class="card-head">
        <h3>销量 TOP10</h3>
        <span>按商品名称汇总销量</span>
      </header>
      <div v-if="!top10.length" class="empty">该区间暂无已完成订单的商品销量</div>
      <div v-show="top10.length" ref="topChart" v-loading="loading" class="chart" />
    </div>
  </section>
</template>

<script>
import * as echarts from 'echarts'
import { getOrderStatistics, getSalesTop10, getTurnover } from '../../api/report'
import { formatPrice } from '../../utils/order'

const BAR_COLOR = '#f28a2e'
const VALID_COLOR = '#37506d'

function toDateString(date) {
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

function recentRange(days) {
  const end = new Date()
  const begin = new Date()
  begin.setDate(end.getDate() - (days - 1))
  return [toDateString(begin), toDateString(end)]
}

export default {
  name: 'ReportPage',
  data() {
    return {
      loading: false,
      loadError: false,
      dateRange: recentRange(7),
      turnoverList: [],
      statistics: {
        orderCountList: [],
        validOrderCountList: []
      },
      top10: [],
      charts: {
        turnover: null,
        count: null,
        top: null
      }
    }
  },
  computed: {
    rangeText() {
      return this.dateRange && this.dateRange.length === 2
        ? `${this.dateRange[0]} 至 ${this.dateRange[1]}`
        : '未选择统计区间'
    },
    totalTurnover() {
      return this.turnoverList.reduce((sum, item) => sum + (item.turnover || 0), 0)
    },
    totalOrders() {
      return (this.statistics.orderCountList || []).reduce(
        (sum, item) => sum + (item.orderCount || 0),
        0
      )
    },
    totalValidOrders() {
      return (this.statistics.validOrderCountList || []).reduce(
        (sum, item) => sum + (item.validOrderCount || 0),
        0
      )
    },
    avgOrderValue() {
      return this.totalValidOrders ? Math.round(this.totalTurnover / this.totalValidOrders) : 0
    }
  },
  mounted() {
    this.initCharts()
    window.addEventListener('resize', this.handleResize)
    this.load()
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
    Object.keys(this.charts).forEach(key => {
      if (this.charts[key]) {
        this.charts[key].dispose()
        this.charts[key] = null
      }
    })
  },
  methods: {
    formatPrice,
    setRecentDays(days) {
      this.dateRange = recentRange(days)
      this.load()
    },
    async load() {
      if (!this.dateRange || this.dateRange.length !== 2) {
        this.$message.warning('请选择统计区间')
        return
      }
      this.loading = true
      const params = { begin: this.dateRange[0], end: this.dateRange[1] }
      let failed = 0
      try {
        // 三个接口独立降级：某一个失败不影响另外两块图表。
        const [turnover, statistics, top10] = await Promise.all([
          getTurnover(params).catch(() => {
            failed += 1
            return []
          }),
          getOrderStatistics(params).catch(() => {
            failed += 1
            return { orderCountList: [], validOrderCountList: [] }
          }),
          getSalesTop10(params).catch(() => {
            failed += 1
            return []
          })
        ])
        this.loadError = failed === 3
        this.turnoverList = turnover || []
        this.statistics = statistics || { orderCountList: [], validOrderCountList: [] }
        this.top10 = top10 || []
        this.$nextTick(this.renderCharts)
      } finally {
        this.loading = false
      }
    },
    initCharts() {
      if (this.$refs.turnoverChart) {
        this.charts.turnover = echarts.init(this.$refs.turnoverChart)
      }
      if (this.$refs.countChart) {
        this.charts.count = echarts.init(this.$refs.countChart)
      }
      if (this.$refs.topChart) {
        this.charts.top = echarts.init(this.$refs.topChart)
      }
    },
    renderCharts() {
      this.renderTurnoverChart()
      this.renderCountChart()
      this.renderTopChart()
    },
    renderTurnoverChart() {
      const chart = this.charts.turnover
      if (!chart) {
        return
      }
      chart.setOption(
        {
          grid: { left: 8, right: 24, top: 24, bottom: 8, containLabel: true },
          tooltip: {
            trigger: 'axis',
            valueFormatter: value => `￥${Number(value).toFixed(2)}`
          },
          xAxis: {
            type: 'category',
            data: this.turnoverList.map(item => (item.date || '').slice(5)),
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
              type: 'bar',
              barMaxWidth: 28,
              data: this.turnoverList.map(item => Number(item.turnover || 0) / 100),
              itemStyle: { color: BAR_COLOR, borderRadius: [6, 6, 0, 0] }
            }
          ]
        },
        true
      )
    },
    renderCountChart() {
      const chart = this.charts.count
      if (!chart) {
        return
      }
      const dates = (this.statistics.orderCountList || []).map(item => (item.date || '').slice(5))
      const orderCounts = (this.statistics.orderCountList || []).map(
        item => item.orderCount || 0
      )
      const validCounts = (this.statistics.validOrderCountList || []).map(
        item => item.validOrderCount || 0
      )
      chart.setOption(
        {
          grid: { left: 8, right: 24, top: 32, bottom: 8, containLabel: true },
          tooltip: { trigger: 'axis' },
          legend: { top: 0, right: 0, itemHeight: 8, textStyle: { color: '#8a94a3' } },
          xAxis: {
            type: 'category',
            data: dates,
            axisLine: { lineStyle: { color: '#e5e7eb' } },
            axisLabel: { color: '#8a94a3' }
          },
          yAxis: {
            type: 'value',
            minInterval: 1,
            splitLine: { lineStyle: { color: '#f2f4f7' } },
            axisLabel: { color: '#8a94a3' }
          },
          series: [
            {
              name: '订单总数',
              type: 'bar',
              barMaxWidth: 22,
              data: orderCounts,
              itemStyle: { color: VALID_COLOR, borderRadius: [4, 4, 0, 0] }
            },
            {
              name: '有效订单',
              type: 'bar',
              barMaxWidth: 22,
              data: validCounts,
              itemStyle: { color: BAR_COLOR, borderRadius: [4, 4, 0, 0] }
            }
          ]
        },
        true
      )
    },
    renderTopChart() {
      const chart = this.charts.top
      if (!chart) {
        return
      }
      const ranking = this.top10.slice().reverse()
      chart.setOption(
        {
          grid: { left: 8, right: 32, top: 16, bottom: 8, containLabel: true },
          tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
          xAxis: {
            type: 'value',
            minInterval: 1,
            splitLine: { lineStyle: { color: '#f2f4f7' } },
            axisLabel: { color: '#8a94a3' }
          },
          yAxis: {
            type: 'category',
            data: ranking.map(item => item.name),
            axisLine: { lineStyle: { color: '#e5e7eb' } },
            axisLabel: { color: '#4b5563' }
          },
          series: [
            {
              name: '销量',
              type: 'bar',
              barMaxWidth: 18,
              data: ranking.map(item => item.sales || 0),
              itemStyle: { color: BAR_COLOR, borderRadius: [0, 6, 6, 0] },
              label: { show: true, position: 'right', color: '#8a94a3' }
            }
          ]
        },
        true
      )
      chart.resize()
    },
    handleResize() {
      Object.keys(this.charts).forEach(key => {
        if (this.charts[key]) {
          this.charts[key].resize()
        }
      })
    }
  }
}
</script>

<style scoped>
.report-page {
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

.range-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.report-alert {
  margin-bottom: 18px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.summary-card {
  padding: 18px 20px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
}

.summary-card p {
  margin: 0;
  color: #8a94a3;
  font-size: 12px;
}

.summary-card strong {
  display: block;
  margin: 10px 0 6px;
  font-size: 24px;
}

.summary-card small {
  color: #9ba7b8;
  font-size: 11px;
}

.chart-card {
  padding: 20px 22px;
  margin-bottom: 16px;
}

.card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 10px;
}

.card-head h3 {
  margin: 0;
  font-size: 15px;
}

.card-head span {
  color: #9ba7b8;
  font-size: 12px;
}

.chart {
  width: 100%;
  height: 300px;
}

.empty {
  padding: 48px 0;
  color: #9ba7b8;
  font-size: 13px;
  text-align: center;
}

@media (max-width: 1080px) {
  .page-heading {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
