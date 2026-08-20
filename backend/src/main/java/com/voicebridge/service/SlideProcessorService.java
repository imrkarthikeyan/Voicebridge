package com.voicebridge.service;

import com.voicebridge.exception.BusinessRuleViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlideProcessorService {

    private final PresentationStorageService storageService;

    public int processAndExtractSlides(InputStream inputStream, String fileExtension, String storagePath) {
        String ext = fileExtension.toLowerCase().replace(".", "");
        try {
            if ("pptx".equals(ext)) {
                return processPptx(inputStream, storagePath);
            } else if ("pdf".equals(ext)) {
                return processPdf(inputStream, storagePath);
            } else {
                throw new BusinessRuleViolationException("Unsupported presentation file extension: " + ext);
            }
        } catch (BusinessRuleViolationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse and extract slides for format {}", ext, e);
            throw new BusinessRuleViolationException("Could not process presentation slides: " + e.getMessage());
        }
    }

    private int processPptx(InputStream inputStream, String storagePath) throws Exception {
        try (XMLSlideShow ppt = new XMLSlideShow(inputStream)) {
            List<XSLFSlide> slides = ppt.getSlides();
            int totalSlides = slides.size();

            if (totalSlides == 0) {
                throw new BusinessRuleViolationException("PPTX presentation contains no slides");
            }

            Dimension pageSize = ppt.getPageSize();
            int width = Math.max(pageSize.width, 1024);
            int height = (int) Math.round((double) width * pageSize.height / pageSize.width);

            double scaleX = (double) width / pageSize.width;
            double scaleY = (double) height / pageSize.height;

            for (int i = 0; i < totalSlides; i++) {
                XSLFSlide slide = slides.get(i);
                BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = img.createGraphics();

                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                graphics.setPaint(Color.WHITE);
                graphics.fill(new Rectangle2D.Float(0, 0, width, height));

                graphics.scale(scaleX, scaleY);
                slide.draw(graphics);
                graphics.dispose();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "png", baos);
                storageService.saveSlideImage(baos.toByteArray(), storagePath, i + 1);
            }

            log.info("Successfully processed {} slides from PPTX", totalSlides);
            return totalSlides;
        }
    }

    private int processPdf(InputStream inputStream, String storagePath) throws Exception {
        byte[] pdfBytes = inputStream.readAllBytes();
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int totalSlides = document.getNumberOfPages();
            if (totalSlides == 0) {
                throw new BusinessRuleViolationException("PDF document contains no pages");
            }

            PDFRenderer pdfRenderer = new PDFRenderer(document);
            for (int page = 0; page < totalSlides; page++) {
                BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 150, ImageType.RGB);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bim, "png", baos);
                storageService.saveSlideImage(baos.toByteArray(), storagePath, page + 1);
            }

            log.info("Successfully processed {} slides from PDF", totalSlides);
            return totalSlides;
        }
    }
}
