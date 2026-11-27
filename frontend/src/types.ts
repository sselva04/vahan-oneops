// Shapes returned by the backend. Keep in step with ProjectDtos.java and UserDto.java.

export type Role = 'ADMIN' | 'EMPLOYEE'
export type StageStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'ON_HOLD' | 'DONE'

export interface User {
  id: number
  username: string
  fullName: string
  role: Role
  active: boolean
}

export interface StageMaster {
  id: number
  name: string
}

export interface Stage {
  id: number
  stageMasterId: number
  stageName: string
  sequenceNo: number
  assigneeId: number | null
  assigneeName: string | null
  status: StageStatus
  dueDate: string | null
  startedAt: string | null
  completedAt: string | null
  // set when the stage is given to a supplier
  supplierId: number | null
  supplierName: string | null
  jobQty: number | null
  ratePerUnit: number | null
  totalCost: number | null
  preInspectionDone: boolean
  finalInspectionDone: boolean
  remarks: string | null
}

export interface Part {
  id: number
  drawingNo: string
  name: string
  quantity: number
  stages: Stage[]
}

export interface ProjectDetail {
  id: number
  drawingNo: string
  name: string
  createdAt: string
  parts: Part[]
}

export interface ProjectSummary {
  id: number
  drawingNo: string
  name: string
  createdAt: string
  partCount: number
  stageCount: number
  stagesDone: number
  stagesUnassigned: number
}

export interface WorkItem {
  stageId: number
  projectId: number
  projectDrawingNo: string
  projectName: string
  partId: number
  partDrawingNo: string
  partName: string
  quantity: number
  stageName: string
  sequenceNo: number
  status: StageStatus
  dueDate: string | null
  startedAt: string | null
  completedAt: string | null
  waitingFor: string[]
  supplierName: string | null
}

export interface SupplierProcess {
  id: number
  stageMasterId: number
  stageName: string
  contactPerson: string | null
  contactPhone: string | null
}

export interface Supplier {
  id: number
  name: string
  address: string | null
  mapUrl: string | null
  active: boolean
  processes: SupplierProcess[]
}

export interface SupplierInput {
  name: string
  address: string | null
  mapUrl: string | null
  processes: { stageName: string; contactPerson: string | null; contactPhone: string | null }[]
}

/** Gives a stage to a supplier (supplierId) or back to in-house (supplierId = null). */
export interface OutsourceInput {
  supplierId: number | null
  jobQty: number | null
  ratePerUnit: number | null
  preInspectionDone: boolean
  finalInspectionDone: boolean
  remarks: string | null
}

export const STATUS_LABEL: Record<StageStatus, string> = {
  NOT_STARTED: 'Not started',
  IN_PROGRESS: 'In progress',
  ON_HOLD: 'On hold',
  DONE: 'Done',
}
