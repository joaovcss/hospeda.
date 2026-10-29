import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  BedDouble, Bell, CalendarDays, ChevronDown, ChevronRight, History,
  KeyRound, LayoutGrid, LogOut, Menu, Search, Sprout, UserCog, Users, Waves,
} from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { bookingsApi } from '../api/client';
import { USER_TYPE, initials } from '../utils/format';

function navigationFor({ isGuest, can }) {
  if (isGuest) {
    return [
      { to: '/', label: 'Minha estadia', icon: LayoutGrid, end: true },
      { to: '/minhas-reservas', label: 'Minhas reservas', icon: CalendarDays },
    ];
  }
  return [
    { to: '/', label: 'Visão geral', icon: LayoutGrid, end: true },
    can('BOOKING_READ') && { to: '/reservas', label: 'Reservas', icon: CalendarDays, badge: 'pending' },
    can('GUEST_READ') && { to: '/hospedes', label: 'Hóspedes', icon: Users },
    can('ROOM_READ') && { to: '/quartos', label: 'Quartos', icon: BedDouble },
    can('AREA_READ') && { to: '/areas', label: 'Áreas e acessos', icon: Waves },
    can('ACCESS_LOG_READ') && { to: '/historico-acessos', label: 'Histórico de acessos', icon: History },
    can('USER_READ') && { to: '/usuarios', label: 'Equipe', icon: UserCog },
  ].filter(Boolean);
}

const PAGE_TITLES = {
  '/': 'Visão geral',
  '/reservas': 'Reservas',
  '/hospedes': 'Hóspedes',
  '/quartos': 'Quartos',
  '/areas': 'Áreas e acessos',
  '/historico-acessos': 'Histórico de acessos',
  '/usuarios': 'Equipe',
  '/minhas-reservas': 'Minhas reservas',
  '/conta': 'Minha conta',
};

export default function Layout() {
  const auth = useAuth();
  const { user, isGuest, can, logout } = auth;
  const location = useLocation();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const [pendingCount, setPendingCount] = useState(0);
  const [search, setSearch] = useState('');
  const searchRef = useRef(null);

  const nav = navigationFor(auth);
  const pageTitle = isGuest && location.pathname === '/' ? 'Minha estadia' : PAGE_TITLES[location.pathname] || '';

  useEffect(() => {
    setMenuOpen(false);
    setUserMenuOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    if (!can('BOOKING_READ')) return;
    bookingsApi.list({ status: 'PENDING', size: 1 }).then((p) => setPendingCount(p.totalElements)).catch(() => {});
  }, [location.pathname, can]);

  useEffect(() => {
    const onKey = (event) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault();
        searchRef.current?.focus();
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  const submitSearch = (event) => {
    event.preventDefault();
    if (!search.trim()) return;
    navigate(`/hospedes?search=${encodeURIComponent(search.trim())}`);
    setSearch('');
  };

  return (
    <div className={`app ${menuOpen ? 'menu-open' : ''}`}>
      <aside className="sidebar">
        <Link to="/" className="brand">
          <span className="brand-mark"><Sprout size={22} /></span>
          <span className="brand-name">hospeda<span>.</span></span>
        </Link>

        <nav className="nav">
          <span className="nav-section">Principal</span>
          {nav.map(({ to, label, icon: Icon, end, badge }) => (
            <NavLink key={to} to={to} end={end} className="nav-item">
              <Icon size={19} />
              <span>{label}</span>
              {badge === 'pending' && pendingCount > 0 && <span className="nav-badge">{pendingCount}</span>}
            </NavLink>
          ))}
          <hr />
          <NavLink to="/conta" className="nav-item">
            <KeyRound size={19} />
            <span>Minha conta</span>
          </NavLink>
          <button className="nav-item" onClick={logout}>
            <LogOut size={19} />
            <span>Sair</span>
          </button>
        </nav>
      </aside>
      <div className="sidebar-scrim" onClick={() => setMenuOpen(false)} />

      <div className="main">
        <header className="topbar">
          <button className="icon-btn menu-toggle" onClick={() => setMenuOpen(true)} aria-label="Abrir menu"><Menu size={20} /></button>
          <div className="breadcrumb">
            <span>Workspace</span>
            <ChevronRight size={15} />
            <strong>{pageTitle}</strong>
          </div>

          {!isGuest && can('GUEST_READ') && (
            <form className="search" onSubmit={submitSearch}>
              <Search size={17} />
              <input ref={searchRef} value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Buscar hóspede por nome, e-mail ou CPF..." />
              <kbd>Ctrl K</kbd>
            </form>
          )}

          <div className="topbar-right">
            {!isGuest && (
              <button className="icon-btn bell" onClick={() => navigate('/reservas?status=PENDING')} aria-label="Reservas pendentes">
                <Bell size={20} />
                {pendingCount > 0 && <span className="bell-dot" />}
              </button>
            )}
            <div className="user-menu">
              <button className="user-btn" onClick={() => setUserMenuOpen((open) => !open)}>
                <span className="avatar avatar-md avatar-dark">{initials(user.fullName)}</span>
                <span className="user-info">
                  <strong>{user.fullName}</strong>
                  <span>{USER_TYPE[user.userType]}</span>
                </span>
                <ChevronDown size={16} className="muted" />
              </button>
              {userMenuOpen && (
                <div className="dropdown">
                  <div className="dropdown-head">
                    <strong>{user.fullName}</strong>
                    <span>{user.email}</span>
                  </div>
                  <button onClick={() => navigate('/conta')}><KeyRound size={16} /> Alterar senha</button>
                  <button onClick={logout}><LogOut size={16} /> Sair</button>
                </div>
              )}
            </div>
          </div>
        </header>

        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
