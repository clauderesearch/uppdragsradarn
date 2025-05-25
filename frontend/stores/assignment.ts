import {defineStore} from 'pinia'
import {useAuthStore} from './auth'

interface Assignment {
  id: string
  title: string
  description?: string
  companyName?: string
  location?: string
  remotePercentage?: number
  durationMonths?: number
  startDate?: string
  hourlyRateMin?: number
  hourlyRateMax?: number
  currency?: string
  hoursPerWeek?: number
  skills?: string[]
  applicationDeadline?: string
  applicationUrl?: string
  source?: {
    id: string
    name: string
  }
  active: boolean
  createdAt: string
  updatedAt: string
}

interface AssignmentsState {
  assignments: Assignment[]
  recentAssignments: Assignment[]
  userAssignments: Record<string, Assignment[]>
  currentPage: number
  totalPages: number
  totalItems: number
  loading: boolean
  error: string | null
}

export const useAssignmentStore = defineStore('assignment', {
  state: (): AssignmentsState => ({
    assignments: [],
    recentAssignments: [],
    userAssignments: {
      INTERESTED: [],
      APPLIED: [],
      REJECTED: [],
      ACCEPTED: []
    },
    currentPage: 0,
    totalPages: 0,
    totalItems: 0,
    loading: false,
    error: null
  }),

  actions: {
    // This method is now identical to searchAssignments with an empty keyword
    async fetchAssignments(page = 0, size = 12) {
      return this.searchAssignments({ keyword: '' }, page, size);
    },

    async fetchRecentAssignments(limit = 5) {
      try {
        // Build the URL with query parameters
        const apiBase = useRuntimeConfig().public.apiBase
        const url = new URL(`${apiBase}/assignments`);
        url.searchParams.append('page', '0');
        url.searchParams.append('size', limit.toString());
        url.searchParams.append('sort', 'createdAt,desc');

        const response = await fetch(url.toString(), {
          credentials: 'include' // Include cookies in the request
        });

        if (!response.ok) {
          throw new Error('Failed to fetch recent assignments');
        }

        const data = await response.json();
        this.recentAssignments = data.content || [];

        return data.content;
      } catch (error: any) {
        console.error('Error fetching recent assignments:', error);
        return [];
      }
    },

    async fetchAssignmentById(id: string) {
      try {
        this.loading = true
        this.error = null

        const apiBase = useRuntimeConfig().public.apiBase
        const response = await fetch(`${apiBase}/assignments/${id}`, {
          credentials: 'include' // Include cookies in the request
        })

        if (!response.ok) {
          throw new Error('Failed to fetch assignment details')
        }

        return await response.json()
      } catch (error: any) {
        this.error = error.message
        return null
      } finally {
        this.loading = false
      }
    },

    async searchAssignments(criteria: { keyword: string }, page = 0, size = 12) {
      try {
        this.loading = true
        this.error = null

        // Get the keyword from criteria
        const keyword = criteria.keyword || '';

        // Build the search URL with query parameters
        const apiBase = useRuntimeConfig().public.apiBase
        const searchUrl = new URL(`${apiBase}/assignments`);
        searchUrl.searchParams.append('page', page.toString());
        searchUrl.searchParams.append('size', size.toString());

        // Only add keyword if it's not empty
        if (keyword) {
          searchUrl.searchParams.append('keyword', keyword);
        }

        const response = await fetch(searchUrl.toString(), {
          credentials: 'include' // Include cookies in the request
        });

        if (!response.ok) {
          throw new Error('Failed to search assignments');
        }

        const data = await response.json();
        
        // If it's the first page, replace assignments, otherwise append
        if (page === 0) {
          this.assignments = data.content;
        } else {
          this.assignments = [...this.assignments, ...data.content];
        }
        
        this.currentPage = data.currentPage;
        this.totalPages = data.totalPages;
        this.totalItems = data.totalElements;
        
        return data;
      } catch (error: any) {
        this.error = error.message;
        return null;
      } finally {
        this.loading = false;
      }
    },

    async markAssignmentInterest(assignmentId: string, status: string, notes?: string) {
      const authStore = useAuthStore()

      if (!authStore.isAuthenticated) {
        throw new Error('You must be logged in to mark interest in assignments')
      }

      try {
        const apiBase = useRuntimeConfig().public.apiBase
        const response = await fetch(`${apiBase}/assignments/${assignmentId}/interest`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          credentials: 'include',
          body: JSON.stringify({
            status,
            notes
          })
        })

        if (!response.ok) {
          throw new Error('Failed to update assignment status')
        }

        // Refresh user assignments for this status
        await this.fetchUserAssignmentsByStatus(status)

        return await response.json()
      } catch (error: any) {
        console.error('Error updating assignment status:', error)
        throw error
      }
    },

    async fetchUserAssignmentsByStatus(status: string) {
      const authStore = useAuthStore()
      
      if (!authStore.isAuthenticated || !authStore.user) {
        return []
      }
      
      try {
        const apiBase = useRuntimeConfig().public.apiBase
        const response = await fetch(
          `${apiBase}/assignments/user/status/${status}`,
          {
            credentials: 'include'
          }
        )
        
        if (!response.ok) {
          throw new Error(`Failed to fetch ${status} assignments`)
        }
        
        const data = await response.json()
        this.userAssignments[status] = data.content
        
        return data.content
      } catch (error: any) {
        console.error(`Error fetching ${status} assignments:`, error)
        return []
      }
    }
  }
})