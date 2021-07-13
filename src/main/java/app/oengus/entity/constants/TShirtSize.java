package app.oengus.entity.constants;

public enum TShirtSize {
    XS("XS"),
    S("X"),
    M("M"),
    L("L"),
    XL("XL"),
    XXL("2XL"),
    XXXL("3XL"),
    XXXXL("4XL"),
    XXXXXL("5XL"),
    XXXXXXL("6XL");

    private final String display;

    TShirtSize(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }

    @Override
    public String toString() {
        return this.display;
    }
}
