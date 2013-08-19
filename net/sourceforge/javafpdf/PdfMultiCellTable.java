package net.sourceforge.javafpdf;

/**
 * Extends the Java-FPDF with table functionality. Based on mctable.php
 * 
 * @author Olivier Plathey (original PHP)
 * @author Christian Froehlich
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PdfMultiCellTable extends FPDF {
	private List<Integer> widths = new ArrayList<Integer>();
	private List<Alignment> alignments = new ArrayList<Alignment>();

	/**
	 * Set the array of column widths
	 * @param widths The widths
	 */
	public void setWidths(List<Integer> widths) {
		this.widths = widths;
	}

	/**
	 * Set the array of column alignments
	 * @param align The Aligments
	 */
	public void setAligns(List<Alignment> align) {
		this.alignments = align;
	}

	/**
	 * Adds a new row to the table. The count og the columns must be the same as the
	 * Alidnments and the Widths
	 * @param data The Array of the text strings.
	 * @throws IOException If something goes wrong.
	 */
	public void row(List<String> data) throws IOException {
		if (widths.size() != data.size()) {
			throw new IllegalArgumentException("The number of rows (" + data.size()
					+ ") is not equal to the number of widths (" + widths.size() + ")");
		}
		if (alignments.size() != data.size()) {
			throw new IllegalArgumentException("The number of rows (" + data.size()
					+ ") is not equal to the number of alignments (" + alignments.size() + ")");
		}
		if (this.state != PDFCreationState.PAGE) {
			throw new PDFCreationError("Add rows are only availible in PAGE state");
		}
		// Calculate the height of the row
		float nb = 0;
		for (int i = 0; i < data.size(); i++)
			nb = Math.max(nb, this.NbLines(this.widths.get(i), data.get(i)));
		float height = 5 * nb;
		// Issue a page break first if needed
		this.CheckPageBreak(height);
		// Draw the cells of the row
		for (int i = 0; i < data.size(); i++) {
			float width = this.widths.get(i);
			Alignment a = this.alignments.get(i);
			// Save the current position
			float x = this.getX();
			float y = this.getY();
			// Draw the border
			this.Rect(new Coordinate(x, y), width, height, DrawMode.SHAPE);
			// Print the text
			String txt = data.get(i);
			if (txt == null) {
				txt = "";
			}
			this.MultiCell(width, 5, txt, new Borders(false, false, false, false), a, false);
			// Put the position to the right of the cell
			this.setXY(x + width, y);
		}
		// Go to the next line
		this.Ln(height);
	}

	public PdfMultiCellTable() {
		super();
	}

	public PdfMultiCellTable(float unit) {
		super(unit);
	}

	public PdfMultiCellTable(Format format) {
		super(format);
	}

	public PdfMultiCellTable(Orientation orientation, float unit, Format format) {
		super(orientation, unit, format);
	}

	public PdfMultiCellTable(Orientation orientation, float unit) {
		super(orientation, unit);
	}

	public PdfMultiCellTable(Orientation orientation, Format format) {
		super(orientation, format);
	}

	public PdfMultiCellTable(Orientation orientation) {
		super(orientation);
	}

	/**
	 * If the height would cause an overflow, add a new page immediately
	 * @param height The height of the cell
	 * @throws IOException If something goes wrong.
	 */
	private void CheckPageBreak(float height) throws IOException {
		if (this.getY() + height > this.pageBreakTrigger)
			this.addPage(this.currentOrientation);
	}
	
	/**
	 * Computes the number of lines a MultiCell of width will take
	 * @param width The Cell width
	 * @param txt The text
	 * @return number of Lines needed for this text.
	 */
	private int NbLines(float width, String txt) {
		if (this.currentFont == null) {
			throw new PDFCreationError("No default Font. Use SetFont to set a default Font.");
		}
		Font cw = this.currentFont;
		if (width == 0) {
			width = this.w - this.rMargin - this.x;
		}
		float wmax = (width - 2 * this.cMargin) * 1000 / this.fontSize;
		if (txt == null) {
			txt = "";
		}
		String s = txt.replaceAll("\r", "");
		int nb = s.length();
		if ((nb > 0) && (s.charAt(nb - 1) == '\n'))
			nb--;
		int sep = -1;
		int i = 0;
		int j = 0;
		int l = 0;
		int nl = 1;
		while (i < nb) {
			char c = s.charAt(i);
			if (c == '\n') {
				i++;
				sep = -1;
				j = i;
				l = 0;
				nl++;
				continue;
			}
			if (c == ' ')
				sep = i;
			l += cw.getCw().get(c);
			if (l > wmax) {
				if (sep == -1) {
					if (i == j)
						i++;
				} else
					i = sep + 1;
				sep = -1;
				j = i;
				l = 0;
				nl++;
			} else
				i++;
		}
		return nl;
	}
}