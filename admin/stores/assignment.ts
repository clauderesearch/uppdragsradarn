import { defineStore } from 'pinia'

interface Assignment {
  id: string
  title: string
  description: string
  companyName: string
  location: string
  needsManualReview: boolean
  piiDetected: string
  active: boolean
  createdAt: string
  updatedAt: string
  source: {
    id: number
    name: string
  }
}

export const useAssignmentStore = defineStore('assignment', {
  state: () => ({
    assignments: [] as Assignment[],
    pendingReview: [] as Assignment[],
    isLoading: false,
    error: null as string | null,
    currentPage: 0,
    totalPages: 0,
    pageSize: 20,
    currentPageAll: 0,
    totalPagesAll: 0
  }),

  actions: {
    async fetchPendingReview(page: number = 0) {
      this.isLoading = true
      this.error = null
      
      try {
        const { public: { apiBase } } = useRuntimeConfig()
        const response = await fetch(`${apiBase}/admin/assignments/pending-review?page=${page}&size=${this.pageSize}`, {
          credentials: 'include',
          headers: {
            'Accept': 'application/json'
          }
        })
        
        if (!response.ok) {
          throw new Error('Failed to fetch pending assignments')
        }
        
        const data = await response.json()
        this.pendingReview = data.content || []
        this.currentPage = data.number || 0
        this.totalPages = data.totalPages || 0
      } catch (error: any) {
        this.error = error.message || 'Failed to fetch pending assignments'
        throw error
      } finally {
        this.isLoading = false
      }
    },

    async approveAssignment(id: string) {
      this.isLoading = true
      this.error = null
      
      try {
        const { public: { apiBase } } = useRuntimeConfig()
        const response = await fetch(`${apiBase}/admin/assignments/${id}/approve`, {
          method: 'POST',
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json'
          }
        })
        
        if (!response.ok) {
          throw new Error('Failed to approve assignment')
        }
        
        // Remove from pending review list
        this.pendingReview = this.pendingReview.filter(a => a.id !== id)
      } catch (error: any) {
        this.error = error.message || 'Failed to approve assignment'
        throw error
      } finally {
        this.isLoading = false
      }
    },

    async updateAssignment(id: string, updates: Partial<Assignment>) {
      this.isLoading = true
      this.error = null
      
      try {
        const { public: { apiBase } } = useRuntimeConfig()
        const response = await fetch(`${apiBase}/admin/assignments/${id}`, {
          method: 'PUT',
          body: JSON.stringify(updates),
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json'
          }
        })
        
        if (!response.ok) {
          throw new Error('Failed to update assignment')
        }
        
        const updated = await response.json()
        
        // Update in pending review list
        const index = this.pendingReview.findIndex(a => a.id === id)
        if (index !== -1) {
          this.pendingReview[index] = updated
        }
        
        return updated
      } catch (error: any) {
        this.error = error.message || 'Failed to update assignment'
        throw error
      } finally {
        this.isLoading = false
      }
    },

    async fetchAllAssignments(page: number = 0) {
      this.isLoading = true
      this.error = null
      
      try {
        const { public: { apiBase } } = useRuntimeConfig()
        const response = await fetch(`${apiBase}/admin/assignments?page=${page}&size=${this.pageSize}`, {
          credentials: 'include',
          headers: {
            'Accept': 'application/json'
          }
        })
        
        if (!response.ok) {
          throw new Error('Failed to fetch assignments')
        }
        
        const data = await response.json()
        this.assignments = data.content || []
        this.currentPageAll = data.number || 0
        this.totalPagesAll = data.totalPages || 0
      } catch (error: any) {
        this.error = error.message || 'Failed to fetch assignments'
        throw error
      } finally {
        this.isLoading = false
      }
    }
  }
})