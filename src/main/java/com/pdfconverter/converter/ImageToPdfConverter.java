package com.pdfconverter.converter;

import com.pdfconverter.model.ConversionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ImageToPdfConverter {

    public ConversionResult convert(List<File> imageFiles, File outputFile) {
        log.info("Начало сборки PDF из {} картинок → {}", imageFiles.size(), outputFile.getName());

        try (PDDocument document = new PDDocument()) {

            for (int i = 0; i < imageFiles.size(); i++) {
                File imageFile = imageFiles.get(i);
                log.debug("Добавляю страницу {}/{}: {}", i + 1, imageFiles.size(), imageFile.getName());

                PDImageXObject image = PDImageXObject.createFromFile(imageFile.getAbsolutePath(), document);

                PDRectangle pageSize = new PDRectangle(image.getWidth(), image.getHeight());
                PDPage page = new PDPage(pageSize);
                document.addPage(page);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.drawImage(image, 0, 0, image.getWidth(), image.getHeight());
                }
            }

            outputFile.getParentFile().mkdirs();
            document.save(outputFile);

            log.info("PDF собран успешно: {} ({} страниц)", outputFile.getName(), imageFiles.size());
            return new ConversionResult(outputFile, List.of(outputFile));

        } catch (IOException e) {
            log.error("Ошибка при сборке PDF: {}", e.getMessage(), e);
            return new ConversionResult(outputFile, e.getMessage());
        }
    }
}