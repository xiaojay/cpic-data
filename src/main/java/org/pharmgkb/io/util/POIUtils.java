/*
 ----- BEGIN LICENSE BLOCK -----
 This Source Code Form is subject to the terms of the Mozilla Public License, v.2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ----- END LICENSE BLOCK -----
 */
package org.pharmgkb.io.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFCell;


/**
 * This class provides convenience methods for dealing with POI objects.
 *
 * @author Winston Gor
 */
public class POIUtils {

  private POIUtils() {
  }


  /**
   * Converts numbers that include exponents into a regular number.
   *
   * @param number original number
   * @return reformatted number
   **/
  private static String formatNumber(double number) {

    String numString = Double.toString(number);
    int idx = numString.indexOf((int)'E');
    if (idx == -1) {
      // lop off trailing .0
      if (numString.endsWith(".0")) {
        numString = numString.substring(0, numString.length() - 2);
      }
      return numString;
    }

    int exponent = Integer.parseInt(numString.substring(idx + 1));
    int precision = idx - 1;
    if (exponent > 0 && exponent == precision) {
      precision++;
    }
    BigDecimal bd = new BigDecimal(number, new MathContext(precision));
    return bd.toPlainString();
  }

  /**
   * Gets the string value of a cell.
   *
   * @param cell the cell to get the string value of
   * @return the string value of the specified cell
   */
  private static @Nullable String getStringValue(Cell cell) {

    try {
      if (cell != null) {
        switch (cell.getCellType()) {
          case Cell.CELL_TYPE_NUMERIC:
            if (cell instanceof XSSFCell) {
              return ((XSSFCell) cell).getRawValue();
            }
            return formatNumber(cell.getNumericCellValue());
          case Cell.CELL_TYPE_BOOLEAN:
            return Boolean.toString(cell.getBooleanCellValue());
          default:
            return StringUtils.stripToEmpty(cell.getRichStringCellValue().getString());
        }
      }
    }
    catch (IllegalStateException ex) {
      throw new RuntimeException("Error in cell "+cell.getAddress(), ex);
    }
    return null;
  }


  /**
   * Returns value at specified row and cell numbers.  Indexes are 0-based.
   *
   * @param sheet Excel spreadsheet
   * @param rowNum row number
   * @param cellNum cell number
   * @return value of cell
   **/
  public static @Nullable String getStringCellValue(Sheet sheet, int rowNum, int cellNum) {

    Row row = sheet.getRow(rowNum);
    if (row != null) {
      return getStringValue(row.getCell(cellNum));
    }
    return null;
  }


  /**
   * Returns column values in specified row.  Indexes are 0-based.
   *
   * @param sheet Excel spreadsheet
   * @param rowNum row number
   * @return list of cell values in row
   **/
  public static List<String> getStringCellValues(Sheet sheet, int rowNum) {

    Row row = sheet.getRow(rowNum);
    if (row != null) {
      return getStringCellValues(sheet, rowNum, 0, (int)row.getLastCellNum()-1);
    }
    return Lists.newArrayList();
  }


  /**
   * Returns values within specified column range in specified row.
   *
   * @param sheet Excel spreadsheet
   * @param rowNum row number
   * @param columnStart index of beginning of column range
   * @param columnStop index of end of column range
   * @return list of cell values in row
   **/
  public static List<String> getStringCellValues(Sheet sheet, int rowNum, int columnStart, int columnStop) {

    List<String> values = Lists.newArrayList();
    Row row = sheet.getRow(rowNum);
    if (row != null) {
      int stop = columnStop + 1;
      for (int i = columnStart; i < stop; i++) {
        values.add(getStringValue(row.getCell(i)));
      }
    }
    return values;
  }


 /**
  * Returns cells within specified column range in specified row.
  *
  * @param sheet Excel spreadsheet
  * @param rowNum row number
  * @param column column number
  * @return list of cell values in row
  **/
  public static @Nullable Cell getCell(Sheet sheet, int rowNum, int column) {

    Cell cell = null;
    Row row = sheet.getRow(rowNum);
    if (row != null) {
      cell = row.getCell(column);
    }
    return cell;
  }


  public static CellStyle getTitleStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    style.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
    style.setFillPattern(CellStyle.SOLID_FOREGROUND);
    Font font = workbook.createFont();
    font.setColor(HSSFColor.BLACK.index);
    font.setBoldweight(Font.BOLDWEIGHT_BOLD);
    style.setFont(font);
    return style;
  }

  /**
   * Copies the given row to the given sheet, useful for copying rows between two differnt sheets.
   * This will preserve values, formulas, and formatting
   * @param row a Row to copy
   * @param sheet a Sheet to copy to
   * @return the new Row in <code>sheet</code>
   */
  public static Row copyRowTo(Row row, Sheet sheet) {
    return copyRowTo(row, sheet, row.getRowNum());
  }

  /**
   * Copies the given row to the given sheet, useful for copying rows between two different sheets.
   * This will preserve values, formulas, and formatting.  You can specify a new row number if you
   * want the row to appear in a different place in the new sheet.
   * @param row a Row to copy
   * @param sheet a Sheet to copy to
   * @param rowNum the desired row number in <code>sheet</code>
   * @return the new Row in <code>sheet</code>
   */
  public static Row copyRowTo(Row row, Sheet sheet, int rowNum) {
    Row newRow = sheet.createRow(rowNum);

    for (Cell cell : row) {
      Cell newCell = newRow.createCell(cell.getColumnIndex());
      newCell.setCellStyle(cell.getCellStyle());
      switch (cell.getCellType()) {
        case Cell.CELL_TYPE_NUMERIC:
          newCell.setCellValue(cell.getNumericCellValue());
          break;
        case Cell.CELL_TYPE_STRING:
          newCell.setCellValue(cell.getStringCellValue());
          break;
        case Cell.CELL_TYPE_BOOLEAN:
          newCell.setCellValue(cell.getBooleanCellValue());
          break;
        case Cell.CELL_TYPE_FORMULA:
          newCell.setCellFormula(cell.getCellFormula());
        default:
          break;
      }
    }
    return newRow;
  }

  /**
   * Inserts a cell in a row at the specified location.
   *
   * @param row the row to insert the cell in
   * @param column column to add (0 based -- first column is 0)
   */
  public static Cell insertCell(Row row, int column) {

    Preconditions.checkArgument(column >= 0, "newCol cannot be less than 0");
    for (int x = row.getLastCellNum(); x > column; x--) {
      Cell newCell = row.createCell(x);
      Cell oldCell = row.getCell(x - 1);
      if (oldCell != null) {
        newCell.setCellStyle(oldCell.getCellStyle());
        newCell.setCellType(oldCell.getCellType());
        switch (oldCell.getCellType()) {
          case Cell.CELL_TYPE_NUMERIC:
            newCell.setCellValue(oldCell.getNumericCellValue());
            break;
          case Cell.CELL_TYPE_STRING:
            newCell.setCellValue(oldCell.getStringCellValue());
            break;
          case Cell.CELL_TYPE_BOOLEAN:
            newCell.setCellValue(oldCell.getBooleanCellValue());
            break;
          case Cell.CELL_TYPE_FORMULA:
            newCell.setCellFormula(oldCell.getCellFormula());
          default:
            break;
        }
      }
    }
    return row.createCell(column);
  }


  /**
   * Converts an Excel file to TSV format.  TSV file is stored next to Excel file with a ".tsv" extension.
   */
  public static Path convertToTsv(@Nonnull Path xlsFile) throws IOException {

    Preconditions.checkNotNull(xlsFile);

    Path tsvFile;
    String xlsFilename = xlsFile.toString();
    if (xlsFilename.endsWith(".xls")) {
      tsvFile = Paths.get(xlsFilename.substring(0, xlsFilename.length() - 4) + ".tsv");
    } else if (xlsFilename.endsWith(".xlsx")) {
      tsvFile = Paths.get(xlsFilename.substring(0, xlsFilename.length() - 5) + ".tsv");
    } else {
      throw new IllegalArgumentException(("Not an Excel file (does not end with .xls or .xlsx)"));
    }
    return convertToTsv(xlsFile, tsvFile);
  }

  /**
   * Converts an Excel file to TSV format.
   */
  public static Path convertToTsv(@Nonnull Path xlsFile, @Nonnull Path tsvFile) throws IOException {

    Preconditions.checkNotNull(xlsFile);
    if (!Files.exists(xlsFile)) {
      throw new IllegalArgumentException("File '" + xlsFile.toString() + "' does not exist");
    }
    if (!Files.isRegularFile(xlsFile)) {
      throw new IllegalArgumentException("Not a file: '" + xlsFile.toString() + "'");
    }
    if (!xlsFile.toString().endsWith(".xls") && !xlsFile.toString().endsWith(".xlsx")) {
      throw new IllegalArgumentException(("Not an Excel file (does not end with .xls or .xlsx): " + xlsFile));
    }

    Preconditions.checkNotNull(tsvFile);
    if (!tsvFile.toString().endsWith(".tsv")) {
      throw new IllegalArgumentException(("Not a .tsv file (does not end with .tsv)"));
    }

    try (InputStream in = Files.newInputStream(xlsFile);
         PrintWriter writer = new PrintWriter(Files.newBufferedWriter(tsvFile))) {
      Workbook workbook = WorkbookFactory.create(in);
      PoiWorksheetIterator sheetIt = new PoiWorksheetIterator(workbook.getSheetAt(0));
      int minCols = -1;


      while (sheetIt.hasNext()) {
        List<String> line = sheetIt.getNextLine();
        if (line == null) {
          continue;
        }
        if (minCols < 0) {
          minCols = line.size();
        }
        int colCount = 0;
        for (; colCount < line.size(); colCount += 1) {
          if (colCount != 0) {
            writer.print("\t");
          }
          if (colCount < line.size()) {
            writer.print(line.get(colCount));
          }
        }
        for (; colCount < minCols; colCount += 1) {
          writer.print("\t");
        }
        writer.println();
      }
      return tsvFile;

    } catch (InvalidFormatException ex) {
      throw new IOException("Error reading Excel file", ex);
    }
  }
}
