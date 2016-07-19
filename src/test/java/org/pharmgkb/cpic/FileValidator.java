package org.pharmgkb.cpic;

import org.junit.Test;
import org.pharmgkb.pharmcat.definition.CuratedDefinitionParser;
import org.pharmgkb.pharmcat.definition.GeneratedDefinitionSerializer;
import org.pharmgkb.pharmcat.definition.model.DefinitionFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * Test to make files are in the expected format.
 *
 * @author Ryan Whaley
 * @author Mark Woon
 */
public class FileValidator {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


  private Path getDirectory(String dirName) {
    Path dir = Paths.get(dirName);
    assertNotNull(dir);

    assertTrue("Directory doesn't exist: " + dir.toAbsolutePath().toString(),
        dir.toFile().exists());
    assertTrue("Path isn't to a directory: " + dir.toAbsolutePath().toString(),
        dir.toFile().isDirectory());
    return dir;
  }


  @Test
  public void testTranslations() {

    try {
      Path dir = getDirectory("translations");
      Files.newDirectoryStream(dir)
          .forEach(f -> {
            sf_logger.info("Checking {}", f.getFileName());
            if (!f.toString().endsWith(".tsv")) {
              fail("Non .tsv file in translations directory: " + f);
            }
            CuratedDefinitionParser parser = new CuratedDefinitionParser(f);
            DefinitionFile definitionFile = parser.parse();

            assertNotNull("Missing gene symbol", definitionFile.getGeneSymbol());
            assertNotNull("No variant defined", definitionFile.getVariants());
            assertNotNull("No named alleles defined", definitionFile.getNamedAlleles());
            assertTrue("No named alleles defined", definitionFile.getNamedAlleles().size() > 0);
            assertEquals("Number of variants and number of allele positions don't match",
                definitionFile.getVariants().length, definitionFile.getNamedAlleles().get(0).getAlleles().length);
          });

    } catch (Exception e) {
      fail("Exception while running test: " + e.getMessage());
    }
  }

  @Test
  public void testGeneratedDefinitions() {

    try {
      Path dir = getDirectory("generatedDefinitions");
      GeneratedDefinitionSerializer serializer = new GeneratedDefinitionSerializer();
      Files.newDirectoryStream(dir)
          .forEach(f -> {
            sf_logger.info("Checking {}", f.getFileName());
            if (!f.toString().endsWith(".json")) {
              fail("Non .json file in translations directory: " + f);
            }
            try {
              DefinitionFile definitionFile = serializer.deserializeFromJson(f);

              assertNotNull("Missing gene symbol", definitionFile.getGeneSymbol());
              assertNotNull("No variant defined", definitionFile.getVariants());
              assertNotNull("No named alleles defined", definitionFile.getNamedAlleles());
              assertTrue("No named alleles defined", definitionFile.getNamedAlleles().size() > 0);
              assertEquals("Number of variants and number of allele positions don't match",
                  definitionFile.getVariants().length, definitionFile.getNamedAlleles().get(0).getAlleles().length);
            } catch (IOException ex) {
              fail(ex.getMessage());
            }
          });

    } catch (Exception e) {
      fail("Exception while running test: " + e.getMessage());
    }
  }
}
