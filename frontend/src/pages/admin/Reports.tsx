import { useCallback, useEffect, useState } from 'react'
import { api } from '../../api'
import { ErrorNote, errorText, formatMoney } from '../../components'
import type { MonthSummary } from '../../types'

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
const label = (key: string) => `${MONTHS[Number(key.slice(5, 7)) - 1]} ${key.slice(0, 4)}`
const sum = (rows: MonthSummary[], pick: (m: MonthSummary) => number) => rows.reduce((t, m) => t + pick(m), 0)

/** Financial year (April to March) at a glance, like the "VMC Turnover" sheet, with a month to edit and download. */
export default function Reports() {
  const now = new Date()
  const [fyStart, setFyStart] = useState(now.getMonth() >= 3 ? now.getFullYear() : now.getFullYear() - 1)
  const [months, setMonths] = useState<MonthSummary[] | null>(null)
  const [selected, setSelected] = useState<string>(now.toISOString().slice(0, 7))
  const [consumables, setConsumables] = useState('')
  const [salary, setSalary] = useState('')
  const [notes, setNotes] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(() => {
    api.reportMonths(`${fyStart}-04`, 12).then(setMonths).catch(e => setError(errorText(e)))
  }, [fyStart])
  useEffect(load, [load])

  const current = months?.find(m => m.month === selected) ?? null
  useEffect(() => {
    if (current) {
      setConsumables(String(current.consumables)); setSalary(String(current.salary)); setNotes(current.notes ?? '')
    }
  }, [current?.month, current?.consumables, current?.salary, current?.notes])  // eslint-disable-line react-hooks/exhaustive-deps

  const save = async () => {
    setSaving(true); setError(null)
    try {
      await api.saveExpenses(selected, { consumables: Number(consumables) || 0, salary: Number(salary) || 0, notes: notes.trim() || null })
      load()
    } catch (e) { setError(errorText(e)) } finally { setSaving(false) }
  }

  const download = async () => {
    setError(null)
    try { await api.downloadMonthCsv(selected) } catch (e) { setError(errorText(e)) }
  }

  return (
    <div>
      <h1>Monthly report</h1>
      <p className="muted">
        Turnover is the machine-hour amount of the month (before tax). Profit = turnover − consumables − salary,
        the same as the turnover sheet. The other figures are for information only.
      </p>
      <ErrorNote message={error} />

      <div className="card filters">
        <button className="link" onClick={() => setFyStart(fyStart - 1)}>← Previous year</button>
        <strong>April {fyStart} – March {fyStart + 1}</strong>
        <button className="link" onClick={() => setFyStart(fyStart + 1)}>Next year →</button>
      </div>

      <div className="card table-wrap">
        <table>
          <thead>
            <tr><th>Month</th><th>Jobs</th><th>Hours</th><th>Turnover</th><th>Consumables</th><th>Salary</th><th>Expense</th><th>Profit</th></tr>
          </thead>
          <tbody>
            {months?.map(m => (
              <tr key={m.month} className={m.month === selected ? 'selected' : ''} onClick={() => setSelected(m.month)} style={{ cursor: 'pointer' }}>
                <td><strong>{label(m.month)}</strong></td>
                <td>{m.machineEntries}</td>
                <td>{m.machineHours.toFixed(2)}</td>
                <td>{formatMoney(m.turnover)}</td>
                <td>{formatMoney(m.consumables)}</td>
                <td>{formatMoney(m.salary)}</td>
                <td>{formatMoney(m.totalExpense)}</td>
                <td className={m.profit < 0 ? 'rm-over' : ''}><strong>{formatMoney(m.profit)}</strong></td>
              </tr>
            ))}
            {months && (
              <tr>
                <td><strong>Year</strong></td>
                <td>{sum(months, m => m.machineEntries)}</td>
                <td>{sum(months, m => m.machineHours).toFixed(2)}</td>
                <td><strong>{formatMoney(sum(months, m => m.turnover))}</strong></td>
                <td>{formatMoney(sum(months, m => m.consumables))}</td>
                <td>{formatMoney(sum(months, m => m.salary))}</td>
                <td>{formatMoney(sum(months, m => m.totalExpense))}</td>
                <td><strong>{formatMoney(sum(months, m => m.profit))}</strong></td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {current && (
        <div className="card inline-form">
          <h2>{label(current.month)}</h2>
          <div className="grid">
            <label>Consumables (₹)<input type="number" min={0} step="0.01" value={consumables} onChange={e => setConsumables(e.target.value)} /></label>
            <label>Salary (₹)<input type="number" min={0} step="0.01" value={salary} onChange={e => setSalary(e.target.value)} /></label>
          </div>
          <label>Notes<input value={notes} maxLength={500} onChange={e => setNotes(e.target.value)} /></label>
          <p className="rm-preview">
            Turnover {formatMoney(current.turnover)} − expense {formatMoney((Number(consumables) || 0) + (Number(salary) || 0))} ={' '}
            <strong>{formatMoney(current.turnover - (Number(consumables) || 0) - (Number(salary) || 0))}</strong>
            <span className="muted small"> (tax on machine hours: {formatMoney(current.tax)})</span>
          </p>
          <p className="small muted">
            For information, this month: outsourced work sent {formatMoney(current.outsourcedCost)} · supplier debits {formatMoney(current.rejectionDebit)} ·
            tool spend {formatMoney(current.toolSpend)} · raw material bought {formatMoney(current.rmPurchases)}
          </p>
          <div className="builder-actions">
            <button type="button" onClick={download}>Download {label(current.month)} (CSV)</button>
            <span className="spacer" />
            <button className="primary" disabled={saving} onClick={save}>{saving ? 'Saving…' : 'Save expenses'}</button>
          </div>
        </div>
      )}
    </div>
  )
}
