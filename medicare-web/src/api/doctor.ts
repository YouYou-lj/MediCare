import request from './index'
import type { Result, Doctor } from '../types'

export function listDoctors(deptId?: number) {
  return request.get<any, Result<Doctor[]>>('/doctors', { params: { deptId } })
}

export function getDoctor(id: number) {
  return request.get<any, Result<Doctor>>(`/doctors/${id}`)
}

export function createDoctor(data: Doctor) {
  return request.post<any, Result<Doctor>>('/doctors', data)
}

export function updateDoctor(id: number, data: Doctor) {
  return request.put<any, Result<Doctor>>(`/doctors/${id}`, data)
}

export function deleteDoctor(id: number) {
  return request.delete<any, Result<void>>(`/doctors/${id}`)
}