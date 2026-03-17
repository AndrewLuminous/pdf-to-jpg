package com.pdfconverter;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Path logPath = Path.of(System.getProperty("user.home"), "PdfToJpg-startup.log");
        try {
            Files.writeString(logPath, "Step 1: start() called\n");

            var url = getClass().getResource("/main.fxml");
            Files.writeString(logPath, "Step 2: fxml url = " + url + "\n",
                    java.nio.file.StandardOpenOption.APPEND);

            FXMLLoader loader = new FXMLLoader(url);
            Files.writeString(logPath, "Step 3: loader created\n",
                    java.nio.file.StandardOpenOption.APPEND);

            Scene scene = new Scene(loader.load());
            Files.writeString(logPath, "Step 4: scene created\n",
                    java.nio.file.StandardOpenOption.APPEND);

            stage.setTitle("PDF to JPG Converter");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
            Files.writeString(logPath, "Step 5: window shown\n",
                    java.nio.file.StandardOpenOption.APPEND);

        } catch (Exception e) {
            Files.writeString(logPath, "ERROR: " + e + "\n",
                    java.nio.file.StandardOpenOption.APPEND);
            throw e;
        }
    }

    @Override
    public void stop() {
        log.info("Приложение завершено");
    }

    public static void main(String[] args) {
        launch(args);
    }
}