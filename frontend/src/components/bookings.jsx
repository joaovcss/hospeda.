import { useEffect, useState } from 'react';
import { BedDouble, Check, LogIn, LogOut, Plus, Search, ShieldOff, X } from 'lucide-react';
import { areasApi, bookingsApi, guestsApi, roomsApi } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import {
  AREA_STATUS, BOOKING_STATUS, ROOM_CATEGORY, addDays, formatDate, formatDateTime, formatMoney, todayISO,
} from '../utils/format';
import { Avatar, Badge, Button, EmptyState, Field, Modal, Spinner, StatusBadge, useToast } from './ui';

/** Executa uma ação da API com feedback por toast. */
export function useAction() {
  const notify = useToast();
  const [running, setRunning] = useState(null);
  const run = async (key, fn, successMessage) => {
    setRunning(key);
    try {
      const result = await fn();
      if (successMessage) notify(successMessage);
      return result;
    } catch (error) {
      notify(error.message, 'error');
      return undefined;
    } finally {
      setRunning(null);
    }
  };
  return { run, running };
}

export function BookingActions({ booking, onChanged, size = 'sm', compact = false }) {
  const { can } = useAuth();
  const { run, running } = useAction();
  const [cancelling, setCancelling] = useState(false);
  const today = todayISO();

  const act = async (key, fn, message) => {
    const result = await run(key, fn, message);
    if (result !== undefined) onChanged?.(result);
  };

  return (
    <>
      {booking.status === 'PENDING' && can('BOOKING_CONFIRM') && (
        <Button size={size} variant="primary" icon={Check} loading={running === 'confirm'} onClick={() => act('confirm', () => bookingsApi.confirm(booking.id), `Reserva #${booking.id} confirmada`)}>
          Confirmar
        </Button>
      )}
      {booking.status === 'CONFIRMED' && can('CHECKIN') && booking.checkInDate <= today && (
        <Button size={size} variant="dark" icon={LogIn} loading={running === 'checkin'} onClick={() => act('checkin', () => bookingsApi.checkIn(booking.id), `Check-in de ${booking.guestName} realizado`)}>
          Check-in
        </Button>
      )}
      {booking.status === 'CHECKED_IN' && can('CHECKOUT') && (
        <Button size={size} variant="dark" icon={LogOut} loading={running === 'checkout'} onClick={() => act('checkout', () => bookingsApi.checkOut(booking.id), `Check-out de ${booking.guestName} realizado`)}>
          Check-out
        </Button>
      )}
      {!compact && ['PENDING', 'CONFIRMED'].includes(booking.status) && can('BOOKING_CANCEL') && (
        <Button size={size} variant="ghost" icon={X} onClick={() => setCancelling(true)}>Cancelar</Button>
      )}
      {cancelling && (
        <CancelBookingModal booking={booking} onClose={() => setCancelling(false)} onCancelled={(b) => { setCancelling(false); onChanged?.(b); }} />
      )}
    </>
  );
}

export function CancelBookingModal({ booking, onClose, onCancelled }) {
  const [reason, setReason] = useState('');
  const { run, running } = useAction();
  const confirm = async () => {
    const result = await run('cancel', () => bookingsApi.cancel(booking.id, reason || null), `Reserva #${booking.id} cancelada`);
    if (result !== undefined) onCancelled(result);
  };
  return (
    <Modal
      title="Cancelar reserva"
      subtitle={`#RES-${booking.id} · ${booking.guestName} · quarto ${booking.room.number}`}
      onClose={onClose}
      width={460}
      footer={(
        <>
          <Button onClick={onClose}>Voltar</Button>
          <Button variant="danger" loading={running === 'cancel'} onClick={confirm}>Cancelar reserva</Button>
        </>
      )}
    >
      <p className="muted">O cancelamento é definitivo: a reserva não poderá ser reaberta.</p>
      <Field label="Motivo (opcional)">
        <textarea className="textarea" maxLength={255} value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Ex.: mudança de planos do hóspede" />
      </Field>
    </Modal>
  );
}

export function BookingDetailsModal({ booking: initial, onClose, onChanged }) {
  const { can, isGuest } = useAuth();
  const [booking, setBooking] = useState(initial);
  const [grants, setGrants] = useState(null);
  const [areas, setAreas] = useState([]);
  const [areaId, setAreaId] = useState('');
  const { run, running } = useAction();
  const canSeeAreas = isGuest || can('ACCESS_READ');

  useEffect(() => {
    if (!canSeeAreas) return;
    bookingsApi.areas(booking.id).then(setGrants).catch(() => setGrants([]));
    if (can('ACCESS_GRANT')) areasApi.list().then((p) => setAreas(p.content)).catch(() => {});
  }, [booking.id, canSeeAreas, can]);

  const changed = (updated) => {
    setBooking(updated);
    onChanged?.(updated);
  };

  const grant = async () => {
    const result = await run('grant', () => bookingsApi.grantArea(booking.id, Number(areaId)), 'Acesso concedido');
    if (result) {
      setGrants((list) => [...list.filter((g) => g.areaId !== result.areaId), result]);
      setAreaId('');
    }
  };

  const revoke = async (grantItem) => {
    const result = await run(`revoke-${grantItem.areaId}`, () => bookingsApi.revokeArea(booking.id, grantItem.areaId), 'Acesso revogado');
    if (result !== undefined) setGrants((list) => list.map((g) => (g.id === grantItem.id ? { ...g, status: 'REVOKED' } : g)));
  };

  const finalStatus = ['CHECKED_OUT', 'CANCELLED'].includes(booking.status);
  const grantableAreas = areas.filter((a) => a.status !== 'INACTIVE' && !grants?.some((g) => g.areaId === a.id && g.status === 'ACTIVE'));

  const steps = [
    ['Reserva criada', booking.createdAt, 'tone-gray'],
    ['Confirmada', booking.confirmedAt, 'tone-lime'],
    ['Check-in', booking.checkedInAt, 'tone-blue'],
    ['Check-out', booking.checkedOutAt, 'tone-dark'],
    ['Cancelada', booking.cancelledAt, 'tone-amber'],
  ].filter(([, date]) => date);

  return (
    <Modal
      title={`Reserva #RES-${booking.id}`}
      subtitle={`${formatDate(booking.checkInDate, { day: '2-digit', month: 'long' })} a ${formatDate(booking.checkOutDate, { day: '2-digit', month: 'long', year: 'numeric' })}`}
      onClose={onClose}
      width={620}
      footer={!isGuest && !finalStatus ? <BookingActions booking={booking} onChanged={changed} size="md" /> : <Button onClick={onClose}>Fechar</Button>}
    >
      <div className="cell-main">
        <Avatar name={booking.guestName} size="lg" />
        <div className="cell-stack" style={{ flex: 1 }}>
          <strong style={{ fontSize: 16 }}>{booking.guestName}</strong>
          <span>{booking.numberOfGuests} hóspede(s) · {booking.nights} diária(s)</span>
        </div>
        <StatusBadge map={BOOKING_STATUS} value={booking.status} />
      </div>

      <dl className="details">
        <div><dt>Quarto</dt><dd>{booking.room.number} · {ROOM_CATEGORY[booking.room.category]} · {booking.room.floor}º andar</dd></div>
        <div><dt>Valor total</dt><dd>{formatMoney(booking.totalAmount)}</dd></div>
        <div><dt>Entrada</dt><dd>{formatDate(booking.checkInDate, { weekday: 'short', day: '2-digit', month: 'short' })}</dd></div>
        <div><dt>Saída</dt><dd>{formatDate(booking.checkOutDate, { weekday: 'short', day: '2-digit', month: 'short' })}</dd></div>
        {booking.notes && <div style={{ gridColumn: '1 / -1' }}><dt>Observações</dt><dd>{booking.notes}</dd></div>}
        {booking.cancellationReason && <div style={{ gridColumn: '1 / -1' }}><dt>Motivo do cancelamento</dt><dd>{booking.cancellationReason}</dd></div>}
      </dl>

      <div>
        <p className="form-section" style={{ marginBottom: 10 }}>Histórico</p>
        <div className="timeline">
          {steps.map(([label, date, tone]) => (
            <div key={label} className="timeline-item"><span className={`dot ${tone}`} /> <strong>{label}</strong> <span className="muted">{formatDateTime(date)}</span></div>
          ))}
        </div>
      </div>

      {canSeeAreas && (
        <div>
          <p className="form-section" style={{ marginBottom: 10 }}>Acesso às áreas</p>
          {grants === null ? <Spinner /> : (
            <div className="task-list">
              {grants.length === 0 && <span className="muted">Nenhuma área liberada para esta hospedagem.</span>}
              {grants.map((g) => (
                <div key={g.id} className="task">
                  <div className="task-body">
                    <strong>{g.areaName}</strong>
                    <span>{g.areaLocation} · válido de {formatDateTime(g.validFrom)} até {formatDateTime(g.validUntil)}</span>
                  </div>
                  <Badge tone={g.status === 'ACTIVE' ? 'green' : 'gray'}>{g.status === 'ACTIVE' ? 'Ativo' : 'Revogado'}</Badge>
                  {g.status === 'ACTIVE' && can('ACCESS_REVOKE') && (
                    <Button size="sm" variant="ghost" icon={ShieldOff} loading={running === `revoke-${g.areaId}`} onClick={() => revoke(g)}>Revogar</Button>
                  )}
                </div>
              ))}
            </div>
          )}
          {can('ACCESS_GRANT') && !finalStatus && grantableAreas.length > 0 && (
            <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
              <select className="select" value={areaId} onChange={(e) => setAreaId(e.target.value)}>
                <option value="">Liberar acesso a uma área...</option>
                {grantableAreas.map((a) => <option key={a.id} value={a.id}>{a.name} — {AREA_STATUS[a.status].label}</option>)}
              </select>
              <Button variant="primary" icon={Plus} disabled={!areaId} loading={running === 'grant'} onClick={grant}>Liberar</Button>
            </div>
          )}
        </div>
      )}
    </Modal>
  );
}

export function GuestPicker({ value, onChange, autoFocus = false }) {
  const [search, setSearch] = useState('');
  const [results, setResults] = useState(null);

  useEffect(() => {
    const timer = setTimeout(() => {
      guestsApi.list({ search, size: 6 }).then((p) => setResults(p.content)).catch(() => setResults([]));
    }, 250);
    return () => clearTimeout(timer);
  }, [search]);

  if (value) {
    return (
      <div className="summary-box">
        <div className="cell-main">
          <Avatar name={value.fullName} size="sm" />
          <div className="cell-stack"><strong>{value.fullName}</strong><span>{value.email}</span></div>
        </div>
        <Button size="sm" variant="ghost" onClick={() => onChange(null)}>Trocar</Button>
      </div>
    );
  }
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      <div className="search" style={{ width: '100%', margin: 0 }}>
        <Search size={16} />
        <input autoFocus={autoFocus} value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Buscar por nome, e-mail ou CPF" />
      </div>
      <div className="task-list">
        {results?.map((g) => (
          <button key={g.id} type="button" className="task" style={{ textAlign: 'left', alignItems: 'center' }} onClick={() => onChange(g)}>
            <Avatar name={g.fullName} size="sm" />
            <div className="task-body"><strong>{g.fullName}</strong><span>{g.email} · CPF {g.maskedCpf}</span></div>
          </button>
        ))}
        {results?.length === 0 && <span className="muted" style={{ fontSize: 13 }}>Nenhum hóspede encontrado.</span>}
      </div>
    </div>
  );
}

export function NewBookingModal({ onClose, onCreated }) {
  const { isGuest } = useAuth();
  const { run, running } = useAction();
  const [guest, setGuest] = useState(null);
  const [form, setForm] = useState({ checkInDate: todayISO(), checkOutDate: addDays(todayISO(), 2), numberOfGuests: 2, notes: '' });
  const [rooms, setRooms] = useState(null);
  const [roomsError, setRoomsError] = useState(null);
  const [roomId, setRoomId] = useState(null);

  const validDates = form.checkOutDate > form.checkInDate;

  useEffect(() => {
    setRoomId(null);
    if (!validDates || !form.numberOfGuests) return;
    setRooms(null);
    setRoomsError(null);
    roomsApi.available(form.checkInDate, form.checkOutDate, form.numberOfGuests)
      .then(setRooms)
      .catch((error) => { setRooms([]); setRoomsError(error.message); });
  }, [form.checkInDate, form.checkOutDate, form.numberOfGuests, validDates]);

  const room = rooms?.find((r) => r.id === roomId);
  const nights = validDates ? Math.round((new Date(form.checkOutDate) - new Date(form.checkInDate)) / 86400000) : 0;
  const canSubmit = room && validDates && (isGuest || guest);

  const submit = async () => {
    const payload = { ...form, numberOfGuests: Number(form.numberOfGuests), roomId, guestId: guest?.id, notes: form.notes || null };
    const result = await run('create', () => bookingsApi.create(payload), isGuest ? 'Solicitação enviada! Aguarde a confirmação do hotel.' : 'Reserva criada e confirmada');
    if (result) onCreated(result);
  };

  const set = (key) => (event) => setForm({ ...form, [key]: event.target.value });

  return (
    <Modal
      title={isGuest ? 'Solicitar reserva' : 'Nova reserva'}
      subtitle={isGuest ? 'Sua solicitação será confirmada pela recepção.' : 'Reservas criadas pela equipe já nascem confirmadas.'}
      onClose={onClose}
      width={640}
      footer={(
        <>
          <Button onClick={onClose}>Cancelar</Button>
          <Button variant="primary" icon={Check} disabled={!canSubmit} loading={running === 'create'} onClick={submit}>
            {isGuest ? 'Enviar solicitação' : 'Criar reserva'}
          </Button>
        </>
      )}
    >
      {!isGuest && (
        <Field label="Hóspede">
          <GuestPicker value={guest} onChange={setGuest} autoFocus />
        </Field>
      )}
      <div className="form-grid">
        <Field label="Entrada">
          <input className="input" type="date" min={todayISO()} value={form.checkInDate} onChange={set('checkInDate')} />
        </Field>
        <Field label="Saída" error={!validDates ? 'A saída deve ser depois da entrada' : null}>
          <input className="input" type="date" min={addDays(form.checkInDate, 1)} value={form.checkOutDate} onChange={set('checkOutDate')} />
        </Field>
        <Field label="Hóspedes">
          <input className="input" type="number" min={1} max={20} value={form.numberOfGuests} onChange={set('numberOfGuests')} />
        </Field>
        <Field label="Diárias">
          <input className="input" value={nights} disabled />
        </Field>
      </div>

      <Field label="Quarto disponível">
        {rooms === null && validDates ? <Spinner label="Buscando quartos disponíveis..." /> : roomsError ? (
          <div className="alert alert-error">{roomsError}</div>
        ) : rooms?.length === 0 ? (
          <EmptyState icon={BedDouble} title="Nenhum quarto disponível">Tente outras datas ou menos hóspedes.</EmptyState>
        ) : (
          <div className="room-options">
            {rooms?.map((r) => (
              <button type="button" key={r.id} className={`room-option ${roomId === r.id ? 'selected' : ''}`} onClick={() => setRoomId(r.id)}>
                <strong>{r.number}</strong>
                <span>{ROOM_CATEGORY[r.category]} · até {r.capacity}</span>
                <span>{formatMoney(r.dailyRate, true)}/noite</span>
              </button>
            ))}
          </div>
        )}
      </Field>

      <Field label="Observações">
        <textarea className="textarea" maxLength={500} value={form.notes} onChange={set('notes')} placeholder="Ex.: chegada após as 20h" />
      </Field>

      {room && (
        <div className="summary-box">
          <span>{nights} diária(s) × {formatMoney(room.dailyRate)}</span>
          <strong>{formatMoney(room.dailyRate * nights)}</strong>
        </div>
      )}
    </Modal>
  );
}
