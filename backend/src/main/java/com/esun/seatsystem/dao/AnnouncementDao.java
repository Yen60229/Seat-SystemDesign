package com.esun.seatsystem.dao;

import com.esun.seatsystem.common.dto.Announcement;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 資料層：公告存取（透過 Stored Procedure）。
 */
@Repository
public class AnnouncementDao {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<Announcement> MAPPER = (rs, rowNum) -> new Announcement(
            rs.getLong("ANN_SEQ"),
            rs.getString("TITLE"),
            rs.getString("CONTENT"),
            rs.getString("CREATED_BY"),
            rs.getString("CREATED_BY_NAME"),
            rs.getTimestamp("CREATED_AT").toLocalDateTime(),
            rs.getTimestamp("UPDATED_AT").toLocalDateTime());

    public AnnouncementDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Announcement> findAll() {
        return jdbcTemplate.query("CALL SP_GET_ANNOUNCEMENTS()", MAPPER);
    }

    public long create(String title, String content, String createdBy) {
        Long seq = jdbcTemplate.queryForObject(
                "CALL SP_CREATE_ANNOUNCEMENT(?, ?, ?)", Long.class, title, content, createdBy);
        return seq == null ? 0L : seq;
    }

    public void update(long annSeq, String title, String content) {
        jdbcTemplate.update("CALL SP_UPDATE_ANNOUNCEMENT(?, ?, ?)", annSeq, title, content);
    }

    public void delete(long annSeq) {
        jdbcTemplate.update("CALL SP_DELETE_ANNOUNCEMENT(?)", annSeq);
    }
}
