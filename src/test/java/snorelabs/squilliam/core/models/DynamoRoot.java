package snorelabs.squilliam.core.models;

import snorelabs.squilliam.core.annotations.HasMany;
import snorelabs.squilliam.core.annotations.ItemType;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.List;

@DynamoDbBean
public class DynamoRoot {
    public static final String ITEM_TYPE = "Root";
    @HasMany
    public List<TwoFieldMember> members;
    @ItemType(DynamoRoot.ITEM_TYPE)
    private String itemType;

    private String example;

    public DynamoRoot() {
        this.itemType = ITEM_TYPE;
    }

    public DynamoRoot(String example) {
        this.example = example;
        this.itemType = ITEM_TYPE;
    }

    public DynamoRoot(String example, List<TwoFieldMember> members) {
        this.example = example;
        this.itemType = ITEM_TYPE;
        this.members = members;
    }

    @DynamoDbPartitionKey
    @DynamoDbAttribute("Example")
    public String getExample() {
        return example;
    }

    @DynamoDbAttribute("ItemType")
    public String getItemType() {
        return itemType;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    @DynamoDbIgnore
    public List<TwoFieldMember> getMembers() {
        return members;
    }

    public void setMembers(List<TwoFieldMember> members) {
        this.members = members;
    }
}
