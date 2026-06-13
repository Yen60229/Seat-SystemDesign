# 員工座位安排系統

人資部門使用的員工座位管理系統。

---

## 系統需求

| 工具 | 版本 |
|---|---|
| JDK | 17 以上 |
| Maven | 3.6 以上 |
| Node.js | 18 以上 |
| Docker | 任意版本（用於 MySQL） |

---

## 快速啟動（5 步驟）

### Step 1 — 啟動 MySQL（Docker）

```bash
docker run -d --name seat-mysql \
  -e MYSQL_ROOT_PASSWORD=seat1234 \
  -e TZ=Asia/Taipei \
  -p 3306:3306 mysql:8.0
```

> Windows PowerShell 請將 `\` 換成 `` ` ``（反引號）。

### Step 2 — 建立資料庫與種子資料

```bash
docker cp DB/01_DDL.sql seat-mysql:/tmp/
docker cp DB/02_DML.sql seat-mysql:/tmp/
docker exec seat-mysql sh -c "mysql -uroot -pseat1234 --default-character-set=utf8mb4 < /tmp/01_DDL.sql"
docker exec seat-mysql sh -c "mysql -uroot -pseat1234 --default-character-set=utf8mb4 < /tmp/02_DML.sql"
```

`01_DDL.sql`：建立資料庫、所有資料表、Stored Procedures。  
`02_DML.sql`：種子員工 15 筆、3 個樓層各 20 個座位（含部分已配位）。

### Step 3 — 建置前端

```bash
cd frontend
npm install
npm run build
```

產物自動輸出至 `backend/src/main/resources/static/`，後端直接服務。

### Step 4 — 打包後端

```bash
cd backend
mvn package -DskipTests
```

### Step 5 — 啟動後端

```bash
cd backend
java -jar target/seat-system-1.0.0.jar
```

瀏覽器開啟 **http://localhost:8080**

---

## 測試帳號（完整 15 筆）

> 密碼格式：`出生年月日（YYYYMMDD）＋ 身分證後 4 碼`（虛構個資，僅供測試）

| 員編 | 姓名 | 角色 | 測試密碼 | 目前座位 |
|---|---|---|---|---|
| **A0001** | 王小明 | **ADMIN** | `198503126789` | 10F 座位 1 |
| A0002 | 李小華 | EMPLOYEE | `197808154567` | 10F 座位 3 |
| A0003 | 張大同 | EMPLOYEE | `198012289012` | 10F 座位 7 |
| A0004 | 陳美玲 | EMPLOYEE | `197603183456` | 11F 座位 2 |
| A0005 | 林志偉 | EMPLOYEE | `198209071234` | 11F 座位 5 |
| A0006 | 黃淑芬 | EMPLOYEE | `197504268901` | 12F 座位 1 |
| A0007 | 吳建宏 | EMPLOYEE | `198807142345` | 12F 座位 13 |
| A0008 | 劉雅婷 | EMPLOYEE | `199102056678` | 未配位 |
| A0009 | 蔡明哲 | EMPLOYEE | `198711309123` | 未配位 |
| A0010 | 鄭佳蓉 | EMPLOYEE | `199005197890` | 未配位 |
| B0001 | 謝俊傑 | EMPLOYEE | `198606043210` | 未配位 |
| B0002 | 許芳瑜 | EMPLOYEE | `197912185432` | 未配位 |
| B0003 | 楊宗翰 | EMPLOYEE | `198404226543` | 未配位 |
| B0004 | 蕭惠君 | EMPLOYEE | `197701198765` | 未配位 |
| B0005 | 潘冠宇 | EMPLOYEE | `199308114321` | 未配位 |

**忘記密碼流程（測試用）：**
1. 點選「忘記密碼？」
2. 輸入員編送出 → 系統回傳驗證碼（開發模式直接顯示在畫面上）
3. 輸入 6 位驗證碼 → 設定新密碼（至少 8 碼，含英文 + 數字）

---

## 系統功能

| 功能 | ADMIN | EMPLOYEE |
|---|---|---|
| 登入 / 登出 | ✅ | ✅ |
| 忘記密碼 / 重設 | ✅ | ✅ |
| 瀏覽各樓層座位圖 | ✅ | ✅ |
| 搜尋員工並指派座位 | ✅ | — |
| 清除座位 | ✅ | — |
| 批次送出座位異動 | ✅ | — |
| 瀏覽公告 | ✅ | ✅ |
| 新增 / 編輯 / 刪除公告 | ✅ | — |

---

## 系統架構

```
瀏覽器（Vue 3 SPA，vue-router 4）
   │  HTTP / JSON（RESTful API）
   ▼
Spring Boot 3.4（JDK 17，內嵌 Tomcat）
   ├── controller   REST API、Bean Validation 輸入驗證
   ├── service      業務邏輯、@Transactional 交易控制
   ├── dao          JdbcTemplate 呼叫 Stored Procedure
   └── common       DTO、統一回應格式、全域例外處理、JWT、Security
   │  JDBC（一律透過 Stored Procedure）
   ▼
MySQL 8.0（Docker）
```

---

## 技術棧

| 項目 | 技術 |
|---|---|
| 前端 | Vue 3 + Vite + vue-router 4 + axios |
| 後端 | Spring Boot 3.4（JDK 17）、Spring Security、Spring JDBC |
| 認證 | JWT（HS256）+ httpOnly SameSite=Strict Cookie |
| 資料庫 | MySQL 8.0，所有存取透過 Stored Procedure |
| 建置 | Maven 3.9 |
| 測試 | JUnit 5 + Mockito（30 個單元測試） |

---

## 資料庫設計

### Employee 員工資料

| 欄位 | 型別 | 說明 |
|---|---|---|
| EMP_ID | CHAR(5) PK | 員編（Primary Key） |
| NAME | VARCHAR(50) | 員工姓名 |
| EMAIL | VARCHAR(100) | 員工電子郵件 |
| FLOOR_SEAT_SEQ | INT UNIQUE FK | 座位序號（NULL = 未配位） |
| ROLE | VARCHAR(10) | ADMIN / EMPLOYEE |
| PASSWORD_HASH | VARCHAR(100) | BCrypt（cost 12） |

### SeatingChart 樓層座位表

| 欄位 | 型別 | 說明 |
|---|---|---|
| FLOOR_SEAT_SEQ | INT PK AUTO_INCREMENT | 序號（Primary Key） |
| FLOOR_NO | INT | 樓層編號 |
| SEAT_NO | INT | 座位編號 |
| POS_ROW | INT | 格線列位（2D 座位圖定位） |
| POS_COL | INT | 格線欄位（2D 座位圖定位） |
| SEAT_TYPE | VARCHAR(10) | NORMAL / MANAGER |

### 其他資料表

| 資料表 | 說明 |
|---|---|
| SEAT_CHANGE_LOG | 每次座位異動紀錄（員編、異動前後座位、時間） |
| ANNOUNCEMENT | 公告（標題、內容、發布者員編、時間戳） |
| PASSWORD_RESET_CODE | 忘記密碼驗證碼（雜湊儲存、10 分鐘效期、最多嘗試 5 次） |

### Stored Procedures

| SP | 說明 |
|---|---|
| `SP_GET_FLOORS` | 取得所有樓層清單 |
| `SP_GET_SEATS_BY_FLOOR(p_floor_no)` | 取得樓層座位 + 佔用員工（LEFT JOIN） |
| `SP_GET_ALL_EMPLOYEES` | 員工下拉清單（含目前座位） |
| `SP_ASSIGN_SEAT(p_emp_id, p_seq)` | 指派座位：鎖定檢查 → 更新 EMPLOYEE + 寫 LOG |
| `SP_CLEAR_SEAT(p_seq)` | 清除座位：設 NULL + 寫 LOG |
| `SP_GET_LOGIN_INFO` | 取得登入所需欄位（PASSWORD_HASH、ROLE） |
| `SP_GET_ANNOUNCEMENTS` | 取得公告清單 |
| `SP_CREATE/UPDATE/DELETE_ANNOUNCEMENT` | 公告 CRUD |
| `SP_CREATE_RESET_CODE` | 建立忘記密碼驗證碼（雜湊後存入） |
| `SP_GET_ACTIVE_RESET_CODE` | 取得有效驗證碼（未過期、未使用） |
| `SP_MARK_RESET_CODE_USED` | 標記驗證碼已使用 |
| `SP_UPDATE_PASSWORD` | 更新密碼雜湊 |

---

## REST API

### 認證

| Method | Path | 說明 | 權限 |
|---|---|---|---|
| POST | `/api/auth/login` | 登入，回寫 JWT Cookie | 公開 |
| POST | `/api/auth/logout` | 登出，清除 Cookie | 登入 |
| GET | `/api/auth/me` | 取得目前使用者 | 登入 |
| POST | `/api/auth/forgot-password` | 索取驗證碼（開發模式回傳明碼） | 公開 |
| POST | `/api/auth/reset-password` | 驗證碼 + 設定新密碼 | 公開 |

### 座位 / 員工

| Method | Path | 說明 | 權限 |
|---|---|---|---|
| GET | `/api/floors` | 取得所有樓層 | 登入 |
| GET | `/api/floors/{floorNo}/seats` | 取得樓層座位（含佔用員工） | 登入 |
| GET | `/api/employees` | 取得員工清單 | 登入 |
| POST | `/api/seats/batch` | 批次送出座位異動（單一交易） | ADMIN |

### 公告

| Method | Path | 說明 | 權限 |
|---|---|---|---|
| GET | `/api/announcements` | 取得公告清單 | 登入 |
| POST | `/api/announcements` | 新增公告 | ADMIN |
| PUT | `/api/announcements/{id}` | 編輯公告 | ADMIN |
| DELETE | `/api/announcements/{id}` | 刪除公告 | ADMIN |

統一回應格式：`{ "code": "0000", "message": "成功", "data": ... }`

---

## 安全規格

| 面向 | 做法 |
|---|---|
| 密碼儲存 | BCrypt（cost 12），不可逆雜湊 |
| Token 儲存 | JWT 放 httpOnly + SameSite=Strict Cookie，JS 讀不到（防 XSS 竊取） |
| Token 內容 | HS256 簽章，含員編、角色、2 小時效期 |
| CSRF 防護 | SameSite=Strict 防止跨站請求自動帶入 Cookie |
| 帳號列舉防護 | 登入錯誤一律回「帳號或密碼錯誤」；忘記密碼無論帳號是否存在回應相同 |
| 驗證碼 | 6 碼數字，雜湊後儲存、10 分鐘效期、單次使用、最多嘗試 5 次 |
| 速率限制 | 登入連續 5 次失敗鎖定 10 分鐘（以員編為單位） |
| SQL Injection | 全程 Stored Procedure + PreparedStatement 參數綁定，無字串拼接 |
| XSS | Vue 模板插值預設轉義，不使用 `v-html` |
| 授權 | ADMIN 才能異動座位／公告；未登入 401、權限不足 403 |

---

## 設計重點

### Stored Procedure 隔離資料存取
所有 DB 操作透過 SP，應用層不拼接 SQL 字串，統一以 `CALL SP_XXX(?, ?)` PreparedStatement 參數綁定。

### 交易控制（多表異動）
`SP_ASSIGN_SEAT` / `SP_CLEAR_SEAT` 同時寫入 `EMPLOYEE` 與 `SEAT_CHANGE_LOG`。SP 不自行 COMMIT，由 `SeatService.applyChanges()` 的 `@Transactional` 統一控制：批次任一筆失敗就整批還原。

### 並發控制
- DB UNIQUE 約束：`EMPLOYEE.FLOOR_SEAT_SEQ UNIQUE`（最後防線）
- SP SELECT FOR UPDATE 鎖定：降低並發衝突
- 前端去重：同員工重複指派時自動移除前一筆

### 無狀態認證（可水平擴展）
後端不依賴 Session，多個 Application Server 實例可同時運行，一致性由資料庫保證。

### 測試策略（TDD）
30 個單元測試（JUnit 5 + Mockito），每階段先寫測試再實作，含各種邊界情境：
- 密碼雜湊、登入鎖定、帳號列舉防護
- JWT 簽發 / 驗證 / 過期 / 竄改
- 忘記密碼：驗證碼過期、超次數、已使用、密碼強度
- 座位批次交易還原、並發撞鍵
- 公告 CRUD 權限驗證、內容 XSS 防護

---

## 專案結構

```
seat-system/
├── DB/
│   ├── 01_DDL.sql          # 建庫、建表、所有 Stored Procedures
│   └── 02_DML.sql          # 種子資料（15 員工、3 樓層 × 20 座位）
├── backend/
│   └── src/main/java/
│       ├── controller/     # Auth、Seat、Announcement、Floor、Employee、Spa
│       ├── service/        # Auth、Seat、Announcement、PasswordReset
│       ├── dao/            # Auth、Seat、Employee、Announcement、ResetCode
│       └── common/         # JWT、Security、DTO、全域例外、統一回應
├── frontend/
│   └── src/
│       ├── views/          # LoginView、ForgotPasswordView、HomeView
│       ├── router/         # vue-router（未登入自動導向 /login）
│       ├── stores/         # auth.js（reactive 認證狀態）
│       └── api/            # axios shared instance
└── docs/
    ├── 設計說明.md          # 基礎架構設計
    ├── 設計說明-進階功能.md  # 認證、公告、UI 優化設計
    └── seed-passwords.md   # 完整種子帳號密碼
```
