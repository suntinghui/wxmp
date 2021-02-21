package com.emgot.wxmp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/m")
public class JVerifyController {

    @RequestMapping("/verify")
    public String login() {
        return "verify";
    }

}
