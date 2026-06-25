import axios from 'axios';

//Global configuration for Axios instances
const apiHost = import.meta.env.VITE_API_URL || 'http://localhost:8080';
const API = axios.create({
  baseURL: `${apiHost.replace(/\/$/, '')}/api/v1`,
  headers: {
    'Content-Type': 'application/json',
  },
});

//Request interceptor to automatically attach the access token if it exists
API.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);
export default API;