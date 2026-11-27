import type {
  OutsourceInput, ProjectDetail, ProjectSummary, Role, StageMaster, Supplier, SupplierInput, User, WorkItem,
} from './types'

const BASE = import.meta.env.VITE_API_BASE ?? ''
const TOKEN_KEY = 'vahan_token'

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t: string) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

export class ApiError extends Error {
  status: number
  fields: Record<string, string>
  constructor(status: number, message: string, fields: Record<string, string> = {}) {
    super(message)
    this.status = status
    this.fields = fields
  }
}

let onUnauthorized: () => void = () => {}
export const setUnauthorizedHandler = (fn: () => void) => { onUnauthorized = fn }

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const token = tokenStore.get()
  const res = await fetch(`${BASE}/api${path}`, {
    method,
    headers: {
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
  if (res.status === 401 && path !== '/auth/login') {
    onUnauthorized()
    throw new ApiError(401, 'Your session has ended. Please log in again.')
  }
  if (!res.ok) {
    let message = `Request failed (${res.status})`
    let fields: Record<string, string> = {}
    try {
      const err = await res.json()
      message = err.message ?? message
      fields = err.fields ?? {}
    } catch { /* body was not JSON */ }
    throw new ApiError(res.status, message, fields)
  }
  if (res.status === 204) return undefined as T
  return res.json() as Promise<T>
}

const get = <T,>(p: string) => request<T>('GET', p)
const post = <T,>(p: string, b?: unknown) => request<T>('POST', p, b ?? {})
const put = <T,>(p: string, b: unknown) => request<T>('PUT', p, b)
const patch = <T,>(p: string, b: unknown) => request<T>('PATCH', p, b)
const del = <T,>(p: string) => request<T>('DELETE', p)

export const api = {
  login: (username: string, password: string) =>
    post<{ token: string; user: User }>('/auth/login', { username, password }),
  me: () => get<User>('/auth/me'),
  changePassword: (currentPassword: string, newPassword: string) =>
    post<void>('/auth/change-password', { currentPassword, newPassword }),

  myWork: () => get<WorkItem[]>('/my-work'),
  startStage: (stageId: number) => post<WorkItem[]>(`/my-work/${stageId}/start`),
  completeStage: (stageId: number) => post<WorkItem[]>(`/my-work/${stageId}/complete`),
  stageMasters: () => get<StageMaster[]>('/stage-master'),

  // admin
  users: () => get<User[]>('/admin/users'),
  createUser: (u: { username: string; fullName: string; role: Role; password: string }) =>
    post<User>('/admin/users', u),
  updateUser: (id: number, changes: { fullName?: string; role?: Role; active?: boolean }) =>
    patch<User>(`/admin/users/${id}`, changes),
  resetPassword: (id: number, newPassword: string) =>
    post<void>(`/admin/users/${id}/reset-password`, { newPassword }),
  userWork: (id: number) => get<WorkItem[]>(`/admin/users/${id}/work`),

  projects: () => get<ProjectSummary[]>('/admin/projects'),
  project: (id: number) => get<ProjectDetail>(`/admin/projects/${id}`),
  createProject: (drawingNo: string, name: string) =>
    post<ProjectDetail>('/admin/projects', { drawingNo, name }),
  addPart: (projectId: number, p: { drawingNo: string; name: string; quantity: number }) =>
    post<ProjectDetail>(`/admin/projects/${projectId}/parts`, p),
  // A stage is either picked from the list (stageMasterId) or typed in (customName).
  addStages: (partId: number,
    stages: { stageMasterId: number | null; customName: string | null; assigneeId: number | null; dueDate: string | null }[]) =>
    post<ProjectDetail>(`/admin/parts/${partId}/stages`, { stages }),
  updateStage: (stageId: number, assigneeId: number | null, dueDate: string | null) =>
    put<ProjectDetail>(`/admin/stages/${stageId}`, { assigneeId, dueDate }),
  deleteStage: (stageId: number) => del<ProjectDetail>(`/admin/stages/${stageId}`),
  updateOutsourcing: (stageId: number, o: OutsourceInput) =>
    put<ProjectDetail>(`/admin/stages/${stageId}/outsourcing`, o),

  suppliers: (includeInactive = false) =>
    get<Supplier[]>(`/admin/suppliers?includeInactive=${includeInactive}`),
  createSupplier: (s: SupplierInput) => post<Supplier>('/admin/suppliers', s),
  updateSupplier: (id: number, s: SupplierInput) => put<Supplier>(`/admin/suppliers/${id}`, s),
  setSupplierActive: (id: number, active: boolean) => patch<Supplier>(`/admin/suppliers/${id}`, { active }),
  createStageMaster: (name: string) => post<StageMaster>('/admin/stage-master', { name }),
}
