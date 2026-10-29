import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Mail, MapPin, Pencil, Phone, Search, UserPlus, Users } from 'lucide-react';
import { bookingsApi, guestsApi } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Avatar, Button, Card, EmptyState, ErrorState, Field, Modal, PageHeader, Pagination, Spinner, StatusBadge, useToast } from '../components/ui';
import { BOOKING_STATUS, formatCpf, formatDate, formatMoney } from '../utils/format';
import { useApi } from '../utils/useApi';

export default function Guests() {
  const { can } = useAuth();
  const [params, setParams] = useSearchParams();
  const search = params.get('search') || '';
  const [input, setInput] = useState(search);
  const [page, setPage] = useState(0);
  const [selectedId, setSelectedId] = useState(null);
  const [editing, setEditing] = useState(null);

  useEffect(() => setInput(search), [search]);
  useEffect(() => {
    const timer = setTimeout(() => {
      if (input !== search) {
        setPage(0);
        setParams(input ? { search: input } : {});
      }
    }, 300);
    return () => clearTimeout(timer);
  }, [input, search, setParams]);

  const { data, error, loading, reload } = useApi(() => guestsApi.list({ search, page, size: 15 }), [search, page]);

  return (
    <>
      <PageHeader title="Hóspedes" subtitle="Cadastro, contato e histórico de hospedagens de cada hóspede.">
        {can('GUEST_CREATE') && <Button variant="primary" icon={UserPlus} onClick={() => setEditing({})}>Novo hóspede</Button>}
      </PageHeader>

      <Card padded={false} className="card-table">
        <div className="toolbar">
          <div className="search" style={{ margin: 0, width: 'min(380px, 100%)' }}>
            <Search size={16} />
            <input value={input} onChange={(e) => setInput(e.target.value)} placeholder="Buscar por nome, e-mail ou CPF" />
          </div>
        </div>
        {loading ? <Spinner /> : error ? <ErrorState error={error} onRetry={reload} /> : data.content.length === 0 ? (
          <EmptyState icon={Users} title="Nenhum hóspede encontrado">{search ? 'Tente outro termo de busca.' : 'Cadastre o primeiro hóspede.'}</EmptyState>
        ) : (
          <>
            <div className="table-wrap">
              <table className="table">
                <thead><tr><th>Hóspede</th><th>E-mail</th><th>CPF</th><th /></tr></thead>
                <tbody>
                  {data.content.map((g) => (
                    <tr key={g.id} style={{ cursor: 'pointer' }} onClick={() => setSelectedId(g.id)}>
                      <td><div className="cell-main"><Avatar name={g.fullName} /><strong>{g.fullName}</strong></div></td>
                      <td>{g.email}</td>
                      <td className="muted">{g.maskedCpf}</td>
                      <td className="actions"><Button size="sm" variant="ghost">Ver ficha</Button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={data} onChange={setPage} />
          </>
        )}
      </Card>

      {selectedId && <GuestDetails id={selectedId} onClose={() => setSelectedId(null)} onEdit={(guest) => { setSelectedId(null); setEditing(guest); }} />}
      {editing && <GuestForm guest={editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); reload({ silent: true }); }} />}
    </>
  );
}

function GuestDetails({ id, onClose, onEdit }) {
  const { can } = useAuth();
  const { data, error, loading } = useApi(async () => {
    const [guest, bookings] = await Promise.all([
      guestsApi.get(id),
      can('BOOKING_READ') ? bookingsApi.list({ guestId: id, size: 10, sort: 'checkInDate,desc' }) : { content: [] },
    ]);
    return { guest, bookings: bookings.content };
  }, [id]);

  const guest = data?.guest;
  const address = guest?.address;

  return (
    <Modal
      title="Ficha do hóspede"
      onClose={onClose}
      width={600}
      footer={guest && can('GUEST_UPDATE') ? <Button icon={Pencil} onClick={() => onEdit(guest)}>Editar cadastro</Button> : null}
    >
      {loading ? <Spinner /> : error ? <ErrorState error={error} /> : (
        <>
          <div className="cell-main">
            <Avatar name={guest.fullName} size="lg" />
            <div className="cell-stack">
              <strong style={{ fontSize: 17 }}>{guest.fullName}</strong>
              <span>CPF {formatCpf(guest.cpf)} · cliente desde {formatDate(guest.createdAt, { month: 'short', year: 'numeric' })}</span>
            </div>
          </div>
          <div className="area-meta">
            <span><Mail size={15} /> {guest.email}</span>
            <span><Phone size={15} /> {guest.phone}</span>
            {address?.city && <span><MapPin size={15} /> {[address.street, address.number].filter(Boolean).join(', ')} — {address.city}/{address.state}</span>}
          </div>
          <div>
            <p className="form-section" style={{ marginBottom: 8 }}>Últimas hospedagens</p>
            <div className="task-list">
              {data.bookings.length === 0 && <span className="muted">Nenhuma hospedagem registrada.</span>}
              {data.bookings.map((b) => (
                <div key={b.id} className="task" style={{ alignItems: 'center' }}>
                  <div className="task-body">
                    <strong>Quarto {b.room.number} · {formatDate(b.checkInDate)} a {formatDate(b.checkOutDate)}</strong>
                    <span>#RES-{b.id} · {formatMoney(b.totalAmount)}</span>
                  </div>
                  <StatusBadge map={BOOKING_STATUS} value={b.status} />
                </div>
              ))}
            </div>
          </div>
        </>
      )}
    </Modal>
  );
}

function GuestForm({ guest, onClose, onSaved }) {
  const isNew = !guest.id;
  const notify = useToast();
  const [saving, setSaving] = useState(false);
  const [errors, setErrors] = useState({});
  const [form, setForm] = useState({
    fullName: guest.fullName || '', email: guest.email || '', cpf: guest.cpf ? formatCpf(guest.cpf) : '', phone: guest.phone || '',
    birthDate: guest.birthDate || '', password: '',
    address: { street: '', number: '', complement: '', district: '', city: '', state: '', zipCode: '', ...(guest.address || {}) },
  });

  const set = (key) => (e) => setForm({ ...form, [key]: key === 'cpf' ? formatCpf(e.target.value) : e.target.value });
  const setAddress = (key) => (e) => setForm({ ...form, address: { ...form.address, [key]: e.target.value } });

  const submit = async (event) => {
    event.preventDefault();
    setErrors({});
    const hasAddress = Object.values(form.address).some(Boolean);
    const clean = (obj) => Object.fromEntries(Object.entries(obj).map(([k, v]) => [k, v === '' ? null : v]));
    const payload = { ...clean(form), address: hasAddress ? clean(form.address) : null };
    if (!isNew) {
      delete payload.cpf;
      delete payload.password;
    }
    setSaving(true);
    try {
      await (isNew ? guestsApi.create(payload) : guestsApi.update(guest.id, payload));
      notify(isNew ? 'Hóspede cadastrado com sucesso' : 'Cadastro atualizado');
      onSaved();
    } catch (error) {
      setErrors(Object.fromEntries(error.fieldErrors.map((f) => [f.field, f.message])));
      notify(error.message, 'error');
      setSaving(false);
    }
  };

  return (
    <Modal
      title={isNew ? 'Novo hóspede' : 'Editar hóspede'}
      subtitle={isNew ? 'O hóspede poderá entrar no sistema com o e-mail ou CPF e a senha inicial.' : guest.fullName}
      onClose={onClose}
      width={640}
      footer={(
        <>
          <Button onClick={onClose}>Cancelar</Button>
          <Button variant="primary" type="submit" form="guest-form" loading={saving}>{isNew ? 'Cadastrar' : 'Salvar'}</Button>
        </>
      )}
    >
      <form id="guest-form" className="form-grid" onSubmit={submit}>
        <Field label="Nome completo" className="full" error={errors.fullName}><input className="input" required value={form.fullName} onChange={set('fullName')} /></Field>
        <Field label="E-mail" error={errors.email}><input className="input" type="email" required value={form.email} onChange={set('email')} /></Field>
        <Field label="Telefone" error={errors.phone}><input className="input" required value={form.phone} onChange={set('phone')} placeholder="+55 48 99999-9999" /></Field>
        {isNew && <Field label="CPF" error={errors.cpf}><input className="input" required value={form.cpf} onChange={set('cpf')} placeholder="000.000.000-00" /></Field>}
        <Field label="Nascimento" error={errors.birthDate}><input className="input" type="date" value={form.birthDate || ''} onChange={set('birthDate')} /></Field>
        {isNew && <Field label="Senha inicial" error={errors.password} hint="Mínimo de 8 caracteres"><input className="input" type="password" required minLength={8} value={form.password} onChange={set('password')} /></Field>}

        <p className="form-section full">Endereço (opcional)</p>
        <Field label="Rua" className="full"><input className="input" value={form.address.street || ''} onChange={setAddress('street')} /></Field>
        <Field label="Número"><input className="input" value={form.address.number || ''} onChange={setAddress('number')} /></Field>
        <Field label="Bairro"><input className="input" value={form.address.district || ''} onChange={setAddress('district')} /></Field>
        <Field label="Cidade"><input className="input" value={form.address.city || ''} onChange={setAddress('city')} /></Field>
        <Field label="UF" error={errors['address.state']}><input className="input" maxLength={2} value={form.address.state || ''} onChange={setAddress('state')} /></Field>
        <Field label="CEP" error={errors['address.zipCode']}><input className="input" value={form.address.zipCode || ''} onChange={setAddress('zipCode')} /></Field>
      </form>
    </Modal>
  );
}
