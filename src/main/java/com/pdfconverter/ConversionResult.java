package com.pdfconverter;

import lombok.Getter;
import lombok.ToString;

import java.io.File;
import java.util.List;

@Getter
@ToString
public class ConversionResult {

    public enum Status { SUCCESS, ERROR }

    private final File sourceFile;
    private final Status status;
    private final List<File> outputFiles;
    private final String errorMessage;

    public ConversionResult(File sourceFile, List<File> outputFiles) {
        this.sourceFile = sourceFile;
        this.status = Status.SUCCESS;
        this.outputFiles = outputFiles;
        this.errorMessage = null;
    }

    public ConversionResult(File sourceFile, String errorMessage) {
        this.sourceFile = sourceFile;
        this.status = Status.ERROR;
        this.outputFiles = List.of();
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public String getSummary() {
        if (isSuccess()) {
            int pages = outputFiles.size();
            return String.format("✓  %s  →  %d %s",
                    sourceFile.getName(),
                    pages,
                    pages == 1 ? "страница" : "страниц");
        } else {
            return String.format("✗  %s  —  %s", sourceFile.getName(), errorMessage);
        }
    }
}