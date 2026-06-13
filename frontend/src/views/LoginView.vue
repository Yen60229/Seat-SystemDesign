<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { login } from '../stores/auth';

const router   = useRouter();
const empId    = ref('');
const password = ref('');
const error    = ref('');
const loading  = ref(false);
const showPwd  = ref(false);

async function handleLogin() {
  error.value = '';
  if (!empId.value.trim() || !password.value) {
    error.value = '請輸入員工編號與密碼';
    return;
  }
  loading.value = true;
  try {
    await login(empId.value.trim(), password.value);
    router.push('/');
  } catch (err) {
    error.value = err.response?.data?.message ?? '登入失敗，請稍後再試';
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <section class="login-layout">
    <!-- 左側品牌區 -->
    <aside class="login-brand">
      <section class="brand-inner">
        <figure class="brand-logo">
          <svg class="brand-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" fill="none" aria-hidden="true">
            <rect x="15" y="45" width="70" height="42" rx="2" fill="currentColor"/>
            <polygon points="50,10 8,45 92,45" fill="currentColor" opacity="0.85"/>
            <rect x="28" y="56" width="13" height="13" rx="1" fill="currentColor" opacity="0.25"/>
            <rect x="44" y="56" width="13" height="13" rx="1" fill="currentColor" opacity="0.25"/>
            <rect x="60" y="56" width="13" height="13" rx="1" fill="currentColor" opacity="0.25"/>
            <rect x="38" y="72" width="25" height="15" rx="1" fill="currentColor" opacity="0.3"/>
            <rect x="45" y="3" width="10" height="10" rx="2" fill="currentColor" opacity="0.7"/>
          </svg>
        </figure>
        <h1 class="brand-name">座位管理系統</h1>
        <p class="brand-system">Seat Management System</p>
        <hr class="brand-divider" />
        <ul class="brand-features">
          <li>分樓層座位配置管理</li>
          <li>即時異動紀錄</li>
          <li>公告發佈與管理</li>
        </ul>
      </section>
    </aside>

    <!-- 右側表單區 -->
    <main class="login-form-area">
      <article class="login-box">
        <header class="login-heading">
          <h2>登入系統</h2>
          <p>請使用您的員工帳號登入</p>
        </header>

        <form @submit.prevent="handleLogin" novalidate>
          <p class="form-group">
            <label class="form-label" for="empId">員工編號</label>
            <input
              id="empId"
              v-model="empId"
              class="form-control"
              type="text"
              placeholder="例：A0001"
              autocomplete="username"
              :disabled="loading"
            />
          </p>

          <p class="form-group" style="margin-top:16px">
            <label class="form-label" for="password">密碼</label>
            <span class="input-suffix">
              <input
                id="password"
                v-model="password"
                class="form-control"
                :type="showPwd ? 'text' : 'password'"
                placeholder="出生年月日 + 身分證後 4 碼"
                autocomplete="current-password"
                :disabled="loading"
              />
              <button
                type="button"
                class="suffix-btn"
                :aria-label="showPwd ? '隱藏密碼' : '顯示密碼'"
                tabindex="-1"
                @click="showPwd = !showPwd"
              >{{ showPwd ? '隱藏' : '顯示' }}</button>
            </span>
          </p>

          <p v-if="error" class="banner banner-error" style="margin-top:14px">
            {{ error }}
          </p>

          <button
            type="submit"
            class="btn btn-primary btn-lg login-submit"
            :disabled="loading"
          >{{ loading ? '登入中…' : '登入' }}</button>
        </form>

        <footer class="login-footer">
          <RouterLink to="/forgot-password">忘記密碼？</RouterLink>
        </footer>

        <aside class="login-hint">
          <strong>預設密碼格式</strong>
          <span>出生年月日（YYYYMMDD）+ 身分證後 4 碼</span>
          <span class="hint-example">範例：19801225AB12</span>
        </aside>
      </article>
    </main>
  </section>
</template>

<style scoped>
.login-layout {
  min-height: 100vh;
  display: flex;
}

/* ── 左側品牌 ── */
.login-brand {
  width: 360px;
  background: var(--green);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.brand-inner { padding: 56px 44px; }
.brand-logo {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  background: rgba(255,255,255,0.15);
  border: 1px solid rgba(255,255,255,0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 0 24px;
  padding: 0;
  overflow: hidden;
}
.brand-icon {
  width: 46px;
  height: 46px;
  color: #fff;
  display: block;
}
.brand-divider {
  width: 44px;
  height: 2px;
  background: rgba(255,255,255,0.35);
  border: none;
  margin: 32px 0;
}
.brand-name {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 8px;
}
.brand-system {
  font-size: 16px;
  opacity: 0.8;
}
.brand-features {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.brand-features li {
  font-size: 16px;
  opacity: 0.85;
  padding-left: 16px;
  position: relative;
}
.brand-features li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 9px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(255,255,255,0.6);
}

/* ── 右側表單 ── */
.login-form-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg);
  padding: 48px 24px;
}
.login-box {
  width: 100%;
  max-width: 440px;
}
.login-heading {
  margin-bottom: 32px;
}
.login-heading h2 {
  font-size: 26px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 6px;
}
.login-heading p {
  font-size: 16px;
  color: var(--text-muted);
}

/* 輸入框後綴按鈕 */
.input-suffix {
  position: relative;
}
.input-suffix .form-control {
  padding-right: 68px;
}
.suffix-btn {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: var(--green);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  padding: 4px 2px;
}
.suffix-btn:hover { text-decoration: underline; }

.login-submit {
  width: 100%;
  margin-top: 26px;
  font-size: 17px;
  padding: 13px;
}

.login-footer {
  margin-top: 18px;
  text-align: right;
}
.login-footer a {
  font-size: 15px;
  color: var(--green);
  text-decoration: none;
}
.login-footer a:hover { text-decoration: underline; }

.login-hint {
  margin-top: 32px;
  padding: 14px 16px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.login-hint strong {
  font-size: 13px;
  color: var(--text-sub);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.login-hint span {
  font-size: 15px;
  color: var(--text-sub);
}
.hint-example {
  font-family: monospace;
  color: var(--text-muted) !important;
  font-size: 14px !important;
}

/* 小螢幕收合左欄 */
@media (max-width: 640px) {
  .login-brand { display: none; }
}
</style>
