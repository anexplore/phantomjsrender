package com.fd.phantomjsrender;

/*
 * This file is copy from selenium-remote-driver-2.53.1 Some Code are modified.
 */

import org.openqa.selenium.remote.http.HttpMethod;

public class CommandInfo {
    private final String url;
    private final HttpMethod method;

    CommandInfo(String url, HttpMethod method) {
        this.url = url;
        this.method = method;
    }

    String getUrl() {
        return url;
    }

    HttpMethod getMethod() {
        return method;
    }

}
