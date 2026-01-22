import { BrowserRouter, NavLink, Route, Routes } from 'react-router-dom'
import './App.css'

function Shell() {
  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="brand">Parallax</div>
        <nav className="nav">
          <NavLink to="/" end>Overview</NavLink>
          <NavLink to="/account">Account</NavLink>
          <NavLink to="/vehicles">Vehicles</NavLink>
          <NavLink to="/query">Query</NavLink>
          <NavLink to="/admin">Admin</NavLink>
        </nav>
      </header>
      <main className="app-main">
        <Routes>
          <Route path="/" element={<OverviewPage />} />
          <Route path="/account" element={<AccountPage />} />
          <Route path="/vehicles" element={<VehiclesPage />} />
          <Route path="/query" element={<QueryPage />} />
          <Route path="/admin" element={<AdminPage />} />
        </Routes>
      </main>
    </div>
  )
}

function OverviewPage() {
  return (
    <section className="card">
      <h1>Parallax Console</h1>
      <p>React migration scaffold. Authentication, vehicles, and admin views will move here.</p>
    </section>
  )
}

function AccountPage() {
  return (
    <section className="card">
      <h2>Account</h2>
      <p>Profile, password, and contact information will be managed here.</p>
    </section>
  )
}

function VehiclesPage() {
  return (
    <section className="card">
      <h2>Vehicles</h2>
      <p>Register and manage vehicles. Admins will see global listings.</p>
    </section>
  )
}

function QueryPage() {
  return (
    <section className="card">
      <h2>Plate Query</h2>
      <p>Text and image-based queries will be wired to the OCR pipeline.</p>
    </section>
  )
}

function AdminPage() {
  return (
    <section className="card">
      <h2>Admin</h2>
      <p>Admin-only views and controls will live under this route.</p>
    </section>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <Shell />
    </BrowserRouter>
  )
}
