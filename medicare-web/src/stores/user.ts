import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { SysUser } from '../types'

export const useUserStore = defineStore('user', () => {
  const currentUser = ref<SysUser | null>(null)
  const isLoggedIn = ref(false)

  function setUser(user: SysUser) {
    currentUser.value = user
    isLoggedIn.value = true
    sessionStorage.setItem('medicare_user', JSON.stringify(user))
  }

  function clearUser() {
    currentUser.value = null
    isLoggedIn.value = false
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

  function hasRole(...roles: string[]): boolean {
    if (!currentUser.value) return false
    return roles.includes(currentUser.value.role)
  }

  return { currentUser, isLoggedIn, setUser, clearUser, loadFromStorage, hasRole }
})