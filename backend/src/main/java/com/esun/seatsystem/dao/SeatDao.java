package com.esun.seatsystem.dao;

import com.esun.seatsystem.common.dto.SeatInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 資料層：座位相關資料存取。
 * 一律透過 Stored Procedure 並以 PreparedStatement 參數綁定，防止 SQL Injection。
 */
@Repository
public class SeatDao {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<SeatInfo> SEAT_MAPPER = (rs, rowNum) -> new SeatInfo(
            rs.getInt("FLOOR_SEAT_SEQ"),
            rs.getInt("FLOOR_NO"),
            rs.getInt("SEAT_NO"),
            rs.getInt("POS_ROW"),
            rs.getInt("POS_COL"),
            rs.getString("SEAT_TYPE"),
            rs.getString("EMP_ID"),
            rs.getString("NAME"));

    public SeatDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 取得所有樓層編號 */
    public List<Integer> findAllFloors() {
        return jdbcTemplate.query("CALL SP_GET_FLOORS()",
                (rs, rowNum) -> rs.getInt("FLOOR_NO"));
    }

    /** 取得指定樓層全部座位（含佔用者） */
    public List<SeatInfo> findSeatsByFloor(int floorNo) {
        return jdbcTemplate.query("CALL SP_GET_SEATS_BY_FLOOR(?)", SEAT_MAPPER, floorNo);
    }

    /** 指派座位（SP 內檢查座位狀態，違規時 SIGNAL 錯誤） */
    public void assignSeat(String empId, int floorSeatSeq) {
        jdbcTemplate.update("CALL SP_ASSIGN_SEAT(?, ?)", empId, floorSeatSeq);
    }

    /** 清除座位佔用 */
    public void clearSeat(int floorSeatSeq) {
        jdbcTemplate.update("CALL SP_CLEAR_SEAT(?)", floorSeatSeq);
    }
}
