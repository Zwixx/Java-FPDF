package net.sourceforge.javafpdf.fonts;

/**
 * Information about a single Character
 * 
 * @author Christian
 *
 */

public class CharInfo {
	public final String name;
	public final int uv;
	
	public CharInfo(String name, int uv) {
		super();
		this.name = name;
		this.uv = uv;
	}

	@Override
	public String toString() {
		return "CharInfo [name=" + name + ", uv=" + uv + "]";
	}
}
