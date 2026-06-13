package com.esun.seatsystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * SPA fallback：所有非 /api 且非靜態資源的路徑都回傳 index.html，
 * 讓 Vue Router 在瀏覽器端接手路由。
 */
@Controller
public class SpaController {

    @RequestMapping(value = {"/", "/login", "/forgot-password"})
    public String spa() {
        return "forward:/index.html";
    }
}
