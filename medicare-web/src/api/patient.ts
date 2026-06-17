import request from './index'
import type { Result, Patient } from '../types'

export function listPatients() {
  return request.get<any, Result<Patient[]>>('/patients')
}

export function searchPatients(keyword: string) {
  return request.get<any, Result<Patient[]>>('/patients/search', { params: { keyword } })
}

export function getPatient(id: number) {
  return request.get<any, Result<Patient>>(`/patients/${id}`)
}

export function createPatient(data: Patient) {
  return request.post<any, Result<Patient>>('/patients', data)
}

export function updatePatient(id: number, data: Patient) {
  return request.put<any, Result<Patient>>(`/patients/${id}`, data)
}

export function deletePatient(id: number) {
  return request.delete<any, Result<void>>(`/patients/${id}`)
}