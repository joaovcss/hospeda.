import { useState } from 'react';
import { CheckCircle2, Clock, MapPin, Pencil, Plus, ScanLine, ShieldCheck, Waves, XCircle } from 'lucide-react';
import { accessApi, areasApi, guestsApi } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { GuestPicker, useAction } from '../components/bookings';
import { Button, Card, EmptyState, ErrorState, Field, Modal, PageHeader, Spinner, StatusBadge, useToast } from '../components/ui';
import { AREA_STATUS, formatTime } from '../utils/format';
import { useApi } from '../utils/useApi';

export default function Areas() {
  const { isAdmin, can } = useAuth();
  const [editing, setEditing] = useState(null);
  const { run, running } = useAction();
  const { data, error, loading, reload } = useApi(() => areasApi.list(), []);

  const setStatus = async (area, status) => {
    const result = await run(`${area.id}-${status}`, () => areasApi.setStatus(area.id, status), `${area.name}: ${AREA_STATUS[status].label.toLowerCase()}`);
    if (result) reload({ silent: true });
  };

  return (
    <>
      <PageHeader title="Áreas e acessos" subtitle="Piscina, academia, spa e demais espaços: horários, situação e controle de entrada.">
        {isAdmin && <Button variant="primary" icon={Plus} onClick={() => setEditing({})}>Nova área</Button>}
      </PageHeader>

      {can('ACCESS_REGISTER') && data && <AccessDesk areas={data.content} />}

      {loading ? <Spinner /> : error ? <ErrorState error={error} onRetry={reload} /> : data.content.length === 0 ? (
        <Card><EmptyState icon={Waves} title="Nenhuma área cadastrada" /></Card>
      ) : (
        <div className="area-grid">
          {data.content.map((area) => (
            <Card key={area.id} className="area-card">
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 10, alignItems: 'flex-start' }}>
                <h3>{area.name}</h3>
                <StatusBadge map={AREA_STATUS} value={area.status} />
              </div>
              {area.description && <p className="muted" style={{ fontSize: 13 }}>{area.description}</p>}
              <div className="area-meta">
                <span><MapPin size={15} /> {area.location}</span>
                <span><Clock size={15} /> {area.openingTime ? `${formatTime(area.openingTime)} às ${formatTime(area.closingTime)}` : 'Aberta 24 horas'}</span>
              </div>
              {isAdmin && (
                <footer>
                  <Button size="sm" icon={Pencil} onClick={() => setEditing(area)}>Editar</Button>
                  {Object.keys(AREA_STATUS).filter((s) => s !== area.status).map((s) => (
                    <Button key={s} size="sm" variant="ghost" loading={running === `${area.id}-${s}`} onClick={() => setStatus(area, s)}>
                      {s === 'ACTIVE' ? 'Reativar' : s === 'UNAVAILABLE' ? 'Suspender' : 'Desativar'}
                    </Button>
                  ))}
                </footer>
              )}
            </Card>
          ))}
        </div>
      )}

      {editing && <AreaForm area={editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); reload({ silent: true }); }} />}
    </>
  );
}

function AccessDesk({ areas }) {
  const [guest, setGuest] = useState(null);
  const [areaId, setAreaId] = useState('');
  const [result, setResult] = useState(null);
  const { run, running } = useAction();

  const check = async (register) => {
    setResult(null);
    const response = await run(register ? 'register' : 'check', () => (register
      ? accessApi.validate(guest.id, Number(areaId))
      : guestsApi.checkAuthorization(guest.id, Number(areaId))));
    if (response) setResult({ ...response, allowed: response.allowed ?? response.result === 'GRANTED', registered: register });
  };

  return (
    <Card title="Controle de entrada" subtitle="Verifique se o hóspede pode entrar em uma área e registre a tentativa." className="mb">
      <div className="form-grid">
        <Field label="Hóspede"><GuestPicker value={guest} onChange={(g) => { setGuest(g); setResult(null); }} /></Field>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <Field label="Área">
            <select className="select" value={areaId} onChange={(e) => { setAreaId(e.target.value); setResult(null); }}>
              <option value="">Selecione a área...</option>
              {areas.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}
            </select>
          </Field>
          <div className="chip-row">
            <Button variant="primary" icon={ScanLine} disabled={!guest || !areaId} loading={running === 'register'} onClick={() => check(true)}>Registrar entrada</Button>
            <Button icon={ShieldCheck} disabled={!guest || !areaId} loading={running === 'check'} onClick={() => check(false)}>Só verificar</Button>
          </div>
        </div>
      </div>
      {result && (
        <div className={`result-card ${result.allowed ? 'ok' : 'denied'}`}>
          {result.allowed ? <CheckCircle2 size={28} /> : <XCircle size={28} />}
          <div>
            <strong>{result.allowed ? 'Acesso liberado' : 'Acesso negado'}</strong>
            <span>{result.allowed ? `${guest.fullName} pode entrar.` : result.denialMessage}</span>
            {result.registered && <span> · Tentativa registrada no histórico.</span>}
          </div>
        </div>
      )}
    </Card>
  );
}

function AreaForm({ area, onClose, onSaved }) {
  const isNew = !area.id;
  const notify = useToast();
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({
    name: area.name || '', description: area.description || '', location: area.location || '',
    openingTime: formatTime(area.openingTime).replace('—', ''), closingTime: formatTime(area.closingTime).replace('—', ''),
  });
  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    const payload = { ...form, description: form.description || null, openingTime: form.openingTime || null, closingTime: form.closingTime || null };
    try {
      await (isNew ? areasApi.create(payload) : areasApi.update(area.id, payload));
      notify(isNew ? `Área ${form.name} cadastrada` : 'Área atualizada');
      onSaved();
    } catch (error) {
      notify(error.message, 'error');
      setSaving(false);
    }
  };

  return (
    <Modal
      title={isNew ? 'Nova área' : `Editar ${area.name}`}
      onClose={onClose}
      footer={(
        <>
          <Button onClick={onClose}>Cancelar</Button>
          <Button variant="primary" type="submit" form="area-form" loading={saving}>Salvar</Button>
        </>
      )}
    >
      <form id="area-form" className="form-grid" onSubmit={submit}>
        <Field label="Nome" className="full"><input className="input" required maxLength={100} value={form.name} onChange={set('name')} /></Field>
        <Field label="Localização" className="full"><input className="input" required maxLength={150} value={form.location} onChange={set('location')} placeholder="Térreo - Bloco B" /></Field>
        <Field label="Abre às" hint="Deixe vazio para 24h"><input className="input" type="time" value={form.openingTime} onChange={set('openingTime')} /></Field>
        <Field label="Fecha às" hint="Pode passar da meia-noite"><input className="input" type="time" value={form.closingTime} onChange={set('closingTime')} /></Field>
        <Field label="Descrição" className="full"><textarea className="textarea" maxLength={500} value={form.description} onChange={set('description')} /></Field>
      </form>
    </Modal>
  );
}
