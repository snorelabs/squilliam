package snorelabs.squilliam.core.models;

import snorelabs.squilliam.core.annotations.HasMany;

import java.util.List;

public class NonDynamoRoot {
    @HasMany
    private List<TwoFieldMember> members;

    public NonDynamoRoot() {}

    public List<TwoFieldMember> getMembers() {
        return members;
    }
}
