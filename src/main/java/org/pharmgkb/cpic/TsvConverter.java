package org.pharmgkb.cpic;

import org.pharmgkb.io.util.POIUtils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ryan Whaley
 */
public class TsvConverter {

  private static final Pattern sf_geneSymbolPattern = Pattern.compile("([A-Z0-9]+).*");
  private static DirectoryStream.Filter<Path> onlyExcel = p -> p.toString().endsWith(".xlsx") || p.toString().endsWith(".xls");

  public static void main(String[] args) {
    if (args.length != 1) {
      System.err.println("Didn't specify input file or directory");
      System.exit(1);
    }

    Path excelFile = Paths.get(args[0]);

    if (!excelFile.toFile().exists()) {
      System.err.println("The specified path does not exist: "+args[0]);
      System.exit(1);
    }

    if (excelFile.toFile().isDirectory()) {
      System.out.println("Converting all files in " + excelFile);
      try {
        Files.newDirectoryStream(excelFile, onlyExcel).forEach(TsvConverter::convertFile);
      } catch (IOException e) {
        System.err.println("Couldn't crawl input directory");
        e.printStackTrace();
        System.exit(1);
      }
    } else {
      convertFile(excelFile);
    }

    System.exit(0);
  }

  private static void convertFile(Path path) throws RuntimeException {
    System.out.println("Converting file " + path);
    try {
      Matcher m = sf_geneSymbolPattern.matcher(path.getFileName().toString());
      if (!m.matches()) {
        throw new RuntimeException("Error in excel file naming: " + path.getFileName().toString());
      }
      String gene = m.group(1);

      POIUtils.convertToTsv(path, path.getParent().resolve(gene+".allele.translation.tsv"));
    }
    catch (IOException ex) {
      throw new RuntimeException("Error converting file to TSV: " + path, ex);
    }
  }
}
