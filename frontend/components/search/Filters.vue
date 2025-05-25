<template>
  <div class="bg-white rounded-lg shadow p-5">
    <h3 class="text-lg font-medium text-gray-900 mb-4">{{ $t('search.filters') }}</h3>
    
    <form @submit.prevent="applyFilters">
      <div class="space-y-4">
        <!-- Keyword search -->
        <div>
          <label for="keyword" class="form-label">{{ $t('search.keyword') }}</label>
          <input 
            id="keyword" 
            v-model="filters.keyword" 
            type="text" 
            class="form-input"
            :placeholder="$t('search.keyword_placeholder')"
          >
        </div>
        
        <!-- Location -->
        <div>
          <label for="location" class="form-label">{{ $t('search.location') }}</label>
          <input 
            id="location" 
            v-model="filters.location" 
            type="text" 
            class="form-input"
            :placeholder="$t('search.location_placeholder')"
          >
        </div>
        
        <!-- Remote percentage -->
        <div>
          <label for="remote" class="form-label">
            {{ $t('search.remote_work', { percentage: filters.minRemotePercentage }) }}
          </label>
          <input 
            id="remote" 
            v-model="filters.minRemotePercentage" 
            type="range" 
            min="0" 
            max="100" 
            step="25"
            class="w-full"
          >
          <div class="flex justify-between text-xs text-gray-500">
            <span>0%</span>
            <span>25%</span>
            <span>50%</span>
            <span>75%</span>
            <span>100%</span>
          </div>
        </div>
        
        <!-- Duration -->
        <div>
          <label for="duration" class="form-label">
            {{ $t('search.minimum_duration') }}
          </label>
          <select 
            id="duration" 
            v-model="filters.minDurationMonths" 
            class="form-input"
          >
            <option :value="null">{{ $t('search.any_duration') }}</option>
            <option value="1">{{ $t('search.at_least_month', { n: 1 }) }}</option>
            <option value="3">{{ $t('search.at_least_months', { n: 3 }) }}</option>
            <option value="6">{{ $t('search.at_least_months', { n: 6 }) }}</option>
            <option value="12">{{ $t('search.at_least_months', { n: 12 }) }}</option>
          </select>
        </div>
        
        <!-- Hourly rate -->
        <div>
          <label for="rate" class="form-label">
            {{ $t('search.minimum_hourly_rate', { rate: filters.minHourlyRate || 0 }) }}
          </label>
          <input 
            id="rate" 
            v-model="filters.minHourlyRate" 
            type="range" 
            min="0" 
            max="2000" 
            step="100"
            class="w-full"
          >
          <div class="flex justify-between text-xs text-gray-500">
            <span>0</span>
            <span>500</span>
            <span>1000</span>
            <span>1500</span>
            <span>2000</span>
          </div>
        </div>
        
        <!-- Skills -->
        <div>
          <label class="form-label">{{ $t('search.skills') }}</label>
          <div class="space-y-2">
            <div v-for="(skill, index) in skillsInput" :key="index" class="flex items-center">
              <input 
                v-model="skillsInput[index]"
                type="text" 
                class="form-input flex-grow"
                :placeholder="$t('search.skills_placeholder')"
              >
              <button 
                v-if="index === skillsInput.length - 1"
                type="button" 
                @click="addSkillInput"
                class="ml-2 btn btn-secondary"
              >
                +
              </button>
              <button 
                v-else
                type="button" 
                @click="removeSkillInput(index)"
                class="ml-2 btn btn-secondary"
              >
                -
              </button>
            </div>
          </div>
        </div>
        
        <div class="pt-4 flex space-x-3">
          <button type="submit" class="btn btn-primary flex-grow">
            {{ $t('search.apply_filters') }}
          </button>
          <button 
            type="button" 
            @click="resetFilters"
            class="btn btn-secondary"
          >
            {{ $t('common.reset') }}
          </button>
        </div>
        
        <div v-if="isAuthenticated" class="pt-2">
          <button 
            type="button" 
            @click="saveSearch"
            class="text-primary-600 hover:text-primary-700 text-sm flex items-center"
          >
            <BookmarkIcon class="h-4 w-4 mr-1" />
            {{ $t('search.save_search') }}
          </button>
        </div>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import {BookmarkIcon} from '@heroicons/vue/24/outline'
import {useAuthStore} from '~/stores/auth'
import {useSearchStore} from '~/stores/search'

const authStore = useAuthStore()
const searchStore = useSearchStore()

const props = defineProps({
  initialFilters: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:filters', 'apply', 'reset'])

const filters = ref({
  keyword: '',
  location: '',
  minRemotePercentage: 0,
  minDurationMonths: null,
  minHourlyRate: 0,
  skills: [],
  ...props.initialFilters
})

const skillsInput = ref([''])

// Initialize skillsInput based on initial filters
watch(() => props.initialFilters, (newFilters) => {
  if (newFilters.skills && newFilters.skills.length > 0) {
    skillsInput.value = [...newFilters.skills, '']
  }
}, { immediate: true })

const isAuthenticated = computed(() => authStore.isAuthenticated)

function addSkillInput() {
  skillsInput.value.push('')
}

function removeSkillInput(index: number) {
  skillsInput.value.splice(index, 1)
}

function applyFilters() {
  // Filter out empty skills
  const nonEmptySkills = skillsInput.value
    .filter(skill => skill.trim() !== '')
    .map(skill => skill.trim())
  
  filters.value.skills = nonEmptySkills
  
  emit('update:filters', filters.value)
  emit('apply', filters.value)
}

function resetFilters() {
  filters.value = {
    keyword: '',
    location: '',
    minRemotePercentage: 0,
    minDurationMonths: null,
    minHourlyRate: 0,
    skills: []
  }
  
  skillsInput.value = ['']
  
  emit('update:filters', filters.value)
  emit('reset')
}

function saveSearch() {
  // Open a dialog to name and save the search
  const searchName = prompt('Name this search:')
  if (searchName) {
    searchStore.saveSearch({
      name: searchName,
      criteria: { ...filters.value },
      notificationEnabled: false
    })
  }
}
</script>