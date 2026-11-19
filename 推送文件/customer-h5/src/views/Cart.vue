<template>
  <div class="cart-page">
    <van-nav-bar title="购物车" left-arrow @click-left="goHome" />

    <div v-if="loading" class="state">
      <van-loading size="20px">加载中...</van-loading>
    </div>

    <template v-else-if="!items.length">
      <van-empty description="购物车还是空的，先去挑点好吃的">
        <van-button type="warning" round size="small" @click="goHome">去点餐</van-button>
      </van-empty>
    </template>

    <template v-else>
      <div class="list">
        <article v-for="item in items" :key="item.id" class="cart-item">
          <img :src="item.image" :alt="item.name" class="cover" >
          <div class="body">
            <h3>{{ item.name }}</h3>
            <p class="meta">
              <van-tag v-if="item.setmealId" plain type="warning" size="mini">套餐</van-tag>
              <span v-if="item.dishFlavor" class="flavor">{{ item.dishFlavor }}</span>
            </p>
            <div class="row">
              <span class="price">￥{{ formatPrice(item.amount) }}</span>
              <div class="stepper">
                <van-icon name="minus" @click="decrease(item)" />
                <span>{{ item.number }}</span>
                <van-icon name="plus" @click="increase(item)" />
              </div>
            </div>
          </div>
        </article>
      </div>

      <div class="footer">
        <div class="total">
          共 {{ totalNumber }} 件
          <strong>￥{{ totalAmount }}</strong>
        </div>
        <div class="ops">
          <van-button size="small" plain @click="clear">清空</van-button>
          <van-button size="small" type="warning" round @click="checkout">去结算</van-button>
        </div>
      </div>
    </template>

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
import { addCart, cleanCart, getCartList, subCart } from '../api/cart'

export default {
  name: 'CartPage',
  data() {
    return {
      loading: false,
      items: []
    }
  },
  computed: {
    totalNumber() {
      return this.items.reduce((sum, item) => sum + (item.number || 0), 0)
    },
    totalAmount() {
      const amount = this.items.reduce((sum, item) => sum + (item.number || 0) * (item.amount || 0), 0)
      return (amount / 100).toFixed(2)
    }
  },
  created() {
    this.loadCart()
  },
  methods: {
    formatPrice(price) {
      return (Number(price || 0) / 100).toFixed(2)
    },
    payloadOf(item) {
      if (item.setmealId) {
        return { setmealId: item.setmealId }
      }
      return { dishId: item.dishId, dishFlavor: item.dishFlavor || null }
    },
    async loadCart() {
      this.loading = true
      try {
        this.items = (await getCartList()) || []
      } catch (error) {
        this.items = []
        Toast.fail(error.message)
      } finally {
        this.loading = false
      }
    },
    async increase(item) {
      try {
        await addCart(this.payloadOf(item))
        await this.loadCart()
      } catch (error) {
        Toast.fail(error.message)
      }
    },
    async decrease(item) {
      try {
        await subCart(this.payloadOf(item))
        await this.loadCart()
      } catch (error) {
        Toast.fail(error.message)
      }
    },
    async clear() {
      try {
        await Dialog.confirm({
          title: '清空购物车',
          message: '清空后需要重新挑选商品，确定继续吗？'
        })
      } catch (error) {
        return
      }
      try {
        await cleanCart()
        this.items = []
        Toast.success('购物车已清空')
      } catch (error) {
        Toast.fail(error.message)
      }
    },
    checkout() {
      this.$router.push('/checkout')
    },
    goHome() {
      this.$router.push('/home')
    }
  }
}
</script>

<style scoped>
.cart-page {
  min-height: 100vh;
  padding-bottom: 132px;
  background: #f6f7f8;
}

.state {
  display: grid;
  place-items: center;
  padding: 60px 0;
}

.list {
  padding: 12px 14px 0;
}

.cart-item {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 12px;
  padding: 12px;
  margin-bottom: 10px;
  background: #fff;
  border: 1px solid #eceff3;
  border-radius: 14px;
}

.cover {
  width: 72px;
  height: 72px;
  object-fit: cover;
  background: #f2f4f7;
  border-radius: 10px;
}

.body h3 {
  margin: 0;
  font-size: 15px;
}

.meta {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 18px;
  margin: 6px 0 0;
}

.flavor {
  color: #8a94a3;
  font-size: 12px;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.price {
  color: #f2622e;
  font-size: 16px;
  font-weight: 700;
}

.stepper {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #f2622e;
  font-size: 18px;
}

.stepper span {
  min-width: 18px;
  color: #1f2937;
  font-size: 14px;
  font-weight: 600;
  text-align: center;
}

.footer {
  position: fixed;
  bottom: 50px;
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
  display: block;
  margin-top: 4px;
  color: #f2622e;
  font-size: 19px;
}

.ops {
  display: flex;
  gap: 8px;
}
</style>
