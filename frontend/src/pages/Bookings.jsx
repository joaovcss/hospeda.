import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { CalendarDays, MoreHorizontal, Plus } from 'lucide-react';
import { bookingsApi } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { BookingActions, BookingDetailsModal, NewBookingModal } from '../components/bookings';
import { Avatar, Button, Card, EmptyState, ErrorState, PageHeader, Pagination, Spinner, StatusBadge } from '../components/ui';
import { BOOKING_STATUS, ROOM_CATEGORY, formatDate, formatMoney } from '../utils/format';
import { useApi } from '../utils/useApi';

const FILTERS = [['', 'Todas'], ...Object.entries(BOOKING_STATUS).map(([value, meta]) => [value, meta.label])];

export default function Bookings() {
  const { can } = useAuth();
  const [params, setParams] = useSearchParams();
  const status = params.get('status') || '';
  const [range, setRange] = useState({ from: '', to: '' });
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState(null);
  const [creating, setCreating] = useState(false);

  const { data, error, loading, reload } = useApi(
    () => bookingsApi.list({ status, from: range.from, to: range.to, page, size: 15, sort: 'checkInDate,desc' }),
    [status, range.from, range.to, page],
  );

  const setStatus = (value) => {
    setPage(0);
    setParams(value ? { status: value } : {});
  };

  return (
    <>
      <PageHeader title="Reservas" subtitle="Acompanhe o ciclo completo: solicitação, confirmação, check-in e check-out.">
        {can('BOOKING_CREATE') && <Button variant="primary" icon={Plus} onClick={() => setCreating(true)}>Nova reserva</Button>}
      </PageHeader>

      <Card padded={false} className="card-table">
        <div className="toolbar">
          <div className="chip-row grow">
            {FILTERS.map(([value, label]) => (
              <button key={value} className={`chip ${status === value ? 'active' : ''}`} onClick={() => setStatus(value)}>{label}</button>
            ))}
          </div>
          <input className="input input-sm" type="date" value={range.from} onChange={(e) => { setPage(0); setRange({ ...range, from: e.target.value }); }} aria-label="De" />
          <span className="muted">até</span>
          <input className="input input-sm" type="date" value={range.to} onChange={(e) => { setPage(0); setRange({ ...range, to: e.target.value }); }} aria-label="Até" />
        </div>

        {loading ? <Spinner /> : error ? <ErrorState error={error} onRetry={reload} /> : data.content.length === 0 ? (
          <EmptyState icon={CalendarDays} title="Nenhuma reserva encontrada">Ajuste os filtros ou crie uma nova reserva.</EmptyState>
        ) : (
          <>
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr><th>Hóspede</th><th>Quarto</th><th>Entrada</th><th>Saída</th><th>Valor</th><th>Situação</th><th /></tr>
                </thead>
                <tbody>
                  {data.content.map((b) => (
                    <tr key={b.id}>
                      <td>
                        <div className="cell-main">
                          <Avatar name={b.guestName} />
                          <div className="cell-stack"><strong>{b.guestName}</strong><span>#RES-{b.id} · {b.numberOfGuests} hóspede(s)</span></div>
                        </div>
                      </td>
                      <td><div className="cell-stack"><strong>{b.room.number}</strong><span>{ROOM_CATEGORY[b.room.category]}</span></div></td>
                      <td>{formatDate(b.checkInDate, { day: '2-digit', month: 'short', year: 'numeric' })}</td>
                      <td>{formatDate(b.checkOutDate, { day: '2-digit', month: 'short', year: 'numeric' })}</td>
                      <td><div className="cell-stack"><strong>{formatMoney(b.totalAmount)}</strong><span>{b.nights} diária(s)</span></div></td>
                      <td><StatusBadge map={BOOKING_STATUS} value={b.status} /></td>
                      <td className="actions">
                        <BookingActions booking={b} onChanged={() => reload({ silent: true })} />
                        <button className="icon-btn" style={{ display: 'inline-grid', verticalAlign: 'middle' }} onClick={() => setSelected(b)} aria-label="Detalhes"><MoreHorizontal size={18} /></button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={data} onChange={setPage} />
          </>
        )}
      </Card>

      {selected && <BookingDetailsModal booking={selected} onClose={() => setSelected(null)} onChanged={() => reload({ silent: true })} />}
      {creating && <NewBookingModal onClose={() => setCreating(false)} onCreated={() => { setCreating(false); reload({ silent: true }); }} />}
    </>
  );
}
