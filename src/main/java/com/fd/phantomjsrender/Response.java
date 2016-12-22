package com.fd.phantomjsrender;

import java.util.HashMap;
import java.util.List;

public class Response {
    // HTML源码
    public String html;
    // 原始地址
    public String url;
    // 最终地址
    public String finalUrl;
    // 状态码
    public int status;
    // HTTP HEADERS
    public HashMap<String, List<String>> headers = new HashMap<>();
    
    public int getStatusCode() {
        return status;
    }
    
    public String getFinalUrl() {
        return finalUrl;
    }
    
    public HashMap<String, List<String>> getHeaders() {
        return headers;
    }
    
    public String getHtml() {
        return html;
    }
    
    public int getBodySize() {
        // 这里没有考虑编码 不准确
        return html == null ? 0 : html.getBytes().length;
    }
    
    public String getFirstHeader(String header) {
        if (header != null && headers != null) {
            List<String> values = headers.get(header.toLowerCase());
            if (values != null && values.size() > 0) {
                return values.get(0);
            }
        }
        return "";
    }
}
