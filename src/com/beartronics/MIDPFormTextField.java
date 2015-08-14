/*
 * BrowserScreen.java
 *
 * MIDP Form UI to enter a text field
 * 
 * (C) 2001 Beartronics Inc.
 * Author: Henry Minsky (hqm@alum.mit.edu)
 *
 * Licensed under terms "Artistic License"
 * http://www.opensource.org/licenses/artistic-license.html
 *
 * $Id: MIDPFormTextField.java,v 1.4 2001/05/28 09:05:01 hqm Exp $
 *
 */

package com.beartronics;

import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import javax.microedition.midlet.*;
import java.io.*;
import java.util.*;


/**
 * Allow a user to enter a single text input field
 *
 */
public class MIDPFormTextField extends javax.microedition.lcdui.Form
    implements CommandListener {
    /**
     *
     */

    PicoBrowser app;
    FormWidget widget;

    private Command cancel = new Command("Cancel", Command.CANCEL, 1);
    private Command done = new Command("Done", Command.OK, 1);


    TextField textField;

    /**
     * This constructor creates new BaseDemo with the given title & creates
     * the standard commands for the demos.
     */
    public MIDPFormTextField (String title, PicoBrowser app) {
        super(title);
	this.app = app;
        setCommandListener(this) ;
	//$ setItemStateListener(new StateChangeListener (done, this));
	addCommand(cancel);
	addCommand(done);

	textField = new TextField("", "", 25, TextField.ANY);
	this.append(textField);
    }

    public String  setWidget (FormWidget w, int maxSize, int constraints) {
        try {
	widget = w;
	textField.setMaxSize(maxSize);
	textField.setString(w.getValue());
	textField.setConstraints(constraints);
        return null;
        }
        catch(Exception e){
            return "Err setWidget";
        }
    }

    /**
     * Indicates that a command event has occurred. 
     */
    public void commandAction(Command c, Displayable d)  {
	if (c == cancel) {
	    Display.getDisplay(app).setCurrent(app.browserCanvas());
	} else if (c == done) {
	    returnToBrowser();
	}
    }

    void returnToBrowser () {
	String newval = textField.getString();
	widget.setValue(newval);
	BrowserScreen browser = app.browserCanvas();
	Display.getDisplay(app).setCurrent(browser);
    }

    /** This is some fancy footwork here. Perhaps too fancy.  What I
     * want to accomplish is that when the user edits the TextField,
     * that as soon as they hit "save", that we return to the browser
     * screen.  In the MIDP "form" model, after entering or modifying
     * text, they are sent back to the Form, which then requires them
     * to enter another command to go back to the browser.  

     *<p> 

     * I would like to use the itemStateChanged() interface to go
     * right back to the browser screen, but for some reason that
     * won't work in the MIDP emulator. It says "SCREEN event when no
     * next?"  (whatever the hell that means).
     *
     *<p>
     *
     * So, I am using the callSerially() interface, which does seem to
     * do the right thing, though it is pretty hard to understand why.
     */
    class StateChangeListener implements ItemStateListener, Runnable {
	Command cmd;
	MIDPFormTextField form;

	StateChangeListener (Command cmd, MIDPFormTextField form) {
	    this.cmd = cmd;
	    this.form = form;
	}
	public void run() {
	    // Send the "done" command
	    form.commandAction(cmd, form);
	}

	public void itemStateChanged (Item item) {
	    Display d = Display.getDisplay(form.app);
	    // form.commandAction(cmd, form);
	    d.callSerially(this);
	}
    }
}
