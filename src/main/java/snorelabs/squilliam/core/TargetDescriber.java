package snorelabs.squilliam.core;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.stream.Collectors;

import static snorelabs.squilliam.core.Predicates.isManyAnnotated;

public class TargetDescriber {
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
        return Shenanigans.relationFields(targetClass)
                .map(TargetDescriber::relation)
                .collect(Collectors.toMap(r -> Shenanigans.dynamoItemType(r.getModel()), r -> r));
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
        return isManyAnnotated(field) ? Shenanigans.listClass(field) : field.getClass();
    }

}
