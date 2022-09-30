package snorelabs.squilliam.core;

public class ModelException extends RuntimeException {
    private Class<?> instanceClass;

    public ModelException(String msg, Class<?> instanceClass, Exception exception) {
        super(msg, exception);
        this.instanceClass = instanceClass;
    }

    public ModelException(String msg, Class<?> instanceClass) {
        super(msg);
        this.instanceClass = instanceClass;
    }

    public Class<?> getInstanceClass() {
        return instanceClass;
    }
}
