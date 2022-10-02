package snorelabs.squilliam.core;

import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

public class DynamoUtils {
    /**
     * Creates a TableSchema for the provided model class
     */
    protected static <T> TableSchema<T> tableSchema(Class<T> itemClass) {
        return TableSchema.fromClass(itemClass);
    }
}
