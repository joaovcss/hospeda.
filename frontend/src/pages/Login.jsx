import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { AlertCircle, ArrowLeft, Eye, EyeOff, LogIn, Sprout, UserPlus } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { Button, Field } from '../components/ui';

const EMPTY_SIGNUP = { fullName: '', email: '', cpf: '', phone: '', birthDate: '', password: '' };

export default function Login() {
  const { login, register } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();

  const [mode, setMode] = useState(params.get('cadastro') ? 'register' : 'login');
  const [form, setForm] = useState({ login: '', password: '' });
  const [signup, setSignup] = useState(EMPTY_SIGNUP);
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const submitLogin = async (event) => {
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

  const submitSignup = async (event) => {
    event.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await register({
        fullName: signup.fullName.trim(),
        email: signup.email.trim(),
        cpf: signup.cpf.trim(),
        phone: signup.phone.trim(),
        birthDate: signup.birthDate || undefined,
        password: signup.password,
      });
    } catch (err) {
      setError(err.message);
      setLoading(false);
    }
  };

  const switchMode = (next) => {
    setMode(next);
    setError(null);
    setShowPassword(false);
  };

  return (
    <div className="login">
      <section className="login-art">
        <button type="button" className="brand brand-link" onClick={() => navigate('/')} aria-label="Voltar para a página inicial">
          <span className="brand-mark"><Sprout size={22} /></span>
          <span className="brand-name">hospeda<span>.</span></span>
        </button>
        <div style={{ position: 'relative', zIndex: 1 }}>
          <h2>Cada hóspede bem recebido, <em>cada detalhe</em> no lugar.</h2>
          <p>Reservas, check-ins, quartos e acesso às áreas do hotel em um só painel, pensado para a rotina da recepção.</p>
        </div>
      </section>

      <section className="login-panel">
        <button type="button" className="login-back" onClick={() => navigate('/')}>
          <ArrowLeft size={15} /> Voltar ao início
        </button>

        {mode === 'login' ? (
          <form className="login-form" onSubmit={submitLogin}>
            <h1>Bem-vindo de volta</h1>
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

            <p className="login-switch">
              É hóspede e ainda não tem conta?{' '}
              <button type="button" onClick={() => switchMode('register')}>Cadastre-se</button>
            </p>
          </form>
        ) : (
          <form className="login-form" onSubmit={submitSignup}>
            <h1>Criar conta de hóspede</h1>
            <p>Preencha seus dados para acompanhar suas reservas e acessos.</p>

            {error && <div className="alert alert-error"><AlertCircle size={16} /> {error}</div>}

            <Field label="Nome completo">
              <input className="input" autoComplete="name" value={signup.fullName} onChange={(e) => setSignup({ ...signup, fullName: e.target.value })} placeholder="Maria da Silva" required />
            </Field>
            <Field label="E-mail">
              <input className="input" type="email" autoComplete="email" value={signup.email} onChange={(e) => setSignup({ ...signup, email: e.target.value })} placeholder="voce@email.com" required />
            </Field>
            <div className="login-row">
              <Field label="CPF">
                <input className="input" value={signup.cpf} onChange={(e) => setSignup({ ...signup, cpf: e.target.value })} placeholder="000.000.000-00" required />
              </Field>
              <Field label="Telefone">
                <input className="input" autoComplete="tel" value={signup.phone} onChange={(e) => setSignup({ ...signup, phone: e.target.value })} placeholder="+55 11 98888-7777" required />
              </Field>
            </div>
            <Field label="Data de nascimento" hint="Opcional">
              <input className="input" type="date" value={signup.birthDate} onChange={(e) => setSignup({ ...signup, birthDate: e.target.value })} max="2099-12-31" />
            </Field>
            <Field label="Senha" hint="Mínimo de 8 caracteres">
              <div className="password-wrap">
                <input className="input" type={showPassword ? 'text' : 'password'} autoComplete="new-password" value={signup.password} onChange={(e) => setSignup({ ...signup, password: e.target.value })} placeholder="••••••••" minLength={8} required />
                <button type="button" onClick={() => setShowPassword((v) => !v)} aria-label="Mostrar senha">
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </Field>
            <Button type="submit" variant="primary" icon={UserPlus} loading={loading}>Criar conta e entrar</Button>

            <p className="login-switch">
              Já tem conta?{' '}
              <button type="button" onClick={() => switchMode('login')}>Entrar</button>
            </p>
          </form>
        )}
      </section>
    </div>
  );
}
