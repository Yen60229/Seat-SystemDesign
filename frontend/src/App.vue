<template>
  <RouterView />
</template>

<style>
*,
*::before,
*::after {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

:root {
  --green:        #00713d;
  --green-dark:   #005a30;
  --green-light:  #f0f7f3;
  --green-border: #b2d8c4;
  --danger:       #c0392b;
  --danger-bg:    #fdf3f2;
  --warn-bg:      #fffbf0;
  --warn-text:    #7d5a00;
  --info-bg:      #f0f4ff;
  --info-text:    #2c4fa3;
  --ok-bg:        #f0f9f4;
  --ok-text:      #1a6b3e;
  --border:       #dde1e7;
  --border-dark:  #b0b7c3;
  --bg:           #f5f6f8;
  --surface:      #ffffff;
  --text:         #1c2b3a;
  --text-sub:     #52667a;
  --text-muted:   #8996a3;
  --radius-sm:    4px;
  --radius:       6px;
  --shadow-sm:    0 1px 3px rgba(0,0,0,0.08);
  --shadow:       0 2px 8px rgba(0,0,0,0.10);
}

html, body {
  height: 100%;
}

body {
  font-family: "Microsoft JhengHei", "微軟正黑體", -apple-system, BlinkMacSystemFont, system-ui, sans-serif;
  background: var(--bg);
  color: var(--text);
  font-size: 17px;
  line-height: 1.6;
  -webkit-font-smoothing: antialiased;
}

/* ── 按鈕 ── */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 22px;
  font-size: 16px;
  font-weight: 500;
  border-radius: var(--radius);
  border: 1px solid transparent;
  cursor: pointer;
  transition: background 0.12s, border-color 0.12s, color 0.12s;
  white-space: nowrap;
  line-height: 1.4;
}
.btn:disabled { opacity: 0.45; cursor: not-allowed; }

.btn-primary {
  background: var(--green);
  color: #fff;
  border-color: var(--green);
}
.btn-primary:not(:disabled):hover { background: var(--green-dark); border-color: var(--green-dark); }

.btn-secondary {
  background: var(--surface);
  color: var(--text);
  border-color: var(--border-dark);
}
.btn-secondary:not(:disabled):hover { background: #f0f1f3; }

.btn-danger {
  background: var(--surface);
  color: var(--danger);
  border-color: #e0a09a;
}
.btn-danger:not(:disabled):hover { background: var(--danger-bg); }

.btn-sm { padding: 6px 16px; font-size: 14px; }
.btn-lg { padding: 12px 32px; font-size: 17px; }

/* ── 表單 ── */
.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-label { font-size: 15px; font-weight: 600; color: var(--text-sub); }
.form-control {
  padding: 11px 14px;
  font-size: 16px;
  border: 1px solid var(--border-dark);
  border-radius: var(--radius);
  background: var(--surface);
  color: var(--text);
  width: 100%;
  transition: border-color 0.12s, box-shadow 0.12s;
}
.form-control:focus {
  outline: none;
  border-color: var(--green);
  box-shadow: 0 0 0 3px rgba(0,113,61,0.14);
}
.form-control::placeholder { color: var(--text-muted); }

/* ── 提示橫幅 ── */
.banner {
  padding: 12px 16px;
  border-radius: var(--radius);
  font-size: 15px;
  line-height: 1.5;
  border-left: 4px solid transparent;
}
.banner-success { background: var(--ok-bg);    color: var(--ok-text);   border-color: var(--green); }
.banner-error   { background: var(--danger-bg); color: var(--danger);   border-color: var(--danger); }
.banner-info    { background: var(--info-bg);   color: var(--info-text); border-color: #5b7ee5; }
.banner-warn    { background: var(--warn-bg);   color: var(--warn-text); border-color: #e5a000; }

/* ── 頁面結構 ── */
.page { min-height: 100vh; display: flex; flex-direction: column; }
.main-content { flex: 1; max-width: 1200px; width: 100%; margin: 0 auto; padding: 28px 24px; }

/* ── 頂部導覽 ── */
.topbar {
  height: 58px;
  background: var(--green);
  color: #fff;
  display: flex;
  align-items: center;
  padding: 0 24px;
  gap: 16px;
  flex-shrink: 0;
}
.topbar-title {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.3px;
  flex: 1;
}
.topbar-user {
  font-size: 15px;
  opacity: 0.90;
  display: flex;
  align-items: center;
  gap: 10px;
}
.role-tag {
  background: rgba(255,255,255,0.18);
  border: 1px solid rgba(255,255,255,0.35);
  border-radius: 3px;
  font-size: 12px;
  font-weight: 700;
  padding: 2px 7px;
  letter-spacing: 0.5px;
}
.btn-topbar-logout {
  background: transparent;
  color: rgba(255,255,255,0.88);
  border: 1px solid rgba(255,255,255,0.4);
  border-radius: var(--radius-sm);
  padding: 6px 14px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.12s;
}
.btn-topbar-logout:hover { background: rgba(255,255,255,0.15); }
</style>
