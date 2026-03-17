package com.pdfconverter;
import javafx.concurrent.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class BatchProcessor extends Task<List<ConversionResult>> {

    private final List<File> pdfFiles;
    private final File outputDir;
    private final float dpi;

    @Override
    protected List<ConversionResult> call() {
        PdfConverter converter = new PdfConverter(dpi);


        List<ConversionResult> results = new ArrayList<>();
        int total = pdfFiles.size();

        log.info("Запуск пакетной конвертации: {} файлов, outputDir={}, DPI={}", total, outputDir, dpi);
        outputDir.mkdirs();

        for (int i = 0; i < total; i++) {
            if (isCancelled()) {
                log.warn("Конвертация отменена пользователем на файле {}/{}", i + 1, total);
                break;
            }

            File pdfFile = pdfFiles.get(i);
            updateMessage("Обработка: " + pdfFile.getName());

            ConversionResult result = converter.convert(pdfFile, outputDir);
            results.add(result);

            if (result.isSuccess()) {
                log.info("[{}/{}] ✓ {}", i + 1, total, pdfFile.getName());
            } else {
                log.warn("[{}/{}] ✗ {} — {}", i + 1, total, pdfFile.getName(), result.getErrorMessage());
            }

            updateProgress(i + 1, total);
        }

        long success = results.stream().filter(ConversionResult::isSuccess).count();
        log.info("Пакетная конвертация завершена: {}/{} успешно", success, total);
        updateMessage("Готово!");

        return results;
    }
}