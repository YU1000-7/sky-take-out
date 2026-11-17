import Vue from 'vue'
import VueRouter from 'vue-router'
import store from '../store'

Vue.use(VueRouter)

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/index.vue')
  },
  {
    path: '/',
    component: () => import('../views/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/dashboard/index.vue'),
        meta: { title: '仪表盘' }
      },
      {
        path: 'order',
        name: 'Order',
        component: () => import('../views/order/index.vue'),
        meta: { title: '订单管理' }
      },
      {
        path: 'report',
        name: 'Report',
        component: () => import('../views/report/index.vue'),
        meta: { title: '营业报表' }
      },
      {
        path: 'employee',
        name: 'Employee',
        component: () => import('../views/employee/index.vue'),
        meta: { title: '员工管理' }
      },
      {
        path: 'category',
        name: 'Category',
        component: () => import('../views/category/index.vue'),
        meta: { title: '分类管理' }
      },
      {
        path: 'dish',
        name: 'Dish',
        component: () => import('../views/dish/index.vue'),
        meta: { title: '菜品管理' }
      },
      {
        path: 'setmeal',
        name: 'Setmeal',
        component: () => import('../views/setmeal/index.vue'),
        meta: { title: '套餐管理' }
      }
    ]
  },
  {
    path: '*',
    redirect: '/'
  }
]

const router = new VueRouter({
  mode: 'hash',
  routes
})

router.beforeEach((to, from, next) => {
  const token = store.state.token
  if (to.path !== '/login' && !token) {
    next('/login')
    return
  }
  if (to.path === '/login' && token) {
    next('/dashboard')
    return
  }
  document.title = to.meta.title
    ? `${to.meta.title} - 苍穹外卖`
    : '苍穹外卖管理后台'
  next()
})

export default router
