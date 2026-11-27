import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../api'
import { ErrorNote, errorText, formatDate, formatMoney } from '../../components'
import type { RejectionRow, Supplier } from '../../types'

/** Every rejection, newest first, with the debit total. Rejections are recorded on the stage in the project. */
export default function Rejections() {
  const [rows, setRows] = useState<RejectionRow[] | null>(null)
  const [suppliers, setSuppliers] = useState<Supplier[]>([])
  const [supplierId, setSupplierId] = useState<number | ''>('')
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.suppliers(true).then(setSuppliers).catch(e => setError(errorText(e)))
  }, [])
  useEffect(() => {
    api.rejections(supplierId === '' ? undefined : supplierId).then(setRows).catch(e => setError(errorText(e)))
  }, [supplierId])

  const total = (rows ?? []).reduce((sum, r) => sum + r.debitAmount, 0)
  const pieces = (rows ?? []).reduce((sum, r) => sum + r.rejectedQty, 0)

  return (
    <div>
      <h1>Rejections</h1>
      <p className="muted">
        Parts a supplier spoiled and the amount debited to them. To record one, open the project and use
        “Rejections” on the stage given to that supplier.
      </p>
      <ErrorNote message={error} />

      <div className="card filters">
        <select value={supplierId} onChange={e => setSupplierId(e.target.value ? Number(e.target.value) : '')}>
          <option value="">All suppliers</option>
          {suppliers.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
        </select>
        <span className="muted">{rows?.length ?? 0} entries · {pieces} pieces · total debit <strong>{formatMoney(total)}</strong></span>
      </div>

      <div className="card table-wrap">
        <table>
          <thead>
            <tr><th>Date</th><th>Supplier</th><th>Project / part</th><th>Process</th><th>Bill</th><th>Qty</th><th>Debit</th><th>Remarks</th></tr>
          </thead>
          <tbody>
            {rows?.map(r => (
              <tr key={r.id}>
                <td>{r.rejectedOn ? formatDate(r.rejectedOn) : '—'}</td>
                <td>{r.supplierName}</td>
                <td>
                  <Link to={`/admin/projects/${r.projectId}`}>{r.partDrawingNo}</Link>
                  <div className="muted small">{r.partName}</div>
                </td>
                <td>{r.stageName}</td>
                <td>{r.billNo ?? '—'}</td>
                <td>{r.rejectedQty}</td>
                <td>
                  <strong>{formatMoney(r.debitAmount)}</strong>
                  {r.debitManual && <span className="muted small"> (typed)</span>}
                </td>
                <td className="muted small">{r.remarks}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {rows && rows.length === 0 && <div className="empty">No rejections recorded.</div>}
      </div>
    </div>
  )
}
