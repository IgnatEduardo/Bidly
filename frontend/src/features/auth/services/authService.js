import API from '../../../api/axios';

// Auth Service isolation layer for backend communication
export const authService = {
  login: async (username, password) => {
    const response = await API.post('/auth/login', { username, password });
    return response.data; // Contains accessToken, refreshToken, username
  },

  register: async (firstName, lastName, username, email, password, phoneNumber) => {
    const response = await API.post('/auth/register', { 
      firstName, 
      lastName, 
      username, 
      email, 
      password, 
      phoneNumber 
    });
    return response.data;
  },

  confirmAccount: async (token) => {
    const response = await API.get(`/auth/confirm?token=${token}`);
    return response.data;
  },

  logout: async () => {
    const response = await API.post('/auth/logout');
    return response.data;
  },

  refreshToken: async (refreshToken) => {
    const response = await API.post('/auth/refresh-token', { refreshToken });
    return response.data;
  }
};