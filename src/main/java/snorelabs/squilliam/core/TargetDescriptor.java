package snorelabs.squilliam.core;

import snorelabs.squilliam.core.annotations.ItemType;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static snorelabs.squilliam.core.Predicates.isManyAnnotated;

public class TargetDescriptor {
    /**
     * This method reflectively describes the target class. The process is fairly straightforward,
     * we iterate through the inheritance chain and retrieve all fields which are relation
     * annotated. The next step is to create a map of the relevant class's item type.
     */
    public static <T> TransformTarget<T> describe(Class<T> targetClass) {
        return new TransformTarget<>(targetClass, allRelations(targetClass));
    }

    /**
     * Returns all relationships of the class as a map of their item type to relation.
     */
    public static Map<String, Relation> allRelations(Class<?> targetClass) {
        return relationFields(targetClass)
                .map(TargetDescriptor::relation)
                .collect(Collectors.toMap(r -> itemType(r.getModel()), r -> r));
    }

    /**
     * Gets all relation fields. Relation fields are fields on the target which come from other
     * records in Dynamo, marked on the model by the appropriate relation annotation
     */
    private static Stream<Field> relationFields(Class<?> targetClass) {
        return allFields(targetClass).filter(Predicates::isRelationship);
    }

    /**
     * Gets all declared fields of the target class and follows up the inheritance chain to get
     * all fields of every base class.
     */
    private static Stream<Field> allFields(Class<?> targetClass) {
        Stream<Field> fields = Stream.of();
        Class<?> currClass = targetClass;
        while (!Objects.isNull(currClass)) {
            fields = Stream.concat(fields, Stream.of(currClass.getDeclaredFields()));
            currClass = currClass.getSuperclass();
        }
        return fields;
    }

    /**
     * Creates a relation (Class + Field pairing) for the given field.
     */
    public static Relation relation(Field field) {
        return new Relation(fieldClass(field), field);
    }

    /**
     * Gets the class for the given relation field. Some fields are collections, and therefore need
     * some special treatment to get the class which represents an individual row.
     */
    public static Class<?> fieldClass(Field field) {
        return isManyAnnotated(field) ? listClass(field) : field.getClass();
    }

    /**
     * Gets the class of a generic, which should be a collection. This lets us get the type of the
     * individual DynamoDB representations.
     */
    public static Class<?> listClass(Field field) {
        return (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
    }

    /**
     * Gets the item type for a given class.
     * TODO: This is overlapping reflection/annotation shenanigans which should be refactored.
     */
    public static String itemType(Class<?> itemClass) {
        return itemClass.getAnnotation(ItemType.class).value();
    }
}
