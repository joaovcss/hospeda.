import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { authApi, session, setUnauthorizedHandler } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(Boolean(session.getToken()));

  const logout = useCallback(() => {
    session.clear();
    setUser(null);
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(logout);
    if (!session.getToken()) return;
    authApi.me()
      .then(setUser)
      .catch(logout)
      .finally(() => setLoading(false));
  }, [logout]);

  const login = useCallback(async (loginValue, password) => {
    const response = await authApi.login(loginValue, password);
    session.setToken(response.accessToken);
    setUser(response.user);
    return response.user;
  }, []);

  const value = useMemo(() => ({
    user,
    loading,
    login,
    logout,
    isAdmin: user?.userType === 'ADMIN',
    isGuest: user?.userType === 'GUEST',
    can: (permission) => Boolean(user?.permissions?.includes(permission)),
  }), [user, loading, login, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
