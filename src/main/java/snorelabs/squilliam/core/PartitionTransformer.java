package snorelabs.squilliam.core;

import snorelabs.squilliam.core.annotations.ItemType;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static snorelabs.squilliam.core.Predicates.*;

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
            setMember(root, member);
        }
        return root;
    }

    /**
     * Creates an instance of the target, either using a record from the partition or the default
     * constructor of the class.
     */
    private static <T> T rootInstance(Partition partition, Class<T> targetClass) {
        return isInDynamo(targetClass)
                ? instanceFromPartition(partition, targetClass)
                : defaultInstance(targetClass);
    }

    /**
     * Creates instance using item type from partition.
     */
    private static <T> T instanceFromPartition(Partition partition, Class<T> targetClass) {
        return find(partition.getAggregates(), dynamoItemType(targetClass))
                .filter(Predicates::isSingular)
                .map(agg -> instance(agg.getDynamoItems().get(0), targetClass))
                .orElseThrow(() -> new PartitionException("Missing required root item", partition));
    }

    /**
     * Creates an instance of the target class using the default constructor.
     */
    private static <T> T defaultInstance(Class<T> targetClass) {
        try {
            return targetClass.getDeclaredConstructor().newInstance();
        } catch (InvocationTargetException
                 | InstantiationException
                 | IllegalAccessException
                 | NoSuchMethodException e) {
            throw new InstanceException("Unable to create instance of target", targetClass, e);
        }
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
     * This is a side effect method which reflectively sets the value onto the field of the
     * provided root instance. Although unfortunate, Java doesn't natively support creating new
     * instances with the additional field set without the client implementing a builder for it.
     */
    private static <T> void setMember(T root, RootMember member) {
        try {
            member.getField().setAccessible(true);
            member.getField().set(root, member.getVal());
        } catch (IllegalAccessException e) {
            throw new InstanceException("Unable to assign field", root.getClass(), e);
        }
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
     * Gets the item type string (used to identify what type of item the dynamo record is)
     */
    private static String dynamoItemType(Class<?> item) {
        return item.getAnnotation(ItemType.class).value();
    }

    /**
     * Creates an array of instances of the target model from the provided DynamoDB data.
     */
    private static <T> List<T> instances(List<Map<String, AttributeValue>> vals,
                                         Class<T> model) {
        TableSchema<T> schema = tableSchema(model);
        return vals.stream().map(val -> instance(schema, val)).collect(Collectors.toList());
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

    /**
     * Creates a TableSchema for the provided model class
     */
    private static <T> TableSchema<T> tableSchema(Class<T> itemClass) {
        return TableSchema.fromClass(itemClass);
    }
}
