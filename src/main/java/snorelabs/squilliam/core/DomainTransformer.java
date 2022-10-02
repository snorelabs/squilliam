package snorelabs.squilliam.core;

import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static snorelabs.squilliam.core.DynamoUtils.tableSchema;
import static snorelabs.squilliam.core.Predicates.isInDynamo;
import static snorelabs.squilliam.core.Shenanigans.getMembers;
import static snorelabs.squilliam.core.Shenanigans.relationFields;
import static snorelabs.squilliam.core.TargetDescriber.fieldClass;

public class DomainTransformer {
    /**
     * Transforms an object into a list of Dynamo items. A root can be a single item, a single item
     * with a number of relationships, or a container of a number of relationships. A relationship
     * can have a list of items or a single item. The algorithm iterates over the relationship
     * fields and appends all of the related items.
     * ============================================================================================
     * if (root is List):
     *     return to_items(root)
     * fields = relation_fields(root)
     * items = []
     * for (field in fields):
     *     if (is_many(field)):
     *         field_objs = field.get(root)
     *         curr_items = to_items(field_objs)
     *         items.add_all(curr_items)
     *     else:
     *         curr_item = to_item(field.get(root))
     *         items.add(curr_item)
     * if (is_dynamo_item(root)):
     *     items.add(to_item(root))
     * return items
     * ============================================================================================
     * This means a non dynamo item with no relationships would return an empty list (and that
     * should be okay).
     */
    public static List<Map<String, AttributeValue>> transform(Object root) {
        if (root instanceof List<?>) {
            return transformList((List<?>) root);
        }
        Stream<Map<String, AttributeValue>> members = relationFields(root.getClass())
                .flatMap(field -> items(root, field));
        return Stream.concat(members, rootMap(root)).collect(Collectors.toList());
    }

    /**
     * Transforms a list of some application class into a list of dynamo items.
     */
    protected static List<Map<String, AttributeValue>> transformList(List<?> items) {
        // Because of type erasure, we can't really get the class of the generic for the provided
        // list. Unfortunately, that means we need to create a schema per entry.
        return items.stream()
                .filter(item -> isInDynamo(item.getClass()))
                .map(item -> dynamoItem(tableSchema(item.getClass()), item))
                .collect(Collectors.toList());
    }

    /**
     * Gets a stream of the root item if it should be a DynamoDB represented item, otherwise an
     * empty stream
     */
    protected static <T> Stream<Map<String, AttributeValue>> rootMap(T root) {
        if (isInDynamo(root.getClass())) {
            return Stream.of(dynamoItem(tableSchema(root.getClass()), root));
        }
        return Stream.empty();
    }

    /**
     * The field class and the class of the items in the stream should be of the same type.
     */
    protected static Stream<Map<String, AttributeValue>> items(Object root, Field field) {
        TableSchema<?> schema = tableSchema(fieldClass(field));
        return getMembers(root, field).map(item -> dynamoItem(schema, item));
    }

    /**
     * Creates a dynamo map using the provided schema and object.
     */
    @SuppressWarnings("unchecked")
    private static <T> Map<String, AttributeValue> dynamoItem(TableSchema<T> schema, Object obj) {
        return schema.itemToMap((T)obj, false);
    }
}
