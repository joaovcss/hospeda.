import { useState } from 'react';
import { KeyRound, UserCog, UserPlus } from 'lucide-react';
import { usersApi } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { useAction } from '../components/bookings';
import { Avatar, Badge, Button, Card, EmptyState, ErrorState, Field, Modal, PageHeader, Spinner, useToast } from '../components/ui';
import { PERMISSION_LABEL, USER_TYPE, formatDate } from '../utils/format';
import { useApi } from '../utils/useApi';

const GRANTABLE = ['USER_CREATE', 'USER_READ', 'USER_UPDATE', 'ACCESS_GRANT', 'ACCESS_REVOKE', 'ACCESS_LOG_READ'];

export default function Users() {
  const { user: me, isAdmin, can } = useAuth();
  const [creating, setCreating] = useState(false);
  const [permissionsFor, setPermissionsFor] = useState(null);
  const { run, running } = useAction();
  const { data, error, loading, reload } = useApi(() => usersApi.list(), []);

  const toggle = async (user) => {
    const result = await run(`status-${user.id}`, () => usersApi.setStatus(user.id, !user.active), user.active ? `${user.fullName} desativado` : `${user.fullName} reativado`);
    if (result) reload({ silent: true });
  };

  const staff = data?.content.filter((u) => u.userType !== 'GUEST') || [];

  return (
    <>
      <PageHeader title="Equipe" subtitle="Administradores e funcionários com acesso ao painel.">
        {can('USER_CREATE') && <Button variant="primary" icon={UserPlus} onClick={() => setCreating(true)}>Novo usuário</Button>}
      </PageHeader>

      <Card padded={false} className="card-table">
        {loading ? <Spinner /> : error ? <ErrorState error={error} onRetry={reload} /> : staff.length === 0 ? (
          <EmptyState icon={UserCog} title="Nenhum usuário encontrado" />
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>Usuário</th><th>Tipo</th><th>Permissões extras</th><th>Desde</th><th>Situação</th><th /></tr></thead>
              <tbody>
                {staff.map((u) => (
                  <tr key={u.id}>
                    <td>
                      <div className="cell-main">
                        <Avatar name={u.fullName} tone={u.userType === 'ADMIN' ? 'dark' : 'light'} />
                        <div className="cell-stack"><strong>{u.fullName}{u.id === me.id && ' (você)'}</strong><span>{u.email}</span></div>
                      </div>
                    </td>
                    <td>{USER_TYPE[u.userType]}</td>
                    <td>
                      {u.userType === 'ADMIN' ? <span className="muted">Acesso total</span> : u.extraPermissions.length === 0 ? <span className="muted">—</span> : (
                        <div className="chip-row">{u.extraPermissions.map((p) => <Badge key={p} tone="gray" dot={false}>{PERMISSION_LABEL[p]}</Badge>)}</div>
                      )}
                    </td>
                    <td className="muted">{formatDate(u.createdAt, { day: '2-digit', month: 'short', year: 'numeric' })}</td>
                    <td><Badge tone={u.active ? 'green' : 'gray'}>{u.active ? 'Ativo' : 'Inativo'}</Badge></td>
                    <td className="actions">
                      {isAdmin && u.userType === 'EMPLOYEE' && <Button size="sm" variant="ghost" icon={KeyRound} onClick={() => setPermissionsFor(u)}>Permissões</Button>}
                      {isAdmin && u.id !== me.id && (
                        <Button size="sm" variant={u.active ? 'ghost' : 'secondary'} loading={running === `status-${u.id}`} onClick={() => toggle(u)}>
                          {u.active ? 'Desativar' : 'Reativar'}
                        </Button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {creating && <UserForm onClose={() => setCreating(false)} onSaved={() => { setCreating(false); reload({ silent: true }); }} />}
      {permissionsFor && <PermissionsModal user={permissionsFor} onClose={() => setPermissionsFor(null)} onSaved={() => { setPermissionsFor(null); reload({ silent: true }); }} />}
    </>
  );
}

function PermissionsChecklist({ value, onChange }) {
  const toggle = (permission) => onChange(value.includes(permission) ? value.filter((p) => p !== permission) : [...value, permission]);
  return (
    <div>
      {GRANTABLE.map((p) => (
        <label key={p} className="checkbox">
          <input type="checkbox" checked={value.includes(p)} onChange={() => toggle(p)} />
          <span>{PERMISSION_LABEL[p]}<small>{p}</small></span>
        </label>
      ))}
    </div>
  );
}

function PermissionsModal({ user, onClose, onSaved }) {
  const [permissions, setPermissions] = useState(user.extraPermissions);
  const { run, running } = useAction();
  const save = async () => {
    const result = await run('save', () => usersApi.setPermissions(user.id, permissions), 'Permissões atualizadas');
    if (result) onSaved();
  };
  return (
    <Modal
      title="Permissões extras"
      subtitle={`${user.fullName} já possui as permissões operacionais padrão de funcionário.`}
      onClose={onClose}
      width={460}
      footer={<><Button onClick={onClose}>Cancelar</Button><Button variant="primary" loading={running === 'save'} onClick={save}>Salvar</Button></>}
    >
      <PermissionsChecklist value={permissions} onChange={setPermissions} />
    </Modal>
  );
}

function UserForm({ onClose, onSaved }) {
  const { isAdmin } = useAuth();
  const notify = useToast();
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({ fullName: '', email: '', password: '', userType: 'EMPLOYEE', permissions: [] });
  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    try {
      await usersApi.create({ ...form, permissions: form.userType === 'EMPLOYEE' ? form.permissions : [] });
      notify(`${form.fullName} adicionado à equipe`);
      onSaved();
    } catch (error) {
      notify(error.message, 'error');
      setSaving(false);
    }
  };

  return (
    <Modal
      title="Novo usuário"
      subtitle="Hóspedes são cadastrados na tela de Hóspedes."
      onClose={onClose}
      footer={<><Button onClick={onClose}>Cancelar</Button><Button variant="primary" type="submit" form="user-form" loading={saving}>Criar usuário</Button></>}
    >
      <form id="user-form" className="form-grid" onSubmit={submit}>
        <Field label="Nome completo" className="full"><input className="input" required maxLength={150} value={form.fullName} onChange={set('fullName')} /></Field>
        <Field label="E-mail" className="full"><input className="input" type="email" required value={form.email} onChange={set('email')} /></Field>
        <Field label="Senha inicial" hint="Mínimo de 8 caracteres"><input className="input" type="password" required minLength={8} value={form.password} onChange={set('password')} /></Field>
        <Field label="Tipo">
          <select className="select" value={form.userType} onChange={set('userType')} disabled={!isAdmin}>
            <option value="EMPLOYEE">Funcionário</option>
            <option value="ADMIN">Administrador</option>
          </select>
        </Field>
        {isAdmin && form.userType === 'EMPLOYEE' && (
          <div className="full">
            <p className="form-section" style={{ marginBottom: 8 }}>Permissões extras</p>
            <PermissionsChecklist value={form.permissions} onChange={(permissions) => setForm({ ...form, permissions })} />
          </div>
        )}
      </form>
    </Modal>
  );
}
