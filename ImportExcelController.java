package com.aeon.acss.fdu.controller;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/import-excel")
public class ImportExcelController {

  private static final String SESSION_FILE_NAME = "IMPORT_FILE_NAME";
  private static final String SESSION_HEADERS   = "IMPORT_HEADERS";
  private static final String SESSION_ROWS      = "IMPORT_ROWS";

  @GetMapping
  public String page(Model model, HttpSession session) {
    model.addAttribute("content", "import-excel :: content");
    model.addAttribute("activeMenu", "MAIN_IMPORT");

    model.addAttribute("uploadedFileName", session.getAttribute(SESSION_FILE_NAME));
    model.addAttribute("headers", session.getAttribute(SESSION_HEADERS));
    model.addAttribute("rows", session.getAttribute(SESSION_ROWS));

    // rowCount (เผื่อใช้)
    Object rowsObj = session.getAttribute(SESSION_ROWS);
    int rowCount = (rowsObj instanceof List) ? ((List<?>) rowsObj).size() : 0;
    model.addAttribute("rowCount", rowCount);

    return "layout/layout";
  }

  @PostMapping("/clear")
  public String clear(HttpSession session) {
    session.removeAttribute(SESSION_FILE_NAME);
    session.removeAttribute(SESSION_HEADERS);
    session.removeAttribute(SESSION_ROWS);
    return "redirect:/import-excel";
  }

  @PostMapping("/save")
  public String save(HttpSession session, RedirectAttributes ra) {
    // TODO: เอา rows ไปบันทึก DB
    Object rowsObj = session.getAttribute(SESSION_ROWS);
    int rowCount = (rowsObj instanceof List) ? ((List<?>) rowsObj).size() : 0;
    ra.addFlashAttribute("saveMsg", "Saved " + rowCount + " rows");
    return "redirect:/import-excel";
  }

  @PostMapping("/upload")

  public String upload(@RequestParam("file") MultipartFile file,

                       HttpSession session,

                       RedirectAttributes ra) {

    if (file == null || file.isEmpty()) {

      ra.addFlashAttribute("uploadError", "Please choose an .xlsx file");

      return "redirect:/import-excel";

    }

    try (InputStream is = file.getInputStream(); Workbook wb = new XSSFWorkbook(is)) {

      Sheet sheet = wb.getSheetAt(0);

      List<String> headers = new ArrayList<>();

      List<List<String>> rows = new ArrayList<>();

      int firstRow = sheet.getFirstRowNum();

      // ====== Header 2 rows ======

      Row headerRow1 = sheet.getRow(firstRow);       // กลุ่ม (Customer/Address) หรือหัวหลัก

      Row headerRow2 = sheet.getRow(firstRow + 1);   // หัวคอลัมน์จริง (cif, customer_name, ...)

      if (headerRow1 != null || headerRow2 != null) {

        int lastCell1 = (headerRow1 != null) ? headerRow1.getLastCellNum() : 0;

        int lastCell2 = (headerRow2 != null) ? headerRow2.getLastCellNum() : 0;

        int lastCell = Math.max(lastCell1, lastCell2);

        for (int c = 0; c < lastCell; c++) {

          // รองรับ merged cells: ถ้าช่องว่าง จะไปดึงค่าจาก merged region

          String top = getMergedCellString(sheet, firstRow, c);

          String sub = getMergedCellString(sheet, firstRow + 1, c);

          top = top == null ? "" : top.trim();

          sub = sub == null ? "" : sub.trim();

          // รวมชื่อ header

          String finalHeader;

          if (!top.isBlank() && !sub.isBlank()) finalHeader = top + " - " + sub;

          else if (!sub.isBlank()) finalHeader = sub;

          else finalHeader = top; // เผื่อกรณีมีแค่หัวบน

          headers.add(finalHeader);

        }

      }

      // ====== Data start after 2 header rows ======

      int firstDataRow = firstRow + 2;

      int lastRow = sheet.getLastRowNum();

      for (int r = firstDataRow; r <= lastRow; r++) {

        Row row = sheet.getRow(r);

        if (row == null) continue;

        List<String> line = new ArrayList<>();

        for (int c = 0; c < headers.size(); c++) {

          line.add(getCellString(row.getCell(c)));

        }

        // ข้ามแถวว่างทั้งหมด

        if (line.stream().allMatch(s -> s == null || s.isBlank())) continue;

        rows.add(line);

      }

      session.setAttribute(SESSION_FILE_NAME, file.getOriginalFilename());

      session.setAttribute(SESSION_HEADERS, headers);

      session.setAttribute(SESSION_ROWS, rows);

    } catch (Exception e) {

      ra.addFlashAttribute("uploadError", "Upload failed: " + e.getMessage());

    }

    return "redirect:/import-excel";

  }

  /** อ่าน cell แบบรองรับ merged cells (ถ้า cell ว่างแต่เป็น merged จะดึงค่าจากช่องต้นทาง) */

	private String getMergedCellString(Sheet sheet, int rowIdx, int colIdx) {

		Row row = sheet.getRow(rowIdx);

		Cell cell = (row != null) ? row.getCell(colIdx) : null;

		String v = getCellString(cell);

		if (v != null && !v.isBlank())
			return v;

		// ถ้า blank ให้ลองหา merged region ที่ครอบอยู่

		for (int i = 0; i < sheet.getNumMergedRegions(); i++) {

			CellRangeAddress region = sheet.getMergedRegion(i);

			if (region.isInRange(rowIdx, colIdx)) {

				Row r = sheet.getRow(region.getFirstRow());

				if (r == null)
					return "";

				Cell firstCell = r.getCell(region.getFirstColumn());

				return getCellString(firstCell);

			}

		}

		return "";

	}

	private String getCellString(Cell cell) {

		if (cell == null)
			return "";

		return switch (cell.getCellType()) {

		case STRING -> cell.getStringCellValue();

		case NUMERIC -> DateUtil.isCellDateFormatted(cell)

				? String.valueOf(cell.getLocalDateTimeCellValue())

				: String.valueOf(cell.getNumericCellValue());

		case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());

		case FORMULA -> cell.getCellFormula();

		default -> "";

		};

	}
}
