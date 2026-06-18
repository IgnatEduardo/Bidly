import React, { useState } from 'react';
import { authService } from '../services/authService';

const RegisterForm = ({ onSwitch }) => {
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess(false);
    setLoading(true);

    try {
      await authService.register(firstName, lastName, username, email, password, phoneNumber);
      setSuccess(true);
      
      // clear fields on success
      setFirstName('');
      setLastName('');
      setUsername('');
      setEmail('');
      setPassword('');
      setPhoneNumber('');
    } catch (err) {
      const backendMessage = err.response?.data?.message || 'Registration failed';
      setError(backendMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-card" style={{ maxWidth: '450px' }}> {}
      <h2 className="auth-title">Create an account</h2>
      <p className="auth-subtitle">Join Bidly auction platform today</p>

      {error && <div className="alert alert-danger">{error}</div>}
      {success && (
        <div className="alert alert-success">
          Account created successfully! You can now{' '}
          <span className="auth-link" onClick={onSwitch}>Login</span>.
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <div style={{ display: 'flex', gap: '15px' }}>
          <div className="input-group" style={{ flex: 1 }}>
            <label>First Name</label>
            <input
              type="text"
              className="input-field"
              placeholder="John"
              value={firstName}
              onChange={(e) => setFirstName(e.target.value)}
              required
            />
          </div>
          <div className="input-group" style={{ flex: 1 }}>
            <label>Last Name</label>
            <input
              type="text"
              className="input-field"
              placeholder="Doe"
              value={lastName}
              onChange={(e) => setLastName(e.target.value)}
              required
            />
          </div>
        </div>

        <div className="input-group">
          <label>Username</label>
          <input
            type="text"
            className="input-field"
            placeholder="Choose a username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />
        </div>

        <div className="input-group">
          <label>Email Address</label>
          <input
            type="email"
            className="input-field"
            placeholder="you@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </div>

        <div className="input-group">
          <label>Phone Number</label>
          <input
            type="tel"
            className="input-field"
            placeholder="+407xxxxxxxx"
            value={phoneNumber}
            onChange={(e) => setPhoneNumber(e.target.value)}
            required
          />
        </div>

        <div className="input-group">
          <label>Password</label>
          <input
            type="password"
            className="input-field"
            placeholder="••••••••"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>

        <button type="submit" className="auth-button" disabled={loading}>
          {loading ? 'Creating account...' : 'Register'}
        </button>
      </form>

      <p className="auth-switch">
        Already have an account?{' '}
        <span className="auth-link" onClick={onSwitch}>
          Login
        </span>
      </p>
    </div>
  );
};

export default RegisterForm;