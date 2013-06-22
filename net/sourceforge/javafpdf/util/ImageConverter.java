package net.sourceforge.javafpdf.util;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import net.sourceforge.javafpdf.ImageType;

public class ImageConverter {

	private File file;

	public ImageConverter(File file) throws FileNotFoundException {
		this.file = file;
	}

	public ImageConverter(String file) throws FileNotFoundException {
		this(new File(file));
	}

	private Map<String, Object> parsejpg(InputStream input) {
		BufferedImage img = null;
		try {
			input.mark(input.available() + 1);
			img = ImageIO.read(input);

			Map<String, Object> image = new HashMap<String, Object>();
			image.put("w", Integer.valueOf(img.getWidth()));
			image.put("h", Integer.valueOf(img.getHeight()));
			String colspace;
			if (img.getColorModel().getColorSpace().getType() == ColorSpace.TYPE_CMYK) {
				colspace = "DeviceCMYK";
			} else if (img.getColorModel().getColorSpace().getType() == ColorSpace.TYPE_RGB) {
				colspace = "DeviceRGB";
			} else if (img.getColorModel().getColorSpace().getType() == ColorSpace.TYPE_GRAY) {
				colspace = "DeviceGray";
			} else {
				throw new IllegalArgumentException("Ungültiges Farbmodell "
						+ img.getColorModel().getColorSpace().getType());
			}
			image.put("cs", colspace);
			image.put("bpc", 8);
			image.put("f", "DCTDecode");

			input.reset();
			byte[] data = new byte[input.available()];
			input.read(data, 0, input.available());
			input.close();
			image.put("data", data);
			return image;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/** Extract info from a PNG file */
	private Map<String, Object> parsepng(InputStream input) throws IOException {
		FileAccess f = new FileAccess(input);
		try {
			// Check signature
			char[] sig = new char[] { 137, 'P', 'N', 'G', 13, 10, 26, 10 };
			for (int i = 0; i < sig.length; i++) {
				int in = f.read();
				char c = (char) in;
				if (c != sig[i]) {
					throw new IOException("Not a PNG file: " + file);
				}
			}
			f.fread(4);
			// Read header chunk
			char[] chunk = new char[] { 'I', 'H', 'D', 'R' };
			for (int i = 0; i < chunk.length; i++) {
				int in = f.read();
				char c = (char) in;
				if (c != chunk[i]) {
					throw new IOException("Not a PNG file: " + file);
				}
			}
			int w = f.freadint();
			int h = f.freadint();
			int bpc = f.read();
			if (bpc > 8) {
				throw new IOException("16-bit depth not supported: " + file);
			}
			int ct = f.read();
			String colspace;
			if (ct == 0) {
				colspace = "DeviceGray";
			} else if (ct == 2) {
				colspace = "DeviceRGB";
			} else if (ct == 3) {
				colspace = "Indexed";
			} else {
				throw new IOException("Alpha channel not supported: " + file);
			}
			if (f.read() != 0) {
				throw new IOException("Unknown compression method: " + file);
			}
			if (f.read() != 0) {
				throw new IOException("Unknown filter method: " + file);
			}
			if (f.read() != 0) {
				throw new IOException("Interlacing not supported: " + file);
			}
			f.fread(4);
			StringBuilder sb = new StringBuilder();
			sb.append("/DecodeParms <</Predictor 15 /Colors ").append(ct == 2 ? 3 : 1).append(" /BitsPerComponent ")
					.append(bpc).append(" /Columns ").append(w).append(">>");
			String parms = sb.toString();
			// Scan chunks looking for palette, transparency and image data
			byte[] pal = null;
			byte[] trns = null;
			byte[] data = null;
			do {
				int n = f.freadint();
				String type = new String(f.fread(4));
				if (type.equals("PLTE")) {
					// Read palette
					pal = f.freadb(n);
					f.fread(4);
				} else if (type.equals("tRNS")) {
					// Read transparency info
					byte[] t = f.freadb(n);
					if (ct == 0) {
						trns = new byte[] { t[1] };
					} else if (ct == 2) {
						trns = new byte[] { t[1], t[3], t[5] };
					} else {
						int pos = new String(t).indexOf(0);
						if (pos != -1) {
							trns = new byte[] { (byte) pos };
						}
					}
					f.fread(4);
				} else if (type.equals("IDAT")) {
					// Read image data block
					data = f.freadb(n, data);
					f.fread(4);
				} else if (type.equals("IEND")) {
					break;
				} else {
					f.fread(n + 4);
				}
			} while (f.available() > 0);
			if (colspace.equals("Indexed") && (pal == null)) {
				throw new IOException("Missing palette in " + file);
			}
			Map<String, Object> image = new HashMap<String, Object>();
			image.put("w", Integer.valueOf(w));
			image.put("h", Integer.valueOf(h));
			image.put("cs", colspace);
			image.put("bpc", Integer.valueOf(bpc));
			image.put("f", "FlateDecode");
			image.put("parms", parms);
			image.put("pal", pal);
			image.put("trns", trns);
			image.put("data", data);
			// image.put("i", index);
			f.close();
			return image;
		} finally {
			f.close();
		}
	}

	/** Extract info from a PNG file */
	private Map<String, Object> parsegif(InputStream input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		ImageIO.write(ImageIO.read(input), "png", output);

		return parsepng(new ByteArrayInputStream(output.toByteArray()));
	}

	public Map<String, Object> parseimage(ImageType type) throws IOException {
		BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));

		switch (type) {
		case JPEG:
			return parsejpg(input);
		case GIF:
			return parsegif(input);
		case PNG:
			return parsepng(input);
		default:
			return null;
		}
	}
}
