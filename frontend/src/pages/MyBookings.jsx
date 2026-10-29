import { useState } from 'react';
import { CalendarDays, Plus, X } from 'lucide-react';
import { bookingsApi } from '../api/client';
import { BookingDetailsModal, CancelBookingModal, NewBookingModal } from '../components/bookings';
import { Button, Card, EmptyState, ErrorState, PageHeader, Spinner, StatusBadge, Tabs } from '../components/ui';
import { BOOKING_STATUS, ROOM_CATEGORY, formatDate, formatMoney } from '../utils/format';
import { useApi } from '../utils/useApi';

export default function MyBookings() {
  const [scope, setScope] = useState('CURRENT');
  const [selected, setSelected] = useState(null);
  const [cancelling, setCancelling] = useState(null);
  const [creating, setCreating] = useState(false);
  const { data, error, loading, reload } = useApi(() => bookingsApi.mine(scope), [scope]);

  return (
    <>
      <PageHeader title="Minhas reservas" subtitle="Acompanhe suas solicitações, estadias atuais e histórico.">
        <Button variant="primary" icon={Plus} onClick={() => setCreating(true)}>Solicitar reserva</Button>
      </PageHeader>

      <Card padded={false} className="card-table">
        <div style={{ paddingTop: 20 }}>
          <Tabs value={scope} onChange={setScope} tabs={[{ value: 'CURRENT', label: 'Atuais' }, { value: 'PAST', label: 'Anteriores' }, { value: 'ALL', label: 'Todas' }]} />
        </div>
        {loading ? <Spinner /> : error ? <ErrorState error={error} onRetry={reload} /> : data.content.length === 0 ? (
          <EmptyState icon={CalendarDays} title="Nenhuma reserva aqui" />
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>Reserva</th><th>Quarto</th><th>Período</th><th>Valor</th><th>Situação</th><th /></tr></thead>
              <tbody>
                {data.content.map((b) => (
                  <tr key={b.id}>
                    <td><strong>#RES-{b.id}</strong></td>
                    <td><div className="cell-stack"><strong>{b.room.number}</strong><span>{ROOM_CATEGORY[b.room.category]}</span></div></td>
                    <td>{formatDate(b.checkInDate)} → {formatDate(b.checkOutDate)}</td>
                    <td>{formatMoney(b.totalAmount)}</td>
                    <td><StatusBadge map={BOOKING_STATUS} value={b.status} /></td>
                    <td className="actions">
                      {['PENDING', 'CONFIRMED'].includes(b.status) && <Button size="sm" variant="ghost" icon={X} onClick={() => setCancelling(b)}>Cancelar</Button>}
                      <Button size="sm" onClick={() => setSelected(b)}>Detalhes</Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {selected && <BookingDetailsModal booking={selected} onClose={() => setSelected(null)} />}
      {cancelling && <CancelBookingModal booking={cancelling} onClose={() => setCancelling(null)} onCancelled={() => { setCancelling(null); reload({ silent: true }); }} />}
      {creating && <NewBookingModal onClose={() => setCreating(false)} onCreated={() => { setCreating(false); reload({ silent: true }); }} />}
    </>
  );
}
