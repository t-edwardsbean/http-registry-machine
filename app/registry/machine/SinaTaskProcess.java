package registry.machine;

import akka.actor.ActorRef;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by edwardsbean on 2015/2/9 0009.
 */
public class SinaTaskProcess extends TaskProcess {
    private static Logger log = LoggerFactory.getLogger(SinaTaskProcess.class);

    public SinaTaskProcess(String phantomjsPath) {
        super(phantomjsPath);
    }

    public void process(AIMA aima, Task task) throws Exception {
        DesiredCapabilities caps = new DesiredCapabilities();
        List<String> args = new ArrayList<String>();
        args.add("--ignore-ssl-errors=yes");
        //启动phantomjs传递的命令行参数
        if (!task.getArgs().isEmpty()) {
            LogUtils.log(task, "使用代理：" + task.getArgs().get(0));
            args.addAll(task.getArgs());
        }
        caps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, args.toArray(new String[args.size()]));
        //phantomjs启动后的参数
        caps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_SETTINGS_PREFIX + "userAgent", "Mozilla/5.0 (Linux;U;Android 2.2.2;zh-cn;ZTE-C_N880S Build/FRF91) AppleWebkit/531.1(KHTML, like Gecko) Version/4.0 Mobile Safari/531.1");
        caps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, phantomjsPath);
        caps.setJavascriptEnabled(true);
        caps.setCapability("takesScreenshot", true);
        PhantomJSDriver session = new PhantomJSDriver(caps);
        session.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
        session.manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS);
        try {
            try {
                session.get("https://mail.sina.com.cn/register/regmail.php");
            } catch (Exception e) {
                throw new MachineNetworkException(LogUtils.format(task, "网络超时，或者代理不可用:" + task.getArgs().get(0)), e);
            }
            //监听alert
//            ((JavascriptExecutor) session).executeScript("var lastAlert = undefined;window.alert = function(msg){lastAlert = msg;};");
            String beginTitle = session.getTitle();
            if (beginTitle.isEmpty()) {
                throw new MachineNetworkException(LogUtils.format(task, "找不到登陆头,网路超时或代理不可用"));
            }
            log.debug("检测登陆头：" + beginTitle);
            LogUtils.log(task, "检测登陆头：" + beginTitle);
            log.debug(Thread.currentThread() + "填写邮箱名字");
            LogUtils.log(task, "填写邮箱名字");
            String email = task.getEmail();
            if (17 < email.length() || email.length() < 6) {
                throw new MachineException(LogUtils.format(task, "邮箱的长度应该在6-16个字符之间"));
            } else if (!email.matches("\\w+")) {
                throw new MachineException(LogUtils.format(task, "邮箱名仅允许使用小写英文、数字或下划线"));
            }

            WebElement emailElement = session.findElementByCssSelector("#emailName");
            WebElement passwordElement = session.findElementByCssSelector("#password_2");
            emailElement.sendKeys(email);
            String password = task.getPassword();
            if (17 < password.length() || password.length() < 6) {
                throw new MachineException(LogUtils.format(task, "密码的长度应该在6-16个字符之间"));
            }
            passwordElement.sendKeys(password);
            WebElement emailElementAlert = session.findElementByXPath("//*[@id=\"form_2\"]/ul/li[1]/p");
            //如何监控emailcheck.php
            try {
                Thread.sleep(RegistryMachineContext.sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!emailElementAlert.getText().isEmpty()) {
                LogUtils.emailException();
                RegistryMachineContext.registryMachine.queue.remove(task);
                throw new MachineException(LogUtils.format(task, "邮箱不合法：" + emailElementAlert.getText()));
            } else {
                log.debug(Thread.currentThread() + "邮箱ok,继续");
                LogUtils.log(task, "邮箱ok,继续");
            }
            WebElement phoneElement = session.findElementByCssSelector("#phoneNum_2");
            String phone = aima.getPhone(task);
            phoneElement.sendKeys(phone);
            WebElement releaseCodeElement = session.findElementByCssSelector("#getCode_2");
            releaseCodeElement.click();
            //再次点击
            String releaseCodeMessage = session.findElementByCssSelector("#getCode_2").getText();
            log.debug("验证码发送情况：" + releaseCodeMessage);
            LogUtils.log(task, "验证码发送情况：" + releaseCodeMessage);
            if (!releaseCodeMessage.endsWith("秒后重新获取")) {
                try {
                    Thread.sleep(RegistryMachineContext.sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                releaseCodeElement.click();
                log.debug("再次点击获取验证码：" + session.findElementByCssSelector("#getCode_2").getText());
                LogUtils.log(task, "再次点击获取验证码：" + session.findElementByCssSelector("#getCode_2").getText());
            }
            String code = aima.getPhoneCode(task, phone);
            if (code != null) {
                WebElement codeElement = session.findElementByCssSelector("#checkCode_2");
                codeElement.sendKeys(code);
            }
            WebElement submit = session.findElementByCssSelector("#openNow_2");
            submit.click();
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String endTitle = session.getTitle();
            log.debug("成功后跳转：" + endTitle);
            LogUtils.log(task, "成功后跳转：" + endTitle);
            if (!beginTitle.equals(endTitle)) {
                log.debug(Thread.currentThread() + "注册成功，账号：{},密码：{}", email, password);
                LogUtils.log(task, "注册成功，账号：" + email + ",密码：" + password);
                LogUtils.successEmail(task);
                RegistryMachineContext.registryMachine.queue.remove(task);
            } else {
//                Object alert =  ((JavascriptExecutor) session).executeScript("return lastAlert");

                try {
                    FileUtils.copyFile(((TakesScreenshot) session).getScreenshotAs(OutputType.FILE), new File("debug/" + Thread.currentThread().getId() + email + "-end-exception.png"));
                    Thread.sleep(RegistryMachineContext.sleepTime);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                throw new MachineException(LogUtils.format(task, "注册失败"));
            }
        } finally {
            try {
                FileUtils.copyFile(((TakesScreenshot) session).getScreenshotAs(OutputType.FILE), new File("debug/" + task.getEmail() + "-debug.png"));
                Thread.sleep(RegistryMachineContext.sleepTime);
            } catch (Exception e) {
                e.printStackTrace();
            }
            session.quit();
        }
    }
}
