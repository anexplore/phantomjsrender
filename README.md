# phantomjsrender
使用PhantomJs + WebDriver 获取网页渲染以后的源码、HTTP头等。

支持多Window并发模式

支持卡死检测
```java
PhantomJSProperties properties = new PhantomJSProperties();</br>
properties.setExecutePath("phantomjs");</br>
properties.setMaxTabCount(3);</br>
PhantomJSRender render = new PhantomJSRender(properties);</br>
String url = "http://www.sohu.com";</br>
Response response = render.render(url, 30000);<br>
System.out.println(response.url);<br>
System.out.println(response.body);<br>
System.out.println(response.status);<br>
System.out.println(response.headers);<br>
render.close();<br>
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
