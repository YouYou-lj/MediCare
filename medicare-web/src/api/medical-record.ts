import request from './index'
import type { Result, MedicalRecord } from '../types'

export function listMedicalRecords(patientId?: number, registrationId?: number) {
  return request.get<any, Result<MedicalRecord[]>>('/medical-records', { params: { patientId, registrationId } })
}

export function getMedicalRecord(id: number) {
  return request.get<any, Result<MedicalRecord>>(`/medical-records/${id}`)
}

export function createMedicalRecord(data: MedicalRecord) {
  return request.post<any, Result<MedicalRecord>>('/medical-records', data)
}

export function updateMedicalRecord(id: number, data: MedicalRecord) {
  return request.put<any, Result<MedicalRecord>>(`/medical-records/${id}`, data)
}