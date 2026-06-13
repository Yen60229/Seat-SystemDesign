package com.esun.seatsystem.dao;

import com.esun.seatsystem.common.dto.EmployeeInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 資料層：員工相關資料存取（透過 Stored Procedure）。
 */
@Repository
public class EmployeeDao {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<EmployeeInfo> EMPLOYEE_MAPPER = (rs, rowNum) -> new EmployeeInfo(
            rs.getString("EMP_ID"),
            rs.getString("NAME"),
            rs.getString("EMAIL"),
            (Integer) rs.getObject("FLOOR_SEAT_SEQ"),
            (Integer) rs.getObject("FLOOR_NO"),
            (Integer) rs.getObject("SEAT_NO"));

    public EmployeeDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 取得全部員工（含目前座位） */
    public List<EmployeeInfo> findAll() {
        return jdbcTemplate.query("CALL SP_GET_ALL_EMPLOYEES()", EMPLOYEE_MAPPER);
    }
}
