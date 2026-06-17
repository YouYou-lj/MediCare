import request from './index'
import type { Result, Registration } from '../types'

export function listRegistrations(date?: string, status?: number) {
  return request.get<any, Result<Registration[]>>('/registrations', { params: { date, status } })
}

export function register(data: { patientId: number; scheduleId: number }) {
  return request.post<any, Result<Registration>>('/registrations', data)
}

export function callPatient(id: number) {
  return request.put<any, Result<void>>(`/registrations/${id}/call`)
}

export function completeRegistration(id: number) {
  return request.put<any, Result<void>>(`/registrations/${id}/complete`)
}

export function cancelRegistration(id: number) {
  return request.delete<any, Result<void>>(`/registrations/${id}`)
}