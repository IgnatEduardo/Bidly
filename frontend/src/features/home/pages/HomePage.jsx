import React, { useState, useEffect } from 'react';
import API from '../../../api/axios';
import Dashboard from '../components/Dashboard';
import ListingDetails from '../components/ListingDetails';
import CreateListing from '../components/CreateListing';
import WalletPage from '../components/WalletPage';
import MyListings from '../components/MyListings';

const HomePage = ({ onLogout }) => {
  const username = localStorage.getItem('username') || 'User';
  const userId = localStorage.getItem('userId');
  
  const [activeTab, setActiveTab] = useState('dashboard'); // 'dashboard', 'details', 'create-listing', 'listings', 'bids', 'wallet'
  const [selectedListingId, setSelectedListingId] = useState(null);
  
  // User Profile details
  const [userKyc, setUserKyc] = useState(false);
  const [kycLoading, setKycLoading] = useState(false);

  // Top bar Wallet Balance
  const [walletBalance, setWalletBalance] = useState(0);

  // Stack of active push toasts
  const [toasts, setToasts] = useState([]);

  const addToast = (message, type = 'info') => {
    const id = Date.now() + Math.random();
    setToasts((prev) => [...prev, { id, message, type }]);
    setTimeout(() => {
      removeToast(id);
    }, 5000);
  };

  const removeToast = (id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };

  const fetchUserProfile = async () => {
    if (!userId) return;
    try {
      const response = await API.get(`/auth/users/${userId}`);
      setUserKyc(response.data.kycApproved || false);
      localStorage.setItem('kycApproved', response.data.kycApproved || false);
    } catch (err) {
      console.error('Failed to fetch user profile:', err);
    }
  };

  const fetchWalletBalance = async () => {
    if (!userId) return;
    try {
      const response = await API.get(`/auctions/wallets/${userId}`);
      setWalletBalance(response.data.balance || 0);
    } catch (err) {
      console.error('Failed to fetch wallet balance:', err);
    }
  };

  const handleToggleKyc = async () => {
    setKycLoading(true);
    try {
      const response = await API.post(`/auth/users/${userId}/kyc`, null, {
        params: { approved: !userKyc }
      });
      setUserKyc(response.data.kycApproved || false);
      localStorage.setItem('kycApproved', response.data.kycApproved || false);
    } catch (err) {
      console.error('Failed to toggle KYC status:', err);
    } finally {
      setKycLoading(false);
    }
  };

  // Fetch initial profile & wallet balance
  useEffect(() => {
    fetchUserProfile();
    fetchWalletBalance();
  }, [userId]);

  // Handle local trigger to refresh wallet balance when subcomponents complete transactions
  useEffect(() => {
    window.addEventListener('wallet-updated', fetchWalletBalance);
    return () => {
      window.removeEventListener('wallet-updated', fetchWalletBalance);
    };
  }, []);

  // Connect to global WebSocket to capture user transaction notifications
  useEffect(() => {
    if (!userId) return;
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//localhost:8080/ws/auctions`;
    
    console.log(`Connecting global homepage WebSocket: ${wsUrl}`);
    const socket = new WebSocket(wsUrl);

    socket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        // If it's a wallet transaction message for this user
        if (data.type === 'WALLET_TRANSACTION' && Number(data.userId) === Number(userId)) {
          let type = 'info';
          if (data.txType === 'DEPOSIT' || data.txType === 'RELEASE') {
            type = 'success';
          } else if (data.txType === 'LOCK' || data.txType === 'CHARGE') {
            type = 'danger';
          }
          addToast(data.message, type);
          fetchWalletBalance(); // Auto-refresh top bar balance
        }
      } catch (err) {
        console.error('Failed to parse global socket message:', err);
      }
    };

    return () => {
      socket.close();
    };
  }, [userId]);

  const handleLogout = () => {
    localStorage.clear();
    onLogout();
  };

  const navigateTo = (tab, listingId = null) => {
    setActiveTab(tab);
    if (listingId) {
      setSelectedListingId(listingId);
    }
    // Refresh balance on tab switch to be safe
    fetchWalletBalance();
  };

  return (
    <div className="home-container">
      {/* Stacked push notifications container */}
      <div className="toasts-container">
        {toasts.map((t) => (
          <div key={t.id} className={`live-toast toast-${t.type}`} onClick={() => removeToast(t.id)}>
            <span>{t.message}</span>
            <span className="toast-close">×</span>
          </div>
        ))}
      </div>

      {/* Navigation Bar */}
      <nav className="navbar">
        <div className="navbar-brand" onClick={() => navigateTo('dashboard')} style={{ cursor: 'pointer' }}>
          <span className="brand-logo">💎</span> Bidly
        </div>
        
        <div className="navbar-menu">
          <button 
            className={`menu-item-btn ${['dashboard', 'details', 'create-listing'].includes(activeTab) ? 'active' : ''}`}
            onClick={() => navigateTo('dashboard')}
          >
            Dashboard
          </button>
          <button 
            className={`menu-item-btn ${activeTab === 'listings' ? 'active' : ''}`}
            onClick={() => navigateTo('listings')}
          >
            My Listings
          </button>
          <button 
            className={`menu-item-btn ${activeTab === 'bids' ? 'active' : ''}`}
            onClick={() => navigateTo('bids')}
          >
            My Bids
          </button>
          <button 
            className={`menu-item-btn ${activeTab === 'wallet' ? 'active' : ''}`}
            onClick={() => navigateTo('wallet')}
          >
            Wallet
          </button>
        </div>
        
        <div className="navbar-user">
          {/* Top Bar Wallet Amount */}
          <div className="navbar-wallet-display" onClick={() => navigateTo('wallet')} title="Click to view wallet">
            💳 Available: <strong className="wallet-amt">${walletBalance.toLocaleString(undefined, { minimumFractionDigits: 2 })}</strong>
          </div>

          <div className="kyc-toggle-wrapper">
            <span className={`kyc-badge-pill ${userKyc ? 'verified' : 'unverified'}`}>
              {userKyc ? '✓ KYC Verified' : '⚠ Unverified'}
            </span>
            <button 
              className="kyc-toggle-btn" 
              onClick={handleToggleKyc} 
              disabled={kycLoading}
              title="Toggle KYC verification for Vehicles/Real Estate bidding"
            >
              {kycLoading ? '...' : 'Toggle KYC'}
            </button>
          </div>
          
          <span className="user-name">Welcome, <strong>{username}</strong></span>
          <button className="logout-btn" onClick={handleLogout}>Logout</button>
        </div>
      </nav>

      {/* Hero Section */}
      {['dashboard', 'listings', 'bids', 'wallet'].includes(activeTab) && (
        <header className="hero-section">
          <h1 className="hero-title">Premium Bidding Platform</h1>
          <p className="hero-subtitle">Discover, bid, and win high-value listings securely in real-time.</p>
          <div className="hero-badge">KYC Verified Accounts Enabled</div>
        </header>
      )}

      {/* Main Content Area */}
      <main className="main-content">
        {activeTab === 'dashboard' && (
          <Dashboard 
            onViewDetails={(id) => navigateTo('details', id)} 
            onCreateClick={() => navigateTo('create-listing')}
          />
        )}
        
        {activeTab === 'details' && (
          <ListingDetails 
            listingId={selectedListingId} 
            onBack={() => navigateTo('dashboard')} 
            addToast={addToast}
          />
        )}
        
        {activeTab === 'create-listing' && (
          <CreateListing 
            onBack={() => navigateTo('dashboard')} 
            onSuccess={() => navigateTo('dashboard')} 
          />
        )}
        
        {activeTab === 'listings' && (
          <MyListings 
            type="sales"
            onViewDetails={(id) => navigateTo('details', id)} 
          />
        )}

        {activeTab === 'bids' && (
          <MyListings 
            type="purchases"
            onViewDetails={(id) => navigateTo('details', id)} 
          />
        )}
        
        {activeTab === 'wallet' && (
          <WalletPage />
        )}
      </main>
    </div>
  );
};

export default HomePage;
