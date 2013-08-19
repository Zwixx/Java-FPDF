package net.sourceforge.javafpdf;

public class FontFamily {

	public static final FontFamily ARIAL = new FontFamily("arial"); // arial == helvetica
	public static final FontFamily COURIER = new FontFamily("courier");
	public static final FontFamily HELVETICA = new FontFamily("helvetica");
	public static final FontFamily TIMES = new FontFamily("times");
	public static final FontFamily SYMBOL = new FontFamily("symbol");
	public static final FontFamily ZAPF_DINGBATS = new FontFamily("zapfdingbats");
    private final String key;

    public FontFamily(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return key;
    }

    public String getKey() {
        return key;
    }

    public String getBoldKey() {
        return key + FontStyle.BOLD.getOp();
    }

    public String getItalicKey() {
        return key + FontStyle.ITALIC.getOp();
    }

    public String getBoldItalicKey() {
        return key + FontStyle.BOLD.getOp() + FontStyle.ITALIC.getOp();
    }
}
