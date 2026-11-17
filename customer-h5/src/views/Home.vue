<template>
  <div class="home">
    <van-nav-bar title="苍穹外卖" />

    <section class="hero">
      <p class="eyebrow">TODAY'S MENU</p>
      <h1>好好吃饭，<br>认真生活。</h1>
      <p>选择喜欢的分类，看看今天有什么好吃的。</p>
    </section>

    <section class="menu-section">
      <div class="menu-frame">
        <category-nav @change="handleCategoryChange" />
        <dish-list
          v-if="activeType === 1"
          :category-id="activeCategoryId"
          @add-cart="handleAddCart"
        />
        <setmeal-list
          v-else
          :category-id="activeCategoryId"
          @add-cart="handleAddCart"
        />
      </div>
    </section>

    <transition name="slide-up">
      <div v-if="cart.count" class="cart-bar">
        <div class="cart-summary" @click="goCart">
          <span class="cart-icon">
            <van-icon name="shopping-cart" />
            <em>{{ cart.count }}</em>
          </span>
          <span class="cart-total">￥{{ cart.total }}</span>
          <span class="cart-tip">已选商品</span>
        </div>
        <van-button type="danger" size="small" round @click="goCart">去结算</van-button>
      </div>
    </transition>

    <van-tabbar route>
      <van-tabbar-item to="/home" icon="shop-o">点餐</van-tabbar-item>
      <van-tabbar-item to="/cart" icon="shopping-cart-o">购物车</van-tabbar-item>
      <van-tabbar-item to="/orders" icon="orders-o">订单</van-tabbar-item>
      <van-tabbar-item to="/address" icon="location-o">地址</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script>
import CategoryNav from '../components/CategoryNav'
import DishList from '../components/DishList'
import SetmealList from '../components/SetmealList'
import { addCart, getCartList } from '../api/cart'

export default {
  name: 'CustomerHome',
  components: {
    CategoryNav,
    DishList,
    SetmealList
  },
  data() {
    return {
      activeCategoryId: null,
      activeType: 1,
      cart: {
        count: 0,
        total: '0.00'
      }
    }
  },
  created() {
    this.loadCart()
  },
  methods: {
    handleCategoryChange(categoryId, category) {
      this.activeCategoryId = categoryId
      this.activeType = (category && category.type) || 1
    },
    /**
     * 菜品与套餐共用同一个加购入口：payload 带 setmealId 即为套餐。
     */
    async handleAddCart(payload) {
      if (!payload) {
        return
      }
      const data = payload.setmealId
        ? { setmealId: payload.setmealId }
        : { dishId: payload.dishId, dishFlavor: payload.dishFlavor || null }

      if (!data.setmealId && !data.dishId) {
        return
      }

      try {
        await addCart(data)
        this.$toast('已加入购物车')
        this.loadCart()
      } catch (e) {
        this.$toast(e.message || '加购失败')
      }
    },
    async loadCart() {
      try {
        const carts = (await getCartList()) || []
        this.cart = {
          count: carts.reduce((sum, item) => sum + (item.number || 0), 0),
          total: (carts.reduce((sum, item) => sum + (item.number || 0) * (item.amount || 0), 0) / 100).toFixed(2)
        }
      } catch (e) {
        // 未登录或接口未就绪时不打扰用户，悬浮条保持隐藏。
        this.cart = { count: 0, total: '0.00' }
      }
    },
    goCart() {
      this.$router.push('/cart')
    }
  }
}
</script>

<style scoped>
.home {
  min-height: 100vh;
  padding-bottom: 70px;
}

.hero {
  padding: 56px 26px 42px;
  color: #fff;
  background:
    radial-gradient(circle at 85% 10%, rgba(242, 138, 46, 0.4), transparent 36%),
    #202938;
}

.eyebrow {
  margin: 0 0 18px;
  color: #c6ced9;
  font-size: 10px;
  letter-spacing: 0.2em;
}

.hero h1 {
  margin: 0;
  font-size: 34px;
  line-height: 1.35;
}

.hero > p:last-child {
  max-width: 310px;
  margin: 18px 0 0;
  color: #b7c0cc;
  font-size: 13px;
  line-height: 1.7;
}

.menu-section {
  padding: 16px 0 64px;
  background: #fff;
}

.menu-frame {
  display: flex;
  min-height: 520px;
  height: calc(100vh - 315px);
  max-width: 720px;
  margin: 0 auto;
  overflow: hidden;
  border-top: 1px solid #f2f3f5;
}

.cart-bar {
  position: fixed;
  right: 12px;
  bottom: 62px;
  left: 12px;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 696px;
  margin: 0 auto;
  padding: 9px 12px 9px 16px;
  color: #fff;
  background: #202938;
  border-radius: 28px;
  box-shadow: 0 8px 22px rgba(32, 41, 56, 0.28);
}

.cart-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.cart-icon {
  position: relative;
  display: inline-flex;
}

.cart-icon .van-icon {
  font-size: 24px;
}

.cart-icon em {
  position: absolute;
  top: -6px;
  right: -10px;
  min-width: 16px;
  padding: 0 4px;
  color: #fff;
  background: #ee0a24;
  border-radius: 9px;
  font-size: 10px;
  font-style: normal;
  line-height: 16px;
  text-align: center;
}

.cart-total {
  font-size: 17px;
  font-weight: 600;
}

.cart-tip {
  color: #9aa4b2;
  font-size: 11px;
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform 0.24s ease, opacity 0.24s ease;
}

.slide-up-enter,
.slide-up-leave-to {
  opacity: 0;
  transform: translateY(16px);
}
</style>
