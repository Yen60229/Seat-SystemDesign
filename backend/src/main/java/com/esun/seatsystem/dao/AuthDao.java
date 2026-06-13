package com.esun.seatsystem.dao;

import com.esun.seatsystem.common.dto.LoginInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 資料層：認證相關存取（透過 Stored Procedure）。
 */
@Repository
public class AuthDao {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<LoginInfo> LOGIN_MAPPER = (rs, rowNum) -> new LoginInfo(
            rs.getString("EMP_ID"),
            rs.getString("NAME"),
            rs.getString("EMAIL"),
            rs.getString("ROLE"),
            rs.getString("PASSWORD_HASH"));

    public AuthDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 取得登入資訊；查無此員工回傳 null */
    public LoginInfo findLoginInfo(String empId) {
        List<LoginInfo> result = jdbcTemplate.query("CALL SP_GET_LOGIN_INFO(?)", LOGIN_MAPPER, empId);
        return result.isEmpty() ? null : result.get(0);
    }

    /** 更新密碼雜湊 */
    public void updatePassword(String empId, String passwordHash) {
        jdbcTemplate.update("CALL SP_UPDATE_PASSWORD(?, ?)", empId, passwordHash);
    }
}
