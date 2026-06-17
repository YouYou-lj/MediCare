import request from './index'
import type { Result, Schedule } from '../types'

export function listSchedules(date?: string, deptId?: number) {
  return request.get<any, Result<Schedule[]>>('/schedules', { params: { date, deptId } })
}

export function getAvailableSchedules(date: string, deptId?: number) {
  return request.get<any, Result<Schedule[]>>('/schedules/available', { params: { date, deptId } })
}

export function createSchedule(data: Schedule) {
  return request.post<any, Result<Schedule>>('/schedules', data)
}

export function updateSchedule(id: number, data: Schedule) {
  return request.put<any, Result<Schedule>>(`/schedules/${id}`, data)
}

export function deleteSchedule(id: number) {
  return request.delete<any, Result<void>>(`/schedules/${id}`)
}