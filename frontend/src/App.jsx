import React, { useState, useEffect } from 'react';
import LoginPage from './features/auth/pages/LoginPage';
import HomePage from './features/home/pages/HomePage';

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      setIsAuthenticated(true);
    }
  }, []);

  return (
    <>
      {isAuthenticated ? (
        <HomePage onLogout={() => setIsAuthenticated(false)} />
      ) : (
        <LoginPage onLoginSuccess={() => setIsAuthenticated(true)} />
      )}
    </>
  );
}

export default App;