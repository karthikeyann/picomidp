/*
 * FormWidget.java
 *
 * HTML Form
 * 
 * (C) 2001 Beartronics Inc.
 * Author: Henry Minsky (hqm@alum.mit.edu)
 *
 * Licensed under terms "Artistic License"
 * http://www.opensource.org/licenses/artistic-license.html
 *
 * $Id: FormWidget.java,v 1.2 2001/05/27 17:04:24 hqm Exp $
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
public class FormWidget {
    
    public static final int TEXT     = 0;
    public static final int PASSWORD = 1;
    public static final int CHECKBOX = 2;
    public static final int RADIO    = 3;
    public static final int HIDDEN   = 4;
    public static final int IMAGE    = 5;
    public static final int SUBMIT   = 6;
    public static final int RESET    = 7;
    public static final int TEXTAREA = 8;
    public static final int SELECT   = 9;
    public static final int OPTION   = 10;

    public PicoForm form;
    public boolean  checked = false;
    public boolean  multiple = false;
    public String   name;
    public String   value;
    public String   src;
    public int      maxlength;
    public int      size;
    public int      type;
    // For SELECT lists, the selected option, or -1 if none selected
    public int      selection;

    public Vector options;

    public FormWidget (PicoForm form, int type) {
	this.form = form;
	this.type = type;
    }

    public PicoForm getForm () {
	return form;
    }
    public void setValue (String s) {
	value = s;
    }

    public String getValue () {
	return value;
    }

    public String getName () {
	return name;
    }
}
