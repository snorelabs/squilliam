package snorelabs.squilliam.core;

import io.vavr.collection.HashMap;

public class TransformTarget<T> {
    private Class<T> model;
    private HashMap<String, Relation> relations;

    public TransformTarget(Class<T> model, HashMap<String, Relation> relations) {
        this.model = model;
        this.relations = relations;
    }

    public Class<T> getModel() {
        return model;
    }

    public HashMap<String, Relation> getRelations() {
        return relations;
    }
}
