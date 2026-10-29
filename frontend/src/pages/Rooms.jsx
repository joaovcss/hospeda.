import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { BedDouble, Pencil, Plus } from 'lucide-react';
import { fetchAll, roomsApi } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { useAction } from '../components/bookings';
import { Button, Card, EmptyState, ErrorState, Field, Modal, PageHeader, Spinner, StatusBadge, useToast } from '../components/ui';
import { ROOM_CATEGORY, ROOM_STATUS, formatMoney } from '../utils/format';
import { useApi } from '../utils/useApi';

export default function Rooms() {
  const { isAdmin } = useAuth();
  const [params, setParams] = useSearchParams();
  const status = params.get('status') || '';
  const [selected, setSelected] = useState(null);
  const [editing, setEditing] = useState(null);
  const { data: rooms, error, loading, reload } = useApi(() => fetchAll(roomsApi.list), []);

  if (loading) return <Spinner label="Carregando o mapa de quartos..." />;
  if (error) return <ErrorState error={error} onRetry={reload} />;

  const counts = Object.fromEntries(Object.keys(ROOM_STATUS).map((s) => [s, rooms.filter((r) => r.status === s).length]));
  const visible = rooms.filter((r) => !status || r.status === status);
  const floors = [...new Set(visible.map((r) => r.floor))].sort((a, b) => a - b);

  return (
    <>
      <PageHeader title="Quartos" subtitle="Mapa por andar com a situação de cada quarto em tempo real.">
        {isAdmin && <Button variant="primary" icon={Plus} onClick={() => setEditing({})}>Novo quarto</Button>}
      </PageHeader>

      <div className="chip-row" style={{ marginBottom: 22 }}>
        <button className={`chip ${!status ? 'active' : ''}`} onClick={() => setParams({})}>Todos <small>{rooms.length}</small></button>
        {Object.entries(ROOM_STATUS).map(([value, meta]) => (
          <button key={value} className={`chip ${status === value ? 'active' : ''}`} onClick={() => setParams({ status: value })}>
            {meta.label} <small>{counts[value]}</small>
          </button>
        ))}
      </div>

      <Card>
        {visible.length === 0 ? <EmptyState icon={BedDouble} title="Nenhum quarto nesta situação" /> : floors.map((floor) => (
          <div key={floor} className="floor">
            <h3>{floor === 0 ? 'Térreo' : `${floor}º andar`}</h3>
            <div className="room-grid">
              {visible.filter((r) => r.floor === floor).map((room) => (
                <button key={room.id} className={`room-tile ${room.status}`} onClick={() => setSelected(room)}>
                  <span className={`dot ${room.status === 'AVAILABLE' ? 'tone-lime' : room.status === 'MAINTENANCE' ? 'tone-amber' : 'tone-gray'}`} />
                  <strong>{room.number}</strong>
                  <span>{ROOM_CATEGORY[room.category]} · {room.capacity} pax</span>
                  <span>{formatMoney(room.dailyRate, true)}</span>
                </button>
              ))}
            </div>
          </div>
        ))}
      </Card>

      {selected && (
        <RoomDetails
          room={selected}
          onClose={() => setSelected(null)}
          onEdit={() => { setEditing(selected); setSelected(null); }}
          onChanged={(room) => { setSelected(room); reload({ silent: true }); }}
        />
      )}
      {editing && <RoomForm room={editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); reload({ silent: true }); }} />}
    </>
  );
}

function RoomDetails({ room, onClose, onEdit, onChanged }) {
  const { isAdmin } = useAuth();
  const { run, running } = useAction();
  const setStatus = async (status) => {
    const result = await run(status, () => roomsApi.setStatus(room.id, status), `Quarto ${room.number}: ${ROOM_STATUS[status].label.toLowerCase()}`);
    if (result) onChanged(result);
  };

  return (
    <Modal
      title={`Quarto ${room.number}`}
      subtitle={`${room.floor}º andar · ${ROOM_CATEGORY[room.category]}`}
      onClose={onClose}
      width={480}
      footer={isAdmin ? <Button icon={Pencil} onClick={onEdit}>Editar quarto</Button> : <Button onClick={onClose}>Fechar</Button>}
    >
      <dl className="details">
        <div><dt>Situação</dt><dd><StatusBadge map={ROOM_STATUS} value={room.status} /></dd></div>
        <div><dt>Diária</dt><dd>{formatMoney(room.dailyRate)}</dd></div>
        <div><dt>Capacidade</dt><dd>{room.capacity} hóspede(s)</dd></div>
        <div><dt>Categoria</dt><dd>{ROOM_CATEGORY[room.category]}</dd></div>
        {room.description && <div style={{ gridColumn: '1 / -1' }}><dt>Descrição</dt><dd>{room.description}</dd></div>}
      </dl>
      {isAdmin && (
        room.status === 'OCCUPIED' ? (
          <div className="alert alert-info">O quarto está ocupado. A situação muda automaticamente no check-out.</div>
        ) : (
          <div>
            <p className="form-section" style={{ marginBottom: 10 }}>Alterar situação</p>
            <div className="chip-row">
              {['AVAILABLE', 'MAINTENANCE', 'INACTIVE'].filter((s) => s !== room.status).map((s) => (
                <Button key={s} size="sm" loading={running === s} onClick={() => setStatus(s)}>Marcar como {ROOM_STATUS[s].label.toLowerCase()}</Button>
              ))}
            </div>
          </div>
        )
      )}
    </Modal>
  );
}

function RoomForm({ room, onClose, onSaved }) {
  const isNew = !room.id;
  const notify = useToast();
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({
    number: room.number || '', floor: room.floor ?? 1, capacity: room.capacity ?? 2, category: room.category || 'STANDARD',
    dailyRate: room.dailyRate ?? '', description: room.description || '',
  });
  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    const payload = { ...form, floor: Number(form.floor), capacity: Number(form.capacity), dailyRate: Number(form.dailyRate), description: form.description || null };
    try {
      await (isNew ? roomsApi.create(payload) : roomsApi.update(room.id, payload));
      notify(isNew ? `Quarto ${form.number} cadastrado` : 'Quarto atualizado');
      onSaved();
    } catch (error) {
      notify(error.message, 'error');
      setSaving(false);
    }
  };

  return (
    <Modal
      title={isNew ? 'Novo quarto' : `Editar quarto ${room.number}`}
      onClose={onClose}
      footer={(
        <>
          <Button onClick={onClose}>Cancelar</Button>
          <Button variant="primary" type="submit" form="room-form" loading={saving}>Salvar</Button>
        </>
      )}
    >
      <form id="room-form" className="form-grid" onSubmit={submit}>
        <Field label="Número"><input className="input" required pattern="[A-Za-z0-9-]+" maxLength={10} value={form.number} onChange={set('number')} /></Field>
        <Field label="Andar"><input className="input" type="number" min={0} max={200} required value={form.floor} onChange={set('floor')} /></Field>
        <Field label="Categoria">
          <select className="select" value={form.category} onChange={set('category')}>
            {Object.entries(ROOM_CATEGORY).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
        </Field>
        <Field label="Capacidade"><input className="input" type="number" min={1} max={20} required value={form.capacity} onChange={set('capacity')} /></Field>
        <Field label="Diária (R$)" className="full"><input className="input" type="number" min="0.01" step="0.01" required value={form.dailyRate} onChange={set('dailyRate')} /></Field>
        <Field label="Descrição" className="full"><textarea className="textarea" maxLength={500} value={form.description} onChange={set('description')} /></Field>
      </form>
    </Modal>
  );
}
