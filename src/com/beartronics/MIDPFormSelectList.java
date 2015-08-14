/*
 * MIDPFormSelectList.java
 *
 * MIDP Form UI to choose from a SELECT menu list
 * 
 * (C) 2001 Beartronics Inc.
 * Author: Henry Minsky (hqm@alum.mit.edu)
 *
 * Licensed under terms "Artistic License"
 * http://www.opensource.org/licenses/artistic-license.html
 *
 * $Id: MIDPFormSelectList.java,v 1.3 2001/07/02 06:32:53 hqm Exp $
 *
 */

package com.beartronics;

import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import javax.microedition.midlet.*;
import java.io.*;
import java.util.*;


/**
 * Allow a user to choose an entry from a select menu
 *
 */
public class MIDPFormSelectList extends javax.microedition.lcdui.List
    implements CommandListener {
    /**
     *
     */

    PicoBrowser app;
    FormWidget widget;

    private Command cancel = new Command("Cancel", Command.CANCEL, 1);
    private Command done = new Command("Done", Command.OK, 1);


 
    /**
     * Constructor for a SELECT list
     */
    public MIDPFormSelectList (String title, PicoBrowser app) {
	super(title, List.IMPLICIT);
	this.app = app;
        setCommandListener(this) ;
	addCommand(cancel);
	addCommand(done);
    }

    /**
     * Takes a picomidp FormWidget, which can have a list of options.
     * If options is non-null, then add the option's content (text) to
     * this MIDP select widget.
     */
    public void  setWidget (FormWidget w) {
	widget = w;
	if (widget.options != null) {
            //$ to avoid creation of repeated list again and again
            int w_size=widget.options.size();
	    for (int i = this.size(); i < w_size; i++) {
		FormSelectOption option = 
		    (FormSelectOption) (widget.options.elementAt(i));
		String content = option.content;
		this.append(content, null);
	    }
	}
    }

    /**
     * Indicates that a command event has occurred. 
     */
    public void commandAction(Command c, Displayable d)  {
	if (c == cancel) {
	    Display.getDisplay(app).setCurrent(app.browserCanvas());
	} else if (c == done || c == List.SELECT_COMMAND) {
	    int selection = getSelectedIndex();
	    widget.selection = selection;
	    returnToBrowser();
	}
    }

    void returnToBrowser () {
	int selection = getSelectedIndex();
	widget.selection = selection;
	BrowserScreen browser = app.browserCanvas();
	Display.getDisplay(app).setCurrent(browser);
    }

}
