package snorelabs.squilliam.core.models;

import snorelabs.squilliam.core.annotations.HasMany;
import snorelabs.squilliam.core.annotations.ItemType;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.List;

@ItemType("Root")
@DynamoDbBean
public class DynamoRoot {
    @HasMany
    private List<TwoFieldMember> members;

    private String example;

    public DynamoRoot() {
    }

    public DynamoRoot(String example) {
        this.example = example;
    }

    @DynamoDbPartitionKey
    @DynamoDbAttribute("Example")
    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public List<TwoFieldMember> getMembers() {
        return members;
    }
}
