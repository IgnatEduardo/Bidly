import React, { useState, useEffect } from 'react';
import API from '../../../api/axios';
import { useNavigate, useOutletContext } from 'react-router-dom';

const ProfilePage = () => {
  const userId = Number(localStorage.getItem('userId'));
  const navigate = useNavigate();
  
  // Grab addToast from parent route context
  const { addToast } = useOutletContext() || { addToast: () => {} };

  // Profile data states
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [kycApproved, setKycApproved] = useState(false);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Statistics states
  const [totalListings, setTotalListings] = useState(0);
  const [totalBids, setTotalBids] = useState(0);
  const [totalWins, setTotalWins] = useState(0);
  const [totalSpent, setTotalSpent] = useState(0);
  const [statsLoading, setStatsLoading] = useState(true);

  const fetchProfileAndStats = async () => {
    if (!userId) return;
    setLoading(true);
    setStatsLoading(true);
    setError('');

    // 1. Fetch profile details
    try {
      const profileResponse = await API.get(`/auth/users/${userId}`);
      setUsername(profileResponse.data.username || '');
      setEmail(profileResponse.data.email || '');
      setPhoneNumber(profileResponse.data.phoneNumber || '');
      setKycApproved(profileResponse.data.kycApproved || false);
    } catch (err) {
      console.error(err);
      setError('Failed to load profile details.');
      setLoading(false);
      setStatsLoading(false);
      return;
    } finally {
      setLoading(false);
    }

    // 2. Fetch all listings to calculate statistics
    try {
      const listingsResponse = await API.get('/auctions/listings', {
        params: { page: 0, size: 1000 }
      });
      const allListings = listingsResponse.data.content || [];

      // Listings count
      const userListings = allListings.filter(item => Number(item.sellerId) === userId);
      setTotalListings(userListings.length);

      // Bids & Wins metrics
      let bidsCount = 0;
      let winsCount = 0;
      let spentAccumulator = 0;

      allListings.forEach(item => {
        const session = item.biddingSession || {};
        const bidsList = session.bids || [];

        // Count user bids
        const userBids = bidsList.filter(b => Number(b.bidderId) === userId);
        bidsCount += userBids.length;

        // Check if user is highest bidder (active lead or ended winner)
        if (Number(session.currentHighestBidderId) === userId) {
          winsCount++;
          // Sum up paid transactions
          if (item.paid) {
            spentAccumulator += session.currentHighestBid || 0;
          }
        }
      });

      setTotalBids(bidsCount);
      setTotalWins(winsCount);
      setTotalSpent(spentAccumulator);

    } catch (err) {
      console.error('Failed to load platform statistics:', err);
      // We don't fail the entire page, we just log the error and leave stats at zero/empty
    } finally {
      setStatsLoading(false);
    }
  };

  useEffect(() => {
    fetchProfileAndStats();
  }, [userId]);

  const handleUpdate = async (e) => {
    e.preventDefault();
    setUpdating(true);
    setError('');
    setSuccess('');

    try {
      const response = await API.put(`/auth/users/${userId}`, {
        username,
        email,
        phoneNumber
      });
      
      const newUsername = response.data.user.username;
      localStorage.setItem('username', newUsername);
      
      // Emit event so the nav greeting updates dynamically
      window.dispatchEvent(new CustomEvent('username-updated', { detail: newUsername }));
      
      setSuccess('Profile updated successfully!');
      if (addToast) addToast('👤 Profile updated successfully!', 'success');
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || 'Failed to update profile details.');
    } finally {
      setUpdating(false);
    }
  };

  const handleDelete = async () => {
    const doubleConfirm = window.confirm(
      "🚨 DANGER ZONE: Are you sure you want to deactivate your account? \n\nThis will instantly terminate your active sessions, disable logins, and anonymize your account details permanently. This action cannot be undone."
    );
    if (!doubleConfirm) return;

    setUpdating(true);
    try {
      // 1. Deactivate or delete active listings from auction-service
      try {
        await API.delete(`/auctions/listings/seller/${userId}`);
      } catch (listingErr) {
        console.error('Failed to clean up listings during user deactivation:', listingErr);
      }

      // 2. Perform soft-delete/deactivation on auth-service
      await API.delete(`/auth/users/${userId}`);
      if (addToast) addToast('👋 Your account was successfully deactivated.', 'success');
      
      setTimeout(() => {
        localStorage.clear();
        window.location.href = '/login';
      }, 1500);
    } catch (err) {
      console.error(err);
      setError('Failed to deactivate account.');
      setUpdating(false);
    }
  };

  if (loading) {
    return (
      <div className="loader-container">
        <div className="spinner"></div>
        <p>Loading profile details...</p>
      </div>
    );
  }

  return (
    <div className="mylistings-container">
      <h2>My Account Profile</h2>
      <p className="mylistings-subtitle">Manage your personal credentials, view your platform statistics, and review security settings.</p>

      {error && <div className="alert alert-danger" style={{ marginBottom: '20px' }}>{error}</div>}
      {success && <div className="alert alert-success" style={{ marginBottom: '20px' }}>{success}</div>}

      <div className="form-grid" style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: '30px', marginTop: '20px' }}>
        
        {/* Left Column: Edit Form & Danger Zone */}
        <div className="form-section" style={{ backgroundColor: '#161b22', border: '1px solid #30363d', borderRadius: '12px', padding: '24px' }}>
          <h3 style={{ margin: '0 0 20px 0', borderBottom: '1px solid #21262d', paddingBottom: '10px', color: '#ffffff' }}>Account Details</h3>
          
          <form onSubmit={handleUpdate}>
            <div className="input-group">
              <label>Username</label>
              <input
                type="text"
                className="input-field"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                minLength={3}
                maxLength={25}
                disabled={updating}
              />
            </div>

            <div className="input-group" style={{ marginTop: '16px' }}>
              <label>Email Address</label>
              <input
                type="email"
                className="input-field"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                disabled={updating}
              />
            </div>

            <div className="input-group" style={{ marginTop: '16px' }}>
              <label>Phone Number</label>
              <input
                type="text"
                className="input-field"
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                disabled={updating}
              />
            </div>

            <div className="input-group" style={{ marginTop: '16px' }}>
              <label>KYC Verification Badge</label>
              <span className={`kyc-badge-pill ${kycApproved ? 'verified' : 'unverified'}`} style={{ display: 'inline-block', padding: '6px 12px', width: 'fit-content' }}>
                {kycApproved ? '✓ KYC Verified' : '⚠ Unverified'}
              </span>
            </div>

            <button
              type="submit"
              className="place-bid-btn"
              disabled={updating}
              style={{ marginTop: '24px', backgroundColor: 'var(--primary-color)', color: '#fff' }}
            >
              {updating ? 'Saving...' : 'Save Profile Changes'}
            </button>
          </form>

          {/* Danger Zone */}
          <div className="danger-zone-box" style={{ marginTop: '30px' }}>
            <h4>Danger Zone</h4>
            <p>Once you deactivate your account, there is no going back. All of your personal details will be scrambled to protect your privacy.</p>
            <button
              type="button"
              className="delete-account-btn"
              onClick={handleDelete}
              disabled={updating}
            >
              Deactivate My Account
            </button>
          </div>
        </div>

        {/* Right Column: Platform Statistics */}
        <div className="form-section" style={{ backgroundColor: '#161b22', border: '1px solid #30363d', borderRadius: '12px', padding: '24px' }}>
          <h3 style={{ margin: '0 0 20px 0', borderBottom: '1px solid #21262d', paddingBottom: '10px', color: '#ffffff' }}>Platform Statistics</h3>

          {statsLoading ? (
            <div style={{ textAlign: 'center', padding: '40px 0' }}>
              <div className="spinner" style={{ margin: '0 auto 10px auto' }}></div>
              <p>Computing stats...</p>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              
              {/* Stat card 1: Listings Published */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '16px', padding: '16px', background: '#212535', borderRadius: '8px', borderLeft: '4px solid var(--primary-color)' }}>
                <span style={{ fontSize: '2rem' }}>🏷️</span>
                <div>
                  <h4 style={{ margin: '0 0 4px 0', color: '#8b949e', fontSize: '0.85rem' }}>Auctions Created</h4>
                  <strong style={{ fontSize: '1.5rem', color: '#fff' }}>{totalListings}</strong>
                </div>
              </div>

              {/* Stat card 2: Total Bids Placed */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '16px', padding: '16px', background: '#212535', borderRadius: '8px', borderLeft: '4px solid var(--success-color)' }}>
                <span style={{ fontSize: '2rem' }}>💬</span>
                <div>
                  <h4 style={{ margin: '0 0 4px 0', color: '#8b949e', fontSize: '0.85rem' }}>Bids Placed</h4>
                  <strong style={{ fontSize: '1.5rem', color: '#fff' }}>{totalBids}</strong>
                </div>
              </div>

              {/* Stat card 3: Winning / Lead bids */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '16px', padding: '16px', background: '#212535', borderRadius: '8px', borderLeft: '4px solid #f2c94c' }}>
                <span style={{ fontSize: '2rem' }}>🏆</span>
                <div>
                  <h4 style={{ margin: '0 0 4px 0', color: '#8b949e', fontSize: '0.85rem' }}>Leading / Won Auctions</h4>
                  <strong style={{ fontSize: '1.5rem', color: '#fff' }}>{totalWins}</strong>
                </div>
              </div>

              {/* Stat card 4: Wallet Expenses */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '16px', padding: '16px', background: '#212535', borderRadius: '8px', borderLeft: '4px solid #eb5757' }}>
                <span style={{ fontSize: '2rem' }}>💸</span>
                <div>
                  <h4 style={{ margin: '0 0 4px 0', color: '#8b949e', fontSize: '0.85rem' }}>Total Spent (Wins)</h4>
                  <strong style={{ fontSize: '1.5rem', color: '#fff' }}>${totalSpent.toLocaleString(undefined, { minimumFractionDigits: 2 })}</strong>
                </div>
              </div>

            </div>
          )}
        </div>

      </div>
    </div>
  );
};

export default ProfilePage;
