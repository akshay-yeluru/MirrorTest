/*
* Copyright 2001 TSC Software Services Inc. All Rights Reserved.
*
* This software is the proprietary information of TSC Software Services Inc.
* Use is subject to license terms.
 */
package com.tscsoftware.warehouse.cfg;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.StringTokenizer;
import java.text.DecimalFormat;
import java.util.*;

/**
 * This static class was designed to emulate some of the openVMS Basic string functions.
 *
 * @author Troy Makaro
 * @version $Revision$
 */
public class StrU {

	/**
	 * constant of a blank string to be used
	 */
	public static final String BLANK = "";  //$NON-NLS-1$

    /**
     * A simplistic string prompt method for use with test programs and the like.
     * @param prompt The prompt you want to display.
     * @return The string response.
     */
    public static String prompt(String prompt) {
        byte[] b = new byte [1000];

        String strResult = BLANK;
        try {
            
            int intCount = System.in.read(b);
            strResult = new String(b, 0,intCount-1);
        } catch (Exception e) {
            return BLANK;
        } // try
        return new String(strResult);
    } // prompt

    /**
     * Gets the right portion of strText starting at intStart.
     * @param strText  The text you want to get the data from.
     * @param intStart The starting position for collecting data from strText. 1 being the
     *                 first character.
     * @return The right portion of strText starting at intStart.
     */
    public static String right(String strText,int intStart) {
        if (intStart < 1) intStart = 1;
        if (strText.length() < intStart)
            return BLANK;
        return strText.substring(intStart-1, strText.length()).toString();
    } // right

    /**
     * Pads the right of strText with spaces then truncates to intSize.
     * @param strText  The text you want to pad with spaces.
     * @param intSize  The new size of strText.
     * @return strText padded to the right with spaces.
     */
    public static String rPad(String strText,int intSize) {
        if (strText.length() == intSize) return strText;
        return left(strText+space(intSize),intSize);
    } // rPad

    /**
     * Pads the right of strText with the string you specify, then truncates
     * to intSize.
     * @param strText  The text you want to pad with your string
     * @param intSize  The new size of strText.
     * @param strRepeat  The new size of strText.
     * @return strText padded to the right with your strRepeat.
     */
    public static String rPad(String strText,int intSize, String strRepeat) {
        if (strText.length() == intSize) return strText;
        return left(strText+repeatString(strRepeat,intSize),intSize);
    } // rPad

    /**
     * Pads the left of the string with spaces.
     * @param strText The text you want to pad.
     * @param intSize The overall size of the new string.
     * @return The left padded string.
     */
    public static String lPad(String strText,int intSize) {
        return lPad(strText, intSize, ' ');
    } // lPad

	/**
	 * Pads the left of the string with spaces.
	 * @param strText The text you want to pad.
	 * @param intSize The overall size of the new string.
	 * @return The left padded string.
	 */
	public static String lPad(String strText,int intSize, char txtPad) {
		if (strText.length() >= intSize) {
			return left(strText,intSize);
		} // end if
		return replicate(intSize-strText.length(), txtPad)+strText;
	} // lPad

    /**
     * Gets the middle of the passed in text based on a start position and length.
     * @param strText The text you want to use. Duh!
     * @param intStart The starting point for the data collection. 1 being the first character.
     * @param intLength The length of the segment you want to extract from strText.
     * @return The middle of the passed in text based on a start position and length.
     */
    public static String mid(String strText,int intStart,int intLength) {
        if (intStart < 1) intStart = 1;
        if (strText.length() < intStart) return BLANK;
        int intEnd = intLength + intStart - 1;
        if (intEnd > strText.length()) intEnd = strText.length();
        return strText.substring(intStart-1, intEnd).toString();
    } //mid

    /**
     * Gets the middle of the passed in text based on a start position and end Position.
     * @param strText The text you want to use. Duh!
     * @param intStart The starting point for the data collection. 1 being the first character.
     * @param intEnd The end of the segment you want to extract from strText.
     * @return The middle of the passed in text based on a start position and end position.
     */
    public static String seg(String strText,int intStart,int intEnd) {
        if (intStart < 1) intStart = 1;
        if (strText.length() < intStart) return BLANK;
        if (intEnd > strText.length()) intEnd = strText.length();
        return strText.substring(intStart-1, intEnd).toString();
    } //seg

    /**
     * Searches for a string within another string.
     * @param intStart      The start position of the search. 1 is the first character.
     *                      If -1 is specified, the search will start from the END of
     *                      the string and work from right to left instead, ie.,
     *                      -1 is the rightmost character, -2 is the next to last
     *                      character, etc.
     * @param strText       The string you are search in.
     * @param strSearchFor  The string you are searchin for.
     * @return The position that the search string was found in. 0 means it was not found.
     */
    public static int inStr(int intStart, String strText, String strSearchFor){
        if (strText == null || strSearchFor == null) {
            return 0;
        }
        
        int intLength = strText.length();
        int intFoundPos = 0;

        // 0 means search forward starting from position 1
        if (intStart == 0) intStart = 1;

        if (intStart > 0) {
            // search forward from the start position
            return strText.indexOf(strSearchFor, intStart-1)+1;
        } else {
            // search backward
            // make sure the start position is valid
            intStart = intLength + intStart + 1;
            if (intStart > intLength) {
                intStart = intLength;
            }
            for (int i=intStart; i > 0; i--) {
                intFoundPos = strText.indexOf(strSearchFor, i-1)+1;
                if (intFoundPos > 0) i=0;
            }
            return intFoundPos;
        }
    } //inStr

    /**
     * Gets the left part of a string up to and including intEnd.
     * @param strText The test string you want to get the portion from.
     * @param intEnd  The number of characters you want to get for the start of strText. 1 is
     *                the first character.
     * @return The left part of a string up to and including intEnd.
     */
    public static String left(String strText, int intEnd) {
        if (intEnd > strText.length()) {
            return strText.toString();
        } else {
            return strText.substring(0,intEnd).toString();
        } // end if

    } // left

    /**
     * Gets the next parameter from strText.
     * @param strText The list of parameters separated by a comma delimiter. Each time this
     * method is called a parameter is removed from strText making it smaller. The last parameter
     * does not require to have a comma on the end.
     * @return The next parameter from strText
     */
    public static String parse(StringBuffer strText) {
        String strT = BLANK;
        int intPS = 0;
        String strResult = BLANK;

        intPS = inStr(1,strText.toString(),",");
        if (intPS == 0) {
            strT = strText.toString();
            strText.setLength(0);
            return strT.toString();
        }// end if
        strT = left(strText.toString(),intPS-1);
        strResult = right(strText.toString(),intPS+1);
        strText.setLength(0);
        strText.append(strResult);
        return strT.toString();
    } // parse

    /**
     * Passes back a string of spaces.
     * @param intSpaces The number of spaces for the string.
     * @return a string of spaces.
     */
    public static String space(int intSpaces) {
        byte[] bytT = new byte[intSpaces];
        for (int intT = 0; intT < intSpaces; intT++){
            bytT[intT] = 32;
        } // next
        return new String(bytT);
    } // space

	/**
	 * Passes back a string of characters.
	 * @param intTimes The number of spaces for the string.
	 * @param tChar The character you wish to replicate.
	 * @return a string of spaces.
	 */
	public static String replicate(int intTimes, char tChar) {
		char[] chrT = new char[intTimes];
		for (int intT = 0; intT < intTimes; intT++){
			chrT[intT] = tChar;
		} // next
		return new String(chrT);
	} // space

    /**
     * Converts an Exeption's stack trace into a text string.
     * @param e The Exception to be converted.
     * @param strDelim The delimiter to use between each line of the stack trace.
     * @return A text string containing an Exception's stack trace.
     */
    public static String convertStackTrace(Throwable e,String strDelim) {

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.flush();
        StringBuffer sb = sw.getBuffer();
        StringTokenizer st = new StringTokenizer(new String(sb),"\n");
        sb = new StringBuffer(BLANK);
        while (st.hasMoreTokens()) {
            String strToken = st.nextToken();
            sb.append(strToken);
            sb.append(strDelim+"\r\n");
        } // wend
        return new String(sb);
    } // convertStackTrace

    /**
     * Converts an Exeption's stack trace into a text string.
     * @param e The Exception to be converted.
     * @return A text string containing an Exception's stack trace.
     */
    public static String convertStackTrace(Throwable e) {
        return convertStackTrace(e, BLANK);
    } // convertStackTrace

    /**
     * Replace all occurences of a string with a replacement string.
     *
     * @param data The string to look in.
     * @param lookFor The text string to look for.
     * @param replaceWith The replacement string for the 'lookFor' parameter.
     * @return The new string with all the 'lookFor' strings replaced.
     */
    public static String replaceIn(String data, String lookFor,String replaceWith) {
        int start = data.indexOf(lookFor);

        if (start == -1) return data;

        int lf = lookFor.length();
        char[] origChars = data.toCharArray();
        StringBuffer buffer = new StringBuffer();

        int copyFrom = 0;
        while (start != -1) {
            buffer.append(origChars,copyFrom,start - copyFrom).append(replaceWith);
            copyFrom = start + lf;
            start = data.indexOf(lookFor, copyFrom);
        } // wend

        buffer.append(origChars,copyFrom,origChars.length - copyFrom);
        return buffer.toString();
    } // replaceIn
    /**
     * Replace FIRST occurence of a string with a replacement string.
     *
     * @param data The string to look in.
     * @param lookFor The text string to look for.
     * @param replaceWith The replacement string for the 'lookFor' parameter.
     * @return The new string with all the 'lookFor' strings replaced.
     */
    public static String replaceFirstIn(String data, String lookFor,String replaceWith) {
        int start = data.indexOf(lookFor);

        if (start == -1) return data;

        int lf = lookFor.length();
        char[] origChars = data.toCharArray();
        StringBuffer buffer = new StringBuffer();

        int copyFrom = 0;
        while (start != -1) {
            buffer.append(origChars,copyFrom,start - copyFrom).append(replaceWith);
            copyFrom = start + lf;
            start = -1;// data.indexOf(lookFor, copyFrom);
        } // wend

        buffer.append(origChars,copyFrom,origChars.length - copyFrom);
        return buffer.toString();
    } // replaceIn

    /**
     * Trims the spaces off the end of the string.
     * @param data The string to be trimmed.
     * @return A new string with no spaces on the end.
     */
    public static String rTrim(String data) {
        char[] chars = data.toCharArray();
        for (int intT=chars.length-1;intT>=0;intT--) {
            switch (chars[intT]) {
            case 32:
                break;
            default:
                return data.substring(0,intT+1);
        } // switch
        } // next
        return BLANK;
    } // rTrim

    /**
     * Gets the number of times a string is repeated in another string.
     *
     * @param searchIn The string to search for the repeat in.
     * @param searchFor What you are searching for.
     * @return the number of times a string is repeated in another string.
     */
    public static int repeatCount(String searchIn,String searchFor) {
        int count = 0;
        int size = searchFor.length();
        int c = 0 - size;
        while ((c = searchIn.indexOf(searchFor,c+size)) != -1) {
            count++;
        } // wend
        return count;
    } // getCount

    /**
     * Removes a mask from text.
     * @param text The text you want to remove the mask from.
     * @param mask The mask that is to be removed.
     * @return The text without the imbedded mask.
     */
    public static String removeMask(String text, String mask) {
        if (mask == null) return text;
        StringBuffer sb = new StringBuffer();
        char[] tempMask = mask.toLowerCase().toCharArray();
        int begin = 0;
        int start = getFirstNonX(0,tempMask);
        int length = 0;
        while ( (length = getLength(start,tempMask)) != 0) {
            sb.append(text.substring(begin,start));
            begin = start + 1;
            start = getFirstNonX(start+length,tempMask);
        } // wend
        if (begin != text.length()) sb.append(text.substring(begin,start));

        return sb.toString();
    } // removeMask

    private static int getLength(int start, char[] mask) {
        int len = 0;
        for (int intT=start;intT<mask.length;intT++) {
            if (mask[intT] == 'x') return len;
            len++;
        } // next
        return len;
    } // getLength

    private static int getFirstNonX(int start, char[] mask) {
        for (int intT=start;intT<mask.length;intT++) {
            if (mask[intT] != 'x') return intT;
        } // next
        return mask.length;
    } // getFirstNonX

    /**
     * Increments an alphanumeric string. Left pads the return value 
     *  to the number of digits set by 'max'.  Skips '-'.
     * @param value The value you want to increment.
     * @param max The maximum character size of the result.
     * @return The incremented result.
     */
    public static String increment(String value,int max) {
    	return increment(value, max, true);
    }
    
    /**
     * Increments an alphanumeric string. return values until 
     *  the number of digits is greater than 'max'.  Skips '-'.
     * @param value The value you want to increment.
     * @param max The maximum character size of the result.
     * @param isLpad if true will left pad the resulting string with 
     *  spaces so that the string will be the same length as "max"
     * @return The incremented result.
     */
    public static String increment(String value,int max, 
    							   boolean isLpad) {
        // make sure its the size of the seed
        value = value.trim();
        StringBuffer sb = new StringBuffer(value);

        int len = value.length();
        for (int intT=len;intT>=1;intT--) {
            char[] c = new char[]{sb.charAt(intT-1)};
            if (c[0] != '-') {
                if (c[0] == '9') {
                    sb = sb.replace(intT-1,intT,"0");
                } else if (c[0] == 'Z') {
                    sb = sb.replace(intT-1,intT,"A");
                } else if (c[0] == 'z') {
                    sb = sb.replace(intT-1,intT,"a");
                } else {
                    c[0]++;
                    sb = sb.replace(intT-1,intT, new String(c));
                    return fixup(sb,max, isLpad);
                } // endif
            } // end if
        } // next
        if (sb.substring(0,1).equals("0")) {
            sb = sb.insert(0,"1");
        } else {
            sb = sb.insert(0,"A");
        } // end if
        return fixup(sb, max, isLpad);
    } // increment

    private static String fixup(StringBuffer sb, int max, boolean isLpad) {
        if (sb.length() > max) {
        	throw new RuntimeException("AutoNumber has hit maximum");
        }
        return (isLpad) ? StrU.lPad(sb.toString(), max) : sb.toString();
    } // fixup

    /**
     * Formats a text string with a given mask.
     * @param text The string you want to format.
     * @param mask The mask you want to use.Replaces 'x' or 'X' with characters
     * from text.
     * @return the formatted text.
     */
    public static String format(String text, String mask) {
        char[] textArray = text.toCharArray();
        char[] maskArray = mask.toCharArray();
        char[] masked = new char[maskArray.length];
        int textPointer = 0;

        for (int intT=0;intT<maskArray.length;intT++) {
            if (maskArray[intT] == 'x' || maskArray[intT] == 'X') {
                masked[intT] = getCharacter(textArray,textPointer);
                textPointer++;
            } else {
                // literal
                masked[intT] = maskArray[intT];
            } // end if
        } // next

        return new String(masked);
    } // formatData

    private static char getCharacter(char[] chars, int index) {
        if (index >= chars.length) {
            return ' ';
        } else {
            return chars[index];
        } // end if
    } // getCharacter

    /**
     * Takes any string and returns a string of all the digits inside of it.
     * This is useful for removing certain formatting, like the brackets and
     * the dashes from phone numbers.
     * @param string the String you want to extract the digits from
     * @return a String of only digits, in the order they were encountered.
     */
    public static final String getDigitsOnly( String string ) {
        StringBuffer sb = new StringBuffer();
        int stringLength = string.length();
        for (int i=1; i <= stringLength; i++) {
            if (StrU.inStr(1, "1234567890", StrU.mid(string, i, 1)) > 0) {
                sb.append(StrU.mid(string, i, 1));
            }
        }
        return sb.toString();
    }

    /**
     * Takes any string and all leading, trailing, and middle spaces.
     * This is useful for removing spaces where they would be invalid, like
     * inside of an email or ip address.
     * @param string the String you want to remove the spaces from
     * @return a String without any spaces at all
     */
    public static final String removeAllSpaces( String string ) {
        StringBuffer sb = new StringBuffer();
        int stringLength = string.length();
        for (int i=1; i <= stringLength; i++) {
            if (!StrU.mid(string, i, 1).equals(" ")) {
                sb.append(StrU.mid(string, i, 1));
            }
        }
        return sb.toString();
    }

    /**
     * Takes any string and returns a string of all the digits inside of it,
     * plus any negative symbols or decimals.
     * This is useful for removing certain formatting, like the brackets and
     * commas from currency values, or percent symbols.
     * @param string the String you want to extract the digits from
     * @return a String of only digits and "." and "," symbols,
     * in the order they were encountered.
     */
    public static final String getNumeric( String string ) {
        StringBuffer sb = new StringBuffer();
        int stringLength = string.length();
        for (int i=1; i <= stringLength; i++) {
            if (StrU.inStr(1, "1234567890-.", StrU.mid(string, i, 1)) > 0) {
                sb.append(StrU.mid(string, i, 1));
            }
        }
        return sb.toString();
    }

    /**
     * Passes back your input string duplicated as many times as you want
     * @param intRepetitions The number of duplications of the string.
     * @return Your string repeated intRepetitions times.
     */
    public static String repeatString(String str, int intRepetitions) {
        StringBuffer sb = new StringBuffer(intRepetitions);
        for (int intT = 0; intT < intRepetitions; intT++){
            sb.append(str);
        } // next
        return sb.toString();
    } // space

    /**
     * Converts a string to a boolean value. The string case-insensitive and can be
     * "true","false","y", "yes", "n", or "no". If the string is none of the above or null then the default
     * will be taken.
     * @param str The string that has true,false, y, or n in it.
     * @param def The default value if null or none of the allowed strings is not found.
     */
    public static boolean convertToBoolean(String str, boolean def) {
    	if (str == null) {
    		return def;
    	} else if (str.equalsIgnoreCase("y")) {
    		return true;
    	} else if (str.equalsIgnoreCase("yes")) {
    		return true;
		} else if (str.equalsIgnoreCase("true")) {
			return true;
		} else if (str.equalsIgnoreCase("n")) {
			return false;
		} else if (str.equalsIgnoreCase("no")) {
			return false;
		} else if (str.equalsIgnoreCase("false")) {
			return false;
		} else {
			return def;
		} // end if
    } // convertToBoolean
    


    /**
     * Returns the string value or the default is the string value is null.
     * @param value The string value to check for nulls.
     * @param def The default if value is null.
     * @return the original value or the default if the value is null.
     */
    public static String noNull(String value, String def) {
    	if (value == null) {
    		return def;
    	} else {
    		return value;
    	} // end if
    } // noNull

	/**
	 * Returns the string value or a blank String if value is null.
	 * @param value The string value to check for nulls.
	 * @return the original value or a blank string if the value is null.
	 */
	public static String noNull(String value) {
		return noNull(value, BLANK);
	} // noNull

 

     
 
    /**
     * Formats a phone number.
     * @param obj The unformatted phone number
     * @return The formatted result.
     */
    public static String formatPhone(String storageString) {
        String displayString = BLANK;
        if (storageString.length() == 7) {
            displayString = StrU.left(storageString, 3) + "-" +
                StrU.right(storageString, 4);
        }
        else if (storageString.length() == 10) {
            displayString = "(" + StrU.left(storageString, 3) + ")" +
                StrU.mid(storageString, 4, 3) + "-" +
                StrU.mid(storageString, 7, 4);
        } // end if
        return displayString;
    } // formatPhone
    /**
     * UnFormats a phone number - i.e. removes ()-
     * @param obj The formatted phone number
     * @return The unformatted result.
     */
    public static String unFormatPhone(String storageString) {
    	String displayString = StrU.noNull(storageString).trim();
        displayString = StrU.replaceIn(displayString,"(","");	
        displayString = StrU.replaceIn(displayString,")","");	
        displayString = StrU.replaceIn(displayString,"-","");
        displayString = displayString.trim();
        return displayString;
    } // formatPhone

    

    /**
     * Returns the first character position that is different with 0 representing the
     * first character.
     *
     * @param start Position to start with. 0 represents the first character.
     * @param a The first string to compare.
     * @param b The second string to compare.
     * @return -1 if one of the strings is null. If both strings are equal then will return the
     * string.length.
     */
    public static int compareLargeStrings(int start, String a, String b) {
        if (a == null || b == null) return -1;

        char[] aChars = a.toCharArray();
        char[] bChars = b.toCharArray();

        int min = aChars.length < bChars.length ? aChars.length : bChars.length;
        for (int intT=start;intT<min;intT++) {
            if (aChars[intT] != bChars[intT]) {
                return intT;
            } // end if
        } // next
        return min;

    } // compare

	/**
	 * Checks to see if a string is empty
	 * @param s any string
	 * @return true if s is null or just spaces.  False if it has
	 *  any characters other than spaces.
	 */
	public static boolean isStringEmpty(String s) {
		if (s == null) {
			return true; 
		}
		
		if (s.trim().equals(BLANK)) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * Inserts escape characters into a string that is in XML format
	 * @param data a string in XML format
	 * @return the escaped string
	 */
	public static String escape(String data) {
		// do ampersand so it will not replace valid escape sequences
		data = StrU.replaceIn(data, "&", "&amp;");
		data = StrU.replaceIn(data, "<", "&lt;");
		data = StrU.replaceIn(data, ">", "&gt;");
		data = StrU.replaceIn(data, "\"", "&quot;");
		data = StrU.replaceIn(data, "\'", "&apos;");
		return data;
	}
	public static String escapeApostrophe(String data) {
		data = StrU.replaceIn(data, "\'", "&#39;");
		return data;
	}
	
	
	
	/**
	 * <p>Check if a string (match) is located in anothing string (list) separated by
	 * the separator character.  The match string is compared with each full item in 
	 * the list.  It is case sensitive.  The match string must not contain the separator
	 * character or false will be returned.</p>
	 * 
	 * </p>For example (separator = ','):</p>
	 * <table>
	 * 		<tr><td>match</td><td>list</td><td>returns</td></tr>
	 * 		<tr><td>STRINGA</td><td>STRINGA,STRINGB</td><td>true</td></tr>
	 * 		<tr><td>STRINGA</td><td>STRINGB,STRINGC</td><td>false</td></tr>
	 * 		<tr><td>STRING</td><td>STRINGA,STRINGB</td><td>false</td></tr>
	 * </table>
	 * 
	 * @param match String to search for in the list.
	 * @param list String of list separated items.
	 * @param separator Character used to separatate list items in the list string.
	 * @return True if match is contained in list, else false.
	 * 
	 * @author Jason S
	 */
	public static boolean inList(String match, String list, char separator){
		String columnName;
		int curChar;
	
		if(match == null || list == null || separator == 0) return false;
		String curList = StrU.removeAllSpaces(list);
	
		// ensure the separator character isn't in match
		for(curChar = 0; curChar < match.length(); curChar++){
			if(match.charAt(curChar) == separator)
				return false;
		}
	
		while(curList.length() > 0){
			columnName = "";
		
			// add each character between commas to the column name
			for(curChar = 0; curChar < curList.length() && curList.charAt(curChar) != ','; curChar++){
				columnName += curList.charAt(curChar);
			}
		
			// if found comma, remove an extra character for the comma
			if(curChar < curList.length() && curList.charAt(curChar) == separator)
				curChar++;
		
			// remove the column name from columns
			curList = curList.substring(curChar);
		
			// test for match
			if(match.compareTo(columnName) == 0)
				return true;
		};
	
		return false;
	}
} // end class