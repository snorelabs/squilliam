package snorelabs.squilliam;

import snorelabs.squilliam.core.DomainTransformer;
import snorelabs.squilliam.core.Partition;
import snorelabs.squilliam.core.PartitionAggregator;
import snorelabs.squilliam.core.PartitionTransformer;
import snorelabs.squilliam.core.TargetDescriber;
import snorelabs.squilliam.core.TransformTarget;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SquilliamClient {
    private DynamoDbClient dynamoDbClient;
    private String itemAttrName;

    private static final int DYNAMO_BATCH_SIZE = 25;

    public SquilliamClient(DynamoDbClient client, String itemAttrName) {
        this.dynamoDbClient = client;
        this.itemAttrName = itemAttrName;
    }

    /**
     * Queries Dynamo using the provided request and builds an instance of the target class using
     * the retrieved records.
     * TODO: Support load all as with write all
     */
    public <T> T load(QueryRequest request, Class<T> classTarget) {
        QueryResponse response = queryDynamo(request);
        Partition partition = PartitionAggregator.aggregate(this.itemAttrName, response.items());
        TransformTarget<T> target = TargetDescriber.describe(classTarget);
        return PartitionTransformer.transform(partition, target);
    }

    /**
     * Queries Dynamo using the provided request and builds a List of the target class from the
     * retrieved records.
     */
    public <T> List<T> query(QueryRequest request, Class<T> targetClass) {
        QueryResponse response = queryDynamo(request);
        return PartitionTransformer.instances(response.items(), targetClass);
    }

    /**
     * Writes a root item to Dynamo in as many batches of 25 (hard limit from DynamoDB API) as
     * needed.
     */
    public <T> List<BatchWriteItemResponse> writeAll(T root, String tableName) {
        List<List<WriteRequest>> writePartitions = writesForRoot(root);

        return writePartitions.stream()
                .map(partition -> Map.of(tableName, partition))
                .map(items -> BatchWriteItemRequest.builder().requestItems(items).build())
                .map(dynamoDbClient::batchWriteItem)
                .collect(Collectors.toList());
    }

    /**
     * Creates a list of partitions for necessary write batches.
     */
    private <T> List<List<WriteRequest>> writesForRoot(T root) {
        List<WriteRequest> allWrites = DomainTransformer.transform(root)
                .stream()
                .map(record -> PutRequest.builder().item(record).build())
                .map(put -> WriteRequest.builder().putRequest(put).build()).toList();
        List<List<WriteRequest>> writePartitions = new ArrayList<>();

        for (int i = 0; i < allWrites.size(); i += DYNAMO_BATCH_SIZE) {
            writePartitions.add(allWrites.subList(i, DYNAMO_BATCH_SIZE));
        }
        return writePartitions;
    }


    private QueryResponse queryDynamo(QueryRequest request) {
        return dynamoDbClient.query(request);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private DynamoDbClient dynamoDbClient;
        private String itemAttrName;

        public Builder itemAttrName(String itemAttrName) {
            this.itemAttrName = itemAttrName;
            return this;
        }

        public Builder dynamoDbClient(DynamoDbClient client) {
            this.dynamoDbClient = client;
            return this;
        }

        public SquilliamClient build() {
            return new SquilliamClient(dynamoDbClient, itemAttrName);
        }
    }
}
