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

// ---------- Release B: machine hours and tools ----------

export interface MachineHourSettings {
  ratePerHour: number
  taxPercent: number
}

export interface MachineHourEntry {
  id: number
  entryDate: string
  projectId: number
  projectDrawingNo: string | null
  projectName: string | null
  customerName: string | null
  childPartId: number | null
  partDrawingNo: string | null
  partName: string | null
  quantity: number
  hardened: boolean
  designMinutes: number
  settingMinutes: number
  runMinutes: number
  idleMinutes: number
  powerCutMinutes: number
  billableHours: number
  ratePerHour: number
  taxPercent: number
  amount: number
  taxAmount: number
  total: number
  remarks: string | null
}

export interface MachineHourTotals {
  billableHours: number
  amount: number
  taxAmount: number
  total: number
}

export interface MachineHourList {
  entries: MachineHourEntry[]
  totals: MachineHourTotals
}

/** Empty minutes count as 0; empty ratePerHour = the default (new entry) or the entry's own rate (edit). */
export interface MachineHourInput {
  entryDate: string
  projectId: number
  childPartId: number | null
  quantity: number
  hardened: boolean
  designMinutes: number | null
  settingMinutes: number | null
  runMinutes: number | null
  idleMinutes: number | null
  powerCutMinutes: number | null
  ratePerHour: number | null
  remarks: string | null
}

export type ToolStatus = 'IN_STOCK' | 'ISSUED' | 'BROKEN' | 'SCRAPPED'

export interface ToolResharpen {
  id: number
  resharpenedOn: string | null
  cost: number
}

export interface Tool {
  id: number
  toolType: string
  description: string
  itemCode: string | null
  toolSize: string | null
  cost: number
  purchasedOn: string | null
  issuedOn: string | null
  brokenOn: string | null
  scrappedOn: string | null
  brokenReason: string | null
  brokenProjectId: number | null
  brokenProjectDrawingNo: string | null
  brokenPartId: number | null
  brokenPartDrawingNo: string | null
  remarks: string | null
  status: ToolStatus
  resharpens: ToolResharpen[]
  resharpenCount: number
  resharpenCost: number
  totalCost: number
}

export interface ToolInput {
  toolType: string
  description: string
  itemCode: string | null
  toolSize: string | null
  cost: number | null
  purchasedOn: string | null
  issuedOn: string | null
  brokenOn: string | null
  scrappedOn: string | null
  brokenReason: string | null
  brokenProjectId: number | null
  brokenPartId: number | null
  remarks: string | null
  count: number | null
}

// ---------- Release C: raw material purchases and monthly report ----------

export interface RmPurchase {
  id: number
  partId: number
  partDrawingNo: string | null
  partName: string | null
  projectId: number | null
  projectDrawingNo: string | null
  customerName: string | null
  materialName: string | null
  supplierId: number
  supplierName: string | null
  weightKg: number
  ratePerKg: number
  totalCost: number
  inDate: string
  outDate: string | null
  remarks: string | null
}

export interface RmPurchaseInput {
  partId: number
  supplierId: number
  weightKg: number
  ratePerKg: number
  inDate: string
  outDate: string | null
  remarks: string | null
}

export interface RmPurchaseList {
  purchases: RmPurchase[]
  totals: { weightKg: number; totalCost: number }
}

/** profit = turnover - (consumables + salary); the last four figures are for information only. */
export interface MonthSummary {
  month: string
  machineEntries: number
  machineHours: number
  turnover: number
  tax: number
  consumables: number
  salary: number
  totalExpense: number
  profit: number
  notes: string | null
  outsourcedCost: number
  rejectionDebit: number
  toolSpend: number
  rmPurchases: number
}

export interface ExpenseInput {
  consumables: number
  salary: number
  notes: string | null
}
