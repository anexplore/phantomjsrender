package com.fd.phantomjsrender.exception;

public class PhantomJSClosedException extends PhantomJSException {

    private static final long serialVersionUID = 1L;
   
    public PhantomJSClosedException() {
        super();
    }
    
    public PhantomJSClosedException(String errorMsg) {
        super(errorMsg);
    }
    
    public PhantomJSClosedException(String errorMsg, Throwable throwable) {
        super(errorMsg, throwable);
    }
}
