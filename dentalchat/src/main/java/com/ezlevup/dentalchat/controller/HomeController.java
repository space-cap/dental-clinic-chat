package com.ezlevup.dentalchat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "chat";
    }

    @GetMapping("/chat")
    public String chat() {
        return "chat";
    }
}