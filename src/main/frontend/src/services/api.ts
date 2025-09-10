// src/services/api.js - VERSION THAT WORKS WITH YOUR ENDPOINTS
const API_BASE_URL = 'http://localhost:8080'

class ApiService {

  /**
   * Generic query method
   */
  async request(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`
    const config = {
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        ...options.headers,
      },
      ...options,
    }

    try {
      console.log(`API Request: ${options.method || 'GET'} ${url}`)
      const response = await fetch(url, config)

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }

      const data = await response.json()
      console.log(`API Response: ${endpoint} - ${Array.isArray(data) ? data.length : 'object'} items`)
      return data

    } catch (error) {
      console.error(`API Error for ${endpoint}:`, error)
      throw error
    }
  }

  /**
   * EMPLOYEES - Use the endpoint that works
   */
  async getEmployees() {
    try {
      const employees = await this.request('/api/employees')
      console.log(`${employees.length} employees retrieved`)
      // Map to the format expected by the frontend
      return employees.map(emp => ({
        id: emp.id,
        firstName: emp.first_name || emp.firstName,
        lastName: emp.last_name || emp.lastName,
        fullName: emp.fullName || `${emp.first_name || emp.firstName} ${emp.last_name || emp.lastName}`,
        email: emp.email,
        role: emp.role || 'GRADER', // Added role with default
        workHoursPerDay: emp.work_hours_per_day || emp.workHoursPerDay || 8,
        active: emp.active !== undefined ? emp.active : true,
        available: emp.available,
        currentLoad: emp.currentLoad || 0,
        name: emp.name || emp.fullName || `${emp.first_name || emp.firstName} ${emp.last_name || emp.lastName}`
      }))
    } catch (error) {
      console.error('Employee retrieval error:', error)
      return []
    }
  }

  /**
   * ORDERS - Use the endpoint that works
   */
  async getOrders() {
    try {
      const orders = await this.request('/api/orders')
      console.log(`${orders.length} orders retrieved`)

      // Map to the format expected by the frontend
      return orders.map(order => ({
        id: order.id,
        orderNumber: order.orderNumber,
        reference: order.reference,
        cardCount: order.cardCount,
        totalPrice: order.totalPrice,
        priority: order.priority,
        status: order.status,
        statusText: order.statusText,
        estimatedTimeMinutes: order.estimatedTimeMinutes,
        estimatedTimeHours: order.estimatedTimeHours,
        creationDate: order.creationDate,
        orderDate: order.orderDate,
        deadline: order.deadline,
        qualityIndicator: order.qualityIndicator,
        minimumGrade: order.minimumGrade,
        type: order.type,
        unsealing: order.unsealing
      }))
    } catch (error) {
      console.error('Error retrieving orders:', error)
      return []
    }
  }

  /**
   * PLANNINGS - Test several possible endpoints
   */
  async getPlanning() {
    const endpoints = [
      '/api/planning',
      '/api/planning/view-simple'
    ]

    for (const endpoint of endpoints) {
      try {
        const data = await this.request(endpoint)
        console.log(`Planning retrieved via: ${endpoint}`)
        return Array.isArray(data) ? data : []
      } catch (error) {
        console.log(`Failed endpoint ${endpoint}`)
        continue
      }
    }

    console.warn('No planning endpoints available')
    return []
  }

  /**
   * Generation of planning
   */
  async generatePlanning(config = {}) {
    try {
      const body = {
        startDate: config.startDate || new Date().toISOString().split('T')[0],
        timePerCard: config.timePerCard || 3,
        cleanFirst: config.cleanFirst || false,
        ...config
      }

      console.log('Generating planning:', body)

      const result = await this.request('/api/planning/generate', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(body)
      })

      console.log('Planning generated:', result)
      return result

    } catch (error) {
      console.error('Planning generation error:', error)
      throw error
    }
  }

  /**
   * STATISTICS - Calculated from existing data
   */
  async getStats() {
    try {
      const [employees, orders, planning] = await Promise.all([
        this.getEmployees(),
        this.getOrders(),
        this.getPlanning()
      ])

      // Calculate useful statistics
      const stats = {
        employeesCount: employees.length,
        activeEmployees: employees.filter(e => e.active).length,
        ordersCount: orders.length,
        planningCount: planning.length,
        // Order statistics by priority
        ordersByPriority: {
          EXCELSIOR: orders.filter(o => o.priority === 'EXCELSIOR').length,
          FAST_PLUS: orders.filter(o => o.priority === 'FAST+').length,
          FAST: orders.filter(o => o.priority === 'FAST').length,
          CLASSIC: orders.filter(o => o.priority === 'CLASSIC').length
        },

        // Order statistics by status
        ordersByStatus: {
          PENDING: orders.filter(o => o.status === 1).length,
          IN_PROGRESS: orders.filter(o => o.status === 2).length,
          COMPLETED: orders.filter(o => o.status === 3).length
        },

        // Total estimated time
        totalEstimatedMinutes: orders.reduce((sum, o) => sum + (o.estimatedTimeMinutes || 0), 0),
        totalCards: orders.reduce((sum, o) => sum + (o.cardCount || 0), 0),
        totalPrice: orders.reduce((sum, o) => sum + (o.totalPrice || 0), 0),

        // Date of Last update
        lastUpdate: new Date().toISOString()
      }

      console.log('Calculated statistics:', stats)
      return stats

    } catch (error) {
      console.error('Error calculating statistics:', error)
      return {
        employeesCount: 0,
        ordersCount: 0,
        planningCount: 0,
        error: true,
        message: error.message
      }
    }
  }

  /**
   * BACKEND HEALTH CHECK
   */
  async healthCheck() {
    try {
      // Direct test on a working endpoint
      const response = await fetch(`${API_BASE_URL}/api/employees`)
      const isHealthy = response.ok

      if (isHealthy) {
        console.log('Backend Health: OK')
      } else {
        console.log(`Backend Health: HTTP ${response.status}`)
      }

      return {
        healthy: isHealthy,
        status: response.status,
        statusText: response.statusText
      }

    } catch (error) {
      console.error('Backend Health Check Failed:', error)
      return {
        healthy: false,
        error: error.message
      }
    }
  }

  /**
   * SPECIFIC METHODS FOR POKEMON
   */

  // Get orders since June 2025
  async getOrdersSinceJune2025() {
    try {
      const allOrders = await this.getOrders()

      // Filter orders since June 2025
      const june2025 = new Date('2025-06-01')
      const filteredOrders = allOrders.filter(order => {
        const orderDate = new Date(order.creationDate || order.orderDate)
        return orderDate >= june2025
      })

      console.log(`${filteredOrders.length} orders since June 2025`)
      return filteredOrders

    } catch (error) {
      console.error('Error getting orders since June 2025:', error)
      return []
    }
  }

  // Get active employees with their load
  async getActiveEmployeesWithLoad() {
    try {
      const employees = await this.getEmployees()
      const activeEmployees = employees.filter(emp => emp.active && emp.available)

      console.log(`${activeEmployees.length} active employees`)
      return activeEmployees

    } catch (error) {
      console.error('Error getting active employees:', error)
      return []
    }
  }
}

// Export singleton
const apiService = new ApiService()
export default apiService

// For debugging in the console
if (typeof window !== 'undefined') {
  window.apiService = apiService
}