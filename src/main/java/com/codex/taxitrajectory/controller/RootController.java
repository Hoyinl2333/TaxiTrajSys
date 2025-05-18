package com.codex.taxitrajectory.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootController {

    @GetMapping("/")
    public String index() {
        // 自动转发到静态资源路径
        return "forward:/taxi-trajectory.html";
    }
}

