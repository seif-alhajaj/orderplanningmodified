<template>
  <div class="dashboard">
    <!-- Main Header -->
    <div class="hero">
      <h1>Pokemon Planning</h1>
      <p>Pokemon Card Order Management System</p>
      <div class="timestamp">Latest update: {{ currentTime }}</div>
    </div>

    <!-- System Status -->
    <div class="status-grid">
      <div class="status-card" :class="{ 'connected': backendConnected }">
        <h3>Backend</h3>
        <p class="status">{{ backendConnected ? 'Connected' : 'Disconnected' }}</p>
        <button @click="checkBackend" class="btn-refresh" :disabled="checkingBackend">
          {{ checkingBackend ? 'Testing...' : 'Test it' }}
        </button>
      </div>

      <div class="status-card">
        <h3>Statistics</h3>
        <div class="stats" v-if="!loadingStats">
          <p>{{ stats.ordersCount || 0 }} Order</p>
          <p>{{ stats.employeesCount || 0 }} Employees</p>
          <p>{{ stats.planningCount || 0 }} Plannings</p>
        </div>
        <div v-else class="loading">Loading...</div>
      </div>

      <div class="status-card">
        <h3>Quick Actions</h3>
        <div class="actions">
          <button @click="generatePlanning" class="btn-primary" :disabled="generatingPlanning">
            {{ generatingPlanning ? 'Generating...' : 'Generate Planning' }}
          </button>
          <button @click="refreshStats" class="btn-secondary" :disabled="loadingStats">
            {{ loadingStats ? 'Refreshing...' : 'Refresh' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Data Overview -->
    <div class="data-preview">
      <h2>Data Overview</h2>

      <!-- Employees -->
      <div class="preview-section">
        <h3>Employees ({{ employees.length }})</h3>
        <div class="preview-list">
          <div v-for="employee in employees.slice(0, 3)" :key="employee.id" class="preview-item">
            <span class="item-name">{{ employee.fullName }}</span>
            <span class="item-detail">{{ employee.workHoursPerDay }}h/day</span>
            <span :class="['item-status', employee.active ? 'active' : 'inactive']">
              {{ employee.active ? 'Active' : 'Inactive' }}
            </span>
          </div>
          <div v-if="employees.length > 3" class="preview-more">
            +{{ employees.length - 3 }} other employees
          </div>
        </div>
      </div>

      <!-- Recent Orders -->
      <div class="preview-section">
        <h3>Recent Orders ({{ orders.length }})</h3>
        <div class="preview-list">
          <div v-for="order in orders.slice(0, 5)" :key="order.id" class="preview-item">
            <span class="item-name">{{ order.orderNumber }}</span>
            <span class="item-detail">{{ order.cardCount }} cards</span>
            <span :class="['item-priority', order.priority.toLowerCase()]">
              {{ order.priority }}
            </span>
          </div>
          <div v-if="orders.length > 5" class="preview-more">
            +{{ orders.length - 5 }} other orders
          </div>
        </div>
      </div>

      <!-- Priority Statistics -->
      <div class="preview-section" v-if="stats.ordersByPriority">
        <h3>Priority Orders</h3>
        <div class="priority-stats">
          <div v-for="(count, priority) in stats.ordersByPriority" :key="priority"
               :class="['priority-item', priority.toLowerCase()]">
            <span class="priority-label">{{ priority }}</span>
            <span class="priority-count">{{ count }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Navigation -->
    <div class="navigation-grid">
      <h2>Navigation</h2>
      <div class="nav-cards">
        <router-link to="/orders" class="nav-card">
          <div class="card-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
              <path d="M7 18c-1.1 0-1.99.9-1.99 2S5.9 22 7 22s2-.9 2-2-.9-2-2-2zM1 2v2h2l3.6 7.59-1.35 2.45c-.16.28-.25.61-.25.96 0 1.1.9 2 2 2h12v-2H7.42c-.14 0-.25-.11-.25-.25l.03-.12.9-1.63h7.45c.75 0 1.41-.41 1.75-1.03l3.58-6.49c.08-.14.12-.31.12-.48 0-.55-.45-1-1-1H5.21l-.94-2H1zm16 16c-1.1 0-1.99.9-1.99 2s.89 2 1.99 2 2-.9 2-2-.9-2-2-2z"/>
            </svg>
          </div>
          <h3>Orders</h3>
          <p>Manage Pokemon Orders</p>
          <div class="card-count">{{ orders.length }} Orders</div>
        </router-link>

        <router-link to="/employees" class="nav-card">
          <div class="card-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
            </svg>
          </div>
          <h3>Employees</h3>
          <p>Manage the team</p>
          <div class="card-count">{{ employees.length }} employees</div>
        </router-link>

        <router-link to="/planning" class="nav-card">
          <div class="card-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19 3h-1V1h-2v2H8V1H6v2H5c-1.11 0-1.99.9-1.99 2L3 19c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V8h14v11zM7 10h5v5H7z"/>
            </svg>
          </div>
          <h3>Planning</h3>
          <p>View and modify schedules</p>
          <div class="card-count">{{ stats.planningCount || 0 }} planning</div>
        </router-link>
      </div>
    </div>

    <!-- System Messages -->
    <div v-if="systemMessage" :class="['system-message', systemMessage.type]">
      <h4>{{ systemMessage.title }}</h4>
      <p>{{ systemMessage.message }}</p>
      <button @click="systemMessage = null" class="close-btn">×</button>
    </div>
  </div>
</template>

<script>
import ApiService from '../services/api.ts'

export default {
  name: 'Dashboard',

  data() {
    return {
      // System State
      backendConnected: false,
      checkingBackend: false,
      loadingStats: false,
      generatingPlanning: false,

      // Data
      currentTime: new Date().toLocaleString('fr-FR'),
      stats: {},
      employees: [],
      orders: [],
      planning: [],

      // Messages
      systemMessage: null
    }
  },

  async mounted() {
    console.log('Dashboard mounted - Loading data...')

    // Automatic startup
    await this.checkBackend()
    await this.loadAllData()

    // Update time every minute
    this.timeInterval = setInterval(() => {
      this.currentTime = new Date().toLocaleString('fr-FR')
    }, 60000)
  },

  beforeUnmount() {
    if (this.timeInterval) {
      clearInterval(this.timeInterval)
    }
  },

  methods: {
    /**
     * Check backend connection
     */
    async checkBackend() {
      this.checkingBackend = true
      try {
        this.backendConnected = await ApiService.healthCheck()

        if (this.backendConnected) {
          this.showMessage('success', 'Connection OK', 'Backend available')
        } else {
          this.showMessage('error', 'Connection failed', 'Check that the Spring Boot server is running on port 8080')
        }
      } catch (error) {
        this.backendConnected = false
        this.showMessage('error', 'Connection error', error.message)
      } finally {
        this.checkingBackend = false
      }
    },

    /**
     * Load all data
     */
    async loadAllData() {
      this.loadingStats = true
      try {
        console.log('Loading full data...')

        // Load in parallel
        const [employees, orders, stats] = await Promise.all([
          ApiService.getEmployees(),
          ApiService.getOrdersSinceJune2025(), // Orders since June 2025
          ApiService.getStats()
        ])

        this.employees = employees
        this.orders = orders
        this.stats = stats

        console.log('Data loaded:', {
          employees: employees.length,
          orders: orders.length,
          stats
        })

        this.showMessage('success', 'Data loaded',
          `${employees.length} employees, ${orders.length} orders`)

      } catch (error) {
        console.error('Data loading error:', error)
        this.showMessage('error', 'Loading error', error.message)
      } finally {
        this.loadingStats = false
      }
    },

    /**
     * Refresh statistics
     */
    async refreshStats() {
      await this.loadAllData()
    },

    /**
     * Generate planning
     */
    async generatePlanning() {
      this.generatingPlanning = true
      try {
        console.log('Generating planning...')

        const result = await ApiService.generatePlanning({
          startDate: '2025-06-01',
          timePerCard: 3,
          cleanFirst: false
        })

        console.log('Planning generated:', result)

        if (result.success) {
          this.showMessage('success', 'Planning generated',
            result.message || 'Planning created successfully')

          // Reload data
          await this.loadAllData()
        } else {
          this.showMessage('error', 'Planning error',
            result.message || 'Unable to generate planning')
        }

      } catch (error) {
        console.error('Generation error:', error)
        this.showMessage('error', 'Generation error', error.message)
      } finally {
        this.generatingPlanning = false
      }
    },

    /**
     * Display a system message
     */
    showMessage(type, title, message) {
      this.systemMessage = { type, title, message }

      // Auto-close after 5 seconds for successes
      if (type === 'success') {
        setTimeout(() => {
          this.systemMessage = null
        }, 5000)
      }
    }
  }
}
</script>

<style scoped>
.dashboard {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background-color: #f8fafc;
  min-height: 100vh;
}

.hero {
  text-align: center;
  margin-bottom: 40px;
  padding: 40px 20px;
  background: linear-gradient(135deg, #2563eb 0%, #1e40af 100%);
  color: white;
  border-radius: 16px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
}

.hero h1 {
  font-size: 2.5rem;
  margin: 0 0 10px 0;
  font-weight: 700;
}

.hero p {
  font-size: 1.125rem;
  margin-bottom: 15px;
  opacity: 0.9;
}

.timestamp {
  opacity: 0.8;
  font-size: 0.9rem;
  background: rgba(255, 255, 255, 0.15);
  display: inline-block;
  padding: 6px 12px;
  border-radius: 20px;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 24px;
  margin-bottom: 40px;
}

.status-card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  border-left: 4px solid #e2e8f0;
  transition: transform 0.2s, box-shadow 0.2s;
}

.status-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
}

.status-card.connected {
  border-left-color: #10b981;
}

.status-card h3 {
  margin: 0 0 15px 0;
  color: #1e293b;
  font-weight: 600;
  font-size: 1.25rem;
}

.status {
  font-weight: 600;
  margin-bottom: 15px;
  color: #64748b;
}

.stats p, .actions {
  margin: 8px 0;
}

.btn-refresh, .btn-primary, .btn-secondary {
  padding: 10px 18px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9rem;
  margin-right: 10px;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-refresh {
  background: #64748b;
  color: white;
}

.btn-refresh:hover:not(:disabled) {
  background: #475569;
}

.btn-primary {
  background: #3b82f6;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #2563eb;
}

.btn-secondary {
  background: #10b981;
  color: white;
}

.btn-secondary:hover:not(:disabled) {
  background: #059669;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.data-preview {
  margin-bottom: 40px;
}

.data-preview h2 {
  color: #1e293b;
  font-weight: 600;
  margin-bottom: 24px;
  font-size: 1.5rem;
}

.preview-section {
  background: white;
  padding: 24px;
  margin-bottom: 24px;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}

.preview-section h3 {
  color: #1e293b;
  font-weight: 600;
  margin-bottom: 16px;
  font-size: 1.125rem;
}

.preview-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.preview-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.preview-item:hover {
  background: #f1f5f9;
}

.item-name {
  font-weight: 500;
  color: #1e293b;
}

.item-detail {
  color: #64748b;
}

.item-status.active {
  color: #10b981;
  font-weight: 500;
}

.item-status.inactive {
  color: #ef4444;
  font-weight: 500;
}

.item-priority.Excelsior {
  color: #dc2626;
  font-weight: 600;
}

.item-priority.Fast+ {
  color: #ea580c;
  font-weight: 600;
}

.item-priority.Fast {
  color: #d97706;
  font-weight: 600;
}

.item-priority.Classic {
  color: #059669;
  font-weight: 600;
}

.priority-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}

.priority-item {
  display: flex;
  justify-content: space-between;
  padding: 12px 16px;
  border-radius: 8px;
  font-weight: 600;
}

.priority-item.Excelsior {
  background: #fef2f2;
  color: #dc2626;
}

.priority-item.Fast+ {
  background: #fffbeb;
  color: #d97706;
}

.priority-item.Fast {
  background: #fefce8;
  color: #ca8a04;
}

.priority-item.Classic {
  background: #f0fdf4;
  color: #16a34a;
}

.navigation-grid {
  margin-bottom: 40px;
}

.navigation-grid h2 {
  color: #1e293b;
  font-weight: 600;
  margin-bottom: 24px;
  font-size: 1.5rem;
}

.nav-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
}

.nav-card {
  background: white;
  padding: 32px 24px;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  text-decoration: none;
  color: inherit;
  transition: transform 0.2s, box-shadow 0.2s;
  text-align: center;
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.nav-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #3b82f6, #2563eb);
}

.nav-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

.card-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border-radius: 16px;
  color: white;
  padding: 14px;
}

.card-icon svg {
  width: 32px;
  height: 32px;
}

.nav-card h3 {
  color: #1e293b;
  font-weight: 600;
  margin: 0 0 8px 0;
  font-size: 1.25rem;
}

.nav-card p {
  color: #64748b;
  margin: 0 0 16px 0;
}

.card-count {
  color: #3b82f6;
  font-size: 0.9rem;
  margin-top: 10px;
  font-weight: 600;
  background: #eff6ff;
  padding: 6px 12px;
  border-radius: 20px;
}

.system-message {
  position: fixed;
  bottom: 24px;
  right: 24px;
  max-width: 400px;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  z-index: 1000;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.system-message.success {
  background: #ecfdf5;
  color: #065f46;
  border: 1px solid #a7f3d0;
}

.system-message.error {
  background: #fef2f2;
  color: #991b1b;
  border: 1px solid #fecaca;
}

.system-message h4 {
  margin: 0;
  font-weight: 600;
  font-size: 1.125rem;
}

.system-message p {
  margin: 0;
  line-height: 1.5;
}

.close-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: inherit;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading {
  color: #64748b;
  font-style: italic;
}

.preview-more {
  color: #64748b;
  font-style: italic;
  text-align: center;
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
}
</style>