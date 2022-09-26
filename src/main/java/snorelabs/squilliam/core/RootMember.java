package snorelabs.squilliam.core;

import java.lang.reflect.Field;

public class RootMember {
    private Field field;
    private Object val;

    public RootMember(Field field, Object val) {
        this.field = field;
        this.val = val;
    }

    public Field getField() {
        return field;
    }

    public Object getVal() {
        return val;
    }
}
