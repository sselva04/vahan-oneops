import type {
  Customer, ExpenseInput, MonthSummary, RmPurchase, RmPurchaseInput, RmPurchaseList, MachineHourInput, MachineHourList, MachineHourEntry, MachineHourSettings, Tool, ToolInput, ToolResharpen, RejectionInput, RejectionRow, Material, MaterialInput, OutsourceInput, RawMaterialInput, ProjectDetail, ProjectSummary, Role, StageMaster, Supplier, SupplierInput, User, WorkItem,
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
  importStatus: () => get<{ enabled: boolean; batches: { batch: string; rows: Record<string, number> }[] }>('/admin/import/status'),
  importBegin: (batch: string) => post<void>(`/admin/import/begin?batch=${encodeURIComponent(batch)}`),
  importSection: (section: string, batch: string, rows: unknown[]) =>
    post<{ created: number; skipped: number; problems: string[] }>(`/admin/import/${section}?batch=${encodeURIComponent(batch)}`, rows),
  importRemove: (batch: string) => del<Record<string, number>>(`/admin/import/batches/${encodeURIComponent(batch)}`),
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
  createProject: (drawingNo: string, name: string, customerId: number | null) =>
    post<ProjectDetail>('/admin/projects', { drawingNo, name, customerId }),
  setProjectCustomer: (projectId: number, customerId: number) =>
    put<ProjectDetail>(`/admin/projects/${projectId}/customer`, { customerId }),

  machineHourSettings: () => get<MachineHourSettings>('/admin/machine-hour-settings'),
  saveMachineHourSettings: (s: MachineHourSettings) => put<MachineHourSettings>('/admin/machine-hour-settings', s),
  machineHours: (month?: string, projectId?: number) => {
    const q = new URLSearchParams()
    if (month) q.set('month', month)
    if (projectId) q.set('projectId', String(projectId))
    const qs = q.toString()
    return get<MachineHourList>(`/admin/machine-hours${qs ? `?${qs}` : ''}`)
  },
  addMachineHour: (e: MachineHourInput) => post<MachineHourEntry>('/admin/machine-hours', e),
  updateMachineHour: (id: number, e: MachineHourInput) => put<MachineHourEntry>(`/admin/machine-hours/${id}`, e),
  deleteMachineHour: (id: number) => del<void>(`/admin/machine-hours/${id}`),

  tools: (status?: string) => get<Tool[]>(`/admin/tools${status ? `?status=${status}` : ''}`),
  addTools: (t: ToolInput) => post<Tool[]>('/admin/tools', t),
  updateTool: (id: number, t: ToolInput) => put<Tool>(`/admin/tools/${id}`, t),
  deleteTool: (id: number) => del<void>(`/admin/tools/${id}`),
  addResharpen: (toolId: number, r: Pick<ToolResharpen, 'resharpenedOn' | 'cost'>) =>
    post<Tool>(`/admin/tools/${toolId}/resharpens`, r),
  deleteResharpen: (id: number) => del<Tool>(`/admin/tools/resharpens/${id}`),

  rmPurchases: (month?: string, projectId?: number, supplierId?: number) => {
    const q = new URLSearchParams()
    if (month) q.set('month', month)
    if (projectId) q.set('projectId', String(projectId))
    if (supplierId) q.set('supplierId', String(supplierId))
    const qs = q.toString()
    return get<RmPurchaseList>(`/admin/rm-purchases${qs ? `?${qs}` : ''}`)
  },
  addRmPurchase: (p: RmPurchaseInput) => post<RmPurchase>('/admin/rm-purchases', p),
  updateRmPurchase: (id: number, p: RmPurchaseInput) => put<RmPurchase>(`/admin/rm-purchases/${id}`, p),
  deleteRmPurchase: (id: number) => del<void>(`/admin/rm-purchases/${id}`),

  reportMonths: (from: string, count = 12) => get<MonthSummary[]>(`/admin/reports/months?from=${from}&count=${count}`),
  saveExpenses: (month: string, e: ExpenseInput) => put<MonthSummary>(`/admin/reports/monthly/${month}/expenses`, e),
  /** Downloads the month's CSV (needs the login token, so it can't be a plain link). */
  downloadMonthCsv: async (month: string) => {
    const token = tokenStore.get()
    const res = await fetch(`${BASE}/api/admin/reports/monthly/${month}/csv`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    })
    if (!res.ok) throw new ApiError(res.status, `Download failed (${res.status})`)
    const url = URL.createObjectURL(await res.blob())
    const a = document.createElement('a')
    a.href = url
    a.download = `vahan-${month}.csv`
    a.click()
    URL.revokeObjectURL(url)
  },

  customers: (includeInactive = false) =>
    get<Customer[]>(`/admin/customers?includeInactive=${includeInactive}`),
  createCustomer: (name: string) => post<Customer>('/admin/customers', { name }),
  renameCustomer: (id: number, name: string) => put<Customer>(`/admin/customers/${id}`, { name }),
  setCustomerActive: (id: number, active: boolean) => patch<Customer>(`/admin/customers/${id}`, { active }),

  addRejection: (stageId: number, r: RejectionInput) =>
    post<ProjectDetail>(`/admin/stages/${stageId}/rejections`, r),
  updateRejection: (id: number, r: RejectionInput) => put<ProjectDetail>(`/admin/rejections/${id}`, r),
  deleteRejection: (id: number) => del<ProjectDetail>(`/admin/rejections/${id}`),
  rejections: (supplierId?: number) =>
    get<RejectionRow[]>(supplierId ? `/admin/rejections?supplierId=${supplierId}` : '/admin/rejections'),
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

  materials: (includeInactive = false) =>
    get<Material[]>(`/admin/materials?includeInactive=${includeInactive}`),
  createMaterial: (m: MaterialInput) => post<Material>('/admin/materials', m),
  updateMaterial: (id: number, m: MaterialInput) => put<Material>(`/admin/materials/${id}`, m),
  setMaterialActive: (id: number, active: boolean) => patch<Material>(`/admin/materials/${id}`, { active }),
  updateRawMaterial: (partId: number, r: RawMaterialInput) =>
    put<ProjectDetail>(`/admin/parts/${partId}/raw-material`, r),

  suppliers: (includeInactive = false) =>
    get<Supplier[]>(`/admin/suppliers?includeInactive=${includeInactive}`),
  createSupplier: (s: SupplierInput) => post<Supplier>('/admin/suppliers', s),
  updateSupplier: (id: number, s: SupplierInput) => put<Supplier>(`/admin/suppliers/${id}`, s),
  setSupplierActive: (id: number, active: boolean) => patch<Supplier>(`/admin/suppliers/${id}`, { active }),
  createStageMaster: (name: string) => post<StageMaster>('/admin/stage-master', { name }),
}
