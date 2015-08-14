/*
 * Line.java
 *
 * Objects which represent elements to be laid out in a page
 * 
 * (C) 2001 Beartronics Inc.
 * Author: Henry Minsky (hqm@alum.mit.edu)
 *
 * Licensed under terms "Artistic License"
 * http://www.opensource.org/licenses/artistic-license.html
 *
 * $Id: Line.java,v 1.1.1.1 2001/05/27 13:14:08 hqm Exp $
 *
 */

package com.beartronics;

import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import javax.microedition.midlet.*;
import java.io.*;
import java.util.*;



/**
 * A line containing a list of items. 
 *
 * A Line has a baseline y position, as well as left and right margins. The line
 * is rendered from it's bottom y position.
 *
 * The line has a height above the baseline.
 */
public class Line {

    /**
     * Keep a linked list of allocated objects,
     * to ease the job of the GC.
     */
    private static Line poolHead;
    private Line next;

    public Vector items;
    public int ypos = 0;
    public int height = 0;

    private void initializeState () {
	height = 0;
	ypos = 0;
	if (items == null) {
	  items = new Vector();
	} else {
	  items.removeAllElements();
	}
    }

    public Line () {
    }

    public static Line newLine (int y) {
	Line line = get();
	line.initializeState();
	line.ypos = y;
	return line;
    }

    public static synchronized Line get() {
	Line item;
	if (poolHead != null) {
	    item = poolHead;
	    poolHead = item.next;
	}    else {
	    item = new Line();
	}
	return item;
    }

    public static synchronized void put(Line line) {
	line.next = poolHead;
	poolHead = line;
    }


    public void addItem (HtmlItem item) {
	items.addElement(item);
    }

    public int size () {
	return items.size();
    }

    public Object elementAt (int i) {
	return items.elementAt(i);
    }



}
