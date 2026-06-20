import axios from 'axios';

//Global configuration for Axios instances
const API = axios.create({
  baseURL: 'http://localhost:8080/api/v1', //Target Gateway or Auth Service port
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