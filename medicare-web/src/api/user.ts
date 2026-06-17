import request from './index'
import type { Result, SysUser } from '../types'

export function listUsers() {
  return request.get<any, Result<SysUser[]>>('/users')
}

export function createUser(data: SysUser) {
  return request.post<any, Result<SysUser>>('/users', data)
}

export function updateUser(id: number, data: SysUser) {
  return request.put<any, Result<SysUser>>(`/users/${id}`, data)
}

export function deleteUser(id: number) {
  return request.delete<any, Result<void>>(`/users/${id}`)
}

export function updatePassword(id: number, data: { oldPassword: string; newPassword: string }) {
  return request.put<any, Result<void>>(`/users/${id}/password`, data)
}

export function getDashboardStats() {
  return request.get<any, Result<{ todayRegCount: number; waitingCount: number; stockAlertCount: number }>>('/dashboard/stats')
}