import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowUpRight, CalendarDays, CheckCircle2, Plus, Sun, Waves, XCircle } from 'lucide-react';
import { accessApi, bookingsApi, guestsApi } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { BookingDetailsModal, NewBookingModal } from '../components/bookings';
import { Button, Card, EmptyState, ErrorState, PageHeader, Spinner, StatusBadge } from '../components/ui';
import { BOOKING_STATUS, ROOM_CATEGORY, formatDate, formatDateTime, formatMoney, greeting } from '../utils/format';
import { useApi } from '../utils/useApi';

export default function GuestHome() {
  const { user } = useAuth();
  const [creating, setCreating] = useState(false);
  const [selected, setSelected] = useState(null);
  const { data, error, loading, reload } = useApi(async () => {
    const [bookings, grants, logs] = await Promise.all([bookingsApi.mine('CURRENT'), guestsApi.myAreas(), accessApi.myLogs()]);
    return { bookings: bookings.content, grants: grants.filter((g) => g.status === 'ACTIVE'), logs: logs.content.slice(0, 6) };
  }, []);

  if (loading) return <Spinner />;
  if (error) return <ErrorState error={error} onRetry={reload} />;

  const stay = data.bookings.find((b) => b.status === 'CHECKED_IN');
  const upcoming = data.bookings.filter((b) => b !== stay).sort((a, b) => a.checkInDate.localeCompare(b.checkInDate));
  const stayAreas = stay ? data.grants.filter((g) => g.bookingId === stay.id) : data.grants;

  return (
    <>
      <PageHeader title={<>{greeting()}, {user.fullName.split(' ')[0]} <Sun size={26} strokeWidth={1.6} /></>} subtitle="Que bom ter você por aqui. Tudo sobre a sua estadia em um só lugar.">
        <Button variant="primary" icon={Plus} onClick={() => setCreating(true)}>Solicitar reserva</Button>
      </PageHeader>

      {stay ? (
        <Card className="stay-hero mb">
          <div>
            <h2>Sua estadia está em andamento</h2>
            <p>{ROOM_CATEGORY[stay.room.category]} · {stay.room.floor}º andar · {stay.numberOfGuests} hóspede(s)</p>
            <div className="stay-facts">
              <div><span>Entrada</span><strong>{formatDate(stay.checkInDate, { weekday: 'short', day: '2-digit', month: 'short' })}</strong></div>
              <div><span>Saída</span><strong>{formatDate(stay.checkOutDate, { weekday: 'short', day: '2-digit', month: 'short' })}</strong></div>
              <div><span>Total</span><strong>{formatMoney(stay.totalAmount)}</strong></div>
            </div>
          </div>
          <div className="room-number"><span>Quarto</span>{stay.room.number}</div>
        </Card>
      ) : (
        <Card className="mb">
          <EmptyState icon={CalendarDays} title="Nenhuma estadia em andamento">Quando o check-in for feito, os detalhes aparecem aqui.</EmptyState>
        </Card>
      )}

      <div className="grid split">
        <Card title="Próximas reservas" action={<Link to="/minhas-reservas" className="link-btn">Ver todas <ArrowUpRight size={15} /></Link>}>
          {upcoming.length === 0 ? <EmptyState icon={CalendarDays} title="Nenhuma reserva futura">Que tal planejar a próxima viagem?</EmptyState> : (
            <div className="task-list">
              {upcoming.map((b) => (
                <button key={b.id} className="task" style={{ textAlign: 'left', alignItems: 'center' }} onClick={() => setSelected(b)}>
                  <span className="task-icon" style={{ background: 'var(--lime-tint)', color: 'var(--forest)' }}><CalendarDays size={16} /></span>
                  <div className="task-body">
                    <strong>Quarto {b.room.number} · {ROOM_CATEGORY[b.room.category]}</strong>
                    <span>{formatDate(b.checkInDate)} a {formatDate(b.checkOutDate)} · {formatMoney(b.totalAmount)}</span>
                  </div>
                  <StatusBadge map={BOOKING_STATUS} value={b.status} />
                </button>
              ))}
            </div>
          )}
        </Card>

        <Card title="Áreas liberadas" subtitle="Espaços que você pode usar durante a estadia">
          {stayAreas.length === 0 ? <EmptyState icon={Waves} title="Nenhuma área liberada">Fale com a recepção para liberar o acesso.</EmptyState> : (
            <div className="task-list">
              {stayAreas.map((g) => (
                <div key={g.id} className="task">
                  <span className="task-icon" style={{ background: 'var(--lime-tint)', color: 'var(--forest)' }}><Waves size={16} /></span>
                  <div className="task-body"><strong>{g.areaName}</strong><span>{g.areaLocation} · até {formatDateTime(g.validUntil)}</span></div>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>

      <Card title="Seus últimos acessos">
        {data.logs.length === 0 ? <EmptyState title="Nenhum acesso registrado ainda" /> : (
          <div className="task-list">
            {data.logs.map((log) => (
              <div key={log.id} className="task" style={{ alignItems: 'center' }}>
                <span className="task-icon" style={log.result === 'GRANTED' ? { background: 'var(--lime-tint)', color: 'var(--forest)' } : { background: 'var(--red-bg)', color: 'var(--red-text)' }}>
                  {log.result === 'GRANTED' ? <CheckCircle2 size={16} /> : <XCircle size={16} />}
                </span>
                <div className="task-body"><strong>{log.areaName}</strong><span>{formatDateTime(log.attemptedAt)}{log.denialMessage && ` · ${log.denialMessage}`}</span></div>
              </div>
            ))}
          </div>
        )}
      </Card>

      {creating && <NewBookingModal onClose={() => setCreating(false)} onCreated={() => { setCreating(false); reload({ silent: true }); }} />}
      {selected && <BookingDetailsModal booking={selected} onClose={() => setSelected(null)} />}
    </>
  );
}
