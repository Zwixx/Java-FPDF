package net.sourceforge.javafpdf.util;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;

import net.sourceforge.javafpdf.fonts.CharInfo;
import net.sourceforge.javafpdf.fonts.FontType;

public class Type1Parser {
	private int readLittleIndianInt(RandomAccessFile file) throws IOException {
		byte[] buffer = new byte[4]; 
		file.read(buffer);
		return (buffer[0] & 0xFF) | (buffer[1] & 0xFF) << 8 | (buffer[2] & 0xFF) << 16 | (buffer[3] & 0xFF) << 24;
	}
	
	private byte[] concat(byte[] A, byte[] B) {
		int aLen = A.length;
		int bLen = B.length;
		byte[] C = new byte[aLen + bLen];
		System.arraycopy(A, 0, C, 0, aLen);
		System.arraycopy(B, 0, C, aLen, bLen);
		return C;
	}

	private CharInfo[] LoadMap(String enc) {
		InputStream is = this.getClass().getResourceAsStream("../charsets/" + enc + ".map"); 
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));

		CharInfo[] map = new CharInfo[256];
		try {
			if (!reader.ready())
				throw new IllegalArgumentException("Encoding not found: " + enc);
			for (int i = 0; i < 256; i++) {
				map[i] = new CharInfo(".notdef", -1);
			}
			String line;
			while ((line = reader.readLine()) != null) {
				String[] e = line.trim().split(" ");
				int c = Integer.valueOf(e[0].substring(1), 16);
				int uv = Integer.valueOf(e[1].substring(2), 16);
				String name = e[2];
				map[c] = new CharInfo(name, uv);
			}
			reader.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return map;
	}
	
	FontInfo GetInfoFromType1(File file, Boolean embed) throws IOException {
		// Return informations from a Type1 font
		FontInfo font = new FontInfo();
		font.type = FontType.TYPE1;
		font.embed = embed;
		if(embed) {
			RandomAccessFile f = new RandomAccessFile(file, "r");
			// Read first segment
			byte marker = f.readByte();
			byte type = f.readByte();
			int size = readLittleIndianInt(f); // eigentlich nicht Vorzeichenbehaftet!!
			if(marker != -128) {
				throw new IllegalArgumentException("Font file is not a valid binary Type1");
			}
			font.size1 = size;
			byte[] data1 = new byte[font.size1];
			f.read(data1);
			// Read second segment
			marker = f.readByte();
			type = f.readByte();
			size = readLittleIndianInt(f); // eigentlich nicht Vorzeichenbehaftet!!
			if(marker != -128)
				throw new IllegalArgumentException("Font file is not a valid binary Type1");
			font.size2 = size;
			byte[] data2 = new byte[font.size2];
			f.read(data2);
			f.close();
			font.data = concat(data1, data2);
		}

		File afm = new File(file.toString().substring(0, file.toString().length() - 3) + "afm");
		if(!afm.exists())
			throw new IllegalArgumentException("AFM font file not found: " + afm.getName());
		BufferedReader reader = null;
		
		try {
			reader = new BufferedReader(new FileReader(afm));
			if (!reader.ready()) {
				throw new IllegalArgumentException("AFM font file is empty: " + afm.getName());
			}
			String line;
			
			while ((line = reader.readLine()) != null) {
				String[] e = line.trim().split(" ");
				if(e.length < 2)
					continue;
				String entry = e[0];
				if(entry.equals("C")) {
					int w = Integer.valueOf(e[4]);
					String name = e[7];
					font.CharWidth.put(name, w);
				} else
					if(entry.equals("FontName"))
						font.fontname = e[1];
				else
					if(entry.equals("Weight"))
					font.Weight = e[1];
				else
					if(entry.equals("ItalicAngle"))
					font.ItalicAngle = Float.valueOf(e[1]);
				else
					if(entry.equals("Ascender"))
					font.Ascender = Integer.valueOf(e[1]);
				else
					if(entry.equals("Descender"))
					font.Descender = Integer.valueOf(e[1]);
				else
					if(entry.equals("UnderlineThickness"))
					font.UnderlineThickness = Integer.valueOf(e[1]);
				else
					if(entry.equals("UnderlinePosition"))
					font.UnderlinePosition = Integer.valueOf(e[1]);
				else
					if(entry.equals("IsFixedPitch"))
					font.IsFixedPitch = e[1].equals("true");
				else
					if(entry.equals("FontBBox"))
					font.FontBBox = new Rectangle(Integer.valueOf(e[1]), Integer.valueOf(e[2]), Integer.valueOf(e[3]), Integer.valueOf(e[4]));
				else
					if(entry.equals("CapHeight"))
					font.CapHeight = Integer.valueOf(e[1]);
				else
					if(entry.equals("StdVW"))
					font.StdVW = Integer.valueOf(e[1]);
			}
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				throw e;
			}
		}

		if(font.fontname == null)
			throw new IllegalArgumentException("FontName missing in AFM file");
		font.bold = (font.Weight != null) && (font.Weight.matches("/bold|black/i"));
		if(font.CharWidth.containsKey(".notdef"))
			font.MissingWidth = font.CharWidth.get(".notdef");
		else
			font.MissingWidth = 0;
		int widths[] = new int[256];
		
		CharInfo[] chatMap = LoadMap("iso-8859-1");
		
		for(int c = 0; c <= 255; c++) {
			String name = chatMap[c].name;
			if(!name.equals(".notdef"))
			{
				if(font.CharWidth.containsKey(name))
					widths[c] = font.CharWidth.get(name);
				else
					widths[c] = font.MissingWidth;
			}
		}
		font.Widths = widths;
		return font;
	}
}
