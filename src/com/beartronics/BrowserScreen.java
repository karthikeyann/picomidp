/*
 * BrowserScreen.java
 *
 * MIDP "Pico" Web Browser Framework
 * 
 * (C) 2001 Beartronics Inc.
 * Author: Henry Minsky (hqm@alum.mit.edu)
 *
 * Licensed under terms "Artistic License"
 * http://www.opensource.org/licenses/artistic-license.html
 *
 * $Id: BrowserScreen.java,v 1.16 2001/07/02 06:46:37 hqm Exp $
 *
 */

package com.beartronics;

import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import javax.microedition.midlet.*;
import java.io.*;
import java.util.*;

public class BrowserScreen extends Canvas implements CommandListener {

    public static final String HOMEPAGE_URL = "resource://index.html";

    PicoBrowser app;
    String httpMimeType;

    /** Maximum number of levels to follow HTTP redirects */
    public final static int MAX_REDIRECTS = 5;

    /** Size (pixels) to pad display of text intput fields */
    public final static int INPUT_FIELD_BORDER = 4;

    /** The Sun emulator is not very precise about string widths, so
     * we need to do something to adjust the right margin.
     */
    public final static int MARGIN_FUDGE = 0;

    public final static int RED=0x00FF0000;
    public final static int GREEN=0x0000FF00;
    public final static int BLUE=0x000000FF;
    public final static int WHITE=0x00FFFFFF;
    public final static int BLACK=0x00000000;
    public final static int GREY=0X00808080;
    public final static int YELLOW=0X00808080;

    public static int SELECTED_LINK_COLOR = RED;


    /****************************************************************/
    /**
     * The list of all HTML components in the current document
     */
    Vector items = new Vector();
    Vector lines = new Vector();
    
    /** Utility buffer */
    StringBuffer buf = new StringBuffer();

    static final char BULLET_CHAR = '\u00B0';

    /** x position of current "viewpoint" into current page. */
    int        nPosX;

    /** y position of current "viewpoint" into current page. */
    int        nPosY;
    
    /** drawable screen width */
    int        nScreenWidth;

    /** drawable screen height */
    int        nScreenHeight;

    /** A unique id for a group of selected hyperlink items */
    int hyperlinkId = 0;
    int selectedHyperlink = -1;

    /** The maximum Y height of the virtual page. */
    int maxPageHeight = 0;

    /** The screen left margin */
    int leftMargin;

    /** The screen right margin */
    int rightMargin;

    /** The screen top margin */
    int topMargin = 0;
    /** Interline padding */
    int lineSpacing = 0;
    /** How much to indext UL or BLOCKQUOTE */
    int xIndent = 10;

    public static final int CHECKBOX_WIDTH = 10;
    public static final int CHECKBOX_HEIGHT = 10;
    public static final int DEFAULT_BASELINE = 4;
    
    /** If true, try to wrap words at whitespace or punctuation. If false,
     * wrap words at right margin, regardless of whitespace.
     */
    boolean wordWrap = false;

    /** The URL we are currently pointing to. 
     * This can be overriden by the BASE tag in a document.
     */
    String currentURL = HOMEPAGE_URL;
    String currentDocumentBase = null;



    // command
    static final Command menu = new Command("Menu", Command.BACK, 1);
    static final Command back = new Command("back", Command.BACK, 1);
    static final Command confirm = new Command("confirm", Command.OK, 1);
    static final Command cancel = new Command("cancel", Command.BACK, 1);
    /**
     * The current x position as we are placing items during layout.
     */
    int xpos;

    /**
     * The current y position as we are placing items during layout.
     */
    int ypos = 0;

    int maxHeight = 0;

    /**
     * The maximum visible width of a line of items on the screen.
     * Effectively the right margin.
     */
    int lineWidth;

    // Start the initial Line
    Line currentLine;
    HtmlItem currentItem = null; 

    BrowserScreen(PicoBrowser app) {
	this.app = app;

	nScreenWidth = getWidth();
	nScreenHeight = getHeight(); //$ -20 for lower key function indication
	lineWidth = nScreenWidth;

	leftMargin = 0;
	rightMargin = lineWidth - MARGIN_FUDGE;
        
	nPosX = 0;
	nPosY = 0;

	int xpos = leftMargin;
	int ypos = topMargin;

	this.addCommand(menu);
        this.addCommand(back);
        
	this.setCommandListener(this);

	// set up form handler Forms
	textInput = new MIDPFormTextField("Text Input", app);
	selectList = new MIDPFormSelectList("Select Input", app);;
    }


    public void commandAction (Command c, Displayable d) {
	if (c == menu) { //$
	    menu();
	}
	else if (c == back) {
            prevLink();
	} else if(c== confirm) {
            app.destroyApp(true);
        }
        else if(c == cancel){
            Display.getDisplay(app).setCurrent(this);
            //this.setCommandListener(this);
	}
    }

    public void menu(){
        //$ write menu funtion with "Enter url" and "Exit"
        Alert a = new Alert("Exit?", "Want to exit?" , null, AlertType.WARNING);
        a.addCommand(cancel);
        a.addCommand(confirm);
        a.setTimeout(Alert.FOREVER);
	Display.getDisplay(app).setCurrent(a);
        a.setCommandListener(this);
    }

    public void pageUp () {
	if (nPosY < nScreenHeight) {
	    nPosY = 0;
	} else {
	    nPosY -= nScreenHeight;
	}
    }

    public void pageDown () {
	if (nPosY < (maxPageHeight - nScreenHeight)) {
	    nPosY += nScreenHeight;
	}
    }

    /*
     * Debug printing. If developing on a real device, this could log to a
     * remote network server instead of printing locally. 
     */
    void debugPrint (String s) {
	System.out.println(s);
    }

    /**
     *  Tell browser to load data from URL.
     * Valid protocols are "http://", "resource://" 
     * and "local://" for servlets.
     */

    public void getURL (String url) {
	currentURL = url;
	currentPageText = getURLData(url);
        //$ include own line to parse the google proxy
	// Parse and lay out the text
	setText(currentPageText);
    }

    /**
     * submit a data string via HTTP Post, and display
     * returned data.
     */
    public void postURL (String url, String content) {
	currentURL = url;
	currentPageText = postURLData(url, content);
	// Parse and lay out the text
	setText(currentPageText);
    }



    /**
     * Sends the browser to "home". 
     * This should ultimately be a user-specified "prefrences" parameter in 
     * persistent scratchpad/
     */
    public void gotoHomepage () {
	getURL(HOMEPAGE_URL);
    }

    /*****************************************************************/

    /**
     * Start building a new Line of items. */
    Line addNewLine (int ypos, LayoutState ls) {
	Line l = Line.newLine(ypos);
	lines.addElement(l);
	currentLine = l;

	ls.runningWhitespace = true;
	return l;
    }

    /**
     * Is this character whitespace?
     */
    boolean isWhitespace (char ch) {
	return (ch == ' ' || ch == '\r' || ch == '\n' || ch == '\t');
    }

    class LayoutState {

	/** State variables */
	
	boolean isBold = false;
	boolean isItalic = false;

	boolean isLink = false;
	boolean isImage = false;

	int color = BLACK;
	int prevColor = color;

	// The x origin of the current run of text
	int height=0;
	int maxDescent = 0;

	int leftMargin = 0;
	int rightMargin = 0;

	boolean preformat = false; // preserve whitespace and newlines
	boolean center = false; // display text centered on the screen?

	// In HTML, whitespace is not significant, or rather, once the
	// first whitespace is encountered, subsequent whitespace is
	// ignored until the first non-whitespace printing character
	// is encountered. These state vars keep track of whether we
	// are within a run of whitespace.
	
	boolean runningWhitespace = false;

	boolean charAdded = false;
	boolean htmlParagraph = false; // was the last newline a paragraph break?
        
	String linkTarget = null;

	// Default font style
	int fontFace  = Font.FACE_SYSTEM;
	int fontStyle = Font.STYLE_PLAIN;
	int fontSize  = Font.SIZE_MEDIUM;

	PicoForm form = null;
	// The current SELECT widget, if any.
	FormWidget selectWidget = null;

	Font font = Font.getFont(fontFace, fontStyle, fontSize);

	LayoutState (BrowserScreen b) {
	    this.rightMargin = b.rightMargin;
	}
    }

    public void disposeLayout () {
	// Put all items back in the "free pool", as a 
	// efficiency hack to allow reuse instead of GC
	for (int i = 0; i < items.size(); i++) {
	    HtmlItem item = (HtmlItem) items.elementAt(i);
	    HtmlItem.put(item);
	}

	for (int i = 0; i < lines.size(); i++) {
	    Line line = (Line) lines.elementAt(i);
	    Line.put(line);
	}
	items.removeAllElements();
	lines.removeAllElements();
    }

    Element bulletElement = new Element(Element.TEXT, null, null);
    Element horizontalRuleElement = new Element(Element.TEXT, null, null);

    /**
     * Set the HTML text of the current page. This will parse the HTML
     * into HtmlItem objects, and assign them layout positions on a 
     * virtual page.
     */
    public void setText (String text) {
	// in cHTML, case in tag names is not significant
	boolean ignoreCase = true;

	selectedHyperlink = -1;
	hyperlinkId = 0;
	nPosY = 0;
	ypos = 0;
	xpos = 0;
	maxHeight = 0;


	// A document BASE tag will override this.
	currentDocumentBase = currentURL;

	if (text == null) {
	    text = "<Error: null text>";
	}
	LayoutState ls = new LayoutState(this);

	disposeLayout();
	addNewLine(ypos, ls);

	// Make an element used to layout bullet items
	bulletElement.readNextElement("o ", 0);

	StringBuffer underscores = new StringBuffer();
	int cw = ls.font.charWidth('_');
	for (int i = 0; i < (lineWidth / cw); i++) {
	    underscores.append("_");
	}
	horizontalRuleElement.readNextElement(underscores.toString(), 0);

	// Parse the text into contigous runs.

	// An Element object which holds the state of the currently parsed
	// HTML tag or text run. 
	//
	// Eventually we will build a parse tree of these, but for now, 
	// we just reuse a single one as a struct to save memory. 
	Element elt = new Element(Element.TEXT, null, null);

	// Start the initial item.

	//debugPrint("setText: "+text);

	// current index into raw html source
	int idx = 0;
	// end of html source
	int maxidx = text.length();

	/*
	  Parse sequential runs of text or tag from the html source into an Element.
	  
	  Use the Element to typeset into place HtmlItems into lines

	  - eventually, implement a stack of LayoutState, for handling more
	  complex nesting of tags.

	*/

	while (idx < maxidx) {

	    //System.out.print(" "+idx);
	    idx = elt.readNextElement(text, idx);
	    
	    if (elt.type == Element.TEXT) {

		layoutTextElement(elt, ls, wordWrap);

	    } else if (elt.type == Element.TAG) {
		int taglength = elt.tag.length();
		// MODIFY to call SUBROUTINE for each tag, which creates an HTMLITEM,
		// frobs layout state.
		
		if (Element.stringMatch(ignoreCase, "BR", elt.tag)) {
		    forcedLineBreak(ls);
		} else if (Element.stringMatch(ignoreCase, "P", elt.tag)) {
		    forcedLineBreak(ls);
		    forcedLineBreak(ls);
		} else if (Element.stringMatch(ignoreCase, "STRONG", elt.tag)
			   || Element.stringMatch(ignoreCase, "B", elt.tag)) {
		    // bold
		    if (elt.isClosingTag()) {
			ls.isBold = false;
		    } else {
			ls.isBold = true;
		    }
		} else if (Element.stringMatch(ignoreCase, "A", elt.tag)) {
		    // hyperlink
		    if (elt.isClosingTag()) {
			ls.isLink = false;
			// increment to generate new unique ID for future links
			hyperlinkId++;
		    } else {
			// Look for the HREF target value. 
			ls.isLink = true;
			ls.linkTarget = elt.getAttributeVal("href", true);
			//debugPrint("A HREF="+ls.linkTarget);
		    }
		} else if (((elt.tag.charAt(0) == 'h') || (elt.tag.charAt(0) == 'H'))
			   && (taglength == 2) 
			   && ("123456".indexOf(elt.tag.charAt(1)) != -1)) {
		    if (elt.isClosingTag()) {
			ls.isBold = false;
		    } else {
			ls.isBold = true;
		    }
		    lineBreakIfNeeded(ls);
		} else if (Element.stringMatch(ignoreCase, "PRE", elt.tag)) {
		    if (elt.isClosingTag()) {
			ls.preformat = false;
			lineBreakIfNeeded(ls);
			ls.fontFace = Font.FACE_SYSTEM;
			ls.font = Font.getFont(ls.fontFace, ls.fontStyle, ls.fontSize);
		    } else {
			forcedLineBreak(ls);
			ls.fontFace = Font.FACE_MONOSPACE;
			ls.font = Font.getFont(ls.fontFace, ls.fontStyle, ls.fontSize);
			ls.preformat = true;
		    }
		} else if (Element.stringMatch(ignoreCase, "CENTER", elt.tag)) {
		    if (elt.isClosingTag()) {
			lineBreakIfNeeded(ls);
			ls.center = false;

		    } else {
			lineBreakIfNeeded(ls);
			ls.center = true;
		    }
		} else if (Element.stringMatch(ignoreCase, "DIV", elt.tag)) {
		    if (elt.isClosingTag()) {
			lineBreakIfNeeded(ls);
			ls.center = false;
		    } else {
			lineBreakIfNeeded(ls);
			String alignment = elt.getAttributeVal("align", true);
			if (alignment != null && (alignment.toUpperCase().equals("CENTER"))) {
			    ls.center = true;
			}
		    }
		} else if (Element.stringMatch(ignoreCase, "UL", elt.tag)) {
		    if (elt.isClosingTag()) {
			ls.leftMargin = Math.max(ls.leftMargin - xIndent, 0);
			lineBreakIfNeeded(ls);
		    } else {
			lineBreakIfNeeded(ls);
			ls.leftMargin = Math.min(ls.leftMargin + xIndent, ls.rightMargin - xIndent);
		    }
		} else if (Element.stringMatch(ignoreCase, "BLOCKQUOTE", elt.tag)) {
		    if (elt.isClosingTag()) {
			ls.leftMargin = Math.max(ls.leftMargin - xIndent, 0);
			ls.rightMargin = Math.min(ls.rightMargin + xIndent, rightMargin);
			lineBreakIfNeeded(ls);
		    } else {
			ls.leftMargin = Math.min(ls.leftMargin + xIndent, ls.rightMargin - xIndent);
			ls.rightMargin = Math.max(ls.rightMargin - xIndent, ls.leftMargin + xIndent);
			lineBreakIfNeeded(ls);

		    }
		} else if (Element.stringMatch(ignoreCase, "LI", elt.tag)) { 
		    if (!elt.isClosingTag()) { 
			lineBreakIfNeeded(ls); 
			// Force a "o" item into the line 
			layoutTextElement(bulletElement, ls, wordWrap); 
		    } 
		} else if (Element.stringMatch(ignoreCase, "HR", elt.tag)) {
		    lineBreakIfNeeded(ls);
		    // Force a string of "_" chars into a line
		    layoutTextElement(horizontalRuleElement, ls, wordWrap);
		    forcedLineBreak(ls);
		} else if (Element.stringMatch(ignoreCase, "DD", elt.tag)) {
		    // This is a very hacked up way to make DD cause an indent. 
		    // We push the left margin in, until we hit either a closing DD or
		    // a /DL or DT tag.
		    lineBreakIfNeeded(ls);
		    if (elt.isClosingTag()) {
			ls.leftMargin = Math.max(ls.leftMargin - xIndent, 0);
			lineBreakIfNeeded(ls);
		    } else {
			ls.leftMargin = Math.min(ls.leftMargin + xIndent, ls.rightMargin - xIndent);
			lineBreakIfNeeded(ls);
		    }
		} else if (Element.stringMatch(ignoreCase, "DL", elt.tag)) {
		    if (elt.isClosingTag()) {
			ls.leftMargin = Math.max(ls.leftMargin - xIndent, 0);
			lineBreakIfNeeded(ls);
		    }		    
		} else if (Element.stringMatch(ignoreCase, "DT", elt.tag)) {
		    ls.leftMargin = Math.max(ls.leftMargin - xIndent, 0);
		    lineBreakIfNeeded(ls);
		} else if (Element.stringMatch(ignoreCase, "FONT", elt.tag)) {
		    if (elt.isClosingTag()) {
			ls.color = ls.prevColor;
		    } else {
			// Look for the COLOR target value. 
			// Double quotes required: <FONT color="#RRGGBB">
			String colorVal = elt.getAttributeVal("color", true);
			if (colorVal != null) {
			    int r,g,b;
			    r = Integer.parseInt(colorVal.substring(1,3), 16);
			    g = Integer.parseInt(colorVal.substring(3,5), 16);
			    b = Integer.parseInt(colorVal.substring(5,7), 16);
			    ls.prevColor = ls.color;
			    ls.color = ( (r<<16) | (g<<8) | b );
			}
		    }
		} else if (Element.stringMatch(ignoreCase, "IMG", elt.tag)) {
		    layoutImageElement(elt, ls);
		} else if (Element.stringMatch(ignoreCase, "BASE", elt.tag)) {
		    String href = elt.getAttributeVal("href", true);
		    if ((href != null) && !(href.equals(""))) {
			currentDocumentBase = href;
		    }
		} else if (Element.stringMatch(ignoreCase, "TITLE", elt.tag)) {
		    // Search for closing /TITLE tag, discard all input up to there.
		    // We should actually save the title someplace, for use in
		    // a bookmarks list.

		    if (!elt.isClosingTag()) {
			while (idx < maxidx) {
			    idx = elt.readNextElement(text, idx);
			    if ((elt.type == Element.TAG) 
				&& (Element.stringMatch(ignoreCase, "TITLE", elt.tag))
				&& elt.isClosingTag()) {
				break;
			    }
			}
		    }
		} else if (Element.stringMatch(ignoreCase, "I", elt.tag)
			   || Element.stringMatch(ignoreCase, "EM", elt.tag)) {
		    // italic
		    if (elt.isClosingTag()) {
			ls.isItalic = false;
		    } else {
			ls.isItalic = true;
		    }
		} else if (Element.stringMatch(ignoreCase, "FORM", elt.tag)) {
		    // Only handles one form per page for now
		    if (elt.isClosingTag()) {
			ls.form = null;
		    } else {
			ls.form = new PicoForm();
			String formMethod = elt.getAttributeVal("method", true);
			if ((formMethod != null)
			    && (formMethod.toUpperCase().equals("POST"))) {
			    ls.form.method = PicoForm.POST;
			}
			String formTarget = elt.getAttributeVal("action", true);
			if (formTarget == null) {
			    formTarget = currentURL;
			}
			ls.form.action = formTarget;
		    }
		} else  if (Element.stringMatch(ignoreCase, "INPUT", elt.tag)) {
		    // set up an element to represent this 
		    layoutInputElement(elt, ls);
		} else  if (Element.stringMatch(ignoreCase, "SELECT", elt.tag)) {
		    if (elt.isClosingTag()) {
			layoutSelectElement(elt, ls); 
			ls.selectWidget = null;
		    } else {
			if (ls.form != null) {
			    
			    String wName = elt.getAttributeVal("name", true);
			    String multiple = elt.getAttributeVal("multiple", true);
			    String size = elt.getAttributeVal("size", true);


			    FormWidget w = new FormWidget(ls.form, FormWidget.SELECT);
			    ls.form.addWidget(w);

			    // if (size == null) {
			    w.size = lineWidth;
			    // }

			    
			    if (wName == null) {
				wName = "UNNAMED_SELECT";
			    }
			    w.name = wName;

			    if (multiple != null) {
				w.multiple = true;
			    }
			    
			    w.options = new Vector();

			    ls.selectWidget = w;
			}
		    }
		} else if (Element.stringMatch(ignoreCase, "OPTION", elt.tag)) {
		    // Add this option to the current SELECT widget of 
		    // the current form.
		 
		    // Since we don't have a parse tree, we need to scan
		    // forward for the closing /OPTION tag, and take 
		    // everything in between as the value.
		    if (!elt.isClosingTag()) {
			    
			StringBuffer contentBuf = new StringBuffer();
			String value = elt.getAttributeVal("value", true);
			String selected = elt.getAttributeVal("selected", true);

			// If we were doing better bad-html recovery, we 
			// would abort out of this after reading just the next element,
			// if a closing /OPTION tag was not found.
			while (idx < maxidx) {
			    idx = elt.readNextElement(text, idx);
			    if ((elt.type == Element.TAG) 
				&& (Element.stringMatch(ignoreCase, "OPTION", elt.tag))
				&& elt.isClosingTag()) {
				break;
			    } else {
				contentBuf.append(elt.value);
			    }
			}
			String content = contentBuf.toString();

			// if SELECTED attribute is present, set the FormWidget.selection
			// to the current index into the FormWidget.options Vector
			// +++ NYI +++
			
			if (value == null) {
			    value = content;
			}

			if (ls.selectWidget != null) {
			    formAddOption(value, content, ls.selectWidget);
			}
		    }
		}

		// Adjust fonts
		if (ls.isBold && ls.isItalic) {
		    ls.fontStyle = Font.STYLE_BOLD | Font.STYLE_ITALIC;
		} else if (ls.isBold) {
		    ls.fontStyle = Font.STYLE_BOLD;
		} else if (ls.isItalic) {
		    ls.fontStyle = Font.STYLE_ITALIC;
		} else {
		    ls.fontStyle = Font.STYLE_PLAIN;
		}
		    
		ls.font = Font.getFont(ls.fontFace, ls.fontStyle, ls.fontSize);
	    }
	}

	// Finish off this line, space it properly from the previous line.
	adjustLinePosition(currentLine, ls);

	// select the first hyperlink
	nextHyperlink(false);
    }

    /** Handle a OPTION tag. Add the option name and value to the selectWidget
     */
    private void formAddOption (String value, String content, FormWidget selectWidget) {
	FormSelectOption option = new FormSelectOption(value, content);
	selectWidget.options.addElement(option);
    }

    /** Lay out an item to display a SELECT input. Note, this is called
     * when the tag is CLOSED (i.e., when the /SELECT element is encountered).
     *
     * This is an artifact of the fact that we don't have a parse-tree, so we
     * are doing this more as a "event driven" style of parsing.
     */
    private void layoutSelectElement (Element elt, LayoutState ls) {
	FormWidget w = ls.selectWidget;

	// Build an HtmlItem that points to this FormWidget
	lineBreakIfNeeded(ls);
	createNewItem(ls);
	currentItem.type = HtmlItem.INPUT;
	currentItem.widget = w;
	currentItem.width = lineWidth;
	currentItem.height = ls.font.getHeight()+ INPUT_FIELD_BORDER;	

	// We're going to use a hyperlinkId to make it possible to navigate
	// to this item using the "nextlink" and "prevlink" button
	currentItem.linkId = hyperlinkId++;
	// The "select" handler will notice if the HtmlItem is a 
	// input widget, and do some UI instead of trying to fetch the link

	// advance cursor position to right of item
	forcedLineBreak(ls);
    }

    /**
     * Handle an HTML INPUT tag. Examine the TYPE attribute to
     * determine which kind of input widget this is.
     */
    private void layoutInputElement (Element elt, LayoutState ls) {
	String wType = elt.getAttributeVal("type", true);
	String wName = elt.getAttributeVal("name", true);
	String wValue = elt.getAttributeVal("value", true);
	String wSize = elt.getAttributeVal("size", true);
	String chk = elt.getAttributeVal("checked", true);

	// Is this a checked checkbox or selected button?
	boolean checked = false;

	if (chk != null) {
	    checked = true;
	}

	FormWidget w = new FormWidget(ls.form, FormWidget.TEXT);
	ls.form.addWidget(w);

	if (wType != null) {
	    if (wType.toUpperCase().equals("SUBMIT")) {
		w.type = FormWidget.SUBMIT;
	    } else if (wType.toUpperCase().equals("HIDDEN")) {
		w.type = FormWidget.HIDDEN;
	    } else if (wType.toUpperCase().equals("CHECKBOX")) {
		w.type = FormWidget.CHECKBOX;
	    } else if (wType.toUpperCase().equals("RADIO")) {
		w.type = FormWidget.RADIO;
	    } else if (wType.toUpperCase().equals("PASSWORD")) {
		w.type = FormWidget.PASSWORD;
	    } else { 
		// what should we do with an unknown type? 
	    }
	}

	w.checked = checked;

	if (wName == null) {
	    wName = "";
	}
	w.name = wName;

	if (wValue == null) {
	    wValue = "";
	}
	w.setValue(wValue);

	// Build an HtmlItem that points to this FormWidget

	if (w.type == FormWidget.HIDDEN) {
	    // HIDDEN inputs have no corresponding layout item
	} else {
	    createNewItem(ls);
	    currentItem.type = HtmlItem.INPUT;
	    currentItem.text = wValue;
	    currentItem.widget = w;
	    currentItem.width = ls.font.stringWidth(currentItem.text) + INPUT_FIELD_BORDER;
	    currentItem.height = ls.font.getHeight()+ INPUT_FIELD_BORDER;	

	    // parse out the INPUT size attribute, if it exists
	    int size;
	    if (wSize != null) {
		try {
		    size = Integer.parseInt(wSize);
		} catch (NumberFormatException e) {
		    size = 	currentItem.width;
		}
		currentItem.width = size * ls.font.charWidth(' ');
	    }
	    
	    if ((w.type == FormWidget.CHECKBOX) || (w.type == FormWidget.RADIO)) {
		currentItem.width = CHECKBOX_WIDTH + 2;
		currentItem.height = CHECKBOX_HEIGHT;
	    }

	    // We're going to use a hyperlinkId to make it possible to navigate
	    // to this item using the "nextlink" and "prevlink" button
	    currentItem.linkId = hyperlinkId++;
	    // The "select" handler will notice if the HtmlItem is a 
	    // input widget, and do some UI instead of trying to fetch the link

	    // advance cursor position to right of item
	    xpos += currentItem.width;
	}
    }

    /** Layout an image Element. Looks for the SRC and ALIGN tags
     *
     * We don't handle image scaling. 
     * @param elt A IMAGE element
     * @param ls the current layout state 
     */


    private void layoutImageElement(Element elt, LayoutState ls) {
	String src = elt.getAttributeVal("src", true);
	String align = elt.getAttributeVal("align", true);

	//debugPrint("layout Image elt:"+src+" align="+align);
	

	if (src == null) {
	    return;
	}

	if (align == null) {
	    align = "BOTTOM";
	}


	// Build the new image item.
	currentItem = 
	    HtmlItem.newHtmlItem(xpos, 0, 0, 0, ls.font, currentLine);
	currentItem.type = HtmlItem.IMAGE;
	items.addElement(currentItem);

	if (Element.stringMatch(true, "BOTTOM", align)) {
	    currentItem.alignment = Graphics.BOTTOM;
	} else if (Element.stringMatch(true, "TOP", elt.tag)) {
	    currentItem.alignment = Graphics.TOP;
	} else if (Element.stringMatch(true, "MIDDLE", elt.tag)) {
	    currentItem.alignment = Graphics.VCENTER;
	} else {
	    currentItem.alignment = Graphics.BOTTOM;    
	}

	if (ls.isLink) {
	    currentItem.linkId = hyperlinkId;
	    currentItem.link = ls.linkTarget;
	}

	// make an absolute SRC URL to get the image
	String url = makeAbsoluteURL(src);

	//debugPrint("getting Image from src: "+src+"=="+url);

	Image image = readImageFromURL(url);

	// should return a broken image if retrieval failed.
	currentItem.image = image;

	// keep track of what the width of the text string is
	currentItem.width = image.getWidth();
	currentItem.height = image.getHeight();

	boolean overrun = ((xpos + currentItem.width) > ls.rightMargin);

	// If we have reached right screen margin, handle line wrapping, insert item
	// on a new line.
	
	// If this item is the only item in the line, then don't wrap, since
	// it will always be too wide no matter where we put it.

	boolean emptyLine = (currentLine.size() == 0);

	if (overrun && !(ls.preformat) && !emptyLine) {
	    // finish off the current line
	    adjustLinePosition(currentLine, ls);
		
	    // start a new line
	    addNewLine(ypos, ls);
	    // start a new item on the next line
	    xpos = ls.leftMargin;
	    currentItem.xpos = xpos;
	}

	// add item to the current line
	currentLine.addItem(currentItem);
	// advance the cursor to the right side of the item.
	xpos += currentItem.width;
    }

    /** Layout a text Element. Sequentially copy text from Element into
     * the current HtmlItem. If we reach a right margin, perform 
     * a word-wrap. This requires adding a new Line to the layout and
     * creating a new HtmlItem to continue the text on the next line.
     *
     *
     * Note, this wraps words at the character where they hit the
     * right margin.  To do fancier word-breaking, we should save
     * index of the last known whitespace or punctuation.   Then we
     * have the option of backing up to that point.
     *
     * @param elt A TEXT element
     * @param ls the current layout state 
     * @param wordWrap if true, attempt to wrap words at whitespace boundaries
     */

    private void layoutTextElement (Element elt, LayoutState ls, boolean wordWrap) {
	// We may not actually need to instantiate a new HtmlItem, if
	// for example, all text is whitespace.
	int width = 0;
	int height = 0;

	// clear out the scratch stringbuffer
	buf.delete(0, buf.length());

	// +++ MOVE WHITESPACE STATE to LayoutState vector +++
	// then it will persist across invocations to layoutTextElement +++

	//debugPrint("layoutTextElement: xpos="+xpos+",  ls.leftmarg="+ls.leftMargin+" val: "+elt.value);

	// Loop over text element's buffer one char at a time,
	// typesetting into current layout item, performing wordwrap
	// if needed.


	int idx = 0;
	int maxidx = elt.value.length();

	// The index of last whitespace in the elt's raw text
	int lastWhitespaceIdx = 0;

	// The index of the last place we inserted whitespace into the current
	// stringbuffer (not necessarily the same as lastWhitespaceIdx because
	// we are discarding redundant whitespace. 
	int lastWhitespaceBuf = 0;

	boolean wrapped = false;
	char ch;

	while (idx < maxidx) {
	    // Read a character
	    ch = elt.value.charAt(idx++);	

	    boolean charInserted = false;

	    // Decide how to insert it.

	    // Check if we are in a PRE preformat block:
	    if (ls.preformat) {
		// Special case - in preformat text, a newline means
		// start a newline.
		if (ch == '\n') {
		    if (width != 0) {
			createNewItem(ls);
			currentItem.text = buf.toString();
			currentItem.width = ls.font.stringWidth(currentItem.text);
			currentItem.height = ls.font.getHeight();
			width = 0;
		    }
		    forcedLineBreak(ls);
		    // clear out the scratch stringbuffer
		    buf.delete(0, buf.length());
		} else {
		    buf.append(ch);
		    charInserted = true;
		}
	    } else {
		// Otherwise, layout as normal text; shrink any run of 
		// whitespace down to a single whitespace. 
		//
		// If the whitespace occurs before any printed
		// characters have been inserted on this line, keep
		// discarding whitespace until a printing character is seen.
		boolean whitespace = isWhitespace(ch);

		if (!whitespace) {
		    charInserted = true;
		    buf.append(ch);
		    ls.runningWhitespace = false;
		} else {
		    // coerce all whitespace chars in to a ' ' char
		    ch = ' ';
		    if (!ls.runningWhitespace) {
			ls.runningWhitespace = true;
			lastWhitespaceIdx = idx;
			lastWhitespaceBuf = buf.length(); 
			charInserted = true;
			buf.append(ch);
		    }
		}
	    }

	    // keep track of what the width of the text string is
	    if (charInserted) {
		width += ls.font.charWidth(ch);
		//width = ls.font.stringWidth(buf.toString());
		boolean overrun = ((xpos + width) > ls.rightMargin);

		// If we have reached right screen margin, handle line
		// wrapping, start a new itme on a new line
		if (overrun && !(ls.preformat)) {
		    boolean wrapChar = false;
		
		    // It's a real right-margin overrun, so pull the last char
		    // off and start a new run on a new line.

		    /* Actually, there are two choices - 
		     *
		     * 1) If we are in not in wordwrap mode, then just
		     * take off the last char and stick it onto the
		     * next line (unless it's whitespace).
		     *
		     * 2) In "wordWrap" mode we need to back all the
		     * way up to the last whitespace we saw on this
		     * line, (which could in theory be in an HtmlItem
		     * which is several items back on this line, but
		     * we'll finesse that and say that if it's not in
		     * this Item, we're not going to go back further
		     * than the start of this item.
		     */

		    if (overrun && (buf.length() > 0)) {
			if (wordWrap) {
				// no whitespace seen, wrap the whole item to the
				// next line
			    buf.delete(lastWhitespaceBuf, buf.length());
				// back the char index to that point
			    idx = lastWhitespaceIdx;
			} else {
			    // not in wordWrap, so just strip last char
			    buf.deleteCharAt(buf.length()-1);
			    // back up the pointer one char
			    idx--;
			}
			wrapped = true;
		    }
		
		    // We create and typeset a new HtmlItem for this run of text
		    createNewItem(ls);
		    currentItem.text = buf.toString();
		    currentItem.width = ls.font.stringWidth(currentItem.text);
		    currentItem.height = ls.font.getHeight();
		    // clear out the scratch stringbuffer
		    width = 0;
		    buf.delete(0, buf.length());

		    /*
		      debugPrint("wrapping line. currentItem "+
		      items.indexOf(currentItem)+" x="+ currentItem.xpos
		      + " w="+currentItem.width
		      + " h="+currentItem.height 
		      + ": "+currentItem.text
		      ); */

		    // finish off the current line
		    adjustLinePosition(currentLine, ls);
		
		    // start a new line
		    addNewLine(ypos, ls);

		    // start a new item on the next line
		    xpos = ls.leftMargin;
		    // clear out the scratch stringbuffer
		    buf.delete(0, buf.length());

		    // Re-read the char, since idx may have been pushed back by
		    // the linewrap
		    ch = elt.value.charAt(idx++);	

		    if (wrapped && !(isWhitespace(ch))) {
			ls.runningWhitespace = false;
			buf.append(ch);
			width += ls.font.charWidth(ch);
		    }

		    //xpos += currentItem.width;
		} else {
		    // just keep laying out characters
		}
	    }
	}

	// We're finished copying chars from the Element. Finish off
	// the current item.
	if (width != 0) {
	    createNewItem(ls);
	    currentItem.text = buf.toString();
	    currentItem.width = ls.font.stringWidth(currentItem.text);
	    currentItem.height = ls.font.getHeight();	

	    // advance cursor position to right of item
	    xpos += currentItem.width;

	    // compute final dimensions on current item
	}
    }
    void forcedLineBreak (LayoutState ls) {
	// finish off the current line
	adjustLinePosition(currentLine, ls);
		
	// start a new line
	addNewLine(ypos, ls);

	// start a new item on the next line
	xpos = ls.leftMargin;
    }

    /** 
     * Only make a new line if the previous line is non-empty.
     */
    void lineBreakIfNeeded (LayoutState ls) {
	//debugPrint("line "+lines.indexOf(currentLine)+": ypos="+currentLine.ypos+", height="+currentLine.height);
	//debugPrint("lineBreakIfNeeded: c.size="+currentLine.size());
	if (currentLine.size() != 0) {
	    forcedLineBreak(ls);
	}
	xpos = ls.leftMargin;
    }

    /** Compute various adjustments to a line after it has been filled with elements.
     *<ul>
     * <li> Compute total height of this line, to space it vertically from
     * the previous line. 
     * <li> Computer any x offset if items need to be centered.
     * <li> Adjust vertical offsets of image items if their alignment is not BOTTOM
     * </ul>
     */
    void adjustLinePosition (Line line, LayoutState ls) {
	// If this is the first line, space it assuming the previous line
	// descends to ypos = 0

	line.height = 0;
	int width = 0;
	for (int i = 0; i < line.size(); i++) {
	    HtmlItem item = (HtmlItem) line.elementAt(i);
	    line.height = Math.max(line.height,item.height);
	    width += item.width;
	}

	// Only an empty line should ever have zero height. If it is
	// really empty, it was probably being used a blank space by a
	// <P> tag. Force it to be to be one char bbox height in the
	// current font.
	if (line.height == 0) {
	    line.height = ls.font.getHeight();
	}

	// PLACEHOLDER
	ypos += line.height;
	line.ypos = ypos;

	// Computer centering
	if (ls.center) {
	    int deltaX = (lineWidth - width) / 2 ;
	    for (int i = 0; i < line.size(); i++) {
		HtmlItem item = (HtmlItem) line.elementAt(i);
		item.xpos += deltaX;
	    }
	}

	// add up heights of all objects, factor in alignment. 
	// compute spacing from previous line, and set ypos from that.
	// +++ NYI

    }

    /** 
     * Create a new HtmlItem, initialize it for text, and
     * 	add it to the current line.
     */
    void createNewItem (LayoutState ls) {
	currentItem = 
	    HtmlItem.newHtmlItem(xpos, 0, 0, 0, ls.font, currentLine);
	currentItem.color = ls.color;
	currentItem.font = ls.font;
	currentItem.width = 0;
	if (ls.isLink) {
	    currentItem.linkId = hyperlinkId;
	    currentItem.link = ls.linkTarget;
	}
	currentItem.type = HtmlItem.TEXT;
		
	items.addElement(currentItem);
	// add item to the current line
	currentLine.addItem(currentItem);
    }

    /**
     * Redisplay the current page. Highlights any selected element which is visible
     * on the current page.
     */
    public void paint (Graphics g) {
	try {
	    g.setColor(WHITE);
	    g.fillRect(0, 0, nScreenWidth, nScreenHeight);
	
	    // loop over the lines, displaying them
	    Line lastline = null;

	    for (int i = 0; i < lines.size(); i++) {
		lastline = ((Line) lines.elementAt(i));
		paintLine(g, lastline);
	    }

	    if (lastline != null) {
		maxPageHeight = lastline.ypos;
	    }

	} catch (Exception e) {
	    //debugPrint(e.toString());
	}
    }

    void paintImageItem (Graphics g, HtmlItem item, Line line) {
	if ((item.linkId != -1) && (item.linkId == selectedHyperlink)) {
	    // draw a border
	    g.setColor(SELECTED_LINK_COLOR);
	    g.fillRect(item.xpos-1,
		       (line.ypos - (item.height + nPosY + 1)),
		       item.width+2,
		       item.height+2);
	}
	if (item.image == null) {
	    g.drawString("[ERROR:NULLIMAGE="+lines.indexOf(line)
			 +":"+items.indexOf(item)
			 +"]", 
			 item.xpos, line.ypos - nPosY, g.LEFT|g.BOTTOM);
	} else {
	    g.drawImage(item.image,
			item.xpos,
			(line.ypos - (item.height + nPosY)),
			g.LEFT | g.TOP);
	}
    }

    void paintTextItem (Graphics g, HtmlItem item, Line line) {
	g.setFont(item.font);

	// Selected Hyperlink is shown in reverse video
	if (item.linkId != -1) {
	    if (item.linkId == selectedHyperlink) {
		g.setColor(SELECTED_LINK_COLOR);
		g.fillRect(item.xpos-1,
			   (line.ypos - nPosY)-item.height,
			   item.width+1,
			   item.height+1);
		g.setColor(WHITE);
	    }  else {
		g.setColor(SELECTED_LINK_COLOR);		    
	    }
	} else {
	    g.setColor(item.color);
	}

	if (item.text == null) {
	    // There should never be a text item with null text -- show an error:
	    g.drawString("[ERROR:NULL="+lines.indexOf(line)
			 +":"+items.indexOf(item)
			 +"]", 
			 item.xpos, line.ypos - nPosY, g.LEFT|g.BOTTOM);
	} else {
	    g.drawString(item.text, item.xpos, line.ypos - nPosY, g.LEFT|g.BOTTOM);
	}
    }
    
    void paintInputItem (Graphics g, HtmlItem item, Line line) {

	FormWidget w = item.widget;
	if (w == null) {
	    // this is an error
	    g.setColor(RED);
	    g.drawString("NULL_WIDGET_ERR", item.xpos, line.ypos - nPosY, g.LEFT|g.BOTTOM);
	    return;
	}

	int color;
	boolean selected;

	if ((item.linkId != -1) && (item.linkId == selectedHyperlink)) {
	    selected = true;
	    color = RED;
	} else {
	    selected = false;
	    color = BLUE;
	}

	if (w.type == FormWidget.SELECT) {
	    // Draw a box around the thing. Box should extend by widget's SIZE or
	    // to max width of screen.
	    g.setColor(color);
	    g.drawRect(leftMargin,
		       (line.ypos - nPosY)-item.height,
		       lineWidth-1,
		       item.height);
	
	    // If selected, draw an extra heavy border, to show up on B&W displays
	    if (selected) {
		g.drawRect(leftMargin+1,
			   ((line.ypos - nPosY)-item.height)-1,
			   (lineWidth-2),
			   item.height+2);
	    }

	    // Draw the selected item value. 
	    // for now let's draw the first item in the list
	    g.setColor(BLACK);
	    if ((w.options != null) && (w.options.size() > 0)) {
		int selection;
		if (w.selection == -1) {
		    selection = 0;
		} else {
		    selection = w.selection;
		}

		FormSelectOption option = 
		    (FormSelectOption) (w.options.elementAt(selection));

		String textVal = option.content;
		//debugPrint("default SELECT option = "+textVal);

		g.drawString(textVal,
			     item.xpos + (INPUT_FIELD_BORDER/2),
			     line.ypos - nPosY,
			     g.LEFT|g.BOTTOM);
	    }
	} else if (w.type == FormWidget.TEXT) {
	    // Draw a box around the thing. Box should extend by widget's SIZE or
	    // to max width of screen.
	    g.setColor(color);
	    g.drawRect(item.xpos,
		       (line.ypos - nPosY)-item.height,
		       item.width,
		       item.height);
	    // If selected, draw an extra heavy border, to show up on B&W displays
	    if (selected) {
		g.drawRect(item.xpos+1,
			   ((line.ypos - nPosY)-item.height)+1,
			   item.width-2,
			   item.height-2);
	    }

	    g.setColor(item.color);
	    String textVal = w.getValue();
	    if (textVal == null) {
		// cannot happen ,should be at least an empty string
		g.drawString("[ERROR:NULLINPUTVAL="+lines.indexOf(line)
			     +":"+items.indexOf(item)
			     +"]", 
			     item.xpos, line.ypos - nPosY, g.LEFT|g.BOTTOM);
	    } else {
		g.drawString(textVal,
			     item.xpos + (INPUT_FIELD_BORDER/2),
			     line.ypos - nPosY,
			     g.LEFT|g.BOTTOM);
	    }
	} else if (w.type == FormWidget.SUBMIT) {
	    g.setColor(color);
	    g.drawRoundRect(item.xpos,
			    (line.ypos - nPosY)-item.height,
			    item.width,
			    item.height, 
			    4, 4);
	
	    if (selected) {
		g.drawRoundRect(item.xpos-1,
				((line.ypos - nPosY)-item.height)-1,
				item.width+2,
				item.height+2,
				4, 4);
	    }

	    g.setColor(item.color);
	    if (item.text == null) {
		// There should never be a text item with null text -- show an error:
		g.drawString("[ERROR:NULL="+lines.indexOf(line)
			     +":"+items.indexOf(item)
			     +"]", 
			     item.xpos, line.ypos - nPosY, g.LEFT|g.BOTTOM);
	    } else {
		g.drawString(item.text, 
			     item.xpos+(INPUT_FIELD_BORDER/2),
			     line.ypos - nPosY,
			     g.LEFT|g.BOTTOM);
	    }
	} else if (w.type == FormWidget.CHECKBOX) {
	    g.setColor(color);
	    int x1 = item.xpos;
	    int y1 = (line.ypos - nPosY) - DEFAULT_BASELINE - CHECKBOX_HEIGHT;
	    int x2 = x1 + CHECKBOX_WIDTH;
	    int y2 = y1 + CHECKBOX_HEIGHT;

	    g.drawRect(x1,
		       y1,
		       CHECKBOX_WIDTH,
		       CHECKBOX_HEIGHT);

	    if (selected) {
		g.drawRect(x1+1,
			   y1+1,
			   CHECKBOX_WIDTH-2,
			   CHECKBOX_HEIGHT-2);
	    }

	    // If the box is checked, then draw a checkmark
	    if (w.checked) {
		g.setColor(BLACK);

		
		g.drawLine(x1, y1, x2, y2);
		g.drawLine(x2, y1, x1, y2);
	    }
	} else if (w.type == FormWidget.RADIO) {
	    g.setColor(color);
	    int x1 = item.xpos;
	    int y1 = (line.ypos - nPosY) - DEFAULT_BASELINE - CHECKBOX_HEIGHT;
	    int x2 = x1 + CHECKBOX_WIDTH;
	    int y2 = y1 + CHECKBOX_HEIGHT;

	    g.drawArc(x1,
		       y1,
		       CHECKBOX_WIDTH,
		       CHECKBOX_HEIGHT, 0, 360);

	    if (selected) {
		g.drawArc(x1+1,
			   y1+1,
			   CHECKBOX_WIDTH+1,
			   CHECKBOX_HEIGHT+1, 0, 360);
	    }

	    // If the box is checked, then draw a checkmark
	    if (w.checked) {
		g.setColor(BLACK);

		g.fillArc(x1+1,
			   y1+1,
			   CHECKBOX_WIDTH-2,
			   CHECKBOX_HEIGHT-2, 0, 360);
	    }
	}
    }

    /**
     * Render a line of items to the screen
     */
    void  paintLine (Graphics g, Line line) {
	// check if line is visible
	if ((line.ypos >= nPosY) &&
	    (line.ypos-line.height) < (nPosY+nScreenHeight)) {

	    for (int i = 0; i < line.size(); i++) {
		HtmlItem item = (HtmlItem) line.elementAt(i);	
		if (item.type == HtmlItem.IMAGE) {
		    paintImageItem(g, item, line);
		} else if (item.type == HtmlItem.INPUT) {
		    paintInputItem(g, item, line);
		} else {
		    paintTextItem(g, item, line);
		}
	    }
	}
    }

    /** Advance selected hyperlink to next link. If it's not on this
     * page, scroll display to next page. If we scrolled, reselect to
     * find select first link on the page if there is one visible. 
     */
    void nextHyperlink (boolean autoScroll) {
	boolean foundNext = false;
	boolean scrolldown = false;
        boolean notInScreen  = false;

        if (selectedHyperlink < hyperlinkId) {
	    selectedHyperlink++;
	}
        if(selectedHyperlink==0) notInScreen=true;
	//    debugPrint("nextHyperlink selectedHyperlink = "+selectedHyperlink+" hyperlinkId="+hyperlinkId);

         //$
        HtmlItem item2 = null;

        for (int i = 0; i < items.size(); i++) {
	    HtmlItem item = (HtmlItem) items.elementAt(i);
            if(selectedHyperlink>0 && item.linkId==selectedHyperlink-1) {
                item2 = item;
                debugPrint("i.ypos=" + item.line.ypos + "i.ht=" + item.line.height);
                if(((item.line.ypos >= nPosY) || (item.line.height+ item.line.ypos) >= nPosY) && item.line.ypos <= (nPosY + nScreenHeight) )
                    notInScreen=false;
                else notInScreen=true;
                break;
            }
        }

        for (int i = 0; i < items.size(); i++) {
	    HtmlItem item = (HtmlItem) items.elementAt(i);
	    if (item.linkId == selectedHyperlink) { //if next item is on same page  highlight it
                if(item.line.ypos >= nPosY && item.line.ypos <= (nPosY + nScreenHeight))
                {
                    selectedHyperlink=item.linkId;
                    foundNext=true;
                    break;
                }
                /*otherwise if the current link is in current page
                 *and next link is in next part(1/10) of page
                 *high light it else just scroll down
                  */
                else 
                {
                    if(selectedHyperlink>0){
                    if (item2.line.ypos >= nPosY &&  (item2.line.ypos < (nPosY + nScreenHeight)) ){
                        nPosY=nPosY + nScreenHeight/10;
                     if(item.line.ypos < (nPosY + nScreenHeight/10))
                    selectedHyperlink=item.linkId;
                     else selectedHyperlink--;
                    foundNext=true;
                    break;}
                    }}
                }
        }//if current & next link not in current page just scroll. this is code for scroll page without links
        if(selectedHyperlink>0 && foundNext==false){
                    if (item2.line.ypos >= nPosY &&  (item2.line.ypos < (nPosY + nScreenHeight)) ){
                        if (nPosY < (maxPageHeight - nScreenHeight)) {nPosY+= nScreenHeight/10;
                     selectedHyperlink--;
                    foundNext=true;
                        }
                    }
        }
        debugPrint("y=" + nPosY + notInScreen);
        // after pagedowns, down key activates first link in that page
        if(foundNext==false && notInScreen) {
            for (int i = 0; i < items.size(); i++) {
	    HtmlItem item = (HtmlItem) items.elementAt(i);
            if (item.linkId != -1) {
            if(item.line.ypos>=nPosY && item.line.ypos<=(nPosY + nScreenHeight))
            {
                selectedHyperlink=item.linkId;
                foundNext=true;
                break;
            }
            }}
            }
        if(foundNext==false)
        {
            if (nPosY < (maxPageHeight - nScreenHeight)) {nPosY+= nScreenHeight/10;}
            selectedHyperlink--;
        } //finish>
    }

    /**
     * Backup to select the previous hyperlink HtmlItem, or go to prev
     * page if no links on this page.  (Need to update this to work
     * like nextHyperlink does now) 
     */
    void prevHyperlink () {
	boolean foundPrev = false;
        boolean notInScreen = false;
	if (selectedHyperlink >= 0) {
	    selectedHyperlink--;
	}
        if(selectedHyperlink==-1) notInScreen=true;
	//debugPrint("prevHyperlink selectedHyperlink = "+selectedHyperlink+" hyperlinkId="+hyperlinkId);

        //$
        HtmlItem item2 = null;

        for (int i = items.size()-1; i >=0 ; i--) {
	    HtmlItem item = (HtmlItem) items.elementAt(i);
            if(selectedHyperlink>0 && item.linkId==selectedHyperlink+1) {
                item2 = item;
                debugPrint("i.ypos=" + item.line.ypos + "i.ht=" + item.line.height);
                if(((item.line.ypos >= nPosY) || (item.line.height+ item.line.ypos) >= nPosY) && item.line.ypos <= (nPosY + nScreenHeight) )
                    notInScreen=false;
                else notInScreen=true;
                break;
            }
        }

        for (int i = items.size()-1; i >=0 ; i--) {
	    HtmlItem item = (HtmlItem) items.elementAt(i);
	    if (item.linkId == selectedHyperlink && selectedHyperlink>=0) { //if next item is on same page  highlight it
                if(item.line.ypos >= nPosY && item.line.ypos <= (nPosY + nScreenHeight))
                {
                    selectedHyperlink=item.linkId;
                    foundPrev=true; debugPrint("boo"+selectedHyperlink);
                    break;
                }
                /*otherwise if the current link is in current page
                 *and next link is in next part(1/10) of page
                 *high light it else just scroll down
                  */
                else
                {
                    if(selectedHyperlink>0){
                    if (item2.line.ypos >= nPosY &&  (item2.line.ypos < (nPosY + nScreenHeight)) ){
                        nPosY=nPosY - nScreenHeight/10;
                     if(item.line.ypos > nPosY )
                    selectedHyperlink=item.linkId;
                     else selectedHyperlink++;
                    foundPrev=true;
                    break;}
                    }}
                }
        }//if current & next link not in current page just scroll. this is code for scroll page without links
        if(selectedHyperlink>0 && foundPrev==false){
                    if (item2.line.ypos >= nPosY &&  (item2.line.ypos < (nPosY + nScreenHeight)) ){
                        if (nPosY > (nScreenHeight/10)) nPosY-= nScreenHeight/10; else nPosY=0;
                     selectedHyperlink++;
                    foundPrev=true;
                    }
        }
        debugPrint("y=" + nPosY+ notInScreen + foundPrev);
        // after pagedowns, down key activates first link in that page
        if(foundPrev==false && notInScreen ) {
            for (int i = items.size()-1; i >=0; i--) {
	    HtmlItem item = (HtmlItem) items.elementAt(i);
            if (item.linkId != -1) {
            if(item.line.ypos>=nPosY && item.line.ypos<=(nPosY + nScreenHeight))
            {
                selectedHyperlink=item.linkId;
                foundPrev=true;
                break;
            }
            }}
            }
        if(foundPrev==false)
        {
            if (nPosY > (nScreenHeight/10)) nPosY-= nScreenHeight/10; else nPosY=0;
            selectedHyperlink++;
        } //finish>

    }

    static final String BROKEN_IMAGE = "/images/broken.png";

    /** 
     * Takes a possibly relative URL, and generate an absolute URL, merging
     * with the current documentbase if needed.
     * 
     * <p>
     * <ol>
     * <li> If URL starts with http:// or resource:// leave it alone
     * <li> If URL starts with '/', prepend document base protocol and
     * host name. 
     * <li> Otherwise, it's a relative URL, so prepend current document
     * base and directory path.
     * </ol>
     *
     * @param src the (possibly relative) URL 
     * @return absolute URL
     */

    String makeAbsoluteURL (String src) {
	//debugPrint("makeAbsoluteURL: currentDocumentBase="+currentDocumentBase);
	// If no ":", assume it's a relative link, (no protocol),
	// and append current page
	if (("http://".regionMatches(true, 0, src, 0, 7))
	    || ("resource://".regionMatches(true, 0, src, 0, 11))) {
	    return src;
	} else if (src.startsWith("/")) {
	    if ("resource://".regionMatches(true, 0, currentDocumentBase, 0, 11)) {
		// we need to strip a leading slash if it's a local resource, i.e.,
		// "resource://" + "/foo.png" => "resource://foo.png"
		return  Element.protocolAndHostOf(currentDocumentBase) + src.substring(1);
	    } else {
		// for HTTP, we don't need to strip the leading slash, i.e.,
		// "http://foo.bar.com" + "/foo.png" => "http://foo.bar.com/foo.png"
		return  Element.protocolAndHostOf(currentDocumentBase) + src;
	    }
	} else {
	    // It's a relative url, so merge it with the current document
	    // path. 
	    String prefix = Element.protocolAndPathOf(currentDocumentBase);
	    if (prefix.endsWith("/")) {
		return prefix+src;
	    } else {
		return prefix  + "/" + src;
	    }
	}
    }

    /*
      http://foo.com/bar.html | foo.html := http://foo.com + / + foo.html

      resource://bar.html | foo.html := http://foo.com + / + foo.html

    */


    /****************************************************************/
    /* Form handling */

    MIDPFormTextField textInput   = null;
    MIDPFormSelectList selectList = null;

    void showTextInputForm (FormWidget w, int maxSize, int constraints) {
	try { //$ some how the error eliminated
        if(textInput.setWidget(w, maxSize, constraints)==null) myalert("No error");
        else myalert("ya its me");
	Display.getDisplay(app).setCurrent(textInput);
        }
        catch(Exception e ){
            myalert("Error is here \"setTextInput\"");
        }

    }
    void showSelectInputForm (FormWidget w) {
	selectList.setWidget(w);
	Display.getDisplay(app).setCurrent(selectList);
    }


    /** Toggle the state of a checkbox */
    void toggleCheckbox (FormWidget w) {
	w.checked = !w.checked;
    }

    /** Toggle the state of a radio button. This needs to unset all
     * the other radio buttons in this form which have the same name.
     */
    void toggleRadio (FormWidget w) {
	String myName = w.getName();
	if (myName == null) {
	    myName = "";
	}
	PicoForm p = w.getForm();
	StringBuffer formData = new StringBuffer();
	// loop over all widgets, turn off any radio button with same name
	for (int i = 0; i < p.size(); i++) {
	    FormWidget w2 = p.elementAt(i);
	    if (myName.equals(w2.getName())) {
		w2.checked = false;
	    }
	}
	w.checked = true;
    }

    /** 
     * A form input widget was selected. Figure out the action
     * to take:
     *<ul>
     * <li> SUBMIT button
     * <li> INPUT type = TEXT
     * <li> INPUT type = TEXTAREA
     *</ul>
     */
    void handleInputSelection (HtmlItem item) {
	FormWidget w = item.widget;
	try {
            //$myalert(item.widget.value);
	    if ((w.type == FormWidget.TEXT) 
		|| (w.type == FormWidget.TEXTAREA)) {
		showTextInputForm(w, 25, TextField.ANY);
	    } else if (w.type == FormWidget.SUBMIT) {
		doFormSubmit(item);
	    } else if (w.type == FormWidget.SELECT) {
		showSelectInputForm(w);
	    } else if (w.type == FormWidget.CHECKBOX) {
		toggleCheckbox(w);
	    } else if (w.type == FormWidget.RADIO) {
		toggleRadio(w);
	    }

	} catch (Exception err) {
	    Alert a = new Alert("Error");
	    a.setString("Failed in handling the widget at position: " + selectedHyperlink + "\nMess=" +  err.getMessage() + "\nStr=" + err.toString());
	    a.setTimeout(Alert.FOREVER);
	    Display.getDisplay(app).setCurrent(a);
	}
    }

    /**
     * Submit a form. Get the (PicoForm) form of this submit button, 
     * and build a URLencoded string of parameters.
     * Submit that to the form's target (action), using the form's designated method.
     * @param item a form SUBMIT button 
     */
    void doFormSubmit (HtmlItem item) {
	PicoForm p = item.widget.getForm();
	StringBuffer formData = new StringBuffer();
	// loop over all widgets, collecting names and values
	for (int i = 0; i < p.size(); i++) {
	    FormWidget w = p.elementAt(i);
	    String separator = (i == 0) ? "" : "&";
	    if ((w.type == FormWidget.TEXT)
		|| (w.type == FormWidget.TEXTAREA)) {
		formData.append(separator+w.getName()+"="+HttpUtils.URLencode(w.getValue()));
	    } else if (w.type == FormWidget.CHECKBOX) {
		if (w.checked) {
		    formData.append(separator+w.getName()+"="+HttpUtils.URLencode(w.getValue()));
		}
	    } else if (w.type == FormWidget.SUBMIT) {
		formData.append(separator+w.getName()+"="+HttpUtils.URLencode(w.getValue()));
	    } else if (w.type == FormWidget.RADIO) {
		if (w.checked) {
		    formData.append(separator+w.getName()+"="+HttpUtils.URLencode(w.getValue()));
		}
	    } else if (w.type == FormWidget.HIDDEN) {
		formData.append(separator+w.getName()+"="+HttpUtils.URLencode(w.getValue()));
	    } else if (w.type == FormWidget.SELECT) {
		// get selected option or options
		//
		// For now, only single selection implemented
		if ((w.options != null) && (w.options.size() > 0)) {
		    int selection;
		    if (w.selection == -1) {
			selection = 0;
		    } else {
			selection = w.selection;
		    }

		    FormSelectOption option = 
			(FormSelectOption) (w.options.elementAt(selection));

		    String textVal = option.content;
		    //debugPrint("default SELECT option = "+textVal);
		    formData.append(separator+w.getName()+"="+HttpUtils.URLencode(textVal));
		}
	    }
	}
	pushCachePage (currentPageText, currentURL);
	// Parse and render the text
	String target = makeAbsoluteURL(p.getAction());

        debugPrint(formData.toString());
	if (p.getMethod() == PicoForm.GET) {
	    // Do an HTTP GET
	    // append the formdata args
	    String query = formData.toString();
            debugPrint(target+"?"+query);
	    getURL(target+"?"+query);
	} else {
	    // POST
	    postURL(target, formData.toString());
	}
    }

    void handleSelection () {
	// Is something selected? 
	if (selectedHyperlink != -1) {
	    HtmlItem selectedItem = getSelectedItem();
	    if (selectedItem != null) {
		if (selectedItem.type == HtmlItem.INPUT) {
		    // It's an input, pop up the appropriate form to handle it
		    handleInputSelection(selectedItem);
		} else {
		    // It must be a hyperlink, fetch the link
		    fetchLink();
		}
	    }
	}
    }

    /**
     * Fetch an HTML document from a link URL.
     *
     */
    void fetchLink () {
	if (selectedHyperlink != -1) {
	    // Cache the current page and url
	    pushCachePage (currentPageText, currentURL);
	    // Parse and render the text
	    String target = makeAbsoluteURL(getLinkTarget(selectedHyperlink));
	    getURL(target);
	}
    }

    /**
     * Get the hyperlink target of an anchor. This should
     * be changed to take the item directly, instead of 
     * linkId int.
     */
    String getLinkTarget (int linkId) {
	for (int i = 0; i < items.size(); i++) {
	    HtmlItem item = (HtmlItem)items.elementAt(i);
	    if (item.linkId == linkId) {
		return item.link;
	    }
	}
	return null;
    }

    /** 
     * @return  the currently selected item, or null if no item is selected
     */
    HtmlItem getSelectedItem () {
	HtmlItem found = null;
	for (int i = 0; i < items.size(); i++) {
	    HtmlItem item = (HtmlItem) items.elementAt(i);	
	    if (item.linkId == selectedHyperlink) {
		found = item;
	    }
	}
	return found;
    }




    /** Cache the text of last N pages, for use of "back" button */
    Stack pageCache = new Stack();
    Stack urlCache = new Stack();
    /** How many page to cache */
    int cacheNPages = 10;
    
    String currentPageText = "";

    /** Add the text of a page to the cache. Need to save it's URL too. */
    public void pushCachePage (String text, String url) {
	// if stack is full, delete oldest element
	if (pageCache.size() > cacheNPages) {
	    pageCache.removeElementAt(0);
	}
	pageCache.push(text);
	urlCache.push(url);
    }

    /** Pops the cache stacks. Don't call this is pageCache.empty() is true.
     */
    public void popCachePage () {
	currentURL = (String) urlCache.pop();
	currentPageText = (String) pageCache.pop();
    }

    /** 
     * Caches last page for fast "back" feature. 
     * This should save the last n pages in a Vector for better performance.
     */
    void prevLink () {
	if (!pageCache.empty()) {
	    popCachePage();
	    setText(currentPageText);
            debugPrint("hiiiii");
	    selectedHyperlink = -1;
	    nPosY = 0;
            repaint(); //$ after back pressed repaint screen else old page will remain
	}
    }

    /****************************************************************/
    /**
     * keyRepeated
     */
    protected void keyRepeated (int key) {
	keyReleased(key);
    }

    /**
     * keyPressed
     */
    protected void keyPressed (int key) {
	keypress = key; 
	int action = getGameAction(key);
	// Is it a "gameAction"?
	if (action != 0) {
	    switch (action) {
	    case FIRE:
		// select hyperlink
		handleSelection();
		break;
	    case LEFT:
		// select hyperlink
                pageUp();
		break;
            case RIGHT:
                //page down
                pageDown();
                break;
	    case UP:
		prevHyperlink();
		break;
	    case DOWN:
		nextHyperlink(true);
		break;
	    }
	} else {
	    switch (key) {
	    case FIRE:
		// select hyperlink
		fetchLink();
		break;
	    case LEFT:
		// page up
                pageUp();
		break;
            case RIGHT:
                //page down
                pageDown();
                break;
	    case KEY_NUM2:
		// home
		getURL(HOMEPAGE_URL);
		break;
	    case UP:
		prevHyperlink();
		break;
	    case DOWN:
		nextHyperlink(true);
		break;

	    case KEY_NUM1:
		// scroll down
		if (nPosY > 0) {
		    nPosY-=8;
		}
		break;
	    case KEY_NUM3:
		// scroll down
		if (nPosY < maxPageHeight) {
		    nPosY+=8;
		}
		break;

	    case KEY_STAR:
		// scroll down
		//getNewURL();
		return;

	    case KEY_POUND:
		break;
	    }
	}
	
	repaint();
    }



    int keypress = 0;

    /****************************************************************
     * HTTP data fetch routines 
     *****************************************************************/


    /*
     * Fetch a generalized URL, returns a byte array.
     * <p>
     * Supported protocols are
     * <ul>
     * <li> HTTP://hostname/path/file.html uses Connector HttpConnection
     * <li> RESOURCE://path/file.html  read from local JAR file using getResourceAsStream(/path/file.html)
     * </ul>
     *
     */
    public byte[] getURLDataBytes (String url) throws IOException {
	if (url.toUpperCase().startsWith("HTTP://")) {

	    // +++ There's a bug in the Sun MIDP implementation of Connector.open().
	    // If the URL is "http://foo.com", with no trailing slash,
	    // then Connector.open sends "null" as the path in the HTTP request.
	    //
	    // We work around this by checking for this case, and adding a trailing slash.
	    if (url.indexOf('/', 7) == -1) {
		url = url+"/";
	    }
	    //debugPrint("getting http url "+url);
	    return httpGet(url);
	} else if (url.toUpperCase().startsWith("RESOURCE://")) {
	    // We sneakily take a substring which includes a leading slash in the line
	    // below. This makes an "absolute" path to pass to getResourceAsStream(),
	    // I.e., "resource://foo.html" => "/foo.html"
	    return localGet(url.substring(10));
	} else {
	    throw new IOException("getURLDataBytes: unsupported protocol "+url);
	}
    }

    /*
     * Fetch a generalized URL.
     * <p>
     * Supported protocols are
     * <ul>
     * <li> Those supported by getURLDataBytes()
     * <li> LOCAL invokes local servlet handler
     * </ul>
     *
     * In the future, the calls which convert byte[] to String could take
     * an explicit encoding arg if needed. 
     */
    public String getURLData (String url) {
	try {
	    if (url.toUpperCase().startsWith("LOCAL://")) {
		return handleServlet(url.substring(8));
	    } else {
		byte[] result = getURLDataBytes(url);
		if (result == null) {
		    return "error fetching url: "+url;
		}  else {
		    return new String(result);
		}
	    }
	} catch (IOException e) {
	    return "error fetching "+url+": "+e.toString();
	}
    }

    /** Post a data string to a url, return the data from the connection.
     * @param url 
     * @param content data string to send as POST data
     */
    String postURLData (String url, String content) {
	if (url.toUpperCase().startsWith("HTTP://")) {
	    // +++ There's a bug in the Sun MIDP implementation of Connector.open().
	    // If the URL is "http://foo.com", with no trailing slash,
	    // then Connector.open sends "null" as the path in the HTTP request.
	    //
	    // We work around this by checking for this case, and adding a trailing slash.
	    if (url.indexOf('/', 7) == -1) {
		url = url+"/";
	    }
	    try {
		return new String(httpPost(url, content));
	    } catch (IOException e) {
		return "error posting "+url+": "+e.toString();
	    }
	} else {
	    return "postURLData unsupported protocol: "+url;
	}
    }


    /*
     * Load an image from either the network (http) or a local
     * resource.
     *
     * @param url
     * @return an image created by Image.createImage(bytedata)
     */
    public Image readImageFromURL (String url) {
	try {
		byte buf[] = getURLDataBytes(url);
		return Image.createImage(buf, 0, buf.length);
	} catch (Exception e) {
	    try { 
		byte[] buf = localGet(BROKEN_IMAGE);
		return Image.createImage(buf, 0, buf.length);
	    } catch (IOException e2) {
		return null;
	    }
	}
    }



    /**
     * Read the HTTP headers and the data using HttpConnection.
     * Check the response code to insure successful retrieval.
     * <p>
     * Connector.open is used to open url and a HttpConnection is returned.
     * The HTTP headers are read and processed.
     *
     * <p>If postData is non-null, then we perform a HTTP POST.
     * This requires that we know what encoding the POST should
     * be performed in (say you were trying to post Japanese
     * text to a server). The optional encoding arg is used for this.
     * If null, the default system encoding is used.
     *
     * <p>
     * From the HttpConnection the InputStream is opened.
     * <p>
     * It is used to read every character until end of file (-1).
     * If an exception is thrown the connection and stream is closed.
     *
     * <p> A byte array is returned. This can be converted to a Java
     * Unicode string using new String(byteArray), with an optional
     * encoding arg.
     *
     * @param url the URL to process.
     * @param postData optional POST data to send 
     * @param encoding the encoding to use for POSTed data
     */
    public byte[] httpGetOrPost (String url, String postData, String encoding) throws IOException {
	HttpConnection c = null;
	InputStream is = null;
	OutputStream os = null;
	byte[] resultData = null;
	try {
	    int status = -1;

	    // If there are more than n redirects, give up, we might
	    // be in a loop.
	    int redirects = 0;

	    int accessMode = Connector.READ_WRITE;
            //$ change this for free browsing
	    c = (HttpConnection)Connector.open(url, accessMode, true);
	    setRequestHeaders(c);

	    if (postData != null) {
		c.setRequestMethod(HttpConnection.POST);
		os = c.openOutputStream();
		byte[] b = null;
		try {
		    if (encoding != null) {
			b = postData.getBytes(encoding);
		    } else {
			b = postData.getBytes();
		    }
		} catch (java.io.UnsupportedEncodingException e) {
		    b = postData.getBytes();
		}
		os.write(b);
	    }

	    while (true) {
		// Open the connection and check for re-directs

		// Get the status code, causing the connection to be made
		status = c.getResponseCode();
		currentURL = c.getURL();

		if (status == HttpConnection.HTTP_TEMP_REDIRECT ||
		    status == HttpConnection.HTTP_MOVED_TEMP ||
		    status == HttpConnection.HTTP_MOVED_PERM) {
		    // Get the new location and close the connection
		    url = c.getHeaderField("location");

		    if (is != null) {
			is.close();
		    }
		    if (os != null) {
			os.close();
		    }		    
		    
		    //c.close();

		    redirects--;
		    if (redirects > MAX_REDIRECTS) {
			throw new IOException("more than "+MAX_REDIRECTS+" HTTP redirects, maybe a loop?");
		    }

		    //System.out.println("Redirecting to " + url); //$ change this for free browsing
		    c = (HttpConnection)Connector.open(url, accessMode, true);
		    setRequestHeaders(c);
		} else {
		    break;
		}
	    }
            
	    // Only HTTP_OK (200) means the content is returned.
	    if (status != HttpConnection.HTTP_OK) {
		throw new IOException("Response status "+status+" not OK");
	    }

	    // Get the ContentType
	    String type = c.getType();
	    processType(type);

	    // open the InputStream 
	    is = c.openInputStream();

	    // Report the ContentLength 
	    long len = c.getLength(); 
	    // debugPrint("Content-Length: " + len); 

	    // Avoid reading to the end of a stream if you know 
	    // how many bytes you're going to get.  The penalties 
	    // are severe. 

	    if (len > 0) { 
		resultData = new byte[(int) len]; 
		try { 
		    int actlen = is.read(resultData); 
		} catch (IOException ioe) { 
		    return ("httpGetOrPost read failed with IOException " + ioe).getBytes(); 
		} 
	    } else { 
		resultData = readByteArrayFromStream(is); 
	    } 
	    
	} finally {
	    if (is != null) {
		is.close();
	    }
	    if (os != null) {
		os.close();
	    }
	    if (c != null) {
		
		// +++ This seems to be a bug with the SUN MIDP
		// emulator.. If we DO close the connection here, then
		// the next time we attempt to open a connection to the same
		// host, we get an error "could not reconnect to server". If 
		// I comment out the line below, the error doesn't happen.

		//c.close();
	    }
	}
	return resultData;
    }

    /**
     * Add request properties for the configuration, profiles,
     * and locale of this system.
     */
    public void setRequestHeaders (HttpConnection c) throws IOException {
/* //$ 	String conf = System.getProperty("microedition.configuration");
	String prof = System.getProperty("microedition.profiles");
	String locale = System.getProperty("microedition.locale");
	//String ua = "Profile/" + prof + " Configuration/" + conf;
	// Let's impersonate an iMode phone
	String ua = "DoCoMo/1.0/F503i/c10";
	c.setRequestProperty("User-Agent", ua);
        
	if (locale != null) {
	    c.setRequestProperty("Content-Language", locale);
	}*/
        c.setRequestProperty("User-Agent","Nokia2626/2.0 (0.6.80) Profile/MIDP-2.0 Configuration/CLDC-1.1");
        c.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
    }

    public byte[] httpPost (String url, String postData) throws IOException {
	return httpGetOrPost(url, postData, null);
    }

    public byte[] httpGet (String url) throws IOException {
	return httpGetOrPost(url, null, null);
    }

    /*
     * Get a document from the local (jar file) platform.
     *
     * URL should just be an absolute pathname into the jar file, such
     * as "/foo.html"
     *
     * @param url the URL to fetch 
     */

    public byte[] localGet (String url) throws IOException {
	InputStream is = getClass().getResourceAsStream(url);
	return readByteArrayFromStream(is);
    }

    private static byte[] bytebuf = new byte[0x1000];

    /*
     * Reads raw bytes from InputStream. Returns a byte array.
     *
     * Attempts to be efficient by reading the stream in buffered chunks.
     *
     * Uses a static preallocated temp buffer, bytebuf, so caller 
     * be cannot use this in a multithreaded environment.
     */
    public static byte[] readByteArrayFromStream (InputStream in) throws IOException {
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	for (;;) {
	    int len = in.read(bytebuf);
	    if (len < 0)
		break;
	    baos.write(bytebuf, 0, len);
	}
	return baos.toByteArray();

    }


    /**
     * 
     */
    void processType (String type) {
	httpMimeType = type;
    }

    public void myalert(String s){
            Alert a = new Alert("Error");
            a.setString(s);
	    a.setTimeout(Alert.FOREVER);
	    Display.getDisplay(app).setCurrent(a);
    }

    /****************************************************************
     * Minimal Servlet API
     ****************************************************************/
    Hashtable params = new Hashtable();

    /* Returns a table of key-value pairs from a URL. Looks for 
     * everything after the first '?' char.
     *
     */
    Hashtable getQueryParams (String url) {
	Hashtable h = params;
	h.clear();
	int start = url.indexOf('?');
	if (start >= 0) {
	    start++; // advance past ? to first key
	    boolean done = false;
	    while (!done) {
		int equalsign = url.indexOf('=', start);
		if (equalsign == -1) {
		    break;
		}
		String key = url.substring(start, equalsign);
		int nextKey = url.indexOf('&',start);
		String val;
		if (nextKey == -1) {
		    val = url.substring(equalsign+1);
		    done = true;
		} else {
		    val = url.substring(equalsign+1, nextKey);
		    start=nextKey+1;
		}
		h.put(key,HttpUtils.URLdecode(val));
	    }
	}
	return h;
    }

    /**
     * Execute servlet named NAME, return HTML result
     * <p>
     * URLs of the form "local://xxxx" are dispatched here. You can
     * modify code in this method to handle all your servlets. 
     *
     * <p>
     * You can use the utility method getQueryParams to parse the URL query
     * string of the form "servletname?key1=value1&amp;key2=value2&amp;..."
     * into a Hashtable of key-value pairs.
     * 
     */
    String handleServlet (String url) {
	if (url.startsWith("servlet")) {
	    Hashtable params = getQueryParams(url);
	    int n = Integer.parseInt((String)params.get("n"));
	    int n2 = Integer.parseInt((String)params.get("n2"));
	    return "<center>Local servlet!<br>n = " + n+"</center>"
		+ "<br><a href=\"local://servlet?n2="+n+"&n="+(n+n2)+"\">Fibonacci</a>";
	} else {
	    return "undefined servlet "+url;
	}
    }

}
