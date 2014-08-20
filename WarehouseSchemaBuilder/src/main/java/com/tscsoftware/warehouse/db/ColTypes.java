/*
 * Copyright 2005 TSC Software Services Inc. All Rights Reserved.
 *
 * This software is the proprietary information of TSC Software Services Inc.
 * Use is subject to license terms.
 */
package com.tscsoftware.warehouse.db;

import java.sql.*;
import java.util.ArrayList;

import com.tscsoftware.warehouse.WarehouseException;

/**
 * Contains column type data.
 * 
 * @author Jason S
 */
public class ColTypes {

	ArrayList	_colTypes;
	
	ColTypes(){
		_colTypes = null;
	}
	
	/**
	 * Initializes the object with all of the field types provided in rsColTypes.  Eliminates the need to
	 * call setTypes().
	 * 
	 * @param rsColTypes ResultSet containing field types.  Must be set to beforeFirst before calling this
	 * method.
	 * @throws SQLException Any SQL error encountered.
	 * @throws WarehouseException Method is used improperly.
	 */
	ColTypes(ResultSet rsColTypes) throws SQLException, WarehouseException{
		setTypes(rsColTypes);
	}
	
	/**
	 * Sets this class to the types in the provided ResultSet.  Removes any previous types.  The rsColTypes
	 * result set will be closed when method returns (unless exception thrown).
	 *
	 * @param rsColTypes ResultSet containing field types.  Must be set to beforeFirst before calling this
	 * method.
	 * 
	 * @throws SQLException Any SQL error encountered.
	 * @throws WarehouseException Method is used improperly.
	 */
	public void setTypes(ResultSet rsColTypes) throws SQLException, WarehouseException{
		_colTypes = null;	// ensure garbage collection
		_colTypes = new ArrayList();
		
		// ensure we are before the first row, if not, attempt to go there
		try{
			if(!rsColTypes.isBeforeFirst()){
				try{
					rsColTypes.beforeFirst();
				}
				catch(SQLException e){
					throw new WarehouseException("setTypes() method called with a ResultSet that is not on the first row.");
				}
			}
		}
		catch(SQLException e){} // were just checking anyway, continue
		
		// add each type in the result set
		int type;
		while(rsColTypes.next()){
			type = rsColTypes.getInt("DATA_TYPE");
			_colTypes.add(new ColType(type, rsColTypes.getString("TYPE_NAME"), rsColTypes.getString("CREATE_PARAMS")));
			//System.out.println(type + "," + rsColTypes.getString("TYPE_NAME")); // print list of all supported types
		}
		
		rsColTypes.close();
	}
	
	/**
	 * Search the available column types for the data type.
	 * 
	 * @param dataType Data type (DATA_TYPE in typeInfo Result Set) to search for.
	 * @return ColType objects if found in an Array List, else null.
	 */
	public ArrayList get(int dataType){
		return getColType(dataType);
	}
	
	/**
	 * Search the available column types for the data type.
	 * 
	 * @param dataType Data type (DATA_TYPE in typeInfo Result Set) to search for.
	 * @return ColType objects if found in an Array List, else null.
	 */
	public ArrayList getColType(int dataType){
		ArrayList foundTypes = new ArrayList();
		ColType colType = null;
		
		for(int curType = 0; curType < _colTypes.size(); curType++){
			colType = (ColType)_colTypes.get(curType);
			if(colType.getType() == dataType){
				foundTypes.add(colType);
			}
		}
		
		if(foundTypes.size() > 0){
			return foundTypes;
		}
		else{
			return null;
		}
	}
	
	/**
	 * Searches the available column types for the type name specified, and
	 * returns the first match found (there should only ever be one).  Ignores case.
	 * 
	 * @param name Name of type to search for.
	 * @return ColType matching name, null if not found.
	 */
	public ColType get(String name){
		ColType colType = null;
		for(int curType = 0; curType < _colTypes.size(); curType++){
			colType = (ColType)_colTypes.get(curType);
			if(colType.getName().trim().compareToIgnoreCase(name) == 0)
				return colType;
		}
		
		return null;
	}
}
