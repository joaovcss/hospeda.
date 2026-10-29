import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  ArrowUpRight, BedDouble, CalendarDays, CheckCircle2, Clock, DoorOpen, LogIn, LogOut, MoreHorizontal, Plus, Sun,
  TrendingDown, TrendingUp, Wallet, Wrench,
} from 'lucide-react';
import { bookingsApi, fetchAll, roomsApi } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { BookingActions, BookingDetailsModal, NewBookingModal } from '../components/bookings';
import { Avatar, Button, Card, EmptyState, ErrorState, PageHeader, Spinner, StatusBadge, Tabs } from '../components/ui';
import {
  BOOKING_STATUS, ROOM_CATEGORY, addDays, formatDate, formatMoney, greeting, parseISODate, todayISO,
} from '../utils/format';
import { useApi } from '../utils/useApi';

const OCCUPYING = ['CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT'];
const WEEKDAYS = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];

function startOfWeek(iso) {
  const day = parseISODate(iso).getDay();
  return addDays(iso, day === 0 ? -6 : 1 - day);
}

function occupancyOn(bookings, date, roomCount) {
  if (!roomCount) return 0;
  const occupied = bookings.filter((b) => OCCUPYING.includes(b.status) && b.checkInDate <= date && b.checkOutDate > date).length;
  return Math.min(100, Math.round((occupied / roomCount) * 100));
}

function Trend({ value, suffix }) {
  const up = value >= 0;
  const Icon = up ? TrendingUp : TrendingDown;
  return (
    <>
      <Icon size={14} />
      <b>{up ? '+' : ''}{value}{suffix}</b>
    </>
  );
}

export default function Dashboard() {
  const { user, can } = useAuth();
  const today = todayISO();
  const weekStart = startOfWeek(today);
  const weekEnd = addDays(weekStart, 6);
  const [tab, setTab] = useState('checkins');
  const [selected, setSelected] = useState(null);
  const [creating, setCreating] = useState(false);

  const { data, error, loading, reload } = useApi(async () => {
    const [rooms, bookings, pending] = await Promise.all([
      can('ROOM_READ') ? fetchAll(roomsApi.list) : [],
      can('BOOKING_READ') ? fetchAll(bookingsApi.list, { from: addDays(weekStart, -7), to: weekEnd }) : [],
      can('BOOKING_READ') ? fetchAll(bookingsApi.list, { status: 'PENDING' }) : [],
    ]);
    return { rooms, bookings, pending };
  }, []);

  const stats = useMemo(() => {
    if (!data) return null;
    const { rooms, bookings, pending } = data;
    const active = bookings.filter((b) => b.status !== 'CANCELLED');
    const bookableRooms = rooms.filter((r) => r.status !== 'INACTIVE').length;
    const count = (status) => rooms.filter((r) => r.status === status).length;

    const checkIns = active.filter((b) => b.checkInDate === today && b.status !== 'PENDING');
    const checkOuts = active.filter((b) => b.checkOutDate === today && ['CHECKED_IN', 'CHECKED_OUT'].includes(b.status));
    const inHouse = active.filter((b) => b.status === 'CHECKED_IN');
    const tonight = active.filter((b) => OCCUPYING.includes(b.status) && b.checkInDate <= today && b.checkOutDate > today);
    const lastWeekNight = active.filter((b) => OCCUPYING.includes(b.status) && b.checkInDate <= addDays(today, -7) && b.checkOutDate > addDays(today, -7));
    const dailyRevenue = (list) => list.reduce((sum, b) => sum + Number(b.totalAmount) / Math.max(1, b.nights), 0);

    const occupiedRooms = count('OCCUPIED');
    const occupancy = bookableRooms ? Math.round((occupiedRooms / bookableRooms) * 100) : 0;
    const week = Array.from({ length: 7 }, (_, i) => {
      const date = addDays(weekStart, i);
      return { date, value: occupancyOn(active, date, bookableRooms) };
    });
    const peak = week.filter((d) => d.date > today).sort((a, b) => b.value - a.value)[0];
    const revenueToday = dailyRevenue(tonight);
    const revenueLastWeek = dailyRevenue(lastWeekNight);

    const nextRelease = inHouse
      .filter((b) => b.checkOutDate >= today)
      .sort((a, b) => a.checkOutDate.localeCompare(b.checkOutDate))[0];

    return {
      rooms: { total: rooms.length, occupied: occupiedRooms, available: count('AVAILABLE'), maintenance: count('MAINTENANCE'), inactive: count('INACTIVE') },
      occupancy,
      occupancyDelta: occupancy - occupancyOn(active, addDays(today, -7), bookableRooms),
      checkIns,
      checkInsDone: checkIns.filter((b) => b.status !== 'CONFIRMED').length,
      checkOuts,
      checkOutsDone: checkOuts.filter((b) => b.status === 'CHECKED_OUT').length,
      inHouse,
      revenueToday,
      revenueDelta: revenueLastWeek ? Math.round(((revenueToday - revenueLastWeek) / revenueLastWeek) * 1000) / 10 : 0,
      week,
      peak,
      nextRelease,
      pending,
    };
  }, [data, today, weekStart]);

  const refresh = () => reload({ silent: true });

  if (loading) return <Spinner label="Preparando o painel do dia..." />;
  if (error) return <ErrorState error={error} onRetry={reload} />;

  const firstName = user.fullName.split(' ')[0];
  const movement = { checkins: stats.checkIns, checkouts: stats.checkOuts, inhouse: stats.inHouse }[tab];

  return (
    <>
      <PageHeader
        title={<>{greeting()}, {firstName} <Sun size={26} strokeWidth={1.6} /></>}
        subtitle="Tudo pronto para receber bem. Veja o que acontece no hotel hoje."
      >
        <Button icon={CalendarDays}>Hoje, {formatDate(today, { day: '2-digit', month: 'short', year: 'numeric' })}</Button>
        {can('BOOKING_CREATE') && <Button variant="primary" icon={Plus} onClick={() => setCreating(true)}>Nova reserva</Button>}
      </PageHeader>

      <div className="grid stats">
        <Card className="stat stat-highlight" padded={false}>
          <div className="stat-head">Taxa de ocupação <BedDouble size={18} /></div>
          <div className="stat-value"><strong>{stats.occupancy}%</strong><span>{stats.rooms.occupied} de {stats.rooms.total} quartos</span></div>
          <div className="stat-foot"><Trend value={stats.occupancyDelta} suffix=" p.p." /> vs. semana passada</div>
        </Card>
        <Card className="stat" padded={false}>
          <div className="stat-head">Check-ins hoje <LogIn size={18} /></div>
          <div className="stat-value"><strong>{stats.checkIns.length}</strong><span>{stats.checkIns.length - stats.checkInsDone} aguardando chegada</span></div>
          <div className="stat-foot"><CheckCircle2 size={14} /> <b>{stats.checkInsDone} realizados</b> até agora</div>
        </Card>
        <Card className="stat" padded={false}>
          <div className="stat-head">Check-outs hoje <LogOut size={18} /></div>
          <div className="stat-value"><strong>{stats.checkOuts.length}</strong><span>{stats.checkOuts.length - stats.checkOutsDone} saídas pendentes</span></div>
          <div className="stat-foot"><CheckCircle2 size={14} /> <b>{stats.checkOutsDone} realizados</b> até agora</div>
        </Card>
        <Card className="stat" padded={false}>
          <div className="stat-head">Receita do dia <Wallet size={18} /></div>
          <div className="stat-value"><strong>{formatMoney(stats.revenueToday, true)}</strong></div>
          <div className="stat-foot"><Trend value={stats.revenueDelta} suffix="%" /> vs. {WEEKDAYS[parseISODate(today).getDay()].toLowerCase()} anterior</div>
        </Card>
      </div>

      <div className="grid split">
        <Card
          title="Ocupação da semana"
          subtitle="Um olhar sobre os próximos dias"
          action={<span className="muted" style={{ fontSize: 12.5 }}>{formatDate(weekStart)} – {formatDate(weekEnd)}</span>}
        >
          <div className="chart">
            <div className="chart-axis"><span>100%</span><span>50%</span><span>0%</span></div>
            <div className="chart-bars">
              {stats.week.map((day) => {
                const date = parseISODate(day.date);
                return (
                  <div key={day.date} className={`chart-col ${day.date === today ? 'today' : ''}`}>
                    <div className="chart-track">
                      <div className="chart-bar" style={{ height: `${Math.max(day.value, 18)}%` }}>{day.value}%</div>
                    </div>
                    <span className="chart-label">{WEEKDAYS[date.getDay()]} {String(date.getDate()).padStart(2, '0')}</span>
                  </div>
                );
              })}
            </div>
          </div>
          {stats.peak && (
            <p className="chart-note">
              <TrendingUp size={15} /> Pico da semana: <b>{stats.peak.value}% de ocupação prevista {WEEKDAYS[parseISODate(stats.peak.date).getDay()].toLowerCase()}.</b>
            </p>
          )}
        </Card>

        <Card title="Situação dos quartos" action={<Link to="/quartos" className="link-btn">Ver mapa <ArrowUpRight size={15} /></Link>}>
          <div className="big-number"><strong>{stats.rooms.total}</strong><span>quartos no total</span></div>
          <div className="stack-bar">
            <span className="tone-dark" style={{ flexGrow: stats.rooms.occupied }} />
            <span className="tone-lime" style={{ flexGrow: stats.rooms.available }} />
            <span className="tone-amber" style={{ flexGrow: stats.rooms.maintenance }} />
            <span className="tone-blue" style={{ flexGrow: stats.rooms.inactive }} />
          </div>
          <div className="legend">
            <span className="legend-item"><span className="dot tone-dark" /> Ocupados <b>{String(stats.rooms.occupied).padStart(2, '0')}</b></span>
            <span className="legend-item"><span className="dot tone-lime" /> Disponíveis <b>{String(stats.rooms.available).padStart(2, '0')}</b></span>
            <span className="legend-item"><span className="dot tone-amber" /> Manutenção <b>{String(stats.rooms.maintenance).padStart(2, '0')}</b></span>
            <span className="legend-item"><span className="dot tone-blue" /> Inativos <b>{String(stats.rooms.inactive).padStart(2, '0')}</b></span>
          </div>
          {stats.nextRelease && (
            <div className="info-strip">
              <DoorOpen size={16} />
              Próxima liberação: quarto {stats.nextRelease.room.number}, {stats.nextRelease.checkOutDate === today ? 'hoje' : formatDate(stats.nextRelease.checkOutDate)}
            </div>
          )}
        </Card>
      </div>

      <div className="grid split-wide">
        <Card
          className="card-table"
          padded={false}
          title="Movimentação de hoje"
          action={<Link to="/reservas" className="link-btn">Todas as reservas <ArrowUpRight size={15} /></Link>}
        >
          <Tabs
            value={tab}
            onChange={setTab}
            tabs={[
              { value: 'checkins', label: 'Check-ins', count: stats.checkIns.length },
              { value: 'checkouts', label: 'Check-outs', count: stats.checkOuts.length },
              { value: 'inhouse', label: 'Hospedados', count: stats.inHouse.length },
            ]}
          />
          {movement.length === 0 ? (
            <EmptyState icon={CalendarDays} title="Nada por aqui">Nenhuma movimentação nesta categoria hoje.</EmptyState>
          ) : (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr><th>Hóspede</th><th>Quarto</th><th>Período</th><th>Situação</th><th /></tr>
                </thead>
                <tbody>
                  {movement.slice(0, 8).map((b) => (
                    <tr key={b.id}>
                      <td>
                        <div className="cell-main">
                          <Avatar name={b.guestName} />
                          <div className="cell-stack"><strong>{b.guestName}</strong><span>#RES-{b.id} · {b.numberOfGuests} hóspede(s)</span></div>
                        </div>
                      </td>
                      <td><div className="cell-stack"><strong>{b.room.number}</strong><span>{ROOM_CATEGORY[b.room.category]}</span></div></td>
                      <td className="nowrap">{formatDate(b.checkInDate)} → {formatDate(b.checkOutDate)}</td>
                      <td><StatusBadge map={BOOKING_STATUS} value={b.status} /></td>
                      <td className="actions">
                        <BookingActions booking={b} onChanged={refresh} compact />
                        <button className="icon-btn" style={{ display: 'inline-grid', verticalAlign: 'middle' }} onClick={() => setSelected(b)} aria-label="Detalhes"><MoreHorizontal size={18} /></button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>

        <AttentionCard stats={stats} onChanged={refresh} onOpen={setSelected} />
      </div>

      {selected && <BookingDetailsModal booking={selected} onClose={() => setSelected(null)} onChanged={refresh} />}
      {creating && <NewBookingModal onClose={() => setCreating(false)} onCreated={() => { setCreating(false); refresh(); }} />}
    </>
  );
}

function AttentionCard({ stats, onChanged, onOpen }) {
  const today = todayISO();
  const tasks = [
    ...stats.checkIns.filter((b) => b.status === 'CONFIRMED').map((b) => ({
      key: `i${b.id}`, booking: b, icon: LogIn, tone: 'var(--lime-tint)', color: 'var(--forest)',
      title: `Receber ${b.guestName}`, detail: `Quarto ${b.room.number} · chegada hoje`,
    })),
    ...stats.checkOuts.filter((b) => b.status === 'CHECKED_IN').map((b) => ({
      key: `o${b.id}`, booking: b, icon: LogOut, tone: 'var(--blue-bg)', color: 'var(--blue-text)',
      title: `Check-out de ${b.guestName}`, detail: `Quarto ${b.room.number} · saída hoje${b.checkOutDate < today ? ' (atrasado)' : ''}`,
    })),
    ...stats.pending.map((b) => ({
      key: `p${b.id}`, booking: b, icon: Clock, tone: 'var(--amber-bg)', color: 'var(--amber-text)',
      title: `Confirmar reserva de ${b.guestName}`,
      detail: `Quarto ${b.room.number} · ${formatDate(b.checkInDate)} a ${formatDate(b.checkOutDate)}`,
    })),
  ];
  const total = stats.checkIns.length + stats.checkOuts.length;
  const done = stats.checkInsDone + stats.checkOutsDone;

  return (
    <Card title="Atenção para hoje" action={<Link to="/reservas?status=PENDING" className="link-btn">Ver tudo <ArrowUpRight size={15} /></Link>}>
      <div className="progress-row">
        <div className="progress"><span style={{ width: `${total ? (done / total) * 100 : 100}%` }} /></div>
        {done} de {total} concluídas
      </div>
      {tasks.length === 0 && stats.rooms.maintenance === 0 ? (
        <EmptyState icon={CheckCircle2} title="Tudo em dia!">Nenhuma pendência para hoje.</EmptyState>
      ) : (
        <div className="task-list">
          {tasks.slice(0, 4).map(({ key, booking, icon: Icon, tone, color, title, detail }) => (
            <div key={key} className="task">
              <span className="task-icon" style={{ background: tone, color }}><Icon size={16} /></span>
              <div className="task-body">
                <strong>{title}</strong>
                <span>{detail}</span>
                <div className="task-actions">
                  <BookingActions booking={booking} onChanged={onChanged} compact />
                  <Button size="sm" variant="ghost" onClick={() => onOpen(booking)}>Detalhes</Button>
                </div>
              </div>
            </div>
          ))}
          {stats.rooms.maintenance > 0 && (
            <div className="task">
              <span className="task-icon" style={{ background: 'var(--amber-bg)', color: 'var(--amber-text)' }}><Wrench size={16} /></span>
              <div className="task-body">
                <strong>{stats.rooms.maintenance} quarto(s) em manutenção</strong>
                <span>Libere-os no mapa de quartos quando estiverem prontos.</span>
                <em><Link to="/quartos?status=MAINTENANCE">Ver quartos</Link></em>
              </div>
            </div>
          )}
          {tasks.length > 4 && <span className="muted" style={{ fontSize: 12, paddingTop: 10 }}>+ {tasks.length - 4} outras pendências</span>}
        </div>
      )}
    </Card>
  );
}
