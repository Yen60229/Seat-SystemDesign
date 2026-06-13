package com.esun.seatsystem.service;

import com.esun.seatsystem.common.dto.EmployeeInfo;
import com.esun.seatsystem.common.dto.SeatChangeItem;
import com.esun.seatsystem.common.dto.SeatInfo;
import com.esun.seatsystem.common.exception.BusinessException;
import com.esun.seatsystem.dao.EmployeeDao;
import com.esun.seatsystem.dao.SeatDao;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;

/**
 * 業務層：座位安排規則與交易控制。
 */
@Service
public class SeatService {

    /** MySQL SP 內 SIGNAL 使用的自訂錯誤狀態碼 */
    private static final String BUSINESS_SQL_STATE = "45000";

    private final SeatDao seatDao;
    private final EmployeeDao employeeDao;

    public SeatService(SeatDao seatDao, EmployeeDao employeeDao) {
        this.seatDao = seatDao;
        this.employeeDao = employeeDao;
    }

    public List<Integer> getFloors() {
        return seatDao.findAllFloors();
    }

    public List<SeatInfo> getSeatsByFloor(int floorNo) {
        return seatDao.findSeatsByFloor(floorNo);
    }

    public List<EmployeeInfo> getAllEmployees() {
        return employeeDao.findAll();
    }

    /**
     * 批次套用座位異動：整批包在同一交易，任一筆失敗即全部回滾，
     * 確保 EMPLOYEE 與 SEAT_CHANGE_LOG 多表異動的一致性。
     */
    @Transactional
    public void applyChanges(List<SeatChangeItem> changes) {
        for (SeatChangeItem change : changes) {
            try {
                switch (change.action()) {
                    case ASSIGN -> {
                        if (change.empId() == null) {
                            throw new BusinessException("指派座位時員編不可為空");
                        }
                        seatDao.assignSeat(change.empId(), change.floorSeatSeq());
                    }
                    case CLEAR -> seatDao.clearSeat(change.floorSeatSeq());
                }
            } catch (DataAccessException ex) {
                throw toBusinessException(ex, change);
            }
        }
    }

    /**
     * 將資料層拋出的例外轉成可讀的業務錯誤：
     *  - SQLSTATE 45000：SP 內 SIGNAL 的業務檢查（座位已佔用、員工不存在…）
     *  - DuplicateKeyException：並發指派時撞到 UNIQUE 約束（race condition 的最後防線）
     * 其餘未知錯誤原樣拋出，交給全域處理回通用訊息。
     */
    private RuntimeException toBusinessException(DataAccessException ex, SeatChangeItem change) {
        String target = change.empId() != null
                ? "員工 %s 座位異動失敗：".formatted(change.empId())
                : "座位 %d 異動失敗：".formatted(change.floorSeatSeq());

        if (ex.getCause() instanceof SQLException sqlEx && BUSINESS_SQL_STATE.equals(sqlEx.getSQLState())) {
            return new BusinessException(target + sqlEx.getMessage());
        }
        // 兩人同時搶同一張空位：先 commit 者成功，後者撞 UNIQUE 約束
        if (ex instanceof DuplicateKeyException) {
            return new BusinessException(target + "座位已被其他人佔用，請重新整理後再試");
        }
        return ex;
    }
}
