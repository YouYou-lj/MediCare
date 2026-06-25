import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { SysUser } from '../types'

export const useUserStore = defineStore('user', () => {
  const currentUser = ref<SysUser | null>(null)
  const isLoggedIn = ref(false)
  const hasSyncedCurrentUser = ref(false)

  function setUser(user: SysUser) {
    currentUser.value = user
    isLoggedIn.value = true
    hasSyncedCurrentUser.value = true
    sessionStorage.setItem('medicare_user', JSON.stringify(user))
  }

  function clearUser() {
    currentUser.value = null
    isLoggedIn.value = false
    hasSyncedCurrentUser.value = true
    sessionStorage.removeItem('medicare_user')
  }

  function loadFromStorage() {
    const saved = sessionStorage.getItem('medicare_user')
    if (saved) {
      try {
        currentUser.value = JSON.parse(saved)
        isLoggedIn.value = true
      } catch {
        clearUser()
      }
    }
  }

  async function syncFromServer(force = false) {
    if (hasSyncedCurrentUser.value && !force) {
      return currentUser.value
    }

    try {
      const response = await fetch('/api/auth/current', {
        credentials: 'include'
      })
      if (!response.ok) {
        clearUser()
        return null
      }

      const result = await response.json()
      if (result?.code === 200 && result.data) {
        setUser(result.data)
        return currentUser.value
      }

      clearUser()
      return null
    } catch {
      hasSyncedCurrentUser.value = true
      return currentUser.value
    }
  }

  function hasRole(...roles: string[]): boolean {
    if (!currentUser.value) return false
    return roles.includes(currentUser.value.role)
  }

  return { currentUser, isLoggedIn, setUser, clearUser, loadFromStorage, syncFromServer, hasRole }
})
