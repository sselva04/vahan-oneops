import { Fragment, useCallback, useEffect, useState, type FormEvent } from 'react'
import { api } from '../../api'
import { ErrorNote, errorText, formatDate, formatMoney } from '../../components'
import type { Part, ProjectSummary, Tool, ToolInput, ToolStatus } from '../../types'

const TYPES = ['Cutter', 'Drill', 'Boring Bar', 'Reamer', 'Insert', 'Tool Holder', 'ER Collet', 'Tap', 'Other']
const LABEL: Record<ToolStatus, string> = { IN_STOCK: 'In stock', ISSUED: 'Issued', BROKEN: 'Broken', SCRAPPED: 'Scrapped' }
const today = () => new Date().toISOString().slice(0, 10)

/** Tool register: stock, issue to the machine, resharpening, breakage and scrapping. */
export default function Tools() {
  const [tools, setTools] = useState<Tool[] | null>(null)
  const [projects, setProjects] = useState<ProjectSummary[]>([])
  const [status, setStatus] = useState<ToolStatus | ''>('')
  const [search, setSearch] = useState('')
  const [error, setError] = useState<string | null>(null)

  const [editing, setEditing] = useState<Tool | 'new' | null>(null)
  const [f, setF] = useState<Record<string, string>>({})
  const [brokenProject, setBrokenProject] = useState<number | ''>('')
  const [brokenPart, setBrokenPart] = useState<number | ''>('')
  const [parts, setParts] = useState<Part[]>([])
  const [saving, setSaving] = useState(false)

  const [sharpening, setSharpening] = useState<number | null>(null)
  const [shDate, setShDate] = useState(today())
  const [shCost, setShCost] = useState('')

  const load = useCallback(() => {
    api.tools(status || undefined).then(setTools).catch(e => setError(errorText(e)))
  }, [status])
  useEffect(load, [load])
  useEffect(() => { api.projects().then(setProjects).catch(e => setError(errorText(e))) }, [])
  useEffect(() => {
    if (brokenProject === '') { setParts([]); return }
    api.project(brokenProject).then(p => setParts(p.parts)).catch(e => setError(errorText(e)))
  }, [brokenProject])

  const set = (k: string) => (e: { target: { value: string } }) => setF(prev => ({ ...prev, [k]: e.target.value }))

  const open = (t: Tool | 'new') => {
    setEditing(t)
    if (t === 'new') {
      setF({ toolType: 'Cutter', description: '', toolSize: '', itemCode: '', cost: '', purchasedOn: today(), issuedOn: '',
        brokenOn: '', scrappedOn: '', brokenReason: '', remarks: '', count: '1' })
      setBrokenProject(''); setBrokenPart('')
    } else {
      setF({ toolType: t.toolType, description: t.description, toolSize: t.toolSize ?? '', itemCode: t.itemCode ?? '',
        cost: String(t.cost), purchasedOn: t.purchasedOn ?? '', issuedOn: t.issuedOn ?? '', brokenOn: t.brokenOn ?? '',
        scrappedOn: t.scrappedOn ?? '', brokenReason: t.brokenReason ?? '', remarks: t.remarks ?? '', count: '1' })
      setBrokenProject(t.brokenProjectId ?? ''); setBrokenPart(t.brokenPartId ?? '')
    }
  }

  const submit = async (ev: FormEvent) => {
    ev.preventDefault()
    const text = (k: string) => f[k]?.trim() || null
    const input: ToolInput = {
      toolType: f.toolType.trim(), description: f.description.trim(), itemCode: text('itemCode'), toolSize: text('toolSize'),
      cost: f.cost?.trim() ? Number(f.cost) : null, purchasedOn: text('purchasedOn'), issuedOn: text('issuedOn'),
      brokenOn: text('brokenOn'), scrappedOn: text('scrappedOn'), brokenReason: text('brokenReason'),
      brokenProjectId: brokenProject === '' ? null : brokenProject, brokenPartId: brokenPart === '' ? null : brokenPart,
      remarks: text('remarks'), count: editing === 'new' ? Number(f.count) || 1 : null,
    }
    setSaving(true); setError(null)
    try {
      if (editing === 'new') await api.addTools(input)
      else if (editing) await api.updateTool(editing.id, input)
      setEditing(null)
      load()
    } catch (e) { setError(errorText(e)) } finally { setSaving(false) }
  }

  const run = async (action: () => Promise<unknown>) => {
    setError(null)
    try { await action(); load() } catch (e) { setError(errorText(e)) }
  }

  const q = search.trim().toLowerCase()
  const shown = (tools ?? []).filter(t => !q || `${t.toolType} ${t.description} ${t.toolSize ?? ''} ${t.itemCode ?? ''}`.toLowerCase().includes(q))
  const spent = shown.reduce((s, t) => s + t.totalCost, 0)
  const counts = shown.reduce<Record<string, number>>((c, t) => ({ ...c, [t.status]: (c[t.status] ?? 0) + 1 }), {})

  return (
    <div>
      <h1>Tools</h1>
      <p className="muted">Each row is one physical tool. Record when it was issued, resharpened, broken or scrapped.</p>
      <ErrorNote message={error} />

      <div className="card filters">
        <select value={status} onChange={e => setStatus(e.target.value as ToolStatus | '')}>
          <option value="">All tools</option>
          {(Object.keys(LABEL) as ToolStatus[]).map(s => <option key={s} value={s}>{LABEL[s]}</option>)}
        </select>
        <input placeholder="Search type, size, code…" value={search} onChange={e => setSearch(e.target.value)} />
        <span className="muted">
          {shown.length} tools
          {(Object.keys(LABEL) as ToolStatus[]).filter(s => counts[s]).map(s => ` · ${counts[s]} ${LABEL[s].toLowerCase()}`).join('')}
          {' · '}spent {formatMoney(spent)} (purchase + resharpening)
        </span>
        <span className="spacer" />
        <button className="primary" onClick={() => open('new')}>+ Add tools</button>
      </div>

      {editing !== null && (
        <form className="card inline-form" onSubmit={submit}>
          <h2>{editing === 'new' ? 'Add tools' : 'Edit tool'}</h2>
          <datalist id="tool-types">{TYPES.map(t => <option key={t} value={t} />)}</datalist>
          <div className="grid">
            <label>Type<input list="tool-types" value={f.toolType} onChange={set('toolType')} maxLength={60} required /></label>
            <label>Description<input value={f.description} onChange={set('description')} maxLength={200} required placeholder="e.g. Ball Nose Endmil" /></label>
            <label>Size<input value={f.toolSize} onChange={set('toolSize')} maxLength={60} placeholder="e.g. 2.5x50" /></label>
            <label>Item code<input value={f.itemCode} onChange={set('itemCode')} maxLength={60} /></label>
            <label>Cost each (₹)<input type="number" min={0} step="0.01" value={f.cost} onChange={set('cost')} /></label>
            <label>Bought on<input type="date" value={f.purchasedOn} onChange={set('purchasedOn')} /></label>
            {editing === 'new' && <label>How many<input type="number" min={1} max={100} value={f.count} onChange={set('count')} /></label>}
          </div>
          <div className="grid">
            <label>Issued on<input type="date" value={f.issuedOn} onChange={set('issuedOn')} /></label>
            <label>Broken on<input type="date" value={f.brokenOn} onChange={set('brokenOn')} /></label>
            <label>Scrapped on<input type="date" value={f.scrappedOn} onChange={set('scrappedOn')} /></label>
          </div>
          {f.brokenOn && (
            <div className="grid">
              <label>Why it broke<input value={f.brokenReason} onChange={set('brokenReason')} maxLength={300} placeholder="e.g. hardness" /></label>
              <label>On project
                <select value={brokenProject} onChange={e => { setBrokenProject(e.target.value ? Number(e.target.value) : ''); setBrokenPart('') }}>
                  <option value="">— not recorded —</option>
                  {projects.map(p => <option key={p.id} value={p.id}>{p.drawingNo} — {p.name}</option>)}
                </select>
              </label>
              <label>On part
                <select value={brokenPart} onChange={e => setBrokenPart(e.target.value ? Number(e.target.value) : '')} disabled={brokenProject === ''}>
                  <option value="">— not recorded —</option>
                  {parts.map(p => <option key={p.id} value={p.id}>{p.drawingNo} — {p.name}</option>)}
                </select>
              </label>
            </div>
          )}
          <label>Remarks<input value={f.remarks} onChange={set('remarks')} maxLength={500} /></label>
          <div className="builder-actions">
            <span className="spacer" />
            <button type="button" className="link" onClick={() => setEditing(null)}>Cancel</button>
            <button className="primary" disabled={saving}>{saving ? 'Saving…' : 'Save'}</button>
          </div>
        </form>
      )}

      <div className="card table-wrap">
        <table>
          <thead>
            <tr><th>Tool</th><th>Bought</th><th>Status</th><th>Resharpened</th><th>Cost</th><th></th></tr>
          </thead>
          <tbody>
            {shown.map(t => (
              <Fragment key={t.id}>
                <tr>
                  <td>
                    <strong>{t.toolType}</strong> {t.toolSize}
                    <div className="muted small">{t.description}{t.itemCode ? ` · ${t.itemCode}` : ''}</div>
                  </td>
                  <td>{formatDate(t.purchasedOn)}</td>
                  <td>
                    {LABEL[t.status]}
                    <div className="muted small">
                      {t.status === 'ISSUED' && `since ${formatDate(t.issuedOn)}`}
                      {t.status === 'BROKEN' && `${formatDate(t.brokenOn)}${t.brokenReason ? ` · ${t.brokenReason}` : ''}`}
                      {t.status === 'BROKEN' && t.brokenProjectDrawingNo && ` · ${t.brokenPartDrawingNo ?? t.brokenProjectDrawingNo}`}
                      {t.status === 'SCRAPPED' && formatDate(t.scrappedOn)}
                    </div>
                  </td>
                  <td>
                    {t.resharpenCount > 0 ? <>{t.resharpenCount}× <span className="muted small">({formatMoney(t.resharpenCost)})</span></> : <span className="muted">—</span>}
                  </td>
                  <td>{formatMoney(t.totalCost)}</td>
                  <td className="actions">
                    {(t.status === 'IN_STOCK' || t.status === 'ISSUED') && (
                      <button className="link" onClick={() => { setSharpening(sharpening === t.id ? null : t.id); setShDate(today()); setShCost('') }}>Resharpen</button>
                    )}
                    <button className="link" onClick={() => open(t)}>Edit</button>
                    <button className="link danger" onClick={() => {
                      if (window.confirm('Delete this tool?')) void run(() => api.deleteTool(t.id))
                    }}>Delete</button>
                  </td>
                </tr>
                {(sharpening === t.id || t.resharpens.length > 0) && (
                  <tr className="detail-row">
                    <td colSpan={6}>
                      <div className="small">
                        {t.resharpens.map((r, i) => (
                          <span key={r.id} style={{ marginRight: 16 }}>
                            #{i + 1} {formatDate(r.resharpenedOn)} · {formatMoney(r.cost)}{' '}
                            <button className="link danger" onClick={() => { void run(() => api.deleteResharpen(r.id)) }}>remove</button>
                          </span>
                        ))}
                        {sharpening === t.id && (
                          <span className="inline-form">
                            <input type="date" value={shDate} onChange={e => setShDate(e.target.value)} />{' '}
                            <input type="number" min={0} step="0.01" placeholder="Cost ₹" value={shCost} onChange={e => setShCost(e.target.value)} />{' '}
                            <button className="primary" onClick={() => {
                              void run(async () => { await api.addResharpen(t.id, { resharpenedOn: shDate || null, cost: Number(shCost) || 0 }); setSharpening(null) })
                            }}>Add</button>{' '}
                            <button className="link" onClick={() => setSharpening(null)}>Cancel</button>
                          </span>
                        )}
                      </div>
                    </td>
                  </tr>
                )}
              </Fragment>
            ))}
          </tbody>
        </table>
        {tools && shown.length === 0 && <div className="empty">No tools.</div>}
      </div>
    </div>
  )
}
