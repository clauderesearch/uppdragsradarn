<template>
  <div class="crawler-job-manager">
    <h2 class="text-xl font-bold mb-4">Crawler Job Management</h2>
    
    <!-- Control Panel -->
    <div class="mb-6 p-4 bg-white rounded shadow">
      <h3 class="text-lg font-medium mb-3">Actions</h3>
      <div class="flex space-x-4">
        <button
          @click="startAllCrawlers"
          class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
          :disabled="isStartingAllJobs"
        >
          {{ isStartingAllJobs ? 'Starting...' : 'Start All Crawlers' }}
        </button>
        
        <div class="relative">
          <select
            v-model="selectedSource"
            class="appearance-none block w-full px-4 py-2 pr-8 border rounded"
          >
            <option :value="null">Select a source...</option>
            <option v-for="source in sources" :key="source.id" :value="source">
              {{ source.name }}
            </option>
          </select>
          <button
            @click="startSourceCrawler"
            class="ml-2 px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700"
            :disabled="!selectedSource || isStartingSourceJob"
          >
            {{ isStartingSourceJob ? 'Starting...' : 'Start Selected Source' }}
          </button>
        </div>
      </div>
      
      <!-- Status message -->
      <div v-if="statusMessage" :class="['mt-3 p-2 rounded text-sm', statusMessageClass]">
        {{ statusMessage }}
      </div>
    </div>
    
    <!-- Jobs List -->
    <div class="bg-white rounded shadow">
      <h3 class="text-lg font-medium p-4 border-b">Recent Jobs</h3>
      
      <!-- Loading state -->
      <div v-if="loading" class="p-4">
        <p>Loading job data...</p>
      </div>
      
      <!-- Error state -->
      <div v-else-if="error" class="p-4 text-red-600">
        <p>{{ error }}</p>
        <button @click="loadJobs" class="mt-2 text-sm text-blue-600 hover:underline">
          Try again
        </button>
      </div>
      
      <!-- Jobs table -->
      <div v-else-if="jobs.length > 0" class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Job ID
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Source
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Status
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Started At
              </th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Actions
              </th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-for="job in jobs" :key="job.id">
              <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                {{ job.id }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                {{ job.sourceName }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <span 
                  :class="[
                    'px-2 py-1 text-xs rounded-full', 
                    {
                      'bg-yellow-100 text-yellow-800': job.status === 'RUNNING',
                      'bg-green-100 text-green-800': job.status === 'COMPLETED',
                      'bg-red-100 text-red-800': job.status === 'FAILED',
                      'bg-gray-100 text-gray-800': job.status === 'CANCELLED'
                    }
                  ]"
                >
                  {{ job.status }}
                </span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                {{ formatDate(job.startTime) }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm">
                <button 
                  v-if="job.status === 'RUNNING'"
                  @click="cancelJob(job.id)"
                  class="text-red-600 hover:text-red-900"
                >
                  Cancel
                </button>
                <button 
                  @click="viewJobDetails(job.id)"
                  class="ml-2 text-blue-600 hover:text-blue-900"
                >
                  Details
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <!-- Empty state -->
      <div v-else class="p-4 text-center text-gray-500">
        <p>No crawler jobs found.</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useApi } from '~/plugins/api-client'

// State
const jobs = ref<any[]>([])
const sources = ref<any[]>([])
const selectedSource = ref<any>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const isStartingAllJobs = ref(false)
const isStartingSourceJob = ref(false)
const statusMessage = ref<string | null>(null)
const statusMessageClass = ref('text-green-600')

// Get API client
const api = useApi()

// Methods
const loadJobs = async () => {
  loading.value = true
  error.value = null
  
  try {
    // This would be a real API call to get recent crawler jobs
    // const response = await api.adminCrawler.getRecentJobs()
    // jobs.value = response.jobs || []
    
    // For now simulate with mock data
    jobs.value = [
      {
        id: 'job-123',
        sourceName: 'Emagine',
        status: 'COMPLETED',
        startTime: new Date(Date.now() - 3600000),
        endTime: new Date()
      },
      {
        id: 'job-124',
        sourceName: 'ASocietyGroup',
        status: 'RUNNING',
        startTime: new Date(),
        endTime: null
      }
    ]
    
    // Load sources - in real implementation, this would be a separate API call
    sources.value = [
      { id: 'source-1', name: 'Emagine' },
      { id: 'source-2', name: 'ASocietyGroup' },
      { id: 'source-3', name: 'Ework' },
      { id: 'source-4', name: 'Experis' }
    ]
  } catch (err: any) {
    console.error('Error loading jobs:', err)
    error.value = err.message || 'Failed to load crawler jobs'
  } finally {
    loading.value = false
  }
}

// Start all crawler jobs
const startAllCrawlers = async () => {
  isStartingAllJobs.value = true
  statusMessage.value = null
  
  try {
    // In real implementation this would call the API
    // await api.adminCrawler.startScheduledCrawlerJobs()
    console.log('Starting all crawler jobs')
    
    // Set success message
    statusMessage.value = 'All crawler jobs started successfully!'
    statusMessageClass.value = 'bg-green-100 text-green-800'
    
    // Refresh job list after a delay to show new jobs
    setTimeout(() => {
      loadJobs()
    }, 1000)
  } catch (err: any) {
    console.error('Error starting crawler jobs:', err)
    statusMessage.value = `Error starting jobs: ${err.message || 'Unknown error'}`
    statusMessageClass.value = 'bg-red-100 text-red-800'
  } finally {
    isStartingAllJobs.value = false
  }
}

// Start a specific source crawler
const startSourceCrawler = async () => {
  if (!selectedSource.value) return
  
  isStartingSourceJob.value = true
  statusMessage.value = null
  
  try {
    // In real implementation this would call the API
    // await api.adminCrawler.startCrawlerJob(selectedSource.value.id)
    console.log(`Starting crawler job for source: ${selectedSource.value.name}`)
    
    // Set success message
    statusMessage.value = `Crawler job for ${selectedSource.value.name} started successfully!`
    statusMessageClass.value = 'bg-green-100 text-green-800'
    
    // Refresh job list after a delay to show new job
    setTimeout(() => {
      loadJobs()
    }, 1000)
  } catch (err: any) {
    console.error('Error starting source crawler job:', err)
    statusMessage.value = `Error starting job: ${err.message || 'Unknown error'}`
    statusMessageClass.value = 'bg-red-100 text-red-800'
  } finally {
    isStartingSourceJob.value = false
  }
}

// Cancel a running job
const cancelJob = async (jobId: string) => {
  try {
    // In real implementation this would call the API
    // await api.adminCrawler.cancelCrawlerJob(jobId)
    console.log(`Cancelling crawler job: ${jobId}`)
    
    // Update job status locally for immediate feedback
    const job = jobs.value.find(j => j.id === jobId)
    if (job) {
      job.status = 'CANCELLED'
    }
    
    // Set success message
    statusMessage.value = 'Job cancelled successfully'
    statusMessageClass.value = 'bg-blue-100 text-blue-800'
  } catch (err: any) {
    console.error('Error cancelling job:', err)
    statusMessage.value = `Error cancelling job: ${err.message || 'Unknown error'}`
    statusMessageClass.value = 'bg-red-100 text-red-800'
  }
}

// View job details
const viewJobDetails = (jobId: string) => {
  // In a real application, this might navigate to a job details page
  console.log(`View details for job: ${jobId}`)
}

// Format date for display
const formatDate = (date: Date | string | null) => {
  if (!date) return 'N/A'
  const d = new Date(date)
  return d.toLocaleDateString() + ' ' + d.toLocaleTimeString()
}

// Load jobs on component mount
onMounted(() => {
  loadJobs()
})
</script>