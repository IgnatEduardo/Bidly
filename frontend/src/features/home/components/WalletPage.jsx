import React, { useState, useEffect } from 'react';
import API from '../../../api/axios';

const WalletPage = () => {
  const [wallet, setWallet] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  const [depositAmount, setDepositAmount] = useState('');
  const [depositLoading, setDepositLoading] = useState(false);
  const [depositSuccess, setDepositSuccess] = useState('');
  const [depositError, setDepositError] = useState('');

  const userId = localStorage.getItem('userId');

  const fetchWalletDetails = async () => {
    try {
      const response = await API.get(`/auctions/wallets/${userId}`);
      setWallet(response.data);
    } catch (err) {
      console.error(err);
      setError('Failed to load wallet details.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchWalletDetails();
  }, [userId]);

  const handleDeposit = async (e) => {
    e.preventDefault();
    setDepositError('');
    setDepositSuccess('');
    
    const parsedAmount = parseFloat(depositAmount);
    if (isNaN(parsedAmount) || parsedAmount <= 0) {
      setDepositError('Please enter a valid deposit amount.');
      return;
    }

    setDepositLoading(true);

    try {
      const response = await API.post(`/auctions/wallets/${userId}/deposit`, {
        amount: parsedAmount
      });
      setWallet(response.data);
      setDepositSuccess(`Successfully deposited $${parsedAmount.toLocaleString()}!`);
      setDepositAmount('');
    } catch (err) {
      console.error(err);
      setDepositError('Failed to deposit funds. Please try again.');
    } finally {
      setDepositLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="loader-container">
        <div className="spinner"></div>
        <p>Loading Wallet...</p>
      </div>
    );
  }

  if (error || !wallet) {
    return <div className="alert alert-danger">{error || 'Wallet not found'}</div>;
  }

  return (
    <div className="wallet-container">
      <h2>My Wallet</h2>
      <p className="wallet-subtitle">Manage your funds and monitor locked escrow balances.</p>

      {/* Balance Cards */}
      <div className="wallet-balance-row">
        <div className="balance-card available">
          <span className="balance-icon">🟢</span>
          <div className="balance-details">
            <span className="balance-label">Available Balance</span>
            <span className="balance-value">${wallet.balance.toLocaleString(undefined, { minimumFractionDigits: 2 })}</span>
          </div>
        </div>

        <div className="balance-card locked">
          <span className="balance-icon">🔒</span>
          <div className="balance-details">
            <span className="balance-label">Locked Escrow Balance</span>
            <span className="balance-value">${wallet.lockedBalance.toLocaleString(undefined, { minimumFractionDigits: 2 })}</span>
          </div>
        </div>
      </div>

      <div className="wallet-actions-layout">
        {/* Deposit Form */}
        <div className="wallet-action-box">
          <h3>Deposit Funds</h3>
          <p>Add funds to your account instantly to place bids.</p>

          {depositError && <div className="alert alert-danger">{depositError}</div>}
          {depositSuccess && <div className="alert alert-success">{depositSuccess}</div>}

          <form onSubmit={handleDeposit} className="deposit-form">
            <div className="input-group">
              <label>Amount ($)</label>
              <input
                type="number"
                step="0.01"
                className="input-field"
                placeholder="e.g. 500.00"
                value={depositAmount}
                onChange={(e) => setDepositAmount(e.target.value)}
                required
              />
            </div>

            <button type="submit" className="deposit-btn" disabled={depositLoading}>
              {depositLoading ? 'Processing...' : 'Deposit Now'}
            </button>
          </form>
        </div>

        {/* Transaction History */}
        <div className="wallet-history-box">
          <h3>Transaction History</h3>
          {wallet.transactions && wallet.transactions.length > 0 ? (
            <div className="transactions-list">
              {wallet.transactions.map((tx) => {
                let badgeClass = '';
                let typeText = tx.type;
                if (tx.type === 'DEPOSIT') {
                  badgeClass = 'tx-deposit';
                  typeText = '📥 Deposit';
                } else if (tx.type === 'LOCK') {
                  badgeClass = 'tx-lock';
                  typeText = '🔒 Escrow Locked';
                } else if (tx.type === 'RELEASE') {
                  badgeClass = 'tx-release';
                  typeText = '🔓 Escrow Released';
                } else if (tx.type === 'CHARGE') {
                  badgeClass = 'tx-charge';
                  typeText = '💸 Charged Checkout';
                }

                return (
                  <div key={tx.id} className="transaction-item">
                    <div className="tx-meta">
                      <span className={`tx-type-badge ${badgeClass}`}>{typeText}</span>
                      <span className="tx-date">
                        {new Date(tx.timestamp).toLocaleString()}
                      </span>
                    </div>
                    <span className={`tx-amount ${tx.type === 'DEPOSIT' || tx.type === 'RELEASE' ? 'positive' : 'negative'}`}>
                      {tx.type === 'DEPOSIT' || tx.type === 'RELEASE' ? '+' : '-'}${tx.amount.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </span>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="no-transactions">
              <span className="no-tx-icon">📄</span>
              <p>No transaction history recorded yet.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default WalletPage;
