package snorelabs.squilliam.core;

import org.junit.jupiter.api.Test;
import snorelabs.squilliam.core.models.Blank;
import snorelabs.squilliam.core.models.DynamoRoot;
import snorelabs.squilliam.core.models.NonDynamoRoot;
import snorelabs.squilliam.core.models.TwoFieldMember;

public class TargetDescriptorTest {
    @Test
    public void testDescriptions() {
        TransformTarget<Blank> blankTarget = TargetDescriptor.describe(Blank.class);

        assert blankTarget.getModel() == Blank.class;
        assert blankTarget.getRelations().isEmpty();

        TransformTarget<NonDynamoRoot> nonDynamoTarget = TargetDescriptor.describe(NonDynamoRoot.class);

        assert nonDynamoTarget.getModel() == NonDynamoRoot.class;
        assert nonDynamoTarget.getRelations().containsKey(TwoFieldMember.ITEM_TYPE);
        assert nonDynamoTarget.getRelations().keySet().size() == 1;

        TransformTarget<DynamoRoot> dynamoTarget = TargetDescriptor.describe(DynamoRoot.class);
        assert dynamoTarget.getModel() == DynamoRoot.class;
        assert dynamoTarget.getRelations().containsKey(TwoFieldMember.ITEM_TYPE);
        assert dynamoTarget.getRelations().keySet().size() == 1;
    }
}
