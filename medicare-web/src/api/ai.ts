import request from './index'
import type { AiChatRequest, AiChatResponse, Result } from '../types'

export function createAiChat(data: AiChatRequest) {
  return request.post<any, Result<AiChatResponse>>('/ai/chat', data)
}

export interface AiChatStreamCallbacks {
  onChunk: (text: string) => void
  onDone?: (meta: Pick<AiChatResponse, 'provider' | 'model'>) => void
  onError?: (message: string) => void
}

export async function createAiChatStream(data: AiChatRequest, callbacks: AiChatStreamCallbacks) {
  const response = await fetch('/api/ai/chat/stream', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(data)
  })

  if (!response.ok || !response.body) {
    const message = await readErrorMessage(response)
    throw new Error(message)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    buffer = parseSseBuffer(buffer, callbacks)
  }

  buffer += decoder.decode()
  if (buffer.trim()) {
    parseSseBuffer(`${buffer}\n\n`, callbacks)
  }
}

async function readErrorMessage(response: Response) {
  try {
    const result = await response.json()
    return result?.message || 'AI 调用失败'
  } catch {
    if (response.status === 500) {
      return '后端 AI 服务异常，请确认已启动最新后端并加载 application-secret.yml'
    }
    return `AI 调用失败（HTTP ${response.status}）`
  }
}

function parseSseBuffer(buffer: string, callbacks: AiChatStreamCallbacks) {
  const normalized = buffer.replace(/\r\n/g, '\n')
  const parts = normalized.split('\n\n')
  const rest = parts.pop() || ''

  for (const part of parts) {
    const event = parseSseEvent(part)
    if (event.data === '') continue

    if (event.name === 'chunk') {
      callbacks.onChunk(event.data)
    } else if (event.name === 'done') {
      callbacks.onDone?.(parseDoneMeta(event.data))
    } else if (event.name === 'error') {
      callbacks.onError?.(event.data)
      throw new Error(event.data)
    }
  }

  return rest
}

function parseSseEvent(raw: string) {
  let name = 'message'
  const data: string[] = []

  raw.split('\n').forEach((line) => {
    if (!line || line.startsWith(':')) return
    if (line.startsWith('event:')) {
      name = line.slice(6).trim()
    }
    if (line.startsWith('data:')) {
      const value = line.slice(5)
      data.push(value.startsWith(' ') ? value.slice(1) : value)
    }
  })

  return { name, data: data.join('\n') }
}

function parseDoneMeta(data: string): Pick<AiChatResponse, 'provider' | 'model'> {
  try {
    const meta = JSON.parse(data)
    return {
      provider: meta.provider || 'bailian',
      model: meta.model || ''
    }
  } catch {
    return { provider: 'bailian', model: '' }
  }
}
