<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import client from '../api/client';

const router = useRouter();
const step       = ref(1);   // 1=輸入員編  2=驗證碼  3=設新密碼  4=完成
const empId      = ref('');
const code       = ref('');
const newPwd     = ref('');
const confirmPwd = ref('');
const devCode    = ref('');
const loading    = ref(false);
const error      = ref('');
const showPwd    = ref(false);

async function requestCode() {
  error.value = '';
  if (!empId.value.trim()) { error.value = '請輸入員工編號'; return; }
  loading.value = true;
  try {
    const { data } = await client.post('/auth/forgot-password', { empId: empId.value.trim() });
    devCode.value = data.data?.devCode ?? '';
    step.value = 2;
  } catch (err) {
    error.value = err.response?.data?.message ?? '系統錯誤，請稍後再試';
  } finally {
    loading.value = false;
  }
}

function goToReset() {
  error.value = '';
  if (!/^\d{6}$/.test(code.value)) { error.value = '驗證碼為 6 位數字'; return; }
  step.value = 3;
}

async function resetPassword() {
  error.value = '';
  if (!newPwd.value) { error.value = '請輸入新密碼'; return; }
  if (newPwd.value !== confirmPwd.value) { error.value = '兩次密碼輸入不一致'; return; }
  loading.value = true;
  try {
    await client.post('/auth/reset-password', {
      empId: empId.value.trim(),
      code: code.value,
      newPassword: newPwd.value,
    });
    step.value = 4;
  } catch (err) {
    error.value = err.response?.data?.message ?? '重設失敗，請重新操作';
    if (err.response?.status === 400) step.value = 2;
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="fp-page">
    <div class="fp-container">
      <!-- 頂部導覽 -->
      <div class="fp-nav">
        <RouterLink to="/login" class="back-link">
          ← 返回登入
        </RouterLink>
        <span class="fp-title">忘記密碼</span>
      </div>

      <!-- 步驟指示器 -->
      <div class="stepper">
        <div
          v-for="(label, i) in ['輸入員編', '驗證碼', '設定密碼']"
          :key="i"
          class="stepper-item"
        >
          <div :class="['stepper-dot', {
            'done':   step > i + 1,
            'active': step === i + 1,
          }]">{{ step > i + 1 ? '✓' : i + 1 }}</div>
          <span :class="['stepper-label', { active: step === i + 1 }]">{{ label }}</span>
          <div v-if="i < 2" class="stepper-line"></div>
        </div>
      </div>

      <!-- 表單卡片 -->
      <div class="fp-card">

        <!-- Step 1 -->
        <template v-if="step === 1">
          <p class="step-desc">輸入您的員工編號，系統將發送 6 位數驗證碼至綁定信箱。</p>
          <form @submit.prevent="requestCode" novalidate>
            <div class="form-group">
              <label class="form-label" for="fpEmpId">員工編號</label>
              <input id="fpEmpId" v-model="empId" class="form-control" type="text"
                placeholder="例：A0001" autocomplete="username" :disabled="loading" />
            </div>
            <div v-if="error" class="banner banner-error mt12">{{ error }}</div>
            <button type="submit" class="btn btn-primary btn-lg step-btn" :disabled="loading">
              {{ loading ? '處理中…' : '發送驗證碼' }}
            </button>
          </form>
        </template>

        <!-- Step 2 -->
        <template v-else-if="step === 2">
          <p class="step-desc">驗證碼已發送至綁定信箱，10 分鐘內有效，最多可輸入 5 次。</p>

          <!-- 開發模式顯示驗證碼 -->
          <div v-if="devCode" class="devcode-box">
            <span class="devcode-label">開發模式 — 驗證碼</span>
            <span class="devcode-value">{{ devCode }}</span>
          </div>

          <form @submit.prevent="goToReset" novalidate>
            <div class="form-group mt12">
              <label class="form-label" for="fpCode">6 位數驗證碼</label>
              <input id="fpCode" v-model="code" class="form-control code-input"
                type="text" inputmode="numeric" maxlength="6" placeholder="_ _ _ _ _ _"
                autocomplete="one-time-code" />
            </div>
            <div v-if="error" class="banner banner-error mt12">{{ error }}</div>
            <button type="submit" class="btn btn-primary btn-lg step-btn">下一步</button>
          </form>
        </template>

        <!-- Step 3 -->
        <template v-else-if="step === 3">
          <p class="step-desc">新密碼至少 8 碼，須包含英文字母與數字。</p>
          <form @submit.prevent="resetPassword" novalidate>
            <div class="form-group">
              <label class="form-label" for="fpPwd">新密碼</label>
              <div class="input-suffix">
                <input id="fpPwd" v-model="newPwd" class="form-control"
                  :type="showPwd ? 'text' : 'password'"
                  placeholder="至少 8 碼，英文 + 數字"
                  autocomplete="new-password" :disabled="loading" />
                <button type="button" class="suffix-btn" @click="showPwd = !showPwd" tabindex="-1">
                  {{ showPwd ? '隱藏' : '顯示' }}
                </button>
              </div>
            </div>
            <div class="form-group mt12">
              <label class="form-label" for="fpPwd2">確認新密碼</label>
              <input id="fpPwd2" v-model="confirmPwd" class="form-control"
                :type="showPwd ? 'text' : 'password'"
                placeholder="再次輸入新密碼"
                autocomplete="new-password" :disabled="loading" />
            </div>
            <div v-if="error" class="banner banner-error mt12">{{ error }}</div>
            <button type="submit" class="btn btn-primary btn-lg step-btn" :disabled="loading">
              {{ loading ? '更新中…' : '確認重設密碼' }}
            </button>
          </form>
        </template>

        <!-- Step 4：完成 -->
        <template v-else>
          <div class="done-state">
            <div class="done-icon">✓</div>
            <h3>密碼已成功重設</h3>
            <p>請使用新密碼重新登入系統。</p>
            <RouterLink to="/login" class="btn btn-primary btn-lg" style="margin-top:24px;text-decoration:none">
              前往登入
            </RouterLink>
          </div>
        </template>

      </div>
    </div>
  </div>
</template>

<style scoped>
.fp-page {
  min-height: 100vh;
  background: var(--bg);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 56px 24px;
}
.fp-container {
  width: 100%;
  max-width: 520px;
}

/* 頂部導覽 */
.fp-nav {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 32px;
}
.back-link {
  font-size: 15px;
  color: var(--text-muted);
  text-decoration: none;
}
.back-link:hover { color: var(--text); }
.fp-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text);
}

/* 步驟指示器 */
.stepper {
  display: flex;
  align-items: flex-start;
  margin-bottom: 28px;
}
.stepper-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  position: relative;
  flex: 1;
}
.stepper-dot {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 2px solid var(--border-dark);
  background: var(--surface);
  color: var(--text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  position: relative;
  z-index: 1;
}
.stepper-dot.active { border-color: var(--green); background: var(--green); color: #fff; }
.stepper-dot.done   { border-color: var(--green); background: var(--green); color: #fff; font-size: 13px; }
.stepper-label {
  font-size: 14px;
  color: var(--text-muted);
  white-space: nowrap;
}
.stepper-label.active { color: var(--green); font-weight: 600; }
.stepper-line {
  position: absolute;
  top: 17px;
  left: calc(50% + 19px);
  right: calc(-50% + 19px);
  height: 1px;
  background: var(--border);
  z-index: 0;
}

/* 表單卡片 */
.fp-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 32px 32px 28px;
  box-shadow: var(--shadow-sm);
}
.step-desc {
  font-size: 16px;
  color: var(--text-sub);
  margin-bottom: 24px;
  line-height: 1.6;
}
.mt12 { margin-top: 14px; }
.step-btn {
  width: 100%;
  margin-top: 24px;
}

/* 開發模式驗證碼 */
.devcode-box {
  background: #fffbf0;
  border: 1px solid #e5c66a;
  border-radius: var(--radius-sm);
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}
.devcode-label { font-size: 13px; color: #7d5a00; font-weight: 600; }
.devcode-value {
  font-size: 28px;
  font-weight: 900;
  letter-spacing: 6px;
  font-family: monospace;
  color: var(--text);
}

/* 驗證碼輸入框 */
.code-input {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 8px;
  text-align: center;
  font-family: monospace;
}

/* 輸入框後綴 */
.input-suffix { position: relative; }
.input-suffix .form-control { padding-right: 64px; }
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
}
.suffix-btn:hover { text-decoration: underline; }

/* 完成畫面 */
.done-state {
  text-align: center;
  padding: 20px 0;
}
.done-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--green);
  color: #fff;
  font-size: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}
.done-state h3 { font-size: 22px; font-weight: 700; margin-bottom: 10px; }
.done-state p  { font-size: 16px; color: var(--text-sub); }
</style>
