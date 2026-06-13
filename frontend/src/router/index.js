import { createRouter, createWebHistory } from 'vue-router';
import { auth, fetchMe } from '../stores/auth';

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
    meta: { public: true },
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('../views/ForgotPasswordView.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/HomeView.vue'),
    meta: { public: false },
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

let initialized = false;

router.beforeEach(async (to) => {
  if (!initialized) {
    await fetchMe();
    initialized = true;
  }

  const loggedIn = !!auth.user;
  if (!to.meta.public && !loggedIn) return { name: 'Login' };
  if (to.meta.public && loggedIn && to.name !== 'ForgotPassword') return { name: 'Home' };
});

export default router;
