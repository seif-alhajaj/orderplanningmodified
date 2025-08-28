<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <div class="max-w-7xl mx-auto">

      <!-- Breadcrumb et retour -->
      <div class="flex items-center justify-between mb-6">
        <div class="flex items-center space-x-4">
          <button
            @click="$emit('back')"
            class="flex items-center text-blue-600 hover:text-blue-800 font-medium"
          >
            ← Back to Employees
          </button>
          <div class="text-gray-400">/</div>
          <h1 class="text-2xl font-bold text-gray-900">
            {{ employeeData?.name || 'Employee Details' }}
          </h1>
        </div>
        <div class="flex items-center space-x-3">
          <span class="text-sm text-gray-600">{{ selectedDate }}</span>
          <button
            @click="refreshData"
            :disabled="loading"
            class="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50"
          >
            {{ loading ? '⏳' : '🔄' }} Refresh
          </button>
        </div>
      </div>

      <!-- En-tête employé -->
      <div class="bg-white rounded-lg shadow-sm border p-6 mb-6">
        <div class="flex items-center justify-between">
          <div class="flex items-center">
            <div class="w-20 h-20 bg-gradient-to-r from-blue-500 to-purple-600 rounded-full flex items-center justify-center">
              <span class="text-white font-bold text-2xl">
                {{ employeeData?.name?.charAt(0) || '?' }}
              </span>
            </div>
            <div class="ml-6">
              <h2 class="text-2xl font-bold text-gray-900">{{ employeeData?.name || 'Unknown Employee' }}</h2>
              <p class="text-gray-600">{{ employeeData?.email || 'No email' }}</p>
              <div class="flex items-center space-x-4 mt-2">
                <span :class="[
                  'px-3 py-1 rounded-full text-sm font-medium',
                  employeeData?.active ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                ]">
                  {{ employeeData?.active ? '✅ Active' : '❌ Inactive' }}
                </span>
                <span class="text-sm text-gray-600">
                  {{ employeeData?.workHoursPerDay || 8 }}h/day
                </span>
              </div>
            </div>
          </div>

          <!-- Statistiques rapides -->
          <div class="text-right">
            <div class="grid grid-cols-2 gap-4">
              <div class="text-center">
                <div class="text-2xl font-bold text-blue-600">{{ orders.length }}</div>
                <div class="text-sm text-gray-600">Orders</div>
              </div>
              <div class="text-center">
                <div class="text-2xl font-bold text-green-600">{{ totalCards }}</div>
                <div class="text-sm text-gray-600">Cards</div>
              </div>
              <div class="text-center">
                <div class="text-2xl font-bold text-purple-600">
                  {{ Math.round(totalDuration / 60) }}h
                </div>
                <div class="text-sm text-gray-600">Duration</div>
              </div>
              <div class="text-center">
                <div class="text-2xl font-bold text-orange-600">{{ workloadPercentage }}%</div>
                <div class="text-sm text-gray-600">Workload</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Barre de progression globale -->
      <div class="bg-white rounded-lg shadow-sm border p-6 mb-6">
        <div class="flex justify-between items-center mb-2">
          <h3 class="text-lg font-semibold text-gray-900">Daily Workload</h3>
          <span class="text-sm font-medium text-gray-600">
            {{ totalDuration }}min / {{ (employeeData?.workHoursPerDay || 8) * 60 }}min
          </span>
        </div>
        <div class="w-full bg-gray-200 rounded-full h-4">
          <div
            :class="[
              'h-4 rounded-full transition-all duration-500',
              workloadPercentage >= 100 ? 'bg-red-500' :
              workloadPercentage >= 80 ? 'bg-yellow-500' :
              'bg-green-500'
            ]"
            :style="{ width: Math.min(workloadPercentage, 100) + '%' }"
          ></div>
        </div>
      </div>

      <!-- Liste des commandes -->
      <div class="space-y-4">
        <h3 class="text-xl font-bold text-gray-900 flex items-center">
          📋 Assigned Orders
          <span class="ml-2 text-sm font-normal bg-blue-100 text-blue-800 px-2 py-1 rounded-full">
            {{ orders.length }} orders
          </span>
        </h3>

        <div v-if="loading" class="text-center py-8">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p class="text-gray-600">Loading orders...</p>
        </div>

        <div v-else-if="orders.length === 0" class="text-center py-8 bg-white rounded-lg shadow-sm border">
          <svg class="mx-auto h-12 w-12 text-gray-400 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
          </svg>
          <h3 class="text-lg font-medium text-gray-900 mb-2">No orders assigned</h3>
          <p class="text-gray-600">This employee has no orders assigned for the selected date.</p>
        </div>

        <div v-else class="space-y-4">
          <div
            v-for="order in orders"
            :key="order.id"
            class="bg-white rounded-lg shadow-sm border hover:shadow-md transition-shadow"
          >
            <!-- En-tête de commande -->
            <div class="p-6 border-b">
              <div class="flex items-center justify-between">
                <div class="flex items-center">
                  <div class="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center">
                    <span class="text-blue-600 font-bold">📦</span>
                  </div>
                  <div class="ml-4">
                    <h4 class="text-lg font-semibold text-gray-900">{{ order.orderNumber }}</h4>
                    <p class="text-sm text-gray-600">
                      {{ order.cardCount || 0 }} cards •
                      {{ formatDuration(order.estimatedDuration) }} •
                      Priority: {{ order.priority || 'Medium' }}
                    </p>
                  </div>
                </div>

                <div class="flex items-center space-x-3">
                  <span :class="[
                    'px-3 py-1 rounded-full text-sm font-medium',
                    getPriorityClass(order.priority)
                  ]">
                    {{ order.priority || 'Medium' }}
                  </span>
                  <span :class="[
                    'px-3 py-1 rounded-full text-sm font-medium',
                    getStatusClass(order.status)
                  ]">
                    {{ getStatusText(order.status) }}
                  </span>
                  <button
                    @click="toggleOrderCards(order.orderId)"
                    class="bg-blue-50 text-blue-600 px-3 py-2 rounded-lg hover:bg-blue-100 text-sm font-medium"
                  >
                    {{ order.showCards ? '📁 Hide' : '🃏 Show' }} Cards
                  </button>
                </div>
              </div>

              <!-- Barre de progression de la commande -->
              <div class="mt-4">
                <div class="flex justify-between text-sm mb-1">
                  <span class="text-gray-600">Progress</span>
                  <span class="font-medium">{{ order.progress || 0 }}%</span>
                </div>
                <div class="w-full bg-gray-200 rounded-full h-2">
                  <div
                    class="bg-blue-500 h-2 rounded-full transition-all duration-300"
                    :style="{ width: (order.progress || 0) + '%' }"
                  ></div>
                </div>
              </div>
            </div>

            <!-- Liste des cartes (accordéon) -->
            <div v-if="order.showCards" class="border-t bg-gray-50">
              <div class="p-6">
                <div v-if="order.loadingCards" class="text-center py-4">
                  <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-600 mx-auto mb-2"></div>
                  <p class="text-sm text-gray-600">Loading cards...</p>
                </div>

                <div v-else-if="order.cards && order.cards.length > 0">
                  <h5 class="text-sm font-semibold text-gray-700 mb-4">
                    🃏 Cards in this order ({{ order.cards.length }})
                  </h5>
                  <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3">
                    <div
                      v-for="card in order.cards"
                      :key="card.id"
                      class="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-sm transition-shadow"
                    >
                      <div class="flex items-start justify-between mb-2">
                        <div class="flex-1 min-w-0">
                          <h6 class="font-medium text-gray-900 truncate">
                            {{ card.name || 'Unknown Card' }}
                          </h6>
                          <p class="text-sm text-gray-500 truncate">
                            {{ card.label_name || card.code_barre || 'No label' }}
                          </p>
                        </div>
                      </div>

                      <div class="flex justify-between items-center text-xs text-gray-600">
                        <span>⏱️ {{ formatDuration(card.duration || 3) }}</span>
                        <span>💰 {{ (card.amount || 0).toFixed(2) }}€</span>
                      </div>

                      <div class="mt-2 text-xs text-gray-400 truncate" :title="card.id">
                        ID: {{ card.id }}
                      </div>
                    </div>
                  </div>
                </div>

                <div v-else class="text-center py-4 text-gray-500">
                  <p>No cards found for this order</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'

// ========== PROPS ==========
const props = defineProps<{
  employeeId: string
  selectedDate: string
  mode?: 'gestion' | 'planning'
}>()

// ========== EMITS ==========
const emit = defineEmits<{
  back: []
  refresh: []
}>()

// ========== STATE ==========
const loading = ref(false)
const employeeData = ref<any>(null)
const orders = ref<any[]>([])

// ========== COMPUTED ==========
const totalCards = computed(() => {
  return orders.value.reduce((sum, order) => sum + (order.cardCount || 0), 0)
})

const totalDuration = computed(() => {
  return orders.value.reduce((sum, order) => sum + (order.estimatedDuration || 0), 0)
})

const workloadPercentage = computed(() => {
  const maxMinutes = (employeeData.value?.workHoursPerDay || 8) * 60
  return Math.round((totalDuration.value / maxMinutes) * 100)
})

// ========== METHODS ==========
const refreshData = async () => {
  emit('refresh')
  await loadEmployeeData()
  await loadEmployeeOrders()
}

const loadEmployeeData = async () => {
  loading.value = true
  try {
    console.log('🔍 Loading employee data for:', props.employeeId)

    // Utiliser l'endpoint qui fonctionne et filtrer côté client
    const response = await fetch('/api/employees') // ✅ ENDPOINT CORRECT

    if (response.ok) {
      const employees = await response.json()
      const employee = employees.find(emp => emp.id === props.employeeId)

      if (employee) {
        employeeData.value = {
          id: employee.id,
          firstName: employee.firstName,
          lastName: employee.lastName,
          email: employee.email,
          workHoursPerDay: employee.workHoursPerDay || 8,
          active: employee.active,
          status: employee.active ? 'AVAILABLE' : 'OFFLINE'
        }
        console.log('✅ Employee data loaded:', employeeData.value)
      } else {
        console.error('Employee not found with ID:', props.employeeId)
      }
    } else {
      console.error('Failed to load employee data:', response.status)
    }
  } catch (error) {
    console.error('Error loading employee data:', error)
  } finally {
    loading.value = false
  }
}

const loadEmployeeOrders = async () => {
  loading.value = true
  try {
    const planningDate = '2025-06-01' // ou récupérez-la de la configuration
    console.log('🔍 DEBUG - Loading orders for employee:', props.employeeId)
    console.log('🔍 DEBUG - Selected date:', props.selectedDate)

    const response = await fetch(`/api/planning/employee/${props.employeeId}?date=${props.selectedDate}`)
    if (response.ok) {
      const data = await response.json()
      console.log('🔍 DEBUG - Response data:', data)

      if (data.success) {
        orders.value = data.orders || []
        console.log('Employee orders loaded:', orders.value.length, 'orders')
        console.log('Summary:', data.summary)
      } else {
        console.error('Failed to load orders:', data.error)
        orders.value = []
      }
    } else {
      console.error('HTTP error loading orders:', response.status)
      orders.value = []
    }
  } catch (error) {
    console.error('Error loading employee orders:', error)
    orders.value = []
  } finally {
    loading.value = false
  }
}
// Dans EmployeeDetailPage.vue, remplacez la méthode toggleOrderCards :

// Dans EmployeeDetailPage.vue, remplacez la méthode toggleOrderCards :

const toggleOrderCards = async (orderId) => {
  console.log('🃏 toggleOrderCards called with:', orderId);
  console.log('📊 Available orders:', orders.value.map(o => ({ id: o.id, orderId: o.orderId, orderNumber: o.orderNumber })));

  // Essayons de trouver l'ordre avec différentes propriétés
  let order = orders.value.find(o => o.orderId === orderId);
  if (!order) {
    order = orders.value.find(o => o.id === orderId);
  }

  if (!order) {
    console.error('❌ Order not found with any ID:', orderId);
    console.error('❌ Available orders structure:', orders.value[0]);
    return;
  }

  console.log('✅ Order found:', order);

  order.showCards = !order.showCards;
  console.log('🔄 showCards toggled to:', order.showCards);

  if (order.showCards && (!order.cards || order.cards.length === 0)) {
    order.loadingCards = true;

    // Utilisez l'orderId ou l'id selon ce qui fonctionne
    const actualOrderId = order.orderId || order.id;
    console.log('📡 Loading cards for actual order ID:', actualOrderId);

    try {
      const response = await fetch(`http://localhost:8080/api/frontend/employees/order/${actualOrderId}/cards`);
      console.log('📡 Response status:', response.status);

      if (response.ok) {
        const data = await response.json();
        console.log('✅ Cards data received:', data);

        order.cards = data.cards || [];
        console.log('🃏 Cards loaded:', order.cards.length, 'cards');

        if (order.cards.length === 0) {
          console.warn('⚠️ No cards found for this order');
        }
      } else {
        console.error('❌ Failed to fetch cards:', response.status);
        const errorText = await response.text();
        console.error('❌ Error details:', errorText);
      }
    } catch (error) {
      console.error('❌ Error loading order cards:', error);
    } finally {
      order.loadingCards = false;
    }
  }
}
const formatDuration = (minutes: number) => {
  if (minutes < 60) {
    return `${minutes}min`
  }
  const hours = Math.floor(minutes / 60)
  const remainingMinutes = minutes % 60
  return remainingMinutes > 0 ? `${hours}h${remainingMinutes}min` : `${hours}h`
}

const getPriorityClass = (priority: string) => {
  switch (priority?.toLowerCase()) {
    case 'Excelsior':
    case 'Fast+':
      return 'bg-red-100 text-red-800'
    case 'Fast':
      return 'bg-yellow-100 text-yellow-800'
    case 'Classic':
      return 'bg-green-100 text-green-800'
    default:
      return 'bg-gray-100 text-gray-800'
  }
}

const getStatusClass = (status: string) => {
  switch (status?.toLowerCase()) {
    case 'completed':
      return 'bg-green-100 text-green-800'
    case 'in_progress':
      return 'bg-blue-100 text-blue-800'
    case 'scheduled':
      return 'bg-purple-100 text-purple-800'
    default:
      return 'bg-gray-100 text-gray-800'
  }
}

const getStatusText = (status: string) => {
  switch (status?.toLowerCase()) {
    case 'scheduled':
      return '📅 Scheduled'
    case 'in_progress':
      return '⏳ In Progress'
    case 'completed':
      return '✅ Completed'
    case 'cancelled':
      return '❌ Cancelled'
    default:
      return '❓ Unknown'
  }
}

// ========== LIFECYCLE ==========
onMounted(() => {
  loadEmployeeData()
  loadEmployeeOrders()
})

watch(() => props.selectedDate, () => {
  loadEmployeeOrders()
})
</script>

<style scoped>
.transition-shadow {
  transition: box-shadow 0.2s ease;
}

.transition-colors {
  transition: background-color 0.2s, color 0.2s;
}

.transition-all {
  transition: all 0.3s ease;
}
</style>
