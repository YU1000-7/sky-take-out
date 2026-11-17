<template>
  <div class="orders-page">
    <van-nav-bar title="我的订单" left-arrow @click-left="$router.push('/home')" />

    <van-tabs v-model="activeStatus" sticky @change="handleTabChange">
      <van-tab v-for="tab in tabs" :key="tab.value" :title="tab.label" :name="tab.value" />
    </van-tabs>

    <van-list
      v-model="loading"
      :finished="finished"
      :error.sync="loadError"
      error-text="加载失败，点击重试"
      finished-text="没有更多订单了"
      @load="loadMore"
    >
      <article v-for="order in orders" :key="order.id" class="order-card" @click="goDetail(order)">
        <header>
          <span class="number">{{ order.number }}</span>
          <van-tag :type="statusTag(order.status)" plain>{{ statusLabel(order.status) }}</van-tag>
        </header>

        <p class="time">{{ order.orderTime }}</p>

        <div class="summary">
          <span class="goods-count">{{ order.consignee || '收货人' }} · {{ order.phone || '-' }}</span>
          <span class="amount">￥{{ formatPrice(order.amount) }}</span>
        </div>

        <footer>
          <span class="tip">{{ statusTip(order.status) }}</span>
          <div class="ops">
            <van-button
              v-if="order.status === 1"
              size="mini"
              plain
              type="danger"
              @click.stop="handleCancel(order)"
            >
              取消订单
            </van-button>
            <van-button size="mini" round type="warning" @click.stop="goDetail(order)">
              查看详情
            </van-button>
          </div>
        </footer>
      </article>
    </van-list>

    <van-empty v-if="!loading && !orders.length" description="还没有相关订单">
      <van-button type="warning" round size="small" @click="$router.push('/home')">
        去点餐
      </van-button>
    </van-empty>

    <van-tabbar route>
      <van-tabbar-item to="/home" icon="shop-o">点餐</van-tabbar-item>
      <van-tabbar-item to="/cart" icon="shopping-cart-o">购物车</van-tabbar-item>
      <van-tabbar-item to="/orders" icon="orders-o">订单</van-tabbar-item>
      <van-tabbar-item to="/address" icon="location-o">地址</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script>
import { Dialog, Toast } from 'vant'
import { cancelOrder, getOrderPage } from '../api/order'
import { ORDER_STATUS_LIST, formatPrice, statusLabel, statusTag, statusTip } from '../utils/order'

export default {
  name: 'OrdersPage',
  data() {
    return {
      activeStatus: 0,
      tabs: [{ value: 0, label: '全部' }].concat(
        ORDER_STATUS_LIST.map(item => ({ value: item.value, label: item.label }))
      ),
      orders: [],
      page: 1,
      pageSize: 10,
      loading: false,
      finished: false,
      loadError: false,
      requesting: false,
      canceling: false
    }
  },
  methods: {
    formatPrice,
    statusLabel,
    statusTag,
    statusTip,
    handleTabChange() {
      this.orders = []
      this.page = 1
      this.finished = false
      this.loadError = false
      this.loading = false
      this.requesting = false
      this.loadMore()
    },
    async loadMore() {
      if (this.requesting || this.finished) {
        return
      }
      this.requesting = true
      this.loading = true
      try {
        const params = { page: this.page, pageSize: this.pageSize }
        if (this.activeStatus) {
          params.status = this.activeStatus
        }
        const data = await getOrderPage(params)
        const records = data.records || []
        this.orders = this.orders.concat(records)
        this.finished = this.orders.length >= (data.total || 0)
        this.page += 1
      } catch (error) {
        this.loadError = true
        Toast.fail(error.message || '订单加载失败')
      } finally {
        this.requesting = false
        this.loading = false
      }
    },
    async handleCancel(order) {
      if (this.canceling) {
        return
      }
      try {
        await Dialog.confirm({
          title: '取消订单',
          message: `订单 ${order.number} 还未接单，确认取消吗？`
        })
      } catch (error) {
        return
      }
      this.canceling = true
      try {
        await cancelOrder(order.id)
        Toast.success('订单已取消')
        this.orders = []
        this.page = 1
        this.finished = false
        this.requesting = false
        this.loadMore()
      } catch (error) {
        Toast.fail(error.message || '取消失败')
      } finally {
        this.canceling = false
      }
    },
    goDetail(order) {
      this.$router.push(`/orders/${order.id}`)
    }
  }
}
</script>

<style scoped>
.orders-page {
  min-height: 100vh;
  padding-bottom: 60px;
  background: #f6f7f8;
}

.order-card {
  margin: 12px 14px 0;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #eceff3;
  border-radius: 14px;
}

.order-card header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.number {
  font-size: 13px;
  font-weight: 600;
  color: #323233;
}

.time {
  margin: 6px 0 0;
  color: #969799;
  font-size: 12px;
}

.summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #f2f3f5;
}

.goods-count {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  color: #7b8492;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.amount {
  color: #ee0a24;
  font-size: 17px;
  font-weight: 600;
}

.order-card footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 12px;
}

.tip {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  color: #a0a6b0;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ops {
  display: flex;
  gap: 8px;
}
</style>
