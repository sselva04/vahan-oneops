import { ApiError } from './api'
import { STATUS_LABEL, type StageStatus } from './types'

export function errorText(e: unknown): string {
  return e instanceof ApiError || e instanceof Error ? e.message : 'Something went wrong'
}

export function ErrorNote({ message }: { message: string | null }) {
  return message ? <div className="error" role="alert">{message}</div> : null
}

export function StatusBadge({ status }: { status: StageStatus }) {
  return <span className={`badge status-${status.toLowerCase()}`}>{STATUS_LABEL[status]}</span>
}

export function formatDate(iso: string | null): string {
  if (!iso) return '—'
  const d = new Date(iso + 'T00:00:00')
  return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })
}

export function formatDateTime(iso: string | null): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('en-IN', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' })
}

export function isOverdue(due: string | null, status: StageStatus): boolean {
  if (!due || status === 'DONE') return false
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return new Date(due + 'T00:00:00') < today
}
