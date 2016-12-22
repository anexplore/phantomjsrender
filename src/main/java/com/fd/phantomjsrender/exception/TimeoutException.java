package com.fd.phantomjsrender.exception;

public class TimeoutException extends PhantomJSException {

    private static final long serialVersionUID = 1L;
    
    public TimeoutException() {
        super();
    }
    
    public TimeoutException(String errorMsg) {
        super(errorMsg);
    }
    
    public TimeoutException(String errorMsg, Throwable throwable) {
        super(errorMsg, throwable);
    }
}
