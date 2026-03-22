package com.documind.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class FrontendController {

    @RequestMapping(value = {"/dashboard", "/chat", "/upload"})
    public String forward() {
        return "forward:/index.html";
    }
}