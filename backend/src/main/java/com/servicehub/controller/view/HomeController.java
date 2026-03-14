package com.servicehub.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
/**
 * Serves the application home and landing pages.
 */

@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }
}