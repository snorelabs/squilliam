package snorelabs.squilliam.core;

import java.util.List;

public class Partition {
    private List<DynamoAggregate> aggregates;

    public Partition(List<DynamoAggregate> aggregates) {
        this.aggregates = aggregates;
    }

    public List<DynamoAggregate> getAggregates() {
        return aggregates;
    }
}
