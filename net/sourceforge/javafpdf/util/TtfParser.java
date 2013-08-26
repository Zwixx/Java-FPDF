/**
 * Utility to parse TTF font files                                              
 *                                                                              
 * @author  Olivier PLATHEY                                                     
 * @version 1.0                                                                 
 * @since   2011-06-18
 */
package net.sourceforge.javafpdf.util;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.javafpdf.fonts.FontType;

public class TtfParser {
	private Map<String, Integer> tables = new HashMap<>();
	private int unitsPerEm;
	private int xMin;
	private int yMin;
	private int xMax;
	private int yMax;
	private int numberOfHMetrics;
	private int numGlyphs;
	private int[] widths;
	private String postScriptName;
	private boolean Embeddable;
	private boolean Bold;
	private int typoAscender;
	private int typoDescender;
	private int capHeight;
	private float italicAngle;
	private int underlinePosition;
	private int underlineThickness;
	private boolean isFixedPitch;
	private RandomAccessFile ttf;
	private int[] chars;

	private void Parse(File file) throws IOException {
		ttf = new RandomAccessFile(file, "r");

		byte[] version = new byte[4];
		ttf.read(version);
		if (Arrays.equals(version, new byte[] { 'O', 'T', 'T', 'O' })) {
			throw new UnsupportedOperationException(
					"OpenType fonts based on PostScript outlines are not supported");
		}
		if (!Arrays.equals(version, new byte[] { 0, 1, 0, 0 })) {
			throw new UnsupportedOperationException("Unrecognized file format");
		}
		int numTables = ttf.readUnsignedShort();
		// searchRange, entrySelector, rangeShift
		ttf.skipBytes(3 * 2);
		for (int i = 0; i < numTables; i++) {
			byte tag[] = new byte[4];
			ttf.read(tag);
			// checkSum
			ttf.skipBytes(4);
			int offset = ttf.readInt();
			// length
			ttf.skipBytes(4);
			this.tables.put(new String(tag, "iso-8859-1"), offset);
		}

		this.ParseHead();
		this.ParseHhea();
		this.ParseMaxp();
		this.ParseHmtx();
		this.ParseCmap();
		this.ParseName();
		this.ParseOS2();
		this.ParsePost();

		ttf.close();
	}

	private void ParseHead() throws IOException {
		this.Seek("head");
		// version, fontRevision, checkSumAdjustment
		ttf.skipBytes(3 * 4);
		int magicNumber = ttf.readInt();
		if (magicNumber != 0x5F0F3CF5) {
			throw new IllegalArgumentException("Incorrect magic number");
		}
		// flags
		ttf.skipBytes(2);
		unitsPerEm = ttf.readShort();
		// created, modified
		ttf.skipBytes(2 * 8);
		xMin = ttf.readShort();
		yMin = ttf.readShort();
		xMax = ttf.readShort();
		yMax = ttf.readShort();
	}

	private void ParseHhea() throws IOException {
		Seek("hhea");
		ttf.skipBytes(4 + 15 * 2);
		numberOfHMetrics = ttf.readShort();
	}

	private void ParseMaxp() throws IOException {
		Seek("maxp");
		ttf.skipBytes(4);
		numGlyphs = ttf.readShort();
	}

	private void ParseHmtx() throws IOException {
		Seek("hmtx");
		widths = new int[Math.max(Math.max(numGlyphs, numberOfHMetrics), 256)];
		for (int i = 0; i < numberOfHMetrics; i++) {
			int advanceWidth = ttf.readShort();
			ttf.skipBytes(2); // lsb
			widths[i] = advanceWidth;
		}
		if (numberOfHMetrics < numGlyphs) {
			int lastWidth = widths[numberOfHMetrics - 1];
			Arrays.fill(widths, lastWidth, numberOfHMetrics, numGlyphs);
		}
	}

	private void ParseCmap() throws IOException {
		Seek("cmap");
		ttf.skipBytes(2); // version
		int numTables = ttf.readShort();
		long offset31 = 0;
		for (int i = 0; i < numTables; i++) {
			int platformID = ttf.readShort();
			int encodingID = ttf.readShort();
			int offset = ttf.readInt();
			if (platformID == 3 && encodingID == 1)
				offset31 = offset;
		}
		if (offset31 == 0) {
			throw new IllegalArgumentException("No Unicode encoding found");
		}

		ttf.seek(tables.get("cmap") + offset31);
		int format = ttf.readShort();
		if (format != 4) {
			throw new IllegalArgumentException("Unexpected subtable format: "
					+ format);
		}
		ttf.skipBytes(2 * 2); // length, language
		int segCount = ttf.readShort() / 2;
		int[] startCount = new int[segCount];
		int[] endCount = new int[segCount];
		int[] idDelta = new int[segCount];
		int[] idRangeOffset = new int[segCount];
		this.chars = new int[256];

		ttf.skipBytes(3 * 2); // searchRange, entrySelector, rangeShift
		for (int i = 0; i < segCount; i++)
			endCount[i] = ttf.readShort();
		ttf.skipBytes(2); // reservedPad
		for (int i = 0; i < segCount; i++)
			startCount[i] = ttf.readShort();
		for (int i = 0; i < segCount; i++)
			idDelta[i] = ttf.readShort();
		long offset = ttf.getFilePointer();
		for (int i = 0; i < segCount; i++)
			idRangeOffset[i] = ttf.readShort();

		for (int i = 0; i < segCount; i++) {
			int c1 = startCount[i];
			int c2 = endCount[i];
			int d = idDelta[i];
			int ro = idRangeOffset[i];
			if (ro > 0) {
				ttf.seek(offset + 2 * i + ro);
			}
			for (int c = c1; c <= c2; c++) {
				if (c <= 255 && c >= 0) {
					int gid;
					if (c == 0xFFFF)
						break;
					if (ro > 0) {
						gid = ttf.readShort();
						if (gid > 0) {
							gid += d;
						}
					} else {
						gid = c + d;
					}
					if (gid >= 65536) {
						gid -= 65536;
					}
					if (gid > 0) {
						this.chars[c] = gid;
					}
				}
			}
		}
	}

	private void ParseName() throws IOException {
		Seek("name");
		long tableOffset = this.tables.get("name");
		this.postScriptName = "";
		ttf.skipBytes(2); // format
		int count = ttf.readShort();
		int stringOffset = ttf.readShort();
		for (int i = 0; i < count; i++) {
			ttf.skipBytes(3 * 2); // platformID, encodingID, languageID
			int nameID = ttf.readShort();
			int length = ttf.readShort();
			int offset = ttf.readShort();
			if (nameID == 6) {
				// PostScript name
				ttf.seek(tableOffset + stringOffset + offset);
				byte[] buffer = new byte[length];
				ttf.read(buffer);
				String s = new String(buffer, "iso-8859-1");
				s = s.replace((char) 0, ' ');
				s = s.replaceAll("[ \\[\\](){}<>/%]", "");
				this.postScriptName = s.trim();
				break;
			}
		}
		if (this.postScriptName.equals(""))
			throw new IllegalArgumentException("PostScript name not found");
	}

	private void ParseOS2() throws IOException {
		Seek("OS/2");
		int version = ttf.readShort();
		ttf.skipBytes(3 * 2); // xAvgCharWidth, usWeightClass, usWidthClass
		int fsType = ttf.readShort();
		this.Embeddable = (fsType != 2) && (fsType & 0x200) == 0;
		ttf.skipBytes(11 * 2 + 10 + 4 * 4 + 4);
		int fsSelection = ttf.readShort();
		this.Bold = (fsSelection & 32) != 0;
		ttf.skipBytes(2 * 2); // usFirstCharIndex, usLastCharIndex
		this.typoAscender = ttf.readShort();
		this.typoDescender = ttf.readShort();
		if (version >= 2) {
			ttf.skipBytes(3 * 2 + 2 * 4 + 2);
			this.capHeight = ttf.readShort();
			;
		} else {
			this.capHeight = 0;
		}
	}

	private void ParsePost() throws IOException {
		Seek("post");
		// version
		ttf.skipBytes(4);
		italicAngle = ttf.readShort();
		// Skip decimal part
		ttf.skipBytes(2);
		underlinePosition = ttf.readShort();
		underlineThickness = ttf.readShort();
		isFixedPitch = ttf.readInt() != 0;
	}

	private void Seek(String tag) throws IOException {
		if (!tables.containsKey(tag))
			throw new IllegalArgumentException("Table not found: " + tag);
		ttf.seek(tables.get(tag));
	}
	
	public FontInfo GetInfoFromTrueType(File file, Boolean embed) throws IOException {
		// Return informations from a TrueType font
		Parse(file);
		FontInfo result = new FontInfo();
		result.type = FontType.TTF;
		if (embed) {
			if(!this.Embeddable) {
				throw new IllegalArgumentException("Font license does not allow embedding");
			}
			FileInputStream fin = new FileInputStream(file);
			result.data = new byte[(int)file.length()];
			fin.read(result.data);
			fin.close();
			result.size1 = result.data.length;
			result.size2 = 0;
		} else {
			result.size1 = 0;
			result.size2 = 0;
		}
		int k = 1000 / this.unitsPerEm;
		result.fontname = this.postScriptName;
		result.bold = this.Bold;
		result.ItalicAngle = this.italicAngle;
		result.IsFixedPitch = this.isFixedPitch;
		result.Ascender = Math.round(k * this.typoAscender);
		result.Descender = Math.round(k * this.typoDescender);
		result.UnderlineThickness = Math.round(k * this.underlineThickness);
		result.UnderlinePosition = Math.round(k * this.underlinePosition);
		result.FontBBox = new Rectangle(Math.round(k * this.xMin), Math.round(k * this.yMin), Math.round(k * this.xMax), Math.round(k * this.yMax));
		result.CapHeight = Math.round(k * this.capHeight);
		result.MissingWidth = Math.round(k * this.widths[0]);
		result.Widths = this.widths;
		return result;
	}

}
