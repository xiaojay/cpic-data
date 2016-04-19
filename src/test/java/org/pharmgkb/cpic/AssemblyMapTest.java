package org.pharmgkb.cpic;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author Ryan Whaley
 */
public class AssemblyMapTest {

  @Test
  public void testGet() throws IOException {
    AssemblyMap assemblyMap = new AssemblyMap();

    assertEquals(AssemblyMap.GRCH37, assemblyMap.get("NC_000010.10"));
    assertEquals(AssemblyMap.GRCH38, assemblyMap.get("NC_000010.11"));

    assertEquals(AssemblyMap.GRCH37, assemblyMap.get("NC_000004.11"));
    assertEquals(AssemblyMap.GRCH38, assemblyMap.get("NC_000004.12"));
  }
}
