import { useState } from 'react';
import { AlertCircle, Eye, EyeOff, LogIn, Sprout } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { Button, Field } from '../components/ui';

export default function Login() {
  const { login } = useAuth();
  const [form, setForm] = useState({ login: '', password: '' });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login(form.login.trim(), form.password);
    } catch (err) {
      setError(err.message);
      setLoading(false);
    }
  };

  return (
    <div className="login">
      <section className="login-art">
        <div className="brand">
          <span className="brand-mark"><Sprout size={22} /></span>
          <span className="brand-name">hospeda<span>.</span></span>
        </div>
        <div style={{ position: 'relative', zIndex: 1 }}>
          <h2>Cada hóspede bem recebido, <em>cada detalhe</em> no lugar.</h2>
          <p>Reservas, check-ins, quartos e acesso às áreas do hotel em um só painel, pensado para a rotina da recepção.</p>
        </div>
      </section>

      <section className="login-panel">
        <form className="login-form" onSubmit={submit}>
          <div>
            <h1>Bem-vindo de volta</h1>
          </div>
          <p>Entre com seu e-mail ou CPF para acessar o painel.</p>

          {error && <div className="alert alert-error"><AlertCircle size={16} /> {error}</div>}

          <Field label="E-mail ou CPF">
            <input className="input" autoComplete="username" value={form.login} onChange={(e) => setForm({ ...form, login: e.target.value })} placeholder="voce@hotel.com" required />
          </Field>
          <Field label="Senha">
            <div className="password-wrap">
              <input className="input" type={showPassword ? 'text' : 'password'} autoComplete="current-password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} placeholder="••••••••" required />
              <button type="button" onClick={() => setShowPassword((v) => !v)} aria-label="Mostrar senha">
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </Field>
          <Button type="submit" variant="primary" icon={LogIn} loading={loading}>Entrar</Button>
        </form>
      </section>
    </div>
  );
}
