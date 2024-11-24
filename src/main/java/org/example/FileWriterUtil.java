package org.example;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;

public class FileWriterUtil {

  public static void saveTranslation(String translation) throws IOException {
    String userHome = System.getProperty("user.home");
    String desktopPath = userHome + File.separator + "Desktop";

    File directory = new File(desktopPath + File.separator + "gpt-translate");

    if (!directory.exists()) {
      boolean created = directory.mkdirs();
      if (!created) {
        throw new IOException("Error al crear la carpeta gpt-translate.");
      }
    }

    String currentDate = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy"));

    String uuid = UUID.randomUUID().toString();

    String fileName = currentDate + "-" + uuid + ".txt";

    File file = new File(directory, fileName);

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
      writer.write(translation);
    }
  }
}
