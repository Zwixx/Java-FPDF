package net.sourceforge.javafpdf.util;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

public class FontInfo {
	public String fontname;
	public int size1;
	public int size2;
	public String Weight;
	public Float ItalicAngle;
	public int Ascender;
	public int Descender;
	public int UnderlineThickness;
	public int UnderlinePosition;
	public boolean IsFixedPitch;
	public Rectangle FontBBox;
	public int CapHeight;
	public int StdVW;
	public byte[] data;
	public Map<String, Integer> CharWidth = new HashMap<>();
	public int MissingWidth;
	public int[] Widths;
	public boolean bold;
	public int index;
	public Boolean embed;
	public String diff;
	
	public Map<String, String> MakeFontDescriptor() {
		Map<String, String> result = new HashMap<>();
		// Ascent
		result.put("Ascent", Integer.toString(this.Ascender));
		// Descent
		result.put("Descent", Integer.toString(this.Descender));
		// CapHeight
		if(this.CapHeight != 0) {
			result.put("CapHeight", Integer.toString(this.CapHeight));
		} else {
			result.put("CapHeight", Integer.toString(this.Ascender));			
		}
		// Flags
		int flags = 0;
		if(this.IsFixedPitch)
			flags += 1<<0;
		flags += 1<<5;
		if(this.ItalicAngle != 0)
			flags += 1<<6;
		result.put("Flags", Integer.toString(flags));			
		// FontBBox
		Rectangle fbb = this.FontBBox;
		result.put("FontBBox", "[" + fbb.x + " " + fbb.y + " " + fbb.width + " " + fbb.height + "]");			
		// ItalicAngle
		result.put("ItalicAngle", Float.toString(this.ItalicAngle));			
		// StemV
		int stemv = 0;
		if(this.StdVW != 0)
			stemv = this.StdVW;
		else if (this.bold)
			stemv = 120;
		else
			stemv = 70;
		result.put("StemV", Integer.toString(stemv));			
		// MissingWidth
		result.put("MissingWidth", Integer.toString(this.MissingWidth));			
		return result;
	}
	
}
