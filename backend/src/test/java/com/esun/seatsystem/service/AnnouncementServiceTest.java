package com.esun.seatsystem.service;

import com.esun.seatsystem.common.exception.BusinessException;
import com.esun.seatsystem.dao.AnnouncementDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AnnouncementService 測試：建立回傳序號、編輯/刪除不存在公告的錯誤轉換。
 */
class AnnouncementServiceTest {

    private AnnouncementDao dao;
    private AnnouncementService service;

    @BeforeEach
    void setUp() {
        dao = mock(AnnouncementDao.class);
        service = new AnnouncementService(dao);
    }

    @Test
    @DisplayName("建立公告 → 回傳新序號")
    void create_returnsSeq() {
        when(dao.create("標題", "內容", "A0001")).thenReturn(42L);

        assertThat(service.create("標題", "內容", "A0001")).isEqualTo(42L);
    }

    @Test
    @DisplayName("編輯不存在的公告（SP SIGNAL 45000）→ BusinessException")
    void update_notFound() {
        doThrow(new DataIntegrityViolationException("x", new SQLException("公告不存在", "45000")))
                .when(dao).update(999L, "t", "c");

        assertThatThrownBy(() -> service.update(999L, "t", "c"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("公告不存在");
    }

    @Test
    @DisplayName("刪除不存在的公告 → BusinessException")
    void delete_notFound() {
        doThrow(new DataIntegrityViolationException("x", new SQLException("公告不存在", "45000")))
                .when(dao).delete(999L);

        assertThatThrownBy(() -> service.delete(999L)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("含 HTML/script 字串的內容照原樣交給 DAO（不在此層執行；呈現端轉義）")
    void create_passesRawContentSafely() {
        String xss = "<script>alert(1)</script>";
        when(dao.create("t", xss, "A0001")).thenReturn(1L);

        service.create("t", xss, "A0001");

        verify(dao).create("t", xss, "A0001");
    }
}
