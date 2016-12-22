package com.fd.phantomjsrender.exception;

public class MaxTotalCountException extends PhantomJSException {

    private static final long serialVersionUID = 1L;

    public MaxTotalCountException() {}
    
    public MaxTotalCountException(String msg) {
        super(msg);
    }
}
