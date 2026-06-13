import axios from 'axios';

/**
 * 共用 axios 實例。withCredentials 讓瀏覽器帶上 httpOnly cookie（JWT）。
 */
const client = axios.create({
  baseURL: '/api',
  withCredentials: true,
});

export default client;
