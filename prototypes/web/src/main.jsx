import { StrictMode, useEffect, useMemo, useState } from 'react'
import { createRoot } from 'react-dom/client'
import './styles.css'

const navItems = [
  { id: 'overview', label: 'Overview', icon: 'grid' },
  { id: 'charging', label: 'Charging', icon: 'bolt' },
  { id: 'discharging', label: 'Discharging', icon: 'arrow' },
  { id: 'health', label: 'Battery health', icon: 'heart' },
]

const sessions = [
  { date: 'Today, 08:12', type: 'Charge', value: '+24%', duration: '42 min', accent: 'lime' },
  { date: 'Yesterday, 19:41', type: 'Discharge', value: '-31%', duration: '3h 18 min', accent: 'blue' },
  { date: 'Yesterday, 07:18', type: 'Charge', value: '+67%', duration: '1h 46 min', accent: 'lime' },
  { date: 'Mon, 21:05', type: 'Discharge', value: '-18%', duration: '1h 02 min', accent: 'blue' },
]

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
  const dash = circumference * (level / 100)
  return (
    <div className="gauge-wrap">
      <svg className="gauge" viewBox="0 0 280 280" role="img" aria-label={`${level}% battery`}>
        <circle className="gauge-track" cx="140" cy="140" r={radius} />
        <circle className="gauge-value" cx="140" cy="140" r={radius} style={{ strokeDasharray: `${dash} ${circumference}` }} />
      </svg>
      <div className="gauge-center">
        <div className="gauge-number">{level}<span>%</span></div>
        <div className="gauge-state"><span className={`state-dot ${charging ? 'is-charging' : ''}`} />{charging ? 'Charging' : 'On battery'}</div>
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
  const points = range === '7d' ? '8,90 60,70 112,76 164,48 216,56 268,28 320,41 372,21 424,35 476,18 528,27 580,12' : '8,77 62,68 116,74 170,51 224,66 278,45 332,54 386,22 440,40 494,27 548,50 602,32'
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

function App() {
  const [activePage, setActivePage] = useState('overview')
  const [level, setLevel] = useState(82)
  const [charging, setCharging] = useState(true)
  const [theme, setTheme] = useState('dark')
  const [range, setRange] = useState('7d')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    if (!charging) return undefined
    const timer = window.setInterval(() => setLevel((current) => current >= 99 ? 99 : current + 1), 12000)
    return () => window.clearInterval(timer)
  }, [charging])

  useEffect(() => {
    if (!notice) return undefined
    const timer = window.setTimeout(() => setNotice(''), 2800)
    return () => window.clearTimeout(timer)
  }, [notice])

  const pageTitle = useMemo(() => navItems.find((item) => item.id === activePage)?.label || 'Overview', [activePage])
  const healthText = level > 70 ? 'Good condition' : 'Needs attention'

  const toggleCharging = () => {
    setCharging((value) => !value)
    setNotice(charging ? 'Monitoring switched to battery power.' : 'Charging session started.')
  }

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
      <header className="topbar"><div><div className="breadcrumb">Monitor <span>/</span> {pageTitle}</div><h1>{pageTitle}</h1></div><div className="top-actions"><div className="live-status"><span className="pulse-dot" /> Live</div><button className="icon-button" aria-label="Toggle theme" onClick={() => setTheme((value) => value === 'dark' ? 'light' : 'dark')}><Icon name={theme === 'dark' ? 'sun' : 'moon'} size={18} /></button><button className="profile-button" aria-label="Open profile">AK</button></div></header>

      {activePage === 'overview' ? <>
        <section className="overview-grid">
          <div className="panel hero-panel">
            <div className="panel-heading"><div><div className="eyebrow">Battery level</div><h2>Live status</h2></div><div className="updated"><span className="state-dot" /> Updated just now</div></div>
            <div className="gauge-row"><BatteryGauge level={level} charging={charging} /><div className="hero-copy"><div className="condition">{healthText}</div><p>Your battery is performing within its expected range.</p><div className="capacity-line"><span>Estimated full capacity</span><strong>4,138 mAh</strong></div><div className="capacity-bar"><span style={{ width: '92%' }} /></div><div className="capacity-meta"><span>Design capacity 4,500 mAh</span><span>92%</span></div></div></div>
            <div className="charging-strip"><div className="strip-icon"><Icon name="bolt" size={16} /></div><div className="strip-copy"><strong>{charging ? 'Charging slowly' : 'Discharging normally'}</strong><span>{charging ? 'USB-C · 18 W input' : 'Screen on · 4.2 W average draw'}</span></div><div className="strip-value">{charging ? '+1% / 4 min' : '−5% / hour'}</div><button className={`switch ${charging ? 'on' : ''}`} onClick={toggleCharging} aria-label="Toggle charging simulation"><span /></button></div>
          </div>
          <div className="stats-grid"><Stat icon="heart" label="Battery health" value="92" unit="%" tone="lime" /><Stat icon="sun" label="Battery temperature" value="31.4" unit="°C" tone="amber" /><Stat icon="bolt" label="Voltage" value="4.21" unit="V" tone="blue" /><Stat icon="grid" label="Screen-on time" value="5h 42" unit="" tone="purple" /></div>
        </section>

        <section className="lower-grid">
          <div className="panel chart-panel"><div className="panel-heading"><div><div className="eyebrow">Trend</div><h2>{range === '7d' ? '7-day battery level' : '30-day battery level'}</h2></div><div className="range-toggle"><button className={range === '7d' ? 'selected' : ''} onClick={() => setRange('7d')}>7 days</button><button className={range === '30d' ? 'selected' : ''} onClick={() => setRange('30d')}>30 days</button></div></div><HistoryChart range={range} /><div className="chart-foot"><span><i className="legend-line" /> Battery level</span><span>Average <strong>64%</strong></span><span>Best <strong>98%</strong></span></div></div>
          <div className="panel sessions-panel"><div className="panel-heading"><div><div className="eyebrow">Activity</div><h2>Recent sessions</h2></div><button className="text-button" onClick={exportData}><Icon name="download" size={15} /> Export</button></div><div className="session-list">{sessions.map((row) => <button className="session-row" key={`${row.date}-${row.type}`} onClick={() => setNotice(`${row.type} session from ${row.date} selected.`)}><div className={`session-icon ${row.accent}`}><Icon name={row.type === 'Charge' ? 'bolt' : 'arrow'} size={15} /></div><div className="session-main"><strong>{row.type}</strong><span>{row.date}</span></div><div className={`session-change ${row.accent}`}>{row.value}</div><div className="session-duration">{row.duration}</div><Icon name="chevron" size={15} /></button>)}</div><button className="view-all" onClick={() => setActivePage('charging')}>View all activity <Icon name="chevron" size={14} /></button></div>
        </section>
      </> : <section className="page-placeholder panel"><div className="placeholder-icon"><Icon name={navItems.find((item) => item.id === activePage)?.icon || 'grid'} size={25} /></div><div className="eyebrow">Ampere monitor</div><h2>{pageTitle}</h2><p>This view is ready for your live device data. The overview dashboard already runs with local demo readings so you can explore the experience without an account or subscription.</p><button className="primary-button" onClick={() => setActivePage('overview')}>Back to overview <Icon name="chevron" size={15} /></button></section>}
      <footer className="footer"><span>Ampere Battery Lab</span><span>All readings are simulated locally in this demo.</span></footer>
    </main>
    {notice && <div className="toast"><Icon name="check" size={16} />{notice}</div>}
  </div>
}

createRoot(document.getElementById('root')).render(<StrictMode><App /></StrictMode>)
