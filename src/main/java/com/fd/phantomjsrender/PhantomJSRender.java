package com.fd.phantomjsrender;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fd.phantomjsrender.exception.MaxTotalCountException;
import com.fd.phantomjsrender.exception.PhantomJSException;
import com.fd.phantomjsrender.exception.TimeoutException;
import com.fd.phantomjsrender.exception.WrongStatusException;
import com.fd.phantomjsrender.utils.TextUtils;
import com.fd.phantomjsrender.utils.TimeUtils;

/**
 * PhantomJsRender
 * <p>渲染某个网页 拿到网页渲染以后的html内容\状态码\HTTP头</p>
 * <p>支持多window窗口</p>
 * <p>有同步与异步两种使用方式</p>
 * <p>Phantomjs 2.1当不加载图片时存在内容泄漏的bug</p>
 * <p>Example:</p>
 * <p>
 * PhantomJSProperties properties = new PhantomJSProperties();</br>
 * properties.setExecutePath("phantomjs");</br>
 * properties.setMaxTabCount(3);</br>
 * PhantomJSRender render = new PhantomJSRender(properties);</br>
 * String url = "http://www.sohu.com";</br>
 * Response response = render.render(url, 30000);</br>
 * assert response != null;</br>
 * System.out.println(response.url);</br>
 * System.out.println(response.body);</br>
 * System.out.println(response.status);</br>
 * System.out.println(response.headers);</br>
 * </p>
 * @author caoliuyi
 *
 */
public class PhantomJSRender {

    public static final String EXECUTE_SCRIPT = "var page = this;" 
            + "page.settings.resourceTimeout=%d;"
            + "page.onLoadFinished=function(status) {"
            + "page.loadstatus=1;}; var count=0;"
            + "page.onResourceReceived=function(response){"
            + "if (count===0) {page.scode=response.status;"
            + "page.headers=JSON.stringify(response.headers);"
            + "if (response.status === 302) {count=-1;}} count=count+1;};"
            + "page.open('%s');";
    public static final String LOAD_FINISHED = "1";
    public static final String CHECK_LOAD_STATUS_SCRIPT = "var page=this;return page.loadstatus;";
    public static final String GET_HTTP_STATUS_SCRIPT = "var page=this;return page.scode;";
    public static final String GET_HTTP_HEADERS_SCRIPT = "var page=this;return page.headers;";
    public static final String CLEAR_MEMORY_CACHE_SCRIPT = "var page=this;page.clearMemoryCache();";
    public static final String OPEN_EMPTY_TAB_SCRIPT = "window.open('about:blank');";
    
    public static final Logger LOG = LoggerFactory.getLogger(PhantomJSRender.class);
    private static final Map<String, String> EMPTY_ARGS = new HashMap<>();

    private volatile PhantomJSDriver driver;
    @SuppressWarnings("unused")
    private volatile URL driverStatusUrl;
    private volatile PhantomJSDriverService driverService;
    private Thread hangCheckThread;
    private Thread checkThread;
    private String emptyWindow;
    private Semaphore sem;
    private final ConcurrentHashMap<String, RenderTask> windowToTaskMap;
    private final LinkedBlockingQueue<Object> semQueue;
    private LinkedList<RenderTask> tasks;
    private CommandExecutorGuard guard;
    private ReentrantLock execLock;
    private final AtomicBoolean wrongStatus;
    private final PhantomJSProperties properties;
    private volatile boolean stopDriver;

    public PhantomJSRender(PhantomJSProperties properties) {
        assert properties != null;
        sem = new Semaphore(properties.getMaxTabCount());
        semQueue = new LinkedBlockingQueue<>();
        windowToTaskMap = new ConcurrentHashMap<String, RenderTask>();
        tasks = new LinkedList<>();
        guard = new CommandExecutorGuard();
        execLock = new ReentrantLock(true);
        wrongStatus = new AtomicBoolean(false);
        this.properties = properties;
    }

    private void createNewPhantomJsInstance() throws PhantomJSException {
        DesiredCapabilities props = new DesiredCapabilities();
        props.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
                properties.getPhantomJSExecutePath());
        if (properties.loadImages() != null) {
            props.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_SETTINGS_PREFIX + "loadImages",
                properties.loadImages());
        }
        if (properties.getUserAgent() != null) {
            props.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_SETTINGS_PREFIX + "userAgent",
                properties.getUserAgent());
        }
        List<String > args = Arrays.asList("--ignore-ssl-errors=yes",
                "--webdriver-loglevel=NONE",
                "--cookies-file=" + properties.getPhantomJSExecutePath() + ".cookies"
                );
        if (!TextUtils.isBlank(properties.getProxyHost())) {
            args.add("--proxy=" + properties.getProxyHost() + ":" + properties.getProxyPort());
        }
        props.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, args.toArray(new String[0]));
        this.driver = new PhantomJSDriver(props);
        try {
            driverStatusUrl = new URL("http://localhost:" + driver.getDriverPort() + "/status");
        } catch (MalformedURLException e) {
        }
        driverService = driver.getDriverService();
        emptyWindow = getWindowHandle();
    }

    public void start() {
        restart();
        startCheckTaskResultThread();
        startCheckPhantomJSHangThread();
    }

    private void restart() {
        forceKill();
        if (stopDriver) {
            return;
        }
        execLock.lock();
        try {
            do {
                try {
                    createNewPhantomJsInstance();
                    break;
                } catch (PhantomJSException e) {
                    TimeUtils.wait(1, TimeUnit.SECONDS);
                }
            } while (!stopDriver);
            guard.cmdFinished();
            windowToTaskMap.clear();
            semQueue.clear();
            sem = new Semaphore(properties.getMaxTabCount());
            for (RenderTask task : tasks) {
                try {
                    task.fail(new PhantomJSException("phantomjs occurs error")); 
                } catch (Exception ignore) {}
            }
            tasks.clear();
            wrongStatus.getAndSet(false);
        } finally {
            execLock.unlock();
        }
    }
    
    private void forceKillByFindPid() {
        try {
            String[] command = new String[]{
                    "bash",
                    "-c",
                    "ps -ef |grep phantomjs |grep 'webdriver-loglevel' | awk '{print $2}'"
                    + " | xargs kill 1>/dev/null 2>&1"
            };
            Process process = new ProcessBuilder(command).start();
            process.waitFor();
        } catch (Exception ignore) {
        }
    }
    
    private void forceKill() {
        try {
            if (driver != null) {
                PhantomJSCommandExecutor exec = (PhantomJSCommandExecutor)driver.getCommandExecutor();
                exec.close();
            }
            if (driverService != null) {
               driverService.stop();
            }
        } catch (Exception e) {
            forceKillByFindPid();
        }
    }
    
    /**
     * 效果不佳 临时放弃这个方法
     * \/status这个方法本身占用phantomjs执行资源 执行时间受任务量影响
     * 不清楚对phantomjs的每个webdriver请求在phantomjs内部是如何调度的
     * */
    @SuppressWarnings("unused")
    private boolean checkHttpUrl(URL url, int timeout) {
        if (url == null) {
            return true;
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(timeout);
            connection.setConnectTimeout(timeout);
            connection.connect();
            if (connection.getResponseCode() == 200) {
                return true;
            }
            return false;
        } catch (IOException e) {
            return false;
        } catch (Exception ignore) {
            return false;
        }
    }

    private void startCheckPhantomJSHangThread() {
        hangCheckThread = new Thread() {
            public void run() {
                while (!stopDriver) {
                    Object obj = null;
                    try {
                        obj = semQueue.poll(1, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                    }
                    if (obj != null) {
                        semQueue.clear();
                        wrongStatus.compareAndSet(false, true);
                        LOG.warn("received restart semaphore, restart");
                        restart();
                    } /*else if (!checkHttpUrl(driverStatusUrl, properties.getDriverStatusTimeout())) {
                        wrongStatus.compareAndSet(false, true);
                        LOG.warn("phantomjs status request timeout, restart");
                        restart();
                    } */ else {
                        if (guard.isExecuting()) {
                            if (TimeUtils.monotonicNow() - guard.getCmdStartTimestamp() 
                                    > properties.getDriverHangTimeout()) {
                                wrongStatus.compareAndSet(false, true);
                                LOG.warn("command executor cost too long, restart");
                                restart();
                            }
                        }
                    }
                }
            }
        };
        hangCheckThread.setName("Check Hang-Thread");
        hangCheckThread.start();
    }

    private void startCheckTaskResultThread() {
        checkThread = new Thread() {
            public void run() {
                while (!stopDriver) {
                    check();
                    TimeUtils.wait(500, TimeUnit.MILLISECONDS);
                }
            }
        };
        checkThread.setName("Check Result-Thread");
        checkThread.start();
    }

    public void close() {
        stopDriver = true;
        checkThread.interrupt();
        checkThread.interrupt();
        try {
            forceKill();
        } catch (Exception ignore) {
        }
    }

    private void switchToWindow(String name) throws PhantomJSException {
        guard.cmdStart();
        try {
            driver.switchTo().window(name);
        } catch (RuntimeException e) {
            throw new PhantomJSException(e.getMessage(), e);
        } finally {
            guard.cmdFinished();
        }
    }

    private String getWindowHandle() throws PhantomJSException {
        guard.cmdStart();
        try {
            return driver.getWindowHandle();
        } catch (RuntimeException e) {
            throw new PhantomJSException(e.getMessage(), e);
        } finally {
            guard.cmdFinished();
        }
    }

    private Set<String> getWindowHandles() throws PhantomJSException {
        guard.cmdStart();
        try {
            return driver.getWindowHandles();
        } catch (RuntimeException e) {
            throw new PhantomJSException(e.getMessage(), e);
        } finally {
            guard.cmdFinished();
        }
    }

    private String getCurrentUrl() throws PhantomJSException {
        guard.cmdStart();
        try {
            return driver.getCurrentUrl();
        } catch (RuntimeException e) {
            throw new PhantomJSException(e.getMessage(), e);
        } finally {
            guard.cmdFinished();
        }
    }

    private String getPageSource() throws PhantomJSException {
        guard.cmdStart();
        try {
            return driver.getPageSource();
        } catch (RuntimeException e) {
            throw new PhantomJSException(e.getMessage(), e);
        } finally {
            guard.cmdFinished();
        }
    }

    private void closeWindow() throws PhantomJSException {
        guard.cmdStart();
        try {
            driver.executePhantomJS(CLEAR_MEMORY_CACHE_SCRIPT);
            driver.close();
        } catch (RuntimeException e) {
            throw new PhantomJSException(e.getMessage(), e);
        } finally {
            guard.cmdFinished();
        }
    }

    private Object executePhantomJS(String script, Object... args) throws PhantomJSException {
        guard.cmdStart();
        try {
            return driver.executePhantomJS(script, args);
        } catch (RuntimeException e) {
            throw new PhantomJSException(e.getMessage(), e);
        } finally {
            guard.cmdFinished();
        }
    }

    private Object executeScript(String script, Object... args) throws PhantomJSException {
        guard.cmdStart();
        try {
            return driver.executeScript(script, args);
        } catch (RuntimeException e) {
            throw new PhantomJSException(e.getMessage(), e);
        } finally {
            guard.cmdFinished();
        }
    }

    private String parseToString(Object obj) {
        if (obj == null) {
            obj = "";
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        return obj.toString();
    }

    private int parseToIntValue(Object obj) {
        if (obj == null) {
            return -1;
        }
        if (obj instanceof Integer) {
            return (int) obj;
        }
        try {
            return Integer.parseInt(obj.toString());
        } catch (Exception e) {
            return -1;
        }
    }
    
    private HashMap<String, List<String>> convertHeaders(String headerJsonString) {
        HashMap<String, List<String>>  headers = new HashMap<String, List<String>>();
        if (TextUtils.isBlank(headerJsonString)) {
            return headers;
        }
        try {
            JSONArray headerArray = JSON.parseArray(headerJsonString);
            for (int i = 0; i < headerArray.size(); i++) {
                JSONObject obj = headerArray.getJSONObject(i);
                List<String> values = headers.get(obj.getString("name").toLowerCase());
                if (values == null) {
                    values = new ArrayList<String>(1);
                    headers.put(obj.getString("name").toLowerCase(), values);
                }
                values.add(obj.getString("value"));
            }
        } catch (Exception ignore) {}
        return headers;
    }
    
    public class RenderTask {
        private final CountDownLatch latch;
        private Response response;
        private Exception exception;
        private ResponseHandler callback;
        private String windowName;
        private String url;
        private Semaphore sem;
        private long timeoutTimestamp;
        private boolean success;
        private AtomicBoolean isDone;
        
        public RenderTask() {
            latch = new CountDownLatch(1);
            isDone = new AtomicBoolean(false);
        }

        public Response get() throws InterruptedException {
            latch.await();
            return response;
        }

        public Response get(int timeout, TimeUnit unit) throws InterruptedException {
            latch.await(timeout, unit);
            return response;
        }

        protected void success(Response response) {
            if (!isDone.compareAndSet(false, true)) {
                return;
            }
            this.response = response;
            this.success = true;
            taskend();
            latch.countDown();
        }

        protected void fail(Exception exception) {
            if (!isDone.compareAndSet(false, true)) {
                return;
            }
            this.exception = exception;
            taskend();
            latch.countDown();
        }
        
        protected void setWindowName(String windowName) {
            this.windowName = windowName;
        }

        protected void setCallback(ResponseHandler callback) {
            this.callback = callback;
        }

        protected void setUrl(String url) {
            this.url = url;
        }
        
        protected void setSemaphore(Semaphore sem) {
            this.sem = sem;
        }
        
        protected void setTimeoutTimestamp(long timeoutTimestamp) {
            this.timeoutTimestamp = timeoutTimestamp;
        }
        
        public boolean isTimeout() {
            return TimeUtils.monotonicNow() > timeoutTimestamp;
        }
        
        public boolean isDone() {
            return isDone.get();
        }
        
        private void taskend() {
            if (this.sem != null) {
                this.sem.release();
                this.sem = null;
            }
            if (callback != null) {
                if (success) {
                    callback.success(response); 
                } else {
                    callback.fail(exception);
                }
            }
        }
    }

    private void check() {
        if (wrongStatus.get()) {
            return;
        }
        try {
            execLock.lockInterruptibly();
        } catch (InterruptedException e1) {
            return;
        }
        try {
            if (stopDriver) {
                return;
            }
            Iterator<RenderTask> taskIter = tasks.iterator();
            while (taskIter.hasNext()) {
                RenderTask task = taskIter.next();
                if (task.isDone()) {
                    taskIter.remove();
                    continue;
                }
                if (task.isTimeout()) {
                    taskIter.remove();
                    task.fail(new TimeoutException("Timeout"));
                    if (task.windowName != null) {
                        windowToTaskMap.remove(task.windowName);
                        switchToWindow(task.windowName);
                        closeWindow();
                    }
                }
            }
            Iterator<String> iter = getWindowHandles().iterator();
            while (iter.hasNext()) {
                String name = iter.next();
                if (name.equals(emptyWindow)) {
                    continue;
                }
                switchToWindow(name);
                RenderTask task = windowToTaskMap.get(name);
                if (task == null) {
                    closeWindow();
                    continue;
                }
                Object obj = executePhantomJS(CHECK_LOAD_STATUS_SCRIPT);
                if (obj != null && obj.toString().equals(LOAD_FINISHED)) {
                    Response response = new Response();
                    response.html = getPageSource();
                    response.status =
                            parseToIntValue(executePhantomJS(GET_HTTP_STATUS_SCRIPT));
                    response.headers =
                            convertHeaders(parseToString(executePhantomJS(GET_HTTP_HEADERS_SCRIPT)));
                    response.finalUrl = getCurrentUrl();
                    response.url = task.url;
                    iter.remove();
                    closeWindow();
                    windowToTaskMap.remove(name);
                    task.success(response);
                }
            }
        } catch (Exception e) {
        } finally {
            execLock.unlock();
        }
    }
    
    public RenderTask render(String url, int timeout, ResponseHandler callback)
            throws WrongStatusException, MaxTotalCountException {
        if (wrongStatus.get()) {
            throw new WrongStatusException();
        }
        execLock.lock();
        RenderTask task = new RenderTask();
        try {
            if (!sem.tryAcquire()) {
                throw new MaxTotalCountException();
            }
            task.setCallback(callback);
            task.setUrl(url);
            task.setSemaphore(sem);
            task.setTimeoutTimestamp(TimeUtils.monotonicNow() + timeout);
            switchToWindow(emptyWindow);
            Set<String> oldSet = getWindowHandles();
            executeScript(OPEN_EMPTY_TAB_SCRIPT, EMPTY_ARGS);
            String newWindow = null;
            int i = 0;
            do {
                Set<String> newSet = getWindowHandles();
                for (String s : newSet) {
                    if (oldSet.contains(s)) {
                        continue;
                    }
                    newWindow = s;
                    break;
                }
                i++;
            } while (newWindow == null && i < 5);
            if (newWindow == null) {
                throw new PhantomJSException("could not open new tab");
            }
            task.setWindowName(newWindow);
            tasks.add(task);
            windowToTaskMap.put(newWindow, task);
            switchToWindow(newWindow);
            executePhantomJS(String.format(EXECUTE_SCRIPT, timeout, url));
        } catch (MaxTotalCountException e) {
            throw e;
        } catch (Exception e) {
            LOG.warn("phantomjs render error", e);
            task.fail(e);
            if (e.getCause() instanceof RuntimeException
                    || e.getCause() instanceof PhantomJSException) {
                try {
                    semQueue.put(new Object());
                } catch (InterruptedException e1) {
                }
                wrongStatus.compareAndSet(false, true);
            }
        } finally {
            execLock.unlock();
        }
        return task;
    }

    public Response render(String url, int timeout) throws MaxTotalCountException,
            InterruptedException, WrongStatusException {
        RenderTask task = render(url, timeout, null);
        return task.get(timeout, TimeUnit.MILLISECONDS);
    }
}
