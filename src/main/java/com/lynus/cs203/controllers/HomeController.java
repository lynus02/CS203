package com.lynus.cs203.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {

//    @GetMapping("/")
//    public String index() {
//        // returning the view name (index.html)
//        return "index";
//    }

    // Serve React app for main routes
    @GetMapping(value = {"/", "/customs", "/shipping", "/landed", "/database"})
    public String index() {
        return "forward:/index.html";
    }

    // Catch-all for React Router (handles SPA routing)
    @RequestMapping(value = "/{path:[^\\.]*}")
    public String handleReactRoutes() {
        return "forward:/index.html";
    }

}
