package org.pharmgkb.cpic;

import org.junit.Test;
import org.pharmgkb.cpic.model.AlleleTranslation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

import static org.junit.Assert.*;

/**
 * Test to make sure TSV translation tables are in the expected format,
 * and generate the intermediate TSV to be used with the haplotype caller.
 *
 * @author Ryan Whaley
 */
public class AlleleTranslationFileValidator {
  private static final Logger sf_logger = LoggerFactory.getLogger(AlleleTranslationFileValidator.class);


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

    AlleleTranslation t = new AlleleTranslationParser(f).parse();
    assertNotNull(t);

    assertNotNull("translation needs a gene symbol", t.getGeneSymbol());

    assertNotNull("No variant defined", t.getVariants());

    assertNotNull("No named alleles defined", t.getNamedAlleles());
    assertTrue("No named alleles defined", t.getNamedAlleles().size()>0);

    assertEquals("Number of variants and number of allele positions don't match",
        t.getVariants().length,
        t.getNamedAlleles().get(0).getAlleles().length);
  };
}
