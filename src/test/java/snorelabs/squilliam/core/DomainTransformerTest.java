package snorelabs.squilliam.core;

import org.junit.jupiter.api.Test;
import snorelabs.squilliam.core.models.Blank;
import snorelabs.squilliam.core.models.DynamoRoot;
import snorelabs.squilliam.core.models.NonDynamoRoot;
import snorelabs.squilliam.core.models.TwoFieldMember;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

public class DomainTransformerTest {
    @Test
    public void testDomainTransformations() {
        Blank blank = new Blank();
        List<Map<String, AttributeValue>> maps = DomainTransformer.transform(blank);
        // For an object with no associated Dynamo data, we shouldn't get anything
        assert maps.size() == 0;

        List<TwoFieldMember> members = List.of(
                new TwoFieldMember("A", 1),
                new TwoFieldMember("B", 2)
        );

        NonDynamoRoot nonDynamoRoot = new NonDynamoRoot(members);
        maps = DomainTransformer.transform(nonDynamoRoot);

        // For an object that isn't itself a dynamo item, but contains dynamo items, we should only
        // have the associated dynamo items in the list.
        assert maps.size() == members.size();
        assert maps.stream().anyMatch(m -> m.get("Val1").s().equals(members.get(0).getVal1()));
        assert maps.stream()
                .anyMatch(m -> Integer.parseInt(m.get("Val2").n()) == members.get(0).getVal2());
        assert maps.stream().anyMatch(m -> m.get("Val1").s().equals(members.get(1).getVal1()));
        assert maps.stream()
                .anyMatch(m -> Integer.parseInt(m.get("Val2").n()) == members.get(1).getVal2());

        DynamoRoot dynamoRoot = new DynamoRoot("C", members);
        maps = DomainTransformer.transform(dynamoRoot);

        // For an object which is itself a dynamo item and has associated dynamo items, we should
        // have the members and the root in the resulting list.
        assert maps.size() == members.size() + 1;
        assert maps.stream().filter(m -> m.get("ItemType").s().equals(DynamoRoot.ITEM_TYPE))
                .anyMatch(m -> m.get("Example").s().equals(dynamoRoot.getExample()));
        assert maps.stream().filter(m -> m.get("ItemType").s().equals(TwoFieldMember.ITEM_TYPE))
                .anyMatch(m -> m.get("Val1").s().equals(members.get(0).getVal1()));
        assert maps.stream().filter(m -> m.get("ItemType").s().equals(TwoFieldMember.ITEM_TYPE))
                .anyMatch(m -> Integer.parseInt(m.get("Val2").n()) == members.get(0).getVal2());
        assert maps.stream().filter(m -> m.get("ItemType").s().equals(TwoFieldMember.ITEM_TYPE))
                .anyMatch(m -> m.get("Val1").s().equals(members.get(1).getVal1()));
        assert maps.stream().filter(m -> m.get("ItemType").s().equals(TwoFieldMember.ITEM_TYPE))
                .anyMatch(m -> Integer.parseInt(m.get("Val2").n()) == members.get(1).getVal2());

        // For a list of items, we should get the dynamo representations of the list.
        maps = DomainTransformer.transform(members);
        assert maps.size() == members.size();
        assert maps.stream().anyMatch(m -> m.get("Val1").s().equals(members.get(0).getVal1()));
        assert maps.stream()
                .anyMatch(m -> Integer.parseInt(m.get("Val2").n()) == members.get(0).getVal2());
        assert maps.stream().anyMatch(m -> m.get("Val1").s().equals(members.get(1).getVal1()));
        assert maps.stream()
                .anyMatch(m -> Integer.parseInt(m.get("Val2").n()) == members.get(1).getVal2());

    }
}
