import React, { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useNavigate, useParams, useOutletContext } from 'react-router-dom';
import LoginPage from './features/auth/pages/LoginPage';
import ConfirmAccountPage from './features/auth/pages/ConfirmAccountPage'; 
import HomePage from './features/home/pages/HomePage';
import Dashboard from './features/home/components/Dashboard';
import ListingDetails from './features/home/components/ListingDetails';
import CreateListing from './features/home/components/CreateListing';
import MyListings from './features/home/components/MyListings';
import WalletPage from './features/home/components/WalletPage';

// Wrapper components to map route context / parameters to component props
function DashboardWrapper() {
  const navigate = useNavigate();
  return (
    <Dashboard 
      onViewDetails={(id) => navigate(`/listings/${id}`)}
      onCreateClick={() => navigate('/listings/new')}
    />
  );
}

function CreateListingWrapper() {
  const navigate = useNavigate();
  return (
    <CreateListing 
      onBack={() => navigate('/home')}
      onSuccess={() => navigate('/home')}
    />
  );
}

function ListingDetailsWrapper() {
  const navigate = useNavigate();
  const { id } = useParams();
  const { addToast } = useOutletContext();
  return (
    <ListingDetails 
      listingId={parseInt(id)}
      onBack={() => navigate('/home')}
      addToast={addToast}
    />
  );
}

function MyListingsWrapper({ type }) {
  const navigate = useNavigate();
  return (
    <MyListings 
      type={type}
      onViewDetails={(id) => navigate(`/listings/${id}`)}
    />
  );
}

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(() => !!localStorage.getItem('accessToken'));

  useEffect(() => {
    const handleAuthChange = () => {
      setIsAuthenticated(!!localStorage.getItem('accessToken'));
    };
    window.addEventListener('storage', handleAuthChange);
    return () => window.removeEventListener('storage', handleAuthChange);
  }, []);

  const handleLoginSuccess = () => {
    setIsAuthenticated(true);
  };

  const handleLogout = () => {
    localStorage.clear();
    setIsAuthenticated(false);
  };

  return (
    <BrowserRouter>
      <Routes>
        {/* Auth Route */}
        <Route 
          path="/login" 
          element={isAuthenticated ? <Navigate to="/home" replace /> : <LoginPage onLoginSuccess={handleLoginSuccess} />} 
        />

          {/* Confirmation Route */}
        <Route 
          path="/confirm-account" 
          element={<ConfirmAccountPage />} 
        />

        {/* Protected Routes inside HomePage Layout */}
        <Route 
          path="/" 
          element={isAuthenticated ? <HomePage onLogout={handleLogout} /> : <Navigate to="/login" replace />}
        >
          <Route index element={<Navigate to="/home" replace />} />
          <Route path="home" element={<DashboardWrapper />} />
          <Route path="listings/new" element={<CreateListingWrapper />} />
          <Route path="listings/:id" element={<ListingDetailsWrapper />} />
          <Route path="my-listings" element={<MyListingsWrapper type="sales" />} />
          <Route path="my-bids" element={<MyListingsWrapper type="purchases" />} />
          <Route path="wallet" element={<WalletPage />} />
          <Route path="*" element={<Navigate to="/home" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;