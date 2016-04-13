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

    assertEquals("b38", assemblyMap.get("NC_000010.11"));
    assertEquals("b37", assemblyMap.get("NC_000004.11"));
  }
}
