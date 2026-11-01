package com.hcl.stocksmart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

@SpringBootApplication
public class StocksmartApplication {

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(StocksmartApplication.class, args);
    }

    /**
     * Loads key-value pairs from .env file into System properties before Spring Boot boots up.
     * Looks in the current working directory, stocksmart directory, and parent directory.
     */
    private static void loadDotenv() {
        File[] candidates = new File[] {
                new File(".env"),
                new File("stocksmart/.env"),
                new File("../.env")
        };

        for (File file : candidates) {
            if (file.exists() && file.isFile()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        int eqIdx = line.indexOf('=');
                        if (eqIdx > 0) {
                            String key = line.substring(0, eqIdx).trim();
                            String value = line.substring(eqIdx + 1).trim();
                            if ((value.startsWith("\"") && value.endsWith("\"")) ||
                                (value.startsWith("'") && value.endsWith("'"))) {
                                value = value.substring(1, value.length() - 1);
                            }
                            if (System.getProperty(key) == null && System.getenv(key) == null) {
                                System.setProperty(key, value);
                            }
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Notice: Could not load .env file from " + file.getAbsolutePath() + ": " + e.getMessage());
                }
                break;
            }
        }
    }

}
