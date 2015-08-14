/*
 * MIDP "Pico" Web Browser Framework
 * 
 * (C) 2001 Beartronics Inc.
 * Author: Henry Minsky (hqm@alum.mit.edu)
 *
 * Licensed under terms "Artistic License"
 * http://www.opensource.org/licenses/artistic-license.html
 *
 * $Id: PicoBrowser.java,v 1.1.1.1 2001/05/27 13:14:08 hqm Exp $
 *
 */

package com.beartronics;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;

public class PicoBrowser extends MIDlet {
    // display manager
    Display display = null;
    
    BrowserScreen browser;

    /**
     * Construct a browser MIDlet
     */
    public PicoBrowser () {
	display = Display.getDisplay(this);
    }

    /**
     * Returns the BrowserScreen associated with this application.
     * This is the object which implements the core of the browser.
     */
    public BrowserScreen browserCanvas () {
	return browser;
    }

    /**
     * Start the MIDlet
     */
    public void startApp () throws MIDletStateChangeException {
	browser = new BrowserScreen(this);
	browser.gotoHomepage();
	display.setCurrent(browser);
    }

    public void pauseApp () {
    }

    public void destroyApp (boolean unconditional) {
	notifyDestroyed();
    }

}

