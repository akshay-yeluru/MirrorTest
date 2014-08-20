/*
* Copyright 2005 TSC Software Services Inc. All Rights Reserved.
*
* This software is the proprietary information of TSC Software Services Inc.
* Use is subject to license terms.
*/
package com.tscsoftware.warehouse.cfg;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import sun.util.calendar.BaseCalendar.Date;

import org.apache.log4j.Logger;
import org.w3c.dom.*;

import com.tscsoftware.warehouse.db.Table;


/**
 * Contains all configuration information for a table tag represented in a confiruation xml file.  Also
 * handles the parsing of the tag.
 * 
 * @author Jason
 */
public class CfgTable {
	protected String	_name;
	protected Boolean	_createTable;
	protected Boolean	_copyData;
	protected Boolean	_allSrcColumns;	// if set to true, this table includes all columns in src db
	protected ArrayList	_columns;
	protected ArrayList	_indices;
	protected String	_newName;
	protected String	_whereClause;
	protected String	_destWhereClause;
	protected String 	_debugTable;
	protected String	_selectStatement;
	protected String	_joinClause;
	protected String 	_doCopyCount;
	protected String 	_byteEncoding;
	protected Logger		_logger;

	protected Boolean	_defaultCreateTable;
	protected Boolean	_defaultCopyData;
    private SimpleDateFormat _today = new SimpleDateFormat("yyyyMMdd");

	public CfgTable(){
		initializeMembers();
	}
	
	/**
	 * Parse the table element and setup the object.
	 * 
	 * @param elCol Column element.
	 * @throws ConfigException Any error encountered.
	 */
	public CfgTable(Element elTable) throws Exception,ConfigException{
		initializeMembers();
		parse(elTable);
		_defaultCreateTable = new Boolean(true);
		_defaultCopyData 	= new Boolean(true);
	}
	
	/**
	 * Parse the table element and setup the object with the default values.
	 * 
	 * @param elCol Column element.
	 * @param createTableDefault True if table is created by default, otherwise false.
	 * @param copyDataDefault True if data is to be copied by default, otherwise false.
	 * @throws ConfigException Any error encountered.
	 */
	public CfgTable(Element elTable, boolean createTableDefault, boolean copyDataDefault) throws Exception,ConfigException{
		initializeMembers();
		parse(elTable);
		_defaultCreateTable = new Boolean(createTableDefault);
		_defaultCopyData 	= new Boolean(copyDataDefault);
	}
	
	/**
	 * Set all member variables to their defaults.
	 */
	protected void initializeMembers(){
		_name 			= null;
		_createTable	= null;
		_copyData		= null;
		_allSrcColumns	= new Boolean(true);
		_columns		= new ArrayList();
		_indices		= new ArrayList();
		_newName 		= null;
		_whereClause	= null;
		_destWhereClause= null;
		_debugTable		= null;
		_selectStatement= null;
		_joinClause		= null;
		_doCopyCount	= null;
		_byteEncoding	= null;
		_logger 		= Logger.getLogger(CfgTable.class);
	}
	
	
	/* ***********************************************************
	 * GET AND SET METHODS
	 * ***********************************************************/
	
	
	/**
	 * @return Name of this table.
	 */
	public String getName(){
		return _name;
	}
	
	/**
	 * @return New Name of this table.
	 */
	public String getNewName(){
		return _newName;
	}

	/**
	 * @return createTable attribute stored in this object or the default if none.
	 */
	public Boolean getCreateTable(){
		if(_createTable == null){
			return _defaultCreateTable;
		}
		else{
			return _createTable;
		}
	}
	
	/**
	 * @return copyData attribute stored in this object or the default if none.
	 */
	public Boolean getCopyData(){
		if(_copyData == null){
			return _defaultCopyData;
		}
		else{
			return _copyData;
		}
	}
	
	/**
	 * If all columns in the source database should be created or copied (as per table attributes), returns
	 * true, else false.
	 * 
	 * @return If all source columns should be copied to the destination table.
	 */
	public Boolean getAllSrcColumns(){
		return _allSrcColumns;
	}
	
	/**
	 * @return Where clause for source table. Useful to select only specific records from source table.
	 */
	public String getWhereClause(){
		return _whereClause;
	}

	/**
	 * @return select statement for source table. Useful to select only specific records from source table.
	 */
	public String getSelectStatement(){
		return _selectStatement;
	}

	/**
	 * @return join clause for source table. Useful to select only specific records from source table.
	 */
	public String getJoinClause(){
		return _joinClause;
	}
	
	/**
	 * @return join clause for source table. Useful to select only specific records from source table.
	 */
	public String getByteEncoding(){
		return _byteEncoding;
	}

	
	/**
	 * @return whethere copy count should be done on this table source table. May need this to speed up copy.
	 */
	public String getDoCopyCount(){
		return _doCopyCount;
	}

	/**
	 * @return debugMode for table. Only copies one row at a time instead of 50.
	 */
	public String getDebugTable(){
		return _debugTable;
	}
	/**
	 * @return Where clause for destination table. This is used to verify all rows copied successfully.
	 */
	public String getDestWhereClause(){
		return _destWhereClause;
	}
	
	/**
	 * @return Number of column tags in this table tag.
	 */
	public int getNumColumns(){
		return _columns.size();
	}
	
	/**
	 * @return Number of index tags in this table tag.
	 */
	public int getNumIndices(){
		return _indices.size();
	}
	
	/**
	 * Retrieve the configuration data for a column based on the index provided.
	 * 
	 * @param colNum Index of column configuration to be retrieved.
	 * @return CfgColumn containing the column's configuration data, null on error.
	 */
	public CfgColumn getCfgColumn(int colNum){
		if(colNum >= 0 && colNum < _columns.size()){
			return (CfgColumn)_columns.get(colNum);
		}
		
		return null;
	}
	
	/**
	 * Retrieve the configuration data for a column based on the name provided.  If 
	 * 
	 * @param colName Case sensitive name of column configuration to be retrieved.
	 * @return CfgColumn containing the column's configuration data, null on error or not found.
	 */
	public CfgColumn getCfgColumn(String colName){
		if(colName == null) return null;
		CfgColumn tmpCol;
		
		for(int curCol = 0; curCol < _columns.size(); curCol++){
			tmpCol = (CfgColumn)_columns.get(curCol); 
			// use this if statement to match wild cards, need to do more checking on other attributes then
			//if(StringPattern.match(colName, tmpCol.getName())){
			if(tmpCol.getName().compareTo(colName) == 0){
				return tmpCol;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieve the configuration data for an index based on the index provided.
	 * 
	 * @param indexNum Index of index configuration to be retrieved.
	 * @return CfgIndex containing the index's configuration data, null on error.
	 */
	public CfgIndex getCfgIndex(int indexNum){
		if(indexNum >= 0 && indexNum < _indices.size()){
			return (CfgIndex)_indices.get(indexNum);
		}
		
		return null;
	}
	
	/**
	 * Retrieve the configuration data for an index based on the name provided.
	 * 
	 * @param indexName Case sensitive name of index configuration to be retrieved.
	 * @return CfgIndex containing the index's configuration data, null on error or not found.
	 */
	public CfgIndex getCfgIndex(String indexName){
		if(indexName == null) return null;
		CfgIndex tmpIndex;
		
		for(int curIndex = 0; curIndex < _indices.size(); curIndex++){
			tmpIndex = (CfgIndex)_indices.get(curIndex); 
			//if(tmpIndex.getName().compareTo(indexName) == 0){
			if(indexName.equalsIgnoreCase(tmpIndex.getName())){
				return tmpIndex;
			}
		}
		
		return null;
	}
	
	/**
	 * Add a column to this table's configuration.
	 * 
	 * @param col Column configuration to add to the table.
	 */
	public void addColumn(CfgColumn col){
		_columns.add(col);
	}
	
	/**
	 * Add an index to this table's configuration.
	 * 
	 * @param index Index configuration to add to the table.
	 */
	public void addIndex(CfgIndex index){
		_indices.add(index);
	}
	
	/**
	 * Set this table to copy data from the source database.
	 * 
	 * @param copy True to copy the data, else false.
	 */
	public void setCopyData(boolean copy){
		_copyData = new Boolean(copy);
	}
	
	/**
	 * Set this table to create itself from the source database template.
	 * 
	 * @param create True to create, else false.
	 */
	public void setCreateTable(boolean create){
		_createTable = new Boolean(create);
	}
	
	/**
	 * Set the default to use if copy data is not set.
	 * 
	 * @param defCopy True to copy data by default, else false.
	 */
	public void setDefaultCopyData(boolean defCopy){
		_defaultCopyData = new Boolean(defCopy);
	}
	
	/**
	 * Set the default to use if create table is not set.
	 * 
	 * @param defCreate True to create table by default, else false.
	 */
	public void setDefaultCreateTable(boolean defCreate){
		_defaultCreateTable = new Boolean(defCreate);
	}
	
	/**
	 * Set the table name.
	 * 
	 * @param name Name of table.
	 */
	public void setName(String name){
		_name = name;
	}
	
	
	/* ***********************************************************
	 * PARSE METHODS
	 * ***********************************************************/
	
	
	/**
	 * Parse the table element and populate this object.
	 * 
	 * @param elTable Table element.
	 * @throws ConfigException On any error encountered.
	 */
	public void parse(Element elTable) throws Exception,ConfigException{
		initializeMembers();

		// get name
		_name = elTable.getAttribute("name");
		if(_name == null || _name.length() <= 0){
			throw new ConfigException("Missing required attribute \"name\" in table tag.");
		}
		
		// get create table
		String tmp = elTable.getAttribute("createTable");
		if(tmp == null || tmp.length() < 1){
			_createTable = null;
		}
		else{
			_createTable = new Boolean(tmp);
		}

		//get debugmode
		tmp = elTable.getAttribute("debugMode");
		if(tmp == null || tmp.length() < 1){
			_debugTable = "N";
		}else {
			_debugTable = tmp;
		}
		
		// get copy data
		tmp = elTable.getAttribute("copyData");
		if(tmp == null || tmp.length() < 1){
			_copyData = null;
		}
		else{
			_copyData = new Boolean(tmp);
		}
		
		// get copy count
		tmp = elTable.getAttribute("copyCount");
		if(tmp == null || tmp.length() < 1){
			_doCopyCount = "Y";
		}else{
			_doCopyCount = tmp;
		}

		// get new table name (optional)
		tmp = elTable.getAttribute("newName");
		if(tmp == null || tmp.length() < 1){
			_newName = null;
		}else{
			_newName = tmp;
		}
		// get where clause (optional)
		tmp = elTable.getAttribute("whereClause");
		if(tmp == null || tmp.length() < 1){
			_whereClause = null;
		}else {
			// The XML parser does not allow "&" characters that are required in Binary bit comparison. Translate the replacement keyword if specified.
			tmp = tmp.replace("[binary and]", "&");
			// The XML parser does not allow "<" characters that are required in Binary bit comparison. 
			// Translate < from [lt], > from [gt], <= from [lteq] and >= from [gteq] if specified.
			tmp = tmp.replace("[lt]", "<");
			tmp = tmp.replace("[gt]", ">");
			tmp = tmp.replace("[lteq]", "<=");
			tmp = tmp.replace("[gteq]", ">=");
			
			// TESTING REPLACE OF DATES FOR KEYWORDS
			if(tmp.contains("[HR_MINUS_1]")){
				tmp = tmp.replace("[HR_MINUS_1]", subtractTime(1));
			}else if(tmp.contains("[HR_MINUS_3]")){
				tmp = tmp.replace("[HR_MINUS_3]", subtractTime(3));
			}else if(tmp.contains("[HR_MINUS_6]")){
				tmp = tmp.replace("[HR_MINUS_6]", subtractTime(6));
			}else if(tmp.contains("[HR_MINUS_12]")){
				tmp = tmp.replace("[HR_MINUS_12]", subtractTime(12));
			}else if(tmp.contains("[HR_MINUS_24]")){
				tmp = tmp.replace("[HR_MINUS_24]", subtractTime(24));
			}else if(tmp.contains("[PAY_YR_MINUS_1]")){
				tmp = tmp.replace("[PAY_YR_MINUS_1]", subtractPayTime(1,0,"YEAR"));
			}else if(tmp.contains("[PAY_YR_MINUS_2]")){
				tmp = tmp.replace("[PAY_YR_MINUS_2]", subtractPayTime(2,0,"YEAR"));
			}else if(tmp.contains("[PAY_PER_MINUS_ONE_12]")){
				tmp = tmp.replace("[PAY_PER_MINUS_ONE_12]", subtractPayTime(12,12,"PERIOD"));
			}else if(tmp.contains("[PAY_PER_MINUS_ONE_24]")){
				tmp = tmp.replace("[PAY_PER_MINUS_ONE_24]", subtractPayTime(12,24,"PERIOD"));
			}else if(tmp.contains("[PAY_PER_MINUS_ONE_AND_HALF_12]")){
				tmp = tmp.replace("[PAY_PER_MINUS_ONE_AND_HALF_12]", subtractPayTime(18,12,"PERIOD"));
			}else if(tmp.contains("[PAY_PER_MINUS_ONE_AND_HALF_24]")){
				tmp = tmp.replace("[PAY_PER_MINUS_ONE_AND_HALF_24]", subtractPayTime(18,24,"PERIOD"));
			}else if(tmp.contains("[PAY_PER_MINUS_TWO_12]")){
				tmp = tmp.replace("[PAY_PER_MINUS_TWO_12]", subtractPayTime(24,12,"PERIOD"));
			}else if(tmp.contains("[PAY_PER_MINUS_TWO_24]")){
				tmp = tmp.replace("[PAY_PER_MINUS_TWO_24]", subtractPayTime(24,24,"PERIOD"));
			}else if(tmp.contains("[PAY_SER_MINUS_ONE_12]")){
				tmp = tmp.replace("[PAY_SER_MINUS_ONE_12]", subtractPayTime(12,12,"SERIAL"));
			}else if(tmp.contains("[PAY_SER_MINUS_ONE_24]")){
				tmp = tmp.replace("[PAY_SER_MINUS_ONE_24]", subtractPayTime(12,24,"SERIAL"));
			}else if(tmp.contains("[PAY_SER_MINUS_ONE_AND_HALF_12]")){
				tmp = tmp.replace("[PAY_SER_MINUS_ONE_AND_HALF_12]", subtractPayTime(18,12,"SERIAL"));
			}else if(tmp.contains("[PAY_SER_MINUS_ONE_AND_HALF_24]")){
				tmp = tmp.replace("[PAY_SER_MINUS_ONE_AND_HALF_24]", subtractPayTime(18,24,"SERIAL"));
			}else if(tmp.contains("[PAY_SER_MINUS_TWO_12]")){
				tmp = tmp.replace("[PAY_SER_MINUS_TWO_12]", subtractPayTime(24,12,"SERIAL"));
			}else if(tmp.contains("[PAY_SER_MINUS_TWO_24]")){
				tmp = tmp.replace("[PAY_SER_MINUS_TWO_24]", subtractPayTime(24,24,"SERIAL"));
			}
			_logger.debug("tmp:"+tmp);
			_whereClause = tmp;
		}

		// get join clause (optional)
		tmp = elTable.getAttribute("joinClause");
		if(tmp == null || tmp.length() < 1){
			_joinClause = null;
		}else {
			_joinClause = tmp;
		}

		// get select Statement if not all field required - particularly for a join (optional)
		tmp = elTable.getAttribute("selectStatement");
		if(tmp == null || tmp.length() < 1){
			_selectStatement = null;
		}else {
			_selectStatement = tmp;
		}

		// get join clause (optional)
		tmp = elTable.getAttribute("byteEncoding");
		if(tmp == null || tmp.length() < 1){
			_byteEncoding = null;
		}else {
			_byteEncoding = tmp;
		}

		
		// get destrination table where clause (optional)
		tmp = elTable.getAttribute("destWhereClause");
		if(tmp == null || tmp.length() < 1){
			_destWhereClause = null;
		}else{
			_destWhereClause = tmp;
		}		
		// get columns
		NodeList columns = elTable.getElementsByTagName("column");
		for(int curCol = 0; curCol < columns.getLength(); curCol++){
			_columns.add(new CfgColumn((Element)columns.item(curCol)));
		}
		NodeList srcColumns = elTable.getElementsByTagName("srcColumns");
		if(srcColumns == null || srcColumns.getLength() < 1){
			if(_columns.size() > 0){
				// other columns specified, do not copy source columns by default
				_allSrcColumns = new Boolean(false);
			}
			// else no columns specified, use default
		}
		else{
			_allSrcColumns = new Boolean(true);
		}
		
		// get indices
		NodeList indices = elTable.getElementsByTagName("index");
		for(int curIndex = 0; curIndex < indices.getLength(); curIndex++){
			_indices.add(new CfgIndex((Element)indices.item(curIndex)));
		}
	}
	
	private String subtractTime(int months)throws Exception{
    	GregorianCalendar cal = new GregorianCalendar();
    	cal.setTime(new java.util.Date());
    	cal.add(Calendar.MONTH, (months * -1));
    	String displayDate = _today.format(cal.getTime());
    	return displayDate;

	}
	private String subtractPayTime(int time,int totalPers,String mode)throws Exception{
// time is equivalent of months
// totalPers is total number of periods for a year in this payroll - 12 / 24
		GregorianCalendar cal = new GregorianCalendar();
    	cal.setTime(new java.util.Date());
    	String returnStr = "";
    	if(mode.equalsIgnoreCase("YEAR")){
    		cal.add(Calendar.YEAR, (time * -1));
    		returnStr = _today.format(cal.getTime()).substring(0,4);
    	}else if(mode.equalsIgnoreCase("PERIOD")){
    		//12 goes back to one year previous and period 00
    		//18 goes back to one year previous and period 12
    		cal.add(Calendar.MONTH, (time * -1));
    		returnStr = _today.format(cal.getTime()).substring(0,4);
    		String tempStr = _today.format(cal.getTime()).substring(4,6);
			int per = 0;
			if(totalPers == 24){
				per = Integer.parseInt(tempStr)*2 ;
			}
			if(per != 0){
				tempStr = StrU.lPad(""+per, 2, '0');
			}
			returnStr = returnStr+tempStr;
    	}else if(mode.equalsIgnoreCase("SERIAL")){
    		cal.add(Calendar.MONTH, (time * -1));
    		returnStr = _today.format(cal.getTime()).substring(0,4);
    		String tempStr = _today.format(cal.getTime()).substring(4,6);
			int per = 0;
			if(totalPers == 24){
				per = Integer.parseInt(tempStr)*2 ;
			}
			if(per != 0){
				tempStr = StrU.lPad(""+per, 2, '0');
			}
			returnStr = returnStr+tempStr;
			_logger.debug("returnStr:"+returnStr);
			int tempReturn = Integer.parseInt(returnStr);
			tempReturn = 11000000 - tempReturn;
			_logger.debug("tempReturn:"+tempReturn);
			returnStr = (""+tempReturn).substring(2,8)+"99";
			_logger.debug("returnStr final:"+returnStr);
			
    	}
    	
    	
    	return returnStr;

	}
}
