package registry.machine;

import com.google.common.io.Files;
import com.jayway.jsonpath.JsonPath;
import models.Code;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by edwardsbean on 2015/3/1 0001.
 */
public class HttpOldSohuTaskProcess implements TaskProcess {
    private static Logger log = LoggerFactory.getLogger(HttpOldSohuTaskProcess.class);

    @Override
    public void process(Task task) throws Exception {
        CookieStore cookieStore = new BasicCookieStore();
        HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(cookieStore);
        try (CloseableHttpResponse login = HttpUtils.httpclient.execute(new HttpGet("http://i.sohu.com/login/reg.do"), context)) {
        } catch (Exception e) {
        }

        if (!isEmailOK(task)) {
            LogUtils.emailException();
            return;
        }
        //检查邮箱
        if (RegistryMachineContext.isFilter.get()) {
            RegistryMachineContext.okEmailQueue.add(task);
            return;
        }

        String proxy = RegistryMachineContext.proxyQueue.poll();
        while (!Thread.currentThread().isInterrupted()) {
            //获取图片验证码
            Code code = getPictureCode(context, task);
            //提交
            HttpPost submitEmail = new HttpPost("http://i.sohu.com/login/sreg.do?_input_encode=utf-8");
            submitEmail.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36");
            submitEmail.addHeader("Referer", "http://i.sohu.com/login/reg.do");
            submitEmail.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            submitEmail.addHeader("Origin", "http://i.sohu.com");
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("bru", ""));
            nvps.add(new BasicNameValuePair("default_page", ""));
            nvps.add(new BasicNameValuePair("from", ""));
            nvps.add(new BasicNameValuePair("sappId", ""));
            nvps.add(new BasicNameValuePair("source", ""));
            nvps.add(new BasicNameValuePair("user", task.getEmail()));
            nvps.add(new BasicNameValuePair("nickname", task.getEmail()));
            nvps.add(new BasicNameValuePair("passwd", task.getPassword()));
            nvps.add(new BasicNameValuePair("vcode", code.getCode()));
            nvps.add(new BasicNameValuePair("vcodeEn", ""));
            nvps.add(new BasicNameValuePair("agree", "on"));
            //中文url编码
            submitEmail.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            submitEmail.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 4000);
            submitEmail.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 4000);
            CloseableHttpResponse submitEmailResponse = null;
            String submitEmailResult = null;
            int statusCode = 0;
            boolean isSubmitError = false;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    addProxy(proxy, submitEmail, task);
                    log.info(task + "提交注册请求");
                    LogUtils.log(task, "提交注册请求");
                    submitEmailResponse = HttpUtils.httpclient.execute(submitEmail, context);
                    statusCode = submitEmailResponse.getStatusLine().getStatusCode();
                    submitEmailResult = EntityUtils.toString(submitEmailResponse.getEntity());
                    break;
                } catch (IOException e) {
                    isSubmitError = true;
                    //不在RegistryMachine中重试，以免浪费已经获取到的验证码
                    log.error(task + "，提交注册请求失败：" + e.getMessage() + ",状态码：" + statusCode + ",代理ip:" + submitEmail.getParams().getParameter(ConnRoutePNames.DEFAULT_PROXY), e);
                    LogUtils.networkException(LogUtils.format(task, "提交注册请求失败：" + e.getMessage() + ",状态码：" + statusCode + "代理ip:" + submitEmail.getParams().getParameter(ConnRoutePNames.DEFAULT_PROXY)));
                    //移除无效代理
                    if (proxy != null) {
                        LogUtils.log(task, "移除无效代理：" + proxy + ",立刻切换IP重试");
                        proxy = RegistryMachineContext.proxyQueue.poll();
                    } else {
                        LogUtils.log(task, "代理已用完，不尝试重新注册");
                        log.info(task + ",代理已用完，不尝试重新注册");
                        return;
                    }
                } finally {
                    try {
                        if (submitEmailResponse != null) {
                            EntityUtils.consume(submitEmailResponse.getEntity());
                        }
                    } catch (IOException e) {
                        log.warn("close response fail", e);
                    }
                }
            }
            //为了防止提交成功，但是服务器链接不稳定抛出异常而误判失败，再对邮箱做一次校验
            if (isSubmitError) {
                LogUtils.log(task, "为了防止提交成功，但是服务器链接不稳定抛出异常而误判失败，再对邮箱做一次校验");
                if (!isEmailOK(task)) {
                    LogUtils.log(task, "注册成功");
                    LogUtils.successEmail(task);
                    log.info(task + "注册成功");
                    returnProxy(proxy);
                    return;
                }
            }

            boolean isCookieError = false;
            for (Cookie cookie : cookieStore.getCookies()) {
                if (cookie.getName().equals("errorcode")) {
                    log.info(task.getEmail() + ",验证码错误,cookie中包含的错误：" + cookie.getValue());
                    LogUtils.log(task, "提交，验证码错误，重试");
                    isCookieError = true;
                }
            }
            if (isCookieError) {
                cookieStore.clear();
                log.info(task.getEmail() + ",清空cookie中的错误代码,重试注册,cookieStore大小：" + cookieStore.getCookies().size());
                continue;
            }
            if (submitEmailResult != null && submitEmailResult.contains("您的注册数量已超过正常限制,请使用已有账号进行登录")) {
                LogUtils.log(task, ",注册数量已超过正常限制：" + submitEmail.getParams().getParameter(ConnRoutePNames.DEFAULT_PROXY));
                continue;
            } else if (cookieStore.getCookies().size() == 6) {
                LogUtils.log(task, "注册成功");
                LogUtils.successEmail(task);
                log.info(task + "注册成功");
                returnProxy(proxy);
                return;
            } else if (statusCode != 200) {
                LogUtils.log(task, "，状态码非200，重试");
                log.info(task + "，状态码非200，重试:" + submitEmailResult);
            } else {
                LogUtils.log(task, "注册失败：详细错误查看日志,尝试继续注册");
                log.error("{},尝试继续注册，cookie:{},状态码：{},网页信息：{}", task, cookieStore.getCookies(), statusCode, submitEmailResult);
            }
            if (!isSubmitError) {
                LogUtils.log(task, "即没有验证码失败，也没有网络通讯异常，再做一次邮箱验证");
                if (!isEmailOK(task)) {
                    LogUtils.log(task, "注册成功");
                    LogUtils.successEmail(task);
                    log.info(task + "注册成功");
                    returnProxy(proxy);
                    return;
                }
            }
        }

    }

    public void addProxy(String proxy, HttpRequestBase requestBase, Task task) {
        if (proxy != null) {
            String ip = proxy.split(":")[0];
            int port = Integer.parseInt(proxy.split(":")[1]);
            requestBase.getParams().removeParameter(ConnRoutePNames.DEFAULT_PROXY);
            requestBase.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, new HttpHost(ip, port, "http"));
            LogUtils.log(task, "使用代理：" + requestBase.getParams().getParameter(ConnRoutePNames.DEFAULT_PROXY));
        } else {
            LogUtils.log(task, "代理已用完，不尝试重新注册");
            throw new RuntimeException(task + ",代理已用完，不尝试重新注册");
        }
    }

    public boolean isEmailOK(Task task) {
        CookieStore cookieStore = new BasicCookieStore();
        HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(cookieStore);
        //检查邮箱
        String proxy = null;
        cookieStore.getCookies().clear();
        HttpGet checkEmail = new HttpGet("http://i.sohu.com/login/checksname?cn=" + task.getEmail());
        checkEmail.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000);
        checkEmail.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 10000);
        checkEmail.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36");
        checkEmail.addHeader("Referer", "http://i.sohu.com/login/reg.do");
        CloseableHttpResponse checkEmailResponse = null;
        int statusCode = 0;
        String checkResult = null;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                checkEmailResponse = HttpUtils.httpclient.execute(checkEmail, context);
                String checkEmailResult = EntityUtils.toString(checkEmailResponse.getEntity());
                statusCode = checkEmailResponse.getStatusLine().getStatusCode();
                try {
                    checkResult = JsonPath.read(checkEmailResult, "$.msg");
                } catch (Exception e) {
                    log.error(task + "，验证邮箱请求失败：服务的未返回json,状态码：" + statusCode, e);
                    LogUtils.log(task, "验证邮箱请求失败：服务的未返回json,状态码：" + statusCode);
                    initCookie(context);
                    cookieStore.getCookies().clear();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {

                    }
                    continue;
                }
                log.error("{}检查邮箱是否可用：{}", task, checkEmailResult);
                if ("账号可用".equals(checkResult)) {
                    log.info("邮箱可用：" + task.getEmail());
                    LogUtils.log(task, "邮箱可用");
                    return true;
                } else {
                    LogUtils.log(task, checkResult + ",邮箱不可用");
                    return false;
                }
            } catch (IOException e) {
                cookieStore.getCookies().clear();
                initCookie(context);
                //不在RegistryMachine中重试，以免浪费已经获取到的验证码
                log.error(task + "，验证邮箱请求失败：" + e.getMessage() + ",状态码：" + statusCode, e);
                LogUtils.log(task, "验证邮箱请求失败：" + e.getMessage() + ",状态码：" + statusCode);
            } finally {
                try {
                    if (checkEmailResponse != null) {
                        EntityUtils.consume(checkEmailResponse.getEntity());
                    }
                } catch (IOException e) {
                    log.warn("close response fail", e);
                }
            }
        }
        throw new RuntimeException(task + ",用户退出程序");
    }

    public void returnProxy(String proxy) {
        if (proxy != null) {
            RegistryMachineContext.proxyQueue.add(proxy);
        }
    }

    public Code getPictureCode(HttpClientContext context, Task task) throws Exception {
        Code code;
        while (!Thread.currentThread().isInterrupted()) {
            LogUtils.log(task, "获取图片验证码");
            log.info(task + "获取图片验证码");
            String path = "tmp\\" + System.currentTimeMillis() + "-Code.jpg";
            File file = new File(path);
            CloseableHttpResponse response = null;
            try {
                response = HttpUtils.httpclient.execute(new HttpGet("http://i.sohu.com/vcode/register/?nocache=" + (new Date()).getTime()), context);
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    InputStream input = response.getEntity().getContent();
                    //定义输出文件(文件格式要和资源匹配)
                    FileUtils.copyInputStreamToFile(input, file);
                } else {
                    CookieStore cookieStore = new BasicCookieStore();
                    context = HttpClientContext.create();
                    context.setCookieStore(cookieStore);
                    log.warn(task + "，非200状态码，下载失败");
                    Thread.sleep(2000);
                    continue;
                }
            } catch (IOException e) {
            } finally {
                if (response != null) {
                    try {
                        EntityUtils.consume(response.getEntity());
                    } catch (Exception e) {
                    }
                }
            }
            log.info(task + "识别图片验证码:" + path);
            LogUtils.log(task, "等待UU平台识别图片验证码，会比较慢稍等");
            String result[];
            try {
                result = UUAPI.easyDecaptcha(path, 2004);
            } catch (NullPointerException e) {
                log.info(task + "验证码识别异常，等待重试");
                LogUtils.log(task, "验证码识别异常，等待重试");
                Thread.sleep(1000);
                continue;
            }
            code = new Code(result[1], result[0]);
            log.debug(path + "图片验证码codeID:" + result[0]);
            LogUtils.log(task, "图片验证码codeID:" + result[0]);
            log.debug(path + "图片验证码Result:" + code);
            String codeInf = UUAPI.resultType.get(code);
            if (codeInf != null) {
                LogUtils.log(task, "图片验证码Result:" + codeInf);
            } else {
                LogUtils.log(task, "图片验证码Result:" + code);
            }
            if ("-1008".equals(code.getCode()) || "TIMEOUT".equals(code.getCode()) || "校验失败".equals(code.getCode())) {
                //超时，防止尝试太频繁
                UUAPI.reportError(Integer.parseInt(result[0]));
                LogUtils.log(task, "验证码超时或者乱码，向UU汇报验证码错误");
                log.info(task + ",验证码超时或者乱码，向UU汇报验证码错误");
                Thread.sleep(1000);
            } else if ("-1004".equals(code.getCode())) {
                Thread.sleep(1000);
            } else if (codeInf != null) {
                LogUtils.log(task, "验证码异常：" + codeInf + ",重试");
                Thread.sleep(3000);
            } else {
                LogUtils.uuRequest(task);
                return code;
            }
        }
        throw new RuntimeException(task + "用户退出注册");
    }

    public void initCookie(HttpClientContext context) {
        CloseableHttpResponse login = null;
        try {
            login = HttpUtils.httpclient.execute(new HttpGet("http://i.sohu.com/login/reg.do"), context);
        } catch (IOException e1) {
        } finally {
            if (login != null) {
                try {
                    EntityUtils.consume(login.getEntity());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        String uid = "2509003147";
        String pwd = "1314520";
        String pid = "6555";

        Config config = new Config(uid, pwd, pid);
        RegistryMachine registryMachine = new RegistryMachine();
        registryMachine.setConfig(config);
        registryMachine.thread(1);
        registryMachine.setTaskProcess(new HttpOldSohuTaskProcess());
        registryMachine.addTask(
                new Task("aazx123amm20", "2692194")
        );
        boolean status = UUAPI.checkAPI();    //校验API，必须调用一次，校验失败，打码不成功

        if (!status) {
            System.out.print("API文件校验失败，无法使用打码服务");
            return;
        }
        registryMachine.run();
    }
}
