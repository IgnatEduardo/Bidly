import React, { useState, useEffect } from 'react';
import API from '../../../api/axios';

const Dashboard = ({ onViewDetails, onCreateClick }) => {
  const [listings, setListings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  // Filtering, Sorting, Pagination
  const [category, setCategory] = useState('All');
  const [sortBy, setSortBy] = useState('id,desc');
  const [statusFilter, setStatusFilter] = useState('all'); // 'all', 'live'
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  
  const categories = ['All', 'Real Estate', 'Vehicles', 'Electronics', 'Fashion', 'Others'];

  const fetchListings = async () => {
    setLoading(true);
    setError('');
    try {
      const [field, direction] = sortBy.split(',');
      const response = await API.get('/auctions/listings', {
        params: {
          page: page,
          size: 6,
          sort: `${field},${direction}`
        }
      });
      
      let content = response.data.content || [];
      
      // Filter client-side if a specific category is chosen
      if (category !== 'All') {
        content = content.filter(item => {
          if (!item.category) return false;
          return item.category.toLowerCase() === category.toLowerCase();
        });
      }

      // Filter by status (live only) client-side if selected
      if (statusFilter === 'live') {
        content = content.filter(item => {
          const session = item.biddingSession;
          if (!session) return false;
          const isSessionActive = session.active;
          const isNotExpired = new Date(session.endTime) > new Date();
          return isSessionActive && isNotExpired;
        });
      }
      
      setListings(content);
      setTotalPages(response.data.totalPages || 1);
    } catch (err) {
      console.error(err);
      setError('Failed to fetch listings. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchListings();
  }, [category, sortBy, statusFilter, page]);

  // Dynamic countdown timer component helper inside mapping
  const CountdownTimer = ({ endTime, isActive }) => {
    const calculateTimeLeft = () => {
      if (!endTime) return { expired: true, text: 'Not Scheduled' };
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
      <span className={`timer-badge ${timeLeft.expired ? 'expired' : 'active'}`}>
        🕒 {timeLeft.text}
      </span>
    );
  };

  const isHighValue = (cat) => {
    if (!cat) return false;
    const lower = cat.toLowerCase();
    return lower === 'real estate' || lower === 'vehicles' || lower === 'imobiliare' || lower === 'auto';
  };

  return (
    <div className="dashboard-container">
      {/* Search/Filters Controls */}
      <div className="controls-bar">
        <div className="categories-filter">
          {categories.map((cat) => (
            <button
              key={cat}
              className={`category-tab ${category === cat ? 'active' : ''}`}
              onClick={() => {
                setCategory(cat);
                setPage(0);
              }}
            >
              {cat}
            </button>
          ))}
        </div>
        
        <div className="actions-selector">
          <select 
            value={sortBy} 
            onChange={(e) => {
              setSortBy(e.target.value);
              setPage(0);
            }}
            className="sort-select"
          >
            <option value="id,desc">Newest First</option>
            <option value="id,asc">Oldest First</option>
          </select>

          <select 
            value={statusFilter} 
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(0);
            }}
            className="sort-select"
          >
            <option value="all">All Auctions</option>
            <option value="live">Live Only</option>
          </select>
          
          <button className="create-listing-btn" onClick={onCreateClick}>
            + Create Listing
          </button>
        </div>
      </div>

      {loading ? (
        <div className="loader-container">
          <div className="spinner"></div>
          <p>Loading auctions...</p>
        </div>
      ) : error ? (
        <div className="alert alert-danger">{error}</div>
      ) : listings.length === 0 ? (
        <div className="no-items-box">
          <span className="no-items-icon">🔍</span>
          {category !== 'All' || statusFilter !== 'all' ? (
            <>
              <h3>No Matches Found</h3>
              <p>We couldn't find any auctions matching your filter criteria. Try resetting your filters.</p>
              <button 
                className="create-listing-btn" 
                style={{ marginTop: '1rem', float: 'none', display: 'inline-block' }}
                onClick={() => {
                  setCategory('All');
                  setStatusFilter('all');
                  setPage(0);
                }}
              >
                Clear Filters
              </button>
            </>
          ) : (
            <>
              <h3>No Active Auctions</h3>
              <p>There are no active auctions running at the moment. Be the first to list an item!</p>
              <button 
                className="create-listing-btn" 
                style={{ marginTop: '1rem', float: 'none', display: 'inline-block' }} 
                onClick={onCreateClick}
              >
                + Create Listing
              </button>
            </>
          )}
        </div>
      ) : (
        <>
          <div className="listings-grid">
            {listings.map((item) => {
              const session = item.biddingSession || {};
              const currentBid = session.currentHighestBid || session.reservePrice || 0;
              const hasBids = !!session.currentHighestBid;
              
              return (
                <div key={item.id} className="listing-card" onClick={() => onViewDetails(item.id)}>
                  <div className="card-image-container">
                    {item.imageUrl ? (
                      <img src={item.imageUrl} alt={item.title} className="card-image" />
                    ) : (
                      <div className="card-image-placeholder">
                        <span>💎</span>
                      </div>
                    )}
                    <div className="card-badges">
                      <span className="category-badge">{item.category}</span>
                      {isHighValue(item.category) && (
                        <span className="kyc-badge" title="KYC Verification required to place bids">KYC Required</span>
                      )}
                    </div>
                  </div>
                  
                  <div className="card-body">
                    <h3 className="card-title">{item.title}</h3>
                    <p className="card-description">{item.description}</p>
                    
                    <div className="card-bids-row">
                      <div className="bid-info">
                        <span className="bid-label">{hasBids ? 'Highest Bid' : 'Starting Price'}</span>
                        <span className="bid-amount">${currentBid.toLocaleString()}</span>
                      </div>
                      {session.buyItNowPrice && (
                        <div className="bid-info text-right">
                          <span className="bid-label">Buy It Now</span>
                          <span className="buy-now-amount">${session.buyItNowPrice.toLocaleString()}</span>
                        </div>
                      )}
                    </div>
                    
                    <div className="card-footer">
                      <CountdownTimer 
                        endTime={session.endTime} 
                        isActive={session.active} 
                      />
                      <button className="card-action-btn">
                        {session.active ? 'Bid Now' : 'View Details'}
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div className="pagination-bar">
              <button 
                disabled={page === 0} 
                onClick={() => setPage(page - 1)}
                className="pagination-btn"
              >
                ◀ Previous
              </button>
              <span className="pagination-info">
                Page {page + 1} of {totalPages}
              </span>
              <button 
                disabled={page >= totalPages - 1} 
                onClick={() => setPage(page + 1)}
                className="pagination-btn"
              >
                Next ▶
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default Dashboard;
