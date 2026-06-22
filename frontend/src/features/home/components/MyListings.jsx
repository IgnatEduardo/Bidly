import React, { useState, useEffect } from 'react';
import API from '../../../api/axios';

const MyListings = ({ type, onViewDetails }) => {
  const [sales, setSales] = useState([]);
  const [purchases, setPurchases] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  const [actionLoading, setActionLoading] = useState(false);
  const [actionMessage, setActionMessage] = useState('');
  const [actionError, setActionError] = useState('');
  
  const userId = Number(localStorage.getItem('userId'));

  const fetchMyListings = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await API.get('/auctions/listings', {
        params: { page: 0, size: 100 }
      });
      
      const allListings = response.data.content || [];
      
      // Filter sales: current user is the seller
      const userSales = allListings.filter(item => Number(item.sellerId) === userId);
      setSales(userSales);
      
      // Filter purchases: current user is the highest bidder (winner/bidding)
      const userPurchases = allListings.filter(item => {
        const session = item.biddingSession || {};
        return Number(item.sellerId) !== userId && Number(session.currentHighestBidderId) === userId;
      });
      setPurchases(userPurchases);
      
    } catch (err) {
      console.error(err);
      setError('Failed to fetch listings.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMyListings();
  }, [userId, type]);

  const handleConfirmSale = async (listingId, confirm) => {
    setActionLoading(true);
    setActionMessage('');
    setActionError('');
    try {
      await API.post(`/auctions/listings/${listingId}/confirm`, null, {
        params: {
          sellerId: userId,
          confirm: confirm
        }
      });
      setActionMessage(confirm ? 'Sale confirmed successfully!' : 'Sale rejected. Winner refunded.');
      
      // Dispatch custom global event so navbar updates wallet amount instantly
      window.dispatchEvent(new Event('wallet-updated'));
      
      fetchMyListings();
    } catch (err) {
      console.error(err);
      setActionError(err.response?.data?.message || 'Failed to perform confirmation action.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleCheckout = async (listingId) => {
    setActionLoading(true);
    setActionMessage('');
    setActionError('');
    try {
      await API.post(`/auctions/listings/${listingId}/checkout`, null, {
        params: {
          winnerId: userId
        }
      });
      setActionMessage('Checkout completed successfully! Balance charged and item paid.');
      
      // Dispatch custom global event so navbar updates wallet amount instantly
      window.dispatchEvent(new Event('wallet-updated'));
      
      fetchMyListings();
    } catch (err) {
      console.error(err);
      setActionError(err.response?.data?.message || 'Checkout failed. Make sure your wallet balance is sufficient for the remaining 90%.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleForfeit = async (listingId) => {
    setActionLoading(true);
    setActionMessage('');
    setActionError('');
    try {
      await API.post(`/auctions/listings/${listingId}/forfeit`);
      setActionMessage('Forfeiture penalty processed! Claimed 10% escrow from buyer as compensation.');
      
      // Dispatch custom global event so navbar updates wallet amount instantly
      window.dispatchEvent(new Event('wallet-updated'));
      
      fetchMyListings();
    } catch (err) {
      console.error(err);
      setActionError(err.response?.data?.message || 'Forfeiture action failed.');
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="loader-container">
        <div className="spinner"></div>
        <p>Loading listings...</p>
      </div>
    );
  }

  return (
    <div className="mylistings-container">
      {type === 'sales' ? (
        <>
          <h2>My Listings (Sales)</h2>
          <p className="mylistings-subtitle">Track and confirm transactions for your created auctions.</p>
        </>
      ) : (
        <>
          <h2>My Bids & Wins</h2>
          <p className="mylistings-subtitle">Track and complete checkouts for auctions where you lead.</p>
        </>
      )}

      {actionMessage && <div className="alert alert-success">{actionMessage}</div>}
      {actionError && <div className="alert alert-danger">{actionError}</div>}
      {error && <div className="alert alert-danger">{error}</div>}

      {/* Sales Mode */}
      {type === 'sales' && (
        sales.length === 0 ? (
          <div className="no-items-box">
            <span className="no-items-icon">🏷️</span>
            <h3>No Auctions Created</h3>
            <p>You haven't listed any items for bidding yet.</p>
          </div>
        ) : (
          <div className="mylistings-list">
            {sales.map((item) => {
              const session = item.biddingSession || {};
              const currentBid = session.currentHighestBid || 0;
              const isEnded = !session.active || new Date(session.endTime) <= new Date();
              
              return (
                <div key={item.id} className="mylisting-item">
                  <div className="item-details" onClick={() => onViewDetails(item.id)}>
                    {item.imageUrl ? (
                      <img src={item.imageUrl} alt={item.title} className="item-thumbnail" />
                    ) : (
                      <div className="item-thumbnail placeholder">💎</div>
                    )}
                    <div className="item-info">
                      <h4>{item.title}</h4>
                      <p className="item-meta">Category: {item.category}</p>
                      <p className="item-price">
                        {session.currentHighestBid ? 'Highest Bid: ' : 'Starting Price: '}
                        <strong>${(session.currentHighestBid || session.reservePrice || 0).toLocaleString()}</strong>
                      </p>
                    </div>
                  </div>

                  <div className="item-actions">
                    {!isEnded && (
                      <span className="badge-status active-badge">Bidding Active</span>
                    )}

                    {isEnded && !session.currentHighestBid && (
                      <span className="badge-status expired-badge">Ended (No Bids)</span>
                    )}

                    {isEnded && session.currentHighestBid && !item.confirmed && !item.paid && (
                      <div className="action-buttons-group">
                        <span className="badge-status pending-badge">Pending Confirmation</span>
                        <button
                          className="action-btn confirm"
                          onClick={() => handleConfirmSale(item.id, true)}
                          disabled={actionLoading}
                        >
                          Confirm Sale
                        </button>
                        <button
                          className="action-btn reject"
                          onClick={() => handleConfirmSale(item.id, false)}
                          disabled={actionLoading}
                        >
                          Reject Sale
                        </button>
                      </div>
                    )}

                    {isEnded && item.confirmed && !item.paid && (
                      <div className="action-buttons-group">
                        <span className="badge-status confirmed-badge">Sale Confirmed</span>
                        <span className="info-txt">Awaiting checkout...</span>
                        <button
                          className="action-btn forfeit"
                          onClick={() => handleForfeit(item.id)}
                          disabled={actionLoading}
                          title="If buyer defaults, forfeit their 10% escrow to your wallet as penalty"
                        >
                          Claim Forfeit Penalty
                        </button>
                      </div>
                    )}

                    {item.paid && (
                      <span className="badge-status completed-badge">Completed (Paid)</span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )
      )}

      {/* Purchases Mode */}
      {type === 'purchases' && (
        purchases.length === 0 ? (
          <div className="no-items-box">
            <span className="no-items-icon">🏆</span>
            <h3>No Bids Placed</h3>
            <p>You aren't currently winning or haven't won any auctions.</p>
          </div>
        ) : (
          <div className="mylistings-list">
            {purchases.map((item) => {
              const session = item.biddingSession || {};
              const currentBid = session.currentHighestBid || 0;
              const isEnded = !session.active || new Date(session.endTime) <= new Date();

              return (
                <div key={item.id} className="mylisting-item">
                  <div className="item-details" onClick={() => onViewDetails(item.id)}>
                    {item.imageUrl ? (
                      <img src={item.imageUrl} alt={item.title} className="item-thumbnail" />
                    ) : (
                      <div className="item-thumbnail placeholder">💎</div>
                    )}
                    <div className="item-info">
                      <h4>{item.title}</h4>
                      <p className="item-meta">Category: {item.category}</p>
                      <p className="item-price">
                        Your Winning Bid: <strong>${currentBid.toLocaleString()}</strong>
                      </p>
                    </div>
                  </div>

                  <div className="item-actions">
                    {!isEnded && (
                      <span className="badge-status active-badge">Highest Bidder (Active)</span>
                    )}

                    {isEnded && !item.confirmed && !item.paid && (
                      <span className="badge-status pending-badge">Won (Awaiting Seller Confirmation)</span>
                    )}

                    {isEnded && item.confirmed && !item.paid && (
                      <div className="action-buttons-group">
                        <span className="badge-status checkout-badge">Won & Confirmed</span>
                        <button
                          className="action-btn checkout"
                          onClick={() => handleCheckout(item.id)}
                          disabled={actionLoading}
                        >
                          Checkout & Pay Remaining 90%
                        </button>
                      </div>
                    )}

                    {item.paid && (
                      <span className="badge-status completed-badge">Purchased (Paid)</span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )
      )}
    </div>
  );
};

export default MyListings;
