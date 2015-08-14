/*
 * FormSelectOption.java
 *
 * HTML Form OPTION item
 * 
 * (C) 2001 Beartronics Inc.
 * Author: Henry Minsky (hqm@alum.mit.edu)
 *
 * Licensed under terms "Artistic License"
 * http://www.opensource.org/licenses/artistic-license.html
 *
 * $Id: FormSelectOption.java,v 1.2 2001/05/27 17:27:18 hqm Exp $
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
public class FormSelectOption {
    
    public String   value;
    public String   content;
    public boolean  selected = false;

    public FormSelectOption (String value, String content) {
	this.value = value;
	this.content = content;
    }
}


