package snorelabs.squilliam.core.models;

import io.vavr.collection.Array;
import snorelabs.squilliam.core.annotations.HasMany;

public class NonItemRoot {
    @HasMany
    private Array<TwoFieldMember> members;

    public NonItemRoot() {}

    public Array<TwoFieldMember> getMembers() {
        return members;
    }
}
