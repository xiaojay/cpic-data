package org.pharmgkb.cpic;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test to make sure TSV translation tables are in the expected format.
 *
 * @author Ryan Whaley
 */
public class AlleleTranslationFileValidator {

  private static final int sf_minLineCount = 7;
  private static final String sf_separator = "\t";
  private static final Pattern sf_geneFieldPattern = Pattern.compile("^GENE:\\s*(\\w+)$");
  private static final Pattern sf_refSeqPattern = Pattern.compile("^.*(N\\w_\\d+\\.\\d+).*$");
  private static final SimpleDateFormat sf_dateFormat = new SimpleDateFormat("MM/dd/yy");

  private static final int LINE_GENE = 0;
  private static final int LINE_NAMING = 1;
  private static final int LINE_PROTEIN = 2;
  private static final int LINE_CHROMO = 3;
  private static final int LINE_GENESEQ = 4;
  private static final int LINE_POPS = 6;

  @Test
  public void testSheet() {
    Path translationsDirectory = Paths.get("translations");
    assertNotNull(translationsDirectory);

    assertTrue("Directory doesn't exist: " + translationsDirectory.toAbsolutePath().toString(), translationsDirectory.toFile().exists());
    assertTrue("Path isn't to a directory: " + translationsDirectory.toAbsolutePath().toString(), translationsDirectory.toFile().isDirectory());

    try {
      Files.newDirectoryStream(translationsDirectory, onlyTsvs).forEach(checkTranslationFile);
    } catch (IOException e) {
      fail("Exception while running test: " + e.getMessage());
    }
  }

  private static DirectoryStream.Filter<Path> onlyTsvs = p -> p.toString().endsWith(".tsv");

  private static Consumer<Path> checkTranslationFile = f -> {
    System.out.println("Checking "+f.getFileName());
    try {
      List<String> lines = Files.readAllLines(f);

      assertTrue("Not enough lines in the file, expecting at least " + sf_minLineCount, lines.size()>sf_minLineCount);

      testGeneLine(lines.get(LINE_GENE).split(sf_separator));
      testNamingLine(lines.get(LINE_NAMING).split(sf_separator));
      testProteinLine(lines.get(LINE_PROTEIN).split(sf_separator));
      testChromoLine(lines.get(LINE_CHROMO).split(sf_separator));
      testGeneSeqLine(lines.get(LINE_GENESEQ).split(sf_separator));
      testPopLine(lines.get(LINE_POPS).split(sf_separator));
    } catch (Exception e) {
      e.printStackTrace();
      fail("Problem checking file "+e.getMessage());
    }
  };

  private static void testGeneLine(String[] fields) throws ParseException {
    assertNotNull(fields);
    assertTrue(fields.length >= 2);

    Matcher m = sf_geneFieldPattern.matcher(fields[0]);
    assertTrue("Gene field not in expected format: "+fields[0], m.matches());

    System.out.println("\tgene: " + m.group(1));

    Date date = sf_dateFormat.parse(fields[1]);
    assertNotNull(date);
  }

  private static void testNamingLine(String[] fields) {
    assertTrue("Row "+ LINE_NAMING +", Column 1: expected to be blank", StringUtils.isBlank(fields[0]));
  }

  private static void testProteinLine(String[] fields) {
    String title = fields[1];
    assertTrue("No protein description specified", StringUtils.isNotBlank(title));

    Matcher m = sf_refSeqPattern.matcher(title);
    assertTrue("No RefSeq identifier for protein line "+LINE_PROTEIN, m.matches());

    System.out.println("\tprotein seq: "+m.group(1));
  }

  private static void testChromoLine(String[] fields) {
    String title = fields[1];
    assertTrue("No chromosomal position description specified", StringUtils.isNotBlank(title));

    Matcher m = sf_refSeqPattern.matcher(title);
    assertTrue("No RefSeq identifier for chromosomal line "+LINE_CHROMO, m.matches());

    System.out.println("\tchromosome seq: " + m.group(1));

    int lastVariantColumn = 2;
    for (int i=2; i<fields.length; i++) {
      if (StringUtils.isNotBlank(fields[i])) {
        lastVariantColumn = i;
      }
    }
    System.out.println("\t# variants specified: " + (lastVariantColumn-1));
  }

  private static void testGeneSeqLine(String[] fields) {
    String title = fields[1];
    assertTrue("No gene position description specified", StringUtils.isNotBlank(title));

    Matcher m = sf_refSeqPattern.matcher(title);
    assertTrue("No RefSeq identifier for gene sequence line "+LINE_GENESEQ, m.matches());

    System.out.println("\tgene seq: " + m.group(1));
  }

  private static void testPopLine(String[] fields) {
    assertTrue("No populations specified", fields.length>2);
    List<String> pops = Arrays.stream(Arrays.copyOfRange(fields, 2, fields.length))
        .filter(StringUtils::isNotBlank)
        .collect(Collectors.toList());
    assertNotNull(pops);
    assertTrue(pops.size()>0);
  }
}
