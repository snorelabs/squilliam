package snorelabs.squilliam.core;

import snorelabs.squilliam.core.annotations.ItemType;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.stream.Stream;

public class Shenanigans {

    /**
     * This is a side effect method which reflectively sets the value onto the field of the
     * provided root instance. Although unfortunate, Java doesn't natively support creating new
     * instances with the additional field set without the client implementing a builder for it.
     */
    protected static <T> void setMember(T root, Field field, Object val) {
        try {
            field.setAccessible(true);
            field.set(root, val);
        } catch (IllegalAccessException e) {
            throw new ModelException("Unable to assign field", root.getClass(), e);
        }
    }

    /**
     * Creates an instance of the target class using the default constructor.
     */
    protected static <T> T defaultInstance(Class<T> targetClass) {
        try {
            return targetClass.getDeclaredConstructor().newInstance();
        } catch (InvocationTargetException
                 | InstantiationException
                 | IllegalAccessException
                 | NoSuchMethodException e) {
            throw new ModelException("Unable to create instance of target", targetClass, e);
        }
    }

    /**
     * Gets all declared fields of the target class and follows up the inheritance chain to get
     * all fields of every base class.
     */
    protected static Stream<Field> allFields(Class<?> targetClass) {
        Stream<Field> fields = Stream.of();
        Class<?> currClass = targetClass;
        while (!Objects.isNull(currClass)) {
            fields = Stream.concat(fields, Stream.of(currClass.getDeclaredFields()));
            currClass = currClass.getSuperclass();
        }
        return fields;
    }

    /**
     * Gets the item type string (used to identify what type of item the dynamo record is)
     */
    protected static String dynamoItemType(Class<?> item) {
        return allFields(item)
                .filter(Predicates::isItemIdentifier)
                .findFirst()
                .map(itemField -> itemField.getAnnotation(ItemType.class).value())
                .orElseThrow(() -> new ModelException("No item identifier", item));
    }
}
