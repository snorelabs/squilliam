package snorelabs.squilliam.core;

import snorelabs.squilliam.core.annotations.HasMany;
import snorelabs.squilliam.core.annotations.ItemType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class Predicates {
    public static boolean isSingular(DynamoAggregate aggregate) {
        return aggregate.getDynamoItems().size() == 1;
    }

    public static boolean isInDynamo(Class<?> item) {
        return hasAnnotation(item, ItemType.class);
    }

    public static boolean isManyAnnotated(Field field) {
        return hasAnnotation(field, HasMany.class);
    }

    public static boolean hasAnnotation(Class<?> item, Class<? extends Annotation> annotation) {
        return item.isAnnotationPresent(annotation);
    }

    public static boolean hasAnnotation(Field field, Class<? extends Annotation> annotation) {
        return field.isAnnotationPresent(annotation);
    }

    public static boolean isItemType(DynamoAggregate aggregate, String itemType) {
        return aggregate.getItemType().equals(itemType);
    }
}
