package snorelabs.squilliam.core;

import io.vavr.collection.Array;

public class Partition {
    private Array<DynamoAggregate> aggregates;

    public Partition(Array<DynamoAggregate> aggregates) {
        this.aggregates = aggregates;
    }

    public Array<DynamoAggregate> getAggregates() {
        return aggregates;
    }
}
