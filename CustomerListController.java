package com.aeon.acss.fdu.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/customer-list")
public class CustomerListController {

    @GetMapping
    public String customerList(Model model) {
        model.addAttribute("pageTitle", "Customer Lists");
        model.addAttribute("content", "customer-list :: content");
        model.addAttribute("activeMenu", "MAIN_CUSTOMERS");

        model.addAttribute("hasResult", false);

        model.addAttribute("rows", java.util.Collections.emptyList());
        model.addAttribute("totalRows", 0);

        return "layout/layout";
    }
}
