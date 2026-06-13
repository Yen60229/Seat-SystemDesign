<script setup>
import { ref, computed, nextTick, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { auth, isAdmin, logout } from '../stores/auth';
import client from '../api/client';

const router = useRouter();

// ── 資料狀態 ──
const floors        = ref([]);
const currentFloor  = ref(null);
const seats         = ref([]);       // [{ floorSeatSeq, seatNo, posRow, posCol, seatType, empId, empName }]
const employees     = ref([]);
const announcements = ref([]);

// ── 座位操作狀態 ──
const selectedEmpId = ref('');
const employeeQuery = ref('');
const employeeSearchOpen = ref(false);
const employeeSearchInput = ref(null);
const pending       = ref({});       // floorSeatSeq -> { action, empId }
const submitting    = ref(false);
const pendingSeat   = ref(null);     // 點空位時若尚未選人，暫存該座位，選完人自動配上

// ── 公告狀態 ──
const showAnnForm   = ref(false);
const annForm       = ref({ annSeq: null, title: '', content: '' });
const annLoading    = ref(false);

// ── 訊息 ──
const banner = ref(null);   // { type: 'success'|'error'|'info'|'warn', text }

// ── 計算屬性 ──
const pendingCount = computed(() => Object.keys(pending.value).length);

const sortedEmployees = computed(() =>
  [...employees.value].sort((a, b) => (a.floorSeatSeq ? 1 : 0) - (b.floorSeatSeq ? 1 : 0)),
);

const selectedEmployee = computed(() =>
  employees.value.find((emp) => emp.empId === selectedEmpId.value) ?? null,
);

const filteredEmployees = computed(() => {
  const query = normalizeKeyword(employeeQuery.value);
  const source = sortedEmployees.value;
  if (!query) return source.slice(0, 30);

  return source
    .filter((emp) => {
      const empId = normalizeKeyword(emp.empId);
      const name = normalizeKeyword(emp.name);
      return empId.includes(query) || name.includes(query);
    })
    .slice(0, 30);
});

// 將平坦座位陣列轉為 2D map，用 posRow/posCol 定位
const gridLayout = computed(() => {
  if (!seats.value.length) return { rows: 0, cols: 0, map: {} };
  let maxRow = 0, maxCol = 0;
  for (const s of seats.value) {
    if (s.posRow > maxRow) maxRow = s.posRow;
    if (s.posCol > maxCol) maxCol = s.posCol;
  }
  const map = {};
  for (const s of seats.value) {
    map[`${s.posRow}-${s.posCol}`] = s;
  }
  return { rows: maxRow, cols: maxCol, map };
});

// ── 工具函式 ──
function showBanner(type, text, ms = 4000) {
  banner.value = { type, text };
  if (ms) setTimeout(() => { if (banner.value?.text === text) banner.value = null; }, ms);
}

function empLabel(emp) {
  const seat = emp.floorSeatSeq ? `（${emp.floorNo}F-${emp.seatNo}）` : '（未配位）';
  return `${emp.empId} ${emp.name} ${seat}`;
}

function normalizeKeyword(value) {
  return String(value ?? '').trim().toUpperCase();
}

function selectEmployee(emp) {
  selectedEmpId.value = emp.empId;
  employeeQuery.value = `${emp.empId} ${emp.name}`;
  employeeSearchOpen.value = false;

  // 若是從點空位觸發的搜尋，選完人後直接配上那個座位
  if (pendingSeat.value) {
    const seat = pendingSeat.value;
    pendingSeat.value = null;
    for (const [key, p] of Object.entries(pending.value)) {
      if (p.action === 'ASSIGN' && p.empId === emp.empId) {
        delete pending.value[key];
      }
    }
    pending.value[seat.floorSeatSeq] = { action: 'ASSIGN', empId: emp.empId };
    showBanner('info', `已暫選：${emp.empId} ${emp.name} → ${currentFloor.value}F 座位 ${seat.seatNo}，確認無誤後按「確認送出」`);
  }
}

function clearSelectedEmployee() {
  selectedEmpId.value = '';
  employeeQuery.value = '';
  employeeSearchOpen.value = true;
  nextTick(() => employeeSearchInput.value?.focus());
}

function openEmployeeSearch() {
  employeeSearchOpen.value = true;
  nextTick(() => employeeSearchInput.value?.focus());
}

function onEmployeeInput() {
  selectedEmpId.value = '';
  employeeSearchOpen.value = true;
}

function closeEmployeeSearchSoon() {
  setTimeout(() => {
    employeeSearchOpen.value = false;
  }, 120);
}

function seatState(seat) {
  if (pending.value[seat.floorSeatSeq]) return 'selected';
  if (seat.seatType === 'MANAGER') return seat.empId ? 'manager-occ' : 'manager';
  return seat.empId ? 'occupied' : 'empty';
}

// 座位第一行：員編 / 待指派員編 / 空
function seatLine1(seat) {
  const p = pending.value[seat.floorSeatSeq];
  if (p) return p.action === 'ASSIGN' ? p.empId : seat.empId ?? '';
  return seat.empId ?? '';
}

// 座位第二行：姓名 / 清除 / 空位
function seatLine2(seat) {
  const p = pending.value[seat.floorSeatSeq];
  if (p) return p.action === 'ASSIGN' ? '▸ 待指派' : '✕ 清除';
  if (seat.empId) return seat.empName ?? '';
  return '';
}

function seatTitle(seat) {
  const p = pending.value[seat.floorSeatSeq];
  if (p) return p.action === 'ASSIGN' ? `指派 ${p.empId}` : `清除 ${seat.empId}`;
  if (seat.empId) return `${seat.empId} ${seat.empName ?? ''}`.trim();
  return `${seat.seatNo}（空位）`;
}

// ── API 載入 ──
async function loadFloors() {
  const { data } = await client.get('/floors');
  floors.value = data.data;
  if (!currentFloor.value && floors.value.length) currentFloor.value = floors.value[0];
}

async function loadSeats() {
  if (!currentFloor.value) return;
  const { data } = await client.get(`/floors/${currentFloor.value}/seats`);
  seats.value = data.data;
}

async function loadEmployees() {
  const { data } = await client.get('/employees');
  employees.value = data.data;
}

async function loadAnnouncements() {
  const { data } = await client.get('/announcements');
  announcements.value = data.data;
}

async function switchFloor(floor) {
  currentFloor.value = floor;
  pending.value = {};
  pendingSeat.value = null;
  await loadSeats();
}

// ── 座位點擊 ──
function onSeatClick(seat) {
  if (!isAdmin()) return;   // 非 ADMIN 座位唯讀
  banner.value = null;
  const seq = seat.floorSeatSeq;

  if (pending.value[seq]) {
    delete pending.value[seq];
    return;
  }
  if (seat.empId) {
    pending.value[seq] = { action: 'CLEAR', empId: seat.empId };
    return;
  }
  if (!selectedEmpId.value) {
    // 記住這個座位，讓使用者選完人後自動配上，不需要再點一次
    pendingSeat.value = seat;
    openEmployeeSearch();
    showBanner('info', `座位 ${seat.seatNo} 已記住，請從上方搜尋框選擇員工`);
    return;
  }
  for (const [key, p] of Object.entries(pending.value)) {
    if (p.action === 'ASSIGN' && p.empId === selectedEmpId.value) {
      delete pending.value[key];
    }
  }
  pending.value[seq] = { action: 'ASSIGN', empId: selectedEmpId.value };
}

// ── 批次送出 ──
async function submitChanges() {
  if (!pendingCount.value || submitting.value) return;
  submitting.value = true;
  banner.value = null;
  try {
    const changes = Object.entries(pending.value).map(([seq, p]) => ({
      action: p.action,
      empId: p.action === 'ASSIGN' ? p.empId : null,
      floorSeatSeq: Number(seq),
    }));
    await client.post('/seats/batch', { changes });
    pending.value = {};
    selectedEmpId.value = '';
    employeeQuery.value = '';
    await Promise.all([loadSeats(), loadEmployees()]);
    showBanner('success', `${changes.length} 筆座位異動已成功寫入`);
  } catch (err) {
    const msg = err.response?.data?.message ?? '送出失敗，請稍後再試';
    showBanner('error', `${msg}（整批已回滾，無任何異動）`, 6000);
  } finally {
    submitting.value = false;
  }
}

function resetPending() {
  pending.value = {};
  pendingSeat.value = null;
  banner.value = null;
}

// ── 公告 CRUD ──
function openAnnCreate() {
  annForm.value = { annSeq: null, title: '', content: '' };
  showAnnForm.value = true;
}

function openAnnEdit(ann) {
  annForm.value = { annSeq: ann.annSeq, title: ann.title, content: ann.content };
  showAnnForm.value = true;
}

async function saveAnn() {
  if (!annForm.value.title.trim() || !annForm.value.content.trim()) return;
  annLoading.value = true;
  try {
    if (annForm.value.annSeq) {
      await client.put(`/announcements/${annForm.value.annSeq}`, {
        title: annForm.value.title,
        content: annForm.value.content,
      });
    } else {
      await client.post('/announcements', {
        title: annForm.value.title,
        content: annForm.value.content,
      });
    }
    showAnnForm.value = false;
    await loadAnnouncements();
    showBanner('success', `公告已${annForm.value.annSeq ? '更新' : '新增'}`);
  } catch (err) {
    showBanner('error', err.response?.data?.message ?? '操作失敗');
  } finally {
    annLoading.value = false;
  }
}

async function deleteAnn(ann) {
  if (!confirm(`確定要刪除公告「${ann.title}」？`)) return;
  try {
    await client.delete(`/announcements/${ann.annSeq}`);
    await loadAnnouncements();
    showBanner('success', '公告已刪除');
  } catch (err) {
    showBanner('error', err.response?.data?.message ?? '刪除失敗');
  }
}

// ── 登出 ──
async function handleLogout() {
  await logout();
  router.push('/login');
}

onMounted(async () => {
  await loadFloors();
  await Promise.all([loadSeats(), loadEmployees(), loadAnnouncements()]);
});
</script>

<template>
  <div class="page">
    <!-- 頂部導覽列 -->
    <header class="topbar">
      <span class="topbar-title">座位管理系統</span>
      <span class="topbar-user">
        {{ auth.user?.name }}（{{ auth.user?.empId }}）
        <span v-if="isAdmin()" class="role-tag">管理員</span>
      </span>
      <button class="btn-topbar-logout" @click="handleLogout">登出</button>
    </header>

    <main class="main-content">
      <!-- 全域訊息橫幅 -->
      <div v-if="banner" :class="['banner', `banner-${banner.type}`]" style="margin-bottom:16px">
        {{ banner.text }}
        <button class="close-banner" @click="banner = null" aria-label="關閉">✕</button>
      </div>

      <!-- 公告欄 -->
      <section class="panel">
        <div class="panel-head">
          <h2 class="panel-title">公告欄</h2>
          <button v-if="isAdmin()" class="btn btn-primary btn-sm" @click="openAnnCreate">新增公告</button>
        </div>

        <p v-if="!announcements.length" class="empty-text">目前沒有公告。</p>

        <div v-else class="ann-list">
          <article v-for="ann in announcements" :key="ann.annSeq" class="ann-row">
            <div class="ann-row-body">
              <div class="ann-row-top">
                <span class="ann-row-title">{{ ann.title }}</span>
                <span class="ann-row-meta">{{ ann.createdBy }}・{{ ann.createdAt?.slice(0,10) }}</span>
              </div>
              <p class="ann-row-content">{{ ann.content }}</p>
            </div>
            <div v-if="isAdmin()" class="ann-row-actions">
              <button class="btn btn-secondary btn-sm" @click="openAnnEdit(ann)">編輯</button>
              <button class="btn btn-danger btn-sm" @click="deleteAnn(ann)">刪除</button>
            </div>
          </article>
        </div>
      </section>

      <!-- 座位配置 -->
      <section class="panel">
        <div class="panel-head">
          <h2 class="panel-title">座位配置</h2>
          <span class="seat-count">
            已佔用 {{ seats.filter(s => s.empId).length }} / {{ seats.length }} 席
          </span>
        </div>

        <!-- 管理員配位工具列 -->
        <div v-if="isAdmin()" class="assign-bar">
          <div class="assign-search-wrap">
            <label class="form-label" for="employeeSearch">配位員工</label>
            <div class="assign-search-box">
              <input
                id="employeeSearch"
                ref="employeeSearchInput"
                v-model="employeeQuery"
                class="form-control"
                type="text"
                placeholder="輸入員編或姓名搜尋…"
                autocomplete="off"
                :disabled="submitting"
                @input="onEmployeeInput"
                @focus="employeeSearchOpen = true"
                @blur="closeEmployeeSearchSoon"
              />
              <button
                v-if="employeeQuery"
                type="button"
                class="search-clear"
                aria-label="清除"
                @mousedown.prevent
                @click="clearSelectedEmployee"
              >×</button>
            </div>
            <div v-if="employeeSearchOpen" class="search-dropdown" role="listbox">
              <button
                v-for="emp in filteredEmployees"
                :key="emp.empId"
                type="button"
                class="search-option"
                :class="{ selected: selectedEmpId === emp.empId }"
                @mousedown.prevent="selectEmployee(emp)"
              >
                <span class="opt-id">{{ emp.empId }}</span>
                <span class="opt-name">{{ emp.name }}</span>
                <span class="opt-seat">{{ emp.floorSeatSeq ? `${emp.floorNo}F-${emp.seatNo}` : '未配位' }}</span>
              </button>
              <div v-if="!filteredEmployees.length" class="search-empty">查無符合的員工</div>
            </div>
          </div>

          <div class="assign-selected">
            <span class="form-label">已選員工</span>
            <div class="selected-display">
              <template v-if="selectedEmployee">
                <span class="selected-name">{{ selectedEmployee.empId }} {{ selectedEmployee.name }}</span>
                <button class="btn btn-secondary btn-sm" @click="clearSelectedEmployee">換人</button>
              </template>
              <span v-else class="selected-placeholder">尚未選擇 — 可先點空位再選人</span>
            </div>
          </div>

          <div v-if="pendingCount" class="assign-actions">
            <button class="btn btn-secondary btn-sm" :disabled="submitting" @click="resetPending">取消</button>
            <button class="btn btn-primary" :disabled="submitting" @click="submitChanges">
              {{ submitting ? '送出中…' : `送出 ${pendingCount} 筆異動` }}
            </button>
          </div>
        </div>

        <!-- 樓層頁籤 -->
        <div class="floor-tabs" role="tablist">
          <button
            v-for="floor in floors"
            :key="floor"
            :class="['floor-tab', { active: floor === currentFloor }]"
            role="tab"
            :aria-selected="floor === currentFloor"
            @click="switchFloor(floor)"
          >{{ floor }} 樓</button>
        </div>

        <!-- 圖例 -->
        <div class="legend">
          <span class="legend-item"><i class="swatch s-empty"></i>空位</span>
          <span class="legend-item"><i class="swatch s-occ"></i>使用中</span>
          <span class="legend-item"><i class="swatch s-mgr"></i>主管席（空位）</span>
          <span class="legend-item"><i class="swatch s-mgr-occ"></i>主管席（使用中）</span>
          <span class="legend-item"><i class="swatch s-sel"></i>暫選中</span>
        </div>

        <!-- 座位平面圖 -->
        <div class="floor-map">
          <div
            class="seat-grid"
            :style="{
              gridTemplateColumns: `repeat(${gridLayout.cols}, 62px)`,
              gridTemplateRows:    `repeat(${gridLayout.rows}, 76px)`,
            }"
          >
            <template v-for="r in gridLayout.rows" :key="r">
              <template v-for="c in gridLayout.cols" :key="c">
                <button
                  v-if="gridLayout.map[`${r}-${c}`]"
                  :class="['seat', `s-${seatState(gridLayout.map[`${r}-${c}`])}`]"
                  :title="seatTitle(gridLayout.map[`${r}-${c}`])"
                  :style="{ gridRow: r, gridColumn: c }"
                  @click="onSeatClick(gridLayout.map[`${r}-${c}`])"
                >
                  <span class="seat-no">{{ gridLayout.map[`${r}-${c}`].seatNo }}</span>
                  <span class="seat-line1">{{ seatLine1(gridLayout.map[`${r}-${c}`]) }}</span>
                  <span class="seat-line2">{{ seatLine2(gridLayout.map[`${r}-${c}`]) }}</span>
                </button>
                <div v-else :style="{ gridRow: r, gridColumn: c }" aria-hidden="true"></div>
              </template>
            </template>
          </div>
        </div>

        <p v-if="!isAdmin()" class="readonly-note">目前為唯讀模式，座位異動由管理員操作。</p>
      </section>
    </main>

    <!-- 公告 Modal -->
    <div v-if="showAnnForm" class="overlay" @click.self="showAnnForm = false">
      <div class="modal">
        <div class="modal-head">
          <h3>{{ annForm.annSeq ? '編輯公告' : '新增公告' }}</h3>
          <button class="modal-close" @click="showAnnForm = false">×</button>
        </div>
        <form @submit.prevent="saveAnn" novalidate>
          <div class="form-group">
            <label class="form-label" for="ann-title">標題</label>
            <input id="ann-title" v-model="annForm.title" class="form-control"
              type="text" maxlength="100" placeholder="最多 100 字" :disabled="annLoading" />
          </div>
          <div class="form-group" style="margin-top:14px">
            <label class="form-label" for="ann-content">內容</label>
            <textarea id="ann-content" v-model="annForm.content" class="form-control ann-textarea"
              maxlength="1000" placeholder="最多 1000 字" :disabled="annLoading"></textarea>
          </div>
          <div class="modal-foot">
            <button type="button" class="btn btn-secondary" @click="showAnnForm = false">取消</button>
            <button type="submit" class="btn btn-primary"
              :disabled="annLoading || !annForm.title.trim() || !annForm.content.trim()">
              {{ annLoading ? '儲存中…' : '儲存' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ── 版面 ── */
.panel {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  margin-bottom: 20px;
  overflow: hidden;
}
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid var(--border);
  gap: 12px;
}
.panel-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text);
}
.empty-text {
  padding: 20px;
  font-size: 16px;
  color: var(--text-muted);
}
.seat-count {
  font-size: 15px;
  color: var(--text-muted);
}
/* 橫幅關閉 */
.close-banner {
  float: right;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 15px;
  opacity: 0.55;
  margin-left: 12px;
}
.close-banner:hover { opacity: 1; }

/* ── 公告列表 ── */
.ann-list { }
.ann-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--border);
}
.ann-row:last-child { border-bottom: none; }
.ann-row-body { flex: 1; min-width: 0; }
.ann-row-top {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 5px;
  flex-wrap: wrap;
}
.ann-row-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--text);
}
.ann-row-meta {
  font-size: 14px;
  color: var(--text-muted);
}
.ann-row-content {
  font-size: 16px;
  color: var(--text-sub);
  line-height: 1.6;
  white-space: pre-line;
}
.ann-row-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
  align-items: flex-start;
}

/* ── 配位工具列 ── */
.assign-bar {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--border);
  background: #fafbfc;
  flex-wrap: wrap;
}
.assign-search-wrap {
  position: relative;
  flex: 1;
  min-width: 220px;
  max-width: 340px;
}
.assign-search-box { position: relative; }
.assign-search-box .form-control { padding-right: 36px; }
.search-clear {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  font-size: 18px;
  color: var(--text-muted);
  line-height: 1;
  padding: 0 2px;
}
.search-clear:hover { color: var(--text); }
.search-dropdown {
  position: absolute;
  z-index: 30;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  max-height: 280px;
  overflow-y: auto;
  background: var(--surface);
  border: 1px solid var(--border-dark);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
}
.search-option {
  width: 100%;
  display: grid;
  grid-template-columns: 70px 1fr auto;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border: none;
  border-bottom: 1px solid var(--border);
  background: none;
  cursor: pointer;
  text-align: left;
  color: var(--text);
  font-size: 15px;
}
.search-option:last-child { border-bottom: none; }
.search-option:hover, .search-option.selected { background: var(--green-light); }
.opt-id   { font-weight: 600; font-size: 14px; color: var(--green); }
.opt-name { color: var(--text); }
.opt-seat { font-size: 13px; color: var(--text-muted); text-align: right; }
.search-empty { padding: 14px; font-size: 15px; color: var(--text-muted); }

.assign-selected { min-width: 200px; }
.selected-display {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface);
  min-height: 42px;
}
.selected-name { font-size: 16px; font-weight: 600; flex: 1; }
.selected-placeholder { font-size: 15px; color: var(--text-muted); }

.assign-actions {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  padding-bottom: 1px;
}

/* ── 樓層頁籤 ── */
.floor-tabs {
  display: flex;
  gap: 0;
  padding: 0 20px;
  border-bottom: 1px solid var(--border);
  background: #fafbfc;
  overflow-x: auto;
}
.floor-tab {
  padding: 12px 22px;
  border: none;
  border-bottom: 3px solid transparent;
  background: none;
  font-size: 16px;
  font-weight: 500;
  color: var(--text-muted);
  cursor: pointer;
  white-space: nowrap;
  transition: color 0.12s, border-color 0.12s;
}
.floor-tab:hover { color: var(--text); }
.floor-tab.active {
  color: var(--green);
  border-bottom-color: var(--green);
  font-weight: 700;
}

/* ── 圖例 ── */
.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  padding: 12px 20px;
  border-bottom: 1px solid var(--border);
  font-size: 15px;
  color: var(--text-muted);
  background: #fafbfc;
}
.legend-item { display: flex; align-items: center; gap: 6px; }
.swatch {
  width: 14px;
  height: 14px;
  border-radius: 3px;
  display: inline-block;
  flex-shrink: 0;
}
.s-empty     { background: #d4edda; border: 1px solid #28a745; }
.s-occ       { background: #f8d7da; border: 1px solid #dc3545; }
.s-mgr       { background: #dce0f5; border: 1px solid #6673d0; }
.s-mgr-occ   { background: #e2d5f0; border: 1px solid #8b5cf6; }
.s-sel       { background: #fff3cd; border: 1px solid #e5a000; }

/* ── 座位格 ── */
.floor-map {
  padding: 24px 20px;
  overflow-x: auto;
}
.seat-grid {
  display: grid;
  gap: 14px;
  min-width: fit-content;
}
.seat {
  width: 76px;
  height: 90px;
  border-radius: var(--radius-sm);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  cursor: pointer;
  border: 1px solid transparent;
  padding: 6px 4px;
  transition: filter 0.1s;
}
.seat:hover { filter: brightness(0.93); }
.seat.s-empty     { background: #d4edda; border-color: #28a745; }
.seat.s-occupied  { background: #f8d7da; border-color: #dc3545; }
.seat.s-manager   { background: #dce0f5; border-color: #6673d0; }
.seat.s-manager-occ { background: #e2d5f0; border-color: #8b5cf6; }
.seat.s-selected  { background: #fff3cd; border-color: #e5a000; outline: 2px solid #e5a000; }

.seat-no {
  font-size: 11px;
  color: var(--text-muted);
  line-height: 1;
}
.seat-line1 {
  font-size: 12px;
  font-weight: 700;
  text-align: center;
  word-break: break-all;
  line-height: 1.3;
  color: var(--text);
  width: 100%;
}
.seat-line2 {
  font-size: 12px;
  font-weight: 500;
  text-align: center;
  word-break: break-all;
  line-height: 1.3;
  color: var(--text-sub);
  width: 100%;
}
.seat.s-empty .seat-line2   { color: #555; font-weight: 600; }
.seat.s-selected .seat-line2 { color: #7d5a00; font-style: italic; }

.readonly-note {
  padding: 12px 20px;
  font-size: 15px;
  color: var(--text-muted);
  border-top: 1px solid var(--border);
  text-align: right;
}

/* ── Modal ── */
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  z-index: 100;
}
.modal {
  background: var(--surface);
  border-radius: var(--radius);
  width: 100%;
  max-width: 500px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.18);
}
.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border);
}
.modal-head h3 { font-size: 18px; font-weight: 700; }
.modal-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: var(--text-muted);
  line-height: 1;
}
.modal-close:hover { color: var(--text); }
.modal form { padding: 22px; }
.ann-textarea {
  min-height: 140px;
  resize: vertical;
  font-size: 16px;
  line-height: 1.6;
}
.modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}
</style>
