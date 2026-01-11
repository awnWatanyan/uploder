package com.aeon.acss.fdu.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/customer-list")
public class CustomerListController {

    // รองรับทั้ง yyyy-MM-dd (จาก input type=date) และ dd/MM/yyyy (ถ้าคุณส่งมาแบบนี้)
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US);
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US);

    @GetMapping
    public String customerList(Model model) {
        model.addAttribute("pageTitle", "Customer Lists");
        model.addAttribute("content", "customer-list :: content");
        model.addAttribute("activeMenu", "MAIN_CUSTOMERS");

        // เข้า page ครั้งแรก
        model.addAttribute("hasResult", false);
        model.addAttribute("rows", java.util.Collections.emptyList());
        model.addAttribute("totalRows", 0);

        return "layout/layout";
    }

    // ===================== API SEARCH =====================
    @GetMapping(value = "/api/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> search(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String custName,
            @RequestParam(required = false) String client,
            @RequestParam(required = false) String agreementNo,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String zipcode,
            @RequestParam(required = false) String surveyType,
            @RequestParam(required = false) String jobStatus,
            @RequestParam(required = false) String createdFrom,
            @RequestParam(required = false) String createdTo,
            @RequestParam(required = false) String updateFrom,
            @RequestParam(required = false) String updateTo
    ) {

        // 1) โหลดข้อมูล (mock)
        List<CustomerRow> all = mockRows();

        // 2) parse date filters
        LocalDate createdFromDate = parseDate(createdFrom);
        LocalDate createdToDate = parseDate(createdTo);
        LocalDate updateFromDate = parseDate(updateFrom);
        LocalDate updateToDate = parseDate(updateTo);

        // 3) filter
        List<CustomerRow> filtered = all.stream()
                .filter(r -> containsIgnoreCase(r.customerId, customerId))
                .filter(r -> containsIgnoreCase(r.customerName, custName))
                .filter(r -> equalsIfPresent(r.clientCode, client))
                .filter(r -> containsIgnoreCase(r.agreementNo, agreementNo))
                .filter(r -> containsIgnoreCase(r.address, address))
                .filter(r -> containsIgnoreCase(r.zipcode, zipcode))
                .filter(r -> equalsIfPresent(r.surveyType, surveyType))
                .filter(r -> equalsIfPresent(r.status, jobStatus))
                .filter(r -> inDateRange(r.createdDate, createdFromDate, createdToDate))
                .filter(r -> inDateRange(r.updateDate, updateFromDate, updateToDate))
                .collect(Collectors.toList());

        // 4) convert to JSON-friendly rows
        List<Map<String, Object>> rows = filtered.stream()
                .map(CustomerListController::toMap)
                .collect(Collectors.toList());

        Map<String, Object> resp = new HashMap<>();
        resp.put("rows", rows);
        resp.put("totalRows", rows.size());
        return resp;
    }

    // ===================== helpers =====================

    private static boolean containsIgnoreCase(String source, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return true;
        if (source == null) return false;
        return source.toLowerCase(Locale.US).contains(keyword.trim().toLowerCase(Locale.US));
    }

    private static boolean equalsIfPresent(String source, String expected) {
        if (expected == null || expected.trim().isEmpty()) return true;
        return Objects.equals(source, expected.trim());
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        String v = s.trim();
        try {
            // input type=date จะส่ง yyyy-MM-dd
            return LocalDate.parse(v, ISO);
        } catch (DateTimeParseException ignore) {
            // เผื่อ dd/MM/yyyy
            try {
                return LocalDate.parse(v, DMY);
            } catch (DateTimeParseException ignore2) {
                return null; // format ไม่ถูกต้อง -> ไม่ filter ด้วยเงื่อนไขนี้
            }
        }
    }

    private static boolean inDateRange(LocalDate value, LocalDate from, LocalDate to) {
        if (from == null && to == null) return true;
        if (value == null) return false;
        if (from != null && value.isBefore(from)) return false;
        if (to != null && value.isAfter(to)) return false;
        return true;
    }

    private static Map<String, Object> toMap(CustomerRow r) {
        Map<String, Object> m = new HashMap<>();
        m.put("customerId", r.customerId);
        m.put("customerName", r.customerName);
        m.put("status", r.status); // ส่งเป็น code ไปให้หน้า map เป็นคำเอง
        m.put("appointmentDate", r.appointmentDate);
        m.put("surveyType", r.surveyType);
        m.put("clientName", r.clientName);
        m.put("osBalance", r.osBalance);
        m.put("agreementNo", r.agreementNo);
        m.put("createdDate", r.createdDate != null ? r.createdDate.toString() : "");
        m.put("updateDate", r.updateDate != null ? r.updateDate.toString() : "");
        m.put("collectorResult", r.collectorResult);
        m.put("collectorRemark", r.collectorRemark);
        // เผื่อกรอง client ใน backend (ส่ง code ด้วย)
        m.put("clientCode", r.clientCode);
        return m;
    }

    // ===================== mock data =====================

    private static List<CustomerRow> mockRows() {
        List<CustomerRow> list = new ArrayList<>();

        // clientCode: PRMF/AMNF/KBAF/BMW1
        list.add(new CustomerRow("CIF0000001", "John A", "1", "2026-01-10", "SUH", "บริษัท พรอมิส (ประเทศไทย) จำกัด", "PRMF",
                new BigDecimal("12000.50"), "AGR-0001",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 5), "OK", "Draft record", "Bangkok", "10110"));

        list.add(new CustomerRow("CIF0000002", "John B", "2", "2026-01-11", "SUC", "ธนาคารกสิกรไทย จำกัด มหาชน", "KBAF",
                new BigDecimal("900.00"), "AGR-0002",
                LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 8), "OK", "Sending...", "Nonthaburi", "11000"));

        list.add(new CustomerRow("CIF0000003", "John C", "4", "2026-01-12", "GMC", "บริษัท ไอร่า แอนด์ ไอฟุล จำกัด (มหาชน)", "AMNF",
                new BigDecimal("50000.00"), "AGR-0003",
                LocalDate.of(2026, 1, 3), LocalDate.of(2026, 1, 10), "WAIT", "Onprocess", "Pathumthani", "12000"));

        list.add(new CustomerRow("CIF0000004", "John D", "6", "2026-01-13", "GDO", "บริษัท บีเอ็มดับเบิลยู ลิสซิ่ง (ประเทศไทย) จำกัด", "BMW1",
                new BigDecimal("0.00"), "AGR-0004",
                LocalDate.of(2025, 12, 20), LocalDate.of(2026, 1, 9), "DONE", "Completed", "Bangkok", "10250"));

        list.add(new CustomerRow("CIF0000005", "John E", "98", "2026-01-14", "SUO", "บริษัท พรอมิส (ประเทศไทย) จำกัด", "PRMF",
                new BigDecimal("2500.25"), "AGR-0005",
                LocalDate.of(2025, 12, 22), LocalDate.of(2026, 1, 7), "CANCEL", "MCI Cancel", "Bangkok", "10500"));

        list.add(new CustomerRow("CIF0000006", "John F", "99", "2026-01-15", "GMH", "ธนาคารกสิกรไทย จำกัด มหาชน", "KBAF",
                new BigDecimal("8888.88"), "AGR-0006",
                LocalDate.of(2025, 12, 25), LocalDate.of(2026, 1, 6), "CANCEL", "Cancel", "Chonburi", "20150"));

        return list;
    }

    // DTO ภายใน controller (ถ้าคุณมี entity/service จริง ให้ลบส่วนนี้ได้)
    private static class CustomerRow {
        String customerId;
        String customerName;
        String status; // "1","2","4","5","6","98","99"
        String appointmentDate; // string for demo
        String surveyType; // SUH/SUC/...
        String clientName; // thai name
        String clientCode; // PRMF/AMNF/KBAF/BMW1
        BigDecimal osBalance;
        String agreementNo;
        LocalDate createdDate;
        LocalDate updateDate;
        String collectorResult;
        String collectorRemark;
        String address;
        String zipcode;

        CustomerRow(String customerId, String customerName, String status,
                    String appointmentDate, String surveyType,
                    String clientName, String clientCode,
                    BigDecimal osBalance, String agreementNo,
                    LocalDate createdDate, LocalDate updateDate,
                    String collectorResult, String collectorRemark,
                    String address, String zipcode) {
            this.customerId = customerId;
            this.customerName = customerName;
            this.status = status;
            this.appointmentDate = appointmentDate;
            this.surveyType = surveyType;
            this.clientName = clientName;
            this.clientCode = clientCode;
            this.osBalance = osBalance;
            this.agreementNo = agreementNo;
            this.createdDate = createdDate;
            this.updateDate = updateDate;
            this.collectorResult = collectorResult;
            this.collectorRemark = collectorRemark;
            this.address = address;
            this.zipcode = zipcode;
        }
    }
}
