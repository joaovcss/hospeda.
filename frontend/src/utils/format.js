export function toISODate(date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export function addDays(isoDate, days) {
  const [y, m, d] = isoDate.split('-').map(Number);
  return toISODate(new Date(y, m - 1, d + days));
}

export const todayISO = () => toISODate(new Date());

export function parseISODate(isoDate) {
  const [y, m, d] = isoDate.slice(0, 10).split('-').map(Number);
  return new Date(y, m - 1, d);
}

const money = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const moneyShort = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 });

export const formatMoney = (value, short = false) => (short ? moneyShort : money).format(Number(value || 0));

export function formatDate(isoDate, options = { day: '2-digit', month: 'short' }) {
  if (!isoDate) return '—';
  return parseISODate(isoDate).toLocaleDateString('pt-BR', options).replace('.', '');
}

export function formatDateTime(isoDateTime) {
  if (!isoDateTime) return '—';
  const date = new Date(isoDateTime);
  return `${date.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })} às ${date.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}`;
}

export const formatTime = (value) => (value ? value.slice(0, 5) : '—');

export function initials(name = '') {
  const parts = name.trim().split(/\s+/);
  return ((parts[0]?.[0] || '') + (parts.length > 1 ? parts[parts.length - 1][0] : '')).toUpperCase();
}

export function greeting() {
  const hour = new Date().getHours();
  if (hour < 12) return 'Bom dia';
  if (hour < 18) return 'Boa tarde';
  return 'Boa noite';
}

export function formatCpf(value = '') {
  const d = value.replace(/\D/g, '').slice(0, 11);
  return d.replace(/(\d{3})(\d)/, '$1.$2').replace(/(\d{3})(\d)/, '$1.$2').replace(/(\d{3})(\d{1,2})$/, '$1-$2');
}

export const BOOKING_STATUS = {
  PENDING: { label: 'Pendente', tone: 'amber' },
  CONFIRMED: { label: 'Confirmada', tone: 'green' },
  CHECKED_IN: { label: 'Hospedado', tone: 'blue' },
  CHECKED_OUT: { label: 'Finalizada', tone: 'gray' },
  CANCELLED: { label: 'Cancelada', tone: 'red' },
};

export const ROOM_STATUS = {
  AVAILABLE: { label: 'Disponível', tone: 'green' },
  OCCUPIED: { label: 'Ocupado', tone: 'dark' },
  MAINTENANCE: { label: 'Manutenção', tone: 'amber' },
  INACTIVE: { label: 'Inativo', tone: 'gray' },
};

export const AREA_STATUS = {
  ACTIVE: { label: 'Em funcionamento', tone: 'green' },
  UNAVAILABLE: { label: 'Indisponível', tone: 'amber' },
  INACTIVE: { label: 'Inativa', tone: 'gray' },
};

export const ROOM_CATEGORY = {
  STANDARD: 'Standard',
  SUPERIOR: 'Superior',
  DELUXE: 'Deluxe',
  SUITE: 'Suíte',
};

export const USER_TYPE = {
  ADMIN: 'Administrador',
  EMPLOYEE: 'Funcionário',
  GUEST: 'Hóspede',
};

export const PERMISSION_LABEL = {
  USER_CREATE: 'Criar usuários',
  USER_READ: 'Consultar usuários',
  USER_UPDATE: 'Editar usuários',
  USER_STATUS_UPDATE: 'Ativar/desativar usuários',
  PERMISSION_MANAGE: 'Gerenciar permissões',
  ROOM_CREATE: 'Cadastrar quartos',
  ROOM_UPDATE: 'Editar quartos',
  ROOM_READ: 'Consultar quartos',
  AREA_CREATE: 'Cadastrar áreas',
  AREA_UPDATE: 'Editar áreas',
  AREA_READ: 'Consultar áreas',
  GUEST_CREATE: 'Cadastrar hóspedes',
  GUEST_READ: 'Consultar hóspedes',
  GUEST_UPDATE: 'Editar hóspedes',
  BOOKING_CREATE: 'Criar reservas',
  BOOKING_READ: 'Consultar reservas',
  BOOKING_CONFIRM: 'Confirmar reservas',
  BOOKING_CANCEL: 'Cancelar reservas',
  CHECKIN: 'Realizar check-in',
  CHECKOUT: 'Realizar check-out',
  ACCESS_GRANT: 'Conceder acesso a áreas',
  ACCESS_REVOKE: 'Revogar acesso a áreas',
  ACCESS_READ: 'Consultar acessos',
  ACCESS_REGISTER: 'Registrar entrada em áreas',
  ACCESS_LOG_READ: 'Consultar histórico de acessos',
};
