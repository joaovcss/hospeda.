import { ApiError } from './errors';

const TOKEN_KEY = 'hospeda.token';

let onUnauthorized = () => {};

export { ApiError };

export const session = {
  getToken: () => sessionStorage.getItem(TOKEN_KEY),
  setToken: (token) => sessionStorage.setItem(TOKEN_KEY, token),
  clear: () => sessionStorage.removeItem(TOKEN_KEY),
};

export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler;
}

function buildQuery(params) {
  if (!params) return '';
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') query.append(key, value);
  });
  const text = query.toString();
  return text ? `?${text}` : '';
}

async function request(method, path, { params, body } = {}) {
  const url = path + buildQuery(params);

  const headers = { Accept: 'application/json' };
  const token = session.getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  let response;
  try {
    response = await fetch(url, { method, headers, body: body !== undefined ? JSON.stringify(body) : undefined });
  } catch {
    throw new ApiError(0, { message: 'Não foi possível conectar ao servidor. Verifique se o backend está rodando.' });
  }

  if (response.status === 204) return null;
  const data = await response.json().catch(() => null);

  if (!response.ok) {
    if (response.status === 401 && token) onUnauthorized();
    // Sem corpo JSON em erro 5xx: o proxy do Vite não alcançou a API.
    if (!data && response.status >= 500) {
      throw new ApiError(response.status, { message: 'Não foi possível conectar ao servidor. Verifique se o backend está rodando.' });
    }
    throw new ApiError(response.status, data);
  }
  return data;
}

export const api = {
  get: (path, params) => request('GET', path, { params }),
  post: (path, body) => request('POST', path, { body }),
  put: (path, body) => request('PUT', path, { body }),
  patch: (path, body) => request('PATCH', path, { body }),
  delete: (path) => request('DELETE', path),
};

/** Percorre todas as páginas de uma listagem paginada (máximo de 100 itens por página na API). */
export async function fetchAll(listFn, params = {}) {
  const items = [];
  let page = 0;
  let totalPages = 1;
  while (page < totalPages) {
    const result = await listFn({ ...params, page, size: 100 });
    items.push(...result.content);
    totalPages = result.totalPages;
    page += 1;
  }
  return items;
}

/* ---------- Endpoints ---------- */

export const authApi = {
  login: (login, password) => api.post('/api/auth/login', { login, password }),
  me: () => api.get('/api/auth/me'),
  changePassword: (currentPassword, newPassword) => api.put('/api/auth/password', { currentPassword, newPassword }),
};

export const roomsApi = {
  list: (params) => api.get('/api/rooms', { size: 100, sort: 'number,asc', ...params }),
  available: (checkIn, checkOut, guests) => api.get('/api/rooms/available', { checkIn, checkOut, guests }),
  create: (data) => api.post('/api/rooms', data),
  update: (id, data) => api.put(`/api/rooms/${id}`, data),
  setStatus: (id, status) => api.patch(`/api/rooms/${id}/status`, { status }),
};

export const bookingsApi = {
  list: (params) => api.get('/api/bookings', { size: 100, sort: 'checkInDate,asc', ...params }),
  mine: (scope = 'ALL') => api.get('/api/bookings/my-bookings', { scope, size: 100 }),
  get: (id) => api.get(`/api/bookings/${id}`),
  create: (data) => api.post('/api/bookings', data),
  confirm: (id) => api.patch(`/api/bookings/${id}/confirm`),
  checkIn: (id) => api.patch(`/api/bookings/${id}/check-in`),
  checkOut: (id) => api.patch(`/api/bookings/${id}/check-out`),
  cancel: (id, reason) => api.patch(`/api/bookings/${id}/cancel`, { reason }),
  areas: (id) => api.get(`/api/bookings/${id}/areas`),
  grantArea: (id, areaId) => api.post(`/api/bookings/${id}/areas`, { areaId }),
  revokeArea: (id, areaId) => api.delete(`/api/bookings/${id}/areas/${areaId}`),
};

export const guestsApi = {
  list: (params) => api.get('/api/guests', { size: 20, sort: 'id,desc', ...params }),
  get: (id) => api.get(`/api/guests/${id}`),
  me: () => api.get('/api/guests/me'),
  myAreas: () => api.get('/api/guests/me/areas'),
  checkAuthorization: (guestId, areaId) => api.get(`/api/guests/${guestId}/areas/${areaId}/authorization`),
  create: (data) => api.post('/api/guests', data),
  update: (id, data) => api.put(`/api/guests/${id}`, data),
};

export const areasApi = {
  list: (params) => api.get('/api/areas', { size: 100, sort: 'name,asc', ...params }),
  create: (data) => api.post('/api/areas', data),
  update: (id, data) => api.put(`/api/areas/${id}`, data),
  setStatus: (id, status) => api.patch(`/api/areas/${id}/status`, { status }),
};

export const accessApi = {
  validate: (guestId, areaId) => api.post('/api/access-logs/validate', { guestId, areaId }),
  logs: (params) => api.get('/api/access-logs', { size: 20, sort: 'attemptedAt,desc', ...params }),
  myLogs: () => api.get('/api/access-logs/me', { size: 50, sort: 'attemptedAt,desc' }),
};

export const usersApi = {
  list: (params) => api.get('/api/users', { size: 50, sort: 'id,asc', ...params }),
  create: (data) => api.post('/api/users', data),
  update: (id, data) => api.put(`/api/users/${id}`, data),
  setStatus: (id, active) => api.patch(`/api/users/${id}/status`, { active }),
  setPermissions: (id, permissions) => api.put(`/api/users/${id}/permissions`, { permissions }),
  permissions: () => api.get('/api/permissions'),
};
