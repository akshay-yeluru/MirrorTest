/*
 * Copyright 2001 TSC Software Services Inc. All Rights Reserved.
 *
 * This software is the proprietary information of TSC Software Services Inc.
 * Use is subject to license terms.
 */
package com.tscsoftware.warehouse.cfg;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Date;

//import com.tscsoftware.util.IgnoreCaseComparator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.*;
import org.xml.sax.*;
import java.sql.Types;

/**
 * Serializable result set that can be passed back and forth between the client.
 * Provides a easy way to get the data over to the client, manipulate it just like
 * you were still attached to the database, and then send the data back to the server
 * for updating.
 *
 * @author Troy Makaro
 * @version $Revision$
 */
public class DisconnectedResultSet {
    static final long serialVersionUID = -3646607703181076231L;

    private ArrayList _columnNamesOrdered;

    private ArrayList _rows;
    private ArrayList _deleted;
    private boolean _onARow = false;
    private boolean _onInsert = false;
    private boolean _justDeleted = false;
    private int _intCount = 0;
    private Row _currentRow;
    private int _intRowPosition = -1;
    private int _intDeletedRowPosition = -1;
    private int _fieldCount;
    private int _originalFields;
    private boolean _wasNull;

    public static final int UNKNOWN = 10000;
    public static final int DRS = 10001;

//    public static void main(String args[]) {
//        DisconnectedResultSet master = new DisconnectedResultSet();
//        try {
//            master.addColumn(new Column("DUE_DATE", Types.TIMESTAMP));
//            master.moveToInsertRow();
//
////            Date d = DateUtil.toDate("2003-09-23 13:10:46.073","yyyy-MM-dd HH:mm:ss.SSS");
//            Date d = DateUtil.toDate("2003-09-23 13:10:46.730","yyyy-MM-dd HH:mm:ss.SSS");
//            Timestamp x = new Timestamp(d.getTime());
//
//
//            master.updateTimestamp("DUE_DATE",x);
//            master.insertRow();
//            String test = master.toXMLString();
//
//            DisconnectedResultSet drs = new DisconnectedResultSet();
//            drs.parseString(test);
//
//            String test2 = drs.toXMLString();
//            System.out.println(test);
//            System.out.println(test2);
//        } catch (DOMException e) {
//            e.printStackTrace(System.err);
//        } catch (Exception e) {
//            e.printStackTrace(System.err);
//        } // try
//    }

//    public static void main2(String args[]) {
//        DisconnectedResultSet master = new DisconnectedResultSet();
//        DisconnectedResultSet detail = new DisconnectedResultSet();
//        try {
//            master.addColumn(new Column("master1", Types.VARCHAR));
//            master.addColumn(new Column("master2", Types.DOUBLE));
//            master.addColumn(new Column("master3", DisconnectedResultSet.DRS));
//
//            detail.addColumn(new Column("detail1", Types.VARCHAR));
//            detail.addColumn(new Column("detail2", Types.DOUBLE));
//            detail.addColumn(new Column("detail3", Types.VARCHAR));
//
//            detail.moveToInsertRow();
//            detail.updateString(1, "value2");
//            detail.updateDouble(2, 4.5d);
//            detail.updateString(3, "value3a");
//            detail.insertRow();
//
//            master.moveToInsertRow();
//            master.updateString(1, " <  > \" "); // test special xml characters
//            master.updateDouble(2, 3.3d);
//            master.updateBranch(3, detail);
//            master.insertRow();
//
//            String xml = master.toXMLString();
//            System.out.println();
//            System.out.println(xml);
//            System.out.println();
//
//            DisconnectedResultSet drs = new DisconnectedResultSet();
//            drs.parseString(xml);
//
//            drs.first();
//            // print out special xml characters
//            System.out.println("Column 1 is: " + drs.getString(1));
//
//            String xml2 = drs.toXMLString();
//            System.out.println(xml2);
//
//        } catch (DOMException e) {
//            e.printStackTrace(System.err);
//        } catch (Exception e) {
//            e.printStackTrace(System.err);
//        } // try
//    } // main

    public DisconnectedResultSet() {
        _columnNamesOrdered = new ArrayList(_fieldCount);
        _rows = new ArrayList();
        _deleted = new ArrayList();
    } // constructor

    public void parseNode(Node drs) throws DOMException{
		continueParsing(drs);
    } // parseNode

    /**
     * Take an xml string and convert it into a DRS.
     *
     * @param xml The xml string that represents the DRS.
     *
     * @throws DOMException
     * @throws ParseException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public void parseString(String xml) throws DOMException,
        ParseException,
        ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        Document doc = db.parse(bais);

        Element drs = DOM.getChildElement(doc, "disconnectedResultset");
        continueParsing(drs);
    } // constructor

    private void continueParsing(Node drs) throws DOMException {
        Element general = DOM.getChildElement(drs, "general");
        Element columns = DOM.getChildElement(drs, "columns");
        Element rows = DOM.getChildElement(drs, "rows");
        Element deleted = DOM.getChildElement(drs, "deleted");

        // set up general information
        _onARow = StrU.convertToBoolean(general.getAttribute("onARow"), false);
        _onInsert = StrU.convertToBoolean(general.getAttribute("onInsert"), false);
        _justDeleted = StrU.convertToBoolean(general.getAttribute("justDeleted"), false);
        _intCount = Integer.parseInt(general.getAttribute("count"));
        _intRowPosition = Integer.parseInt(general.getAttribute("rowPosition"));
        _fieldCount = Integer.parseInt(general.getAttribute("fieldCount"));
        _originalFields = Integer.parseInt(general.getAttribute(
            "originalFields"));
        _wasNull = StrU.convertToBoolean(general.getAttribute("wasNull"), false);

        _columnNamesOrdered = new ArrayList(_fieldCount);
        _rows = new ArrayList();
        _deleted = new ArrayList();

        // set up the columns
        NodeList nl = columns.getChildNodes();
        for (int intT = 0; intT < nl.getLength(); intT++) {
            Node n = nl.item(intT);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element)n;
                String name = e.getAttribute("name");
                int type = Integer.parseInt(e.getAttribute("type"));
                //int size = Integer.parseInt(e.getAttribute("size"));
                //int isNullable = Integer.parseInt(e.getAttribute("nullable"));;
                _columnNamesOrdered.add(new Column(name, type));
            } // end if
        } // next

        // set up rows
        nl = rows.getChildNodes();
        for (int intT = 0; intT < nl.getLength(); intT++) {
            Node n = nl.item(intT);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                _rows.add(new Row(n));
            } // end if
        } // next

        // set up deleted rows
        nl = deleted.getChildNodes();
        for (int intT = 0; intT < nl.getLength(); intT++) {
            Node n = nl.item(intT);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                _deleted.add(new Row(n));
            } // end if
        } // next

    } // continueParsing

    /**
     * Takes a given result set and converts it into a disconnected one.
     * @param rs The result set you want to convert.
     * @throws SQLException Any error caught while trying to convert the result set.
     */
    public DisconnectedResultSet(ResultSet rs) throws SQLException {
        try {
            long sTime = System.currentTimeMillis();
            ResultSetMetaData rsmd = rs.getMetaData();
            _fieldCount = rsmd.getColumnCount();
            _originalFields = _fieldCount;
            //int rowCount = 1;
            // will not work on a forward only result set
            /*
                    if (rs.last()) {
                        rowCount = rs.getRow();
                    } // end if
             */
            _columnNamesOrdered = new ArrayList(_fieldCount);
            //_rows = new ArrayList(rowCount);
            _rows = new ArrayList();
            _deleted = new ArrayList();

            for (int intT = 1; intT <= rsmd.getColumnCount(); intT++) {
                String name = rsmd.getColumnName(intT);
                int type = rsmd.getColumnType(intT);
                //int size = 0;
                //int isNullable = rsmd.isNullable(intT);
                _columnNamesOrdered.add(new Column(name, type));
            } // next
            //rs.beforeFirst();
            while (rs.next()) {
                Row row = new Row();
                for (int intT = 1; intT <= rsmd.getColumnCount(); intT++) {
                    Object obj = rs.getObject(intT);
                    row.addToOriginal(obj, _fieldCount);
                } // next
                _rows.add(row);
            } // wend
            _intCount = _rows.size();
            long dur = System.currentTimeMillis() - sTime;
            Debugger.print(DisconnectedResultSet.class, "wrap resultSet time: " +
                           dur / (1000d), Debugger.BENCHMARK);
        } catch (SQLException e) {
			e.printStackTrace(System.err);
			Debugger.print(DisconnectedResultSet.class, 
						   StrU.convertStackTrace(e),
						   Debugger.FATAL);          	
            throw e;
        } finally {
            rs.close();
        } // try
    } // constructor

    /**
     * If you plan on using XML then you shouldn't use this method because it
     *  doesn't define the data types.
     *
     * @param columnName The name of the column to create.
     */
    public void addColumn(String columnName) throws SQLException {
        try {
            findColumn(columnName);
        } catch (Exception e) {
            // first time the column is referenced, it must be added
            _columnNamesOrdered.add(new Column(columnName,
                                               DisconnectedResultSet.UNKNOWN));
            findColumn(columnName);
            _fieldCount++;
        } // try
    } // addColumn

    /**
     * Adds another column to the DisconnectedResultSet
     * @param column A Column object that defines the columns attributes.
     */
    public void addColumn(Column column) throws SQLException {
        String columnName = column.getName();
        try {
            findColumn(columnName);
        } catch (Exception e) {
            // first time the column is referenced, it must be added
            _columnNamesOrdered.add(column);
            findColumn(columnName);
            _fieldCount++;
        } // try
    } // addColumn

    /**
     * Permanently remove a column from the DRS.  originalField is not updated so if the column being
     * deleted was an original column from the table the originalField count will be incorrect.
     * 
     * @param column Name of column to remove.
     * @throws SQLException Any SQL error encountered.
     */
    public void deleteColumn(String column) throws SQLException {
    	// make sure column existed
    	try{
    		findColumn(column);
    	}
    	catch(SQLException e){
    		// column does not exist
    		return;
    	}
    	
    	// remove column from all rows
    	int curRow;
    	DisconnectedResultSet.Row row;
    	for(curRow = 0; curRow < _rows.size(); curRow++){
    		row = (DisconnectedResultSet.Row)_rows.get(curRow);
    		row.deleteColumn(column);
    	}
    	for(curRow = 0; curRow < _deleted.size(); curRow++){
    		row = (DisconnectedResultSet.Row)_deleted.get(curRow);
    		row.deleteColumn(column);
    	}
    	
    	// remove column from DRS
    	_columnNamesOrdered.remove(findColumn(column) - 1);
    	_fieldCount--;
    }

    private void notAllowedFromInsertRow() throws SQLException {
        if (_onInsert) {
            throw new SQLException("Not allowed from insert row.");
        }
    } // notAllowedFromInsertRow

    private void columnIndexOutOfRange(int columnIndex) throws SQLException {
        if (columnIndex < 1 || columnIndex > _fieldCount) {
            throw new SQLException("column index out of range.");
        } // end if
    } // columnIndexOutOfRange

    private void notOnARow() throws SQLException {
        if (!_onARow) {
            throw new SQLException("Method requires a valid row.");
        } // end if
    } // columnIndexOutOfRange

    /*=======================================================================
     *  navigational methods
     */

    /**
     * Moves the current record position to a specific row.
     * @param row The row number starting at one.
     * @return True if the row is valid.
     * @throws SQLException Moved past end of rows.
     * @throws SQLException Row cannot be less than one.
     */
    public boolean absolute(int row) throws SQLException {
        _onInsert = false;
        _justDeleted = false;
        _onARow = false;
        if (row > _rows.size()) {
            throw new SQLException("Moved past end of rows");
        }
        if (row < 1) {
            throw new SQLException("Row cannot be less than one");
        }

        if (_currentRow != null) {
            _currentRow.clearTemp();
        }
        _currentRow = (Row)_rows.get(row - 1);

        _intRowPosition = row - 1;
        _intDeletedRowPosition = -1;
        _onARow = true;
        return _onARow;
    } // absolute

    /** Moves one record past the last record in the table.
     * @throws SQLException Any cache loading problems.
     */
    public void afterLast() throws SQLException {
		_intDeletedRowPosition = -1;
        _onInsert = false;
        _justDeleted = false;
        _onARow = false;
        if (_intCount == 0) {
            return;
        }
        _intRowPosition = _intCount;
        return;
    } // afterLast

    /** Moves the record point to one record before the first. You typically call this
     * method loop of next() methods.
     * @throws SQLException Any cache loading problems.
     */
    public void beforeFirst() throws SQLException {
        _onInsert = false;
        _justDeleted = false;
        _intRowPosition = -1;
		_intDeletedRowPosition = -1;
        _onARow = false;
        return;
    } // beforeFirst

    /** Moves to the first record in the table.
     * @throws SQLException Any cache loading problems.
     * @return false if there are not records.
     */
    public boolean first() throws SQLException {
		_intDeletedRowPosition = -1;
        _onInsert = false;
        _justDeleted = false;
        _onARow = false;
        if (_intCount == 0) {
            return false;
        }
        _intRowPosition = 0;
        if (_currentRow != null) {
            _currentRow.clearTemp();
        }
        _currentRow = (Row)_rows.get(_intRowPosition);
        _onARow = true;
        return true;
    } // first

    /** Moves to the last record in the table.
     * @throws SQLException Any cache loading problems.
     * @return false if there are no records.
     */
    public boolean last() throws SQLException {
		_intDeletedRowPosition = -1;
        _onInsert = false;
        _justDeleted = false;
        _onARow = false;
        if (_intCount == 0) {
            return false;
        }
        _intRowPosition = _intCount - 1;
        if (_currentRow != null) {
            _currentRow.clearTemp();
        }
        _currentRow = (Row)_rows.get(_intRowPosition);
        _onARow = true;
        return true;
    } // last

    /** Moves the the next record in the table.
     * @throws SQLException Any cache loading problems.
     * @return true if you have not gone past the end of the records.
     */
    public boolean next() throws SQLException {
        _onInsert = false;
        // @TODO modify for the new Deleted functionality function
        if (_justDeleted) {
            _justDeleted = false;
            if (_intRowPosition >= _intCount) {
                return false;
            }
            _currentRow = (Row)_rows.get(_intRowPosition);
            _onARow = true;
            return true;
        } // end if

        _onARow = false;
        if (_intCount == 0) {
            return false;
        }
        if (_intRowPosition == _intCount) {
            return false;
        } else {
            _intRowPosition++;
            if (_intRowPosition == _intCount) {
                return false;
            }

            if (_currentRow != null) {
                _currentRow.clearTemp();
            }
            
            if (_rows.size() != _intCount) {
            	Debugger.print(DisconnectedResultSet.class, "_rows.size()|_intCount|_intRowPosition" + _rows.size() + "|" + _intCount + "|"+ _intRowPosition, Debugger.LOTSA_INFO);
				Debugger.print(DisconnectedResultSet.class, _rows.get(0).toString(), Debugger.LOTSA_INFO);
				Debugger.print(DisconnectedResultSet.class, toXMLString(), Debugger.LOTSA_INFO);      	
            }
            _currentRow = (Row)_rows.get(_intRowPosition);
            _onARow = true;
            return true;
        } // end if
    } // next

    /** Moves back one record.
     * @throws SQLException Any cache loading problems.
     * @return true if you have not moved before the first record.
     */
    public boolean previous() throws SQLException {
		_intDeletedRowPosition = -1;
        _onInsert = false;
        _justDeleted = false;
        _onARow = false;
        if (_intCount == 0) {
            return false;
        }
        if (_intRowPosition == -1) {
            return false;
        } else {
            _intRowPosition--;
            if (_intRowPosition == -1) {
                return false;
            }
            if (_currentRow != null) {
                _currentRow.clearTemp();
            }
            _currentRow = (Row)_rows.get(_intRowPosition);
        } // end if
        _onARow = true;
        // moving back into normal rows so reset the deleted position

        return true;
    } // previous

    /** Checks if the record position is after the last record.
     * @throws SQLException Any cache loading problems.
     * @return true if the record position is after the last one.
     */
    public boolean isAfterLast() throws SQLException {
        if (_onInsert) {
            return false;
        }
        if (_intCount == 0) {
            return true;
        }
        if (_intRowPosition != _intCount) {
            return false;
        }
        return true;
    } // isAfterLast

    /** check to see if the record position is before the first record.
     * @return true if the record position is before the first record.
     * @throws SQLException Any cache loading problems.
     */
    public boolean isBeforeFirst() throws SQLException {
        if (_onInsert) {
            return false;
        }
        if (_intCount == 0) {
            return false;
        }
        if (_intRowPosition != -1) {
            return false;
        }
        return true;
    } // isBeforeFirst

    /** check to see if the record position is on the first record.
     * @return true if the record position is the first record.
     * @throws SQLException Any cache loading problems.
     */
    public boolean isFirst() throws SQLException {
        if (_onInsert) {
            return false;
        }
        if (_intRowPosition == 0) {
            return true;
        }
        return false;
    } // isFirst

    /** check to see if the record postion is on the last record.
     * @throws SQLException Any cache loading problems.
     * @return true if the record position is on the last record.
     */
    public boolean isLast() throws SQLException {
        if (_onInsert) {
            return false;
        }
        if (_intCount == 0) {
            return true;
        }
        if (_intRowPosition == _intCount - 1) {
            return true;
        }
        return false;
    } // isLast

    /** Finds the column number given the name of the column. Column numbers start
     * with 1.
     * @param columnName The case-sensitive name of the column to find.
     * @throws SQLException Column could not be found.
     * @return The index of the column starting at 1.
     */
    public int findColumn(String columnName) throws SQLException {
    	
    	Column column;
    	
    	for(int curField = 0; curField < _columnNamesOrdered.size(); curField++){
    		column = (Column)_columnNamesOrdered.get(curField);
    		if(column.getName().compareToIgnoreCase(columnName) == 0){
    			return curField + 1;
    		}
    	}
    	
    	throw new SQLException("Column name: " + columnName + " does not exist");
    } // findColumn

    /** An iteration of all the field names in the table.
     * @throws SQLException Any cache loading problems.
     * @return An Iterator 0bject of all the field names in the table.
     */
    public Iterator getFieldNames() throws SQLException {
        ArrayList names = new ArrayList(_columnNamesOrdered.size());
        Iterator i = _columnNamesOrdered.iterator();
        while (i.hasNext()) {
            Column c = (Column)i.next();
            names.add(c.getName());
        } // next

        return names.iterator();
    } // getFieldNames

    ArrayList getFields() {
        ArrayList names = new ArrayList(_columnNamesOrdered.size());
        Iterator i = _columnNamesOrdered.iterator();
        while (i.hasNext()) {
            Column c = (Column)i.next();
            names.add(c.getName());
        } // next

        return names;
    } // getFields

    int getFieldCount() {
        return _fieldCount;
    } // getFieldCount

    /**
     * Returns the number of rows in the DisconnectedResulSet
     *
     * @return number of rows
     */
    public int getRowCount() {
        return _intCount;
    }

    int getOriginalFieldCount() {
        return _originalFields;
    } // getFieldCount

    /** Gets the row position of the current record.
     * @throws SQLException Any cache loading problems.
     * @return The row position of the current record.
     */
    public int getRow() throws SQLException {
        if (!_onARow) {
            return 0;
        }
        return _intRowPosition + 1;
    }

    /**
     * Check if the last field read in was null.
     * @return true if the last field read was null.
     * @throws SQLException Any thrown SQL Exception
     */
    public boolean wasNull() throws SQLException {
        return _wasNull;
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>String</code> in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>null</code>
     * @exception SQLException if a database access error occurs
     */
    public String getString(int columnIndex) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        if (isNull(columnIndex)) {
            return null;
        }
        return _currentRow.get(columnIndex - 1).toString();
    }

    /**
     * Gets an int array of all the column indexes that have been modified.
     * JDBC columns start at one.
     * @throws SQLException Any caught sql exception.
     */
    public int[] getColumnIndexesModified() throws SQLException {
        ArrayList list = new ArrayList();
        for (int intT=1;intT<=_fieldCount;intT++) {
            if (isColumnModified(intT)) {
                list.add(new Integer(intT));
            } // next
        } // next
        int[] columns = new int[list.size()];
        for (int intT=0;intT<list.size();intT++) {
            columns[intT] = ((Integer)list.get(intT)).intValue();
        } // next
        return columns;
    }

    /**
     * Gets an String array of all the column names that have been modified.
     * JDBC columns start at one.
     * @throws SQLException Any caught sql exception.
     */
    public String[] getColumnNamesModified() throws SQLException {
        ArrayList list = new ArrayList();
        for (int intT=1;intT<=_fieldCount;intT++) {
            if (isColumnModified(intT)) {
                Column c = (Column)_columnNamesOrdered.get(intT-1);
                list.add(c.getName());
            } // next
        } // next
        String[] columns = new String[list.size()];
        for (int intT=0;intT<list.size();intT++) {
            columns[intT] = (String)list.get(intT);
        } // next
        return columns;
    }

    /**
     * Returns true if the column has been modified.
     * @param columnName The name of the column to check.
     * @throws SQLException Any caught sql exception.
     */
    public boolean isColumnModified(String columnName) throws SQLException {
        int columnIndex = findColumn(columnName);
        return isColumnModified(columnIndex);
    }

    /**
     * Returns true if the column has been modified.
     * @param columnIndex The column number to check starting at one.
     * @throws SQLException Any caught sql exception.
     */
    public boolean isColumnModified(int columnIndex) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        return _currentRow.isColumnModified(columnIndex -1);
    }

    private Number getNumber(int columnIndex) throws SQLException {
        Object obj = _currentRow.get(columnIndex - 1);
        if (!(obj instanceof Number)) {
            throw new SQLException("column does not contain a number");
        } // end if
        return (Number)obj;
    } // getNumber

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>boolean</code> in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>false</code>
     * @exception SQLException if a database access error occurs
     */
    public boolean getBoolean(int columnIndex) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        if (isNull(columnIndex)) {
            return false;
        }
        Object obj = _currentRow.get(columnIndex - 1);
        if (!(obj instanceof Boolean)) {
            throw new SQLException("column does not contain a boolean");
        } // end if
        return ((Boolean)obj).booleanValue();
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>byte</code> in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>0</code>
     * @exception SQLException if a database access error occurs
     */
    public byte getByte(int columnIndex) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        if (isNull(columnIndex)) {
            return 0;
        }
        Number n = getNumber(columnIndex);
        return n.byteValue();
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>short</code> in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>0</code>
     * @exception SQLException if a database access error occurs
     */
    public short getShort(int columnIndex) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        if (isNull(columnIndex)) {
            return 0;
        }
        _currentRow.get(columnIndex - 1);
        Number n = getNumber(columnIndex);
        return n.shortValue();
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * an <code>int</code> in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>0</code>
     * @exception SQLException if a database access error occurs
     */
    public int getInt(int columnIndex) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        if (isNull(columnIndex)) {
            return 0;
        }
        _currentRow.get(columnIndex - 1);
        Number n = getNumber(columnIndex);
        return n.intValue();
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>long</code> in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>0</code>
     * @exception SQLException if a database access error occurs
     */
    public long getLong(int columnIndex) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        if (isNull(columnIndex)) {
            return 0;
        }
        _currentRow.get(columnIndex - 1);
        Number n = getNumber(columnIndex);
        return n.longValue();
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>float</code> in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>0</code>
     * @exception SQLException if a database access error occurs
     */
    public float getFloat(int columnIndex) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        if (isNull(columnIndex)) {
            return 0;
        }
        _currentRow.get(columnIndex - 1);
        Number n = getNumber(columnIndex);
        return n.floatValue();
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>double</code> in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>0</code>
     * @exception SQLException if a database access error occurs
     */
    public double getDouble(int columnIndex) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        if (isNull(columnIndex)) {
            return 0;
        }
        _currentRow.get(columnIndex - 1);
        Number n = getNumber(columnIndex);
        return n.doubleValue();
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>java.sql.BigDecimal</code> in the Java programming language.
     *
     * If this method is called and the actual column type is one of the
     * following it will attempt to convert it to BigDecimal and return it:
     * Double, String, Long, Integer, BigInteger, Short and Float
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param scale the number of digits to the right of the decimal point
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>null</code>
     * @exception SQLException if a database access error occurs
     * @deprecated
     */
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws
        SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        if (isNull(columnIndex)) {
            return null;
        }
        Object obj = _currentRow.get(columnIndex - 1);

		BigDecimal returnValue = null;
		if (obj instanceof Double) {
			returnValue = new BigDecimal(((Double)obj).doubleValue()); 	        	
		} else if (obj instanceof String) {
			returnValue = new BigDecimal(obj.toString());
		} else if (obj instanceof BigDecimal) {
			returnValue = (BigDecimal)obj;
		} else if (obj instanceof Long) {
			returnValue = new BigDecimal(((Long)obj).longValue());
		} else if (obj instanceof Integer) {
			returnValue = new BigDecimal(((Integer)obj).intValue());
		} else if (obj instanceof BigInteger) {
			returnValue = new BigDecimal((BigInteger)obj);
		} else if (obj instanceof Float) {
			returnValue = new BigDecimal(((Float)obj).doubleValue());
		} else if (obj instanceof Short) {
			returnValue = new BigDecimal(((Short)obj).intValue());
		} else {
			throw new java.lang.UnsupportedOperationException("getBigDecimal can not convert the following object into a BigDecimal:" + obj);
		}
		
		return returnValue.setScale(scale);
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>byte</code> array in the Java programming language.
     * The bytes represent the raw values returned by the driver.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>null</code>
     * @exception SQLException if a database access error occurs
     */
    public byte[] getBytes(int columnIndex) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        if (isNull(columnIndex)) {
            return null;
        }
        _currentRow.get(columnIndex - 1);
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method getBytes() not yet implemented.");
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>java.sql.Date</code> object in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>null</code>
     * @exception SQLException if a database access error occurs
     */
    public java.sql.Date getDate(int columnIndex) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        if (isNull(columnIndex)) {
            return null;
        }
        Object obj = _currentRow.get(columnIndex - 1);
        		
  		return (java.sql.Date)copyObject(obj);
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>java.sql.Time</code> object in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>null</code>
     * @exception SQLException if a database access error occurs
     */
    public Time getTime(int columnIndex) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        if (isNull(columnIndex)) {
            return null;
        }
        _currentRow.get(columnIndex - 1);
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method getTime() not yet implemented.");
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>java.sql.Timestamp</code> object in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>null</code>
     * @exception SQLException if a database access error occurs
     */
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        if (isNull(columnIndex)) {
            return null;
        }
        Object obj = _currentRow.get(columnIndex - 1);
        return (Timestamp)obj;
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a stream of ASCII characters. The value can then be read in chunks from the
     * stream. This method is particularly
     * suitable for retrieving large <char>LONGVARCHAR</char> values.
     * The JDBC driver will
     * do any necessary conversion from the database format into ASCII.
     *
     * <P><B>Note:</B> All the data in the returned stream must be
     * read prior to getting the value of any other column. The next
     * call to a <code>getXXX</code> method implicitly closes the stream.  Also, a
     * stream may return <code>0</code> when the method
     * <code>InputStream.available</code>
     * is called whether there is data available or not.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a Java input stream that delivers the database column value
     * as a stream of one-byte ASCII characters;
     * if the value is SQL <code>NULL</code>, the
     * value returned is <code>null</code>
     * @exception SQLException if a database access error occurs
     */
    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        if (isNull(columnIndex)) {
            return null;
        }
        _currentRow.get(columnIndex - 1);
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method getAsciiStream() not yet implemented.");
    }

    /**
     * Gets the value of a column in the current row as a stream of
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * as a stream of Unicode characters.
     * The value can then be read in chunks from the
     * stream. This method is particularly
     * suitable for retrieving large<code>LONGVARCHAR</code>values.  The JDBC driver will
     * do any necessary conversion from the database format into Unicode.
     * The byte format of the Unicode stream must be Java UTF-8,
     * as specified in the Java virtual machine specification.
     *
     * <P><B>Note:</B> All the data in the returned stream must be
     * read prior to getting the value of any other column. The next
     * call to a <code>getXXX</code> method implicitly closes the stream.  Also, a
     * stream may return <code>0</code> when the method
     * <code>InputStream.available</code>
     * is called whether there is data available or not.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a Java input stream that delivers the database column value
     * as a stream in Java UTF-8 byte format;
     * if the value is SQL <code>NULL</code>, the value returned is <code>null</code>
     * @exception SQLException if a database access error occurs
     * @deprecated use <code>getCharacterStream</code> in place of
     *              <code>getUnicodeStream</code>
     */
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        if (isNull(columnIndex)) {
            return null;
        }
        _currentRow.get(columnIndex - 1);
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method getUnicodeStream() not yet implemented.");
    }

    private boolean isNull(int columnIndex) throws SQLException {
        Object obj = _currentRow.get(columnIndex - 1);
        if (obj == null) {
            _wasNull = true;
        } else {
            _wasNull = false;
        } // end if
        return _wasNull;
    }

    /**
     * Gets the value of a column in the current row as a stream of
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a binary stream of
     * uninterpreted bytes. The value can then be read in chunks from the
     * stream. This method is particularly
     * suitable for retrieving large <code>LONGVARBINARY</code> values.
     *
     * <P><B>Note:</B> All the data in the returned stream must be
     * read prior to getting the value of any other column. The next
     * call to a <code>getXXX</code> method implicitly closes the stream.  Also, a
     * stream may return <code>0</code> when the method
     * <code>InputStream.available</code>
     * is called whether there is data available or not.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a Java input stream that delivers the database column value
     * as a stream of uninterpreted bytes;
     * if the value is SQL <code>NULL</code>, the value returned is <code>null</code>
     * @exception SQLException if a database access error occurs
     */
    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        if (isNull(columnIndex)) {
            return null;
        }
        _currentRow.get(columnIndex - 1);
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method getBinaryStream() not yet implemented.");
    }

    //======================================================================
    // Methods for accessing results by column name
    //======================================================================

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>String</code> in the Java programming language.
     *
     * @param columnName the SQL name of the column
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>null</code>
     * @exception SQLException if a database access error occurs
     */
    public String getString(String columnName) throws SQLException {
        int columnIndex = findColumn(columnName);
        return getString(columnIndex);
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>boolean</code> in the Java programming language.
     *
     * @param columnName the SQL name of the column
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>false</code>
     * @exception SQLException if a database access error occurs
     */
    public boolean getBoolean(String columnName) throws SQLException {
        int columnIndex = findColumn(columnName);
        return getBoolean(columnIndex);
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>byte</code> in the Java programming language.
     *
     * @param columnName the SQL name of the column
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>0</code>
     * @exception SQLException if a database access error occurs
     */
    public byte getByte(String columnName) throws SQLException {
        int columnIndex = findColumn(columnName);
        return getByte(columnIndex);
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>short</code> in the Java programming language.
     *
     * @param columnName the SQL name of the column
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>0</code>
     * @exception SQLException if a database access error occurs
     */
    public short getShort(String columnName) throws SQLException {
        int columnIndex = findColumn(columnName);
        return getShort(columnIndex);
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * an <code>int</code> in the Java programming language.
     *
     * @param columnName the SQL name of the column
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>0</code>
     * @exception SQLException if a database access error occurs
     */
    public int getInt(String columnName) throws SQLException {
        int columnIndex = findColumn(columnName);
        return getInt(columnIndex);
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>long</code> in the Java programming language.
     *
     * @param columnName the SQL name of the column
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>0</code>
     * @exception SQLException if a database access error occurs
     */
    public long getLong(String columnName) throws SQLException {
        int columnIndex = findColumn(columnName);
        return getLong(columnIndex);
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>float</code> in the Java programming language.
     *
     * @param columnName the SQL name of the column
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>0</code>
     * @exception SQLException if a database access error occurs
     */
    public float getFloat(String columnName) throws SQLException {
        int columnIndex = findColumn(columnName);
        return getFloat(columnIndex);
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>double</code> in the Java programming language.
     *
     * @param columnName the SQL name of the column
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>0</code>
     * @exception SQLException if a database access error occurs
     */
    public double getDouble(String columnName) throws SQLException {
        int columnIndex = findColumn(columnName);
        return getDouble(columnIndex);
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>java.math.BigDecimal</code> in the Java programming language.
     *
     * @param columnName the SQL name of the column
     * @param scale the number of digits to the right of the decimal point
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>null</code>
     * @exception SQLException if a database access error occurs
     * @deprecated
     */
    public BigDecimal getBigDecimal(String columnName, int scale) throws
        SQLException {
        int columnIndex = findColumn(columnName);
        return getBigDecimal(columnIndex, scale);
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>byte</code> array in the Java programming language.
     * The bytes represent the raw values returned by the driver.
     *
     * @param columnName the SQL name of the column
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>null</code>
     * @exception SQLException if a database access error occurs
     */
    public byte[] getBytes(String columnName) throws SQLException {
        int columnIndex = findColumn(columnName);
        return getBytes(columnIndex);
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>java.sql.Date</code> object in the Java programming language.
     *
     * @param columnName the SQL name of the column
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>null</code>
     * @exception SQLException if a database access error occurs
     */
    public java.sql.Date getDate(String columnName) throws SQLException {
        int columnIndex = findColumn(columnName);
        return getDate(columnIndex);
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>java.sql.Time</code> object in the Java programming language.
     *
     * @param columnName the SQL name of the column
     * @return the column value;
     * if the value is SQL <code>NULL</code>,
     * the value returned is <code>null</code>
     * @exception SQLException if a database access error occurs
     */
    public Time getTime(String columnName) throws SQLException {
        int columnIndex = findColumn(columnName);
        return getTime(columnIndex);
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>java.sql.Timestamp</code> object.
     *
     * @param columnName the SQL name of the column
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>null</code>
     * @exception SQLException if a database access error occurs
     */
    public Timestamp getTimestamp(String columnName) throws SQLException {
        int columnIndex = findColumn(columnName);
        return getTimestamp(columnIndex);
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a stream of
     * ASCII characters. The value can then be read in chunks from the
     * stream. This method is particularly
     * suitable for retrieving large <code>LONGVARCHAR</code> values.
     * The JDBC driver will
     * do any necessary conversion from the database format into ASCII.
     *
     * <P><B>Note:</B> All the data in the returned stream must be
     * read prior to getting the value of any other column. The next
     * call to a <code>getXXX</code> method implicitly closes the stream. Also, a
     * stream may return <code>0</code> when the method <code>available</code>
     * is called whether there is data available or not.
     *
     * @param columnName the SQL name of the column
     * @return a Java input stream that delivers the database column value
     * as a stream of one-byte ASCII characters.
     * If the value is SQL <code>NULL</code>,
     * the value returned is <code>null</code>.
     * @exception SQLException if a database access error occurs
     */
    public InputStream getAsciiStream(String columnName) throws SQLException {
        int columnIndex = findColumn(columnName);
        return getAsciiStream(columnIndex);
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a stream of
     * Unicode characters. The value can then be read in chunks from the
     * stream. This method is particularly
     * suitable for retrieving large <code>LONGVARCHAR</code> values.
     * The JDBC driver will
     * do any necessary conversion from the database format into Unicode.
     * The byte format of the Unicode stream must be Java UTF-8,
     * as defined in the Java virtual machine specification.
     *
     * <P><B>Note:</B> All the data in the returned stream must be
     * read prior to getting the value of any other column. The next
     * call to a <code>getXXX</code> method implicitly closes the stream. Also, a
     * stream may return <code>0</code> when the method <code>available</code>
     * is called whether there is data available or not.
     *
     * @param columnName the SQL name of the column
     * @return a Java input stream that delivers the database column value
     * as a stream of two-byte Unicode characters.
     * If the value is SQL <code>NULL</code>,
     * the value returned is <code>null</code>.
     * @exception SQLException if a database access error occurs
     * @deprecated
     */
    public InputStream getUnicodeStream(String columnName) throws SQLException {
        int columnIndex = findColumn(columnName);
        return getUnicodeStream(columnIndex);
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a stream of uninterpreted
     * <code>byte</code>s.
     * The value can then be read in chunks from the
     * stream. This method is particularly
     * suitable for retrieving large <code>LONGVARBINARY</code>
     * values.
     *
     * <P><B>Note:</B> All the data in the returned stream must be
     * read prior to getting the value of any other column. The next
     * call to a <code>getXXX</code> method implicitly closes the stream. Also, a
     * stream may return <code>0</code> when the method <code>available</code>
     * is called whether there is data available or not.
     *
     * @param columnName the SQL name of the column
     * @return a Java input stream that delivers the database column value
     * as a stream of uninterpreted bytes;
     * if the value is SQL <code>NULL</code>, the result is <code>null</code>
     * @exception SQLException if a database access error occurs
     */
    public InputStream getBinaryStream(String columnName) throws SQLException {
        int columnIndex = findColumn(columnName);
        return getBinaryStream(columnIndex);
    }

    //=====================================================================
    // Advanced features:
    //=====================================================================

    /**
     * Returns the first warning reported by calls on this
     * <code>ResultSet</code> object.
     * Subsequent warnings on this <code>ResultSet</code> object
     * will be chained to the <code>SQLWarning</code> object that
     * this method returns.
     *
     * <P>The warning chain is automatically cleared each time a new
     * row is read.
     *
     * <P><B>Note:</B> This warning chain only covers warnings caused
     * by <code>ResultSet</code> methods.  Any warning caused by
     * <code>Statement</code> methods
     * (such as reading OUT parameters) will be chained on the
     * <code>Statement</code> object.
     *
     * @return the first <code>SQLWarning</code> object reported or <code>null</code>
     * @exception SQLException if a database access error occurs
     */
    public SQLWarning getWarnings() throws SQLException {
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method getWarnings() not yet implemented.");
    }

    /**
     * Clears all warnings reported on this <code>ResultSet</code> object.
     * After this method is called, the method <code>getWarnings</code>
     * returns <code>null</code> until a new warning is
     * reported for this <code>ResultSet</code> object.
     *
     * @exception SQLException if a database access error occurs
     */
    public void clearWarnings() throws SQLException {
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method clearWarnings() not yet implemented.");
    }

    /**
     * Gets the name of the SQL cursor used by this <code>ResultSet</code>
     * object.
     *
     * <P>In SQL, a result table is retrieved through a cursor that is
     * named. The current row of a result set can be updated or deleted
     * using a positioned update/delete statement that references the
     * cursor name. To insure that the cursor has the proper isolation
     * level to support update, the cursor's <code>select</code> statement should be
     * of the form 'select for update'. If the 'for update' clause is
     * omitted, the positioned updates may fail.
     *
     * <P>The JDBC API supports this SQL feature by providing the name of the
     * SQL cursor used by a <code>ResultSet</code> object.
     * The current row of a <code>ResultSet</code> object
     * is also the current row of this SQL cursor.
     *
     * <P><B>Note:</B> If positioned update is not supported, a
     * <code>SQLException</code> is thrown.
     *
     * @return the SQL name for this <code>ResultSet</code> object's cursor
     * @exception SQLException if a database access error occurs
     */
    public String getCursorName() throws SQLException {
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method getCursorName() not yet implemented.");
    }

    /**
     * <p>Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * an <code>Object</code> in the Java programming language.
     *
     * <p>This method will return the value of the given column as a
     * Java object.  The type of the Java object will be the default
     * Java object type corresponding to the column's SQL type,
     * following the mapping for built-in types specified in the JDBC
     * specification.
     *
     * <p>This method may also be used to read datatabase-specific
     * abstract data types.
     *
     * In the JDBC 2.0 API, the behavior of method
     * <code>getObject</code> is extended to materialize
     * data of SQL user-defined types.  When a column contains
     * a structured or distinct value, the behavior of this method is as
     * if it were a call to: <code>getObject(columnIndex,
     * this.getStatement().getConnection().getTypeMap())</code>.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a <code>java.lang.Object</code> holding the column value
     * @exception SQLException if a database access error occurs
     */
    public Object getObject(int columnIndex) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        if (isNull(columnIndex)) {
            return null;
        }
        Object obj = _currentRow.get(columnIndex - 1);
        return copyObject(obj);

    }

    /**
     * Makes sure that the pointer returned is a pointer to the copy of the
     * object and not the original object since we don't know if the object is
     * mutable or not.
     *
     * @param obj The object to copy
     * @return The copy of the object
     * @throws SQLException Any Exception
     */
    private Object copyObject(Object obj) throws SQLException {
        if (!(obj instanceof Serializable)) {
            throw new SQLException("Object is not serializable");
        } // end if

        Object newObj = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BufferedOutputStream bos = new BufferedOutputStream(baos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.
                toByteArray());
            BufferedInputStream bis = new BufferedInputStream(bais);
            ObjectInputStream ois = new ObjectInputStream(bis);

            newObj = ois.readObject();
            ois.close();
            oos.close();
        } catch (Exception e) {
			e.printStackTrace(System.err);
			Debugger.print(DisconnectedResultSet.class, 
						   StrU.convertStackTrace(e),
						   Debugger.FATAL);             	
            throw new SQLException("Unable to copy the object");
        } // try

        return newObj;
    } // copyObject

    /**
     * Gets a branch from a given column index.
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return The disconnectedResultSet for the branch
     * @throws SQLException Any generated error.
     */
    public DisconnectedResultSet getBranch(int columnIndex) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        if (isNull(columnIndex)) {
            return null;
        }
        Object obj = _currentRow.get(columnIndex - 1);
        if (!(obj instanceof DisconnectedResultSet)) {
            throw new SQLException("Field is not a branch");
        } // end if
        return (DisconnectedResultSet)copyObject(obj);
    } // getBranch

    /**
     * Same as getBranch but returns a pointer not a deep copy.
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return The disconnectedResultSet for the branch
     * @throws SQLException Any generated error.
     */
    public DisconnectedResultSet getBranchNoCopy(int columnIndex) throws
        SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        if (isNull(columnIndex)) {
            return null;
        }
        Object obj = _currentRow.get(columnIndex - 1);
        if (!(obj instanceof DisconnectedResultSet)) {
            throw new SQLException("Field is not a branch");
        } // end if
        return (DisconnectedResultSet)obj;
    } // getBranch

    /**
     * Gets a branch from a given column name.
     * @param columnName the name of the branch.
     * @return The disconnectedResultSet for the branch
     * @throws SQLException Any generated error.
     */
    public DisconnectedResultSet getBranch(String columnName) throws
        SQLException {
        int columnIndex = 0;
        try {
            columnIndex = findColumn(columnName);
        } catch (Exception e) {
            return null;
        } // try
        return getBranch(columnIndex);
    } // getBranch

    /**
     * Same as getBranch only returns a pointer not a deep copy.
     * @param columnName the name of the branch.
     * @return The disconnectedResultSet for the branch
     * @throws SQLException Any generated error.
     */
    public DisconnectedResultSet getBranchNoCopy(String columnName) 
    											 throws SQLException {
        int columnIndex = 0;
        try {
            columnIndex = findColumn(columnName);
        } catch (Exception e) {
            return null;
        } // try
        return getBranchNoCopy(columnIndex);
    } // getBranchNoCopy

//	------ START METHODS FOR JBO ----------------------------------------------

// The following methods are meant for use of the JBO.  They allow a DRS to be
// created which will only have rows that are deleted or a DRS which has rows
// that are inserted, updated or unchanged.

	/*
	 * Returns a new DRS which contains only the deleted rows of this
	 *  DRS or has all the inserted, updated and unchanged rows
	 * @param isDeletedDRS true if you want the DRS that is deleted
	 * @return the new DRS 
	 */
	DisconnectedResultSet getModifiedDRS(boolean isDeletedDRS) {
		
		if (isDeletedDRS) {
			return new DisconnectedResultSet(_columnNamesOrdered,
											 new ArrayList(), 
											 (ArrayList)_deleted.clone(), 
											 false, false,
											 false, 0, null, -1, -1, 
											 _fieldCount, _originalFields,
											 false);
		} 
				
		return new DisconnectedResultSet(_columnNamesOrdered,
										 (ArrayList)_rows.clone(), 
										 new ArrayList(), false, false,
										 false, _intCount, null, -1, -1, 
										 _fieldCount, _originalFields,
										 false);
	}
	
	/*
	 * private constructor which allows us to construct modified versions
	 *  of the DRS
	 */
	private DisconnectedResultSet(ArrayList columnNamesOrdered,
								  ArrayList rows,
								  ArrayList deleted,
								  boolean onARow,
								  boolean onInsert,
								  boolean justDeleted,
								  int intCount,
								  Row currentRow,
								  int intRowPosition,
								  int intDeletedRowPosition,
								  int fieldCount,
								  int originalFields,
								  boolean wasNull) {
		_columnNamesOrdered = columnNamesOrdered;
		_rows = rows;
		_deleted = deleted;
		_onARow = onARow;
		_onInsert = onInsert;
		_justDeleted = justDeleted;
		_intCount = intCount;
		_currentRow = currentRow;
		_intRowPosition = intRowPosition;
		_intDeletedRowPosition = intDeletedRowPosition;
		_fieldCount = fieldCount;
		_originalFields = originalFields;
		_wasNull = wasNull;		
	}
	
	
// ------ END METHODS FOR JBO -------------------------------------------------

    public void updateBranch(int columnIndex, DisconnectedResultSet x) throws
        SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        _currentRow.update(columnIndex - 1, x);
    }

    public void updateBranch(String columnName, DisconnectedResultSet x) throws
        SQLException {
        int columnIndex = findColumn(columnName);
        updateBranch(columnIndex, x);
    }

    /**
     * <p>Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * an <code>Object</code> in the Java programming language.
     *
     * <p>This method will return the value of the given column as a
     * Java object.  The type of the Java object will be the default
     * Java object type corresponding to the column's SQL type,
     * following the mapping for built-in types specified in the JDBC
     * specification.
     *
     * <p>This method may also be used to read datatabase-specific
     * abstract data types.
     *
     * In the JDBC 2.0 API, the behavior of the method
     * <code>getObject</code> is extended to materialize
     * data of SQL user-defined types.  When a column contains
     * a structured or distinct value, the behavior of this method is as
     * if it were a call to: <code>getObject(columnIndex,
     * this.getStatement().getConnection().getTypeMap())</code>.
     *
     * @param columnName the SQL name of the column
     * @return a <code>java.lang.Object</code> holding the column value
     * @exception SQLException if a database access error occurs
     */
    public Object getObject(String columnName) throws SQLException {
        int columnIndex = findColumn(columnName);
        Debugger.print(DisconnectedResultSet.class, "column name: " + columnName + " column index: " + 
        		columnIndex + " fieldCount: " + _fieldCount, Debugger.LOTSA_INFO);
        return getObject(columnIndex);
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a
     * <code>java.io.Reader</code> object.
     * @return a <code>java.io.Reader</code> object that contains the column
     * value; if the value is SQL <code>NULL</code>, the value returned is
     * <code>null</code> in the Java programming language.
     * @param columnIndex the first column is 1, the second is 2, ...
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public Reader getCharacterStream(int columnIndex) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        if (isNull(columnIndex)) {
            return null;
        }
        _currentRow.get(columnIndex - 1);
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method getCharacterStream() not yet implemented.");
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a
     * <code>java.io.Reader</code> object.
     *
     * @return a <code>java.io.Reader</code> object that contains the column
     * value; if the value is SQL <code>NULL</code>, the value returned is
     * <code>null</code> in the Java programming language.
     * @param columnName the name of the column
     * @return the value in the specified column as a <code>java.io.Reader</code>
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public Reader getCharacterStream(String columnName) throws SQLException {
        int columnIndex = findColumn(columnName);
        return getCharacterStream(columnIndex);
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a
     * <code>java.math.BigDecimal</code> with full precision.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value (full precision);
     * if the value is SQL <code>NULL</code>, the value returned is
     * <code>null</code> in the Java programming language.
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        if (isNull(columnIndex)) {
            return null;
        }
        Object obj = _currentRow.get(columnIndex - 1);

		BigDecimal returnValue = null;
		if (obj instanceof Double) {
			//returnValue = new BigDecimal(((Double)obj).doubleValue());
			// NOTE: new BigDecimal(double) is not predictable and not recommended
			returnValue = new BigDecimal(((Double)obj).toString());
		} else if (obj instanceof String) {
			returnValue = new BigDecimal(obj.toString());
		} else if (obj instanceof BigDecimal) {
			returnValue = (BigDecimal)obj;
		} else if (obj instanceof Long) {
			returnValue = new BigDecimal(((Long)obj).longValue());
		} else if (obj instanceof Integer) {
			returnValue = new BigDecimal(((Integer)obj).intValue());
		} else if (obj instanceof BigInteger) {
			returnValue = new BigDecimal((BigInteger)obj);
		} else if (obj instanceof Float) {
			returnValue = new BigDecimal(((Float)obj).toString());
		} else if (obj instanceof Short) {
			returnValue = new BigDecimal(((Short)obj).intValue());
		} else {
			throw new java.lang.UnsupportedOperationException("getBigDecimal can not convert the following object into a BigDecimal:" + obj);
		}
		
		return returnValue;
    }

    /**
     * Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a
     * <code>java.math.BigDecimal</code> with full precision.
     *
     * @param columnName the column name
     * @return the column value (full precision);
     * if the value is SQL <code>NULL</code>, the value returned is
     * <code>null</code> in the Java programming language.
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     *
     */
    public BigDecimal getBigDecimal(String columnName) throws SQLException {
        int columnIndex = findColumn(columnName);
        return getBigDecimal(columnIndex);
    }

    /**
     * Moves the cursor a relative number of rows, either positive or negative.
     * Attempting to move beyond the first/last row in the
     * result set positions the cursor before/after the
     * the first/last row. Calling <code>relative(0)</code> is valid, but does
     * not change the cursor position.
     *
     * <p>Note: Calling the method <code>relative(1)</code>
     * is different from calling the method <code>next()</code>
     * because is makes sense to call <code>next()</code> when there
     * is no current row,
     * for example, when the cursor is positioned before the first row
     * or after the last row of the result set.
     *
     * @return <code>true</code> if the cursor is on a row;
     * <code>false</code> otherwise
     * @exception SQLException if a database access error occurs,
     * there is no current row, or the result set type is
     * <code>TYPE_FORWARD_ONLY</code>
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public boolean relative(int rows) throws SQLException {
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method relative() not yet implemented.");
    }

    /**
     * Gives a hint as to the direction in which the rows in this
     * <code>ResultSet</code> object will be processed.
     * The initial value is determined by the
     * <code>Statement</code> object
     * that produced this <code>ResultSet</code> object.
     * The fetch direction may be changed at any time.
     *
     * @exception SQLException if a database access error occurs or
     * the result set type is <code>TYPE_FORWARD_ONLY</code> and the fetch
     * direction is not <code>FETCH_FORWARD</code>
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     * @see Statement#setFetchDirection
     */
    public void setFetchDirection(int direction) throws SQLException {
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method setFetchDirection() not yet implemented.");
    }

    /**
     * Returns the fetch direction for this
     * <code>ResultSet</code> object.
     *
     * @return the current fetch direction for this <code>ResultSet</code> object
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public int getFetchDirection() throws SQLException {
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method getFetchDirection() not yet implemented.");
    }

    /**
     * Gives the JDBC driver a hint as to the number of rows that should
     * be fetched from the database when more rows are needed for this
     * <code>ResultSet</code> object.
     * If the fetch size specified is zero, the JDBC driver
     * ignores the value and is free to make its own best guess as to what
     * the fetch size should be.  The default value is set by the
     * <code>Statement</code> object
     * that created the result set.  The fetch size may be changed at any time.
     *
     * @param rows the number of rows to fetch
     * @exception SQLException if a database access error occurs or the
     * condition <code>0 <= rows <= this.getMaxRows()</code> is not satisfied
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void setFetchSize(int rows) throws SQLException {
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method setFetchSize() not yet implemented.");
    }

    /**
     *
     * Returns the fetch size for this
     * <code>ResultSet</code> object.
     *
     * @return the current fetch size for this <code>ResultSet</code> object
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public int getFetchSize() throws SQLException {
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method getFetchSize() not yet implemented.");
    }

    /**
     * Returns the type of this <code>ResultSet</code> object.
     * The type is determined by the <code>Statement</code> object
     * that created the result set.
     *
     * @return <code>TYPE_FORWARD_ONLY</code>,
     * <code>TYPE_SCROLL_INSENSITIVE</code>,
     * or <code>TYPE_SCROLL_SENSITIVE</code>
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public int getType() throws SQLException {
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method getType() not yet implemented.");
    }

    /**
     * Returns the concurrency mode of this <code>ResultSet</code> object.
     * The concurrency used is determined by the
     * <code>Statement</code> object that created the result set.
     *
     * @return the concurrency type, either <code>CONCUR_READ_ONLY</code>
     * or <code>CONCUR_UPDATABLE</code>
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public int getConcurrency() throws SQLException {
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method getConcurrency() not yet implemented.");
    }

    /**
     * Not implemnted yet
     * @param columnIndex
     * @return an java.net.URL
     * @throws SQLException
     */
    public URL getURL(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException("Method getURL() Not Implemented Yet");
    }

    /**
     * Not implemnted yet
     * @param columnName
     * @return an java.net.URL
     * @throws SQLException
     */
    public URL getURL(String columnName) throws SQLException {
        throw new UnsupportedOperationException("Method getURL() Not Implemented Yet");
    }

    /**
     * Indicates whether the current row has been updated.  The value returned
     * depends on whether or not the result set can detect updates.
     *
     * @return <code>true</code> if the row has been visibly updated
     * by the owner or another, and updates are detected
     * @exception SQLException if a database access error occurs
     *
     * @see DatabaseMetaData#updatesAreDetected
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public boolean rowUpdated() throws SQLException {
        notOnARow();
        return _currentRow.rowUpdated();
    }
	
	/**
	 * Indicates whether the current row has been updated.  The value returned
	 * depends on whether or not the result set can detect updates.
	 * This is exactly the same as <code>rowUpdated</code> except
	 * that an inserted branch does not count as an insert.
	 * 
	 * @return <code>true</code> if the row has been visibly updated
	 * by the owner or another, and updates are detected
	 * @exception SQLException if a database access error occurs
	 *
	 * @see DatabaseMetaData#updatesAreDetected
	 * @since 1.2
	 * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
	 *      2.0 API</a>
	 */
	public boolean rowUpdatedNoBranches() throws SQLException {
		notOnARow();
		return _currentRow.rowUpdatedNoBranches();
	}
	
    /**
     * Indicates whether the current row has had an insertion.
     * The value returned depends on whether or not this
     * <code>ResultSet</code> object can detect visible inserts.
     *
     * @return <code>true</code> if a row has had an insertion
     * and insertions are detected; <code>false</code> otherwise
     * @exception SQLException if a database access error occurs
     *
     * @see DatabaseMetaData#insertsAreDetected
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public boolean rowInserted() throws SQLException {
        notOnARow();
        return _currentRow.rowInserted();
    }

	/**
	 * Indicates whether the current row has had an insertion.
	 * The value returned depends on whether or not this
	 * <code>ResultSet</code> object can detect visible inserts.
	 * This is exactly the same as <code>rowInserted</code> except
	 * that an inserted branch does not count as an insert.
	 *
	 * @return <code>true</code> if a row has had an insertion
	 * and insertions are detected; <code>false</code> otherwise
	 * @exception SQLException if a database access error occurs
	 *
	 * @see DatabaseMetaData#insertsAreDetected
	 * @since 1.2
	 * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
	 *      2.0 API</a>
	 */
	public boolean rowInsertedNoBranches() throws SQLException {
		notOnARow();
		return _currentRow.rowInsertedNoBranches();
	}

    /**
     * Indicates whether a row has been deleted.  A deleted row may leave
     * a visible "hole" in a result set.  This method can be used to
     * detect holes in a result set.  The value returned depends on whether
     * or not this <code>ResultSet</code> object can detect deletions.
     *
     * @return <code>true</code> if a row was deleted and deletions are detected;
     * <code>false</code> otherwise
     * @exception SQLException if a database access error occurs
     *
     * @see DatabaseMetaData#deletesAreDetected
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public boolean rowDeleted() throws SQLException {
        notOnARow();
        // if we have moved to a deleted row and
        // we are not at the end of a deleted row then
        if (_justDeleted) {
        	return true;
        }
        
        if (_intDeletedRowPosition >= 0 &&
        		_intDeletedRowPosition < _deleted.size()) {
        	// if we actually have a row
        	if (_currentRow != null) {
        		return true;
        	}
        }
        return false;
    }

    /**
     * Gives a nullable column a null value.
     *
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code>
     * or <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateNull(int columnIndex) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        _currentRow.update(columnIndex - 1, null);
    }

    /**
     * Updates the designated column with a <code>boolean</code> value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        _currentRow.update(columnIndex - 1, new Boolean(x));
    }

    /**
     * Updates the designated column with a <code>byte</code> value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateByte(int columnIndex, byte x) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        _currentRow.update(columnIndex - 1, new Byte(x));
    }

    /**
     * Updates the designated column with a <code>short</code> value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateShort(int columnIndex, short x) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        _currentRow.update(columnIndex - 1, new Short(x));
    }

    /**
     * Updates the designated column with an <code>int</code> value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateInt(int columnIndex, int x) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        _currentRow.update(columnIndex - 1, new Integer(x));
    }

    /**
     * Updates the designated column with a <code>long</code> value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateLong(int columnIndex, long x) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        _currentRow.update(columnIndex - 1, new Long(x));
    }

    /**
     * Updates the designated column with a <code>float</code> value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateFloat(int columnIndex, float x) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        _currentRow.update(columnIndex - 1, new Float(x));
    }

    /**
     * Updates the designated column with a <code>double</code> value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateDouble(int columnIndex, double x) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        _currentRow.update(columnIndex - 1, new Double(x));
    }

    /**
     * Updates the designated column with a <code>java.math.BigDecimal</code>
     * value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateBigDecimal(int columnIndex, BigDecimal x) throws
        SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        
        _currentRow.update(columnIndex - 1, x);
    }

    /**
     * Updates the designated column with a <code>String</code> value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateString(int columnIndex, String x) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        _currentRow.update(columnIndex - 1, x);
    }

    /**
     * Updates the designated column with a <code>byte</code> array value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateBytes(int columnIndex, byte[] x) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        _currentRow.update(columnIndex - 1, x);
    }

    /**
     * Updates the designated column with a <code>java.sql.Date</code> value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateDate(int columnIndex, java.sql.Date x) throws
        SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        _currentRow.update(columnIndex - 1, x);
    }

    /**
     * Updates the designated column with a <code>java.sql.Time</code> value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateTime(int columnIndex, Time x) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        _currentRow.update(columnIndex - 1, x);
    }

    /**
     * Updates the designated column with a <code>java.sql.Timestamp</code>
     * value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateTimestamp(int columnIndex, Timestamp x) throws
        SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        _currentRow.update(columnIndex - 1, x);
    }

    /**
     * Updates the designated column with an ascii stream value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @param length the length of the stream
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws
        SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method updateAsciiStream() not yet implemented.");
    }

    /**
     * Updates the designated column with a binary stream value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @param length the length of the stream
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws
        SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method updateBinaryStream() not yet implemented.");
    }

    /**
     * Updates the designated column with a character stream value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @param length the length of the stream
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateCharacterStream(int columnIndex, Reader x, int length) throws
        SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method updateCharacterStream() not yet implemented.");
    }

    /**
     * Updates the designated column with an <code>Object</code> value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @param scale for <code>java.sql.Types.DECIMA</code>
     *  or <code>java.sql.Types.NUMERIC</code> types,
     *  this is the number of digits after the decimal point.  For all other
     *  types this value will be ignored.
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateObject(int columnIndex, Object x, int scale) throws
        SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method updateObject() not yet implemented.");
    }

    /**
     * Updates the designated column with an <code>Object</code> value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateObject(int columnIndex, Object x) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        _currentRow.update(columnIndex - 1, x);
    }

    /**
     * Updates the designated column with a <code>null</code> value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnName the name of the column
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateNull(String columnName) throws SQLException {
        int columnIndex = findColumn(columnName);
        updateNull(columnIndex);
    }

    /**
     * Updates the designated column with a <code>boolean</code> value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnName the name of the column
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateBoolean(String columnName, boolean x) throws SQLException {
        int columnIndex = findColumn(columnName);
        updateBoolean(columnIndex, x);
    }

    /**
     * Updates the designated column with a <code>byte</code> value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnName the name of the column
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateByte(String columnName, byte x) throws SQLException {
        int columnIndex = findColumn(columnName);
        updateByte(columnIndex, x);
    }

    /**
     * Updates the designated column with a <code>short</code> value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnName the name of the column
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateShort(String columnName, short x) throws SQLException {
        int columnIndex = findColumn(columnName);
        updateShort(columnIndex, x);
    }

    /**
     * Updates the designated column with an <code>int</code> value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnName the name of the column
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateInt(String columnName, int x) throws SQLException {
        int columnIndex = findColumn(columnName);
        updateInt(columnIndex, x);
    }

    /**
     * Updates the designated column with a <code>long</code> value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnName the name of the column
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateLong(String columnName, long x) throws SQLException {
        int columnIndex = findColumn(columnName);
        updateLong(columnIndex, x);
    }

    /**
     * Updates the designated column with a <code>float	</code> value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnName the name of the column
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateFloat(String columnName, float x) throws SQLException {
        int columnIndex = findColumn(columnName);
        updateFloat(columnIndex, x);
    }

    /**
     * Updates the designated column with a <code>double</code> value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnName the name of the column
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateDouble(String columnName, double x) throws SQLException {
        int columnIndex = findColumn(columnName);
        updateDouble(columnIndex, x);
    }

    /**
     * Updates the designated column with a <code>java.sql.BigDecimal</code>
     * value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnName the name of the column
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateBigDecimal(String columnName, BigDecimal x) throws
        SQLException {
        int columnIndex = findColumn(columnName);
        updateBigDecimal(columnIndex, x);
    }

    /**
     * Updates the designated column with a <code>String</code> value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnName the name of the column
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateString(String columnName, String x) throws SQLException {
        int columnIndex = findColumn(columnName);
        updateString(columnIndex, x);
    }

    /**
     * Updates the designated column with a <code>boolean</code> value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * JDBC 2.0
     *
     * Updates a column with a byte array value.
     *
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row, or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or <code>insertRow</code>
     * methods are called to update the database.
     *
     * @param columnName the name of the column
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateBytes(String columnName, byte[] x) throws SQLException {
        int columnIndex = findColumn(columnName);
        updateBytes(columnIndex, x);
    }

    /**
     * Updates the designated column with a <code>java.sql.Date</code> value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnName the name of the column
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateDate(String columnName, java.sql.Date x) throws
        SQLException {
        int columnIndex = findColumn(columnName);
        updateDate(columnIndex, x);
    }

    /**
     * Updates the designated column with a <code>java.sql.Time</code> value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnName the name of the column
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateTime(String columnName, Time x) throws SQLException {
        int columnIndex = findColumn(columnName);
        updateTime(columnIndex, x);
    }

    /**
     * Updates the designated column with a <code>java.sql.Timestamp</code>
     * value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnName the name of the column
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateTimestamp(String columnName, Timestamp x) throws
        SQLException {
        int columnIndex = findColumn(columnName);
        updateTimestamp(columnIndex, x);
    }

    /**
     * Updates the designated column with an ascii stream value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnName the name of the column
     * @param x the new column value
     * @param length the length of the stream
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateAsciiStream(String columnName, InputStream x, int length) throws
        SQLException {
        int columnIndex = findColumn(columnName);
        updateAsciiStream(columnIndex, x, length);
    }

    /**
     * Updates the designated column with a binary stream value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnName the name of the column
     * @param x the new column value
     * @param length the length of the stream
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateBinaryStream(String columnName, InputStream x, int length) throws
        SQLException {
        int columnIndex = findColumn(columnName);
        updateBinaryStream(columnIndex, x, length);
    }

    public void updateCharacterStream(String columnName, Reader reader,
                                      int length) throws SQLException {
        int columnIndex = findColumn(columnName);
        updateCharacterStream(columnIndex, reader, length);
    }

    /**
     * Updates the designated column with an <code>Object</code> value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnName the name of the column
     * @param x the new column value
     * @param scale for <code>java.sql.Types.DECIMA</code>
     *  or <code>java.sql.Types.NUMERIC</code> types,
     *  this is the number of digits after the decimal point.  For all other
     *  types this value will be ignored.
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateObject(String columnName, Object x, int scale) throws
        SQLException {
        int columnIndex = findColumn(columnName);
        updateObject(columnIndex, x, scale);
    }

    /**
     * Updates the designated column with an <code>Object</code> value.
     * The <code>updateXXX</code> methods are used to update column values in the
     * current row or the insert row.  The <code>updateXXX</code> methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnName the name of the column
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateObject(String columnName, Object x) throws SQLException {
        int columnIndex = findColumn(columnName);
        updateObject(columnIndex, x);
    }

    /**
     * Not implemnted yet
     * @param columnIndex
     * @param x
     * @throws SQLException
     */
    public void updateRef(int columnIndex, java.sql.Ref x) throws SQLException {
        throw new UnsupportedOperationException("Method updateRef() Not Implemented Yet");
    }

    /**
     * Not implemnted yet
     * @param columnName
     * @param x
     * @throws SQLException
     */
    public void updateRef(String columnName, java.sql.Ref x) throws SQLException {
        throw new UnsupportedOperationException("Method updateRef() Not Implemented Yet");
    }

    /**
     * Updates the designated column with a <code>java.sql.Blob</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.4
     */
    public void updateBlob(int columnIndex, java.sql.Blob x) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        _currentRow.update(columnIndex - 1, x);
    }

    /**
     * Updates the designated column with a <code>java.sql.Blob</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnName the name of the column
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.4
     */
    public void updateBlob(String columnName, java.sql.Blob x) throws SQLException {
        int columnIndex = findColumn(columnName);
        updateBlob(columnIndex, x);
    }

    /**
     * Updates the designated column with a <code>java.sql.Clob</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.4
     */
    public void updateClob(int columnIndex, java.sql.Clob x) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        _currentRow.update(columnIndex - 1, x);
    }

    /**
     * Updates the designated column with a <code>java.sql.Clob</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnName the name of the column
     * @param x the new column value
     * @exception SQLException if a database access error occurs
     * @since 1.4
     */
    public void updateClob(String columnName, java.sql.Clob x) throws SQLException {
        int columnIndex = findColumn(columnName);
        updateClob(columnIndex, x);
    }

    /**
     * Not implemnted yet
     * @param columnIndex
     * @param x
     * @throws SQLException
     */
    public void updateArray(int columnIndex, java.sql.Array x) throws SQLException {
        throw new UnsupportedOperationException("Method updateArray() Not Implemented Yet");
    }

    /**
     * Not implemnted yet
     * @param columnName
     * @param x
     * @throws SQLException
     */
    public void updateArray(String columnName, java.sql.Array x) throws SQLException {
        throw new UnsupportedOperationException("Method updateArray() Not Implemented Yet");
    }

    /**
     * Inserts the contents of the insert row into this
     * <code>ResultSet</code> object and into the database.
     * The cursor must be on the insert row when this method is called.
     *
     * @exception SQLException if a database access error occurs,
     * if this method is called when the cursor is not on the insert row,
     * or if not all of non-nullable columns in
     * the insert row have been given a value
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void insertRow() throws SQLException {
        if (!_onInsert) {
            throw new SQLException("must be on insert row.");
        } // end if
        _currentRow.updateRow();
        _rows.add(_currentRow);
        _currentRow = new Row();
        _intCount++;
    }

    /**
     * Updates the underlying database with the new contents of the
     * current row of this <code>ResultSet</code> object.
     * This method cannot be called when the cursor is on the insert row.
     *
     * @exception SQLException if a database access error occurs or
     * if this method is called when the cursor is on the insert row
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void updateRow() throws SQLException {
        notAllowedFromInsertRow();
        _currentRow.updateRow();
    }

    /**
     * Deletes the current row from this <code>ResultSet</code> object
     * and from the underlying database.  This method cannot be called when
     * the cursor is on the insert row.
     *
     * @exception SQLException if a database access error occurs
     * or if this method is called when the cursor is on the insert row
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void deleteRow() throws SQLException {
        notAllowedFromInsertRow();
        notOnARow();
        if (!_currentRow.rowInserted()) {
            _deleted.add(_currentRow);
        } // end if
        _rows.remove(_intRowPosition);
        _intCount = _rows.size();
        if (_intRowPosition > 0) {
			_intRowPosition -= 1;
        }

        _onARow = true;
        _justDeleted = true;
    }

    /**
     * Does not add to the _deleted collection so that it won't be physically
     * deleted later.
     * @throws SQLException
     */
    void removeRow() throws SQLException {
        notAllowedFromInsertRow();
        notOnARow();
        _rows.remove(_intRowPosition);
        _intCount = _rows.size();
        _onARow = false;
        _justDeleted = true;
    }

    /**
     * Refreshes the current row with its most recent value in
     * the database.  This method cannot be called when
     * the cursor is on the insert row.
     *
     * <P>The <code>refreshRow</code> method provides a way for an
     * application to
     * explicitly tell the JDBC driver to refetch a row(s) from the
     * database.  An application may want to call <code>refreshRow</code> when
     * caching or prefetching is being done by the JDBC driver to
     * fetch the latest value of a row from the database.  The JDBC driver
     * may actually refresh multiple rows at once if the fetch size is
     * greater than one.
     *
     * <P> All values are refetched subject to the transaction isolation
     * level and cursor sensitivity.  If <code>refreshRow</code> is called after
     * calling an <code>updateXXX</code> method, but before calling
     * the method <code>updateRow</code>, then the
     * updates made to the row are lost.  Calling the method
     * <code>refreshRow</code> frequently will likely slow performance.
     *
     * @exception SQLException if a database access error
     * occurs or if this method is called when the cursor is on the insert row
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void refreshRow() throws SQLException {
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method refreshRow() not yet implemented.");
    }

    /**
     * Cancels the updates made to the current row in this
     * <code>ResultSet</code> object.
     * This method may be called after calling an
     * <code>updateXXX</code> method(s) and before calling
     * the method <code>updateRow</code> to roll back
     * the updates made to a row.  If no updates have been made or
     * <code>updateRow</code> has already been called, this method has no
     * effect.
     *
     * @exception SQLException if a database access error
     * occurs or if this method is called when the cursor is on the insert row
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void cancelRowUpdates() throws SQLException {
        notAllowedFromInsertRow();
        notOnARow();
        _currentRow.clearTemp();
    }

    /**
     * Moves the cursor to the insert row.  The current cursor position is
     * remembered while the cursor is positioned on the insert row.
     *
     * The insert row is a special row associated with an updatable
     * result set.  It is essentially a buffer where a new row may
     * be constructed by calling the <code>updateXXX</code> methods prior to
     * inserting the row into the result set.
     *
     * Only the <code>updateXXX</code>, <code>getXXX</code>,
     * and <code>insertRow</code> methods may be
     * called when the cursor is on the insert row.  All of the columns in
     * a result set must be given a value each time this method is
     * called before calling <code>insertRow</code>.
     * An <code>updateXXX</code> method must be called before a
     * <code>getXXX</code> method can be called on a column value.
     *
     * @exception SQLException if a database access error occurs
     * or the result set is not updatable
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void moveToInsertRow() throws SQLException {
        if (_onInsert) {
            return;
        }
        _currentRow = new Row();
        _onARow = true;
        _onInsert = true;

    }

    /**
     * Moves the cursor to the remembered cursor position, usually the
     * current row.  This method has no effect if the cursor is not on
     * the insert row.
     *
     * @exception SQLException if a database access error occurs
     * or the result set is not updatable
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public void moveToCurrentRow() throws SQLException {
        if (!_onInsert) {
            return;
        }
        _onInsert = false;
        int row = _intRowPosition + 1;
        _intDeletedRowPosition = -1;
        if (_justDeleted) {
            _onARow = false;
            return;
        } // end if
        if (row < 1 || row > _rows.size()) {
            _onARow = false;
        } else {
            _onARow = true;
            _currentRow = (Row)_rows.get(row - 1);
        } // end if
    }

    /**
     * Returns the <code>Statement</code> object that produced this
     * <code>ResultSet</code> object.
     * If the result set was generated some other way, such as by a
     * <code>DatabaseMetaData</code> method, this method returns
     * <code>null</code>.
     *
     * @return the <code>Statment</code> object that produced
     * this <code>ResultSet</code> object or <code>null</code>
     * if the result set was produced some other way
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public Statement getStatement() throws SQLException {
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method getStatement() not yet implemented.");
    }

    /**
     * Returns the value of the designated column in the current row
     * of this <code>ResultSet</code> object as an <code>Object</code>
     * in the Java programming language.
     * This method uses the given <code>Map</code> object
     * for the custom mapping of the
     * SQL structured or distinct type that is being retrieved.
     *
     * @param i the first column is 1, the second is 2, ...
     * @param map a <code>java.util.Map</code> object that contains the mapping
     * from SQL type names to classes in the Java programming language
     * @return an <code>Object</code> in the Java programming language
     * representing the SQL value
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public Object getObject(int i, Map map) throws SQLException {
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method getObject() not yet implemented.");
    }

    /**
     * Returns the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>Ref</code> object
     * in the Java programming language.
     *
     * @param i the first column is 1, the second is 2, ...
     * @return a <code>Ref</code> object representing an SQL <code>REF</code> value
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public Ref getRef(int i) throws SQLException {
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method getRef() not yet implemented.");
    }

    /**
     * Returns the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>Blob</code> object
     * in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a <code>Blob</code> object representing the SQL <code>BLOB</code> value in
     *         the specified column
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public Blob getBlob(int columnIndex) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        if (isNull(columnIndex)) {
            return null;
        } // end if
        if (!(_currentRow.get(columnIndex - 1) instanceof Blob)) {
            throw new SQLException("Column "+columnIndex+" is not an instance of Blob");
        } // end if
        return (Blob)_currentRow.get(columnIndex - 1);
    }

    /**
     * Returns the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>Clob</code> object
     * in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a <code>Clob</code> object representing the SQL <code>CLOB</code> value in
     *         the specified column
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public Clob getClob(int columnIndex) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        if (isNull(columnIndex)) {
            return null;
        } // end if
        if (!(_currentRow.get(columnIndex - 1) instanceof Clob)) {
            throw new SQLException("Column "+columnIndex+" is not an instance of Clob");
        } // end if
        return (Clob)_currentRow.get(columnIndex - 1);
    }

    /**
     * Returns the value of the designated column in the current row
     * of this <code>ResultSet</code> object as an <code>Array</code> object
     * in the Java programming language.
     *
     * @param i the first column is 1, the second is 2, ...
     * @return an <code>Array</code> object representing the SQL <code>ARRAY</code> value in
     *         the specified column
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public Array getArray(int i) throws SQLException {
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method getArray() not yet implemented.");
    }

    /**
     * Returns the value of the designated column in the current row
     * of this <code>ResultSet</code> object as an <code>Object</code>
     * in the Java programming language.
     * This method uses the specified <code>Map</code> object for
     * custom mapping if appropriate.
     *
     * @param colName the name of the column from which to retrieve the value
     * @param map a <code>java.util.Map</code> object that contains the mapping
     * from SQL type names to classes in the Java programming language
     * @return an <code>Object</code> representing the SQL value in the specified column
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public Object getObject(String colName, Map map) throws SQLException {
        int columnIndex = findColumn(colName);
        return getObject(columnIndex, map);
    }

    /**
     * Returns the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>Ref</code> object
     * in the Java programming language.
     *
     * @param colName the column name
     * @return a <code>Ref</code> object representing the SQL <code>REF</code> value in
     *         the specified column
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public Ref getRef(String colName) throws SQLException {
        int columnIndex = findColumn(colName);
        return getRef(columnIndex);
    }

    /**
     * Returns the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>Blob</code> object
     * in the Java programming language.
     *
     * @param colName the name of the column from which to retrieve the value
     * @return a <code>Blob</code> object representing the SQL <code>BLOB</code> value in
     *         the specified column
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public Blob getBlob(String colName) throws SQLException {
        int columnIndex = findColumn(colName);
        return getBlob(columnIndex);
    }

    /**
     * Returns the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>Clob</code> object
     * in the Java programming language.
     *
     * @param colName the name of the column from which to retrieve the value
     * @return a <code>Clob</code> object representing the SQL <code>CLOB</code>
     * value in the specified column
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public Clob getClob(String colName) throws SQLException {
        int columnIndex = findColumn(colName);
        return getClob(columnIndex);
    }

    /**
     * Returns the value of the designated column in the current row
     * of this <code>ResultSet</code> object as an <code>Array</code> object
     * in the Java programming language.
     *
     * @param colName the name of the column from which to retrieve the value
     * @return an <code>Array</code> object representing the SQL <code>ARRAY</code> value in
     *         the specified column
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public Array getArray(String colName) throws SQLException {
        int columnIndex = findColumn(colName);
        return getArray(columnIndex);
    }

    /**
     * Returns the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>java.sql.Date</code> object
     * in the Java programming language.
     * This method uses the given calendar to construct an appropriate millisecond
     * value for the date if the underlying database does not store
     * timezone information.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param cal the <code>java.util.Calendar</code> object
     * to use in constructing the date
     * @return the column value as a <code>java.sql.Date</code> object;
     * if the value is SQL <code>NULL</code>,
     * the value returned is <code>null</code> in the Java programming language
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public java.sql.Date getDate(int columnIndex, Calendar cal) throws
        SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        if (isNull(columnIndex)) {
            return null;
        }
        
		_currentRow.get(columnIndex - 1);
		
		        
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method getDate() not yet implemented.");
    }

    /**
     * Returns the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>java.sql.Date</code> object
     * in the Java programming language.
     * This method uses the given calendar to construct an appropriate millisecond
     * value for the date if the underlying database does not store
     * timezone information.
     *
     * @param columnName the SQL name of the column from which to retrieve the value
     * @param cal the <code>java.util.Calendar</code> object
     * to use in constructing the date
     * @return the column value as a <code>java.sql.Date</code> object;
     * if the value is SQL <code>NULL</code>,
     * the value returned is <code>null</code> in the Java programming language
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public java.sql.Date getDate(String columnName, Calendar cal) throws
        SQLException {
        int columnIndex = findColumn(columnName);
        return getDate(columnIndex, cal);
    }

    /**
     * Returns the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>java.sql.Time</code> object
     * in the Java programming language.
     * This method uses the given calendar to construct an appropriate millisecond
     * value for the time if the underlying database does not store
     * timezone information.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param cal the <code>java.util.Calendar</code> object
     * to use in constructing the time
     * @return the column value as a <code>java.sql.Time</code> object;
     * if the value is SQL <code>NULL</code>,
     * the value returned is <code>null</code> in the Java programming language
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        if (isNull(columnIndex)) {
            return null;
        }
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method getTime() not yet implemented.");
    }

    /**
     * Returns the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>java.sql.Time</code> object
     * in the Java programming language.
     * This method uses the given calendar to construct an appropriate millisecond
     * value for the time if the underlying database does not store
     * timezone information.
     *
     * @param columnName the SQL name of the column
     * @param cal the <code>java.util.Calendar</code> object
     * to use in constructing the time
     * @param cal the calendar to use in constructing the time
     * @return the column value as a <code>java.sql.Time</code> object;
     * if the value is SQL <code>NULL</code>,
     * the value returned is <code>null</code> in the Java programming language
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public Time getTime(String columnName, Calendar cal) throws SQLException {
        int columnIndex = findColumn(columnName);
        return getTime(columnIndex, cal);
    }

    /**
     * Returns the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>java.sql.Timestamp</code> object
     * in the Java programming language.
     * This method uses the given calendar to construct an appropriate millisecond
     * value for the timestamp if the underlying database does not store
     * timezone information.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param cal the <code>java.util.Calendar</code> object
     * to use in constructing the timestamp
     * @return the column value as a <code>java.sql.Timestamp</code> object;
     * if the value is SQL <code>NULL</code>,
     * the value returned is <code>null</code> in the Java programming language
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws
        SQLException {
        notOnARow();
        columnIndexOutOfRange(columnIndex);
        if (isNull(columnIndex)) {
            return null;
        }
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method getTimestamp() not yet implemented.");
    }

    /**
     * Returns the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>java.sql.Timestamp</code> object
     * in the Java programming language.
     * This method uses the given calendar to construct an appropriate millisecond
     * value for the timestamp if the underlying database does not store
     * timezone information.
     *
     * @param columnName the SQL name of the column
     * @param cal the <code>java.util.Calendar</code> object
     * to use in constructing the date
     * @return the column value as a <code>java.sql.Timestamp</code> object;
     * if the value is SQL <code>NULL</code>,
     * the value returned is <code>null</code> in the Java programming language
     * @exception SQLException if a database access error occurs
     * @since 1.2
     * @see <a href="package-summary.html#2.0 API">What Is in the JDBC
     *      2.0 API</a>
     */
    public Timestamp getTimestamp(String columnName, Calendar cal) throws
        SQLException {
        int columnIndex = findColumn(columnName);
        return getTimestamp(columnIndex, cal);
    } // getRow

    public void close() throws SQLException {
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method close() not yet implemented.");
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        /**@todo: Implement this java.sql.ResultSet method*/
        throw new java.lang.UnsupportedOperationException(
            "Method getMetaData() not yet implemented.");
    }

    ArrayList getRows() {
        return _rows;
    } // getRows

    ArrayList getDeleted() {
        return _deleted;
    } // getDeleted

    /**
     * Sort a DRS by the given field.  Null values will be sorted to the top.  Sorts in ascending order. 
     * Current row is set to beforeFirst().
     * 
     * @param fieldName Field (column) name to sort by.
     * @throws SQLException If unable to get data from the row.  DRS may be partially sorted when this is thrown.
     */
    public void sort(String fieldName) throws SQLException{
    	if(fieldName == null) return;
    	
    	ArrayList al = new ArrayList();
    	al.add(fieldName);
    	sort(al, true);
    }
    
    /** 
     * Sort a DRS by the given field.  Null values will be sorted to the top.
     * Current row is set to beforeFirst().
     * 
     * @param fieldName Names of fields to be sorted by in order of precedence.
     * @param ascending True to sort in ascending order, false for descending.
     * @throws SQLException If unable to get data from the row.  DRS may be partially sorted when this is thrown.
     */
    public void sort(String fieldName, boolean ascending) throws SQLException{
    	if(fieldName == null) return;
    	
    	ArrayList al = new ArrayList();
    	al.add(fieldName);
    	sort(al, ascending);
    }
    
    /** 
     * Sort a DRS by the given field.  Null values will be sorted to the top.
     * Current row is set to beforeFirst().
     * 
     * @param fieldName Names of fields to be sorted by in order of precedence.
     * @param ascending True to sort in ascending order, false for descending.
     * @param ignoreCase True to ignore case, false to check it.
     * @throws SQLException If unable to get data from the row.  DRS may be partially sorted when this is thrown.
     */
    public void sort(String fieldName, boolean ascending, boolean ignoreCase) throws SQLException{
    	if(fieldName == null) return;
    	
    	ArrayList al = new ArrayList();
    	al.add(fieldName);
    	sort(al, ascending, ignoreCase);
    }
    
    /**
     * Sort a DRS by the given field names.  Null values will be sorted to the top.  Current row is set
     * to beforeFirst().
     * 
     * @param fieldNames Names of fields to be sorted by in order of precedence.  NOT TESTED
     * @param ascending True to sort in ascending order, false for descending.
     * @throws SQLException If unable to get data from the row.  DRS may be partially sorted when this is thrown.
     */
    public void sort(ArrayList fieldNames, boolean ascending) throws SQLException {
    	if(fieldNames == null) return;

    	mergeSort(fieldNames, _rows, 0, _rows.size() - 1, ascending, true);
    	beforeFirst();
    }
    
    /**
     * Sort a DRS by the given field names.  Null values will be sorted to the top.  Current row is set
     * to beforeFirst().
     * 
     * @param fieldNames Names of fields to be sorted by in order of precedence.  NOT TESTED
     * @param ascending True to sort in ascending order, false for descending.
     * @param ignoreCase True to ignore case, false to check it.
     * @throws SQLException If unable to get data from the row.  DRS may be partially sorted when this is thrown.
     */
    public void sort(ArrayList fieldNames, boolean ascending, boolean ignoreCase) throws SQLException {
    	if(fieldNames == null) return;

    	mergeSort(fieldNames, _rows, 0, _rows.size() - 1, ascending, ignoreCase);
    	beforeFirst();
    }
    
    /**
     * Perform a merge sort on the array of rows from the low index to the high.  Completely sorts
     * the given range of the array.
     * 
     * @param fieldName Names of fields to be sorted by in order of precedence.
     * @param list Array list to be sorted.
     * @param low Lowest index in the array to be sorted.
     * @param high Highest index in the array to be sorted.
     * @param ascending True to sort in ascending order, false for descending.
     * @param ignoreCase True to ignore case, false to check it.
     * @throws SQLException If unable to get data from the row.  List may be partially sorted when this is thrown.
     */
    private void mergeSort(ArrayList fieldNames, ArrayList list, int low, int high, boolean ascending,
    		boolean ignoreCase) 
    		throws SQLException{
    	// check if there is anything left to sort
    	if(low < high){
    		int mid = (low + high) / 2;
    		mergeSort(fieldNames, list, low, mid, ascending, ignoreCase);		// sort left side of midpoint
    		mergeSort(fieldNames, list, mid + 1, high, ascending, ignoreCase);	// sort right side of midpoint
    		// merge the results into a sorted list
    		mergeSortMerge(fieldNames, list, low, high, ascending, ignoreCase);
    	}
    }
    
    /**
     * Perform a merge of the lower and upper halves of the list starting at low and ending at high.
     * This is used by mergeSort to merge two sorted sections of the array.  The two halves are merged
     * in sorted order.
     * 
     * @param fieldNames Names of fields to be sorted by in order of precedence.
     * @param list Array list containing partially sorted rows.
     * @param low Lowest index to merge.
     * @param high Largest index to merge.
     * @param ascending True to sort in ascending order, false for descending.
     * @param ignoreCase True to ignore case, false to check it.
     * @throws SQLException If unable to get data from the row.
     */
    private void mergeSortMerge(ArrayList fieldNames, ArrayList list, int low, int high, boolean ascending,
    		boolean ignoreCase)
    		throws SQLException{
    	int mid = (high + low) / 2;
    	int numOfElements = high - low;
    	int curElement;
    	ArrayList tempList = new ArrayList();
    	
		// copy lower half to temporary array
    	tempList.addAll(list.subList(low, mid + 1));
    	
    	// copy upper half to temporary in opposite order
    	for(curElement = high; curElement >= mid + 1; curElement--){
    		tempList.add(list.get(curElement));
    	}
    	
    	// loop through the temporary array from both directions
    	// and each time add the smaller back to the original array
    	int left = 0;
    	int right = numOfElements;
    	curElement = low;
    	DisconnectedResultSet.Row leftRow;
    	DisconnectedResultSet.Row rightRow;
    	
    	while(left <= right){
    		leftRow = (DisconnectedResultSet.Row)tempList.get(left);
    		rightRow = (DisconnectedResultSet.Row)tempList.get(right);
    		
    		if((compareRow(leftRow, rightRow, fieldNames, ignoreCase) <= 0 && ascending) ||
    				(compareRow(leftRow, rightRow, fieldNames, ignoreCase) >= 0 && !ascending)){
    			list.set(curElement, tempList.get(left));
    			left++;
    		}
    		else{
    			list.set(curElement, tempList.get(right));
    			right--;
    		}
    		
    		curElement++;
    	}
    }
    
    /**
     * Compares the left and right rows and determines which is larger.
     * If an object is null, it is considered to be the lowest value.  Uses the fieldNames to sort
     * the appropriate columns in order of precedence.
     * 
     * @param leftRow Row to compare.
     * @param rightRow Row to compare.
     * @param fieldNames Names of fields to be sorted by in order of precedence.
     * @param ignoreCase True to ignore case, false to check it.
     * @return 0 if rows are equal, > 0 if left > right, < 0 if left < right
     * @throws SQLException If unable to get appropriate data from the rows.
     */
    private int compareRow(DisconnectedResultSet.Row leftRow, DisconnectedResultSet.Row rightRow,
    		ArrayList fieldNames, boolean ignoreCase) throws SQLException{

    	Object leftField;
    	Object rightField;
    	String fieldName;
    	int comparisson = 0;
    	
    	// loop through each field and as soon as they are different, return the comparisson
    	for(int curField = 0; curField < fieldNames.size(); curField++){
    		fieldName = (String)fieldNames.get(curField);
    		leftField = leftRow.get(fieldName);
    		rightField = rightRow.get(fieldName);
    		
    		comparisson = compareField(leftField, rightField, ignoreCase);
    		if(comparisson != 0){
    			return comparisson;
    		}
    	}
    	
    	return 0;
    }
    
    /**
     * Compares the left and right fields and determines which is larger.
     * If an object is null, it is considered to be the lowest value.
     * 
     * @param leftField Object to compare.
     * @param rightField Object to compare.
     * @param ignoreCase True to ignore case, false to check it.
     * @return 0 if Objects are equal, > 0 if left > right, < 0 if left < right
     */
    private int compareField(Object leftField, Object rightField, boolean ignoreCase){
    	if(leftField == null && rightField == null)
    		return 0;
    	if(leftField == null)
    		return -1;
    	if(rightField == null)
    		return 1;
    	
    	// for now, use strings to compare everything
    	if(ignoreCase)
    		return leftField.toString().compareToIgnoreCase(rightField.toString());
    	else
    		return leftField.toString().compareTo(rightField.toString());
    }
    
    /**
     * Returns the DRS in xml string format.
     */
    public String toXMLString() {
        StringBuffer sb = new StringBuffer();
        sb.append("<disconnectedResultset>");

        defineGeneral(sb);
        defineColumns(sb);
        defineRows(sb);
        defineDeletedRows(sb);

        sb.append("</disconnectedResultset>");
        return sb.toString();
    } // toXMLString

    private void defineRows(StringBuffer sb) {
        sb.append("<rows>");
        Iterator i = _rows.iterator();
        while (i.hasNext()) {
            Row row = (Row)i.next();
            sb.append(row.toXMLString());
        } // wend
        sb.append("</rows>");
    } // defineRows

    private void defineDeletedRows(StringBuffer sb) {
        sb.append("<deleted>");
        Iterator i = _deleted.iterator();
        while (i.hasNext()) {
            Row row = (Row)i.next();
            sb.append(row.toXMLString());
        } // wend
        sb.append("</deleted>");
    } // defineRows

    private void defineGeneral(StringBuffer sb) {
        sb.append("<general");
        sb.append(" onARow=\"" + _onARow + "\"");
        sb.append(" onInsert=\"" + _onInsert + "\"");
        sb.append(" justDeleted=\"" + _justDeleted + "\"");
        sb.append(" count=\"" + _intCount + "\"");
        sb.append(" rowPosition=\"" + _intRowPosition + "\"");
        sb.append(" fieldCount=\"" + _fieldCount + "\"");
        sb.append(" originalFields=\"" + _originalFields + "\"");
        sb.append(" wasNull=\"" + _wasNull + "\"");
        sb.append("/>");
    } // defineGeneral

    private void defineColumns(StringBuffer sb) {
        sb.append("<columns>");
        Iterator i = _columnNamesOrdered.iterator();
        int count = 1;
        while (i.hasNext()) {
            Column c = (Column)i.next();
            sb.append("<column name=\"" + c.getName() +
//                      "' size='"+c.getSize() +
//                      "' nullable='"+c.isNullable() +
                      "\" type=\"" + c.getType() + "\"/>");
            count++;
        } // wend
        sb.append("</columns>");
    } // defineColumns

    class CustomArrayList extends ArrayList implements Serializable {
        static final long serialVersionUID = -6629126505543698516L;

        public CustomArrayList() {
            super();
        } // constructor

        public CustomArrayList(int initialCapacity) {
            super(initialCapacity);
        } // constructor

        public Object get(int index) {
            // stop from throwing and error
            if (index >= this.size()) {
                return null;
            }

            return super.get(index);
        } // get

        public void update(int index, Object obj) {
            int required = (index + 1) - this.size();
            for (int intT = 0; intT < required; intT++) {
                this.add(null);
            } // next
            this.remove(index);
            this.add(index, obj);
        } // update

    } // inner class

    public class Row implements Serializable {
        private static final int TYPE_ORIGINAL = 1;
        private static final int TYPE_MODIFIED = 3;

        static final long serialVersionUID = 6491826657078693765L;

        private ArrayList _original;
        private HashMap _tempChanges;
        private HashMap _modified;

        /**
         * only this package can create a row.
         */
        Row() {
        } // constructor

        /**
         * only this package can create a row.
         */
        Row(Node row) throws DOMException {
            Element original = DOM.getChildElement(row, "original");
            NodeList nl = original.getChildNodes();
            if (nl.getLength() != 0) {
                _original = new ArrayList(nl.getLength());
                define(original, TYPE_ORIGINAL);
            } // end if

            Element modified = DOM.getChildElement(row, "modified");
            nl = modified.getChildNodes();
            if (nl.getLength() != 0) {
                _modified = new HashMap(nl.getLength());
                define(modified, TYPE_MODIFIED);
            } // end if
        } // constructor

        /**
         * Remove all data from the specified column.  The column must not have been removed from the DRS
         * before this function is called as it uses findColumn.
         * 
         * @param column Name of column to delete.
         */
        public void deleteColumn(String column) throws SQLException{
        	int colNum = findColumn(column);
        	if(_original != null){
	        	if(colNum > 0 && colNum <= _original.size()){
	        		_original.remove(colNum-1);
	        	}
        	}
        	
        	if(_tempChanges != null){
        		deleteColumnFromHashmap(colNum, _tempChanges);
        	}
        	
        	if(_modified != null){
        		deleteColumnFromHashmap(colNum, _modified);
        	}
        }
        
        /**
         * Remove the given column and renumber all subsequent columns to account for the loss.
         * 
         * @param colNum Column number starting at 1.
         * @param map HashMap to check for the column and remove.
         */
        private void deleteColumnFromHashmap(int colNum, HashMap map){
        	colNum--;
        	
        	// remove column
        	map.remove(new Integer(colNum));
        	
        	// renumber subsequent columns
        	Set keys = map.keySet();
        	Iterator keyIterator = keys.iterator();
        	Integer curKey;
        	Object curValue;
        	while(keyIterator.hasNext()){
        		curKey = (Integer)keyIterator.next();
        		if(curKey.intValue() > colNum){
        			// renumber
        			curValue = map.get(curKey);
        			map.remove(curKey);
        			map.put(new Integer(curKey.intValue() - 1), curValue);
        		}
        	}
        }
        
        /**
         * Find out if a given column name was one of the original columns.  If column name was not found,
         * return false.
         * 
         * @param column Name of column to test.
         * @return true if column is an original column, else false.
         */
        public boolean isOriginal(String column){
        	int colNum;
        	try{
        		colNum = findColumn(column);
        	}
        	catch(SQLException e){
        		// column not found
        		return false;
        	}
        	
        	if(colNum < 1 || colNum > _original.size()){
        		return false;
        	}
        	
        	return true;
        }
        
        private void define(Element element, int type) throws DOMException{
            NodeList nl = element.getChildNodes();
            for (int intT = 0; intT < nl.getLength(); intT++) {
                Node columnNode = nl.item(intT);
                if (columnNode.getNodeType() == Node.ELEMENT_NODE) {
                    Object obj = getColumnData((Element)columnNode);
                    switch (type) {
                        case TYPE_ORIGINAL:
                            _original.add(obj);
                            break;
                            /*
                                                    case TYPE_TEMP:
                                 Integer key = getKey(columnNode.getNodeName());
                                                        _tempChanges.put(key, obj);
                                                        break;
                        */
                        case TYPE_MODIFIED:
                            Integer key = getKey(columnNode.getNodeName());
                            _modified.put(key, obj);
                            break;
                    } // switch

                } // end if
            } // next
        } // define

        private Integer getKey(String columnName) {
            int i = Integer.parseInt(StrU.right(columnName, 2));
            return new Integer(i - 1);
        } // getKey

        private Object getColumnData(Element columnElement) throws DOMException{
            Attr attr = columnElement.getAttributeNode("v");
            if (attr == null) {
                Element element = DOM.getFirstChildElement(columnElement);
                if (element != null) {
                    // only two elements
                    if (element.getNodeName().equalsIgnoreCase(
                        "disconnectedresultset")) {
                        DisconnectedResultSet drs = new DisconnectedResultSet();
                        drs.parseNode(element);
                        return drs;
                    } else { // a null element
                        return null;
                    } // end if
                } else {
                    throw new DOMException(DOMException.SYNTAX_ERR,
                                           "No v attribute or element!");
                } // end if
            } else {
                int i = Integer.parseInt(StrU.right(columnElement.getNodeName(),
                    2)) - 1;
                Column c = (Column)_columnNamesOrdered.get(i);
                int type = c.getType();

                String value = attr.getValue();
	            switch (type) {
                    case Types.BIT:
                        return new Boolean(StrU.convertToBoolean(value, false));
                    case Types.TINYINT:
                        return new Byte(value);
                    case Types.SMALLINT:
                        return new Short(value);
                    case Types.INTEGER:
                        return new Integer(value);
                    case Types.BIGINT:
                        return new Long(value);
                    case Types.REAL:
                        return new Float(value);
                    case Types.FLOAT:
                    case Types.DOUBLE:
                    case Types.NUMERIC:
                        return new Double(value);
                    case Types.DECIMAL:

                        // blank strings will cause a number format exception
                        if (value.trim().equals("") == true) {
                            value = "0";
                        }
                        return new java.math.BigDecimal(value);
                    case Types.CHAR:
                    case Types.VARCHAR:
                    case Types.LONGVARCHAR:
                        return value;
                    case Types.DATE:
//						Debugger.print(DisconnectedResultSet.class, "Date:" + value, Debugger.LOTSA_INFO);
                    	// check to see if it is given to us in milliseconds else
                    	// parse the date in the db format
                    	try {
							return new java.sql.Date(Long.parseLong(value));
                    	} catch (NumberFormatException nfE) {
                    		try {
                    			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                    			return new java.sql.Date(formatter.parse(value).getTime());
							} catch (Exception e) {
								short s = 1;
								DOMException domE = new DOMException(s, e.getMessage());
								domE.printStackTrace(System.err); 
//								Debugger.print(DisconnectedResultSet.class, 
//											   StrU.convertStackTrace(domE),
//											   Debugger.FATAL);     
//								final String eMessage = TSCException.ROOT_CAUSE + StrU.convertStackTrace(e);
//								System.err.print(eMessage);
								e.printStackTrace();
//								Debugger.print(DisconnectedResultSet.class, 
//											   eMessage,
//											   Debugger.FATAL);     
								throw domE;
							}
                    	}

                    case Types.TIME:
					// check to see if it is given to us in milliseconds else
					// parse the time in the db format
//					Debugger.print(DisconnectedResultSet.class, "Time: "+ value, Debugger.LOTSA_INFO);
						try {
							return new java.sql.Time(Long.parseLong(value));
						} catch (NumberFormatException nfE) {
							try {
								SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
								return new java.sql.Time(formatter.parse(value).getTime());
							} catch (Exception e) {
								short s = 1;
								DOMException domE = new DOMException(s, e.getMessage());
								domE.printStackTrace(System.err); 
//								Debugger.print(DisconnectedResultSet.class, 
//											   StrU.convertStackTrace(domE),
//											   Debugger.FATAL);     
//								final String eMessage = TSCException.ROOT_CAUSE + StrU.convertStackTrace(e);											   
//								System.err.println(eMessage);
//								Debugger.print(DisconnectedResultSet.class,  
//											   eMessage,
//											   Debugger.FATAL);     
								throw domE;
							}
						}
					// check to see if it is given to us in milliseconds else
					// parse the datetime in the db format
                    case Types.TIMESTAMP:
//						Debugger.print(DisconnectedResultSet.class, "Timestamp: " + value, Debugger.LOTSA_INFO);
						// Can't convert null so just return it
						if (StrU.noNull(value).trim().equals("")) {
							return null;
						}
						SimpleDateFormat formatter = null;
						try {
							return new java.sql.Timestamp(Long.parseLong(value));
						} catch (NumberFormatException nfE) {
							// Try formatting a date time
							try {
								formatter = new
									SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
								return new java.sql.Timestamp(formatter.parse(value).getTime());
							} catch (ParseException pE) {
								// try formatting a date
								try {
									formatter = new
										SimpleDateFormat("yyyy-MM-dd");
									return new java.sql.Timestamp(formatter.parse(value).getTime());
								} catch (ParseException pE2) {
									// try formatting a time
									try {
										formatter = new SimpleDateFormat("HH:mm:ss");
										return new java.sql.Timestamp(formatter.parse(value).getTime());
									} catch (Exception e) {
										short s = 1;
										DOMException domE = new DOMException(s, e.getMessage());
										domE.printStackTrace(System.err); 
//										Debugger.print(DisconnectedResultSet.class, 
//													   StrU.convertStackTrace(domE),
//													   Debugger.FATAL); 
//										final String eMessage = TSCException.ROOT_CAUSE + StrU.convertStackTrace(e);
//										System.err.println(eMessage);
										e.printStackTrace(System.err);
//										Debugger.print(DisconnectedResultSet.class,  
//													   eMessage,
//													   Debugger.FATAL);     
										throw domE;
									}
								}
							} catch (Exception e) {
								final short s = 1;
								DOMException domE = new DOMException(s, "Unable to convert datetime to Timestamp:" + value + ":" + e.getMessage());
								domE.printStackTrace(System.err); 
								Debugger.print(DisconnectedResultSet.class, 
											   StrU.convertStackTrace(domE),
											   Debugger.FATAL);     
//								final String eMessage = TSCException.ROOT_CAUSE + StrU.convertStackTrace(e);											   
								final String eMessage = e.getMessage();
								System.err.println(eMessage);
								Debugger.print(DisconnectedResultSet.class,  
											   eMessage,
											   Debugger.FATAL);     
								throw domE;
							}
						}
                    case Types.BINARY:
                    case Types.VARBINARY:
                    case Types.LONGVARBINARY:
                        return value.getBytes();
                } // switch
                throw new DOMException(DOMException.SYNTAX_ERR,
                                       "unknown type: " +
                                       type + " for column: " +
                                       c.getName());

            } // end if
        } // getData

        public String toXMLString() {
            StringBuffer sb = new StringBuffer();
            sb.append("<row>");

            defineOriginal(sb);
            //defineType(sb, "temp", _tempChanges);
            defineType(sb, "modified", _modified);

            sb.append("</row>");
            return sb.toString();
        } // toXMLString

		private void defineType(StringBuffer sb, String type, HashMap map) {
            sb.append("<" + type + ">");

            if (map != null) {
                String data = null;
                Set s = map.entrySet();
                Iterator i = s.iterator();
                while (i.hasNext()) {
                    Map.Entry me = (Map.Entry)i.next();
                    Object obj = me.getValue();
                    Integer key = (Integer)me.getKey();
                    int count = key.intValue() + 1;
                    if (obj == null) {
                        sb.append("<c" + count + "><null/></c" + count + ">");
                    } else if (obj instanceof DisconnectedResultSet) {
                        data = ((DisconnectedResultSet)obj).toXMLString();
                        sb.append("<c" + count + ">" + data + "</c" + count +
                                  ">");
                    } else {
                        data = getText(obj);
                        sb.append("<c" + count + " v=\"" + escape(data) + "\"/>");
                    } // end if
                } // wend
            } // end if
            sb.append("</" + type + ">");
        } // defineType

        private String escape(String data) {
            // do ampersand so it will not replace valid escape sequences
            data = StrU.replaceIn(data, "&", "&amp;");
            data = StrU.replaceIn(data, "<", "&lt;");
            data = StrU.replaceIn(data, ">", "&gt;");
            data = StrU.replaceIn(data, "\"", "&quot;");
			data = StrU.replaceIn(data, "\'", "&apos;");
            return data;
        }

        private void defineOriginal(StringBuffer sb) {
            sb.append("<original>");

            if (_original != null) {
                int count = 1;
                String data = null;
                Iterator i = _original.iterator();
                while (i.hasNext()) {
                    Object obj = i.next();
                    if (obj == null) {
                        sb.append("<c" + count + "><null/></c" + count + ">");
                    } else if (obj instanceof DisconnectedResultSet) {
                        data = ((DisconnectedResultSet)obj).toXMLString();
                        sb.append("<c" + count + ">" + data + "</c" + count +
                                  ">");
                    } else {
                        data = getText(obj);
                        sb.append("<c" + count + " v=\"" + escape(data) + "\"/>");
                    } // end if
                    count++;
                } // wend
            } // end if
            sb.append("</original>");
        } // defineOriginal

        // converts the object into a string
        private String getText(Object obj) {
            String data = null;
            /*
            if (obj instanceof Timestamp) {
                Timestamp t = (Timestamp)obj;
                Date d = new Date(t.getTime());
                data = DateUtil.toText(d,"yyyy-MM-dd HH:mm:ss.SSS");
            } else if (obj instanceof java.sql.Date) {
                data = DateUtil.toText((java.sql.Date)obj,"yyyy-MM-dd");
            } else if (obj instanceof Time) {
                data = DateUtil.toText((Time)obj,"HH:mm:ss.SSS");
            } else {
                data = obj.toString();
            } // end if
            */
            return data;
        } // getText

        public void addToOriginal(Object obj, int fieldCount) {
            if (_original == null) {
                _original = new ArrayList(fieldCount);
            } // end if
            _original.add(obj);
        } // add

        Object getOriginal(int index) {
            return _original.get(index);
        } // getOriginal

        HashMap getModifieds() {
            return _modified;
        } // getOriginal

        /**
         * Gets the data based on the column name.
         * @param name The column Name.
         * @throws SQLException Column not found.
         */
        public Object get(String name) throws SQLException {
            int index = findColumn(name);
            return get(index - 1);
        } // get

        public boolean isColumnModified(String name) throws SQLException {
            int index = findColumn(name);
            return isColumnModified(index - 1);
        } // get

        public boolean isColumnModified(int index) {
            // this row has not changed
            if (this.rowUpdated() == false) {
                return false;
            } // end if

            // The field does not exist in the modified collection
            if (!_modified.containsKey(new Integer(index))) {
                return false;
            } // end if
            try {
                // this happens when the drs is created from scratch.
                if (_original == null) return true;

                Object original = _original.get(index);
                Object modified = _modified.get(new Integer(index));
                if (modified == null || original == null) {
                    if (modified != null || original != null) {
                        return true;
                    } // end if
                    // both are null
                    return false;
                } else {
                    return !modified.equals(original);
                } // end if
            } catch (IndexOutOfBoundsException e) {
                // occurs if they try and get a column from the original
                // and it is not there. For instance you could have added
                // new columns.
                return true;
            } // try

        } // isColumnModified

        /**
         * Gets the current column beginning with zero.
         * @param index
         * @return
         */
        public Object get(int index) {
            if (_tempChanges != null &&
                _tempChanges.containsKey(new Integer(index))) {
                return _tempChanges.get(new Integer(index));
            } else if (_modified != null &&
                       _modified.containsKey(new Integer(index))) {
                return _modified.get(new Integer(index));
            } else {
                try {
                    // this happens when the drs is created from scratch.
                    if (_original == null) return null;

                    Object obj = _original.get(index);
                    return obj;
                } catch (IndexOutOfBoundsException e) {
                    // occurs if they try and get a column from the original
                    // and it is not there.
                    return null;
                } // try
            } // end if
        } // get

        public void update(String columnName, Object obj) throws SQLException {
            int index = findColumn(columnName);
            update(index - 1, obj);
        } // update

        public void update(int index, Object obj) {
            if (_tempChanges == null) {
                _tempChanges = new HashMap();
            } // end if
            _tempChanges.put(new Integer(index), obj);
        } // update

        public void updateRow() {
            if (_tempChanges == null) {
                return;
            }
            if (_modified == null) {
                _modified = new HashMap();
            } // end if
            // now move the temp changes into _modified
            Set s = _tempChanges.entrySet();
            Iterator i = s.iterator();

            while (i.hasNext()) {
                Map.Entry entrySet = (Map.Entry)i.next();
                _modified.put(entrySet.getKey(), entrySet.getValue());
            } // wend

            _tempChanges = null;
        } // updateRow

        public void clearTemp() {
            _tempChanges = null;
        } // clearTemp

        public boolean rowInserted() {
            if (_original == null) {
                return true;
            } else {
                return false;
            } // end if
        } // rowInserted

        public boolean rowUpdated() {
            if (_modified == null) {
                return false;
            } else {
                return true;
            } // end if
        } // rowUpdated

        public int getFieldCount() {
            return _fieldCount;
        } // getFieldCount

        /**
         * same as rowUpdated but do not include branches.
         */
        boolean rowUpdatedNoBranches() {
            if (_modified == null) {
                return false;
            } else {
                Collection c = _modified.values();
                Iterator i = c.iterator();
                while (i.hasNext()) {
                    Object obj = i.next();
                    if (!(obj instanceof DisconnectedResultSet)) {
                        return true;
                    }
                } // wend
                return false;
            } // end if
        } // rowUpdated

		/**
		 * same as rowInserted but do not include branches.
		 */
		boolean rowInsertedNoBranches() {
			if (_original == null) {
				Collection c = _modified.values();
				Iterator i = c.iterator();
				while (i.hasNext()) {
					Object obj = i.next();
					// if the object that was modified was not a disconnected
					// result set then it really was inserted
					if (!(obj instanceof DisconnectedResultSet)) {
						return true;
					}
				} // wend
			} // end if
			
			return false;
		} // rowUpdated
    }

    /**
     * Deep copy.
     * @return
     * @throws CloneNotSupportedException
     */
    public Object clone() throws java.lang.CloneNotSupportedException {
        Object obj = null;
        ObjectInputStream ois = null;
        ObjectOutputStream oos = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(this);
            oos.flush();
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.
                toByteArray());
            ois = new ObjectInputStream(bais);
            obj = ois.readObject();
        } catch (Exception e) {
			CloneNotSupportedException cE = 
				new CloneNotSupportedException(e.toString());
			cE.printStackTrace(System.err); 
			Debugger.print(DisconnectedResultSet.class, 
						   StrU.convertStackTrace(cE),
						   Debugger.FATAL);     
			System.err.print("Root Cause:");
			e.printStackTrace(System.err);
			Debugger.print(DisconnectedResultSet.class, "Root Cause:" + 
						   StrU.convertStackTrace(e),
						   Debugger.FATAL);     
			throw cE;
        } // try
        try {
            if (oos != null) {
                oos.close();
            }
            if (ois != null) {
                ois.close();
            }
        } catch (Exception e) {
			CloneNotSupportedException cE = 
				new CloneNotSupportedException(e.toString());
			cE.printStackTrace(System.err); 
			Debugger.print(DisconnectedResultSet.class, 
						   StrU.convertStackTrace(cE),
						   Debugger.FATAL); 
//			final String eMessage = TSCException.ROOT_CAUSE + StrU.convertStackTrace(e);
			final String eMessage = e.getMessage();   
			System.err.print(eMessage);
			e.printStackTrace(System.err);
			Debugger.print(DisconnectedResultSet.class,  
						   eMessage,
						   Debugger.FATAL);
        } // try
        return obj;
    } // inner class

    /**
     * Defines a DisconnectedResultSet's column attributes.
     * Types are defined by java.sql.Types.
     * Two other types are DisconnectedResultSet.DRS and DisconnectedResultSet.UNKNOWN
     * @author Troy Makaro
     */
    public static class Column implements Serializable {
        static final long serialVersionUID = -1530396216211072355L;
        private String _name;
        private int _type;

        /**
         * Constructs the Column.
         * @param name The name of the column.
         * @param type The type of the column as defined by java.sql.Types
         */
        public Column(String name, int type) {
            _name = name;
            _type = type;
        } // constructor

        /**
         * Gets the name of the column.
         */
        public String getName() {
            return _name;
        } // getName

        /**
         * Gets the type of the column as defined by java.sql.Types.
         * Two other types are DisconnectedResultSet.DRS and DisconnectedResultSet.UNKNOWN
         */
        public int getType() {
            return _type;
        } // getType

    } // inner class

	

} // class