package net.sourceforge.javafpdf.fonts;

/**
 * Font types.
 * 
 * @author Alan Plum
 * @since 4 Mar 2008
 * @version $Revision: 1.2 $
 */
public enum FontType {
	/** Core font. */
	CORE("Type1"),
	/** TrueType Font. */
	TTF("TrueType"),
	/** Postscript Type1 Font. */
	TYPE1("Type1");
	
	private String pdfString;
	
	FontType(String pdfString) {
		this.pdfString = pdfString;
	}

	public String toPdfString() {
		return this.pdfString;
	}
}