package snorelabs.squilliam.core;

public class InstanceException extends RuntimeException {
    private Class<?> instanceClass;

    public InstanceException(String msg, Class<?> instanceClass, Exception exception) {
        super(msg, exception);
        this.instanceClass = instanceClass;
    }

    public Class<?> getInstanceClass() {
        return instanceClass;
    }
}
