import request from './index'
import type { Result, Prescription, PrescriptionItem } from '../types'

export function listPrescriptions(patientId?: number, today?: boolean) {
  return request.get<any, Result<Prescription[]>>('/prescriptions', { params: { patientId, today } })
}

export function getPrescription(id: number) {
  return request.get<any, Result<Prescription>>(`/prescriptions/${id}`)
}

export function getByRecord(recordId: number) {
  return request.get<any, Result<Prescription>>(`/prescriptions/by-record/${recordId}`)
}

export function createPrescription(data: { prescription: Prescription; items: PrescriptionItem[] }) {
  return request.post<any, Result<Prescription>>('/prescriptions', data)
}

export function dispensePrescription(id: number) {
  return request.put<any, Result<void>>(`/prescriptions/${id}/dispense`)
}

export function cancelPrescription(id: number) {
  return request.put<any, Result<void>>(`/prescriptions/${id}/cancel`)
}