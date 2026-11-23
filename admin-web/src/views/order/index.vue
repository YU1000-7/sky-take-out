<template>
  <section class="order-page">
    <div class="page-heading">
      <div>
        <h2>订单管理</h2>
        <p>
          状态流转：待接单 → 已接单 → 派送中 → 已完成，客户只能在待接单时取消。
          每一次流转都由后端校验当前状态，非法流转会直接返回业务异常。
        </p>
      </div>
      <el-button icon="el-icon-refresh" @click="loadOrders">刷新</el-button>
    </div>

    <div class="page-card toolbar">
      <el-input
        v-model.trim="query.number"
        clearable
        placeholder="按订单号搜索"
        prefix-icon="el-icon-search"
        @keyup.enter.native="handleSearch"
        @clear="handleSearch"
      />
      <el-select v-model="query.status" clearable placeholder="全部状态" @change="handleSearch">
        <el-option
          v-for="item in statusOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        value-format="yyyy-MM-dd"
        range-separator="至"
        start-placeholder="下单开始日期"
        end-placeholder="下单结束日期"
        unlink-panels
        @change="handleSearch"
      />
      <el-button type="primary" plain @click="handleSearch">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>

    <div class="page-card table-card">
      <el-table v-loading="loading" :data="records" row-key="id" stripe style="width: 100%">
        <el-table-column prop="number" label="订单号" min-width="150" />
        <el-table-column label="客户" min-width="140">
          <template slot-scope="{ row }">
            <div class="customer">
              <strong>{{ row.consignee || '-' }}</strong>
              <small>{{ row.phone || '-' }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="address" label="收货地址" min-width="210" show-overflow-tooltip />
        <el-table-column label="金额" width="110" align="right">
          <template slot-scope="{ row }">
            <span class="amount">￥{{ formatPrice(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderTime" label="下单时间" min-width="170" />
        <el-table-column label="操作" width="210" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" @click="openDetail(row)">详情</el-button>
            <el-button v-if="actionOf(row.status)" type="text" @click="handleAction(row)">
              {{ actionOf(row.status).label }}
            </el-button>
            <el-button v-if="row.status === 1" type="text" class="danger" @click="handleReject(row)">
              拒单
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :current-page="query.page"
          :page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <el-dialog
      title="订单详情"
      :visible.sync="dialogVisible"
      width="760px"
      :close-on-click-modal="false"
      @closed="resetDetail"
    >
      <div v-loading="detailLoading">
        <template v-if="detail.order">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="订单号">{{ detail.order.number }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusTag(detail.order.status)" size="small">
                {{ statusLabel(detail.order.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="下单时间">{{ detail.order.orderTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="预计送达">
              {{ detail.order.estimateDeliveryTime || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="收货人">{{ detail.order.consignee || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ detail.order.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="收货地址" :span="2">
              {{ detail.order.address || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="订单备注" :span="2">
              {{ detail.order.remark || '无' }}
            </el-descriptions-item>
            <el-descriptions-item label="支付方式">
              {{ payMethodLabel(detail.order.payMethod) }}
            </el-descriptions-item>
            <el-descriptions-item label="订单金额">
              ￥{{ formatPrice(detail.order.amount) }}
            </el-descriptions-item>
            <el-descriptions-item v-if="detail.order.deliveryTime" label="派送时间">
              {{ detail.order.deliveryTime }}
            </el-descriptions-item>
            <el-descriptions-item v-if="detail.order.cancelReason" label="取消原因">
              {{ detail.order.cancelReason }}
            </el-descriptions-item>
            <el-descriptions-item v-if="detail.order.rejectionReason" label="拒单原因">
              {{ detail.order.rejectionReason }}
            </el-descriptions-item>
            <el-descriptions-item v-if="detail.order.cancelTime" label="取消/拒单时间">
              {{ detail.order.cancelTime }}
            </el-descriptions-item>
          </el-descriptions>

          <el-table :data="detail.orderDetailList" size="small" class="detail-table">
            <el-table-column label="商品" min-width="190">
              <template slot-scope="{ row }">
                <span>{{ row.name }}</span>
                <el-tag v-if="row.setmealId" type="warning" size="mini">套餐</el-tag>
                <span v-if="row.dishFlavor" class="flavor">{{ row.dishFlavor }}</span>
              </template>
            </el-table-column>
            <el-table-column label="单价" width="100" align="right">
              <template slot-scope="{ row }">￥{{ formatPrice(row.amount) }}</template>
            </el-table-column>
            <el-table-column prop="number" label="数量" width="80" align="center" />
            <el-table-column label="小计" width="110" align="right">
              <template slot-scope="{ row }">
                ￥{{ formatPrice((row.amount || 0) * (row.number || 0)) }}
              </template>
            </el-table-column>
          </el-table>

          <div class="detail-total">
            共 {{ totalNumber }} 件，合计 <strong>￥{{ formatPrice(detail.order.amount) }}</strong>
          </div>
        </template>
      </div>

      <span slot="footer">
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button
          v-if="detail.order && detail.order.status === 1"
          type="danger"
          plain
          @click="handleReject(detail.order)"
        >
          拒单
        </el-button>
        <el-button
          v-if="actionOf(detail.order && detail.order.status)"
          type="primary"
          @click="handleAction(detail.order)"
        >
          {{ actionOf(detail.order && detail.order.status).label }}
        </el-button>
      </span>
    </el-dialog>
  </section>
</template>

<script>
import {
  completeOrder,
  confirmOrder,
  deliveryOrder,
  getOrderDetail,
  getOrderPage,
  rejectOrder
} from '../../api/order'
import {
  ORDER_STATUS_OPTIONS,
  formatPrice,
  nextAction,
  statusLabel,
  statusTag
} from '../../utils/order'

const ACTION_HANDLERS = {
  confirm: confirmOrder,
  delivery: deliveryOrder,
  complete: completeOrder
}

export default {
  name: 'OrderPage',
  data() {
    return {
      loading: false,
      detailLoading: false,
      dialogVisible: false,
      statusOptions: ORDER_STATUS_OPTIONS,
      dateRange: null,
      query: {
        page: 1,
        pageSize: 10,
        number: '',
        status: null
      },
      total: 0,
      records: [],
      detail: {
        order: null,
        orderDetailList: []
      }
    }
  },
  computed: {
    totalNumber() {
      return (this.detail.orderDetailList || []).reduce(
        (sum, item) => sum + (item.number || 0),
        0
      )
    }
  },
  created() {
    const status = Number(this.$route.query.status)
    if (status) {
      this.query.status = status
    }
    this.loadOrders()
  },
  methods: {
    formatPrice,
    statusLabel,
    statusTag,
    actionOf(status) {
      return nextAction(status)
    },
    payMethodLabel(payMethod) {
      return payMethod === 1 ? '微信支付（模拟）' : '其他方式'
    },
    buildParams() {
      const params = {
        page: this.query.page,
        pageSize: this.query.pageSize
      }
      if (this.query.number) {
        params.number = this.query.number
      }
      if (this.query.status !== null && this.query.status !== '') {
        params.status = this.query.status
      }
      if (this.dateRange && this.dateRange.length === 2) {
        params.beginTime = `${this.dateRange[0]} 00:00:00`
        params.endTime = `${this.dateRange[1]} 23:59:59`
      }
      return params
    },
    async loadOrders() {
      this.loading = true
      try {
        const data = await getOrderPage(this.buildParams())
        this.records = data.records || []
        this.total = data.total || 0
      } catch (error) {
        this.$message.error(error.message)
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.query.page = 1
      this.loadOrders()
    },
    resetSearch() {
      this.query = {
        page: 1,
        pageSize: this.query.pageSize,
        number: '',
        status: null
      }
      this.dateRange = null
      this.loadOrders()
    },
    handlePageChange(page) {
      this.query.page = page
      this.loadOrders()
    },
    handleSizeChange(pageSize) {
      this.query.pageSize = pageSize
      this.query.page = 1
      this.loadOrders()
    },
    async openDetail(row) {
      this.dialogVisible = true
      this.detailLoading = true
      try {
        const data = await getOrderDetail(row.id)
        this.detail = data || { order: null, orderDetailList: [] }
      } catch (error) {
        this.$message.error(error.message)
        this.dialogVisible = false
      } finally {
        this.detailLoading = false
      }
    },
    resetDetail() {
      this.detail = { order: null, orderDetailList: [] }
    },
    /**
     * 接单 / 派送 / 完成统一入口：先确认再调用，成功后刷新列表与详情。
     */
    async handleAction(row) {
      if (!row) {
        return
      }
      const action = nextAction(row.status)
      if (!action) {
        return
      }
      try {
        await this.$confirm(
          `订单 ${row.number} 当前状态为「${statusLabel(row.status)}」，确定${action.label}吗？`,
          `${action.label}确认`,
          { type: 'warning' }
        )
      } catch (error) {
        return
      }

      try {
        await ACTION_HANDLERS[action.key](row.id)
        this.$message.success(`已${action.label}`)
        this.loadOrders()
        if (this.dialogVisible && this.detail.order && this.detail.order.id === row.id) {
          await this.openDetail(row)
        }
      } catch (error) {
        this.$message.error(error.message || `${action.label}失败`)
      }
    },
    /**
     * 拒单：只有待接单能拒，先把原因问出来（可留空，后端记「商家拒单」），再二次确认。
     */
    async handleReject(row) {
      let rejectionReason = ''
      try {
        const result = await this.$prompt(
          `拒单后订单会进入「已取消」，客户能看到拒单原因。订单号：${row.number}`,
          '拒单原因',
          {
            confirmButtonText: '确认拒单',
            cancelButtonText: '取消',
            inputPlaceholder: '例如：菜品已售完 / 门店已打烊（可留空）',
            inputValidator: value => !value || value.length <= 100 || '拒单原因不能超过 100 字',
            type: 'warning'
          }
        )
        rejectionReason = (result.value || '').trim()
      } catch (error) {
        return
      }

      try {
        await rejectOrder(row.id, rejectionReason)
        this.$message.success('已拒单')
        this.loadOrders()
        if (this.dialogVisible && this.detail.order && this.detail.order.id === row.id) {
          await this.openDetail(row)
        }
      } catch (error) {
        this.$message.error(error.message || '拒单失败')
      }
    }
  }
}
</script>

<style scoped>
.order-page {
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
  max-width: 760px;
  margin: 8px 0 0;
  color: #7b8492;
  font-size: 13px;
  line-height: 1.7;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 16px 18px;
  margin-bottom: 18px;
}

.toolbar .el-input {
  width: 220px;
}

.toolbar .el-select {
  width: 150px;
}

.table-card {
  padding: 4px 0 18px;
  overflow: hidden;
}

.customer strong,
.customer small {
  display: block;
}

.customer strong {
  font-size: 13px;
}

.customer small {
  margin-top: 3px;
  color: #8a94a3;
  font-size: 11px;
}

.amount {
  color: #d14343;
  font-weight: 600;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  padding: 20px 18px 0;
}

.detail-table {
  margin-top: 18px;
}

.flavor {
  margin-left: 8px;
  color: #8a94a3;
  font-size: 12px;
}

.detail-total {
  margin-top: 14px;
  color: #5b6472;
  font-size: 13px;
  text-align: right;
}

.detail-total strong {
  color: #d14343;
  font-size: 18px;
}

@media (max-width: 900px) {
  .page-heading {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
