package net.sourceforge.javafpdf.util;

import java.io.File;
import java.io.IOException;

public class FontInfoRequest {
	
	public FontInfo info;
	
	public FontInfoRequest(File font) {
		try {
			if (font.getName().endsWith(".ttf")) {
				TtfParser ttf = new TtfParser();
				info = ttf.GetInfoFromTrueType(font, true);
			} else {
				Type1Parser type1 = new Type1Parser();
				info = type1.GetInfoFromType1(font, true);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
