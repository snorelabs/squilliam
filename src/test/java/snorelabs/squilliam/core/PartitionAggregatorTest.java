package snorelabs.squilliam.core;

import org.junit.jupiter.api.Test;
import snorelabs.squilliam.core.models.DynamoRoot;
import snorelabs.squilliam.core.models.TwoFieldMember;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PartitionAggregatorTest {
    @Test
    public void testPartitionAggregation() {
        TwoFieldMember member1 = new TwoFieldMember("A", 1);
        TwoFieldMember member2 = new TwoFieldMember("B", 2);

        DynamoRoot root = new DynamoRoot("C");

        TableSchema<TwoFieldMember> memberSchema = TableSchema.fromClass(TwoFieldMember.class);
        TableSchema<DynamoRoot> rootSchema = TableSchema.fromClass(DynamoRoot.class);

        List<Map<String, AttributeValue>> allItems = List.of(
                memberSchema.itemToMap(member1, false),
                memberSchema.itemToMap(member2, false),
                rootSchema.itemToMap(root, false)
        );

        Partition partition = PartitionAggregator.aggregate("ItemType", allItems);

        assert partition.getAggregates().size() == 2;
        assert partition.getAggregates()
                .stream()
                .anyMatch(agg -> agg.getItemType().equals(DynamoRoot.ITEM_TYPE));

        Optional<DynamoAggregate> optMemberAgg = partition.getAggregates().stream()
                .filter(agg -> agg.getItemType().equals(TwoFieldMember.ITEM_TYPE))
                .findFirst();

        assert optMemberAgg.isPresent();
        DynamoAggregate memberAgg = optMemberAgg.get();
        assert memberAgg.getDynamoItems().size() == 2;
    }
}
