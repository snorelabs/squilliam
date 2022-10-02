package snorelabs.squilliam.core;

import snorelabs.squilliam.core.annotations.ItemType;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static snorelabs.squilliam.core.Predicates.isManyAnnotated;

/**
 * Once in a while, given the right circumstances and a language that doesn't let you just do what
 * you want to do... You need to introduce some shenanigans. Shenanigans are things that would make
 * me say "Hold up, why are we doing this? There's gotta be another way... Can't we just sell our
 * souls to the Java overlords or something? Fiddle competition with the devil? Anything????" in a
 * code review. Now, if I were a good human being I'd be doing code gen. If I were a decent human
 * being maybe I'd start messing with javac. I'm neither of those. I am the bringer of chaos. And
 * I ain't got time to do things all fancy-like, ya hear? So anyway, Shenanigans. Things we should
 * not do, but we be doing em anyway.
 */
public class Shenanigans {
    /**
     * This is a side effect method which reflectively sets the value onto the field of the
     * provided root instance. Although unfortunate, Java doesn't natively support creating new
     * instances with the additional field set without the client implementing a builder for it.
     * Supporting immutables is on the radar but not currently handled.
     */
    protected static <T> void setMember(T root, Field field, Object val) {
        try {
            field.setAccessible(true);
            field.set(root, val);
        } catch (IllegalAccessException e) {
            throw new ModelException("Unable to assign field", root.getClass(), e);
        }
    }

    protected static Stream<?> getMembers(Object root, Field field) {
        try {
            field.setAccessible(true);
            return isManyAnnotated(field)
                    ? ((List<?>)field.get(root)).stream()
                    : Stream.of(field.get(root));
        } catch (IllegalAccessException e) {
            throw new ModelException("Unable to get field", root.getClass(), e);
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
     * Gets all relation fields. Relation fields are fields on the target which come from other
     * records in Dynamo, marked on the model by the appropriate relation annotation
     */
    protected static Stream<Field> relationFields(Class<?> targetClass) {
        return allFields(targetClass).filter(Predicates::isRelationship);
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
                .orElseThrow(() -> new ModelException("No item type identifier", item));
    }

    /**
     * Gets the class of a generic, which should be a collection. This lets us get the type of the
     * individual DynamoDB representations.
     */
    public static Class<?> listClass(Field field) {
        return (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
    }
}
