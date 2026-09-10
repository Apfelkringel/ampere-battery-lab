import { StrictMode, useEffect, useMemo, useRef, useState } from 'react'
import { createRoot } from 'react-dom/client'
import './styles.css'

const navItems = [
  { id: 'overview', label: 'Overview', icon: 'grid' },
  { id: 'charging', label: 'Charging', icon: 'bolt' },
  { id: 'discharging', label: 'Discharging', icon: 'arrow' },
  { id: 'health', label: 'Battery health', icon: 'heart' },
]

const SESSION_STORAGE_KEY = 'ampere-browser-sessions'
const HISTORY_STORAGE_KEY = 'ampere-browser-history'

function Icon({ name, size = 18 }) {
  const common = { width: size, height: size, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 1.8, strokeLinecap: 'round', strokeLinejoin: 'round', 'aria-hidden': true }
  const paths = {
    grid: <><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></>,
    bolt: <path d="m13 2-9 12h7l-1 8 9-12h-7l1-8Z"/>,
    arrow: <><path d="M12 3v14"/><path d="m6 11 6 6 6-6"/><path d="M5 21h14"/></>,
    heart: <path d="M20.8 8.7c0 5.2-8.8 10.5-8.8 10.5S3.2 13.9 3.2 8.7A4.6 4.6 0 0 1 12 6.4a4.6 4.6 0 0 1 8.8 2.3Z"/>,
    sun: <><circle cx="12" cy="12" r="3.5"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/></>,
    moon: <path d="M20.5 14.3A8.5 8.5 0 0 1 9.7 3.5 8.5 8.5 0 1 0 20.5 14.3Z"/>,
    settings: <><path d="M12 15.2a3.2 3.2 0 1 0 0-6.4 3.2 3.2 0 0 0 0 6.4Z"/><path d="m19.4 15 .1.1a1.7 1.7 0 0 1-2.4 2.4l-.1-.1a1.7 1.7 0 0 0-2.9 1.2v.2a1.7 1.7 0 0 1-3.4 0v-.2a1.7 1.7 0 0 0-2.9-1.2l-.1.1a1.7 1.7 0 0 1-2.4-2.4l.1-.1a1.7 1.7 0 0 0-1.2-2.9H4a1.7 1.7 0 0 1 0-3.4h.2a1.7 1.7 0 0 0 1.2-2.9l-.1-.1a1.7 1.7 0 0 1 2.4-2.4l.1.1a1.7 1.7 0 0 0 2.9-1.2V2a1.7 1.7 0 0 1 3.4 0v.2A1.7 1.7 0 0 0 17 3.4l.1-.1a1.7 1.7 0 0 1 2.4 2.4l-.1.1a1.7 1.7 0 0 0 1.2 2.9h.2a1.7 1.7 0 0 1 0 3.4h-.2a1.7 1.7 0 0 0-1.2 2.9Z"/></>,
    download: <><path d="M12 3v12"/><path d="m7 10 5 5 5-5"/><path d="M4 20h16"/></>,
    chevron: <path d="m9 18 6-6-6-6"/>,
    check: <path d="m5 12 4 4L19 6"/>,
  }
  return <svg {...common}>{paths[name] || paths.grid}</svg>
}

function BatteryGauge({ level, charging }) {
  const radius = 112
  const circumference = 2 * Math.PI * radius
  const dash = circumference * ((level == null ? 0 : level) / 100)
  return (
    <div className="gauge-wrap">
      <svg className="gauge" viewBox="0 0 280 280" role="img" aria-label={level == null ? 'Battery status unavailable' : `${level}% battery`}>
        <circle className="gauge-track" cx="140" cy="140" r={radius} />
        <circle className="gauge-value" cx="140" cy="140" r={radius} style={{ strokeDasharray: `${dash} ${circumference}` }} />
      </svg>
      <div className="gauge-center">
        <div className="gauge-number">{level == null ? '—' : level}<span>{level == null ? '' : '%'}</span></div>
        <div className="gauge-state"><span className={`state-dot ${charging ? 'is-charging' : ''}`} />{level == null ? 'Unavailable' : charging ? 'Charging' : 'On battery'}</div>
      </div>
    </div>
  )
}

function Stat({ icon, label, value, unit, tone }) {
  return <div className="stat-card">
    <div className={`stat-icon ${tone}`}><Icon name={icon} size={17} /></div>
    <div className="stat-copy"><div className="stat-label">{label}</div><div className="stat-value">{value}<small>{unit}</small></div></div>
  </div>
}

function HistoryChart({ range }) {
  const history = range === '7d' ? loadHistory().slice(-12) : loadHistory()
  const points = history.length > 1
    ? history.map((value, index) => `${8 + index * (572 / Math.max(1, history.length - 1))},${126 - Math.max(0, Math.min(100, value)) * 1.1}`).join(' ')
    : ''
  if (!points) return <div className="chart-empty">Battery history will appear here after this browser records more than one reading.</div>
  return <div className="chart-wrap">
    <svg className="history-chart" viewBox="0 0 610 126" preserveAspectRatio="none" role="img" aria-label={`${range === '7d' ? '7 day' : '30 day'} battery level history`}>
      <defs><linearGradient id="chartFill" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stopColor="#c7f36b" stopOpacity=".22"/><stop offset="1" stopColor="#c7f36b" stopOpacity="0"/></linearGradient></defs>
      <path className="chart-grid" d="M0 20H610M0 57H610M0 94H610" />
      <path className="chart-area" d={`M${points.split(' ').map((p) => p).join(' L')} L580 126 L8 126 Z`} />
      <polyline className="chart-line" points={points} />
      <circle className="chart-dot" cx="580" cy="12" r="4" />
    </svg>
    <div className="chart-labels"><span>Mon</span><span>Tue</span><span>Wed</span><span>Thu</span><span>Fri</span><span>Sat</span><span>Sun</span></div>
  </div>
}

function loadSessions() {
  try {
    const values = JSON.parse(window.localStorage.getItem(SESSION_STORAGE_KEY) || '[]')
    return Array.isArray(values) ? values : []
  } catch { return [] }
}

function loadHistory() {
  try {
    const values = JSON.parse(window.localStorage.getItem(HISTORY_STORAGE_KEY) || '[]')
    return Array.isArray(values) ? values.filter((value) => Number.isFinite(value)).map((value) => Math.max(0, Math.min(100, value))) : []
  } catch { return [] }
}

function LiveDetailPage({ page, level, charging, supported, refresh, sessions, onSelectSession }) {
  if (page === 'charging') return <section className="detail-layout">
    <div className="panel detail-hero"><div className="eyebrow">Charging monitor</div><h2>{charging ? 'Charging detected' : 'Not charging'}</h2><p>{supported ? 'This status is read from the browser Battery Status API.' : 'This browser does not expose charging state.'}</p><div className="detail-value">{level == null ? '—' : `${level}%`}</div><button className="primary-button" onClick={refresh}>Refresh battery status <Icon name="check" size={15} /></button></div>
    <div className="detail-stats"><Stat icon="bolt" label="Charging state" value={charging ? 'Active' : 'Idle'} unit="" tone="lime" /><Stat icon="grid" label="Battery level" value={level == null ? '—' : level} unit={level == null ? '' : '%'} tone="blue" /></div>
    <div className="panel detail-note"><div className="eyebrow">What is measured</div><h2>Live device signal</h2><p>Charging state and battery level update when the browser publishes a new reading. Current, temperature, capacity and charge time are not exposed by this web API, so this page does not invent them.</p></div>
  </section>

  if (page === 'discharging') return <section className="detail-layout">
    <div className="panel detail-hero"><div className="eyebrow">Discharge monitor</div><h2>{charging ? 'Device is charging' : 'Running on battery'}</h2><p>{charging ? 'Discharge tracking starts automatically after the charger is removed.' : 'The browser records the live level while this page is open.'}</p><div className="detail-value">{level == null ? '—' : `${level}%`}</div><button className="primary-button" onClick={refresh}>Refresh battery status <Icon name="check" size={15} /></button></div>
    <div className="detail-stats"><Stat icon="arrow" label="Power state" value={charging ? 'Charging' : 'Battery'} unit="" tone="blue" /><Stat icon="grid" label="Recorded sessions" value={sessions.length} unit="" tone="purple" /></div>
    <div className="panel detail-note"><div className="eyebrow">Browser limitation</div><h2>No fake consumption values</h2><p>Android-specific current, app attribution and deep-sleep data require the native app. The browser view reports only values the platform actually provides.</p></div>
  </section>

  if (page === 'health') return <section className="detail-layout">
    <div className="panel detail-hero"><div className="eyebrow">Battery health</div><h2>Health measurement unavailable</h2><p>The browser Battery Status API does not provide design capacity, charge counter or battery health.</p><div className="detail-value">Not available</div><button className="primary-button" onClick={refresh}>Check available data <Icon name="check" size={15} /></button></div>
    <div className="detail-stats"><Stat icon="heart" label="Health estimate" value="—" unit="" tone="lime" /><Stat icon="bolt" label="Design capacity" value="—" unit="" tone="amber" /></div>
    <div className="panel detail-note"><div className="eyebrow">Accurate by design</div><h2>Use the Android app for health</h2><p>The native Ampere app can use Android’s local charge-counter and session data. This browser companion keeps missing measurements explicitly unavailable.</p></div>
  </section>

  return <section className="detail-layout">
    <div className="panel detail-note"><div className="eyebrow">Activity</div><h2>Recorded sessions</h2><p>Sessions are created from real charging-state changes while this browser is open.</p><div className="session-list">{sessions.length === 0 ? <div className="empty-state">No completed sessions recorded on this browser yet.</div> : sessions.map((row) => <button className="session-row" key={row.id} onClick={() => onSelectSession(row)}><div className={`session-icon ${row.accent}`}><Icon name={row.type === 'Charge' ? 'bolt' : 'arrow'} size={15} /></div><div className="session-main"><strong>{row.type}</strong><span>{row.date}</span></div><div className={`session-change ${row.accent}`}>{row.value}</div><div className="session-duration">{row.duration}</div><Icon name="chevron" size={15} /></button>)}</div><button className="primary-button" onClick={() => { const csv = ['date,type,change,duration', ...sessions.map((row) => `${row.date},${row.type},${row.value},${row.duration}`)].join('\\n'); const blob = new Blob([csv], { type: 'text/csv' }); const url = URL.createObjectURL(blob); const anchor = document.createElement('a'); anchor.href = url; anchor.download = 'ampere-sessions.csv'; anchor.click(); URL.revokeObjectURL(url) }}>Export sessions <Icon name="download" size={15} /></button></div>
  </section>
}

function App() {
  const [activePage, setActivePage] = useState('overview')
  const [level, setLevel] = useState(null)
  const [charging, setCharging] = useState(false)
  const [supported, setSupported] = useState(false)
  const [sessions, setSessions] = useState(loadSessions)
  const previousBattery = useRef(null)
  const batteryCleanup = useRef(() => {})
  const batteryRequest = useRef(0)
  const [theme, setTheme] = useState('dark')
  const [range, setRange] = useState('7d')
  const [notice, setNotice] = useState('')

  const readBattery = () => {
    const requestId = ++batteryRequest.current
    batteryCleanup.current()
    batteryCleanup.current = () => {}
    if (!navigator.getBattery) { setSupported(false); setLevel(null); return }
    navigator.getBattery().then((battery) => {
      if (requestId !== batteryRequest.current) return
      const update = () => {
        if (requestId !== batteryRequest.current) return
        const nextLevel = Math.round(battery.level * 100)
        setSupported(true)
        setLevel(nextLevel)
        setCharging(battery.charging)
        const history = loadHistory()
        if (history.at(-1) !== nextLevel) window.localStorage.setItem(HISTORY_STORAGE_KEY, JSON.stringify([...history, nextLevel].slice(-180)))
      }
      update()
      const handleTransition = () => {
        const previous = previousBattery.current
        if (previous && previous.charging !== battery.charging) {
          const change = Math.round((battery.level - previous.level) * 100)
          if (change !== 0) {
            const row = { id: `${Date.now()}-${battery.charging}`, date: new Date().toLocaleString(), type: battery.charging ? 'Charge' : 'Discharge', value: `${change > 0 ? '+' : ''}${change}%`, duration: 'Live', accent: battery.charging ? 'lime' : 'blue' }
            setSessions((current) => { const next = [row, ...current].slice(0, 150); window.localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(next)); return next })
          }
        }
        previousBattery.current = { charging: battery.charging, level: battery.level }
        update()
      }
      battery.addEventListener('chargingchange', handleTransition)
      battery.addEventListener('levelchange', update)
      batteryCleanup.current = () => {
        battery.removeEventListener('chargingchange', handleTransition)
        battery.removeEventListener('levelchange', update)
      }
      previousBattery.current = { charging: battery.charging, level: battery.level }
      const history = loadHistory()
      const currentLevel = Math.round(battery.level * 100)
      if (history.at(-1) !== currentLevel) window.localStorage.setItem(HISTORY_STORAGE_KEY, JSON.stringify([...history, currentLevel].slice(-180)))
    }).catch(() => { setSupported(false); setLevel(null) })
  }

  useEffect(() => {
    readBattery()
    return () => { batteryRequest.current += 1; batteryCleanup.current() }
  }, [])

  useEffect(() => {
    if (!notice) return undefined
    const timer = window.setTimeout(() => setNotice(''), 2800)
    return () => window.clearTimeout(timer)
  }, [notice])

  const pageTitle = useMemo(() => navItems.find((item) => item.id === activePage)?.label || 'Overview', [activePage])
  const displayLevel = level == null ? 0 : level
  const healthText = level == null ? 'Health not available' : 'Health measurement requires Android data'

  const exportData = () => {
    const csv = ['date,type,change,duration', ...sessions.map((row) => `${row.date},${row.type},${row.value},${row.duration}`)].join('\n')
    const blob = new Blob([csv], { type: 'text/csv' })
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = 'ampere-sessions.csv'
    anchor.click()
    URL.revokeObjectURL(url)
    setNotice('Session data exported as CSV.')
  }

  return <div className={`app-shell ${theme}`}>
    <aside className="sidebar">
      <div className="brand"><div className="brand-mark"><Icon name="bolt" size={18} /></div><span>Ampere</span></div>
      <div className="sidebar-rule" />
      <nav className="nav-list" aria-label="Main navigation">
        <div className="nav-kicker">Monitor</div>
        {navItems.map((item) => <button key={item.id} className={`nav-item ${activePage === item.id ? 'active' : ''}`} onClick={() => setActivePage(item.id)}><Icon name={item.icon} size={17} /><span>{item.label}</span>{item.id === 'overview' && <span className="nav-live" />}</button>)}
      </nav>
      <div className="sidebar-bottom">
        <button className="nav-item" onClick={() => setNotice('Settings are saved locally on this device.')}><Icon name="settings" size={17} /><span>Settings</span></button>
        <div className="device-chip"><div className="device-avatar">P</div><div><strong>Pixel 8</strong><span>Android device</span></div><Icon name="chevron" size={15} /></div>
        <div className="free-note">Free forever <span>•</span> Local data only</div>
      </div>
    </aside>

    <main className="main-content">
      <header className="topbar"><div><div className="breadcrumb">Monitor <span>/</span> {pageTitle}</div><h1>{pageTitle}</h1></div><div className="top-actions"><div className="live-status"><span className="pulse-dot" /> {supported ? 'Live' : 'Local'}</div><button className="icon-button" aria-label="Toggle theme" onClick={() => setTheme((value) => value === 'dark' ? 'light' : 'dark')}><Icon name={theme === 'dark' ? 'sun' : 'moon'} size={18} /></button><button className="profile-button" aria-label="Open local settings" onClick={() => setNotice('All browser data stays in local storage on this device.')}>AK</button></div></header>

      {activePage === 'overview' ? <>
        <section className="overview-grid">
          <div className="panel hero-panel">
            <div className="panel-heading"><div><div className="eyebrow">Battery level</div><h2>Live status</h2></div><div className="updated"><span className="state-dot" /> Updated just now</div></div>
            <div className="gauge-row"><BatteryGauge level={level} charging={charging} /><div className="hero-copy"><div className="condition">{healthText}</div><p>{supported ? 'Live readings are supplied by the browser device API.' : 'This browser does not expose a battery status signal.'}</p><div className="capacity-line"><span>Estimated full capacity</span><strong>Not available</strong></div><div className="capacity-bar"><span style={{ width: supported ? `${displayLevel}%` : '0%' }} /></div><div className="capacity-meta"><span>Design capacity is not exposed here</span><span>{level == null ? '—' : `${level}%`}</span></div></div></div>
            <div className="charging-strip"><div className="strip-icon"><Icon name="bolt" size={16} /></div><div className="strip-copy"><strong>{charging ? 'Charging detected' : 'Running on battery'}</strong><span>{supported ? 'Updated from the browser battery signal' : 'Battery signal unavailable'}</span></div><div className="strip-value">{level == null ? '—' : `${level}%`}</div><button className="primary-button compact-action" onClick={() => { readBattery(); setNotice('Battery status refreshed.') }}>Refresh <Icon name="check" size={14} /></button></div>
          </div>
          <div className="stats-grid"><Stat icon="heart" label="Battery health" value="—" unit="" tone="lime" /><Stat icon="sun" label="Battery temperature" value="—" unit="" tone="amber" /><Stat icon="bolt" label="Voltage" value="—" unit="" tone="blue" /><Stat icon="grid" label="Screen-on time" value="—" unit="" tone="purple" /></div>
        </section>

        <section className="lower-grid">
          <div className="panel chart-panel"><div className="panel-heading"><div><div className="eyebrow">Trend</div><h2>{range === '7d' ? '7-day battery level' : '30-day battery level'}</h2></div><div className="range-toggle"><button className={range === '7d' ? 'selected' : ''} onClick={() => setRange('7d')}>7 days</button><button className={range === '30d' ? 'selected' : ''} onClick={() => setRange('30d')}>30 days</button></div></div><HistoryChart range={range} /><div className="chart-foot"><span><i className="legend-line" /> Battery level</span><span>Recorded <strong>{loadHistory().length}</strong></span></div></div>
          <div className="panel sessions-panel"><div className="panel-heading"><div><div className="eyebrow">Activity</div><h2>Recent sessions</h2></div><button className="text-button" onClick={exportData}><Icon name="download" size={15} /> Export</button></div><div className="session-list">{sessions.length === 0 ? <div className="empty-state">No completed sessions recorded yet.</div> : sessions.slice(0, 4).map((row) => <button className="session-row" key={row.id} onClick={() => setNotice(`${row.type} session from ${row.date} selected.`)}><div className={`session-icon ${row.accent}`}><Icon name={row.type === 'Charge' ? 'bolt' : 'arrow'} size={15} /></div><div className="session-main"><strong>{row.type}</strong><span>{row.date}</span></div><div className={`session-change ${row.accent}`}>{row.value}</div><div className="session-duration">{row.duration}</div><Icon name="chevron" size={15} /></button>)}</div><button className="view-all" onClick={() => setActivePage('charging')}>View all activity <Icon name="chevron" size={14} /></button></div>
        </section>
      </> : <LiveDetailPage page={activePage} level={level} charging={charging} supported={supported} refresh={() => { readBattery(); setNotice('Battery status refreshed.') }} sessions={sessions} onSelectSession={(row) => setNotice(`${row.type} session from ${row.date} selected.`)} />}
      <footer className="footer"><span>Ampere Battery Lab</span><span>{supported ? 'Live browser battery signal · local history' : 'No battery signal exposed by this browser'}</span></footer>
    </main>
    {notice && <div className="toast"><Icon name="check" size={16} />{notice}</div>}
  </div>
}

createRoot(document.getElementById('root')).render(<StrictMode><App /></StrictMode>)
