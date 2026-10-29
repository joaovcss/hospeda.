import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import Layout from './components/Layout';
import { Spinner } from './components/ui';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Bookings from './pages/Bookings';
import Guests from './pages/Guests';
import Rooms from './pages/Rooms';
import Areas from './pages/Areas';
import AccessLogs from './pages/AccessLogs';
import Users from './pages/Users';
import Account from './pages/Account';
import GuestHome from './pages/GuestHome';
import MyBookings from './pages/MyBookings';

function Guard({ permission, children }) {
  const { can } = useAuth();
  return can(permission) ? children : <Navigate to="/" replace />;
}

export default function App() {
  const { user, loading, isGuest } = useAuth();

  if (loading) return <Spinner label="Carregando sua sessão..." />;
  if (!user) {
    return (
      <Routes>
        <Route path="*" element={<Login />} />
      </Routes>
    );
  }

  return (
    <Routes>
      <Route element={<Layout />}>
        {isGuest ? (
          <>
            <Route index element={<GuestHome />} />
            <Route path="minhas-reservas" element={<MyBookings />} />
          </>
        ) : (
          <>
            <Route index element={<Dashboard />} />
            <Route path="reservas" element={<Guard permission="BOOKING_READ"><Bookings /></Guard>} />
            <Route path="hospedes" element={<Guard permission="GUEST_READ"><Guests /></Guard>} />
            <Route path="quartos" element={<Guard permission="ROOM_READ"><Rooms /></Guard>} />
            <Route path="areas" element={<Guard permission="AREA_READ"><Areas /></Guard>} />
            <Route path="historico-acessos" element={<Guard permission="ACCESS_LOG_READ"><AccessLogs /></Guard>} />
            <Route path="usuarios" element={<Guard permission="USER_READ"><Users /></Guard>} />
          </>
        )}
        <Route path="conta" element={<Account />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
