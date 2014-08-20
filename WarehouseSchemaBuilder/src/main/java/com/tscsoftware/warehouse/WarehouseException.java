package com.tscsoftware.warehouse;
/*
 * Copyright 2005 TSC Software Services Inc. All Rights Reserved.
 *
 * This software is the proprietary information of TSC Software Services Inc.
 * Use is subject to license terms.
 */
/**
 * @author Jason
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class WarehouseException extends Exception {
	
	/* ERROR CODES */
	public static final short UNKNOWN			= 0;
	public static final short RETRY_CONNECTION 	= 1; // request to try re-establising the connection and re-calling failed function
	
	protected short _errCode = 0;
	
	
	
	public WarehouseException(String msg){
		super(msg);
	}
	
	public WarehouseException(String msg, short errCode){
		super(msg);
		_errCode = errCode;
	}
	
	public WarehouseException(String message, Throwable cause) {
        super(message, cause);
    }
	
	public WarehouseException(Throwable cause) {
        super(cause);
    }
	
	public short getErrCode(){
		return _errCode;
	}
	
	
}
