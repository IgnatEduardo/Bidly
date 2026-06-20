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
    
    // Client-side validations
    if (new Date(endTime) <= new Date(startTime)) {
      setError('End Time must be after Start Time.');
      return;
    }
    if (buyItNowPrice && parseFloat(buyItNowPrice) <= parseFloat(reservePrice)) {
      setError('Buy It Now Price must be greater than Reserve Price.');
      return;
    }
    if (parseFloat(bidIncrement) <= 0) {
      setError('Bid Increment must be greater than 0.');
      return;
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
        startTime: new Date(startTime).toISOString(),
        endTime: new Date(endTime).toISOString(),
        reservePrice: parseFloat(reservePrice),
        buyItNowPrice: buyItNowPrice ? parseFloat(buyItNowPrice) : null,
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

            <div className="input-group">
              <label>Start Time</label>
              <input
                type="datetime-local"
                className="input-field"
                value={startTime}
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
                onChange={(e) => setEndTime(e.target.value)}
                required
              />
            </div>
          </div>
        </div>

        <div className="form-actions">
          <button type="button" className="cancel-btn" onClick={onBack} disabled={loading}>
            Cancel
          </button>
          <button type="submit" className="submit-btn" disabled={loading}>
            {loading ? 'Creating...' : 'Launch Auction'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default CreateListing;
