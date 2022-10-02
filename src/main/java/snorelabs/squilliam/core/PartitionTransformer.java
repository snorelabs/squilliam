package snorelabs.squilliam.core;

import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static snorelabs.squilliam.core.DynamoUtils.tableSchema;
import static snorelabs.squilliam.core.Predicates.isInDynamo;
import static snorelabs.squilliam.core.Predicates.isItemType;
import static snorelabs.squilliam.core.Predicates.isManyAnnotated;


public class PartitionTransformer {
    /**
     *
     * Transforms the provided partition into the target described by the transform target. The
     * basic process is to take the intersection of the partition and the transform target fields
     * and set those fields for an aggregate root. The algorithm works in the following way:
     * ============================================================================================
     * root = null;
     * if is_dynamo_item(root_model):
     *     root = instance(find(root_type, partition))
     * else:
     *     root = default()
     * for member, field in zip(partition, target):
     *     field.set(root, member)
     * return root
     *=============================================================================================
     * This presents an interesting caveat, fields will only be set in the case that they have
     * available data in the retrieved DynamoDB Partition.
     *
     * @param partition - Partition containing retrieved DynamoDB data.
     * @param target - TransformTarget to aggregate the partition results into.
     * @return An instance of the root class with the related fields set.
     * @param <T> Class of the desired target instance
     */
    public static <T> T transform(Partition partition, TransformTarget<T> target) {
        T root = rootInstance(partition, target.getModel());
        for (RootMember member : zip(partition.getAggregates(), target.getRelations())) {
            Shenanigans.setMember(root, member.getField(), member.getVal());
        }
        return root;
    }

    /**
     * Creates an array of instances of the target model from the provided DynamoDB data.
     */
    public static <T> List<T> instances(List<Map<String, AttributeValue>> vals, Class<T> model) {
        TableSchema<T> schema = tableSchema(model);
        return vals.stream().map(val -> instance(schema, val)).collect(Collectors.toList());
    }

    /**
     * Creates an instance of the target, either using a record from the partition or the default
     * constructor of the class.
     */
    private static <T> T rootInstance(Partition partition, Class<T> targetClass) {
        return isInDynamo(targetClass)
                ? instanceFromPartition(partition, targetClass)
                : Shenanigans.defaultInstance(targetClass);
    }

    /**
     * Creates instance using item type from partition.
     */
    private static <T> T instanceFromPartition(Partition partition, Class<T> targetClass) {
        return find(partition.getAggregates(), Shenanigans.dynamoItemType(targetClass))
                .filter(Predicates::isSingular)
                .map(agg -> instance(agg.getDynamoItems().get(0), targetClass))
                .orElseThrow(() -> new PartitionException("Missing required root item", partition));
    }

    /**
     * Finds a DynamoAggregate of the matching item type.
     */
    private static Optional<DynamoAggregate> find(List<DynamoAggregate> aggregates, String itemType) {
        return aggregates.stream()
                .filter(aggregate -> isItemType(aggregate, itemType))
                .findFirst();
    }

    /**
     * Merges the dynamo aggregates from a partition with the relations defined by the target class
     */
    private static List<RootMember> zip(List<DynamoAggregate> aggregates,
                                        Map<String, Relation> relations) {
        return aggregates.stream()
                .filter(aggregate -> relations.containsKey(aggregate.getItemType()))
                .map(agg -> rootMember(agg.getDynamoItems(), relations.get(agg.getItemType())))
                .collect(Collectors.toList());
    }

    /**
     * Creates a root member. Root members can either be aggregates or singular so this method
     * will check the determination and create a single instance or array.
     */
    private static RootMember rootMember(List<Map<String, AttributeValue>> items,
                                         Relation relation) {
        return isManyAnnotated(relation.getField())
                ? new RootMember(relation.getField(), instances(items, relation.getModel()))
                : new RootMember(relation.getField(), instance(items.get(0), relation.getModel()));
    }

    /**
     * Creates an instance of the desired model using the provided DynamoDB data.
     */
    private static <T> T instance(Map<String, AttributeValue> val, Class<T> model) {
        return instance(tableSchema(model), val);
    }

    /**
     * Builds an instance of the target model using the provided schema and Dynamo data.
     */
    private static <T> T instance(TableSchema<T> schema, Map<String, AttributeValue> val) {
        return schema.mapToItem(val);
    }
}
