package com.aeon.acss.fdu.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/client")
public class ClientController {

    // ===== Simple DTO (temporary) =====
    public static class ClientRow {
        private String code;
        private String service;
        private String nameTh;
        private String nameEn;
        private String createdBy;
        private String createdAt;   // yyyy-MM-dd HH:mm:ss
        private String updatedBy;
        private String updatedAt;   // yyyy-MM-dd HH:mm:ss

        public ClientRow(
                String code,
                String service,
                String nameTh,
                String nameEn,
                String createdBy,
                String createdAt,
                String updatedBy,
                String updatedAt
        ) {
            this.code = code;
            this.service = service;
            this.nameTh = nameTh;
            this.nameEn = nameEn;
            this.createdBy = createdBy;
            this.createdAt = createdAt;
            this.updatedBy = updatedBy;
            this.updatedAt = updatedAt;
        }

        public String getCode() { return code; }
        public String getService() { return service; }
        public String getNameTh() { return nameTh; }
        public String getNameEn() { return nameEn; }
        public String getCreatedBy() { return createdBy; }
        public String getCreatedAt() { return createdAt; }
        public String getUpdatedBy() { return updatedBy; }
        public String getUpdatedAt() { return updatedAt; }
    }

    @GetMapping
    public String clients(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "service", required = false) String service,
            @RequestParam(name = "nameTh", required = false) String nameTh,
            @RequestParam(name = "nameEn", required = false) String nameEn,
            Model model
    ) {
        // 1) Temp data
        List<ClientRow> all = buildTempClients();

        // 2) Simple filter (GET search)
        String qCode = safeLower(code);
        String qService = safeLower(service);
        String qNameTh = safeLower(nameTh);
        String qNameEn = safeLower(nameEn);

        List<ClientRow> filtered = all.stream()
                .filter(r -> qCode.isEmpty() || safeLower(r.getCode()).contains(qCode))
                .filter(r -> qService.isEmpty() || safeLower(r.getService()).contains(qService))
                .filter(r -> qNameTh.isEmpty() || safeLower(r.getNameTh()).contains(qNameTh))
                .filter(r -> qNameEn.isEmpty() || safeLower(r.getNameEn()).contains(qNameEn))
                .collect(Collectors.toList());

        // 3) Model for layout
        model.addAttribute("pageTitle", "Clients");
        model.addAttribute("content", "client :: content");
        model.addAttribute("activeMenu", "MASTER_CLIENT");

        // 4) Data for page
        model.addAttribute("clients", filtered);
        model.addAttribute("totalRows", filtered.size());

        // Keep search values
        model.addAttribute("qCode", code == null ? "" : code);
        model.addAttribute("qService", service == null ? "" : service);
        model.addAttribute("qNameTh", nameTh == null ? "" : nameTh);
        model.addAttribute("qNameEn", nameEn == null ? "" : nameEn);

        return "layout/layout";
    }

    private List<ClientRow> buildTempClients() {
        List<ClientRow> list = new ArrayList<>();
        list.add(new ClientRow("C001", "PRMF", "บริษัท เอ บี ซี", "ABC Co.,Ltd.", "admin", "2026-01-02 10:11:12", "admin", "2026-01-02 10:11:12"));
        list.add(new ClientRow("C002", "LON",  "คุณ เจน สมิธ",  "Jane Smith",    "admin", "2026-01-01 09:05:30", "ops01", "2026-01-02 08:15:10"));
        list.add(new ClientRow("C003", "CAR",  "คุณ บ๊อบ",      "Bob Johnson",   "ops01", "2025-12-30 14:22:05", "ops01", "2025-12-31 16:40:00"));
        list.add(new ClientRow("C004", "PRMF", "แอคมี",         "ACME Co.,Ltd.", "admin", "2025-12-25 11:00:00", "admin", "2025-12-26 13:10:45"));
        list.add(new ClientRow("C005", "INS",  "เอ็กซ์วายแซด",  "XYZ Industries","ops02", "2025-12-20 08:30:00", "ops02", "2025-12-20 08:30:00"));
        return list;
    }

    private String safeLower(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }
}
