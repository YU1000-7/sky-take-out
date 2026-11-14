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
        <dish-list :category-id="activeCategoryId" @add-cart="handleAddCart" />
      </div>
    </section>

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

export default {
  name: 'CustomerHome',
  components: {
    CategoryNav,
    DishList
  },
  data() {
    return {
      activeCategoryId: null
    }
  },
  methods: {
    handleCategoryChange(categoryId) {
      this.activeCategoryId = categoryId
    },
    handleAddCart() {
      this.$toast('购物车模块接入后即可加购')
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
</style>
