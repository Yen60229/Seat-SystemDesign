package com.esun.seatsystem.dao;

import com.esun.seatsystem.common.dto.ResetCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 資料層：密碼重設驗證碼存取（透過 Stored Procedure）。
 */
@Repository
public class ResetCodeDao {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<ResetCode> MAPPER = (rs, rowNum) -> new ResetCode(
            rs.getLong("CODE_SEQ"),
            rs.getString("EMP_ID"),
            rs.getString("CODE_HASH"),
            rs.getTimestamp("EXPIRES_AT").toLocalDateTime(),
            rs.getInt("ATTEMPTS"),
            rs.getBoolean("USED"));

    public ResetCodeDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 建立驗證碼（SP 內會先作廢該員工舊的未使用碼） */
    public void createCode(String empId, String codeHash, LocalDateTime expiresAt) {
        jdbcTemplate.update("CALL SP_CREATE_RESET_CODE(?, ?, ?)", empId, codeHash, expiresAt);
    }

    /** 取得目前有效（未使用）的驗證碼；無則回 null */
    public ResetCode findActiveCode(String empId) {
        List<ResetCode> result = jdbcTemplate.query("CALL SP_GET_ACTIVE_RESET_CODE(?)", MAPPER, empId);
        return result.isEmpty() ? null : result.get(0);
    }

    public void markUsed(long codeSeq) {
        jdbcTemplate.update("CALL SP_MARK_RESET_CODE_USED(?)", codeSeq);
    }

    public void incrementAttempts(long codeSeq) {
        jdbcTemplate.update("CALL SP_INCREMENT_RESET_ATTEMPTS(?)", codeSeq);
    }
}
