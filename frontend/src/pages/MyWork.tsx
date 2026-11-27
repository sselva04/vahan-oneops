import { useEffect, useState } from 'react'
import { api } from '../api'
import { ErrorNote, StatusBadge, errorText, formatDate, formatDateTime, isOverdue } from '../components'
import type { WorkItem } from '../types'

interface WorkListProps {
  items: WorkItem[]
  /** Give both to show the Start / Mark done buttons (the employee screen). Leave out for a read-only list. */
  onStart?: (item: WorkItem) => void
  onComplete?: (item: WorkItem) => void
  busyId?: number | null
}

/** One card per allocated stage. Also used by the admin to look at one person's work. */
export function WorkList({ items, onStart, onComplete, busyId }: WorkListProps) {
  if (items.length === 0) {
    return <div className="empty">Nothing is allocated yet.</div>
  }
  return (
    <div className="work-list">
      {items.map(w => {
        const overdue = isOverdue(w.dueDate, w.status)
        const blocked = w.waitingFor.length > 0
        const busy = busyId === w.stageId
        return (
          <article key={w.stageId} className={`card work ${w.status === 'DONE' ? 'done' : ''}`}>
            <div className="work-head">
              <span className="step">{w.sequenceNo}</span>
              <h3>{w.stageName}</h3>
              <StatusBadge status={w.status} />
            </div>
            <div className="work-part">
              <strong>{w.partDrawingNo}</strong> · {w.partName} · qty {w.quantity}
            </div>
            <div className="muted small">Project {w.projectDrawingNo} · {w.projectName}</div>
            {w.supplierName && <div className="supplier-line">At supplier: <strong>{w.supplierName}</strong></div>}
            <div className="work-foot">
              <span className={overdue ? 'overdue' : 'muted'}>
                Due {formatDate(w.dueDate)}{overdue ? ' (late)' : ''}
              </span>
              {w.status === 'NOT_STARTED' && blocked && (
                <span className="waiting">Waiting for: {w.waitingFor.join(', ')}</span>
              )}
              {w.status === 'IN_PROGRESS' && <span className="muted">Started {formatDateTime(w.startedAt)}</span>}
              {w.status === 'DONE' && <span className="muted">Completed {formatDateTime(w.completedAt)}</span>}
            </div>
            {onStart && onComplete && w.status === 'NOT_STARTED' && (
              <div className="work-actions">
                <button className="primary" disabled={blocked || busy} onClick={() => onStart(w)}>
                  {busy ? 'Starting…' : '▶ Start'}
                </button>
              </div>
            )}
            {onStart && onComplete && w.status === 'IN_PROGRESS' && (
              <div className="work-actions">
                <button className="primary finish" disabled={busy} onClick={() => onComplete(w)}>
                  {busy ? 'Saving…' : '✓ Mark done'}
                </button>
              </div>
            )}
          </article>
        )
      })}
    </div>
  )
}

export default function MyWork() {
  const [items, setItems] = useState<WorkItem[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)

  useEffect(() => {
    api.myWork().then(setItems).catch(e => setError(errorText(e)))
  }, [])

  const act = async (item: WorkItem, action: (id: number) => Promise<WorkItem[]>) => {
    setError(null)
    setBusyId(item.stageId)
    try {
      setItems(await action(item.stageId))
    } catch (e) {
      setError(errorText(e))
      api.myWork().then(setItems).catch(() => {})   // the screen may be out of date; show what is true now
    } finally {
      setBusyId(null)
    }
  }

  const complete = (item: WorkItem) => {
    // Nothing can undo "done" yet, so ask first (a stray tap on a phone is easy).
    if (window.confirm(`Mark "${item.stageName}" on ${item.partDrawingNo} as done?`)) {
      act(item, api.completeStage)
    }
  }

  const open = items?.filter(i => i.status !== 'DONE').length ?? 0

  return (
    <div className="narrow">
      <h1>My work</h1>
      <p className="muted">{items ? `${open} open stage${open === 1 ? '' : 's'}` : ' '}</p>
      <ErrorNote message={error} />
      {items && <WorkList items={items} busyId={busyId}
        onStart={item => act(item, api.startStage)} onComplete={complete} />}
    </div>
  )
}
