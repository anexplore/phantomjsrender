package com.fd.phantomjsrender.exception;

public class PhantomJSException extends Exception {

    private static final long serialVersionUID = 1L;
    
    public PhantomJSException() {
        super();
    }
    
    public PhantomJSException(String errorMsg) {
        super(errorMsg);
    }
    
    public PhantomJSException(String errorMsg, Throwable throwable) {
        super(errorMsg, throwable);
    }
}
