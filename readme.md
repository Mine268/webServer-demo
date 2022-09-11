# 多线程服务器Demo

这个简单的多线程服务器实现了静态页面的跳转和表单的提交。

有网页文件
```
webroot
    index.html
    not_fount.html
    subpage.html
```

内容分别为
```html
<!-- index.html -->
<html lang="html">
<head><title>the page</title></head>
<body>
    <form action="index.html" method="post">
        First name: <input type="text" name="fname"><br>
        Last name: <input type="text" name="lname"><br>
        <input type="submit" value="submit the form">
    </form>
    <a href="subpage.html">go to sub page.</a>
</body>
</html>

<!-- not_found.html -->
404

<!-- subpage.html -->
<html>
<head><title>sub page</title></head>
<body>
    this is the sub page.
    <a href="index.html">go back to index.</a>
</body>
</html>
```

这个demo实现了：
1. 提交表单：将以`GET`/`POST`方式提交的表单的内容提取出来
2. 页面跳转：通过`<a></a>`标签进行静态页面的跳转

其主要组成部分为：
1. `WebServer`：将请求转发到线程池的`RequestHandler`
2. `RequestHandler`：接受、解释请求，并返回结果

## `Socket`

`socket`是用来进行HTTP通信的重要工具，是一种点对点的通讯方式，通过socket可以进行进程间的远程通信，当通信完成的时候就需要关闭`socket`。

以下是一个通讯实例
```java
var socket = new Socket("host", 95001);
var output_stream = socket.getOutputStream();
output_stream.write("""
    HTTP/1.0 200 OK
    Content-Type: text/html
    Server: myServer
    
    hello
""");
output_stream.close();
socket.close();
```

通过`socket`连接到了`host:95001`并发送了信息
```
HTTP/1.0 200 OK
Content-Type: text/html
Server: myServer

hello
```

## `WebServer`

`WebServer`利用`ServerSocket`监听服务器的某一个端口，当浏览器请求页面的时候会产生对应的`socket`，`WebServer`就将这个`socket`转发给对应的`RequestHandler`进行具体的处理。
```java
ServerSocket serverSocket;
try {
    serverSocket = new ServerSocket(port);
} catch (Exception ex) {
    ex.printStackTrace();
    return;
}

while (true) {
    try {
        var socket = serverSocket.accept();
        threadPool.execute(new RequestHandler(socket));
    } catch (Exception ex) {
        ex.printStackTrace();
    }
}
```

`serverSocket.accpet()`会导致线程阻塞直到某一个通信请求接入对应的端口`port`，然后生成处理这个`socket`的`RequestHandler`实例并交给线程池中的某一个线程执行。这样的线程池由这样的语句生成。
```java
ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
        10, // 核心线程数
        10, // 最大线程数
        60, // 大于核心线程数的线程的最长等待时间
        TimeUnit.SECONDS, // 上面这个时间的单位是什么
        new LinkedBlockingQueue<>(20), // 等待的任务的容器
        new ThreadPoolExecutor.DiscardOldestPolicy()); // 需要终结线程的时候选择哪一个线程
```

## `RequestHandler`

`RequestHandler`用于处理具体的请求。当用户的HTTP请求发到`WebServer`的时候，它看到的是一个`socket`，`RequestHandler`会对`socket`进行解析，从中提取出header和context进行文件读取操作，然后将内容按照HTTP的格式返回回去。

大致流程为：
1. 读取请求的内容
2. 分析请求的内容
3. 返回对应的内容

### 读取请求的内容

`socket`按照流式的方式进行内容的读取，对应的读取流为`socket.getInputStream()`，由于网络传输的性质，调用其`read`方法不能够保证依次将完整的请求读取过来。demo中采取了多次循环读取、每次读取后暂停一小会并判断是否有剩余信息（若有则继续读取，否则认为读取完成）的方式进行读取。代码如下
```java
private byte[] readStream(InputStream input_stream) throws Exception {
    var bao = new ByteArrayOutputStream();
    readStreamRecursion(bao, input_stream);
    bao.close();
    return bao.toByteArray();
}

private void readStreamRecursion(ByteArrayOutputStream output_stream, InputStream input_stream) throws Exception {
    var start_time = System.currentTimeMillis();
    while (input_stream.available() == 0) {
        if (System.currentTimeMillis() - start_time >= ServerConfig.socket_timeout_limit) {
            throw new SocketTimeoutException("Socket reading time-outs.");
        }
    }

    var buffer = new byte[2048];
    var read_count = input_stream.read(buffer);
    output_stream.write(buffer, 0, read_count);
    Thread.sleep(100);
    if (input_stream.available() != 0) {
        readStreamRecursion(output_stream, input_stream);
    }
}
```

调用`readStream(socket.getInputStream())`将返回一个`byte`数组，对应了请求的字节流信息，然后将其转换为`String`信息。

### 分析请求信息

读取并将请求信息转为为`String`格式之后，对其进行分析，demo中仅仅判断了这个请求是`POST`还是`GET`，然后按照对应的方式将请求的参数抓取出来。同时将访问的页面路径也抓取出来。

> 琐碎的字符串操作省略。

### 返回对应的内容

返回对应的内容是根据访问的页面路径决定的（更强大的实现还会依赖请求的参数，但是demo中没有实现）。

如果该页面为空则返回默认页面，如果不存在返回404页面，否则返回对应的页面。
```java
private void returnDesignatedPage(Socket socket, String page_path) throws Exception {
    if (page_path.isEmpty()) {
        returnDefaultPage(socket);
    } else {
        try {
            var file_stream = new FileInputStream(ServerConfig.webRoot + page_path);
            returnPage(socket, ServerConfig.StatusCode.OK, file_stream);
            file_stream.close();
        } catch (Exception ex) {
            return404Page(socket);
        }
    }
}

private void returnPage(Socket socket, ServerConfig.StatusCode code, InputStream input_stream) throws IOException {
    var output_stream = socket.getOutputStream();
    output_stream.write(ServerConfig.getHtmlHeader(code).getBytes());
    output_stream.write(input_stream.readAllBytes());
    output_stream.close();
}

private void return404Page(Socket socket) throws IOException {
    var page_input_stream = new FileInputStream(ServerConfig.webRoot + ServerConfig.notFoundPage);
    returnPage(socket, ServerConfig.StatusCode.NOT_FOUND, page_input_stream);
    page_input_stream.close();
}

private void returnDefaultPage(Socket socket) throws IOException {
    var page_input_stream = new FileInputStream(ServerConfig.webRoot + ServerConfig.defaultPage);
    returnPage(socket, ServerConfig.StatusCode.OK, page_input_stream);
    page_input_stream.close();
}
```
