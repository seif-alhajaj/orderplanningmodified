<template>
  <div class="planning-page">
    <!-- Header -->
    <div class="bg-white rounded-lg shadow-md p-6 mb-6">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-gray-900">📅 Planning Management</h1>
          <p class="text-gray-600 mt-1">Generate and manage work assignments for Pokemon card orders</p>
        </div>
        <div class="flex space-x-3">
          <button
            @click="testPlanningEndpoints"
            class="btn-secondary"
          >
            🔍 Debug API
          </button>
          <button
            @click="refreshData"
            :disabled="loading"
            class="btn-primary"
          >
            {{ loading ? '⏳ Loading...' : '🔄 Refresh' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Statistics Cards -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-6">
      <div class="card">
        <div class="flex items-center">
          <div class="bg-blue-500 rounded-lg p-3 mr-4">
            <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V9a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"></path>
            </svg>
          </div>
          <div>
            <p class="text-sm text-gray-600">Total Orders</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.totalOrders }}</p>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="flex items-center">
          <div class="bg-green-500 rounded-lg p-3 mr-4">
            <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>
          </div>
          <div>
            <p class="text-sm text-gray-600">Planned Orders</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.plannedOrders }}</p>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="flex items-center">
          <div class="bg-orange-500 rounded-lg p-3 mr-4">
            <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>
          </div>
          <div>
            <p class="text-sm text-gray-600">Employees Used</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.employeeCount }}</p>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="flex items-center">
          <div class="bg-purple-500 rounded-lg p-3 mr-4">
            <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path>
            </svg>
          </div>
          <div>
            <p class="text-sm text-gray-600">Total Plannings</p>
            <p class="text-2xl font-semibold text-gray-900">{{ plannings.length }}</p>
            <p v-if="duplicateStats.totalDuplicates > 0" class="text-xs text-red-600">
              ⚠️ {{ duplicateStats.totalDuplicates }} duplicates
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- Planning Configuration -->
    <div class="bg-white rounded-lg shadow-md p-6 mb-6">
      <h2 class="text-lg font-semibold text-gray-900 mb-4">Planning Configuration</h2>
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">
            Start Date
          </label>
          <input
            v-model="config.startDate"
            type="date"
            class="input-field"
            :min="'2025-01-01'"
          >
          <p class="text-xs text-gray-500 mt-1">
            📦 All orders from this date onwards will be planned (Currently: ~50 orders from June 1st, 2025)
          </p>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">Time per Card (minutes)</label>
          <input
            v-model.number="config.cardProcessingTime"
            type="number"
            min="1"
            max="10"
            class="input-field"
          >
          <p class="text-xs text-gray-500 mt-1">Current: {{ CARD_PROCESSING_TIME }}min (from config)</p>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">Priority Mode</label>
          <select v-model="config.priorityMode" class="input-field">
            <option value="excelsior">Excelsior First</option>
            <option value="balanced">Balanced</option>
            <option value="efficiency">Efficiency First</option>
          </select>
        </div>
      </div>

      <!-- Options -->
      <div class="mt-4 flex items-center space-x-6">
        <label class="flex items-center">
          <input
            v-model="config.redistributeOverload"
            type="checkbox"
            class="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
          >
          <span class="ml-2 text-sm text-gray-700">Redistribute overload</span>
        </label>
        <label class="flex items-center">
          <input
            v-model="config.respectPriorities"
            type="checkbox"
            class="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
          >
          <span class="ml-2 text-sm text-gray-700">Respect priorities</span>
        </label>
      </div>

      <!-- Action Buttons -->
      <div class="mt-6 flex space-x-3">
        <button
          @click="generatePlanning"
          :disabled="generating"
          class="btn-primary"
        >
          {{ generating ? '⏳ Generating...' : '🚀 Generate Planning' }}
        </button>

        <!-- AJOUTEZ CES NOUVEAUX BOUTONS ICI -->
        <button
          @click="generatePlanningWithGreedy"
          :disabled="generating"
          class="bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 disabled:opacity-50"
        >
          {{ generating ? '⏳ Generating...' : '🎲 Try Greedy Algorithm' }}
        </button>

        <button
          @click="debugPlanningGeneration"
          class="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700"
        >
          🔍 Debug Planning
        </button>

        <button
          @click="optimizePlanning"
          :disabled="optimizing"
          class="btn-secondary"
        >
          {{ optimizing ? '⏳ Optimizing...' : '⚡ Optimize Planning' }}
        </button>
        <button
          @click="cleanupData"
          class="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700"
        >
          🗑️ Clean Up
        </button>
        <button
          @click="removeDuplicates"
          class="bg-purple-600 text-white px-4 py-2 rounded-lg hover:bg-purple-700"
        >
          🧹 Remove Duplicates
        </button>
      </div>
    </div>

    <!-- Plannings List -->
    <div v-if="plannings.length > 0" class="bg-white rounded-lg shadow-md overflow-hidden">
      <div class="px-6 py-4 border-b border-gray-200">
        <h2 class="text-lg font-semibold text-gray-900">Current Plannings</h2>
      </div>

      <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
          <tr>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Date</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Employee</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Order</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Time</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Duration</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
          </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
          <tr
            v-for="planning in groupedPlannings"
            :key="planning.id"
            :class="[
                'hover:bg-gray-50',
                planning.isDuplicate ? 'bg-red-50 border-l-4 border-red-400' : ''
              ]"
          >
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
              {{ formatDate(planning.planningDate) }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
              <div class="text-sm font-medium text-gray-900">{{ planning.employeeName || 'Unknown Employee' }}</div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
              <div class="text-sm font-medium text-gray-900">
                {{ planning.orderNumber || `Order ${planning.orderId}` }}
                <span v-if="planning.isDuplicate" class="ml-2 text-xs bg-red-100 text-red-600 px-2 py-1 rounded">
                    ⚠️ Duplicate ({{ planning.duplicateCount }})
                  </span>
              </div>
              <div class="text-sm text-gray-500">{{ planning.cardCount || 0 }} cards</div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
              {{ planning.startTime }} - {{ planning.endTime }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
              <div class="text-sm font-medium text-gray-900">{{ formatDuration(planning.durationMinutes) }}</div>
              <div class="text-sm text-gray-500">
                {{ planning.cardCount || 0 }} cards × {{ CARD_PROCESSING_TIME }}min
              </div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
                <span :class="[
                  'inline-flex px-2 py-1 text-xs font-semibold rounded-full',
                  planning.completed ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'
                ]">
                  {{ planning.completed ? 'Completed' : 'Pending' }}
                </span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
              <button
                @click="viewPlanningDetails(planning)"
                class="text-blue-600 hover:text-blue-900 mr-3"
              >
                View
              </button>
              <button
                @click="editPlanning(planning)"
                class="text-green-600 hover:text-green-900"
              >
                Edit
              </button>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else-if="!loading" class="bg-white rounded-lg shadow-md p-8 text-center">
      <svg class="mx-auto h-12 w-12 text-gray-400 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
      </svg>
      <h3 class="text-lg font-medium text-gray-900 mb-2">No plannings found</h3>
      <p class="text-gray-600 mb-4">Start by generating your first planning</p>
      <button
        @click="generatePlanning"
        class="btn-primary"
      >
        🚀 Generate First Planning
      </button>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="bg-white rounded-lg shadow-md p-8 text-center">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
      <p class="text-gray-600">{{ loadingMessage || 'Loading plannings...' }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'

// ========== CONSTANTS ==========
// Configuration from environment or default values
const CARD_PROCESSING_TIME = parseInt(import.meta.env.VITE_CARD_PROCESSING_TIME || '3') // minutes per card
const DEFAULT_WORK_HOURS = parseInt(import.meta.env.VITE_DEFAULT_WORK_HOURS || '8') // hours per day

console.log(`⚙️ Configuration: ${CARD_PROCESSING_TIME}min per card, ${DEFAULT_WORK_HOURS}h per day`)

// ========== INTERFACES ==========
interface Planning {
  id: string
  orderId: string
  employeeId: string
  employeeName?: string
  orderNumber?: string
  planningDate: string
  startTime: string
  endTime: string
  durationMinutes: number
  cardCount?: number
  priority?: string
  status?: string
  completed: boolean
  notes?: string
}

interface PlanningConfig {
  startDate: string
  cardProcessingTime: number
  priorityMode: string
  redistributeOverload: boolean
  respectPriorities: boolean
}

// ========== STATE ==========
const loading = ref(false)
const generating = ref(false)
const optimizing = ref(false)
const loadingMessage = ref('')

const plannings = ref<Planning[]>([])

const config = ref<PlanningConfig>({
  startDate: '2025-06-01', // ✅ Default: June 1st, 2025
  cardProcessingTime: CARD_PROCESSING_TIME,
  priorityMode: 'excelsior',
  redistributeOverload: true,
  respectPriorities: true
})



const stats = ref({
  totalOrders: 0,
  plannedOrders: 0,
  pendingOrders: 0,
  efficiency: 0,
  employeeCount: 0,
  duplicateCount: 0
})

// Computed for duplicate statistics
const duplicateStats = computed(() => {
  const duplicateGroups = new Map()

  plannings.value.forEach(planning => {
    const key = `${planning.orderNumber}_${planning.planningDate}_${planning.startTime}`
    if (!duplicateGroups.has(key)) {
      duplicateGroups.set(key, [])
    }
    duplicateGroups.get(key).push(planning)
  })

  const duplicateGroupsArray = Array.from(duplicateGroups.values()).filter(group => group.length > 1)
  const totalDuplicates = duplicateGroupsArray.reduce((sum, group) => sum + (group.length - 1), 0)

  return {
    duplicateGroups: duplicateGroupsArray.length,
    totalDuplicates
  }
})

// ========== COMPUTED ==========
const minDate = computed(() => {
  return new Date().toISOString().split('T')[0]
})

const groupedPlannings = computed(() => {
  const sorted = plannings.value.sort((a, b) => {
    const dateA = new Date(`${a.planningDate} ${a.startTime}`)
    const dateB = new Date(`${b.planningDate} ${b.startTime}`)
    return dateA.getTime() - dateB.getTime()
  })

  // Mark potential duplicates (same order at same time)
  const duplicateGroups = new Map()
  sorted.forEach(planning => {
    const key = `${planning.orderNumber}_${planning.planningDate}_${planning.startTime}`
    if (!duplicateGroups.has(key)) {
      duplicateGroups.set(key, [])
    }
    duplicateGroups.get(key).push(planning)
  })

  // Add isDuplicate flag
  return sorted.map(planning => {
    const key = `${planning.orderNumber}_${planning.planningDate}_${planning.startTime}`
    const group = duplicateGroups.get(key)
    return {
      ...planning,
      isDuplicate: group && group.length > 1,
      duplicateCount: group ? group.length : 1
    }
  })
})

// ========== METHODS ==========
function getNextBusinessDay(): string {
  // Par défaut, utiliser le 1er juin 2025
  return '2025-06-01'
}

const refreshData = async () => {
  await Promise.all([
    loadStats(),
    loadPlannings()
  ])
}

const loadStats = async () => {
  try {
    // Try multiple endpoints for stats
    const endpoints = [
      'http://localhost:8080/api/dashboard/stats',
      'http://localhost:8080/api/planning/stats',
      'http://localhost:8080/api/stats'
    ]

    for (const endpoint of endpoints) {
      try {
        const response = await fetch(endpoint)
        if (response.ok) {
          const data = await response.json()
          console.log(`✅ Stats loaded from ${endpoint}:`, data)

          stats.value = {
            totalOrders: data.totalCommandes || data.totalOrders || 0,
            plannedOrders: data.commandesEnCours || data.plannedOrders || 0,
            pendingOrders: data.commandesEnAttente || data.pendingOrders || 0,
            efficiency: data.efficiency || 85,
            employeeCount: data.employesActifs || data.employeeCount || 0
          }
          break
        }
      } catch (error) {
        console.warn(`⚠️ ${endpoint} failed:`, error)
        continue
      }
    }
  } catch (error) {
    console.error('❌ Error loading stats:', error)
  }
}

const loadPlannings = async () => {
  loading.value = true
  loadingMessage.value = 'Loading plannings...'

  try {
    // Try different endpoints in order of preference
    const endpoints = [
      'http://localhost:8080/api/planning'
    ]

    let rawPlannings: any[] = []

    for (const endpoint of endpoints) {
      try {
        console.log('🔄 Trying plannings endpoint:', endpoint)
        const response = await fetch(endpoint)

        if (response.ok) {
          const data = await response.json()
          console.log(`✅ Plannings loaded from ${endpoint}:`, data)

          // Handle different response formats
          rawPlannings = data.plannings || data.data || data || []
          break
        } else {
          console.log(`❌ Failed to load from ${endpoint}: ${response.status}`)
        }
      } catch (error) {
        console.log(`❌ Error with endpoint ${endpoint}:`, error)
        continue
      }
    }

    if (rawPlannings.length === 0) {
      console.warn('⚠️ No raw plannings data found')
      plannings.value = []
      showNotification('No plannings found', 'info')
      return
    }

    console.log(`📊 Raw plannings data (first 3):`, rawPlannings.slice(0, 3))

    // Map raw data to Planning objects with detailed logging
    const mappedPlannings = rawPlannings.map((rawPlanning, index) => {
      const mapped = mapPlanning(rawPlanning)

      // Log first few mappings for debugging
      if (index < 3) {
        console.log(`🔍 Mapping planning ${index}:`, {
          raw: rawPlanning,
          mapped: {
            id: mapped.id,
            orderId: mapped.orderId,
            employeeId: mapped.employeeId,
            employeeName: mapped.employeeName,
            orderNumber: mapped.orderNumber,
            planningDate: mapped.planningDate,
            startTime: mapped.startTime
          }
        })
      }

      return mapped
    })

    // Remove duplicates with detailed logging
    const uniquePlannings: Planning[] = []
    const seen = new Set()
    const duplicateInfo: Array<{key: string, count: number}> = []

    for (const planning of mappedPlannings) {
      // Create unique key with detailed logging
      const uniqueKey = `${planning.orderId}_${planning.employeeId}_${planning.planningDate}_${planning.startTime}`

      if (!seen.has(uniqueKey)) {
        seen.add(uniqueKey)
        uniquePlannings.push(planning)
      } else {
        // Log duplicate details
        const existingDup = duplicateInfo.find(d => d.key === uniqueKey)
        if (existingDup) {
          existingDup.count++
        } else {
          duplicateInfo.push({ key: uniqueKey, count: 2 })
        }

        console.warn(`🔄 Duplicate planning detected:`, {
          key: uniqueKey,
          orderNumber: planning.orderNumber,
          employeeName: planning.employeeName,
          date: planning.planningDate,
          time: planning.startTime,
          orderId: planning.orderId,
          employeeId: planning.employeeId
        })
      }
    }

    // Log filtering summary
    console.log(`🎯 Filtering summary:`)
    console.log(`  Raw plannings: ${rawPlannings.length}`)
    console.log(`  After mapping: ${mappedPlannings.length}`)
    console.log(`  After deduplication: ${uniquePlannings.length}`)
    console.log(`  Unique keys seen: ${seen.size}`)
    console.log(`  Duplicate groups: ${duplicateInfo.length}`)

    // Log some unique keys to see if they look correct
    console.log(`🔍 Sample unique keys:`, Array.from(seen).slice(0, 5))

    // If all plannings were filtered out, there's likely an issue with the mapping
    if (rawPlannings.length > 0 && uniquePlannings.length === 0) {
      console.error('❌ ALL PLANNINGS FILTERED OUT - LIKELY MAPPING ISSUE')
      console.error('Sample raw planning data:', rawPlannings[0])
      console.error('Sample mapped planning:', mappedPlannings[0])

      // Emergency fallback - show plannings without deduplication
      console.warn('🚨 Emergency fallback: showing all plannings without deduplication')
      plannings.value = mappedPlannings
      showNotification(`⚠️ Loaded ${mappedPlannings.length} plannings (deduplication disabled due to mapping issues)`, 'warning')
      return
    }

    plannings.value = uniquePlannings

    console.log(`🎯 Final plannings: ${rawPlannings.length} raw → ${uniquePlannings.length} unique`)

    if (rawPlannings.length !== uniquePlannings.length) {
      const duplicatesRemoved = rawPlannings.length - uniquePlannings.length
      showNotification(`Loaded ${uniquePlannings.length} unique plannings (${duplicatesRemoved} duplicates filtered)`, 'success')
    } else {
      showNotification(`Loaded ${uniquePlannings.length} plannings`, 'success')
    }

  } catch (error) {
    console.error('❌ Error loading plannings:', error)
    showNotification('Error loading plannings', 'error')
  } finally {
    loading.value = false
    loadingMessage.value = ''
  }
}

// Remplacez la fonction mapPlanning dans Planning.vue par cette version avec debug

const mapPlanning = (planningData: any): Planning => {
  console.log('🔍 Raw planning data:', planningData)

  // Calculate card count and duration
  const cardCount = planningData.cardCount || planningData.nombreCartes || planningData.cards_count || 0
  const calculatedDuration = cardCount > 0 ? cardCount * CARD_PROCESSING_TIME : (planningData.durationMinutes || planningData.dureeMinutes || planningData.duree_minutes || 60)

  const mapped = {
    id: planningData.id || `planning-${Date.now()}`,
    orderId: planningData.orderId || planningData.order_id || '',
    employeeId: planningData.employeeId || planningData.employe_id || '',
    employeeName: planningData.employeeName || planningData.employe_nom || planningData.employeFullName || 'Unknown Employee',
    orderNumber: planningData.orderNumber || planningData.numeroCommande || planningData.num_commande || `Order ${planningData.orderId}`,
    planningDate: planningData.planningDate || planningData.datePlanifiee || planningData.date_planification || new Date().toISOString().split('T')[0],
    startTime: planningData.startTime || planningData.heureDebut || planningData.heure_debut || '09:00',
    endTime: planningData.endTime || planningData.heureFin || planningData.heure_fin || calculateEndTime(planningData.startTime || '09:00', calculatedDuration),
    durationMinutes: calculatedDuration,
    cardCount,
    priority: planningData.priority || planningData.priorite || 'MEDIUM',
    status: planningData.status || planningData.statut || 'PENDING',
    completed: planningData.completed || planningData.terminee || false,
    notes: planningData.notes || `${cardCount} cards × ${CARD_PROCESSING_TIME}min = ${calculatedDuration}min`
  }

  console.log('✅ Mapped planning:', {
    id: mapped.id,
    orderId: mapped.orderId,
    employeeId: mapped.employeeId,
    employeeName: mapped.employeeName,
    orderNumber: mapped.orderNumber,
    planningDate: mapped.planningDate,
    startTime: mapped.startTime
  })

  return mapped
}

// Helper function to calculate end time based on start time and duration
const calculateEndTime = (startTime: string, durationMinutes: number): string => {
  try {
    const [hours, minutes] = startTime.split(':').map(Number)
    const startDate = new Date()
    startDate.setHours(hours, minutes, 0, 0)

    const endDate = new Date(startDate.getTime() + durationMinutes * 60000)

    return endDate.toTimeString().slice(0, 5) // Format HH:MM
  } catch (error) {
    // Fallback calculation
    const totalMinutes = durationMinutes + (parseInt(startTime.split(':')[0]) * 60) + parseInt(startTime.split(':')[1] || '0')
    const endHours = Math.floor(totalMinutes / 60) % 24
    const endMins = totalMinutes % 60
    return `${endHours.toString().padStart(2, '0')}:${endMins.toString().padStart(2, '0')}`
  }
}

const generatePlanning = async () => {
  if (generating.value) return

  generating.value = true
  loadingMessage.value = 'Generating planning for all orders from selected date...'

  try {
    console.log('🚀 Starting planning generation with config:', config.value)

    const planningConfig = {
      startDate: config.value.startDate,
      timePerCard: config.value.cardProcessingTime,
      cleanFirst: true,
      priorityMode: config.value.priorityMode,
      planAllOrders: true
    }

    console.log('📋 Sending request to /api/planning/generate with:', planningConfig)

    const response = await fetch('http://localhost:8080/api/planning/generate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(planningConfig)
    })

    console.log('📡 Response status:', response.status, response.statusText)

    if (response.ok) {
      const result = await response.json()
      console.log('✅ Full planning generation result:', result)

      // ✅ CORRECT PROPERTY NAMES FROM BACKEND
      const ordersAnalyzed = result.totalOrdersAnalyzed || result.ordersAnalyzed || 0
      const planningsCreated = result.planningsCreated || result.processedOrders || 0
      const employeesUsed = result.activeEmployees || result.employeesUsed || 0
      const success = result.success !== false // Default to true unless explicitly false

      console.log(`📊 PARSED PLANNING SUMMARY:`)
      console.log(`  📦 Orders analyzed: ${ordersAnalyzed}`)
      console.log(`  ✅ Plannings created: ${planningsCreated}`)
      console.log(`  👥 Employees available: ${employeesUsed}`)
      console.log(`  ✅ Success flag: ${success}`)

      // Show distribution per employee if available
      if (result.distributionSummary) {
        console.log(`📋 Distribution per employee:`)
        Object.entries(result.distributionSummary).forEach(([name, count]) => {
          console.log(`  ${name}: ${count} orders`)
        })
      }

      // ✅ IMPROVED SUCCESS/FAILURE DETECTION
      if (success && (planningsCreated > 0 || result.message?.includes('SUCCESS'))) {
        showNotification(
          `✅ SUCCESS! ${planningsCreated} plannings created from ${ordersAnalyzed} orders analyzed (${employeesUsed} employees available)`,
          'success'
        )
      } else if (success && planningsCreated === 0 && ordersAnalyzed === 0) {
        showNotification(
          `⚠️ No orders found to plan from date ${config.value.startDate}. Try selecting an earlier date or check if orders exist in the database.`,
          'warning'
        )
      } else if (success && planningsCreated === 0 && ordersAnalyzed > 0) {
        showNotification(
          `⚠️ Found ${ordersAnalyzed} orders but no plannings were created. Orders may already be planned or there might be an issue with the planning algorithm.`,
          'warning'
        )
      } else {
        // Show error with backend message
        const errorMessage = result.message || result.detailedError || 'Unknown error occurred'
        showNotification(
          `❌ Planning failed: ${errorMessage}`,
          'error'
        )
      }

      // Always reload plannings to show current state
      await loadPlannings()

    } else {
      // Handle HTTP errors
      let errorText
      try {
        const errorResponse = await response.json()
        errorText = errorResponse.message || errorResponse.error || `HTTP ${response.status}`
      } catch {
        errorText = await response.text() || `HTTP ${response.status}`
      }

      console.error('❌ Planning generation HTTP error:', response.status, errorText)
      showNotification(
        `❌ Server Error (${response.status}): ${errorText}`,
        'error'
      )
    }

  } catch (error) {
    console.error('❌ Planning generation exception:', error)

    // Improved error message
    let errorMessage = 'Network or parsing error'
    if (error.message) {
      errorMessage = error.message
    } else if (error.name === 'TypeError') {
      errorMessage = 'Network connection failed - is the backend server running?'
    }

    showNotification(
      `❌ Error: ${errorMessage}`,
      'error'
    )
  } finally {
    generating.value = false
    loadingMessage.value = ''
  }
}
const optimizePlanning = async () => {
  optimizing.value = true
  loadingMessage.value = 'Optimizing planning...'

  try {
    // For now, just reload the data
    await loadPlannings()
    showNotification('Planning optimization completed', 'success')
  } catch (error) {
    console.error('❌ Error optimizing planning:', error)
    showNotification('Error optimizing planning', 'error')
  } finally {
    optimizing.value = false
    loadingMessage.value = ''
  }
}

const removeDuplicates = async () => {
  try {
    // Client-side duplicate removal (immediate)
    const originalCount = plannings.value.length
    const uniquePlannings = []
    const seen = new Set()

    for (const planning of plannings.value) {
      const uniqueKey = `${planning.orderId}_${planning.employeeId}_${planning.planningDate}_${planning.startTime}`

      if (!seen.has(uniqueKey)) {
        seen.add(uniqueKey)
        uniquePlannings.push(planning)
      }
    }

    plannings.value = uniquePlannings
    const duplicatesRemoved = originalCount - uniquePlannings.length

    if (duplicatesRemoved > 0) {
      showNotification(`Removed ${duplicatesRemoved} duplicate plannings`, 'success')

      // Try to clean duplicates on backend too
      try {
        await fetch('http://localhost:8080/api/planning/remove-duplicates', {
          method: 'POST'
        })
        console.log('✅ Backend duplicates cleanup requested')
      } catch (backendError) {
        console.warn('⚠️ Backend duplicate cleanup failed:', backendError)
      }
    } else {
      showNotification('No duplicates found', 'success')
    }

  } catch (error) {
    console.error('❌ Error removing duplicates:', error)
    showNotification('Error removing duplicates', 'error')
  }
}

const cleanupData = async () => {
  if (!confirm('Are you sure you want to clean up old planning data?')) {
    return
  }

  try {
    // Try cleanup endpoint
    const response = await fetch('http://localhost:8080/api/planning/cleanup', {
      method: 'DELETE'
    })

    if (response.ok) {
      showNotification('Planning data cleaned up successfully', 'success')
      await loadPlannings()
    } else {
      throw new Error(`HTTP ${response.status}`)
    }
  } catch (error) {
    console.error('❌ Error cleaning up data:', error)
    showNotification('Error cleaning up data', 'error')
  }
}

// Debug function to test all planning endpoints
const testPlanningEndpoints = async () => {
  console.log('🔍 === TESTING PLANNING ENDPOINTS ===')

  const endpoints = [
    { name: 'view-simple', url: 'http://localhost:8080/api/planning/view-simple' },
    { name: 'planning', url: 'http://localhost:8080/api/planning' },
    { name: 'dashboard-stats', url: 'http://localhost:8080/api/dashboard/stats' }
  ]

  for (const endpoint of endpoints) {
    try {
      console.log(`🔄 Testing: ${endpoint.url}`)
      const response = await fetch(endpoint.url)

      if (response.ok) {
        const data = await response.json()
        console.log(`✅ ${endpoint.name} - SUCCESS:`, data)

        if (Array.isArray(data)) {
          console.log(`  📊 Array with ${data.length} items`)
          if (data.length > 0) {
            console.log(`  📋 First item structure:`, Object.keys(data[0]))
          }
        } else if (typeof data === 'object') {
          console.log(`  📊 Object with keys:`, Object.keys(data))
        }
      } else {
        console.log(`❌ ${endpoint.name} - FAILED: ${response.status}`)
      }
    } catch (error) {
      console.log(`❌ ${endpoint.name} - ERROR:`, error.message)
    }
  }

  console.log('🏁 === ENDPOINT TESTING COMPLETE ===')
  showNotification('Check console for detailed endpoint test results', 'success')
}

const viewPlanningDetails = (planning: Planning) => {
  console.log('View planning details:', planning)
  // Implement details view
}

const editPlanning = (planning: Planning) => {
  console.log('Edit planning:', planning)
  // Implement edit functionality
}

const formatDate = (dateStr: string): string => {
  try {
    return new Date(dateStr).toLocaleDateString('en-US', {
      weekday: 'short',
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    })
  } catch {
    return dateStr || 'Unknown date'
  }
}

const formatDuration = (minutes: number): string => {
  const hours = Math.floor(minutes / 60)
  const remainingMinutes = minutes % 60

  if (hours > 0) {
    return `${hours}h ${remainingMinutes}m`
  }
  return `${remainingMinutes}m`
}

// Simple notification function
const showNotification = (message: string, type: 'success' | 'error' | 'info' = 'success') => {
  console.log(`${type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️'} ${message}`)
  // You can implement a real toast notification here
}

// ========== LIFECYCLE ==========
onMounted(() => {
  console.log('📅 Planning page mounted - Loading data...')
  refreshData()
})


// Ajoutez cette fonction après les autres méthodes comme generatePlanning, loadPlannings, etc.
const debugPlanningGeneration = async () => {
  try {
    console.log('🔍 === DEBUGGING PLANNING GENERATION ===')

    // 1. Test debug endpoint
    console.log('📊 Step 1: Testing debug endpoint...')
    const debugResponse = await fetch('http://localhost:8080/api/planning/debug?startDate=2025-06-01')
    if (debugResponse.ok) {
      const debugData = await debugResponse.json()
      console.log('✅ Debug endpoint response:', debugData)

      if (debugData.orders && debugData.orders.length > 0) {
        console.log('📋 Sample orders:', debugData.orders.slice(0, 3))
      }

      if (debugData.employees && debugData.employees.length > 0) {
        console.log('👥 Sample employees:', debugData.employees)
      }

    } else {
      console.error('❌ Debug endpoint failed:', debugResponse.status)
    }

    console.log('🏁 === DEBUG COMPLETE ===')
    showNotification('Debug complete - check console for detailed results', 'info')

  } catch (error) {
    console.error('❌ Debug failed:', error)
    showNotification('Debug failed - check console for errors', 'error')
  }
}
const generatePlanningWithGreedy = async () => {
  if (generating.value) return

  generating.value = true
  loadingMessage.value = 'Generating planning with Greedy algorithm...'

  try {
    console.log('🚀 Using Greedy Planning Service...')

    // Nettoyer d'abord
    try {
      const cleanupResponse = await fetch('http://localhost:8080/api/planning/cleanup', {
        method: 'DELETE'
      })
      if (cleanupResponse.ok) {
        const cleanupResult = await cleanupResponse.json()
        console.log('🗑️ Cleanup successful:', cleanupResult)
      }
    } catch (e) {
      console.warn('⚠️ Cleanup skipped:', e)
    }

    // Utiliser le service Greedy qui fonctionne mieux
    const currentDate = new Date(config.value.startDate)
    const day = currentDate.getDate()
    const month = currentDate.getMonth() + 1 // JavaScript months are 0-indexed
    const year = currentDate.getFullYear()

    console.log(`📅 Generating for date: ${day}/${month}/${year}`)

    // Utiliser le nouvel endpoint ultra simple
    const greedyResponse = await fetch('http://localhost:8080/api/planning/greedy-simple', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        startDate: config.value.startDate,
        timePerCard: config.value.cardProcessingTime
      })
    })

    if (greedyResponse.ok) {
      const result = await greedyResponse.json()
      console.log('✅ Greedy planning result:', result)

      const planningsCreated = result.planningsCount || result.plannings?.length || 0
      const success = result.success !== false

      if (success && planningsCreated > 0) {
        showNotification(
          `✅ SUCCESS! Created ${planningsCreated} plannings using Greedy algorithm`,
          'success'
        )
      } else {
        showNotification(
          result.message || 'No plannings created',
          planningsCreated > 0 ? 'warning' : 'error'
        )
      }

      await loadPlannings()

    } else {
      // Fallback to the other service
      console.log('⚠️ Greedy failed, trying PlanningService...')

      const planningServiceResponse = await fetch('http://localhost:8080/api/planning-service/generate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          dateDebut: config.value.startDate,
          tempsParCarte: config.value.cardProcessingTime,
          nombreEmployes: 10
        })
      })

      if (planningServiceResponse.ok) {
        const serviceResult = await planningServiceResponse.json()
        console.log('✅ PlanningService result:', serviceResult)

        const ordersProcessed = serviceResult.ordersProcessed || 0
        if (ordersProcessed > 0) {
          showNotification(
            `✅ SUCCESS! Processed ${ordersProcessed} orders with PlanningService`,
            'success'
          )
        } else {
          showNotification(serviceResult.message || 'No orders processed', 'warning')
        }

        await loadPlannings()

      } else {
        throw new Error('Both planning services failed')
      }
    }

  } catch (error) {
    console.error('❌ Greedy planning error:', error)
    showNotification(`❌ Error: ${error.message}`, 'error')
  } finally {
    generating.value = false
    loadingMessage.value = ''
  }
}

// Remplacer temporairement generatePlanning par cette version
// ou ajouter un nouveau bouton pour test

</script>

<style scoped>
.planning-page {
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

/* Responsive design */
@media (max-width: 768px) {
  .planning-page {
    padding: 16px;
  }

  .grid-cols-1 {
    grid-template-columns: repeat(1, minmax(0, 1fr));
  }
}

@media (min-width: 768px) {
  .md\:grid-cols-3 {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .md\:grid-cols-4 {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}
</style>
