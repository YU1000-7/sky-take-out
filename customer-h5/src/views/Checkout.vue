<template>
  <div class="checkout-page">
    <van-nav-bar title="确认订单" left-arrow @click-left="goBack" />

    <div v-if="loading" class="state">
      <van-loading size="20px">加载中...</van-loading>
    </div>

    <template v-else>
      <section class="address-card" @click="openAddressPicker">
        <template v-if="selectedAddress">
          <div class="address-head">
            <strong>{{ selectedAddress.consignee }}</strong>
            <span>{{ selectedAddress.phone }}</span>
            <van-tag v-if="selectedAddress.isDefault === 1" plain type="warning" size="mini">
              默认
            </van-tag>
          </div>
          <p class="address-text">{{ addressText(selectedAddress) }}</p>
        </template>
        <p v-else class="address-empty">还没有收货地址，点击新增</p>
        <van-icon name="arrow" class="address-arrow" />
      </section>

      <section class="goods-card">
        <h3>商品清单</h3>
        <article v-for="item in items" :key="item.id" class="goods-item">
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
        <div v-if="!items.length" class="empty">购物车是空的，先去点餐吧</div>
      </section>

      <section class="form-card">
        <van-field
          v-model="remark"
          label="订单备注"
          type="textarea"
          rows="2"
          maxlength="100"
          show-word-limit
          placeholder="口味、餐具等要求（选填）"
        />
        <van-cell title="支付方式" value="微信支付（模拟）" />
        <van-cell title="预计送达" :value="estimateText" />
        <van-cell title="商品件数" :value="totalNumber + ' 件'" />
      </section>

      <p class="tips">提交后商家会尽快接单；接单之前可以取消订单。</p>

      <div class="footer">
        <div class="total">
          合计
          <strong>￥{{ totalAmount }}</strong>
        </div>
        <van-button
          type="warning"
          round
          :loading="submitting"
          :disabled="!canSubmit"
          @click="submit"
        >
          提交订单
        </van-button>
      </div>

      <van-popup v-model="pickerVisible" position="bottom" round closeable>
        <div class="picker">
          <header class="picker-head">
            <strong>选择收货地址</strong>
            <span class="picker-add" @click="goAddressPage">新增地址</span>
          </header>
          <div v-if="!addresses.length" class="empty">还没有收货地址</div>
          <ul v-else class="picker-list">
            <li
              v-for="item in addresses"
              :key="item.id"
              :class="{ active: selectedAddress && selectedAddress.id === item.id }"
              @click="selectAddress(item)"
            >
              <div class="picker-person">
                <strong>{{ item.consignee }}</strong>
                <span>{{ item.phone }}</span>
                <van-tag v-if="item.isDefault === 1" plain type="warning" size="mini">默认</van-tag>
              </div>
              <p>{{ addressText(item) }}</p>
            </li>
          </ul>
        </div>
      </van-popup>
    </template>
  </div>
</template>

<script>
import { Dialog, Toast } from 'vant'
import { listAddresses } from '../api/address'
import { getCartList, submitOrder } from '../api/order'
import { formatPrice, joinAddress } from '../utils/order'

export default {
  name: 'CheckoutPage',
  data() {
    return {
      loading: false,
      submitting: false,
      items: [],
      addresses: [],
      selectedAddress: null,
      remark: '',
      pickerVisible: false
    }
  },
  computed: {
    totalNumber() {
      return this.items.reduce((sum, item) => sum + (item.number || 0), 0)
    },
    totalAmount() {
      const amount = this.items.reduce(
        (sum, item) => sum + (item.number || 0) * (item.amount || 0),
        0
      )
      return formatPrice(amount)
    },
    canSubmit() {
      return Boolean(this.selectedAddress && this.items.length && !this.submitting)
    },
    estimateText() {
      if (!this.items.length) {
        return '-'
      }
      const estimate = new Date(Date.now() + 30 * 60 * 1000)
      const hours = `${estimate.getHours()}`.padStart(2, '0')
      const minutes = `${estimate.getMinutes()}`.padStart(2, '0')
      return `约 ${hours}:${minutes} 送达（下单后 30 分钟）`
    }
  },
  created() {
    this.load()
  },
  methods: {
    formatPrice,
    addressText(address) {
      return joinAddress(address) || '-'
    },
    async load() {
      this.loading = true
      try {
        const [carts, addresses] = await Promise.all([
          getCartList().catch(() => []),
          listAddresses().catch(() => [])
        ])
        this.items = carts || []
        this.addresses = addresses || []
        this.selectedAddress =
          this.addresses.find(item => item.isDefault === 1) || this.addresses[0] || null
      } finally {
        this.loading = false
      }
    },
    openAddressPicker() {
      if (!this.addresses.length) {
        this.goAddressPage()
        return
      }
      this.pickerVisible = true
    },
    selectAddress(address) {
      this.selectedAddress = address
      this.pickerVisible = false
    },
    goAddressPage() {
      this.pickerVisible = false
      this.$router.push('/address')
    },
    async submit() {
      if (!this.selectedAddress) {
        Toast('请先选择收货地址')
        return
      }
      if (!this.items.length) {
        Toast('购物车是空的')
        return
      }

      try {
        await Dialog.confirm({
          title: '提交订单',
          message: `共 ${this.totalNumber} 件商品，合计 ￥${this.totalAmount}，确认提交吗？`
        })
      } catch (error) {
        return
      }

      this.submitting = true
      try {
        const data = await submitOrder({
          addressBookId: this.selectedAddress.id,
          payMethod: 1,
          remark: this.remark || null
        })
        Toast.success('下单成功')
        this.$router.replace(`/orders/${data.orderId}`)
      } catch (error) {
        Toast.fail(error.message || '下单失败')
      } finally {
        this.submitting = false
      }
    },
    goBack() {
      this.$router.back()
    }
  }
}
</script>

<style scoped>
.checkout-page {
  min-height: 100vh;
  padding-bottom: 96px;
  background: #f6f7f8;
}

.state {
  display: grid;
  place-items: center;
  padding: 60px 0;
}

.address-card {
  position: relative;
  margin: 12px 14px 0;
  padding: 16px 40px 16px 16px;
  background: #fff;
  border-radius: 14px;
  border: 1px solid #eceff3;
}

.address-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.address-head strong {
  font-size: 16px;
}

.address-head span {
  color: #7b8492;
  font-size: 13px;
}

.address-text {
  margin: 10px 0 0;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.6;
}

.address-empty {
  margin: 0;
  color: #f28a2e;
  font-size: 14px;
}

.address-arrow {
  position: absolute;
  top: 50%;
  right: 16px;
  color: #c8c9cc;
  transform: translateY(-50%);
}

.goods-card,
.form-card {
  margin: 12px 14px 0;
  background: #fff;
  border-radius: 14px;
  border: 1px solid #eceff3;
  overflow: hidden;
}

.goods-card {
  padding: 14px 16px 6px;
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

.goods-item:last-of-type {
  border-bottom: none;
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

.empty {
  padding: 28px 0;
  color: #969799;
  font-size: 13px;
  text-align: center;
}

.tips {
  margin: 14px 24px 0;
  color: #a0a6b0;
  font-size: 11px;
  line-height: 1.7;
}

.footer {
  position: fixed;
  bottom: 0;
  left: 50%;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: min(100vw, 560px);
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #eceff3;
  transform: translateX(-50%);
}

.total {
  color: #5b6472;
  font-size: 12px;
}

.total strong {
  margin-left: 6px;
  color: #f2622e;
  font-size: 20px;
}

.picker {
  max-height: 68vh;
  padding: 18px 16px 20px;
  overflow-y: auto;
}

.picker-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid #f2f3f5;
}

.picker-add {
  color: #f28a2e;
  font-size: 13px;
}

.picker-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.picker-list li {
  padding: 14px 12px;
  margin-top: 10px;
  border: 1px solid #eceff3;
  border-radius: 12px;
}

.picker-list li.active {
  border-color: #f28a2e;
  background: #fff8f1;
}

.picker-person {
  display: flex;
  align-items: center;
  gap: 10px;
}

.picker-person span {
  color: #7b8492;
  font-size: 13px;
}

.picker-list p {
  margin: 8px 0 0;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.6;
}
</style>
