package snorelabs.squilliam.core;

import io.vavr.collection.Array;
import io.vavr.collection.HashMap;
import io.vavr.control.Try;
import org.junit.jupiter.api.Test;
import snorelabs.squilliam.core.models.Blank;
import snorelabs.squilliam.core.models.TwoFieldMember;
import snorelabs.squilliam.core.models.NonItemRoot;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.lang.reflect.Field;

public class PartitionTransformerTest {
    // A couple very naive test cases to check the function of the transformer. These need to
    // actually be thought out and replaced with confident tests.
    @Test
    public void defaultAggregate() {
        // Here, we simulate the sort of base case of the Partition Transformer. This is as if
        // we had a non Dynamo related target model and no Dynamo items returned from a query.

        // Creates an empty Dynamo partition, as if a query returned no data, and a target model
        // for a "Blank" class.
        Partition partition = new Partition(Array.empty());
        TransformTarget<Blank> targetModel = new TransformTarget<>(Blank.class, HashMap.empty());

        Try<Blank> tryDefault = PartitionTransformer.transform(partition, targetModel);

        // We should have succeeded in creating a default instance of the class from the default
        // constructor
        assert tryDefault.isSuccess();
        assert tryDefault.get().getVal().equals(Blank.STATIC_VAL);
    }

    @Test
    public void aggregateMember() throws NoSuchFieldException {
        // This simulates creating an instance of a class with an aggregate member
        // which is not itself a DynamoDB item.
        // We create two members first and their dynamo representation (a Partition)
        TwoFieldMember member1 = new TwoFieldMember("Hello", 1);
        TwoFieldMember member2 = new TwoFieldMember("Whatup", 2);

        TableSchema<TwoFieldMember> schema = TableSchema.fromClass(TwoFieldMember.class);

        Array<HashMap<String, AttributeValue>> dynamoItems = Array.of(
                HashMap.ofAll(schema.itemToMap(member1, false)),
                HashMap.ofAll(schema.itemToMap(member2, false))
        );
        Partition partition = new Partition(Array.of(new DynamoAggregate("TwoField", dynamoItems)));

        // Create the TransformTarget representation for the NonItemRoot class.
        Field membersField = NonItemRoot.class.getDeclaredField("members");
        membersField.setAccessible(true);

        HashMap<String, Relation> relationMap = HashMap.of(
                "TwoField", new Relation(TwoFieldMember.class, membersField)
        );

        TransformTarget<NonItemRoot> targetModel = new TransformTarget<>(NonItemRoot.class, relationMap);

        // Transform the partition and target model into a NonItemRoot.
        Try<NonItemRoot> tryRoot = PartitionTransformer.transform(partition, targetModel);

        // We should have an instance with the available data from the partition added to it.
        assert tryRoot.isSuccess();
        NonItemRoot root = tryRoot.get();
        assert root.getMembers().size() == 2;
        assert root.getMembers().find(m -> m.getVal1().equals(member1.getVal1())).isDefined();
    }
}
