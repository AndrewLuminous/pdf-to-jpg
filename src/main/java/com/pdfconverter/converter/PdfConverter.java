package com.pdfconverter.converter;
import com.pdfconverter.model.ConversionResult;
import org.apache.pdfbox.Loader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class PdfConverter {

    private final float dpi;

    public ConversionResult convert(File pdfFile, File outputDir) {
        log.info("Начало конвертации: {} (DPI={})", pdfFile.getName(), dpi);

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();
            List<File> outputFiles = new ArrayList<>();

            log.debug("Файл {} содержит {} страниц", pdfFile.getName(), pageCount);

            String baseName = getBaseName(pdfFile.getName());

            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                log.debug("Рендеринг страницы {}/{} файла {}", pageIndex + 1, pageCount, pdfFile.getName());

                BufferedImage image = renderer.renderImageWithDPI(pageIndex, dpi, ImageType.RGB);

                String jpgName = String.format("%s_page%d.jpg", baseName, pageIndex + 1);
                File outputFile = new File(outputDir, jpgName);

                ImageIO.write(image, "JPEG", outputFile);
                outputFiles.add(outputFile);

                log.debug("Сохранено: {} ({}x{}px)", jpgName, image.getWidth(), image.getHeight());
            }

            log.info("Конвертация завершена: {} → {} файлов", pdfFile.getName(), outputFiles.size());
            return new ConversionResult(pdfFile, outputFiles);

        } catch (IOException e) {
            log.error("Ошибка при конвертации {}: {}", pdfFile.getName(), e.getMessage(), e);
            return new ConversionResult(pdfFile, e.getMessage());
        }
    }

    /** Убирает расширение из имени файла: "report.pdf" → "report" */
    private String getBaseName(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    }
}