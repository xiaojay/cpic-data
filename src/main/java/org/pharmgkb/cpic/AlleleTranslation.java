package org.pharmgkb.cpic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * @author Ryan Whaley
 */
public class AlleleTranslation {
  private static final String sf_separator = "\t";

  public AlleleTranslation(Path pathToFile) throws IOException {
    List<String> lines = Files.readAllLines(pathToFile);

    if (lines.size()<8) {

    }
  }
}
