package net.sourceforge.javafpdf;

public enum FontFamily {

    ARIAL("arial"), // arial == helvetica
    COURIER("courier"),
    HELVETICA("helvetica"),
    TIMES("times"),
    SYMBOL("symbol"),
    ZAPF_DINGBATS("zapfdingbats");
    private final String key;

    private FontFamily(String key) {
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
