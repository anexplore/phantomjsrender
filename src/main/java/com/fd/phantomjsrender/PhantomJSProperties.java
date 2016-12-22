package com.fd.phantomjsrender;
/**
 * PhantomJS属性
 * @author caoliuyi
 *
 */
public class PhantomJSProperties {
    
    /*phantomjs 可执行文件目录*/
    private String executePath;
    /*是否加载图片*/
    private Boolean loadImages;
    /*User-Agent*/
    private String userAgent;
    /*最大Tab页数*/
    private int maxTabCount = 3;
    /*判断phantomjs hang住时间阈值*/
    private int driverHangTimeout = 60_000;
    /*请求phantomjs status请求超时时间*/
    private int driverStatusTimeout = 60_000;
    /*代理HOST*/
    private String proxyHost;
    /*代理PORT*/
    private int proxyPort;
    
    public String getPhantomJSExecutePath() {
        return executePath;
    }
    
    public Boolean loadImages() {
        return loadImages;
    }
    
    public String getUserAgent() {
        return userAgent;
    }

    public PhantomJSProperties setExecutePath(String executePath) {
        this.executePath = executePath;
        return this;
    }

    public PhantomJSProperties setLoadImages(Boolean loadImages) {
        this.loadImages = loadImages;
        return this;
    }

    public PhantomJSProperties setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }
    
    public PhantomJSProperties setMaxTabCount(int maxTabCount) {
        this.maxTabCount = maxTabCount;
        return this;
    }
    
    public int getMaxTabCount() {
        return maxTabCount;
    }

    public int getDriverHangTimeout() {
        return driverHangTimeout;
    }

    public PhantomJSProperties setDriverHangTimeout(int driverHangTimeout) {
        this.driverHangTimeout = driverHangTimeout;
        return this;
    }

    public int getDriverStatusTimeout() {
        return driverStatusTimeout;
    }

    public PhantomJSProperties setDriverStatusTimeout(int driverStatusTimeout) {
        this.driverStatusTimeout = driverStatusTimeout;
        return this;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public PhantomJSProperties setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
        return this;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public PhantomJSProperties setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
        return this;
    }
}
