package snorelabs.squilliam.core;

import java.lang.reflect.Field;

public class Relation {
    private Class<?> model;
    private Field field;

    public Relation(Class<?> model, Field field) {
        this.model = model;
        this.field = field;
    }

    public Field getField() {
        return field;
    }

    public Class<?> getModel() {
        return model;
    }
}
