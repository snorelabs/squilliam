package snorelabs.squilliam.core;

import io.vavr.collection.Array;
import io.vavr.collection.HashMap;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class DynamoAggregate {
    private Array<HashMap<String, AttributeValue>> dynamoItems;
    private String itemType;

    public DynamoAggregate(String itemType, Array<HashMap<String, AttributeValue>> dynamoItems) {
        this.itemType = itemType;
        this.dynamoItems = dynamoItems;
    }

    public String getItemType() {
        return itemType;
    }

    public Array<HashMap<String, AttributeValue>> getDynamoItems() {
        return dynamoItems;
    }
}
