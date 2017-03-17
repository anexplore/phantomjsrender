# phantomjsrender
使用PhantomJs + WebDriver 获取网页渲染以后的源码、HTTP头等。

支持多Window并发模式

支持卡死检测
```java
PhantomJSProperties properties = new PhantomJSProperties();
properties.setExecutePath("phantomjs");
properties.setMaxTabCount(3);
PhantomJSRender render = new PhantomJSRender(properties);
String url = "http://www.sohu.com";
Response response = render.render(url, 30000);
System.out.println(response.url)；
System.out.println(response.body);
System.out.println(response.status);
System.out.println(response.headers);
render.close();
````

# phantomjs内存泄露问题
在loadImages=false的情况下，phantomjs内存泄露，此bug仍没有被修复；
解决途径：
```javascript
loadImages=true,
page.onResourceRequest=function(dataReq, webReq) {
  //dataReq.url 对图片过滤，webReq.abort()图片请求
} 
```
