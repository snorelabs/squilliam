package snorelabs.squilliam.core;

import io.vavr.collection.Array;

public class AggregateRoot<T> {
    private T rootInstance;
    private Array<RootMember> members;

    public AggregateRoot(T rootInstance, Array<RootMember> members) {
        this.rootInstance = rootInstance;
        this.members = members;
    }

    public T getRootInstance() {
        return rootInstance;
    }

    public Array<RootMember> getMembers() {
        return members;
    }

    public AggregateRoot<T> appendMember(RootMember rootMember) {
        return new AggregateRoot<T>(rootInstance, members.append(rootMember));
    }
}
