import axios from 'axios';

//Global configuration for Axios instances
const API = axios.create({
  baseURL: 'http://localhost:8080/api/v1', //gateway
  headers: {
    'Content-Type': 'application/json',
  },
});

//Request interceptor to automatically attach the access token if it exists
API.interceptors.request.use(
  (config) => {
    const isAuthRoute = config.url && config.url.includes('/auth/');

    if (!isAuthRoute) {
      const token = localStorage.getItem('accessToken');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);
export default API;