package com.pdfconverter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;

@Slf4j
public class MainController {

    @FXML private Button addButton;
    @FXML private Button clearButton;
    @FXML private Button convertButton;
    @FXML private ListView<String> fileListView;
    @FXML private TextField outputDirField;
    @FXML private ComboBox<Integer> dpiComboBox;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;

    private final ObservableList<File> pdfFiles = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        log.debug("Инициализация MainController");

        dpiComboBox.setItems(FXCollections.observableArrayList(72, 100, 150, 200, 300));
        dpiComboBox.setValue(200);

        fileListView.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            }
            event.consume();
        });

        fileListView.setOnDragDropped(event -> {
            List<File> dropped = event.getDragboard().getFiles();
            log.debug("Drag & Drop: {} файлов", dropped.size());
            addPdfFiles(dropped);
            event.setDropCompleted(true);
            event.consume();
        });
    }

    @FXML
    private void onAddFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Выберите PDF файлы");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF файлы", "*.pdf", "*.PDF")
        );

        List<File> selected = chooser.showOpenMultipleDialog(addButton.getScene().getWindow());
        if (selected != null) {
            log.debug("Пользователь выбрал {} файлов через диалог", selected.size());
            addPdfFiles(selected);
        }
    }

    private void addPdfFiles(List<File> files) {
        int beforeCount = pdfFiles.size();

        for (File file : files) {
            if (file.getName().toLowerCase().endsWith(".pdf") && !pdfFiles.contains(file)) {
                pdfFiles.add(file);
            } else {
                log.debug("Пропущен файл (дубликат или не PDF): {}", file.getName());
            }
        }

        int added = pdfFiles.size() - beforeCount;
        log.info("Добавлено {} файлов, всего в списке: {}", added, pdfFiles.size());

        refreshListView();
        updateStatus();
    }

    @FXML
    private void onClear() {
        log.info("Список файлов очищен");
        pdfFiles.clear();
        refreshListView();
        progressBar.setProgress(0);
        statusLabel.setText("Добавьте PDF файлы для начала");
    }

    @FXML
    private void onChooseOutputDir() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Выберите папку для JPG файлов");

        File current = new File(outputDirField.getText());
        if (current.exists()) {
            chooser.setInitialDirectory(current);
        }

        File selected = chooser.showDialog(outputDirField.getScene().getWindow());
        if (selected != null) {
            log.debug("Папка вывода изменена: {}", selected.getAbsolutePath());
            outputDirField.setText(selected.getAbsolutePath());
        }
    }

    @FXML
    private void onConvert() {
        if (pdfFiles.isEmpty()) {
            log.warn("Попытка запустить конвертацию без файлов");
            showAlert("Нет файлов", "Добавьте PDF файлы для конвертации.");
            return;
        }

        File outputDir = new File(outputDirField.getText());
        float dpi = dpiComboBox.getValue();

        log.info("Запуск конвертации: {} файлов, outputDir={}, DPI={}", pdfFiles.size(), outputDir, dpi);

        BatchProcessor task = new BatchProcessor(List.copyOf(pdfFiles), outputDir, dpi);

        progressBar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());

        setControlsDisabled(true);

        task.setOnSucceeded(event -> {
            List<ConversionResult> results = task.getValue();
            log.info("Конвертация завершена успешно");
            setControlsDisabled(false);
            progressBar.progressProperty().unbind();
            statusLabel.textProperty().unbind();
            showResults(results);
        });

        task.setOnFailed(event -> {
            log.error("Конвертация завершилась с ошибкой", task.getException());
            setControlsDisabled(false);
            progressBar.progressProperty().unbind();
            statusLabel.textProperty().unbind();
            statusLabel.setText("Ошибка: " + task.getException().getMessage());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void refreshListView() {
        ObservableList<String> displayItems = FXCollections.observableArrayList();
        for (File f : pdfFiles) {
            displayItems.add("📄  " + f.getName());
        }
        fileListView.setItems(displayItems);
    }

    private void updateStatus() {
        statusLabel.setText(String.format("Добавлено файлов: %d", pdfFiles.size()));
    }

    private void showResults(List<ConversionResult> results) {
        ObservableList<String> displayItems = FXCollections.observableArrayList();
        long success = results.stream().filter(ConversionResult::isSuccess).count();

        for (ConversionResult r : results) {
            displayItems.add(r.getSummary());
        }

        fileListView.setItems(displayItems);
        statusLabel.setText(String.format("Готово: %d/%d файлов конвертированы", success, results.size()));
        progressBar.setProgress(1.0);

        log.info("Результаты отображены в GUI: {}/{} успешно", success, results.size());
    }

    private void setControlsDisabled(boolean disabled) {
        addButton.setDisable(disabled);
        clearButton.setDisable(disabled);
        convertButton.setDisable(disabled);
        dpiComboBox.setDisable(disabled);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}