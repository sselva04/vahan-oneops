import { useCallback, useEffect, useState } from 'react'
import { api } from '../../api'
import { ErrorNote, errorText } from '../../components'

type Batch = { batch: string; rows: Record<string, number> }
type Row = { section: string; label: string; total: number; created: number; skipped: number; problems: string[]; state: 'wait' | 'run' | 'done' }

// Order matters: later sections refer to rows created by earlier ones.
const SECTIONS: { key: string; path: string; label: string; needsBatch: boolean }[] = [
  { key: 'customers', path: 'customers', label: 'Customers', needsBatch: false },
  { key: 'suppliers', path: 'suppliers', label: 'Suppliers', needsBatch: true },
  { key: 'projects', path: 'projects', label: 'Projects', needsBatch: true },
  { key: 'parts', path: 'parts', label: 'Parts', needsBatch: true },
  { key: 'stages', path: 'stages', label: 'Outsourced stages', needsBatch: true },
  { key: 'rejections', path: 'rejections', label: 'Rejections', needsBatch: true },
  { key: 'machineHours', path: 'machine-hours', label: 'Machine hours', needsBatch: true },
  { key: 'tools', path: 'tools', label: 'Tools', needsBatch: true },
  { key: 'rmPurchases', path: 'rm-purchases', label: 'Raw material purchases', needsBatch: true },
  { key: 'expenses', path: 'expenses', label: 'Monthly expenses', needsBatch: true },
]
const CHUNK = 200

/** Dev-only: loads the converted Excel data. Rows that already exist are skipped, and a batch can be removed again. */
export default function ImportData() {
  const [batches, setBatches] = useState<Batch[]>([])
  const [enabled, setEnabled] = useState<boolean | null>(null)
  const [batch, setBatch] = useState('excel-1')
  const [data, setData] = useState<Record<string, unknown[]> | null>(null)
  const [rows, setRows] = useState<Row[]>([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(() => {
    api.importStatus().then(s => { setEnabled(s.enabled); setBatches(s.batches) }).catch(() => setEnabled(false))
  }, [])
  useEffect(refresh, [refresh])

  const onFile = async (f: File | undefined) => {
    setError(null); setData(null); setRows([])
    if (!f) return
    try {
      const json = JSON.parse(await f.text())
      setData(json)
      setRows(SECTIONS.map(s => ({ section: s.key, label: s.label, total: (json[s.key] ?? []).length, created: 0, skipped: 0, problems: [], state: 'wait' })))
    } catch { setError('That file is not a valid import file.') }
  }

  const update = (key: string, patch: Partial<Row>) =>
    setRows(rs => rs.map(r => (r.section === key ? { ...r, ...patch } : r)))

  const run = async () => {
    if (!data) return
    setBusy(true); setError(null)
    try {
      await api.importBegin(batch.trim())
      for (const s of SECTIONS) {
        const list = (data[s.key] ?? []) as unknown[]
        update(s.key, { state: 'run', created: 0, skipped: 0, problems: [] })
        let created = 0, skipped = 0
        const problems: string[] = []
        for (let i = 0; i < list.length; i += CHUNK) {
          const res = await api.importSection(s.path, batch.trim(), list.slice(i, i + CHUNK))
          created += res.created; skipped += res.skipped
          problems.push(...res.problems)
          update(s.key, { created, skipped, problems: problems.slice(0, 40) })
        }
        update(s.key, { state: 'done' })
      }
    } catch (e) { setError(errorText(e)) } finally { setBusy(false); refresh() }
  }

  const remove = async (b: string) => {
    if (!window.confirm(`Remove everything loaded by batch "${b}"? Rows you have since attached other data to are kept.`)) return
    setBusy(true); setError(null)
    try { await api.importRemove(b) } catch (e) { setError(errorText(e)) } finally { setBusy(false); refresh() }
  }

  if (enabled === false) return <div className="page"><h1>Import data</h1><p>Data import is switched off on this server.</p></div>

  return (
    <div className="page">
      <h1>Import data</h1>
      <p className="muted">Test data only. Choose the converted import file, give the load a name, and start. Customers, suppliers, projects and parts that already exist are left as they are.</p>
      <ErrorNote message={error} />
      <div className="card">
        <label>Batch name <input value={batch} onChange={e => setBatch(e.target.value)} disabled={busy} /></label>{' '}
        <input type="file" accept=".json,application/json" onChange={e => onFile(e.target.files?.[0])} disabled={busy} />{' '}
        <button onClick={run} disabled={busy || !data || !batch.trim()}>{busy ? 'Importing…' : 'Start import'}</button>
      </div>

      {rows.length > 0 && (
        <table className="table">
          <thead><tr><th>Section</th><th>In file</th><th>Loaded</th><th>Skipped</th><th></th></tr></thead>
          <tbody>
            {rows.map(r => (
              <tr key={r.section}>
                <td>{r.label}</td><td>{r.total}</td><td>{r.created}</td><td>{r.skipped}</td>
                <td>{r.state === 'run' ? 'Loading…' : r.state === 'done' ? 'Done' : ''}
                  {r.problems.length > 0 && (
                    <details><summary>{r.problems.length} issues</summary>
                      <ul>{r.problems.map((p, i) => <li key={i}>{p}</li>)}</ul></details>
                  )}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <h2>Loaded batches</h2>
      {batches.length === 0 ? <p className="muted">Nothing imported yet.</p> : (
        <table className="table">
          <thead><tr><th>Batch</th><th>Rows loaded</th><th></th></tr></thead>
          <tbody>
            {batches.map(b => (
              <tr key={b.batch}>
                <td>{b.batch}</td>
                <td>{Object.entries(b.rows).map(([t, n]) => `${t} ${n}`).join(', ')}</td>
                <td><button className="danger" onClick={() => remove(b.batch)} disabled={busy}>Remove</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
