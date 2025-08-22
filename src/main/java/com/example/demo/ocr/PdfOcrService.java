package com.example.demo.ocr;

import com.example.demo.model.Trade;
import com.example.demo.repository.TradeRepository;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfOcrService {

    private final Tesseract tesseract;
    private final TradeRepository tradeRepository;

    @Autowired
    public PdfOcrService(TradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
        tesseract = new Tesseract();
        tesseract.setDatapath("/usr/share/tesseract-ocr/5/tessdata/");
        tesseract.setLanguage("eng");
    }

    public Trade processPdfForTrade(MultipartFile pdfFile) throws IOException, TesseractException {
        String extractedText = extractTextFromPdf(pdfFile);
        Trade trade = parseTradeData(extractedText);
        return tradeRepository.save(trade);
    }

    private String extractTextFromPdf(MultipartFile pdfFile) throws IOException, TesseractException {
        Path tempPdfPath = Files.createTempFile("temp_pdf_", ".pdf");
        pdfFile.transferTo(tempPdfPath);
        File tempPdf = tempPdfPath.toFile();

        StringBuilder extractedText = new StringBuilder();

        try (PDDocument document = PDDocument.load(tempPdf)) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(page, 300);
                Path tempImagePath = Files.createTempFile("temp_image_", ".png");
                ImageIO.write(bufferedImage, "png", tempImagePath.toFile());
                File tempImage = tempImagePath.toFile();

                try {
                    extractedText.append(tesseract.doOCR(tempImage));
                } finally {
                    Files.deleteIfExists(tempImagePath);
                }
            }
        } finally {
            Files.deleteIfExists(tempPdfPath);
        }

        return extractedText.toString();
    }

    private Trade parseTradeData(String text) {
        Trade trade = new Trade();

        // Example: Parsing Trade ID
        Pattern tradeIdPattern = Pattern.compile("Trade ID:\s*([A-Za-z0-9]+)");
        Matcher tradeIdMatcher = tradeIdPattern.matcher(text);
        if (tradeIdMatcher.find()) {
            trade.setTradeId(tradeIdMatcher.group(1));
        }

        // Example: Parsing Instrument
        Pattern instrumentPattern = Pattern.compile("Instrument:\s*([A-Za-z0-9\s]+)");
        Matcher instrumentMatcher = instrumentPattern.matcher(text);
        if (instrumentMatcher.find()) {
            trade.setInstrument(instrumentMatcher.group(1).trim());
        }

        // Example: Parsing Quantity
        Pattern quantityPattern = Pattern.compile("Quantity:\s*([0-9.]+)");
        Matcher quantityMatcher = quantityPattern.matcher(text);
        if (quantityMatcher.find()) {
            try {
                trade.setQuantity(new BigDecimal(quantityMatcher.group(1)));
            } catch (NumberFormatException e) {
                // Handle parsing error
            }
        }

        // Example: Parsing Price
        Pattern pricePattern = Pattern.compile("Price:\s*([0-9.]+)");
        Matcher priceMatcher = pricePattern.matcher(text);
        if (priceMatcher.find()) {
            try {
                trade.setPrice(new BigDecimal(priceMatcher.group(1)));
            } catch (NumberFormatException e) {
                // Handle parsing error
            }
        }

        // Example: Parsing Trade Date
        Pattern tradeDatePattern = Pattern.compile("Trade Date:\s*(\\d{4}-\\d{2}-\\d{2})");
        Matcher tradeDateMatcher = tradeDatePattern.matcher(text);
        if (tradeDateMatcher.find()) {
            try {
                trade.setTradeDate(LocalDate.parse(tradeDateMatcher.group(1)));
            } catch (DateTimeParseException e) {
                // Handle parsing error
            }
        }

        // Example: Parsing Settlement Date
        Pattern settlementDatePattern = Pattern.compile("Settlement Date:\s*(\\d{4}-\\d{2}-\\d{2})");
        Matcher settlementDateMatcher = settlementDatePattern.matcher(text);
        if (settlementDateMatcher.find()) {
            try {
                trade.setSettlementDate(LocalDate.parse(settlementDateMatcher.group(1)));
            } catch (DateTimeParseException e) {
                // Handle parsing error
            }
        }

        // Example: Parsing Buyer
        Pattern buyerPattern = Pattern.compile("Buyer:\s*([A-Za-z\s]+)");
        Matcher buyerMatcher = buyerPattern.matcher(text);
        if (buyerMatcher.find()) {
            trade.setBuyer(buyerMatcher.group(1).trim());
        }

        // Example: Parsing Seller
        Pattern sellerPattern = Pattern.compile("Seller:\s*([A-Za-z\s]+)");
        Matcher sellerMatcher = sellerPattern.matcher(text);
        if (sellerMatcher.find()) {
            trade.setSeller(sellerMatcher.group(1).trim());
        }

        // Example: Parsing Currency
        Pattern currencyPattern = Pattern.compile("Currency:\s*([A-Z]{3})");
        Matcher currencyMatcher = currencyPattern.matcher(text);
        if (currencyMatcher.find()) {
            trade.setCurrency(currencyMatcher.group(1));
        }

        return trade;
    }
}
