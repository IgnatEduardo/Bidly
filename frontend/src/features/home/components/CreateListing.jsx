import React, { useState } from 'react';
import API from '../../../api/axios';

const CreateListing = ({ onBack, onSuccess }) => {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [imageUrl, setImageUrl] = useState('');
  const [category, setCategory] = useState('Electronics');
  
  // Bidding Session configs
  const [reservePrice, setReservePrice] = useState('');
  const [buyItNowPrice, setBuyItNowPrice] = useState('');
  const [bidIncrement, setBidIncrement] = useState('10');
  const [shouldSchedule, setShouldSchedule] = useState(true);
  
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

  // Timestamps
  const [startTime, setStartTime] = useState(() => {
    const now = new Date();
    now.setMinutes(now.getMinutes() + 1); // default 1 min from now
    return new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
  });
  
  const [endTime, setEndTime] = useState(() => {
    const now = new Date();
    now.setHours(now.getHours() + 24); // default 1 day from now
    return new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  
  const categories = ['Electronics', 'Vehicles', 'Real Estate', 'Fashion', 'Others'];

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    
    // reservePrice and bidIncrement are always required on listing creation
    if (!reservePrice || !bidIncrement) {
      setError('Please fill in Reserve Price and Bid Increment.');
      return;
    }
    if (parseFloat(reservePrice) <= 0) {
      setError('Reserve Price must be greater than 0.');
      return;
    }
    if (parseFloat(bidIncrement) <= 0) {
      setError('Bid Increment must be greater than 0.');
      return;
    }

    if (shouldSchedule) {
      if (!startTime || !endTime) {
        setError('Please fill in Start Time and End Time.');
        return;
      }

      // Client-side validations for scheduling
      const now = new Date();
      const minStart = new Date(now.getTime() - 60000); // 1-minute grace buffer for request delays
      if (new Date(startTime) < minStart) {
        setError('Start Time must be starting now or in the future.');
        return;
      }
      if (new Date(endTime) <= new Date(startTime)) {
        setError('End Time must be after Start Time.');
        return;
      }
      const maxEnd = new Date(startTime);
      maxEnd.setMonth(maxEnd.getMonth() + 6);
      if (new Date(endTime) > maxEnd) {
        setError('End Time must be at most 6 months from the Start Time.');
        return;
      }
      if (buyItNowPrice && parseFloat(buyItNowPrice) <= parseFloat(reservePrice)) {
        setError('Buy It Now Price must be greater than Reserve Price.');
        return;
      }
    }

    setLoading(true);
    const sellerId = localStorage.getItem('userId');

    try {
      const payload = {
        title,
        description,
        imageUrl: imageUrl || null,
        category,
        sellerId: parseInt(sellerId),
        startTime: shouldSchedule ? startTime : null,
        endTime: shouldSchedule ? endTime : null,
        reservePrice: parseFloat(reservePrice),
        buyItNowPrice: shouldSchedule && buyItNowPrice ? parseFloat(buyItNowPrice) : null,
        bidIncrement: parseFloat(bidIncrement)
      };

      await API.post('/auctions/listings', payload);
      onSuccess();
    } catch (err) {
      console.error(err);
      const backendMessage = err.response?.data?.message || err.response?.data || 'Failed to create listing. Note: For high-value categories (Vehicles, Real Estate), sellers must be KYC-Approved.';
      setError(backendMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="form-card-container">
      <div className="form-header">
        <button className="back-btn" onClick={onBack}>◀ Back to Dashboard</button>
        <h2>Create New Listing</h2>
        <p>Set up your auction session. High-value listings (Real Estate, Vehicles) require KYC verification.</p>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      <form onSubmit={handleSubmit} className="premium-form">
        <div className="form-grid">
          {/* Left Column: Basic Details */}
          <div className="form-section">
            <h3>Item Information</h3>
            
            <div className="input-group">
              <label>Title</label>
              <input
                type="text"
                className="input-field"
                placeholder="e.g. Vintage Rolex Submariner"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                required
              />
            </div>

            <div className="input-group">
              <label>Description</label>
              <textarea
                className="input-field textarea-field"
                placeholder="Describe your item in detail, including its condition, history, and key features..."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                required
              />
            </div>

            <div className="input-group">
              <label>Image URL (Optional)</label>
              <input
                type="url"
                className="input-field"
                placeholder="https://example.com/image.jpg"
                value={imageUrl}
                onChange={(e) => setImageUrl(e.target.value)}
              />
            </div>

            <div className="input-group">
              <label>Category</label>
              <select
                className="input-field"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
              >
                {categories.map(cat => (
                  <option key={cat} value={cat}>{cat}</option>
                ))}
              </select>
            </div>
          </div>

          {/* Right Column: Pricing & Timings */}
          <div className="form-section">
            <h3>Bidding Configuration</h3>

            <div className="input-group">
              <label>Reserve Price ($)</label>
              <input
                type="number"
                step="0.01"
                className="input-field"
                placeholder="e.g. 150.00 (Minimum starting price)"
                value={reservePrice}
                onChange={(e) => setReservePrice(e.target.value)}
                required
              />
            </div>

            <div className="input-group">
              <label>Bid Increment ($)</label>
              <input
                type="number"
                step="0.01"
                className="input-field"
                placeholder="e.g. 10.00"
                value={bidIncrement}
                onChange={(e) => setBidIncrement(e.target.value)}
                required
              />
            </div>

            <div className="input-group" style={{ display: 'flex', alignItems: 'center', gap: '10px', marginTop: '24px', marginBottom: '20px' }}>
              <input
                type="checkbox"
                id="shouldSchedule"
                checked={shouldSchedule}
                onChange={(e) => setShouldSchedule(e.target.checked)}
                style={{ width: '18px', height: '18px', cursor: 'pointer' }}
              />
              <label htmlFor="shouldSchedule" style={{ margin: 0, cursor: 'pointer', fontWeight: 'bold', fontSize: '0.95rem' }}>
                Schedule Auction Immediately
              </label>
            </div>

            {shouldSchedule ? (
              <>
                <div className="input-group">
                  <label>Buy It Now Price ($) (Optional)</label>
                  <input
                    type="number"
                    step="0.01"
                    className="input-field"
                    placeholder="e.g. 500.00 (Auction ends instantly if met)"
                    value={buyItNowPrice}
                    onChange={(e) => setBuyItNowPrice(e.target.value)}
                  />
                </div>

                <div className="input-group">
                  <label>Start Time</label>
                  <input
                    type="datetime-local"
                    className="input-field"
                    value={startTime}
                    min={getNowString()}
                    onChange={(e) => setStartTime(e.target.value)}
                    required
                  />
                </div>

                <div className="input-group">
                  <label>End Time</label>
                  <input
                    type="datetime-local"
                    className="input-field"
                    value={endTime}
                    min={startTime}
                    max={getSixMonthsLaterString(startTime)}
                    onChange={(e) => setEndTime(e.target.value)}
                    required
                  />
                </div>
              </>
            ) : (
              <div className="info-box" style={{ background: '#161b22', padding: '16px', borderRadius: '8px', border: '1px dashed #30363d', color: '#8b949e', fontSize: '0.9rem' }}>
                ℹ️ You are choosing to list this item without scheduling an active auction session right now. Other clients will see the item listing details, but they will not be able to place bids. 
                <br/><br/>
                You can schedule and launch the auction later from the listing details view.
              </div>
            )}
          </div>
        </div>

        <div className="form-actions">
          <button type="button" className="cancel-btn" onClick={onBack} disabled={loading}>
            Cancel
          </button>
          <button type="submit" className="submit-btn" disabled={loading}>
            {loading ? 'Creating...' : shouldSchedule ? 'Launch Auction' : 'Create Listing'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default CreateListing;
