import { useNavigate } from 'react-router-dom';
import {
  Sprout, LogIn, UserPlus, ArrowRight, CalendarCheck, BedDouble,
  DoorOpen, ShieldCheck, Users, KeyRound,
} from 'lucide-react';
import { Button } from '../components/ui';

const FEATURES = [
  { icon: CalendarCheck, title: 'Reservas e check-in', text: 'Crie reservas, confirme, faça check-in e check-out acompanhando cada hospedagem do começo ao fim.' },
  { icon: BedDouble, title: 'Quartos e disponibilidade', text: 'Veja quartos livres por período, controle manutenção e ocupação em um mapa sempre atualizado.' },
  { icon: DoorOpen, title: 'Áreas e acessos', text: 'Autorize o hóspede a usar piscina, academia e outras áreas, com registro de cada entrada.' },
  { icon: ShieldCheck, title: 'Controle por permissões', text: 'Administradores e funcionários com acessos sob medida — cada um enxerga só o que precisa.' },
];

export default function Landing() {
  const navigate = useNavigate();

  return (
    <div className="lp">
      <header className="lp-nav">
        <div className="brand">
          <span className="brand-mark"><Sprout size={22} /></span>
          <span className="brand-name">hospeda<span>.</span></span>
        </div>
        <div className="lp-nav-actions">
          <Button variant="ghost" icon={LogIn} onClick={() => navigate('/login')}>Entrar</Button>
          <Button variant="primary" icon={UserPlus} onClick={() => navigate('/login?cadastro=1')}>Criar conta</Button>
        </div>
      </header>

      <main className="lp-hero">
        <div className="lp-hero-text">
          <span className="lp-eyebrow"><KeyRound size={14} /> Gestão hoteleira completa</span>
          <h1>Cada hóspede bem recebido, <em>cada detalhe</em> no lugar.</h1>
          <p>
            O hospeda reúne reservas, check-ins, quartos e acesso às áreas do hotel em um só painel —
            pensado para a rotina da recepção e também para o hóspede acompanhar a própria estadia.
          </p>
          <div className="lp-cta">
            <Button variant="primary" size="md" icon={LogIn} onClick={() => navigate('/login')}>
              Acessar o sistema
            </Button>
            <Button variant="secondary" size="md" icon={UserPlus} onClick={() => navigate('/login?cadastro=1')}>
              Sou hóspede, quero me cadastrar
            </Button>
          </div>
          <div className="lp-hint">
            <Users size={15} />
            <span>Hóspedes criam a própria conta e acompanham reservas, áreas liberadas e acessos.</span>
          </div>
        </div>

        <aside className="lp-hero-card">
          <div className="lp-card-top">
            <span className="brand-mark"><Sprout size={18} /></span>
            <div>
              <strong>Suíte 204 · pronta</strong>
              <span>Check-in confirmado para hoje</span>
            </div>
          </div>
          <ul className="lp-card-list">
            <li><CalendarCheck size={16} /> 3 check-ins agendados</li>
            <li><BedDouble size={16} /> 18 de 24 quartos ocupados</li>
            <li><DoorOpen size={16} /> Piscina e academia liberadas</li>
          </ul>
          <button type="button" className="lp-card-link" onClick={() => navigate('/login')}>
            Entrar no painel <ArrowRight size={15} />
          </button>
        </aside>
      </main>

      <section className="lp-features">
        {FEATURES.map(({ icon: Icon, title, text }) => (
          <article key={title} className="lp-feature">
            <span className="lp-feature-icon"><Icon size={20} /></span>
            <h3>{title}</h3>
            <p>{text}</p>
          </article>
        ))}
      </section>

      <section className="lp-bottom">
        <div>
          <h2>Pronto para começar?</h2>
          <p>Entre com sua conta da equipe ou crie seu acesso de hóspede em menos de um minuto.</p>
        </div>
        <div className="lp-cta">
          <Button variant="dark" size="md" icon={LogIn} onClick={() => navigate('/login')}>Entrar</Button>
          <Button variant="primary" size="md" icon={UserPlus} onClick={() => navigate('/login?cadastro=1')}>Criar conta de hóspede</Button>
        </div>
      </section>

      <footer className="lp-footer">
        <span className="brand-name">hospeda<span>.</span></span>
        <span>Sistema de gestão hoteleira</span>
      </footer>
    </div>
  );
}
