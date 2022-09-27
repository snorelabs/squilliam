package snorelabs.squilliam.core;

import org.junit.jupiter.api.Test;
import snorelabs.squilliam.core.models.Blank;
import snorelabs.squilliam.core.models.DynamoRoot;
import snorelabs.squilliam.core.models.TwoFieldMember;
import snorelabs.squilliam.core.models.NonDynamoRoot;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class PartitionTransformerTest {
    // A couple very naive test cases to check the function of the transformer. These need to
    // actually be thought out and replaced with confident tests.
    @Test
    public void defaultAggregate() {
        // Here, we simulate the sort of base case of the Partition Transformer. This is as if
        // we had a non Dynamo related target model and no Dynamo items returned from a query.

        // Creates an empty Dynamo partition, as if a query returned no data, and a target model
        // for a "Blank" class.
        Partition partition = new Partition(List.of());
        TransformTarget<Blank> targetModel = new TransformTarget<>(Blank.class, Map.of());

        Blank defaultBlank = PartitionTransformer.transform(partition, targetModel);

        // We should have succeeded in creating a default instance of the class from the default
        // constructor
        assert defaultBlank.getVal().equals(Blank.STATIC_VAL);
    }

    @Test
    public void nonDynamoAggregate() throws NoSuchFieldException {
        // This simulates creating an instance of a class with an aggregate member
        // which is not itself a DynamoDB item.
        // We create two members first and their dynamo representation (a Partition)
        TwoFieldMember member1 = new TwoFieldMember("A", 1);
        TwoFieldMember member2 = new TwoFieldMember("B", 2);

        TableSchema<TwoFieldMember> schema = TableSchema.fromClass(TwoFieldMember.class);

        List<Map<String, AttributeValue>> dynamoItems = List.of(
                schema.itemToMap(member1, false),
                schema.itemToMap(member2, false)
        );
        Partition partition = new Partition(List.of(new DynamoAggregate("TwoField", dynamoItems)));

        // Create the TransformTarget representation for the NonItemRoot class.
        Field membersField = NonDynamoRoot.class.getDeclaredField("members");

        Map<String, Relation> relationMap = Map.of("TwoField", new Relation(TwoFieldMember.class, membersField));

        TransformTarget<NonDynamoRoot> targetModel = new TransformTarget<>(NonDynamoRoot.class, relationMap);

        // Transform the partition and target model into a NonItemRoot.
        NonDynamoRoot nonDynamoRoot = PartitionTransformer.transform(partition, targetModel);

        // We should have an instance with the available data from the partition added to it.
        assert nonDynamoRoot.getMembers().size() == 2;
        assert nonDynamoRoot.getMembers()
                .stream().anyMatch(m -> m.getVal1().equals(member1.getVal1()));
    }

    @Test
    public void dynamoAggregate() throws NoSuchFieldException {
        TwoFieldMember member1 = new TwoFieldMember("A", 1);
        TwoFieldMember member2 = new TwoFieldMember("B", 2);

        DynamoRoot root = new DynamoRoot("C");

        TableSchema<TwoFieldMember> memberSchema = TableSchema.fromClass(TwoFieldMember.class);
        TableSchema<DynamoRoot> rootSchema = TableSchema.fromClass(DynamoRoot.class);

        List<Map<String, AttributeValue>> memberItems = List.of(
                memberSchema.itemToMap(member1, false),
                memberSchema.itemToMap(member2, false)
        );

        List<Map<String, AttributeValue>> rootItems = List.of(rootSchema.itemToMap(root, false));

        List<DynamoAggregate> aggs = List.of(
                new DynamoAggregate("TwoField", memberItems),
                new DynamoAggregate("Root", rootItems)
        );

        Partition partition = new Partition(aggs);

        // Create the TransformTarget representation for the NonItemRoot class.
        Field membersField = DynamoRoot.class.getDeclaredField("members");

        Map<String, Relation> relationMap = Map.of("TwoField", new Relation(TwoFieldMember.class, membersField));

        TransformTarget<DynamoRoot> targetModel = new TransformTarget<>(DynamoRoot.class, relationMap);

        DynamoRoot retrieved = PartitionTransformer.transform(partition, targetModel);

        assert retrieved.getExample().equals(root.getExample());
        assert retrieved.getMembers().size() == 2;

    }
}
