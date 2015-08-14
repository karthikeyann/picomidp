/*
 * PicoForm.java
 *
 * Represents an HTML Form region from a page. This means
 * basically keeping track of what widgets belong to this form.
 * 
 * (C) 2001 Beartronics Inc.
 * Author: Henry Minsky (hqm@alum.mit.edu)
 *
 * Licensed under terms "Artistic License"
 * http://www.opensource.org/licenses/artistic-license.html
 *
 * $Id: PicoForm.java,v 1.2 2001/07/02 06:32:53 hqm Exp $
 *
 */

package com.beartronics;

import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import javax.microedition.midlet.*;
import java.io.*;
import java.util.*;

/**
 */
public class PicoForm {

    /**
     * Keep a linked list of allocated objects,
     * to ease the job of the GC.
     */
    public static final int GET = 0;
    public static final int POST = 1;
    public int method = GET;
    public String action;

    public Vector widgets;

    public PicoForm () {
	widgets = new Vector();
    }
    public void addWidget (FormWidget widget) {
	widgets.addElement(widget);
    }

    public int getMethod () {
	return method;
    }

    public String getAction () {
	return action;
    }

    public int size () {
	return widgets.size();
    }

    public FormWidget elementAt (int i) {
	return (FormWidget) widgets.elementAt(i);
    }

}
