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
    component: () => import('../views/Placeholder.vue'),
    meta: { title: '购物车', owner: 'C' }
  },
  {
    path: '/orders',
    component: () => import('../views/Placeholder.vue'),
    meta: { title: '我的订单', owner: 'D' }
  },
  {
    path: '/address',
    component: () => import('../views/AddressBook.vue'),
    meta: { title: '地址簿' }
  },
  {
    path: '/login',
    component: () => import('../views/Login.vue'),
    meta: { title: '客户登录', public: true }
  },
  {
    path: '/address/edit/:id?',
    component: () => import('../views/AddressEdit.vue'),
    meta: { title: '编辑地址' }
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
