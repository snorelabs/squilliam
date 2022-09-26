package snorelabs.squilliam.core;

import io.vavr.collection.Array;
import io.vavr.collection.HashMap;
import io.vavr.control.Option;
import io.vavr.control.Try;
import snorelabs.squilliam.core.annotations.ItemType;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.lang.reflect.Field;

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
     * for agg, field in intersect(partition, target):
     *     member = instance(agg)
     *     root.set(field, member)
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
    public static <T> Try<T> transform(Partition partition, TransformTarget<T> target) {
        return aggregateRoot(partition, target)
                .flatMap(agg -> addAll(agg.getRootInstance(), agg.getMembers()));
    }

    /**
     * Adds all members to the root instance.
     */
    private static <T> Try<T> addAll(T root, Array<RootMember> members) {
        return members.foldLeft(Try.success(root), PartitionTransformer::add);

    }

    /**
     * Maps the provided Try wrapped root instance to one with the additional root member set.
     */
    private static <T> Try<T> add(Try<T> tryRoot, RootMember member) {
        return tryRoot.flatMap(root -> setMember(root, member));
    }

    /**
     * Sets the value of the root member for the associated field on the root.
     */
    private static <T> Try<T> setMember(T root, RootMember member) {
        return Try.run(() -> member.getField().set(root, member.getVal()))
                .map(__ -> root);
    }

    /**
     * Attempts to find a required item in the DynamoDB Partition. Returns a failure in the case of
     * no item found or the retrieved aggregate having more than one item.
     */
    private static Try<HashMap<String, AttributeValue>> findRequired(Partition partition,
                                                                     String itemType) {
        return find(partition, itemType)
                .filter(Predicates::isSingular)
                .map(aggregate -> aggregate.getDynamoItems().head())
                .map(Try::success)
                .getOrElse(Try.failure(new RuntimeException("Issue in partition")));
    }

    /**
     * Attempts to find a provided item type in the DynamoDB Partition.
     */
    private static Option<DynamoAggregate> find(Partition partition, String itemType) {
        return partition.getAggregates()
                .find(aggregate -> aggregate.getItemType().equals(itemType));
    }

    private static <T> Try<AggregateRoot<T>> aggregateRoot(Partition partition,
                                                           TransformTarget<T> target) {
        Array<RootMember> members = intersect(partition, target.getRelations());
        return targetInstance(partition, target)
                .map(root -> new AggregateRoot<>(root, members));
    }

    /**
     * Computes the intersection of the provided DynamoDB data and target model relations. This
     * is expressed as an array of RootMember instances.
     */
    private static <T> Array<RootMember> intersect(Partition partition,
                                                   HashMap<String, Relation> relations) {
        return partition.getAggregates()
                .foldLeft(Array.empty(), (members, agg) -> findAndAppend(members, relations, agg));
    }

    /**
     * Attempts to find an applicable relation for the provided Dynamo data and appends a new
     * root member for it if available. Otherwise, it returns the provided Array.
     */
    private static Array<RootMember> findAndAppend(Array<RootMember> members,
                                                   HashMap<String, Relation> relations,
                                                   DynamoAggregate agg) {
        return find(relations, agg)
                .map(r -> append(members, r.getField(), r.getModel(), agg))
                .getOrElse(members);
    }

    /**
     * Returns a concatenation of the provided members and a new root member for the field, model,
     * and dynamo data.
     */
    private static Array<RootMember> append(Array<RootMember> members, Field field,
                                            Class<?> model, DynamoAggregate agg) {
        return members.append(rootMember(agg.getDynamoItems(), field, model));
    }

    /**
     * Attempts to find the relation for the given dynamo aggregate.
     */
    private static Option<Relation> find(HashMap<String, Relation> relations,
                                         DynamoAggregate aggregate) {
        return relations.get(aggregate.getItemType());
    }

    /**
     * Creates a root member. Root members can either be aggregates or singular so this method
     * will check the determination and create a single instance or array.
     */
    private static RootMember rootMember(Array<HashMap<String, AttributeValue>> items,
                                         Field field, Class<?> model) {
        return isManyAnnotated(field)
                ? new RootMember(field, instances(items, model))
                : new RootMember(field, instance(items.head(), model));
    }

    /**
     * Creates an instance for the transform target, either as a default instance or with an
     * associated DynamoDB record
     */
    private static <T> Try<T> targetInstance(Partition partition, TransformTarget<T> target) {
        return isInDynamo(target.getModel())
                ? instanceFromPartition(partition, target.getModel())
                : defaultInstance(target.getModel());
    }

    /**
     * Attempts to create an instance of the desired class with data from the partition. Requires
     * that the instance class has an associated item type.
     */
    private static <T> Try<T> instanceFromPartition(Partition partition, Class<T> instanceClass) {
        return findRequired(partition, dynamoItemType(instanceClass))
                .map(dynamoItem -> instance(dynamoItem, instanceClass));
    }

    /**
     * Creates a new instance of the specified class with the default constructor.
     */
    private static <T> Try<T> defaultInstance(Class<T> model) {
        return Try.of(() -> model.getDeclaredConstructor().newInstance());
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
    private static <T> Array<T> instances(Array<HashMap<String, AttributeValue>> vals,
                                          Class<T> model) {
        TableSchema<T> schema = tableSchema(model);
        return vals.map(val -> instance(schema, val));
    }

    /**
     * Creates an instance of the desired model using the provided DynamoDB data.
     */
    private static <T> T instance(HashMap<String, AttributeValue> val, Class<T> model) {
        return instance(tableSchema(model), val);
    }

    /**
     * Builds an instance of the target model using the provided schema and Dynamo data.
     */
    private static <T> T instance(TableSchema<T> schema, HashMap<String, AttributeValue> val) {
        return schema.mapToItem(val.toJavaMap());
    }

    /**
     * Creates a TableSchema for the provided model class
     */
    private static <T> TableSchema<T> tableSchema(Class<T> itemClass) {
        return TableSchema.fromClass(itemClass);
    }
}
