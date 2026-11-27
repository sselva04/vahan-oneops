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
  sentOn: string | null
  receivedOn: string | null
  turnaroundDays: number | null
  rejectedQty: number
  rejections: Rejection[]
}

export interface Customer {
  id: number
  name: string
  active: boolean
}

export interface Rejection {
  id: number
  rejectedQty: number
  partCost: number
  machiningCost: number
  debitAmount: number
  debitManual: boolean
  calculatedDebit: number
  billNo: string | null
  rejectedOn: string | null
  remarks: string | null
}

/** debitAmount empty = let the server calculate (qty x part cost - machining cost, minimum 0). */
export interface RejectionInput {
  rejectedQty: number
  partCost: number
  machiningCost: number | null
  debitAmount: number | null
  billNo: string | null
  rejectedOn: string | null
  remarks: string | null
}

export interface RejectionRow {
  id: number
  supplierId: number
  supplierName: string
  projectId: number
  projectDrawingNo: string
  partDrawingNo: string
  partName: string
  stageName: string
  rejectedQty: number
  partCost: number
  machiningCost: number
  debitAmount: number
  debitManual: boolean
  billNo: string | null
  rejectedOn: string | null
  remarks: string | null
}

export type RmShape = 'BLOCK' | 'ROUND' | 'TUBE' | 'MANUAL'

export interface Material {
  id: number
  name: string
  density: number          // g/cm3
  pricePerKg: number | null
  active: boolean
}

export interface MaterialInput {
  name: string
  density: number
  pricePerKg: number | null
}

/** Sizes in mm: BLOCK a=length b=width c=thickness, ROUND a=diameter b=length, TUBE a=outer b=inner c=length. */
export interface RawMaterialInput {
  materialId: number | null
  shape: RmShape | null
  dimA: number | null
  dimB: number | null
  dimC: number | null
  manualWeightKg: number | null
  actualWeightKg: number | null
}

export interface RawMaterial {
  materialId: number
  materialName: string
  shape: RmShape
  dimA: number | null
  dimB: number | null
  dimC: number | null
  manualWeightKg: number | null
  theoreticalPerPieceKg: number
  theoreticalTotalKg: number
  actualWeightKg: number | null
  differenceKg: number | null
  differencePercent: number | null
  ratePerKg: number | null
  cost: number | null
  costFromActual: boolean
}

export interface Part {
  id: number
  drawingNo: string
  name: string
  quantity: number
  stages: Stage[]
  rawMaterial: RawMaterial | null
}

export interface ProjectDetail {
  id: number
  drawingNo: string
  name: string
  createdAt: string
  parts: Part[]
  rawMaterialCost: number
  customerId: number | null
  customerName: string | null
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
  customerName: string | null
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
  debitTotal: number
  rejectedQty: number
  avgTurnaroundDays: number | null
  jobsReceived: number
}

export interface SupplierInput {
  name: string
  address: string | null
  mapUrl: string | null
  processes: { stageName: string; contactPerson: string | null; contactPhone: string | null }[]
}

/** Gives a stage to a supplier (supplierId) or back to in-house (supplierId = null). */
export interface OutsourceInput {
  sentOn: string | null
  receivedOn: string | null
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
