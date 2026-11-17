import Vue from 'vue'
import VueRouter from 'vue-router'

Vue.use(VueRouter)

const routes = [
  { path: '/', redirect: '/home' },
  {
    path: '/home',
    component: () => import('../views/Home.vue'),
    meta: { title: '点餐' }
  },
  {
    path: '/cart',
    component: () => import('../views/Cart.vue'),
    meta: { title: '购物车', owner: 'C' }
  },
  {
    path: '/checkout',
    component: () => import('../views/Checkout.vue'),
    meta: { title: '确认订单' }
  },
  {
    path: '/orders',
    component: () => import('../views/Orders.vue'),
    meta: { title: '我的订单' }
  },
  {
    path: '/orders/:id',
    component: () => import('../views/OrderDetail.vue'),
    meta: { title: '订单详情' }
  },
  {
    path: '/address',
    component: () => import('../views/AddressBook.vue'),
    meta: { title: '地址簿' }
  },
  {
    path: '/address/edit/:id?',
    component: () => import('../views/AddressEdit.vue'),
    meta: { title: '编辑地址' }
  },
  {
    path: '/login',
    component: () => import('../views/Login.vue'),
    meta: { title: '客户登录', public: true }
  }
]

const router = new VueRouter({
  mode: 'hash',
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('sky_customer_token')
  if (!to.meta.public && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  if (to.path === '/login' && token) {
    next('/home')
    return
  }
  next()
})

router.afterEach(to => {
  document.title = to.meta.title
    ? `${to.meta.title} - 苍穹外卖`
    : '苍穹外卖'
})

export default router
