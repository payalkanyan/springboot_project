package com.example.demo.reconciliation;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Service
public class ExcelReconciliationService {

    public byte[] reconcileExcelFiles(MultipartFile file1, MultipartFile file2) throws IOException {
        Map<String, BigDecimal> trades1 = parseExcelFile(file1);
        Map<String, BigDecimal> trades2 = parseExcelFile(file2);

        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Reconciliation Results");

        // Create header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Trade ID");
        headerRow.createCell(1).setCellValue("Amount File 1");
        headerRow.createCell(2).setCellValue("Amount File 2");
        headerRow.createCell(3).setCellValue("Difference");
        headerRow.createCell(4).setCellValue("Status");

        int rowNum = 1;

        // Process trades from file1
        for (Map.Entry<String, BigDecimal> entry : trades1.entrySet()) {
            String tradeId = entry.getKey();
            BigDecimal amount1 = entry.getValue();
            BigDecimal amount2 = trades2.getOrDefault(tradeId, BigDecimal.ZERO);
            BigDecimal difference = amount1.subtract(amount2);

            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(tradeId);
            row.createCell(1).setCellValue(amount1.doubleValue());
            row.createCell(2).setCellValue(amount2.doubleValue());
            row.createCell(3).setCellValue(difference.doubleValue());
            if (difference.compareTo(BigDecimal.ZERO) == 0) {
                row.createCell(4).setCellValue("Matched");
            } else {
                row.createCell(4).setCellValue("Mismatch");
            }
            trades2.remove(tradeId); // Remove processed trade from file2 map
        }

        // Add remaining trades from file2 (those not in file1)
        for (Map.Entry<String, BigDecimal> entry : trades2.entrySet()) {
            String tradeId = entry.getKey();
            BigDecimal amount2 = entry.getValue();

            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(tradeId);
            row.createCell(1).setCellValue(0.0); // Not in file1
            row.createCell(2).setCellValue(amount2.doubleValue());
            row.createCell(3).setCellValue(BigDecimal.ZERO.subtract(amount2).doubleValue());
            row.createCell(4).setCellValue("Missing in File 1");
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }

    private Map<String, BigDecimal> parseExcelFile(MultipartFile file) throws IOException {
        Map<String, BigDecimal> trades = new HashMap<>();
        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0); // Assuming data is in the first sheet

        Iterator<Row> rowIterator = sheet.iterator();
        if (rowIterator.hasNext()) {
            rowIterator.next(); // Skip header row
        }

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Cell tradeIdCell = row.getCell(0); // Assuming Trade ID is in the first column
            Cell amountCell = row.getCell(1); // Assuming Amount is in the second column

            if (tradeIdCell != null && amountCell != null) {
                String tradeId;
                if (tradeIdCell.getCellType() == CellType.STRING) {
                    tradeId = tradeIdCell.getStringCellValue();
                } else if (tradeIdCell.getCellType() == CellType.NUMERIC) {
                    // If it's a number, convert it to a string without decimal if it's an integer
                    double numericValue = tradeIdCell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        tradeId = String.format("%d", (long) numericValue);
                    } else {
                        tradeId = String.format("%s", numericValue);
                    }
                } else {
                    // Handle other cell types for Trade ID, or skip this row
                    continue;
                }

                BigDecimal amount = BigDecimal.ZERO;
                if (amountCell.getCellType() == CellType.NUMERIC) {
                    amount = BigDecimal.valueOf(amountCell.getNumericCellValue());
                } else if (amountCell.getCellType() == CellType.STRING) {
                    try {
                        amount = new BigDecimal(amountCell.getStringCellValue());
                    } catch (NumberFormatException e) {
                        // Log error or handle invalid number format
                    }
                }
                trades.put(tradeId, amount);
            }
        }
        workbook.close();
        return trades;
    }
}
