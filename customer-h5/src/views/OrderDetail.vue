<template>
  <div class="order-detail-page">
    <van-nav-bar title="订单详情" left-arrow @click-left="$router.push('/orders')" />

    <div v-if="loading" class="state">
      <van-loading size="20px">加载中...</van-loading>
    </div>

    <van-empty v-else-if="!order" description="订单不存在或已被删除">
      <van-button type="warning" round size="small" @click="$router.push('/orders')">
        返回订单列表
      </van-button>
    </van-empty>

    <template v-else>
      <section :class="['status-banner', statusClass]">
        <h2>{{ statusLabel(order.status) }}</h2>
        <p>{{ statusTip(order.status) }}</p>
      </section>

      <section class="goods-card">
        <h3>商品清单</h3>
        <article v-for="item in details" :key="item.id" class="goods-item">
          <img v-if="item.image" :src="item.image" :alt="item.name" class="goods-cover" >
          <div class="goods-info">
            <strong>{{ item.name }}</strong>
            <div class="goods-meta">
              <van-tag v-if="item.setmealId" plain type="warning" size="mini">套餐</van-tag>
              <span v-if="item.dishFlavor">{{ item.dishFlavor }}</span>
            </div>
          </div>
          <div class="goods-price">
            <span>￥{{ formatPrice(item.amount) }}</span>
            <small>× {{ item.number }}</small>
          </div>
        </article>
        <div class="goods-total">
          共 {{ totalNumber }} 件，合计 <strong>￥{{ formatPrice(order.amount) }}</strong>
        </div>
      </section>

      <section class="info-card">
        <van-cell title="订单号" :value="order.number" />
        <van-cell title="下单时间" :value="order.orderTime || '-'" />
        <van-cell
          v-if="order.estimateDeliveryTime"
          title="预计送达"
          :value="order.estimateDeliveryTime"
        />
        <van-cell v-if="order.deliveryTime" title="派送时间" :value="order.deliveryTime" />
        <van-cell v-if="order.rejectionReason" title="拒单原因" :value="order.rejectionReason" />
        <van-cell v-if="order.cancelReason" title="取消原因" :value="order.cancelReason" />
        <van-cell v-if="order.cancelTime" title="取消时间" :value="order.cancelTime" />
        <van-cell title="支付方式" :value="payMethodText" />
        <van-cell title="收货人" :value="order.consignee || '-'" />
        <van-cell title="联系电话" :value="order.phone || '-'" />
        <van-cell title="收货地址" :value="order.address || '-'" />
        <van-cell title="备注" :value="order.remark || '无'" />
      </section>

      <div class="actions">
        <van-button
          v-if="order.status === 1"
          block
          plain
          type="danger"
          round
          :loading="canceling"
          @click="handleCancel"
        >
          取消订单
        </van-button>
        <van-button block type="warning" round @click="$router.push('/orders')">
          返回订单列表
        </van-button>
      </div>
    </template>
  </div>
</template>

<script>
import { Dialog, Toast } from 'vant'
import { cancelOrder, getOrderDetail } from '../api/order'
import { formatPrice, statusLabel, statusTip } from '../utils/order'

const STATUS_CLASS = {
  1: 'pending',
  2: 'confirmed',
  3: 'delivering',
  4: 'completed',
  5: 'cancelled'
}

export default {
  name: 'OrderDetailPage',
  data() {
    return {
      loading: false,
      canceling: false,
      order: null,
      details: []
    }
  },
  computed: {
    totalNumber() {
      return this.details.reduce((sum, item) => sum + (item.number || 0), 0)
    },
    statusClass() {
      return STATUS_CLASS[this.order && this.order.status] || 'pending'
    },
    payMethodText() {
      return this.order && this.order.payMethod === 1 ? '微信支付（模拟）' : '其他方式'
    }
  },
  created() {
    this.load()
  },
  methods: {
    formatPrice,
    statusLabel,
    statusTip,
    async load() {
      this.loading = true
      try {
        const data = await getOrderDetail(this.$route.params.id)
        this.order = data.order || null
        this.details = data.orderDetailList || []
      } catch (error) {
        this.order = null
        this.details = []
        Toast.fail(error.message || '订单加载失败')
      } finally {
        this.loading = false
      }
    },
    async handleCancel() {
      if (this.canceling) {
        return
      }
      try {
        await Dialog.confirm({
          title: '取消订单',
          message: '商家接单之前可以取消，确认取消该订单吗？'
        })
      } catch (error) {
        return
      }
      this.canceling = true
      try {
        await cancelOrder(this.order.id)
        Toast.success('订单已取消')
        this.load()
      } catch (error) {
        Toast.fail(error.message || '取消失败')
      } finally {
        this.canceling = false
      }
    }
  }
}
</script>

<style scoped>
.order-detail-page {
  min-height: 100vh;
  padding-bottom: 40px;
  background: #f6f7f8;
}

.state {
  display: grid;
  place-items: center;
  padding: 60px 0;
}

.status-banner {
  padding: 26px 22px;
  color: #fff;
  background: #202938;
}

.status-banner.pending {
  background: linear-gradient(135deg, #f2994a, #f28a2e);
}

.status-banner.confirmed {
  background: linear-gradient(135deg, #4e6a8a, #37506d);
}

.status-banner.delivering {
  background: linear-gradient(135deg, #2f9e44, #26813a);
}

.status-banner.completed {
  background: linear-gradient(135deg, #4b5563, #374151);
}

.status-banner.cancelled {
  background: linear-gradient(135deg, #9aa4b2, #7b8492);
}

.status-banner h2 {
  margin: 0;
  font-size: 22px;
}

.status-banner p {
  margin: 8px 0 0;
  color: rgba(255, 255, 255, 0.86);
  font-size: 13px;
}

.goods-card,
.info-card {
  margin: 12px 14px 0;
  background: #fff;
  border: 1px solid #eceff3;
  border-radius: 14px;
  overflow: hidden;
}

.goods-card {
  padding: 14px 16px 12px;
}

.goods-card h3 {
  margin: 0 0 6px;
  font-size: 14px;
  color: #323233;
}

.goods-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px dashed #f2f3f5;
}

.goods-cover {
  width: 52px;
  height: 52px;
  object-fit: cover;
  border-radius: 8px;
  background: #f7f8fa;
}

.goods-info {
  flex: 1;
  min-width: 0;
}

.goods-info strong {
  display: block;
  font-size: 14px;
  color: #323233;
}

.goods-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  color: #969799;
  font-size: 12px;
}

.goods-price {
  text-align: right;
}

.goods-price span {
  display: block;
  color: #ee0a24;
  font-size: 14px;
  font-weight: 600;
}

.goods-price small {
  color: #969799;
  font-size: 12px;
}

.goods-total {
  padding-top: 12px;
  color: #5b6472;
  font-size: 12px;
  text-align: right;
}

.goods-total strong {
  margin-left: 6px;
  color: #f2622e;
  font-size: 18px;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 20px 16px 0;
}
</style>
