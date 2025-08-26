import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../views/Dashboard.vue'
import Orders from '../views/Orders.vue'
import Employees from '../views/Employees.vue'
import Planning from '../views/Planning.vue'


const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: Dashboard
    },
    {
      path: '/orders',
      name: 'orders',
      component: Orders
    },
    {
      path: '/employees',
      name: 'employees',
      component: Employees
    },
    {
      path: '/planning',
      name: 'planning',
      component: Planning
    }
  ]
})

export default router
