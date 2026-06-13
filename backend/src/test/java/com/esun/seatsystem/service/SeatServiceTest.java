package com.esun.seatsystem.service;

import com.esun.seatsystem.common.dto.SeatChangeItem;
import com.esun.seatsystem.common.exception.BusinessException;
import com.esun.seatsystem.dao.EmployeeDao;
import com.esun.seatsystem.dao.SeatDao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 業務層測試：聚焦在批次異動的交易行為與例外轉換。
 * 用 Mockito 隔離 DAO，不依賴實際資料庫，mvn test 不需啟動 MySQL。
 */
@ExtendWith(MockitoExtension.class)
class SeatServiceTest {

    @Mock
    private SeatDao seatDao;

    @Mock
    private EmployeeDao employeeDao;

    @InjectMocks
    private SeatService seatService;

    private static SeatChangeItem assign(String empId, int seq) {
        return new SeatChangeItem(SeatChangeItem.Action.ASSIGN, empId, seq);
    }

    private static SeatChangeItem clear(int seq) {
        return new SeatChangeItem(SeatChangeItem.Action.CLEAR, null, seq);
    }

    @Test
    @DisplayName("批次內含 ASSIGN 與 CLEAR，會逐筆呼叫對應的 DAO 方法")
    void applyChanges_dispatchesEachItem() {
        seatService.applyChanges(List.of(assign("A0001", 6), clear(3)));

        verify(seatDao).assignSeat("A0001", 6);
        verify(seatDao).clearSeat(3);
    }

    @Test
    @DisplayName("SP 以 SIGNAL（SQLSTATE 45000）回報座位已佔用，轉成 BusinessException")
    void applyChanges_mapsSpSignalToBusinessException() {
        DataIntegrityViolationException spError = new DataIntegrityViolationException(
                "occupied", new SQLException("座位已被佔用", "45000"));
        doThrow(spError).when(seatDao).assignSeat("A0009", 9);

        assertThatThrownBy(() -> seatService.applyChanges(List.of(assign("A0009", 9))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("座位已被佔用");
    }

    @Test
    @DisplayName("並發指派撞 UNIQUE 約束（DuplicateKeyException），轉成可讀的 race condition 提示")
    void applyChanges_mapsDuplicateKeyToBusinessException() {
        DuplicateKeyException raceError = new DuplicateKeyException(
                "duplicate", new SQLException("Duplicate entry", "23000", 1062));
        doThrow(raceError).when(seatDao).assignSeat("A0008", 6);

        assertThatThrownBy(() -> seatService.applyChanges(List.of(assign("A0008", 6))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("座位已被其他人佔用");
    }

    @Test
    @DisplayName("第一筆成功、第二筆失敗時直接中止；交易回滾交給 @Transactional，不會再處理後續")
    void applyChanges_stopsOnFirstFailure() {
        // 第一筆 A0008 也會呼叫 assignSeat，故此 stub 用 lenient 避免嚴格模式誤判參數不匹配
        lenient().doThrow(new DataIntegrityViolationException("occupied", new SQLException("座位已被佔用", "45000")))
                .when(seatDao).assignSeat("A0009", 9);

        List<SeatChangeItem> batch = List.of(assign("A0008", 6), assign("A0009", 9), clear(3));

        assertThatThrownBy(() -> seatService.applyChanges(batch))
                .isInstanceOf(BusinessException.class);

        verify(seatDao).assignSeat("A0008", 6); // 第一筆有執行（後續由交易回滾）
        verify(seatDao, never()).clearSeat(3);   // 第三筆不會被執行
    }

    @Test
    @DisplayName("ASSIGN 但員編為空，視為業務錯誤")
    void applyChanges_rejectsAssignWithoutEmpId() {
        assertThatThrownBy(() -> seatService.applyChanges(List.of(assign(null, 6))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("員編不可為空");

        verify(seatDao, never()).assignSeat(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("查詢樓層座位會委派給 SeatDao")
    void getSeatsByFloor_delegatesToDao() {
        seatService.getSeatsByFloor(10);
        verify(seatDao).findSeatsByFloor(10);

        assertThat(seatService).isNotNull();
    }
}
