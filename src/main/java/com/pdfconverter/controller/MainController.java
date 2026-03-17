package com.pdfconverter.controller;

import com.pdfconverter.converter.BatchProcessor;
import com.pdfconverter.converter.ImageBatchProcessor;
import com.pdfconverter.model.ConversionResult;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;

@Slf4j
public class MainController {

    // Общие элементы
    @FXML private Button convertButton;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    @FXML private Label subtitleLabel;
    @FXML private Button modePdfToJpgBtn;
    @FXML private Button modeJpgToPdfBtn;
    @FXML private VBox pdfToJpgPane;
    @FXML private VBox jpgToPdfPane;

    // PDF → JPG элементы
    @FXML private Button addButton;
    @FXML private Button clearButton;
    @FXML private Button browseButton;
    @FXML private ListView<File> fileListView;
    @FXML private TextField outputDirField;
    @FXML private ComboBox<Integer> dpiComboBox;
    @FXML private StackPane dropZone;
    @FXML private Label dropLabel;
    @FXML private VBox emptyPlaceholder;

    // JPG → PDF элементы
    @FXML private Button addImageButton;
    @FXML private Button clearImageButton;
    @FXML private Button browsePdfButton;
    @FXML private ListView<File> imageListView;
    @FXML private TextField outputPdfNameField;
    @FXML private TextField outputPdfDirField;
    @FXML private StackPane imageDropZone;
    @FXML private VBox imageEmptyPlaceholder;

    private final ObservableList<File> pdfFiles = FXCollections.observableArrayList();
    private final ObservableList<File> imageFiles = FXCollections.observableArrayList();

    private enum Mode { PDF_TO_JPG, JPG_TO_PDF }
    private Mode currentMode = Mode.PDF_TO_JPG;

    @FXML
    public void initialize() {
        log.debug("Инициализация MainController");

        // ComboBox DPI
        dpiComboBox.setItems(FXCollections.observableArrayList(72, 100, 150, 200, 300));
        dpiComboBox.setValue(200);
        dpiComboBox.setCellFactory(lv -> new ListCell<Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) {
                    setText(String.valueOf(item));
                    setStyle("-fx-text-fill: white; -fx-background-color: #18181f;");
                    setOnMouseEntered(e -> setStyle("-fx-text-fill: white; -fx-background-color: #3a3a50;"));
                    setOnMouseExited(e -> setStyle("-fx-text-fill: white; -fx-background-color: #18181f;"));
                }
            }
        });
        dpiComboBox.setButtonCell(new ListCell<Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) {
                    setText(String.valueOf(item));
                    setStyle("-fx-text-fill: white;");
                }
            }
        });

        // Drag & Drop — PDF режим
        dropZone.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            }
            event.consume();
        });
        dropZone.setOnDragDropped(event -> {
            addPdfFiles(event.getDragboard().getFiles());
            event.setDropCompleted(true);
            event.consume();
        });
        dropZone.setOnMouseClicked(e -> onAddFiles());

        // Drag & Drop — JPG режим
        imageDropZone.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            }
            event.consume();
        });
        imageDropZone.setOnDragDropped(event -> {
            addImageFiles(event.getDragboard().getFiles());
            event.setDropCompleted(true);
            event.consume();
        });
        imageDropZone.setOnMouseClicked(e -> onAddImages());

        // Hover эффекты
        setupHover(addButton, "#23232e", "#3a3a50",
                "rgba(255,255,255,0.75)", "12px", "7", "0 16");
        setupHover(clearButton, "transparent", "transparent",
                "rgba(255,255,255,0.3)", "12px", "7", "0 16");
        setupHover(addImageButton, "#23232e", "#3a3a50",
                "rgba(255,255,255,0.75)", "12px", "7", "0 16");
        setupHover(clearImageButton, "transparent", "transparent",
                "rgba(255,255,255,0.3)", "12px", "7", "0 16");

        convertButton.setOnMouseEntered(e -> convertButton.setStyle(
                "-fx-background-color: #a090ff; -fx-text-fill: #ffffff;" +
                        "-fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 10; -fx-cursor: hand;"));
        convertButton.setOnMouseExited(e -> convertButton.setStyle(
                "-fx-background-color: #7c6fff; -fx-text-fill: #ffffff;" +
                        "-fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 10; -fx-cursor: hand;"));

        setupDropZoneHover(dropZone);
        setupDropZoneHover(imageDropZone);

        setupBrowseHover(browseButton);
        setupBrowseHover(browsePdfButton);

        // ProgressBar стиль
        progressBar.skinProperty().addListener((obs, old, newSkin) -> {
            if (newSkin != null) {
                progressBar.lookup(".track").setStyle("-fx-background-color: #2a2540;");
                progressBar.lookup(".bar").setStyle("-fx-background-color: #7c6fff;");
            }
        });

        // CellFactory для списков
        fileListView.setCellFactory(lv -> new FileCell(pdfFiles, fileListView, emptyPlaceholder));
        imageListView.setCellFactory(lv -> new FileCell(imageFiles, imageListView, imageEmptyPlaceholder));
    }

    // ===== ПЕРЕКЛЮЧЕНИЕ РЕЖИМОВ =====

    @FXML
    private void onSwitchToPdfToJpg() {
        currentMode = Mode.PDF_TO_JPG;
        pdfToJpgPane.setVisible(true);
        pdfToJpgPane.setManaged(true);
        jpgToPdfPane.setVisible(false);
        jpgToPdfPane.setManaged(false);
        subtitleLabel.setText("Конвертация документов в изображения");
        modePdfToJpgBtn.setStyle(
                "-fx-background-color: #7c6fff; -fx-text-fill: white;" +
                        "-fx-font-size: 12px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 8 0 0 8; -fx-cursor: hand; -fx-padding: 0 20;");
        modeJpgToPdfBtn.setStyle(
                "-fx-background-color: #23232e; -fx-text-fill: rgba(255,255,255,0.5);" +
                        "-fx-font-size: 12px;" +
                        "-fx-background-radius: 0 8 8 0; -fx-cursor: hand; -fx-padding: 0 20;");
        log.debug("Переключено на режим PDF → JPG");
    }

    @FXML
    private void onSwitchToJpgToPdf() {
        currentMode = Mode.JPG_TO_PDF;
        pdfToJpgPane.setVisible(false);
        pdfToJpgPane.setManaged(false);
        jpgToPdfPane.setVisible(true);
        jpgToPdfPane.setManaged(true);
        subtitleLabel.setText("Сборка картинок в PDF документ");
        modeJpgToPdfBtn.setStyle(
                "-fx-background-color: #7c6fff; -fx-text-fill: white;" +
                        "-fx-font-size: 12px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 0 8 8 0; -fx-cursor: hand; -fx-padding: 0 20;");
        modePdfToJpgBtn.setStyle(
                "-fx-background-color: #23232e; -fx-text-fill: rgba(255,255,255,0.5);" +
                        "-fx-font-size: 12px;" +
                        "-fx-background-radius: 8 0 0 8; -fx-cursor: hand; -fx-padding: 0 20;");
        log.debug("Переключено на режим JPG → PDF");
    }

    // ===== PDF → JPG =====

    @FXML
    private void onAddFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Выберите PDF файлы");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF файлы", "*.pdf", "*.PDF"));
        List<File> selected = chooser.showOpenMultipleDialog(addButton.getScene().getWindow());
        if (selected != null) addPdfFiles(selected);
    }

    private void addPdfFiles(List<File> files) {
        int before = pdfFiles.size();
        for (File file : files) {
            if (file.getName().toLowerCase().endsWith(".pdf") && !pdfFiles.contains(file)) {
                pdfFiles.add(file);
            }
        }
        log.info("Добавлено {} PDF файлов", pdfFiles.size() - before);
        refreshPdfList();
        updateStatus();
    }

    @FXML
    private void onClear() {
        pdfFiles.clear();
        refreshPdfList();
        progressBar.setProgress(0);
        statusLabel.setText("");
    }

    @FXML
    private void onChooseOutputDir() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Выберите папку для JPG файлов");
        File current = new File(outputDirField.getText());
        if (current.exists()) chooser.setInitialDirectory(current);
        File selected = chooser.showDialog(outputDirField.getScene().getWindow());
        if (selected != null) outputDirField.setText(selected.getAbsolutePath());
    }

    private void refreshPdfList() {
        fileListView.setItems(FXCollections.observableArrayList(pdfFiles));
        emptyPlaceholder.setVisible(pdfFiles.isEmpty());
    }

    // ===== JPG → PDF =====

    @FXML
    private void onAddImages() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Выберите картинки");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Картинки", "*.jpg", "*.jpeg", "*.png", "*.JPG", "*.PNG"));
        List<File> selected = chooser.showOpenMultipleDialog(addImageButton.getScene().getWindow());
        if (selected != null) addImageFiles(selected);
    }

    private void addImageFiles(List<File> files) {
        int before = imageFiles.size();
        for (File file : files) {
            String name = file.getName().toLowerCase();
            if ((name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png"))
                    && !imageFiles.contains(file)) {
                imageFiles.add(file);
            }
        }
        log.info("Добавлено {} картинок", imageFiles.size() - before);
        refreshImageList();
        updateStatus();
    }

    @FXML
    private void onClearImages() {
        imageFiles.clear();
        refreshImageList();
        progressBar.setProgress(0);
        statusLabel.setText("");
    }

    @FXML
    private void onChoosePdfOutputDir() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Выберите папку для PDF");
        File current = new File(outputPdfDirField.getText());
        if (current.exists()) chooser.setInitialDirectory(current);
        File selected = chooser.showDialog(outputPdfDirField.getScene().getWindow());
        if (selected != null) outputPdfDirField.setText(selected.getAbsolutePath());
    }

    private void refreshImageList() {
        imageListView.setItems(FXCollections.observableArrayList(imageFiles));
        imageEmptyPlaceholder.setVisible(imageFiles.isEmpty());
    }

    // ===== КОНВЕРТАЦИЯ =====

    @FXML
    private void onConvert() {
        if (currentMode == Mode.PDF_TO_JPG) {
            convertPdfToJpg();
        } else {
            convertJpgToPdf();
        }
    }

    private void convertPdfToJpg() {
        if (pdfFiles.isEmpty()) {
            showAlert("Нет файлов", "Добавьте PDF файлы для конвертации.");
            return;
        }
        File outputDir = new File(outputDirField.getText());
        float dpi = dpiComboBox.getValue();

        BatchProcessor task = new BatchProcessor(List.copyOf(pdfFiles), outputDir, dpi);
        bindTask(task);

        task.setOnSucceeded(event -> {
            List<ConversionResult> results = task.getValue();
            setControlsDisabled(false);
            progressBar.progressProperty().unbind();
            statusLabel.textProperty().unbind();
            long success = results.stream().filter(ConversionResult::isSuccess).count();
            statusLabel.setText(String.format("✓ %d конвертировано", success));
            progressBar.setProgress(1.0);
        });

        runTask(task);
    }

    private void convertJpgToPdf() {
        if (imageFiles.isEmpty()) {
            showAlert("Нет файлов", "Добавьте картинки для сборки PDF.");
            return;
        }
        String pdfName = outputPdfNameField.getText();
        if (!pdfName.toLowerCase().endsWith(".pdf")) pdfName += ".pdf";

        File outputFile = new File(outputPdfDirField.getText(), pdfName);

        ImageBatchProcessor task = new ImageBatchProcessor(List.copyOf(imageFiles), outputFile);
        bindTask(task);

        task.setOnSucceeded(event -> {
            setControlsDisabled(false);
            progressBar.progressProperty().unbind();
            statusLabel.textProperty().unbind();
            ConversionResult result = task.getValue();
            if (result.isSuccess()) {
                statusLabel.setText("✓ PDF собран: " + outputFile.getName());
            } else {
                statusLabel.setText("✗ Ошибка: " + result.getErrorMessage());
            }
            progressBar.setProgress(1.0);
        });

        runTask(task);
    }

    private void bindTask(javafx.concurrent.Task<?> task) {
        progressBar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());
        setControlsDisabled(true);

        task.setOnFailed(event -> {
            setControlsDisabled(false);
            progressBar.progressProperty().unbind();
            statusLabel.textProperty().unbind();
            statusLabel.setText("Ошибка: " + task.getException().getMessage());
            log.error("Задача завершилась с ошибкой", task.getException());
        });
    }

    private void runTask(javafx.concurrent.Task<?> task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void updateStatus() {
        if (currentMode == Mode.PDF_TO_JPG) {
            statusLabel.setText(pdfFiles.isEmpty() ? "" : "Добавлено файлов: " + pdfFiles.size());
        } else {
            statusLabel.setText(imageFiles.isEmpty() ? "" : "Добавлено картинок: " + imageFiles.size());
        }
    }

    private void setControlsDisabled(boolean disabled) {
        addButton.setDisable(disabled);
        clearButton.setDisable(disabled);
        convertButton.setDisable(disabled);
        dpiComboBox.setDisable(disabled);
        addImageButton.setDisable(disabled);
        clearImageButton.setDisable(disabled);
        modePdfToJpgBtn.setDisable(disabled);
        modeJpgToPdfBtn.setDisable(disabled);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ===== HOVER HELPERS =====

    private void setupHover(Button btn, String bgDefault, String bgHover,
                            String textColor, String fontSize, String radius, String padding) {
        String def = String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: %spx;" +
                        "-fx-background-radius: %s; -fx-cursor: hand; -fx-padding: %s;",
                bgDefault, textColor, fontSize, radius, padding);
        String hover = String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: %spx;" +
                        "-fx-background-radius: %s; -fx-cursor: hand; -fx-padding: %s;",
                bgHover, textColor, fontSize, radius, padding);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(def));
    }

    private void setupDropZoneHover(StackPane zone) {
        String def = zone.getStyle();
        String hover = def.replace("#18181f", "#1e1e2b")
                .replace("rgba(255,255,255,0.12)", "rgba(255,255,255,0.25)");
        zone.setOnMouseEntered(e -> zone.setStyle(hover));
        zone.setOnMouseExited(e -> zone.setStyle(def));
    }

    private void setupBrowseHover(Button btn) {
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #3a3a50; -fx-text-fill: rgba(255,255,255,0.8);" +
                        "-fx-font-size: 13px; -fx-background-radius: 7; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: #23232e; -fx-text-fill: rgba(255,255,255,0.6);" +
                        "-fx-font-size: 13px; -fx-background-radius: 7; -fx-cursor: hand;"));
    }

    // ===== CELL FACTORY =====

    private class FileCell extends ListCell<File> {
        private final HBox container = new HBox();
        private final Label icon = new Label();
        private final Label name = new Label();
        private final Label deleteBtn = new Label("✕");
        private final Region spacer = new Region();

        private final ObservableList<File> sourceList;
        private final ListView<File> listView;
        private final VBox placeholder;

        FileCell(ObservableList<File> sourceList, ListView<File> listView, VBox placeholder) {
            this.sourceList = sourceList;
            this.listView = listView;
            this.placeholder = placeholder;

            container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            container.setSpacing(10);
            container.setStyle("-fx-padding: 4 8;");
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            name.setStyle("-fx-text-fill: rgba(255,255,255,0.75); -fx-font-size: 12px;");
            deleteBtn.setStyle(
                    "-fx-text-fill: rgba(255,255,255,0.0); -fx-font-size: 12px;" +
                            "-fx-cursor: hand; -fx-padding: 2 6; -fx-background-radius: 4;");

            container.getChildren().addAll(icon, name, spacer, deleteBtn);

            container.setOnMouseEntered(e -> {
                deleteBtn.setStyle(deleteBtn.getStyle()
                        .replace("rgba(255,255,255,0.0)", "rgba(255,80,80,0.85)"));
                container.setStyle("-fx-padding: 4 8;" +
                        "-fx-background-color: rgba(255,255,255,0.04);" +
                        "-fx-background-radius: 6;");
            });
            container.setOnMouseExited(e -> {
                deleteBtn.setStyle(deleteBtn.getStyle()
                        .replace("rgba(255,80,80,0.85)", "rgba(255,255,255,0.0)"));
                container.setStyle("-fx-padding: 4 8; -fx-background-color: transparent;");
            });

            deleteBtn.setOnMouseClicked(e -> {
                File file = getItem();
                if (file != null) {
                    sourceList.remove(file);
                    listView.setItems(FXCollections.observableArrayList(sourceList));
                    placeholder.setVisible(sourceList.isEmpty());
                    log.debug("Удалён файл: {}", file.getName());
                }
                e.consume();
            });
        }

        @Override
        protected void updateItem(File file, boolean empty) {
            super.updateItem(file, empty);
            if (empty || file == null) {
                setGraphic(null);
            } else {
                String fname = file.getName().toLowerCase();
                if (fname.endsWith(".pdf")) {
                    icon.setText("📄");
                } else {
                    icon.setText("🖼");
                }
                icon.setStyle("-fx-font-size: 14px;");
                name.setText(file.getName());
                setGraphic(container);
            }
            setStyle("-fx-background-color: transparent;");
        }
    }
}