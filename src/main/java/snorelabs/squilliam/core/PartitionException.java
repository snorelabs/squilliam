package snorelabs.squilliam.core;

public class PartitionException extends RuntimeException {

    private Partition partition;

    public PartitionException(String msg, Partition partition) {
        super(msg);
        this.partition = partition;
    }

    public Partition getPartition() {
        return partition;
    }
}
