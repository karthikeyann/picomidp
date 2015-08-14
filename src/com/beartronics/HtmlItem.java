/*
 * HtmlItem.java
 *
 * Objects which represent elements to be laid out in a page
 * 
 * (C) 2001 Beartronics Inc.
 * Author: Henry Minsky (hqm@alum.mit.edu)
 *
 * Licensed under terms "Artistic License"
 * http://www.opensource.org/licenses/artistic-license.html
 *
 * $Id: HtmlItem.java,v 1.1.1.1 2001/05/27 13:14:08 hqm Exp $
 *
 */

package com.beartronics;

import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import javax.microedition.midlet.*;
import java.io.*;
import java.util.*;


/** An HTML content layout item.
 * Currently the only types supported
 * are a run of text or an inline image. 
 *
 */
public class HtmlItem {
    
    /**
     * Keep a linked list of allocated objects,
     * to ease the job of the GC.
     */
    private static HtmlItem poolHead;
    private HtmlItem next;

    /** 
     * These are public instance variables. 
     */
    public static final int TEXT = 0;
    public static final int IMAGE = 1;
    public static final int INPUT = 2;
    public int type = TEXT;

    // If this is an INPUT widget, points to more info
    public FormWidget widget;

    public Line line;
    // xpos,ypos = lower left corner
    public int xpos, ypos, width, height;

    public String text;
    public Font font = Font.getDefaultFont();
    public int color = BrowserScreen.BLACK;

    /** If nonzero, this is a hyperlink. */
    public int linkId = -1;

    /** vertical alignment: BOTTOM | TOP | VCENTER */
    public int alignment = Graphics.BOTTOM;

    public String link = null;
    public Image image;
    
    private HtmlItem () {
    }

    private void initializeState () {
	type = TEXT;
	image = null;
	line = null;
	text = null;

	xpos = 0;
	ypos = 0;
	height = 0;
	width = 0;

	font = Font.getDefaultFont();
	color = BrowserScreen.BLACK;

	linkId = -1;
	link = null;

	alignment = Graphics.BOTTOM;
    }

    public static HtmlItem newHtmlItem (int x, int y, int width, int height, Font f, Line line) {
	HtmlItem item = get();
	item.initializeState();

	item.font = f;
	item.xpos = x;
	item.ypos = y;
	item.line = line;
	item.width = width;
	item.height = height;
	return item;
    }
    
    public static HtmlItem newHtmlItem (String text, int x, int y, int width, int height, Font f, Line line) {
	HtmlItem item = get();
	item.initializeState();

	item.line = line;
	item.font = f;
	item.xpos = x;
	item.ypos = y;
	item.width = width;
	item.height = height;
	item.text = text;
	return item;
    }
    

    public static synchronized HtmlItem get() {
	HtmlItem item;
	if (poolHead != null) {
	    item = poolHead;
	    poolHead = item.next;
	}    else {
	    item = new HtmlItem();
	}
	return item;
    }


    public static synchronized void put(HtmlItem item) {
	item.next = poolHead;
	poolHead = item;
    }

}
