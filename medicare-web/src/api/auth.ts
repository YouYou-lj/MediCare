import request from './index'
import type { Result, LoginRequest, LoginResponse, SysUser } from '../types'

export function login(data: LoginRequest) {
  return request.post<any, Result<LoginResponse>>('/auth/login', data)
}

export function logout() {
  return request.post<any, Result<void>>('/auth/logout')
}

export function getCurrentUser() {
  return request.get<any, Result<SysUser>>('/auth/current')
}