import React, { useEffect, useState, useRef } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { authService } from '../services/authService'; 

const ConfirmAccountPage = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState('loading'); 
  const [message, setMessage] = useState('Se verifică contul, vă rugăm așteptați...');
  const calledRef = useRef(false);

  useEffect(() => {
    const token = searchParams.get('token');

    if (!token) {
      setStatus('error');
      setMessage('Token-ul lipsește sau este invalid!');
      return;
    }

    if (calledRef.current) return;
    calledRef.current = true;

    authService.confirmAccount(token)
      .then((data) => {
        setStatus('success');
        setMessage('Contul tău a fost activat cu succes! Te redirecționăm la Login...');
        
        setTimeout(() => {
          navigate('/login'); 
        }, 3000);
      })
      .catch((err) => {
        setStatus('error');
        const backendMessage = err.response?.data?.message || 'Link-ul de confirmare a expirat sau este invalid!';
        setMessage(backendMessage);
      });
  }, [searchParams, navigate]);

  return (
    <div className="auth-container">
      <div className="auth-card" style={{ textAlign: 'center', padding: '30px' }}>
        <h2 className="auth-title">Verificare Cont</h2>
        
        {status === 'loading' && (
          <div className="alert alert-info" style={{ marginTop: '20px' }}>
            {message}
          </div>
        )}

        {status === 'success' && (
          <div className="alert alert-success" style={{ marginTop: '20px' }}>
            {message}
          </div>
        )}

        {status === 'error' && (
          <div>
            <div className="alert alert-danger" style={{ marginTop: '20px' }}>
              {message}
            </div>
            <button 
              className="auth-button" 
              style={{ marginTop: '15px' }}
              onClick={() => navigate('/login')}
            >
              Mergi la Login
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default ConfirmAccountPage;