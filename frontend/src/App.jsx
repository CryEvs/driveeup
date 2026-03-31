import { useEffect, useMemo, useState } from 'react'
import { BrowserRouter, Link, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import './App.css'

const API_BASE = '/api'

function useAuth() {
  const [token, setToken] = useState(localStorage.getItem('driveeup_token') || '')
  const [user, setUser] = useState(null)
  const [theme, setTheme] = useState(localStorage.getItem('driveeup_theme') || 'light')

  useEffect(() => {
    document.body.className = theme === 'dark' ? 'theme-dark' : 'theme-light'
    localStorage.setItem('driveeup_theme', theme)
  }, [theme])

  async function fetchMe(currentToken = token) {
    if (!currentToken) return
    const res = await fetch(`${API_BASE}/auth/me`, { headers: { Authorization: `Bearer ${currentToken}` } })
    if (res.ok) {
      setUser(await res.json())
    }
  }

  useEffect(() => {
    fetchMe()
  }, [token])

  function saveToken(nextToken) {
    setToken(nextToken)
    if (nextToken) localStorage.setItem('driveeup_token', nextToken)
    else localStorage.removeItem('driveeup_token')
  }

  return { token, user, setUser, saveToken, theme, setTheme, fetchMe }
}

function AuthPage({ mode, onAuth }) {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState('PASSENGER')
  const [error, setError] = useState('')

  async function submit() {
    setError('')
    if (!email.trim() || !password.trim()) {
      setError('Заполни все поля')
      return
    }
    if (mode === 'register' && !role) {
      setError('Выбери роль')
      return
    }

    const endpoint = mode === 'register' ? '/auth/register' : '/auth/login'
    const payload = mode === 'register' ? { email, password, role } : { email, password }

    const res = await fetch(`${API_BASE}${endpoint}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })
    const data = await res.json()

    if (!res.ok || !data.accessToken) {
      setError(data.error || 'Ошибка авторизации')
      return
    }

    onAuth(data.accessToken, data.user)
    navigate('/')
  }

  return (
    <main className="auth-page">
      <section className="auth-card">
        <h1>{mode === 'register' ? 'Регистрация' : 'Авторизация'} DriveeUP</h1>
        <input value={email} onChange={(e) => setEmail(e.target.value)} placeholder="Email" type="email" />
        <input value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Пароль" type="password" />
        {mode === 'register' && (
          <select value={role} onChange={(e) => setRole(e.target.value)}>
            <option value="PASSENGER">Пассажир</option>
            <option value="DRIVER">Водитель</option>
          </select>
        )}
        {error && <p className="error">{error}</p>}
        <button onClick={submit}>{mode === 'register' ? 'Создать аккаунт' : 'Войти'}</button>
        <p className="switch-link">
          {mode === 'register' ? <Link to="/login">Уже есть аккаунт? Войти</Link> : <Link to="/register">Нет аккаунта? Регистрация</Link>}
        </p>
      </section>
    </main>
  )
}

function Sidebar({ user, theme, setTheme, onLogout }) {
  return (
    <aside className="sidebar">
      <div>
        <h2>DriveeUP</h2>
        <nav>
          <Link to="/profile">Профиль</Link>
          <Link to="/battle-pass">Батл пас</Link>
        </nav>
        <div className="coins">DriveeCoin: {user?.driveeCoin ?? 0}</div>
      </div>
      <div className="theme-zone">
        <label>Тема</label>
        <select value={theme} onChange={(e) => setTheme(e.target.value)}>
          <option value="light">Светлая</option>
          <option value="dark">Тёмная</option>
        </select>
        <button className="logout" onClick={onLogout}>Выйти</button>
      </div>
    </aside>
  )
}

function MainPage() {
  return (
    <section className="content-card">
      <h1>Главная</h1>
      <p>Добро пожаловать в DriveeUP. Выбери раздел в боковом меню.</p>
    </section>
  )
}

function ProfilePage({ token, user, setUser }) {
  const [error, setError] = useState('')

  function onFile(file) {
    if (!file) return
    const reader = new FileReader()
    reader.onload = async () => {
      const avatarUrl = String(reader.result || '')
      if (!avatarUrl) return

      const res = await fetch(`${API_BASE}/auth/avatar`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({ avatarUrl })
      })
      const data = await res.json()
      if (!res.ok) {
        setError(data.error || 'Не удалось обновить аватар')
        return
      }
      setError('')
      setUser(data)
    }
    reader.readAsDataURL(file)
  }

  return (
    <section className="content-card">
      <h1>Профиль</h1>
      <div className="profile-block">
        <img className="avatar" src={user?.avatarUrl || 'https://placehold.co/120x120?text=Avatar'} alt="avatar" />
        <div>
          <p>Email: {user?.email}</p>
          <p>Роль: {user?.role}</p>
          <label className="upload-label">
            Изменить аватарку
            <input type="file" accept="image/*" onChange={(e) => onFile(e.target.files?.[0])} />
          </label>
        </div>
      </div>
      {error && <p className="error">{error}</p>}
    </section>
  )
}

function BattlePassPage() {
  return (
    <section className="content-card">
      <h1>Батл пас</h1>
      <p>Раздел в разработке.</p>
    </section>
  )
}

function Dashboard({ auth }) {
  const location = useLocation()
  if (!auth.token) return <Navigate to="/login" replace />

  return (
    <main className="app-layout">
      <Sidebar
        user={auth.user}
        theme={auth.theme}
        setTheme={auth.setTheme}
        onLogout={() => {
          auth.saveToken('')
          auth.setUser(null)
        }}
      />
      <section className="content-wrap" key={location.pathname}>
        <Routes>
          <Route path="/" element={<MainPage />} />
          <Route path="/profile" element={<ProfilePage token={auth.token} user={auth.user} setUser={auth.setUser} />} />
          <Route path="/battle-pass" element={<BattlePassPage />} />
        </Routes>
      </section>
    </main>
  )
}

function RootApp() {
  const auth = useAuth()
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<AuthPage mode="login" onAuth={(token, user) => { auth.saveToken(token); auth.setUser(user) }} />} />
        <Route path="/register" element={<AuthPage mode="register" onAuth={(token, user) => { auth.saveToken(token); auth.setUser(user) }} />} />
        <Route path="*" element={<Dashboard auth={auth} />} />
      </Routes>
    </BrowserRouter>
  )
}

export default RootApp
