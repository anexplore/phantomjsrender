package com.fd.phantomjsrender;

public interface ResponseHandler {
    /**
     * 下载成功
     * @param response
     */
    void success(Response response);

    /**
     * 下载失败
     * @param exception
     */
    void fail(Exception exception);
}
