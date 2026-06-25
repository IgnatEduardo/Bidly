import React, { useState, useEffect, useRef } from 'react';
import API from '../../../api/axios';

const ListingDetails = ({ listingId, onBack, addToast }) => {
  const [listing, setListing] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [bidAmount, setBidAmount] = useState('');
  const [bidLoading, setBidLoading] = useState(false);
  const [bidError, setBidError] = useState('');
  const [bidSuccess, setBidSuccess] = useState('');

  // Scheduling states
  const [showScheduleForm, setShowScheduleForm] = useState(false);
  const [scheduleStartTime, setScheduleStartTime] = useState('');
  const [scheduleEndTime, setScheduleEndTime] = useState('');
  const [scheduleBuyItNowPrice, setScheduleBuyItNowPrice] = useState('');
  const [scheduleLoading, setScheduleLoading] = useState(false);
  const [scheduleError, setScheduleError] = useState('');
  const [scheduleSuccess, setScheduleSuccess] = useState('');

  const wsRef = useRef(null);
  const userId = Number(localStorage.getItem('userId'));

  const getNowString = () => {
    const now = new Date();
    return new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
  };

  const getSixMonthsLaterString = (baseDateStr) => {
    if (!baseDateStr) return '';
    const date = new Date(baseDateStr);
    date.setMonth(date.getMonth() + 6);
    return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
  };

  // Fetch listing
  const fetchListingDetails = async () => {
    try {
      const response = await API.get(`/auctions/listings/${listingId}`);
      const data = response.data;
      setListing(data);
      if (data.biddingSession) {
        setScheduleStartTime(data.biddingSession.startTime ? data.biddingSession.startTime.slice(0, 16) : '');
        setScheduleEndTime(data.biddingSession.endTime ? data.biddingSession.endTime.slice(0, 16) : '');
        setScheduleBuyItNowPrice(data.biddingSession.buyItNowPrice || '');
      }
    } catch (err) {
      console.error(err);
      setError('Failed to fetch details for this listing.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchListingDetails();
  }, [listingId]);

  // Connect to WebSocket via Gateway for real-time list refreshes
  useEffect(() => {
    const apiHost = import.meta.env.VITE_API_URL || 'http://localhost:8080';
    const wsProtocol = apiHost.startsWith('https') ? 'wss:' : 'ws:';
    const hostOnly = apiHost.replace(/^https?:\/\//, '').replace(/\/$/, '');
    const wsUrl = `${wsProtocol}//${hostOnly}/ws/auctions`;
    
    console.log(`Connecting details to WebSocket: ${wsUrl}`);
    const socket = new WebSocket(wsUrl);
    wsRef.current = socket;

    socket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        console.log('Received WebSocket message in details:', data);
        
        if (data.listingId && Number(data.listingId) === Number(listingId)) {
          // Instantly refresh from server to grab clean bids list with usernames & sniping extensions
          fetchListingDetails();

          if (data.type === 'BID_PLACED') {
            if (addToast) {
              addToast(`🎉 A new bid of $${data.amount} was placed!`, 'success');
            }
          } else if (data.type === 'AUCTION_ENDED') {
            if (addToast) {
              addToast(`🛑 The auction has closed!`, 'danger');
            }
          }
        }
      } catch (err) {
        console.error('Failed to parse WebSocket message:', err);
      }
    };

    return () => {
      if (socket) {
        socket.close();
      }
    };
  }, [listingId, addToast]);

  // Timer Countdown
  const Countdown = ({ endTime, isActive }) => {
    const calculateTimeLeft = () => {
      if (!isActive) return { expired: true, text: 'Ended' };
      const difference = +new Date(endTime) - +new Date();
      if (difference <= 0) return { expired: true, text: 'Ended' };
      
      let days = Math.floor(difference / (1000 * 60 * 60 * 24));
      let hours = Math.floor((difference / (1000 * 60 * 60)) % 24);
      let minutes = Math.floor((difference / 1000 / 60) % 60);
      let seconds = Math.floor((difference / 1000) % 60);
      
      let text = '';
      if (days > 0) text += `${days}d `;
      text += `${hours.toString().padStart(2, '0')}h ${minutes.toString().padStart(2, '0')}m ${seconds.toString().padStart(2, '0')}s`;
      
      return { expired: false, text };
    };

    const [timeLeft, setTimeLeft] = useState(calculateTimeLeft());

    useEffect(() => {
      const timer = setInterval(() => {
        setTimeLeft(calculateTimeLeft());
      }, 1000);
      return () => clearInterval(timer);
    }, [endTime, isActive]);

    return (
      <div className={`details-timer ${timeLeft.expired ? 'expired' : 'active'}`}>
        <span className="timer-label">{timeLeft.expired ? 'Auction Status:' : 'Time Remaining:'}</span>
        <span className="timer-value">{timeLeft.text}</span>
      </div>
    );
  };

  const handlePlaceBid = async (e) => {
    e.preventDefault();
    setBidError('');
    setBidSuccess('');

    if (isHighestBidder) {
      setBidError('You are already the highest bidder.');
      return;
    }
    
    const parsedBid = parseFloat(bidAmount);
    if (isNaN(parsedBid) || parsedBid <= 0) {
      setBidError('Please enter a valid positive bid amount.');
      return;
    }

    const session = listing.biddingSession;
    const currentHighest = session.currentHighestBid || session.reservePrice;
    const minBid = session.currentHighestBid ? currentHighest + session.bidIncrement : session.reservePrice;
    
    if (parsedBid < minBid) {
      setBidError(`Bid must be at least $${minBid.toLocaleString()}.`);
      return;
    }

    setBidLoading(true);
    const bidderId = localStorage.getItem('userId');

    try {
      await API.post(`/auctions/listings/${listingId}/bids`, {
        bidderId: parseInt(bidderId),
        amount: parsedBid
      });
      
      setBidSuccess('Bid placed successfully! Escrow of 10% locked.');
      setBidAmount('');
      
      // Dispatch custom global event so navbar updates wallet amount instantly
      window.dispatchEvent(new Event('wallet-updated'));

      fetchListingDetails();
    } catch (err) {
      console.error(err);
      const backendMessage = err.response?.data?.message || err.response?.data || 'Failed to place bid. Ensure your wallet has enough funds for the 10% escrow lock.';
      setBidError(backendMessage);
    } finally {
      setBidLoading(false);
    }
  };

  const handleSchedule = async (e) => {
    e.preventDefault();
    setScheduleError('');
    setScheduleSuccess('');

    if (!scheduleStartTime || !scheduleEndTime) {
      setScheduleError('Please fill in all scheduling fields.');
      return;
    }

    const now = new Date();
    const minStart = new Date(now.getTime() - 60000); // 1-minute grace buffer
    if (new Date(scheduleStartTime) < minStart) {
      setScheduleError('Start Time must be starting now or in the future.');
      return;
    }
    if (new Date(scheduleEndTime) <= new Date(scheduleStartTime)) {
      setScheduleError('End Time must be after Start Time.');
      return;
    }
    const maxEnd = new Date(scheduleStartTime);
    maxEnd.setMonth(maxEnd.getMonth() + 6);
    if (new Date(scheduleEndTime) > maxEnd) {
      setScheduleError('End Time must be at most 6 months from the Start Time.');
      return;
    }
    
    const reservePriceVal = listing?.biddingSession?.reservePrice || 0;
    if (scheduleBuyItNowPrice && parseFloat(scheduleBuyItNowPrice) <= parseFloat(reservePriceVal)) {
      setScheduleError('Buy It Now Price must be greater than Reserve Price.');
      return;
    }

    setScheduleLoading(true);
    try {
      const payload = {
        startTime: scheduleStartTime,
        endTime: scheduleEndTime,
        buyItNowPrice: scheduleBuyItNowPrice ? parseFloat(scheduleBuyItNowPrice) : null
      };

      const response = await API.post(`/auctions/listings/${listingId}/schedule`, payload);
      setListing(response.data);
      setScheduleSuccess('Auction scheduled successfully!');
      if (addToast) addToast('📅 Auction schedule updated!', 'success');
      setShowScheduleForm(false);
    } catch (err) {
      console.error(err);
      setScheduleError(err.response?.data?.message || 'Failed to update auction schedule.');
    } finally {
      setScheduleLoading(false);
    }
  };

  const isHighValue = (cat) => {
    if (!cat) return false;
    const lower = cat.toLowerCase();
    return lower === 'real estate' || lower === 'vehicles' || lower === 'imobiliare' || lower === 'auto';
  };

  if (loading) {
    return (
      <div className="loader-container">
        <div className="spinner"></div>
        <p>Loading details...</p>
      </div>
    );
  }

  if (error || !listing) {
    return (
      <div className="detail-error-container">
        <button className="back-btn" onClick={onBack}>◀ Back</button>
        <div className="alert alert-danger">{error || 'Listing not found'}</div>
      </div>
    );
  }

  const session = listing.biddingSession || {};
  const isScheduled = !!(session.startTime && session.endTime);
  const currentBid = session.currentHighestBid || session.reservePrice || 0;
  const minRequiredBid = session.currentHighestBid 
    ? currentBid + (session.bidIncrement || 0)
    : (session.reservePrice || 0);
  const isSeller = Number(listing.sellerId) === userId;
  
  // Status check for alerts
  const isHighestBidder = session.currentHighestBidderId !== null && Number(session.currentHighestBidderId) === userId;
  const hasPlacedBid = session.bids && session.bids.some(b => Number(b.bidderId) === userId);
  const hasBids = session.bids && session.bids.length > 0;
  const canSchedule = isSeller && (!isScheduled || !hasBids);

  return (
    <div className="details-page-container">
      <button className="back-btn" onClick={onBack}>◀ Back to Auctions</button>

      <div className="details-layout">
        {/* Left Side: Images, Description & History */}
        <div className="details-left">
          <div className="details-image-box">
            {listing.imageUrl ? (
              <img src={listing.imageUrl} alt={listing.title} className="details-image" />
            ) : (
              <div className="details-image-placeholder">
                <span>💎</span>
              </div>
            )}
            <div className="details-category-row">
              <span className="category-tag">{listing.category}</span>
              {isHighValue(listing.category) && (
                <span className="kyc-warn-tag">High Value Listing (KYC Required)</span>
              )}
            </div>
          </div>

          <div className="details-info-box">
            <h1 className="details-title">{listing.title}</h1>
            <p className="details-description">{listing.description}</p>
          </div>

          {/* Bids History list */}
          <div className="details-info-box bids-history-card">
            <h3>Bids History</h3>
            {session.bids && session.bids.length > 0 ? (
              <div className="bids-history-list">
                {session.bids.map((b, idx) => (
                  <div key={b.id} className={`bid-history-item ${idx === 0 ? 'highest-bid-row' : ''}`}>
                    <div className="bidder-meta">
                      <span className="bidder-username">
                        👤 {b.bidderUsername || `User ${b.bidderId}`}
                        {Number(b.bidderId) === userId && <span className="me-badge"> (You)</span>}
                      </span>
                      <span className="bid-date">{new Date(b.timestamp).toLocaleString()}</span>
                    </div>
                    <div className="bid-amount-section">
                      {idx === 0 && <span className="leader-star">🥇 Highest Bid</span>}
                      <span className="bid-amount-value">${b.amount.toLocaleString()}</span>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="no-bids-text">No bids have been placed yet. Be the first to place a bid!</p>
            )}
          </div>
        </div>

        {/* Right Side: Bidding Panel */}
        <div className="details-right">
          <div className="bidding-panel">
            {isScheduled ? (
              <Countdown endTime={session.endTime} isActive={session.active} />
            ) : (
              <div className="details-timer expired">
                <span className="timer-label">Auction Status:</span>
                <span className="timer-value">Not Scheduled</span>
              </div>
            )}

            {session.reservePrice != null ? (
              <>
                <div className="prices-summary">
                  <div className="price-box">
                    <span className="price-label">{session.currentHighestBid ? 'Highest Bid' : 'Starting Price'}</span>
                    <span className="price-amount">${currentBid.toLocaleString()}</span>
                  </div>
                  {session.buyItNowPrice && (
                    <div className="price-box">
                      <span className="price-label">Buy It Now Price</span>
                      <span className="price-amount bin-color">${session.buyItNowPrice.toLocaleString()}</span>
                    </div>
                  )}
                </div>

                <div className="bid-increments-info">
                  <span>Min Required Bid: <strong>${minRequiredBid.toLocaleString()}</strong></span>
                  <span>Increment: <strong>${(session.bidIncrement || 0).toLocaleString()}</strong></span>
                </div>
              </>
            ) : (
              <div style={{ textAlign: 'center', padding: '16px 0', color: '#8b949e', borderBottom: '1px solid #21262d', marginBottom: '20px' }}>
                No pricing or bidding increments set yet.
              </div>
            )}

            {/* Bidding alerts (Are you winning / outbid) */}
            {isScheduled && session.active && !isSeller && (
              <div className="bidding-alert-box">
                {isHighestBidder && (
                  <div className="alert alert-success text-center">🥇 You are currently the highest bidder!</div>
                )}
                {hasPlacedBid && !isHighestBidder && (
                  <div className="alert alert-danger text-center">⚠️ You have been outbid! Raise your bid.</div>
                )}
              </div>
            )}

            {/* Action panel (Form/Notice depending on scheduler vs. bidder role) */}
            {canSchedule ? (
              showScheduleForm ? (
                <form onSubmit={handleSchedule} className="bid-form">
                  <h3>📅 Schedule Auction</h3>
                  
                  {scheduleError && <div className="alert alert-danger">{scheduleError}</div>}
                  {scheduleSuccess && <div className="alert alert-success">{scheduleSuccess}</div>}

                  <div className="input-group">
                    <label>Buy It Now Price ($) (Optional)</label>
                    <input
                      type="number"
                      step="0.01"
                      className="input-field"
                      placeholder="Ends auction instantly if met"
                      value={scheduleBuyItNowPrice}
                      onChange={(e) => setScheduleBuyItNowPrice(e.target.value)}
                      disabled={scheduleLoading}
                    />
                  </div>

                  <div className="input-group">
                    <label>Start Time</label>
                    <input
                      type="datetime-local"
                      className="input-field"
                      value={scheduleStartTime}
                      min={getNowString()}
                      onChange={(e) => setScheduleStartTime(e.target.value)}
                      required
                      disabled={scheduleLoading}
                    />
                  </div>

                  <div className="input-group">
                    <label>End Time</label>
                    <input
                      type="datetime-local"
                      className="input-field"
                      value={scheduleEndTime}
                      min={scheduleStartTime}
                      max={getSixMonthsLaterString(scheduleStartTime)}
                      onChange={(e) => setScheduleEndTime(e.target.value)}
                      required
                      disabled={scheduleLoading}
                    />
                  </div>

                  <div style={{ display: 'flex', gap: '10px', marginTop: '16px' }}>
                    <button type="submit" className="place-bid-btn" style={{ flex: 1 }} disabled={scheduleLoading}>
                      {scheduleLoading ? 'Saving...' : 'Save Schedule'}
                    </button>
                    <button type="button" className="cancel-btn" style={{ flex: 1, padding: '12px' }} onClick={() => setShowScheduleForm(false)} disabled={scheduleLoading}>
                      Cancel
                    </button>
                  </div>
                </form>
              ) : (
                <div style={{ textAlign: 'center', padding: '10px 0' }}>
                  <p style={{ color: '#8b949e', fontSize: '0.9rem', marginBottom: '16px' }}>
                    {isScheduled 
                      ? 'No bids have been placed on this listing yet. You can update the timing parameters.'
                      : 'This listing is currently unscheduled. Set up your start and end times to open bidding.'}
                  </p>
                  <button className="place-bid-btn" onClick={() => setShowScheduleForm(true)}>
                    📅 {isScheduled ? 'Reschedule Auction' : 'Schedule Auction'}
                  </button>
                </div>
              )
            ) : (
              // Bidding panel / Buyer Notice
              !isScheduled ? (
                <div className="seller-notice" style={{ textAlign: 'center' }}>
                  This listing is not currently scheduled for an active auction. Check back later!
                </div>
              ) : session.active ? (
                isSeller ? (
                  <div className="seller-notice">
                    You are the owner of this auction. Sellers cannot bid on their own listings.
                  </div>
                ) : (
                  <form onSubmit={handlePlaceBid} className="bid-form">
                    <h3>Place Your Bid</h3>
                    
                    {bidError && <div className="alert alert-danger">{bidError}</div>}
                    {bidSuccess && <div className="alert alert-success">{bidSuccess}</div>}

                    <div className="input-group">
                      <label>Bid Amount ($)</label>
                      <div className="bid-input-wrapper">
                        <input
                          type="number"
                          step="0.01"
                          className="input-field bid-input"
                          placeholder={`Min $${minRequiredBid}`}
                          value={bidAmount}
                          onChange={(e) => setBidAmount(e.target.value)}
                          required
                        />
                        <button
                          type="button"
                          className="increment-bid-btn"
                          onClick={() => {
                            const currentVal = parseFloat(bidAmount) || minRequiredBid;
                            setBidAmount((currentVal + session.bidIncrement));
                          }}
                          title={`Increase by $${session.bidIncrement}`}
                        >
                          +
                        </button>
                      </div>
                    </div>

                    <div className="escrow-disclaimer">
                      ⚠️ Placed bids lock an <strong>escrow deposit of 10%</strong> (${(parseFloat(bidAmount || 0) * 0.10).toFixed(2)}) from your wallet. If you are outbid, the escrow is immediately released back to you.
                    </div>

                    <button type="submit" className="place-bid-btn" disabled={bidLoading || isHighestBidder}>
                      {bidLoading ? 'Submitting Bid...' : isHighestBidder ? 'You are Leading' : 'Place Official Bid'}
                    </button>
                  </form>
                )
              ) : (
                <div className="ended-notice">
                  This bidding session has closed.
                  {session.currentHighestBid ? (
                    <p>Winner: <strong>{session.currentHighestBidderId === userId ? "You" : `User ${session.currentHighestBidderId}`}</strong></p>
                  ) : null}
                  {session.currentHighestBid ? (
                    <p>Highest final bid was <strong>${session.currentHighestBid.toLocaleString()}</strong>.</p>
                  ) : (
                    <p>No bids were received for this listing.</p>
                  )}
                </div>
              )
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default ListingDetails;
