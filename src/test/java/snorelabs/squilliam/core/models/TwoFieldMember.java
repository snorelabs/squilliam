package snorelabs.squilliam.core.models;

import snorelabs.squilliam.core.annotations.ItemType;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;


@DynamoDbBean
public class TwoFieldMember {
    public static final String ITEM_TYPE = "TwoField";
    private String val1;
    private int val2;
    @ItemType(TwoFieldMember.ITEM_TYPE)
    private String itemType;

    public TwoFieldMember() {
        val1 = "";
        val2 = 0;
    }

    public TwoFieldMember(String val1, int val2) {
        this.val1 = val1;
        this.val2 = val2;
        this.itemType = ITEM_TYPE;
    }

    @DynamoDbPartitionKey
    @DynamoDbAttribute("Val1")
    public String getVal1() {
        return val1;
    }

    @DynamoDbAttribute("Val2")
    public int getVal2() {
        return val2;
    }

    @DynamoDbAttribute("ItemType")
    public String getItemType() {
        return this.itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public void setVal1(String val1) {
        this.val1 = val1;
    }

    public void setVal2(int val2) {
        this.val2 = val2;
    }
}
