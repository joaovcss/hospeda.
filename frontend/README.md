# hospeda. — Frontend

Painel web (React + Vite) do Sistema de Gestão Hoteleira. Consome a API Spring deste repositório.

## Executar

```bash
cd frontend
npm install
npm run dev
```

Acesse http://localhost:5173.

Antes, suba o PostgreSQL e a API em `http://localhost:8080` (veja o README da raiz). O Vite encaminha `/api` para ela, então não é preciso configurar CORS em desenvolvimento. Entre com um usuário cadastrado no banco (e-mail ou CPF e senha).

Build de produção: `npm run build` (gera `dist/`).

## Estrutura

```text
src/
├── api/          client.js (fetch + token JWT + endpoints), errors.js
├── auth/         AuthContext (login, sessão, permissões)
├── components/   Layout (sidebar + topbar), ui.jsx (cards, modais, badges, toasts), bookings.jsx (ações e modais de reserva)
├── pages/        Dashboard, Bookings, Guests, Rooms, Areas, AccessLogs, Users, Account, GuestHome, MyBookings
├── utils/        format.js (datas, moeda, rótulos), useApi.js
└── styles.css    Tema (cores, tipografia, componentes, responsivo)
```

## Telas

| Perfil | Telas |
|---|---|
| Administrador / Funcionário | Visão geral, Reservas, Hóspedes, Quartos (mapa por andar), Áreas e acessos (controle de entrada), Histórico de acessos, Equipe, Minha conta |
| Hóspede | Minha estadia, Minhas reservas (solicitar e cancelar), Minha conta |

Menus e botões aparecem conforme `user.permissions` retornado no login; a autorização real continua no backend.
O token fica em `sessionStorage` e é descartado ao sair ou ao receber `401`.
