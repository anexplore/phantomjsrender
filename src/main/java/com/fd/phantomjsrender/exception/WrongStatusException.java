package com.fd.phantomjsrender.exception;

public class WrongStatusException extends PhantomJSException {
    private static final long serialVersionUID = 1L;
    
    public WrongStatusException() {
        super();
    }
    
    public WrongStatusException(String errorMsg) {
        super(errorMsg);
    }
    
    public WrongStatusException(String errorMsg, Throwable throwable) {
        super(errorMsg, throwable);
    }
}
