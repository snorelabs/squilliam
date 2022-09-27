package snorelabs.squilliam.core;

import java.util.Map;

public class TransformTarget<T> {
    private Class<T> model;
    private Map<String, Relation> relations;

    public TransformTarget(Class<T> model, Map<String, Relation> relations) {
        this.model = model;
        this.relations = relations;
    }

    public Class<T> getModel() {
        return model;
    }

    public Map<String, Relation> getRelations() {
        return relations;
    }
}
