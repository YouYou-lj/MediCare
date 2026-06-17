import request from './index'
import type { Result, Medicine, StockRequest } from '../types'

export function listMedicines(keyword?: string) {
  return request.get<any, Result<Medicine[]>>('/medicines', { params: { keyword } })
}

export function getMedicine(id: number) {
  return request.get<any, Result<Medicine>>(`/medicines/${id}`)
}

export function createMedicine(data: Medicine) {
  return request.post<any, Result<Medicine>>('/medicines', data)
}

export function updateMedicine(id: number, data: Medicine) {
  return request.put<any, Result<Medicine>>(`/medicines/${id}`, data)
}

export function deleteMedicine(id: number) {
  return request.delete<any, Result<void>>(`/medicines/${id}`)
}

export function stockIn(id: number, data: StockRequest) {
  return request.post<any, Result<void>>(`/medicines/${id}/stock-in`, data)
}

export function stockOut(id: number, data: StockRequest) {
  return request.post<any, Result<void>>(`/medicines/${id}/stock-out`, data)
}

export function listLowStock() {
  return request.get<any, Result<Medicine[]>>('/medicines/low-stock')
}