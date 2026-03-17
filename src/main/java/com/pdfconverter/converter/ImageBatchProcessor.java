package com.pdfconverter.converter;

import com.pdfconverter.model.ConversionResult;
import javafx.concurrent.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ImageBatchProcessor extends Task<ConversionResult> {

    private final List<File> imageFiles;
    private final File outputFile;

    @Override
    protected ConversionResult call() {
        log.info("Запуск сборки PDF: {} картинок → {}", imageFiles.size(), outputFile.getName());

        updateMessage("Подготовка...");
        updateProgress(0, imageFiles.size());

        ImageToPdfConverter converter = new ImageToPdfConverter();
        for (int i = 0; i < imageFiles.size(); i++) {
            if (isCancelled()) {
                log.warn("Сборка отменена пользователем");
                break;
            }
            updateMessage("Добавляю: " + imageFiles.get(i).getName());
            updateProgress(i, imageFiles.size());
        }

        updateMessage("Сохраняю PDF...");
        ConversionResult result = converter.convert(imageFiles, outputFile);

        if (result.isSuccess()) {
            updateProgress(imageFiles.size(), imageFiles.size());
            updateMessage("Готово! " + outputFile.getName());
            log.info("Сборка завершена успешно");
        } else {
            updateMessage("Ошибка: " + result.getErrorMessage());
            log.error("Сборка завершилась с ошибкой: {}", result.getErrorMessage());
        }
        return result;
    }
}