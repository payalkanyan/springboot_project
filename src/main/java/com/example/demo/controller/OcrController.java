package com.example.demo.controller;

import com.example.demo.ocr.PdfOcrService;
import com.example.demo.model.Trade;
import com.example.demo.reconciliation.ExcelReconciliationService;
import com.example.demo.xml.XmlService;
import com.example.demo.xml.XmlTrade;
import com.example.demo.xml.XmlTradeList;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
public class OcrController {

    private final PdfOcrService pdfOcrService;
    private final ExcelReconciliationService excelReconciliationService;
    private final XmlService xmlService;

    @Autowired
    public OcrController(PdfOcrService pdfOcrService, ExcelReconciliationService excelReconciliationService, XmlService xmlService) {
        this.pdfOcrService = pdfOcrService;
        this.excelReconciliationService = excelReconciliationService;
        this.xmlService = xmlService;
    }

    @GetMapping("/upload")
    public String showUploadForm() {
        return "uploadForm";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("pdfFile") MultipartFile pdfFile, Model model) {
        if (pdfFile.isEmpty()) {
            model.addAttribute("message", "Please select a PDF file to upload.");
            return "uploadForm";
        }

        try {
            Trade trade = pdfOcrService.processPdfForTrade(pdfFile);
            model.addAttribute("message", "OCR and Trade extraction successful!");
            model.addAttribute("trade", trade);
        } catch (IOException | TesseractException e) {
            model.addAttribute("message", "Error during OCR and trade extraction: " + e.getMessage());
            e.printStackTrace();
        }
        return "uploadForm";
    }

    @GetMapping("/reconcile")
    public String showReconciliationForm() {
        return "reconciliationForm"; // This will resolve to src/main/resources/templates/reconciliationForm.html
    }

    @PostMapping("/reconcile")
    public ResponseEntity<byte[]> handleExcelReconciliation(@RequestParam("file1") MultipartFile file1,
                                                              @RequestParam("file2") MultipartFile file2,
                                                              Model model) {
        if (file1.isEmpty() || file2.isEmpty()) {
            // In a real application, you'd redirect back with an error message
            return ResponseEntity.badRequest().body("Please upload both Excel files.".getBytes());
        }

        try {
            byte[] reconciledExcel = excelReconciliationService.reconcileExcelFiles(file1, file2);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            String filename = "reconciliation_results.xlsx";
            headers.setContentDispositionFormData(filename, filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(reconciledExcel, headers, org.springframework.http.HttpStatus.OK);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(("Error processing Excel files: " + e.getMessage()).getBytes());
        }
    }

    @GetMapping("/xml")
    public String showXmlForm() {
        return "xmlForm"; // This will resolve to src/main/resources/templates/xmlForm.html
    }

    @PostMapping("/xml/upload")
    public String handleXmlUpload(@RequestParam("xmlFile") MultipartFile xmlFile, Model model) {
        if (xmlFile.isEmpty()) {
            model.addAttribute("message", "Please select an XML file to upload.");
            return "xmlForm";
        }
        try {
            String xmlString = new String(xmlFile.getBytes());
            XmlTradeList tradeList = xmlService.unmarshal(xmlString);
            // For demonstration, let's just add the first trade to the model
            if (tradeList != null && !tradeList.getTrades().isEmpty()) {
                model.addAttribute("message", "XML uploaded and unmarshalled successfully!");
                model.addAttribute("xmlTrade", tradeList.getTrades().get(0));
            } else {
                model.addAttribute("message", "XML uploaded, but no trades found or unmarshalling failed.");
            }
        } catch (Exception e) {
            model.addAttribute("message", "Error processing XML file: " + e.getMessage());
            e.printStackTrace();
        }
        return "xmlForm";
    }

    @GetMapping("/xml/download")
    public ResponseEntity<byte[]> downloadSampleXml() {
        try {
            // Create some sample trade data
            XmlTrade trade1 = new XmlTrade("T001", "AAPL", new BigDecimal("150.00"), new BigDecimal("100"), LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 3), "BuyerA", "SellerX", "USD");
            XmlTrade trade2 = new XmlTrade("T002", "GOOG", new BigDecimal("2500.00"), new BigDecimal("10"), LocalDate.of(2023, 1, 2), LocalDate.of(2023, 1, 4), "BuyerB", "SellerY", "USD");
            XmlTradeList tradeList = new XmlTradeList(java.util.List.of(trade1, trade2));

            String xmlString = xmlService.marshal(tradeList);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            String filename = "sample_trades.xml";
            headers.setContentDispositionFormData(filename, filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(xmlString.getBytes(), headers, org.springframework.http.HttpStatus.OK);

        } catch (JAXBException e) {
            e.printStackTrace();
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(("Error generating sample XML: " + e.getMessage()).getBytes());
        }
    }
}
