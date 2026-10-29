import { useState } from 'react';
import { KeyRound } from 'lucide-react';
import { authApi } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Avatar, Badge, Button, Card, Field, PageHeader, useToast } from '../components/ui';
import { PERMISSION_LABEL, USER_TYPE } from '../utils/format';

export default function Account() {
  const { user } = useAuth();
  const notify = useToast();
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirm: '' });
  const [saving, setSaving] = useState(false);
  const mismatch = form.confirm && form.confirm !== form.newPassword;

  const submit = async (event) => {
    event.preventDefault();
    if (mismatch) return;
    setSaving(true);
    try {
      await authApi.changePassword(form.currentPassword, form.newPassword);
      notify('Senha alterada com sucesso');
      setForm({ currentPassword: '', newPassword: '', confirm: '' });
    } catch (error) {
      notify(error.message, 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <PageHeader title="Minha conta" subtitle="Seus dados de acesso e segurança." />
      <div className="grid split">
        <Card title="Alterar senha" subtitle="Use pelo menos 8 caracteres, misturando letras, números e símbolos.">
          <form className="form-grid" onSubmit={submit}>
            <Field label="Senha atual" className="full"><input className="input" type="password" required autoComplete="current-password" value={form.currentPassword} onChange={(e) => setForm({ ...form, currentPassword: e.target.value })} /></Field>
            <Field label="Nova senha"><input className="input" type="password" required minLength={8} maxLength={72} autoComplete="new-password" value={form.newPassword} onChange={(e) => setForm({ ...form, newPassword: e.target.value })} /></Field>
            <Field label="Confirme a nova senha" error={mismatch ? 'As senhas não conferem' : null}><input className="input" type="password" required autoComplete="new-password" value={form.confirm} onChange={(e) => setForm({ ...form, confirm: e.target.value })} /></Field>
            <div className="full"><Button variant="primary" type="submit" icon={KeyRound} loading={saving} disabled={mismatch}>Salvar nova senha</Button></div>
          </form>
        </Card>
        <Card title="Perfil">
          <div className="cell-main" style={{ marginBottom: 18 }}>
            <Avatar name={user.fullName} size="lg" tone="dark" />
            <div className="cell-stack"><strong style={{ fontSize: 16 }}>{user.fullName}</strong><span>{user.email}</span></div>
          </div>
          <dl className="details" style={{ marginBottom: 16 }}>
            <div><dt>Tipo de acesso</dt><dd>{USER_TYPE[user.userType]}</dd></div>
            <div><dt>Permissões</dt><dd>{user.permissions.length}</dd></div>
          </dl>
          <div className="chip-row">
            {user.permissions.map((p) => <Badge key={p} tone="gray" dot={false}>{PERMISSION_LABEL[p] || p}</Badge>)}
          </div>
        </Card>
      </div>
    </>
  );
}
