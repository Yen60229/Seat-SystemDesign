package com.esun.seatsystem.service;

import com.esun.seatsystem.common.dto.Announcement;
import com.esun.seatsystem.common.exception.BusinessException;
import com.esun.seatsystem.dao.AnnouncementDao;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

/**
 * 業務層：公告。建立/編輯/刪除為 ADMIN 操作（授權於 SecurityConfig 把關）。
 */
@Service
public class AnnouncementService {

    private static final String BUSINESS_SQL_STATE = "45000";

    private final AnnouncementDao announcementDao;

    public AnnouncementService(AnnouncementDao announcementDao) {
        this.announcementDao = announcementDao;
    }

    public List<Announcement> list() {
        return announcementDao.findAll();
    }

    public long create(String title, String content, String createdBy) {
        return announcementDao.create(title, content, createdBy);
    }

    public void update(long annSeq, String title, String content) {
        try {
            announcementDao.update(annSeq, title, content);
        } catch (DataAccessException ex) {
            throw toBusinessException(ex);
        }
    }

    public void delete(long annSeq) {
        try {
            announcementDao.delete(annSeq);
        } catch (DataAccessException ex) {
            throw toBusinessException(ex);
        }
    }

    /** 將 SP SIGNAL（如公告不存在）轉成可讀業務錯誤 */
    private RuntimeException toBusinessException(DataAccessException ex) {
        if (ex.getCause() instanceof SQLException sqlEx && BUSINESS_SQL_STATE.equals(sqlEx.getSQLState())) {
            return new BusinessException(sqlEx.getMessage());
        }
        return ex;
    }
}
