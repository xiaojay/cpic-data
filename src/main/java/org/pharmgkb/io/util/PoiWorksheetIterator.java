/*
 ----- BEGIN LICENSE BLOCK -----
 This Source Code Form is subject to the terms of the Mozilla Public License, v.2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ----- END LICENSE BLOCK -----
 */
package org.pharmgkb.io.util;

import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;


/**
 * This is an iterator for POI worksheets.  It determines whether there are more lines by looking
 * for a specific number of empty rows (the default is {@link #MAX_EMPTY_ROWS}.  If will still read
 * intermediary empty rows if the number of empty rows is less than the maximum number of allowed
 * empty rows.
 *
 *
 * @author Mark Woon
 */
public class PoiWorksheetIterator implements Iterator<List<String>> {
  public static final int MAX_EMPTY_ROWS = 3;
  private Sheet m_sheet;
  private int m_currentRow;
  private int m_maxEmptyRows = MAX_EMPTY_ROWS;
  private int m_maxColumns;


  /**
   * Standard constructor.
   *
   * @param sheet the worksheet to iterate through
   */
  public PoiWorksheetIterator(Sheet sheet) {

    this(sheet, 1);
  }

  /**
   * Standard constructor.
   *
   * @param sheet the worksheet to iterate through
   * @param startRow the row number to start iterating from, the first row being 1
   */
  public PoiWorksheetIterator(Sheet sheet, int startRow) {

    m_sheet = sheet;
    m_currentRow = startRow - 1;
    m_maxColumns = -1;
  }

  /**
   * Standard constructor.
   *
   * @param sheet the worksheet to iterate through
   * @param startRow the row number to start iterating from, the first row being 1
   * @param maxColumns the number of columns to read per row (from the first column), -1 for all of them
   */
  public PoiWorksheetIterator(Sheet sheet, int startRow, int maxColumns) {

    m_sheet = sheet;
    m_currentRow = startRow - 1;
    if (m_currentRow < 0) {
      throw new IllegalArgumentException("Start row cannot be less than 0");
    }
    m_maxColumns = maxColumns - 1;
    if (m_maxColumns < 0) {
      throw new IllegalArgumentException("Max columns cannot be less than 0");
    }
  }


  /**
   * Gets the maximum number of empty rows before considering that there are no more rows.  The
   * default is {@link #MAX_EMPTY_ROWS}.
   *
   * @return the maximum number of empty rows before considering that there are no more rows
   */
  public int getMaxEmptyRows() {

    return m_maxEmptyRows;
  }

  /**
   * Sets the maximum number of empty rows before considering that there are no more rows.
   *
   * @param maxEmptyRows the maximum number of empty rows before considering that there are no more
   * rows.
   */
  public void setMaxEmptyRows(int maxEmptyRows) {

    m_maxEmptyRows = maxEmptyRows;
  }


  /**
   * Returns <tt>true</tt> if the iteration has more elements. (In other words, returns
   * <tt>true</tt> if <tt>next</tt> would return an element rather than throwing an exception.)
   *
   * @return <tt>true</tt> if the iterator has more elements.
   */
  public boolean hasNext() {

    int currentRow = m_currentRow;
    for (int emptyRowCount = 0; emptyRowCount < m_maxEmptyRows; emptyRowCount++) {
      // advance row count and grab it from sheet
      Row row = m_sheet.getRow(currentRow);
      currentRow += 1;
      if (row != null) {
        return true;
      }
    }
    return false;
  }


  /**
   * Returns the next element in the iteration.  Calling this method repeatedly until the {@link
   * #hasNext()} method returns false will return each element in the underlying collection exactly
   * once.
   *
   * @return the next element in the iteration.
   * @throws java.util.NoSuchElementException iteration has no more elements.
   */
  public List<String> next() {

    List<String> data;
    if (m_maxColumns == -1) {
      data = POIUtils.getStringCellValues(m_sheet, m_currentRow);
    } else {
      data = POIUtils.getStringCellValues(m_sheet, m_currentRow, 0, m_maxColumns);
    }
    m_currentRow += 1;
    return data;
  }


  /**
   * Gets the next line with data.
   *
   * @return the next line with data or null if there are no more lines with data
   */
  public @Nullable List<String> getNextLine() {

    if (!hasNext()) {
      return null;
    }
    List<String> data = next();
    while (isEmpty(data) && hasNext()) {
      data = next();
    }
    if (isEmpty(data)) {
      return null;
    }
    return data;
  }

  /**
   * Checks if list is empty or only contains null values.
   */
  private boolean isEmpty(List<String> data) {

    if (data == null || data.isEmpty()) {
      return true;
    }
    for (String d : data) {
      if (d != null) {
        return false;
      }
    }
    return true;
  }


  /**
   * Gets the current row number of the worksheet that this iterator is on.  The first line is 1 (0 means no lines read).
   *
   * @return the current row number of the worksheet that this iterator is on
   */
  public int getRowNumber() {

    return m_currentRow;
  }


  /**
   * Removes from the underlying collection the last element returned by the iterator (optional
   * operation).  This method can be called only once per call to <tt>next</tt>.  The behavior of an
   * iterator is unspecified if the underlying collection is modified while the iteration is in
   * progress in any way other than by calling this method.
   *
   * @throws UnsupportedOperationException if the <tt>remove</tt> operation is not supported by this
   * Iterator.
   * @throws IllegalStateException if the <tt>next</tt> method has not yet been called, or the
   * <tt>remove</tt> method has already been called after the last call to the <tt>next</tt>
   * method.
   */
  public void remove() {

    throw new UnsupportedOperationException("remove() is not supported");
  }
}
