package net.sourceforge.javafpdf.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class FileAccess {
	
	InputStream input;
	
	public FileAccess(File file) throws FileNotFoundException {
		this.input = new FileInputStream(file);
	}
	
	public FileAccess(InputStream input) {
		this.input = input;
	}
	
	int read() throws IOException {
		return input.read();
	}
	
	/**
	 * Equivalent of PHP fread().
	 * 
	 * @throws IOException
	 *             if the stream can not be read.
	 */
	public char[] fread(final int length) throws IOException {
		char[] chars = new char[length];
		for (int i = 0; i < length; i++) {
			int in = input.read();
			chars[i] = (char) in;
		}
		return chars;
	}

	public byte[] freadb(final int length) throws IOException {
		byte[] bytes = new byte[length];
		input.read(bytes);
		return bytes;
	}

	/**
	 * Reads bytes from a given stream and appends them to a given array, then
	 * returns the combined array.
	 */
	public byte[] freadb(final int length, final byte[] bytes) throws IOException {
		byte[] b;
		int offset;
		if (bytes != null) {
			b = new byte[bytes.length + length];
			for (int i = 0; i < bytes.length; i++) {
				b[i] = bytes[i];
			}
			offset = bytes.length;
		} else {
			b = new byte[length];
			offset = 0;
		}
		input.read(b, offset, length);
		return b;
	}

	/**
	 * Read a 4-byte integer from file
	 * 
	 * @throws IOException
	 *             if the stream can not be read.
	 */
	public int freadint() throws IOException {
		// We'll assume big-endian encoding here.
		int a = 0;
		for (int i = 0; i < 4; i++) {
			int shift = (4 - 1 - i) * 8;
			int in = input.read();
			byte b = (byte) in;
			a += (b & 0x000000FF) << shift;
		}
		return a;
	}

	public int available() throws IOException {
		return input.available();
	}

	public void close() throws IOException {
		input.close();
	}
}
