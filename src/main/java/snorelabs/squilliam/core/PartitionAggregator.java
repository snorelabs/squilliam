package snorelabs.squilliam.core;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregates the results in a partition on their item type
 */
public class PartitionAggregator {
    /**
     * Aggregates the items by their dynamo item type, as described by the type attr.
     */
    public static Partition aggregate(String typeAttr, List<Map<String, AttributeValue>> items) {
        return new Partition(aggregates(typeAttr, items));
    }

    /**
     * Gets the list of aggregates for the given items.
     */
    private static List<DynamoAggregate> aggregates(String k, List<Map<String, AttributeValue>> v) {
        return group(v, k).entrySet().stream()
                .map(PartitionAggregator::dynamoAggregate)
                .collect(Collectors.toList());
    }

    /**
     * Groups the given items by their type attribute
     */
    private static Map<String, List<Map<String, AttributeValue>>> group(
            List<Map<String, AttributeValue>> items, String typeAttr) {
        return items.stream().collect(Collectors.groupingBy(map -> map.get(typeAttr).s()));
    }

    /**
     * Creates a DynamoAggregate for a given Entry
     */
    private static DynamoAggregate dynamoAggregate(Map.Entry<String, List<Map<String, AttributeValue>>> e) {
        return new DynamoAggregate(e.getKey(), e.getValue());
    }
}
