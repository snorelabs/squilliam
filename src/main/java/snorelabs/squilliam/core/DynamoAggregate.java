package snorelabs.squilliam.core;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

public class DynamoAggregate {
    private List<Map<String, AttributeValue>> dynamoItems;
    private String itemType;

    public DynamoAggregate(String itemType, List<Map<String, AttributeValue>> dynamoItems) {
        this.itemType = itemType;
        this.dynamoItems = dynamoItems;
    }

    public String getItemType() {
        return itemType;
    }

    public List<Map<String, AttributeValue>> getDynamoItems() {
        return dynamoItems;
    }
}
