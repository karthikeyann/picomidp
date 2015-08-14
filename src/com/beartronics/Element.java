/*
 * Element.java
 *
 * Objects which represent elements parsed from HTML
 * 
 * (C) 2001 Beartronics Inc.
 * Author: Henry Minsky (hqm@alum.mit.edu)
 *
 * Licensed under terms "Artistic License"
 * http://www.opensource.org/licenses/artistic-license.html
 *
 *
 * $Id: Element.java,v 1.2 2001/05/29 10:34:45 hqm Exp $
 */

package com.beartronics;

import java.io.*;
import java.util.*;


/**
 */
public class Element {

    public static final int TEXT = 0; 
    public static final int TAG  = 1; 
    public Vector children;

    public static final String ALPHA_CHARS =
	"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    /**
     * Element type: TEXT for a run of plain text, or TAG for a tag.
     */
    public int type = TEXT;

    boolean leadingSlash = false;
    public boolean isClosingTag () {
	return leadingSlash;
    }

    public String toString () {
	return "elt "+((type == TEXT) ? "TEXT" : "TAG" )+": "+value;

    }
    /**
     * The tag name or text data. 
     */
    public String tag;

    /**
     * The raw substring which contains the attributes or text content i.e., 
     * 'foo=3 bar="baz baz baz"'
     */
    public String value = "";

    public Element () {
    }

    public Element (String tag, String value) {
	this.value = value;
	this.tag = tag;
    }


    public Element (int type, String tag, String value) {
	this.type = type;
	this.value = value;
	this.tag = tag;
    }

    /**
     * Reads the next Element from a text string. Creates either a TAG or TEXT
     * type element. Returns the last position of the cursor in the text string.
     *
     * <p>
     * Note, this will not handle the presence of (illegal) isolated &lt; and &gt; chars
     * in an HTML document very well. I think the code should ignore them and treat
     * them as literals. The code below needs to be rewritten to do one character of
     * lookahead to see if a '&lt;' has whitespace after it, and if so,
     * to ignore it. 

     * @param elt an Element to fill in
     * @param text the HTML raw text string
     * @param idx pointer to the current offset in text
     *
     * @return last character position after element was read
     */
    int readNextElement (String text, int idx) {
	int maxidx = text.length();

	char ch = text.charAt(idx);
	char nextchar;

	// one character lookahead, watch out for end of buffer.
	if (idx == (maxidx-1)) {
	    nextchar = ' ';
	} else {
	    nextchar = text.charAt(idx+1);
	}
	// Look for a tag in <>'s
	if ((ch == '<') && (!isWhitespace(nextchar))) {
	    idx++;

	    // look for end of tag
	    int tagend = text.indexOf('>', idx);

	    // If we've got a badly formed tag at the end of the buffer, 
	    // just try to do the best we can.
	    if (tagend == -1) {
		tagend = maxidx-1;
	    }

	    // Grab the tag name
	    
	    // first whitespace after the tag.
	    int whitespace = text.indexOf(' ', idx);

	    // index of the end of the tag name
	    int eltEnd;

	    // start of the attributes text
	    int attrStart;

	    if (whitespace >= 0) {
		eltEnd = (whitespace < tagend ? whitespace : tagend);
	    } else {
		eltEnd = tagend;
	    }
		
	    if (whitespace >= 0) {
		attrStart = (whitespace+1 < tagend ? whitespace+1 : tagend);
	    } else {
		attrStart = tagend;
	    }

	    // We need to preserve case of tags for XHTML.

	    // Is it a closing tag?
	    if (text.charAt(idx) == '/') {
		leadingSlash = true;
		idx++;
	    } else {
		leadingSlash = false;
	    }

	    String tagName = text.substring(idx, eltEnd);
	    String attributes = text.substring(attrStart, tagend);

	    this.type = Element.TAG;
	    this.tag = tagName;
	    this.value = attributes;

	    if ((tagend + 1) >= maxidx) {
		return maxidx;
	    } else {
		return tagend + 1;
	    }
	} else {
	    // Look for next occurence of '<' marking a new tag. 

	    // Clear the string buffer
	    buf.delete(0, buf.length());

	    while (idx < maxidx) {
		ch = text.charAt(idx++);

		// Look for HTML entities of the form &xxx;
		if (ch == '&') {
		    // Numeric entity?
		    if (text.startsWith("#", idx)) {
			idx++;
			// &#65;
			char val = '!';
			// search for the next non-numeric char
			int term;
			char cterm = 0;
			for (term = idx; term < maxidx; term++) {
			    cterm = text.charAt(term);
			    if (!Character.isDigit(cterm)) {
				break;
			    }
			}
			try {
			    val = (char) (Integer.parseInt(text.substring(idx,term)));
			} catch (Exception e) {
			}
			buf.append(val);
			if (cterm == ';') {
			    idx = term+1;
			} else {
			    idx = term;
			}
		    } else {
			// Symbolic entity (&nbsp; &gt; etc...)
			// People are sloppy, and will forget to terminate the entities
			// with a ';', so let's be nice and also terminate at the first whitespace.
			char cterm = 0;
			int term;
			// Scan for non-alpha char
			for (term = idx; term < maxidx; term++) {
			    cterm = text.charAt(term);
			    if ((ALPHA_CHARS.indexOf(cterm)) == -1) {
				break;
			    }
			}

			if ("AMP".regionMatches(true, 0, text, idx, 3)) {
			    buf.append("&");
			} else if ("LT".regionMatches(true, 0, text, idx, 2)) {
			    buf.append("<");
			} else if ("GT".regionMatches(true, 0, text, idx, 2)) {
			    buf.append(">");
			} else if ("QUOT".regionMatches(true, 0, text, idx, 4)) {
			    buf.append("\"");
			} else if ("COPY".regionMatches(true, 0, text, idx, 4)) {
			    buf.append((char)169);
			} else if ("NBSP".regionMatches(true, 0, text, idx, 4)) {
			    // We should handle nonbreaking space differently, but we'll
			    // just treat it is regular space for now. 
			    buf.append(" ");
			}
			// Move on past end of entity

			// If it was properly terminated with a semicolon, eat the terminator
			if (cterm == ';') {
			    idx = term+1;
			} else {
			    idx = term;
			}
		    }
		} else if (ch == '<') {
			if (idx == (maxidx-1)) {
			    nextchar = ' ';
			} else {
			    nextchar = text.charAt(idx);
			}

			if (!isWhitespace(nextchar)) {
			    // A real tag is starting we're done, return this element,
			    // back the index up one to where the '<' is.
			    idx--;
			    break;
			}
		    } else {
		    // add to the buffer
		    buf.append(ch);
		}
	    }

	    this.type = Element.TEXT;
	    this.tag = null;
	    this.value = buf.toString();
	    return idx;
	}
    }

    /* state machine for parsing attributes */
      
    private static final int ATTR_WHITESPACE  = 0;
    private static final int ATTR_NAME        = 1;
    private static final int ATTR_EQUALS      = 2;
    private static final int VALUE_WHITESPACE = 3;
    private static final int ATTR_VALUE       = 4;
    private static final int ATTR_VALUE_QUOTE = 5;
    private static final int CHECK_MATCH      = 6;

    // These are not threadsafe now. 
    private static StringBuffer attrname = new StringBuffer();
    private static StringBuffer attrvalue = new StringBuffer();

    // Reusable string buffer (does this really help the GC?)
    private static StringBuffer buf = new StringBuffer();

    /** Gets the attribute value of a tag.  
     *
     * @param attribute the name of the attribute
     * @return returns a value string or null if attribute not found.
     * If the attribute is present but with no value, then the empty string "" is returned.
     */

    public String getAttributeVal (String target, boolean ignoreCase) {

	if (value == null) {
	    return null;
	}

	boolean valueFound = false;
	int state = ATTR_WHITESPACE;

	// reinitialize result buffer
	attrvalue.delete(0,attrvalue.length());
	attrname.delete(0,attrname.length());
	int idx = 0;
	int len = value.length();
	char ch;

	while (idx < len) {
	    switch (state) {
	    case ATTR_WHITESPACE:
		// We're looking for the next attribute name.
		// Scan until we find the first non-whitespace char.
		ch = value.charAt(idx++);
		if (isWhitespace(ch)) {
		    continue;
		} else {
		    // start collecting the attribute name
		    attrname.append(ch);
		    // clear any preexisting value
		    attrvalue.delete(0,attrvalue.length());
		    state = ATTR_NAME;
		}
		break;
	    case ATTR_NAME: // gather attribute name
		// look for the first '=' or whitespace
		ch = value.charAt(idx++);
		if (isWhitespace(ch)) {
		    // whitespace found, look if an '=' occurs next
		    state = ATTR_EQUALS;
		} else if (ch == '=') {
		    state = VALUE_WHITESPACE;
		} else {
		    attrname.append(ch);
		}
		break;
	    case ATTR_EQUALS:
		// look for the '=' that may come after attribute name
		ch = value.charAt(idx++);
		if (ch == '=') {
		    state = VALUE_WHITESPACE;
		} else if (!isWhitespace(ch)) {
		    // We found a non-whitespace, non-'=' character.
		    // So there is no value for this attribute. We
		    // should move on to check if this current
		    // attribute matches the desired target attribute.

		    // Unread this char, it will start the next attribute name
		    idx--;

		    state = CHECK_MATCH;
		}
		break;
	    case VALUE_WHITESPACE:
		// Scan until non whitespace found.
		ch = value.charAt(idx++);
		if (isWhitespace(ch)) {
		    continue;
		} else {
		    // Start collecting the attribute value.
		    // If this starts with a double-quote, then
		    // go to state ATTR_VALUE_QUOTE
		    if (ch == '"' || ch=='\'') {
			state = ATTR_VALUE_QUOTE;
			//			System.out.println("VALUE_WHITESPACE going to ATTR_VALUE_QUOTE");
		    } else {
			attrvalue.append(ch);
			state = ATTR_VALUE;
			//			System.out.println("VALUE_WHITESPACE going to ATTR_VALUE");
		    }
		}
		break;
	    case ATTR_VALUE:
		// Reading an unquoted attribute value. Look
		// for whitespace to terminate.
		ch = value.charAt(idx++);
		if (isWhitespace(ch)) {
		    state = CHECK_MATCH;
		} else {
		    attrvalue.append(ch);
		}
		break;
	    case ATTR_VALUE_QUOTE:
		ch = value.charAt(idx++);
		// Reading a quoted value, look for double quote
		// to terminate.
		if (ch == '"' || ch=='\'') {
		    state = CHECK_MATCH;
		} else {
		    attrvalue.append(ch);
		}
		break;
	    case CHECK_MATCH:
		// Check if the last attribute name that we read
		// matches the desired attribute name.

		if ((target.length() == attrname.length()) 
		    && (target.regionMatches(ignoreCase,
					 0,
					 attrname.toString(),
					 0,
					 target.length()))) {
		    return attrvalue.toString();

		} else {
		    // no match, try next attribute
		    attrvalue.delete(0,attrvalue.length());
		    attrname.delete(0,attrname.length());
		    state = ATTR_WHITESPACE;
		}
		break;
	    }
	}

	if ((target.length() == attrname.length()) 
	    && (stringMatch(ignoreCase, target, attrname.toString()))) {
	    return attrvalue.toString();
	} else {
	    return null;
	}
    }

    boolean isWhitespace (char ch) {
	return (ch == ' ' || ch == '\r' || ch == '\n' || ch == '\t');
    }


    /** Case insensitive string match 
     * @param ignoreCase if true, ignore case
     * @param s1
     * @param s2
     */
    public static boolean stringMatch (boolean ignoreCase, String s1, String s2) {
	int cmplen = Math.max(s1.length(), s2.length());
	if ((s1 == null) || (s2 == null)) {
	    return false;
	}
	return s1.regionMatches(true, 0, s2, 0, cmplen);
    }

    /**
     * "http://foo.bar.com/baz/blah.html" => "http://foo.bar.com"
     * <p>
     * "resource://baz/blah.html" => "resource://"
     * <br>
     * "resource://baz/blah.html" => "resource://"
     *
     *
     */

    public static  String protocolAndHostOf (String url) {
	if ("resource://".regionMatches(true, 0, url, 0, 11)) {
	    return "resource://";
	} else if ("http://".regionMatches(true, 0, url, 0, 7)) {
	    int hostStart = url.indexOf("//");
	    // figure out what error checking to do here
	    hostStart+=2;
	    // look for next '/'. If none, assume rest of string is hostname
	    int hostEnd = url.indexOf("/", hostStart);
	    if (hostEnd != -1) {
		return url.substring(0, hostEnd);
	    } else {
		return url;
	    }
	} else {
	    // unsupported protocol
	    return url;
	}
    }

    /**
     * "http://foo.bar.com/baz/boo/blah.html" => "http://foo.bar.com/baz/boo"
     * <br>
     " "http://foo.bar.com" => "http://foo.bar.com"
     * <br>
     * "resource://baz/blah.html" => "resource://baz"
     *<br>
     * "resource://blah.html" => "resource://"
     */

    public static String protocolAndPathOf (String url) {
	// 1. Look for query args, or end of string.
	// 2. from there, scan backward for first '/', 
	// 3. cut the string there.
	// figure out what error checking to do here

	int end = url.indexOf('?');
	if (end == -1) {
	    end = url.length()-1;
	}
	
	int hostStart = url.indexOf("//");
	// figure out what error checking to do here
	hostStart+=2;

	int lastSlash = url.lastIndexOf('/', end);

	// RESOURCE urls have no host portion, so return everything between
	// the "resource://" and last slash, if it exists.
	if ("resource://".regionMatches(true, 0, url, 0, 11)) {
	    if ((lastSlash == -1) || (lastSlash <= hostStart)) {
		return "resource://";
	    } else {
		return url.substring(0, lastSlash);
	    }
	} else {
	    if ((lastSlash == -1) || (lastSlash <= hostStart)) {
		return url;
	    } else {
		return url.substring(0, lastSlash);
	    }
	}
    }
}
