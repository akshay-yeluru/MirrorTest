/*
 * Copyright 2005 TSC Software Services Inc. All Rights Reserved.
 *
 * This software is the proprietary information of TSC Software Services Inc.
 * Use is subject to license terms.
 */
package com.tscsoftware.warehouse;

import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.TriggeringEventEvaluator;

/**
 * Used for the log4j engine to determine if sending an e-mail message is necessary.  Basically,
 * this class will always say yes at the end of a warehouse process.
 * 
 * @author Jason S
 */
public class Log implements TriggeringEventEvaluator {

	/**
	 * 
	 */
	public Log() {
		super();
		// Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.apache.log4j.spi.TriggeringEventEvaluator#isTriggeringEvent(org.apache.log4j.spi.LoggingEvent)
	 */
	public boolean isTriggeringEvent(LoggingEvent event) {
		
		// marks end of warehouse, send e-mail
		return event.getRenderedMessage().startsWith("Done warehouse ");
	}

}
