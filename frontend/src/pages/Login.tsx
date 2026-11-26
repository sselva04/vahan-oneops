import { useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth'
import { ErrorNote, errorText } from '../components'

export default function Login() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to="/" replace />

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await login(username, password)
      navigate('/')
    } catch (err) {
      setError(errorText(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login-wrap">
      <form className="card login" onSubmit={submit}>
        <div className="brand big">VAHAN <span>OneOps</span></div>
        <p className="muted">Sign in to see your work</p>
        <label>Username
          <input value={username} onChange={e => setUsername(e.target.value)} autoComplete="username" autoFocus required />
        </label>
        <label>Password
          <input type="password" value={password} onChange={e => setPassword(e.target.value)} autoComplete="current-password" required />
        </label>
        <ErrorNote message={error} />
        <button className="primary" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
      </form>
    </div>
  )
}
