import { reactive, readonly } from 'vue';
import client from '../api/client';

/**
 * 簡易認證狀態（不存 token —— token 在 httpOnly cookie，前端讀不到）。
 * 僅保存對外的使用者資訊，用於畫面顯示與角色判斷。
 */
const state = reactive({
  user: null, // { empId, name, role }
  ready: false, // 是否已完成初次 /me 確認
});

export function isAdmin() {
  return state.user?.role === 'ADMIN';
}

export async function fetchMe() {
  try {
    const { data } = await client.get('/auth/me');
    state.user = data.data;
  } catch {
    state.user = null;
  } finally {
    state.ready = true;
  }
  return state.user;
}

export async function login(empId, password) {
  const { data } = await client.post('/auth/login', { empId, password });
  state.user = data.data;
  return state.user;
}

export async function logout() {
  try {
    await client.post('/auth/logout');
  } finally {
    state.user = null;
  }
}

export const auth = readonly(state);
