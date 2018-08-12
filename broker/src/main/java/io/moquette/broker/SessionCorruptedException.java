package io.moquette.broker;

public class SessionCorruptedException extends RuntimeException {

    private static final long serialVersionUID = 5848069213104389412L;

    SessionCorruptedException(String msg) {
        super(msg);
    }
}
