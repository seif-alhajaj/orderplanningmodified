<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <div class="max-w-7xl mx-auto">

      <!--  HEADER -->
      <div class="bg-white rounded-lg shadow p-6 mb-6">
        <div class="flex justify-between items-center mb-4">
          <div>
            <h1 class="text-3xl font-bold text-gray-900"> Orders Management</h1>
            <p class="text-gray-600">Real orders from Pokemon card database</p>
          </div>

          <div class="flex gap-3">
            <button
              @click="refreshOrders"
              :disabled="loading"
              class="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50 flex items-center gap-2"
            >
              <svg v-if="loading" class="animate-spin h-4 w-4" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <span v-else></span>
              {{ loading ? 'Loading...' : 'Refresh' }}
            </button>

            <button
              @click="debugWorkingEndpoint"
              class="bg-orange-600 text-white px-4 py-2 rounded-lg hover:bg-orange-700"
            >
               Debug API
            </button>
          </div>
        </div>
      </div>

      <!--  DATE FILTER CONTROLS -->
      <div class="bg-white rounded-lg shadow p-6 mb-6">
        <div class="flex items-center justify-between mb-4">
          <div>
            <h3 class="text-lg font-semibold text-gray-900"> Date Filtering</h3>
            <p class="text-sm text-gray-600">Filter orders by creation date</p>
          </div>

          <div class="flex items-center gap-4">
            <label class="flex items-center gap-2">
              <input
                type="checkbox"
                v-model="enableDateFilter"
                class="rounded border-gray-300"
              />
              <span class="text-sm font-medium">Enable date filter</span>
            </label>

            <div v-if="enableDateFilter" class="flex items-center gap-2">
              <label class="text-sm font-medium">Show orders since:</label>
              <input
                type="date"
                v-model="filterFromDate"
                class="border border-gray-300 rounded-md px-3 py-2 text-sm"
              />
            </div>
          </div>
        </div>

        <div class="bg-gray-50 p-3 rounded-lg">
          <div class="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
            <div>
              <span class="font-medium">Total in DB:</span>
              <span class="ml-2">{{ statistics.totalInDb }} orders</span>
            </div>
            <div>
              <span class="font-medium">Currently shown:</span>
              <span class="ml-2">{{ statistics.total }} orders</span>
            </div>
            <div>
              <span class="font-medium">Filter active:</span>
              <span class="ml-2">{{ enableDateFilter ? `Since ${filterFromDate}` : 'No filter' }}</span>
            </div>
            <div>
              <span class="font-medium">Cards:</span>
              <span class="ml-2">{{ statistics.totalCards }} total</span>
            </div>
          </div>
        </div>
      </div>

      <!-- QUICK STATISTICS -->
      <div class="grid grid-cols-2 md:grid-cols-4 gap-6 mb-6">
        <div class="bg-white p-6 rounded-lg shadow border-l-4 border-blue-500">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-gray-600">Displayed Orders</p>
              <p class="text-2xl font-bold text-blue-600">{{ statistics.total }}</p>
            </div>
            <div class="text-3xl text-blue-600"></div>
          </div>
        </div>

        <div class="bg-white p-6 rounded-lg shadow border-l-4 border-yellow-500">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-gray-600">Pending</p>
              <p class="text-2xl font-bold text-yellow-600">{{ statistics.pending }}</p>
            </div>
            <div class="text-3xl text-yellow-600"></div>
          </div>
        </div>

        <div class="bg-white p-6 rounded-lg shadow border-l-4 border-green-500">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-gray-600">Completed</p>
              <p class="text-2xl font-bold text-green-600">{{ statistics.completed }}</p>
            </div>
            <div class="text-3xl text-green-600"></div>
          </div>
        </div>

        <div class="bg-white p-6 rounded-lg shadow border-l-4 border-purple-500">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-gray-600">Total Cards</p>
              <p class="text-2xl font-bold text-purple-600">{{ statistics.totalCards }}</p>
            </div>
            <div class="text-3xl text-purple-600"></div>
          </div>
        </div>
      </div>

      <!--  SEARCH & FILTERS -->
      <div class="bg-white rounded-lg shadow p-6 mb-6">
        <h3 class="text-lg font-semibold text-gray-900 mb-4"> Search & Filters</h3>
        <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Search</label>
            <input
              v-model="searchTerm"
              type="text"
              placeholder="Order number..."
              class="w-full border border-gray-300 rounded-md px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Status</label>
            <select v-model="filterStatus" class="w-full border border-gray-300 rounded-md px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:border-blue-500">
              <option value="all">All Statuses</option>
              <option value="PENDING">Pending</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="COMPLETED">Completed</option>
            </select>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Priority</label>
            <select v-model="filterPriority" class="w-full border border-gray-300 rounded-md px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:border-blue-500">
              <option value="all">All Priorities</option>
              <option value="EXCELSIOR">Excelsior</option>
              <option value="FAST+">Fast+</option>
              <option value="FAST">Fast</option>
              <option value="CLASSIC">Classic</option>
            </select>
          </div>

          <div class="flex items-end">
            <button
              @click="loadOrders"
              class="w-full bg-gray-600 text-white px-4 py-2 rounded-md hover:bg-gray-700 transition-colors"
            >
              Reload Data
            </button>
          </div>
        </div>
      </div>

      <!-- LOADING STATE -->
      <div v-if="loading" class="text-center py-12">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
        <span class="text-gray-600 mt-3 block">Loading real orders from database...</span>
      </div>

      <!-- ORDERS TABLE -->
      <div v-else-if="filteredOrders.length > 0" class="bg-white rounded-lg shadow overflow-hidden">
        <div class="px-6 py-4 border-b border-gray-200">
          <h2 class="text-lg font-semibold text-gray-900">
            {{ filteredOrders.length }} Order{{ filteredOrders.length > 1 ? 's' : '' }}
          </h2>
        </div>

        <div class="overflow-x-auto">
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Order</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Date</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Cards</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Quality</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Priority</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Duration</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
            </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
            <tr v-for="order in filteredOrders" :key="order.id" class="hover:bg-gray-50 transition-colors">
              <!-- Order -->
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="flex items-center">
                  <div>
                    <div class="text-sm font-medium text-gray-900">{{ order.orderNumber }}</div>
                    <div class="text-sm text-gray-500">ID: {{ order.id.slice(-8) }}</div>
                  </div>
                </div>
              </td>

              <!-- Date -->
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="text-sm text-gray-900">{{ formatDate(order.createdDate) }}</div>
              </td>

              <!-- Cards -->
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="text-sm text-gray-900">{{ order.cardCount }} cards</div>
                <div class="text-sm text-gray-500">
                  {{ order.cardsWithName || 0 }} with names ({{ order.namePercentage || 0 }}%)
                  <span :class="(order.namePercentage || 0) >= 95 ? 'text-green-600' : 'text-orange-600'">
                      {{ (order.namePercentage || 0) >= 95 ? '' : '' }}
                    </span>
                </div>
              </td>

              <!-- Quality -->
              <td class="px-6 py-4 whitespace-nowrap">
                <span class="text-2xl">{{ getQualityIndicator(order.namePercentage || 0) }}</span>
              </td>

              <!-- Priority -->
              <td class="px-6 py-4 whitespace-nowrap">
                  <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium"
                        :class="getPriorityColor(order.priority)">
                    {{ getPriorityLabel(order.priority) }}
                  </span>
              </td>

              <!-- Status -->
              <td class="px-6 py-4 whitespace-nowrap">
                  <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium"
                        :class="getStatusColor(order.status)">
                    {{ getStatusLabel(order.status) }}
                  </span>
              </td>

              <!-- Duration -->
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="text-sm text-gray-900">{{ order.estimatedDuration || 0 }} min</div>
                <div class="text-xs text-gray-500">{{ formatDuration(order.estimatedDuration || 0) }}</div>
              </td>

              <!-- Actions -->
              <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                <div class="flex space-x-2">
                  <button
                    @click="viewDetails(order)"
                    class="text-blue-600 hover:text-blue-900 p-1 rounded hover:bg-blue-50"
                    title="View details"
                  >
                    
                  </button>
                  <button
                    @click="viewCards(order)"
                    class="text-green-600 hover:text-green-900 p-1 rounded hover:bg-green-50"
                    title="View cards"
                  >
                    
                  </button>
                  <button
                    v-if="order.status === 'PENDING'"
                    @click="startOrder(order.id)"
                    class="text-purple-600 hover:text-purple-900 p-1 rounded hover:bg-purple-50"
                    title="Start"
                  >
                    ▶
                  </button>
                  <button
                    v-if="order.status === 'IN_PROGRESS'"
                    @click="completeOrder(order.id)"
                    class="text-green-600 hover:text-green-900 p-1 rounded hover:bg-green-50"
                    title="Complete"
                  >
                    
                  </button>
                </div>
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!--  EMPTY STATE -->
      <div v-else-if="!loading" class="text-center py-12">
        <div class="text-gray-500">
          <div class="text-4xl mb-4"></div>
          <div class="text-lg font-medium mb-2">{{ orders.length === 0 ? 'No orders found' : 'No orders match your filters' }}</div>
          <div class="text-sm">{{ orders.length === 0 ? 'Check if your backend is running' : 'Try adjusting your search criteria' }}</div>
          <button
            @click="loadOrders"
            class="mt-4 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700"
          >
             Reload
          </button>
        </div>
      </div>

      <!--  MODALS -->

      <!-- Details Modal -->
      <div v-if="showDetailsModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
        <div class="bg-white rounded-lg max-w-2xl w-full mx-4 max-h-96 overflow-y-auto">
          <div class="p-6">
            <div class="flex justify-between items-center mb-4">
              <h3 class="text-lg font-semibold">Details - {{ selectedOrder?.orderNumber }}</h3>
              <button @click="showDetailsModal = false" class="text-gray-400 hover:text-gray-600 text-xl">✕</button>
            </div>

            <div v-if="selectedOrder" class="space-y-3">
              <p><strong>ID:</strong> {{ selectedOrder.id }}</p>
              <p><strong>Created Date:</strong> {{ selectedOrder.createdDate }}</p>
              <p><strong>Card Count:</strong> {{ selectedOrder.cardCount }}</p>
              <p><strong>Cards with Names:</strong> {{ selectedOrder.cardsWithName }} ({{ selectedOrder.namePercentage }}%)</p>
              <p><strong>Priority:</strong> {{ selectedOrder.priority }}</p>
              <p><strong>Status:</strong> {{ selectedOrder.status }}</p>
              <p><strong>Estimated Duration:</strong> {{ selectedOrder.estimatedDuration }} minutes</p>
              <p><strong>Total Price:</strong> ${{ selectedOrder.totalPrice || 0 }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Cards Modal -->
      <div v-if="showCardsModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
        <div class="bg-white rounded-lg max-w-4xl w-full mx-4 max-h-96 overflow-y-auto">
          <div class="p-6">
            <div class="flex justify-between items-center mb-4">
              <h3 class="text-lg font-semibold">Cards - {{ selectedOrder?.orderNumber }}</h3>
              <button @click="showCardsModal = false" class="text-gray-400 hover:text-gray-600 text-xl">✕</button>
            </div>

            <div v-if="loadingCards" class="text-center py-8">
              <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
              <span class="text-gray-600 mt-2 block">Loading cards...</span>
            </div>

            <div v-else-if="orderCards" class="space-y-3">
              <p><strong>Total Cards:</strong> {{ orderCards.cardCount }}</p>
              <p><strong>With Names:</strong> {{ orderCards.cardsWithName }}</p>
              <p><strong>Percentage:</strong> {{ orderCards.namePercentage }}%</p>
              <p class="text-sm text-gray-600">Individual card details available via API</p>
            </div>

            <div v-else class="text-gray-500 text-center py-8">
              No card data available
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

// ========== STATE ==========
const loading = ref(false)
const orders = ref([])
const selectedOrder = ref(null)
const showDetailsModal = ref(false)
const showCardsModal = ref(false)
const orderCards = ref(null)
const loadingCards = ref(false)

// Date filtering
const enableDateFilter = ref(true)
const filterFromDate = ref('2025-06-01')

// Other filters
const filterStatus = ref('all')
const filterPriority = ref('all')
const searchTerm = ref('')

// ========== COMPUTED ==========
const filteredOrders = computed(() => {
  let filtered = [...orders.value]

  // Date filter (applied first)
  if (enableDateFilter.value && filterFromDate.value) {
    filtered = filtered.filter(order => {
      const orderDate = order.createdDate || order.orderDate || order.date
      if (!orderDate) return true

      const dateOnly = orderDate.split('T')[0]
      return dateOnly >= filterFromDate.value
    })
  }

  // Status filter
  if (filterStatus.value !== 'all') {
    filtered = filtered.filter(order => order.status === filterStatus.value)
  }

  // Priority filter
  if (filterPriority.value !== 'all') {
    filtered = filtered.filter(order => order.priority === filterPriority.value)
  }

  // Search filter
  if (searchTerm.value) {
    const term = searchTerm.value.toLowerCase()
    filtered = filtered.filter(order =>
      order.orderNumber.toLowerCase().includes(term) ||
      order.id?.toString().toLowerCase().includes(term)
    )
  }

  return filtered
})

const statistics = computed(() => {
  const total = filteredOrders.value.length
  const totalInDb = orders.value.length
  const pending = filteredOrders.value.filter(o => o.status === 'PENDING').length
  const inProgress = filteredOrders.value.filter(o => o.status === 'IN_PROGRESS').length
  const completed = filteredOrders.value.filter(o => o.status === 'COMPLETED').length
  const totalCards = filteredOrders.value.reduce((sum, o) => sum + o.cardCount, 0)

  return {
    total,
    totalInDb,
    pending,
    inProgress,
    completed,
    totalCards
  }
})

// ========== METHODS ==========

const loadOrders = async () => {
  loading.value = true
  try {
    console.log(' Loading orders using SAME endpoint as Dashboard...')

    // Try commandes endpoint like Dashboard
    const commandesResponse = await fetch('http://localhost:8080/api/commandes', {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' }
    })

    if (commandesResponse.ok) {
      const commandesData = await commandesResponse.json()
      console.log(' Commandes data retrieved:', commandesData.length)

      // Map French commandes to English orders
      const mappedOrders = commandesData.map(cmd => ({
        id: cmd.id,
        orderNumber: cmd.numeroCommande || cmd.num_commande,
        createdDate: (cmd.dateCreation || cmd.dateReception || cmd.date || '').split('T')[0],
        cardCount: cmd.nombreCartes || 0,
        cardsWithName: cmd.nombreAvecNom || 0,
        namePercentage: cmd.pourcentageAvecNom || 0,
        priority: mapPriority(cmd.priorite),
        totalPrice: cmd.prixTotal || 0,
        status: mapStatus(cmd.status || cmd.statut),
        estimatedDuration: cmd.dureeEstimeeMinutes || cmd.tempsEstimeMinutes || 0
      })).filter(order => order !== null)

      orders.value = mappedOrders
      console.log(' French commandes mapped to English orders:', mappedOrders.length)
      showNotification(` ${mappedOrders.length} real orders loaded`)
      return
    }

    // Fallback to /api/orders
    const response = await fetch('http://localhost:8080/api/orders')
    if (response.ok) {
      const data = await response.json()
      orders.value = data.map(mapOrderFromApi).filter(order => order !== null)
      showNotification(` ${orders.value.length} orders loaded (fallback)`)
    }

  } catch (error) {
    console.error(' Error loading orders:', error)
    showNotification(' Error loading orders', 'error')
    orders.value = []
  } finally {
    loading.value = false
  }
}

const mapOrderFromApi = (order) => {
  let orderDate = order.createdDate || order.dateCreation || order.date || '2025-06-01'
  if (orderDate.includes('T')) {
    orderDate = orderDate.split('T')[0]
  }

  return {
    id: order.id,
    orderNumber: order.orderNumber || order.numeroCommande || `ORD-${order.id}`,
    createdDate: orderDate,
    cardCount: order.cardCount || order.nombreCartes || 1,
    cardsWithName: order.cardsWithName || order.nombreAvecNom || 0,
    namePercentage: order.namePercentage || order.pourcentageAvecNom || 0,
    priority: mapPriority(order.priority || order.priorite || 'FAST'),
    totalPrice: order.totalPrice || order.prixTotal || 0,
    status: mapStatus(order.status || order.statut || 'PENDING'),
    estimatedDuration: order.estimatedDuration || order.estimatedTimeMinutes || 0
  }
}

const mapPriority = (priority) => {
  if (!priority) return 'FAST'
  const p = String(priority).toUpperCase()
  if (p.includes('EXCELSIOR')) return 'EXCELSIOR'
  if (p.includes('FAST+')) return 'FAST+'
  if (p.includes('FAST')) return 'FAST'
  if (p.includes('CLASSIC')) return 'CLASSIC'
  return 'FAST'
}

const mapStatus = (status) => {
  if (typeof status === 'number') {
    switch (status) {
      case 1: return 'PENDING'
      case 2: return 'SCHEDULED'
      case 3: return 'IN_PROGRESS'
      case 4: return 'COMPLETED'
      default: return 'PENDING'
    }
  }
  return String(status || 'PENDING').toUpperCase()
}

const refreshOrders = () => loadOrders()

const debugWorkingEndpoint = async () => {
  try {
    const response = await fetch('http://localhost:8080/api/commandes')
    if (response.ok) {
      const data = await response.json()
      console.log(' Debug data:', data)
      alert(`Found ${data.length} orders. Check console for details.`)
    }
  } catch (error) {
    alert(`Error: ${error.message}`)
  }
}

const viewDetails = (order) => {
  selectedOrder.value = order
  showDetailsModal.value = true
}

const viewCards = (order) => {
  selectedOrder.value = order
  showCardsModal.value = true
  orderCards.value = {
    cardCount: order.cardCount,
    cardsWithName: order.cardsWithName,
    namePercentage: order.namePercentage
  }
}

const startOrder = (id) => console.log('Start order:', id)
const completeOrder = (id) => console.log('Complete order:', id)

const getPriorityColor = (priority) => {
  switch (priority?.toUpperCase()) {
    case 'EXCELSIOR': return 'bg-red-100 text-red-800'
    case 'FAST+': return 'bg-orange-100 text-orange-800'
    case 'FAST': return 'bg-yellow-100 text-yellow-800'
    case 'CLASSIC': return 'bg-green-100 text-green-800'
    default: return 'bg-gray-100 text-gray-800'
  }
}

const getPriorityLabel = (priority) => {
  switch (priority?.toUpperCase()) {
    case 'EXCELSIOR': return ' Excelsior'
    case 'FAST+': return ' Fast+'
    case 'FAST': return ' Fast'
    case 'CLASSIC': return 'Classic'
    default: return ' Unknown'
  }
}

const getStatusColor = (status) => {
  switch (status?.toUpperCase()) {
    case 'PENDING': return 'bg-gray-100 text-gray-800'
    case 'SCHEDULED': return 'bg-blue-100 text-blue-800'
    case 'IN_PROGRESS': return 'bg-yellow-100 text-yellow-800'
    case 'COMPLETED': return 'bg-green-100 text-green-800'
    default: return 'bg-gray-100 text-gray-800'
  }
}

const getStatusLabel = (status) => {
  switch (status?.toUpperCase()) {
    case 'PENDING': return 'Pending'
    case 'SCHEDULED': return 'Scheduled'
    case 'IN_PROGRESS': return 'In Progress'
    case 'COMPLETED': return 'Completed'
    default: return 'Unknown'
  }
}

const getQualityIndicator = (percentage) => {
  if (percentage >= 95) return ''
  if (percentage >= 80) return ''
  return ''
}

const formatDate = (dateStr) => {
  if (!dateStr) return 'N/A'
  try {
    return new Date(dateStr).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    })
  } catch {
    return dateStr
  }
}

const formatDuration = (minutes) => {
  if (!minutes) return 'N/A'
  const hours = Math.floor(minutes / 60)
  const remainingMinutes = minutes % 60
  if (hours > 0) {
    return `${hours}h ${remainingMinutes}m`
  }
  return `${remainingMinutes}m`
}

const showNotification = (message, type = 'success') => {
  console.log(`${type === 'success' ? '' : ''} ${message}`)
}

onMounted(() => {
  console.log(' OrdersView mounted - Loading real orders...')
  loadOrders()
})
</script>
<style scoped>
.orders-view {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

/* Loading spinner */
.animate-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Hover effects */
.hover\:bg-gray-50:hover {
  background-color: #f9fafb;
}

.transition-colors {
  transition-property: color, background-color, border-color;
  transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
  transition-duration: 150ms;
}

/* Responsive design */
@media (max-width: 768px) {
  .orders-view {
    padding: 16px;
  }

  .overflow-x-auto {
    font-size: 0.875rem;
  }

  .grid-cols-1 {
    grid-template-columns: repeat(1, minmax(0, 1fr));
  }
}

@media (min-width: 768px) {
  .md\:grid-cols-4 {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}
</style>
