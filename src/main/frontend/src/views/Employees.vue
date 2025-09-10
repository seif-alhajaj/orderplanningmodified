<template>
  <div class="container mx-auto p-6">
    <!-- Header with Mode Toggle -->
    <div class="flex justify-between items-center mb-6">
      <div>
        <h1 class="text-3xl font-bold text-gray-900">Employee Management & Planning</h1>
        <p class="text-gray-600 mt-1">
          {{ currentView === 'management' ? 'Manage team members and their information' : 'View employee schedules and workload distribution' }}
        </p>
      </div>

      <!-- Mode Toggle -->
      <div class="flex bg-white rounded-lg shadow-sm border p-1">
        <button
          @click="currentView = 'management'"
          :class="[
            'px-4 py-2 rounded-md text-sm font-medium transition-colors',
            currentView === 'management' ? 'bg-blue-600 text-white' : 'text-gray-600 hover:text-gray-900'
          ]"
        >
          Management
        </button>
        <button
          @click="currentView = 'planning'"
          :class="[
            'px-4 py-2 rounded-md text-sm font-medium transition-colors',
            currentView === 'planning' ? 'bg-blue-600 text-white' : 'text-gray-600 hover:text-gray-900'
          ]"
        >
          Planning View
        </button>
      </div>
    </div>

    <!-- Stats Dashboard -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
      <div class="card">
        <div class="flex items-center">
          <div class="bg-blue-500 rounded-lg p-3 mr-4">
            <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path>
            </svg>
          </div>
          <div>
            <p class="text-sm text-gray-600">Total Team</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.total }}</p>
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
            <p class="text-sm text-gray-600">Available</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.available }}</p>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="flex items-center">
          <div class="bg-orange-500 rounded-lg p-3 mr-4">
            <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path>
            </svg>
          </div>
          <div>
            <p class="text-sm text-gray-600">Busy</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.busy }}</p>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="flex items-center">
          <div class="bg-red-500 rounded-lg p-3 mr-4">
            <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>
          </div>
          <div>
            <p class="text-sm text-gray-600">Overloaded</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.overloaded }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Employee List -->
    <div v-if="!selectedEmployeeId">

      <!-- MANAGEMENT VIEW -->
      <div v-if="currentView === 'management'" class="bg-white rounded-lg shadow-md overflow-hidden">
        <div class="px-6 py-4 border-b border-gray-200">
          <div class="flex items-center justify-between">
            <h2 class="text-lg font-semibold text-gray-900">Team Members</h2>
            <button @click="showAddForm = !showAddForm" class="btn-primary">
              {{ showAddForm ? 'Cancel' : '+ Add Employee' }}
            </button>
          </div>
        </div>

        <!-- Add Employee Form -->
        <div v-if="showAddForm" class="p-6 bg-gray-50 border-b">
          <form @submit.prevent="addEmployee" class="grid grid-cols-1 md:grid-cols-4 gap-4">
            <input
              v-model="newEmployee.firstName"
              placeholder="First Name"
              class="input-field"
              required
            >
            <input
              v-model="newEmployee.lastName"
              placeholder="Last Name"
              class="input-field"
              required
            >
            <input
              v-model="newEmployee.email"
              type="email"
              placeholder="Email"
              class="input-field"
              required
            >
            <select
              v-model="newEmployee.role"
              class="input-field"
              required
            >
              <option value="GRADER">Grader</option>
              <option value="CERTIFIER">Certifier</option>
              <option value="GRADER_CERTIFIER">Both</option>
            </select>
            <input
              v-model="newEmployee.workHoursPerDay"
              type="number"
              min="1"
              max="12"
              placeholder="Work Hours/Day"
              class="input-field"
              required
            >
            <div class="flex space-x-2 md:col-span-4">
              <button type="submit" class="btn-primary flex-1">Add</button>
              <button type="button" @click="showAddForm = false" class="btn-secondary">Cancel</button>
            </div>
          </form>
        </div>

        <!-- Management Employee Grid -->
        <div v-if="employees.length > 0" class="p-6">
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <div
              v-for="employee in employees"
              :key="employee.id"
              class="border border-gray-200 rounded-lg p-6 hover:shadow-md transition-shadow cursor-pointer"
            >
              <!-- Employee Header -->
              <div class="flex items-center justify-between mb-4">
                <div class="flex items-center">
                  <div class="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center text-blue-600 font-bold text-lg">
                    {{ getInitials(employee) }}
                  </div>
                  <div class="ml-3">
                    <h3 class="text-lg font-medium text-gray-900">{{ employee.firstName }} {{ employee.lastName }}</h3>
                    <p class="text-sm text-gray-600">{{ employee.email }}</p>
                  </div>
                </div>
                <span :class="[
                  'px-2 py-1 text-xs rounded-full font-medium',
                  employee.status === 'AVAILABLE' ? 'bg-green-100 text-green-800' :
                  employee.status === 'BUSY' ? 'bg-orange-100 text-orange-800' :
                  'bg-gray-100 text-gray-800'
                ]">
                  {{ employee.status || 'AVAILABLE' }}
                </span>
              </div>

              <!-- Employee Stats -->
              <div class="space-y-2 mb-4">
                <div class="flex justify-between text-sm">
                  <span class="text-gray-600">Work Hours:</span>
                  <span class="font-medium">{{ employee.workHoursPerDay || 8 }}h/day</span>
                </div>
                <div class="flex justify-between text-sm">
                  <span class="text-gray-600">Role:</span>
                  <span class="font-medium">{{ formatRole(employee.role) }}</span>
                </div>
                <div class="flex justify-between text-sm">
                  <span class="text-gray-600">Active Orders:</span>
                  <span class="font-medium">{{ employee.activeOrders || 0 }}</span>
                </div>
              </div>

              <!-- Actions -->
              <div class="flex space-x-2">
                <button
                  @click="viewEmployee(employee.id)"
                  class="flex-1 bg-blue-50 text-blue-600 px-3 py-2 rounded text-sm font-medium hover:bg-blue-100"
                >
                  View Details
                </button>
                <button
                  @click="viewEmployeePlanning(employee.id)"
                  class="flex-1 bg-green-50 text-green-600 px-3 py-2 rounded text-sm font-medium hover:bg-green-100"
                >
                  View Planning
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- PLANNING VIEW -->
      <div v-else-if="currentView === 'planning'" class="space-y-6">

        <!-- Planning Controls -->
        <div class="bg-white rounded-lg shadow-sm border p-6">
          <div class="flex justify-between items-center mb-4">
            <h2 class="text-lg font-semibold text-gray-900">Planning Overview</h2>
            <div class="flex space-x-3 items-center">
              <input
                v-model="selectedDate"
                type="date"
                class="border rounded-lg px-3 py-2"
                @change="loadPlanningData"
              >
              <button
                @click="generateUnifiedPlanning"
                :disabled="loading"
                class="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center space-x-2"
              >
                <span v-if="loading">Generating...</span>
                <span v-else>Generate Planning</span>
              </button>
            </div>
          </div>

          <!-- Unified algorithm description -->
          <p class="text-sm text-gray-600">
            Using the same round-robin algorithm as Global Planning for consistent results across both pages.
          </p>
        </div>

        <!-- Planning Employee Grid -->
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <div
            v-for="employee in employeesWithWorkload"
            :key="employee.id"
            class="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow cursor-pointer"
          >
            <div class="p-6">
              <!-- Employee Header -->
              <div class="flex items-center justify-between mb-4">
                <div class="flex items-center">
                  <div class="w-16 h-16 bg-gradient-to-r from-blue-500 to-purple-600 rounded-full flex items-center justify-center">
                    <span class="text-white font-bold text-xl">
                      {{ getInitials(employee) }}
                    </span>
                  </div>
                  <div class="ml-4">
                    <h3 class="text-xl font-bold text-gray-900">{{ employee.firstName }} {{ employee.lastName }}</h3>
                    <p class="text-gray-600">{{ employee.activeOrders || 0 }} active tasks</p>
                  </div>
                </div>

                <span :class="[
                  'px-3 py-1 rounded-full text-sm font-medium',
                  getWorkloadColor(employee.workload || 0)
                ]">
                  {{ getWorkloadStatus(employee.workload || 0) }}
                </span>
              </div>

              <!-- Workload Progress -->
              <div class="mb-4">
                <div class="flex justify-between text-sm mb-2">
                  <span class="text-gray-600">Daily Workload</span>
                  <span class="font-medium">{{ Math.round((employee.workload || 0) * 100) }}%</span>
                </div>
                <div class="w-full bg-gray-200 rounded-full h-3">
                  <div
                    :class="[
                      'h-3 rounded-full transition-all duration-300',
                      getWorkloadProgressColor(employee.workload || 0)
                    ]"
                    :style="{ width: Math.min((employee.workload || 0) * 100, 100) + '%' }"
                  ></div>
                </div>
                <div class="text-xs text-gray-500 mt-1">
                  {{ employee.estimatedHours || 0 }}h / {{ employee.workHoursPerDay || 8 }}h
                </div>
              </div>

              <!-- Role and Action Buttons -->
              <div class="mb-4">
                <div class="flex justify-between text-sm">
                  <span class="text-gray-600">Role:</span>
                  <span class="font-medium">{{ formatRole(employee.role) }}</span>
                </div>
              </div>
              <div class="flex space-x-2">
                <button
                  @click="viewEmployeeOrders(employee.id)"
                  class="flex-1 bg-blue-50 text-blue-600 px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-100 transition-colors"
                >
                  View Orders & Cards
                </button>
                <button
                  class="bg-gray-50 text-gray-600 px-3 py-2 rounded-lg text-sm font-medium hover:bg-gray-100 transition-colors"
                >
                  {{ employee.totalCards || 0 }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-if="employees.length === 0 && !loading" class="bg-white rounded-lg shadow-md p-8 text-center">
        <svg class="mx-auto h-12 w-12 text-gray-400 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z"></path>
        </svg>
        <h3 class="text-lg font-medium text-gray-900 mb-2">No employees found</h3>
        <p class="text-gray-600 mb-4">Start by adding your first team member</p>
        <button @click="showAddForm = true" class="btn-primary">
          + Add First Employee
        </button>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="bg-white rounded-lg shadow-md p-8 text-center">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
        <p class="text-gray-600">Loading employees...</p>
      </div>
    </div>

    <!-- EMPLOYEE DETAIL VIEW -->
    <div v-else>
      <EmployeeDetailPage
        :employeeId="selectedEmployeeId"
        :selectedDate="selectedDate"
        :mode="currentView"
        @back="selectedEmployeeId = null"
        @refresh="refreshEmployees"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import EmployeeDetailPage from '../components/EmployeeDetailPage.vue'

// ========== INTERFACES ==========
interface Employee {
  id: string
  firstName: string
  lastName: string
  email: string
  role: 'GRADER' | 'CERTIFIER' | 'GRADER_CERTIFIER'
  status: 'AVAILABLE' | 'BUSY' | 'OFFLINE'
  workHoursPerDay?: number
  workload?: number
  activeOrders?: number
  estimatedHours?: number
  totalCards?: number
}

interface NewEmployee {
  firstName: string
  lastName: string
  email: string
  role: 'GRADER' | 'CERTIFIER' | 'GRADER_CERTIFIER'
  workHoursPerDay?: number
}

// ========== STATE ==========
const currentView = ref<'management' | 'planning'>('management')
const selectedEmployeeId = ref<string | null>(null)
const selectedDate = ref(new Date().toISOString().split('T')[0])
const loading = ref(false)
const showAddForm = ref(false)

// Employee data
const employees = ref<Employee[]>([])
const newEmployee = ref<NewEmployee>({
  firstName: '',
  lastName: '',
  email: '',
  role: 'GRADER',
  workHoursPerDay: 8
})

// ========== COMPUTED ==========
const stats = computed(() => ({
  total: employees.value.length,
  available: employees.value.filter(e => e.workload && e.workload < 0.8).length,
  busy: employees.value.filter(e => e.workload && e.workload >= 0.8 && e.workload < 1.0).length,
  overloaded: employees.value.filter(e => e.workload && e.workload >= 1.0).length
}))

const employeesWithWorkload = computed(() => {
  return employees.value.map(emp => ({
    ...emp,
    workload: emp.workload || Math.random() * 1.2, // Demo data
    estimatedHours: emp.estimatedHours || Math.round((emp.workload || 0) * (emp.workHoursPerDay || 8)),
    totalCards: emp.totalCards || Math.floor(Math.random() * 200),
    activeOrders: emp.activeOrders || Math.floor(Math.random() * 5)
  }))
})

// ========== METHODS ==========
const loadEmployees = async () => {
  loading.value = true
  try {
    console.log('Loading employees from API...')
    const response = await fetch('/api/employees/active')

    if (response.ok) {
      const data = await response.json()
      console.log('Employees loaded:', data.length, 'employees')

      // Map data for frontend
      employees.value = data.map((emp: any) => ({
        id: emp.id,
        firstName: emp.firstName || emp.first_name,
        lastName: emp.lastName || emp.last_name,
        email: emp.email,
        role: emp.role || 'GRADER',
        status: emp.active ? 'AVAILABLE' : 'OFFLINE',
        workHoursPerDay: emp.workHoursPerDay || emp.work_hours_per_day || 8,
        workload: emp.currentLoad || 0,
        activeOrders: emp.activeOrders || 0,
        estimatedHours: Math.round((emp.currentLoad || 0) * (emp.workHoursPerDay || 8)),
        totalCards: emp.totalCards || 0
      }))
    } else {
      console.error('Failed to load employees:', response.status, response.statusText)
      // Fallback to demo data
      employees.value = generateDemoEmployees()
    }
  } catch (error) {
    console.error('Error loading employees:', error)
    // Fallback to demo data
    employees.value = generateDemoEmployees()
  } finally {
    loading.value = false
  }
}

const generateDemoEmployees = (): Employee[] => {
  return [
    {
      id: '1',
      firstName: 'Jean',
      lastName: 'Martin',
      email: 'jean.martin@pokemon.com',
      role: 'GRADER',
      status: 'AVAILABLE',
      workHoursPerDay: 8,
      workload: 0.3,
      activeOrders: 2,
      estimatedHours: 2,
      totalCards: 67
    },
    {
      id: '2',
      firstName: 'Marie',
      lastName: 'Dupont',
      email: 'marie.dupont@pokemon.com',
      role: 'CERTIFIER',
      status: 'BUSY',
      workHoursPerDay: 8,
      workload: 0.9,
      activeOrders: 4,
      estimatedHours: 7,
      totalCards: 145
    },
    {
      id: '3',
      firstName: 'Pierre',
      lastName: 'Bernard',
      email: 'pierre.bernard@pokemon.com',
      role: 'GRADER_CERTIFIER',
      status: 'AVAILABLE',
      workHoursPerDay: 7,
      workload: 0.6,
      activeOrders: 3,
      estimatedHours: 4,
      totalCards: 89
    }
  ]
}

const loadPlanningData = async () => {
  if (currentView.value === 'planning') {
    try {
      await generateUnifiedPlanning()
    } catch (error) {
      console.error('Error loading planning data:', error)
    }
  }
}

const generateUnifiedPlanning = async () => {
  try {
    loading.value = true

    console.log('Generating unified planning...')

    const response = await fetch('/api/planning/generate-unified', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        startDate: '2025-06-01',
        planningDate: selectedDate.value,
        timePerCard: 3,
        cleanFirst: true
      })
    })

    if (response.ok) {
      const result = await response.json()
      console.log('Unified planning result:', result)

      if (result.success) {
        employees.value = result.employeeAssignments || []

        const totalAssigned = result.totalOrdersAssigned || 0
        const totalAnalyzed = result.totalOrdersAnalyzed || 0

        console.log(`Unified Planning Summary:`)
        console.log(`  Orders assigned: ${totalAssigned}`)
        console.log(`  Orders analyzed: ${totalAnalyzed}`)
        console.log(`  Employees: ${employees.value.length}`)
      }
    } else {
      const errorText = await response.text()
      console.error('Unified planning failed:', errorText)
    }

  } catch (error) {
    console.error('Error generating unified planning:', error)
  } finally {
    loading.value = false
  }
}

const addEmployee = async () => {
  try {
    // Map the role for backend compatibility
    const employeeToSend = {
      ...newEmployee.value,
      active: true
    }

    const response = await fetch('/api/employees', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(employeeToSend)
    })

    if (response.ok) {
      newEmployee.value = { firstName: '', lastName: '', email: '', role: 'GRADER', workHoursPerDay: 8 }
      showAddForm.value = false
      await loadEmployees()
    } else {
      console.error('Failed to add employee:', await response.text())
    }
  } catch (error) {
    console.error('Error adding employee:', error)
  }
}

const viewEmployee = (employeeId: string) => {
  selectedEmployeeId.value = employeeId
}

const viewEmployeePlanning = (employeeId: string) => {
  currentView.value = 'planning'
  selectedEmployeeId.value = employeeId
}

const viewEmployeeOrders = (employeeId: string) => {
  selectedEmployeeId.value = employeeId
}

const refreshEmployees = () => {
  loadEmployees()
  if (currentView.value === 'planning') {
    loadPlanningData()
  }
}

const formatRole = (role: string) => {
  switch(role) {
    case 'GRADER': return 'Grader';
    case 'CERTIFIER': return 'Certifier';
    case 'GRADER_CERTIFIER': return 'Grader & Certifier';
    default: return role;
  }
}

// Helper functions
const getInitials = (employee: Employee) => {
  return `${employee.firstName?.charAt(0) || ''}${employee.lastName?.charAt(0) || ''}`
}

const getWorkloadStatus = (workload: number) => {
  if (workload < 0.5) return 'Available'
  if (workload < 0.8) return 'Moderate'
  if (workload < 1.0) return 'Busy'
  return 'Overloaded'
}

const getWorkloadColor = (workload: number) => {
  if (workload < 0.5) return 'bg-green-100 text-green-800'
  if (workload < 0.8) return 'bg-yellow-100 text-yellow-800'
  if (workload < 1.0) return 'bg-orange-100 text-orange-800'
  return 'bg-red-100 text-red-800'
}

const getWorkloadProgressColor = (workload: number) => {
  if (workload < 0.8) return 'bg-green-500'
  if (workload < 1.0) return 'bg-yellow-500'
  return 'bg-red-500'
}

// ========== LIFECYCLE ==========
onMounted(() => {
  loadEmployees()
})
</script>

<style scoped>
.card {
  @apply bg-white rounded-lg shadow-sm border p-6;
}

.btn-primary {
  @apply bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors;
}

.btn-secondary {
  @apply bg-gray-300 text-gray-700 px-4 py-2 rounded-lg hover:bg-gray-400 transition-colors;
}

.input-field {
  @apply w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500;
}

.transition-colors {
  transition: background-color 0.2s, color 0.2s;
}

.transition-shadow {
  transition: box-shadow 0.2s ease;
}
</style>