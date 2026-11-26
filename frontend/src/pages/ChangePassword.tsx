import { useState, type FormEvent } from 'react'
import { api } from '../api'
import { ErrorNote, errorText } from '../components'

export default function ChangePassword() {
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setDone(false)
    try {
      await api.changePassword(current, next)
      setCurrent('')
      setNext('')
      setDone(true)
    } catch (err) {
      setError(errorText(err))
    }
  }

  return (
    <div className="narrow">
      <h1>Change password</h1>
      <form className="card stack" onSubmit={submit}>
        <label>Current password
          <input type="password" value={current} onChange={e => setCurrent(e.target.value)} required />
        </label>
        <label>New password (at least 8 characters)
          <input type="password" value={next} onChange={e => setNext(e.target.value)} minLength={8} required />
        </label>
        <ErrorNote message={error} />
        {done && <div className="ok">Password changed.</div>}
        <button className="primary">Change password</button>
      </form>
    </div>
  )
}
