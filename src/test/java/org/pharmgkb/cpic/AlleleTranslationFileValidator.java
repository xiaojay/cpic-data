package org.pharmgkb.cpic;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Test to make sure TSV translation tables are in the expected format,
 * and generate the intermediate TSV to be used with the haplotype caller.
 *
 * @author Ryan Whaley
 * @author Alex Frase
 */
public class AlleleTranslationFileValidator {
  private static final Logger sf_logger = LoggerFactory.getLogger(AlleleTranslationFileValidator.class);

  private static final int sf_minLineCount = 7;
  private static final String sf_separator = "\t";
  private static final Pattern sf_geneFieldPattern = Pattern.compile("^GENE:\\s*(\\w+)$");
  private static final Pattern sf_refSeqPattern = Pattern.compile("^.*(N\\w_(\\d+)\\.\\d+).*$");
  private static final Pattern sf_genomeBuildPattern = Pattern.compile("^.*(GRCh\\d+(?:\\.p\\d+)?).*$");
  private static final Pattern sf_populationTitle = Pattern.compile("^(.*) Allele Frequency$");
  private static final Pattern sf_basePattern = Pattern.compile("^(del[ATCG]*)|(ins[ATCG]*)|([ATCGMRWSYKVHDBN]*)$");
  private static final Pattern sf_hgvsPosition = Pattern.compile("^[cgp]\\.(\\d+).*$");
  private static final Pattern sf_orientationPattern = Pattern.compile("^.*(forward|reverse).*$");
  private static final String sf_hgvsSeparator = ";";
  private static final SimpleDateFormat sf_dateFormat = new SimpleDateFormat("MM/dd/yy");

  private static final int LINE_GENE = 0;
  private static final int LINE_NAMING = 1;
  private static final int LINE_PROTEIN = 2;
  private static final int LINE_CHROMO = 3;
  private static final int LINE_GENESEQ = 4;
  private static final int LINE_RSID = 5;
  private static final int LINE_POPS = 6;
  private static final int OUTPUT_FORMAT_VERSION = 1;

  private static Writer outputWriter;
  private static int numVariants;
  private static String geneName;
  private static String geneRefSeq;
  private static String geneOrientation;
  private static String versionDate;
  private static String versionTag;
  private static String genomeBuild;
  private static String chromosomeName;
  private static String chromosomeRefSeq;
  private static String proteinRefSeq;
  private static ArrayList<String> headersResourceNote;
  private static ArrayList<String> headersProteinNote;
  private static ArrayList<String> headersChrPosition;
  private static ArrayList<String> headersGenePosition;
  private static ArrayList<String> headersRSID;

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
    sf_logger.info("Checking {}", f.getFileName());
    try {
      List<String> lines = Files.readAllLines(f);
      assertTrue("Not enough lines in the file, expecting at least " + sf_minLineCount, lines.size()>sf_minLineCount);

      testChromoLine(lines.get(LINE_CHROMO).split(sf_separator)); // do this "out of order" since this is what sets numVariants
      testGeneLine(lines.get(LINE_GENE).split(sf_separator));
      testNamingLine(lines.get(LINE_NAMING).split(sf_separator));
      testProteinLine(lines.get(LINE_PROTEIN).split(sf_separator));
      testGeneSeqLine(lines.get(LINE_GENESEQ).split(sf_separator));
      testRSIDLine(lines.get(LINE_RSID).split(sf_separator));
      testPopLine(lines.get(LINE_POPS).split(sf_separator));

      outputWriter = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(Paths.get("output",geneName+".tsv")), "utf-8"));
      Process prc = Runtime.getRuntime().exec(new String[]{"git","log","-1","--format=\"%at\"","--",f.toAbsolutePath().toString()});
      BufferedReader prcReader = new BufferedReader(new InputStreamReader(prc.getInputStream()));
      String line;
      while ((line = prcReader.readLine()) != null) {
        versionTag = line.replaceAll("^[ \t\r\n\"]+|[ \t\r\n\"]$", "");
      }
      writeHeader();

      testVariantLines(lines);

      outputWriter.close();
    } catch (Exception e) {
      e.printStackTrace();
      fail("Problem checking file "+e.getMessage());
    }
  };

  private static ArrayList<String> getVariantFields(String[] fields) {
    ArrayList<String> list = new ArrayList<String>();
    for (int i = 2;  i < 2 + numVariants;  i++) {
      if (i < fields.length) {
        list.add(fields[i]);
      } else {
        list.add("");
      }
    }
    return list;
  }

  private static void writeHeader() throws IOException {
    outputWriter.write("FormatVersion\t" + OUTPUT_FORMAT_VERSION + "\n");
    outputWriter.write("GeneName\t" + geneName + "\n");
    outputWriter.write("GeneRefSeq\t" + geneRefSeq + "\n");
    outputWriter.write("GeneOrientation\t" + geneOrientation + "\n");
    outputWriter.write("ContentDate\t" + versionDate + "\n");
    outputWriter.write("ContentVersion\t" + versionTag + "\n");
    outputWriter.write("GenomeBuild\t" + genomeBuild + "\n");
    outputWriter.write("ChrName\t" + chromosomeName + "\n");
    outputWriter.write("ChrRefSeq\t" + chromosomeRefSeq + "\n");
    outputWriter.write("ProteinRefSeq\t" + proteinRefSeq + "\n");
    outputWriter.write("NumVariants\t" + numVariants + "\n");
    outputWriter.write("ResourceNote\t\t\t\t" + String.join("\t", headersResourceNote) + "\n");
    outputWriter.write("ProteinNote\t\t\t\t" + String.join("\t", headersProteinNote) + "\n");
    outputWriter.write("ChrPosition\t\t\t\t" + String.join("\t", headersChrPosition) + "\n");
    outputWriter.write("GenePosition\t\t\t\t" + String.join("\t", headersGenePosition) + "\n");
    outputWriter.write("rsID\t\t\t\t" + String.join("\t", headersRSID) + "\n");
    outputWriter.write("Header\tID\tName\tFunctionStatus\n");
  }

  private static void testGeneLine(String[] fields) throws ParseException {
    assertNotNull(fields);
    assertTrue(fields.length >= 2);

    Matcher m = sf_geneFieldPattern.matcher(fields[0]);
    assertTrue("Gene field not in expected format: "+fields[0], m.matches());

    geneName = m.group(1);
    sf_logger.info("\tgene: " + geneName);

    Date date = sf_dateFormat.parse(fields[1]);
    assertNotNull(date);
    versionDate = fields[1];
  }

  private static void testNamingLine(String[] fields) {
    assertTrue("Row "+ LINE_NAMING +", Column 1: expected to be blank", StringUtils.isBlank(fields[0]));

    headersResourceNote = getVariantFields(fields);
  }

  private static void testProteinLine(String[] fields) {
    String title = fields[1];
    assertTrue("No protein description specified", StringUtils.isNotBlank(title));

    Matcher m = sf_refSeqPattern.matcher(title);
    assertTrue("No RefSeq identifier for protein line "+LINE_PROTEIN, m.matches());

    proteinRefSeq = m.group(1);
    sf_logger.info("\tprotein seq: "+proteinRefSeq);

    headersProteinNote = getVariantFields(fields);
  }

  private static void testChromoLine(String[] fields) throws IOException {
    String title = fields[1];
    assertTrue("No chromosomal position description specified", StringUtils.isNotBlank(title));

    Matcher m = sf_refSeqPattern.matcher(title);
    assertTrue("No RefSeq identifier for chromosomal line "+LINE_CHROMO, m.matches());

    chromosomeRefSeq = m.group(1);
    sf_logger.info("\tchromosome seq: " + chromosomeRefSeq);

    AssemblyMap assemblyMap = new AssemblyMap();
    String build = assemblyMap.get(chromosomeRefSeq);
    assertNotNull("Unrecognized chromosome identifier " + chromosomeRefSeq, build);
    assertEquals("Chromosome identifier not on GRCh38: " + chromosomeRefSeq, "b38", build);

    int chrNum = Integer.parseInt(m.group(2), 10); // a leading 0 sometimes indicates octal, but we know this is always base 10
    assertTrue("Unknown or unsupported chromosome number "+chrNum+" on chromosomal line "+LINE_CHROMO, (chrNum >= 1 && chrNum <= 24));
    if (chrNum == 23) {
      chromosomeName = "chrX";
    } else if (chrNum == 24) {
      chromosomeName = "chrY";
    } else {
      chromosomeName = "chr" + chrNum;
    }
    sf_logger.info("\tchromosome name: " + chromosomeName);

    m = sf_genomeBuildPattern.matcher(title);
    assertTrue("No genome build identifier for chromosomal line "+LINE_CHROMO, m.matches());

    genomeBuild = m.group(1);
    sf_logger.info("\tgenome build: " + genomeBuild);

    numVariants = 0;
    for (int i=2; i<fields.length; i++) {
      if (StringUtils.isNotBlank(fields[i])) {
        numVariants = i - 1;
        verifyPositionText(fields[i]);
      }
    }
    sf_logger.info("\t# variants specified: " + numVariants);

    headersChrPosition = getVariantFields(fields);
  }

  private static void verifyPositionText(String text) {
    String[] positions = text.split(sf_hgvsSeparator);
    Arrays.stream(positions).forEach(p -> assertTrue("Position format doesn't match: "+p, sf_hgvsPosition.matcher(StringUtils.strip(p)).matches()));
  }

  private static void testGeneSeqLine(String[] fields) {
    String title = fields[1];
    assertTrue("No gene position description specified", StringUtils.isNotBlank(title));

    Matcher m = sf_refSeqPattern.matcher(title);
    assertTrue("No RefSeq identifier for gene sequence line "+LINE_GENESEQ, m.matches());

    geneRefSeq = m.group(1);
    sf_logger.info("\tgene seq: " + geneRefSeq);

    m = sf_orientationPattern.matcher(title);
    //TODO assertTrue("No gene orientation for gene sequence line "+LINE_GENESEQ, m.matches());

    geneOrientation = "";//TODO m.group(1);
    //TODO geneOrientation = geneOrientation.substring(0,1).toUpperCase() + geneOrientation.substring(1).toLowerCase();
    sf_logger.info("\tgene orientation: " + geneOrientation);

    for (int i=2; i<fields.length; i++) {
      verifyPositionText(fields[i]);
    }

    headersGenePosition = getVariantFields(fields);
  }

  private static void testRSIDLine(String[] fields) {
    String title = fields[1];
    assertTrue("No rsIDs specified", StringUtils.isNotBlank(title));

    headersRSID = getVariantFields(fields);
  }

  private static void testPopLine(String[] fields) {
    assertEquals("Expected the title 'Allele' in first column of row " + (LINE_POPS+1), "Allele", fields[0]);
    assertEquals("Expected the title 'Allele Functional Status' in second column of row " + (LINE_POPS+1), "Allele Functional Status", fields[1]);

    assertTrue("No populations specified", fields.length>2);
    List<String> pops = Arrays.stream(Arrays.copyOfRange(fields, 2, fields.length))
        .filter(StringUtils::isNotBlank)
        .collect(Collectors.toList());
    assertNotNull(pops);
    assertTrue(pops.size()>0);

    pops.stream().forEach(p -> {
      Matcher m = sf_populationTitle.matcher(p);
      assertTrue("Allele frequency column title should end in 'Allele Frequency'", m.matches());
    });
  }

  private static void testVariantLines(List<String> lines) throws IOException {
    HaplotypeIDMap geneHapMap = new HaplotypeIDMap();
    boolean isVariantLine = false;
    boolean isNoteLine = false;
    for (String line : lines) {
      if (isNoteLine) {
        outputWriter.write("Note\t" + line + "\n");
      }
      else if (line.toLowerCase().startsWith("notes:")) {
        isVariantLine = false;
        isNoteLine = true;
      }
      else if (isVariantLine) {
        String[] fields = line.split(sf_separator);
        if (fields.length > 2) {
          ArrayList<String> alleles = getVariantFields(fields);
          Set<String> badAlleles = alleles
              .stream()
              .filter(f -> !sf_basePattern.matcher(f).matches())
              .collect(Collectors.toSet());
          assertFalse(fields[0] + " has bad base pair values " + badAlleles.stream().collect(Collectors.joining(";")), badAlleles.size()>0);

          for (int i = 0;  i < alleles.size();  i++) {
            if (alleles.get(i).startsWith("ins")) {
              alleles.set(i, alleles.get(i).substring(3));
            } else if (alleles.get(i).startsWith("del")) {
              alleles.set(i, "-");
            }
          }
          String id = geneHapMap.get(geneName, fields[0]);
          if (id == null) {
            id = "";
          }
          outputWriter.write("Allele\t" + id + "\t"+fields[0]+"\t"+fields[1]+"\t"+String.join("\t", alleles)+"\n");
        }
      }
      else if (line.startsWith("Allele")) {
        isVariantLine = true;
      }
    }
  }
}
