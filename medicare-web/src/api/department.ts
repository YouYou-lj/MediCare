import request from './index'
import type { Result, Department } from '../types'

export function listDepartments() {
  return request.get<any, Result<Department[]>>('/departments')
}

export function getDepartment(id: number) {
  return request.get<any, Result<Department>>(`/departments/${id}`)
}

export function createDepartment(data: Department) {
  return request.post<any, Result<Department>>('/departments', data)
}

export function updateDepartment(id: number, data: Department) {
  return request.put<any, Result<Department>>(`/departments/${id}`, data)
}

export function deleteDepartment(id: number) {
  return request.delete<any, Result<void>>(`/departments/${id}`)
}