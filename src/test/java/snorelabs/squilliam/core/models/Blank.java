package snorelabs.squilliam.core.models;

public class Blank {
    private final String val;
    public static String STATIC_VAL = "Hello";

    public Blank() {
        this.val = STATIC_VAL;
    }

    public String getVal() {
        return val;
    }
}