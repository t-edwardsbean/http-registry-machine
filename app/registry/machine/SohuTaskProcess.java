package registry.machine;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by edwardsbean on 15-2-13.
 */
public class SohuTaskProcess extends TaskProcess {
    private static Logger log = LoggerFactory.getLogger(SohuTaskProcess.class);
    public static String SOHU = "https://passport.sohu.com/web/dispatchAction.action?appid=1000&ru=http://login.mail.sohu.com/reg/signup_success.jsp";

    public SohuTaskProcess(String phantomjsPath) {
        super(phantomjsPath);
    }

    @Override
    public void process(AIMA aima, Task task) {
        DesiredCapabilities caps = new DesiredCapabilities();
        List<String> args = new ArrayList<String>();
        args.add("--ignore-ssl-errors=yes");
        //启动phantomjs传递的命令行参数
        if (task.getArgs() != null) {
            args.addAll(task.getArgs());
        }
        caps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, args.toArray(new String[args.size()]));
        //phantomjs启动后的参数
        caps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_SETTINGS_PREFIX + "userAgent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.111 Safari/537.36");
        caps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, phantomjsPath);
        caps.setJavascriptEnabled(true);
        caps.setCapability("takesScreenshot", true);
        PhantomJSDriver session = null;
        try {
            session = new PhantomJSDriver(caps);
            session.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
            session.manage().timeouts().pageLoadTimeout(5, TimeUnit.SECONDS);
            try {
                session.get(SOHU);
            } catch (Exception e) {
                throw new MachineException("网络超时，或者代理不可用", e);
            }
            String beginTitle = session.getTitle();
            if (beginTitle.isEmpty()) {
                throw new MachineException("找不到登陆头,网路超时或代理不可用");
            }
            log.debug("检测登陆头：" + beginTitle);
            log.debug(Thread.currentThread() + "填写邮箱名字");
            String email = task.getEmail();
            if (17 < email.length() || email.length() < 6) {
                throw new MachineException(Thread.currentThread() + "邮箱的长度应该在6-16个字符之间");
            } else if (!email.matches("\\w+")) {
                throw new MachineException(Thread.currentThread() + "邮箱名仅允许使用小写英文、数字或下划线");
            }
            WebElement emailElement = session.findElementByCssSelector("#email");
            WebElement passwordElementOne = session.findElementByCssSelector("#psw1");
            WebElement passwordElementAgain = session.findElementByCssSelector("#psw11");
            WebElement serviceAdmit = session.findElementByCssSelector("#email_reg > p:nth-child(8) > input");
            serviceAdmit.click();
            emailElement.sendKeys(email);
            WebElement emailElementAlert = session.findElementByCssSelector("#email_reg > p:nth-child(2) > span:nth-child(4) em");
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String emailAlert = emailElementAlert.getAttribute("class");
            //貌似浏览器内核版本不支持搜狐的邮箱名验证？
            if (emailAlert.equals("success")) {
                log.debug(Thread.currentThread() + "邮箱ok,继续");
            } else {
                throw new MachineException(Thread.currentThread() + "邮箱不合法：" + emailElementAlert.getText());
            }
            String password = task.getPassword();
            if (17 < password.length() || password.length() < 6) {
                throw new MachineException(Thread.currentThread() + "密码的长度应该在6-16个字符之间");
            }
            passwordElementOne.sendKeys(password);
            passwordElementAgain.sendKeys(password);
            WebElement phoneElement = session.findElementByCssSelector("#email_reg input[name=mobile]");
//            String phone = aima.getPhone();
//            phoneElement.sendKeys(phone);
            phoneElement.sendKeys("18046049822");
            WebElement releaseElement = session.findElementByCssSelector("#email_reg a.mt5");
            releaseElement.click();
            try {
                log.debug("等待第一个验证码");
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            log.debug("截取第一个图片验证码");
            WebElement firstPicture = session.findElementByCssSelector("body > div.modal.verification.popsmsyzm > div.vContext > img");
            log.debug("图片验证码：" + firstPicture.getTagName());
            try {
//                TakeScreenShot(session, firstPicture);
                TakeScreenShot(session, session.findElementByCssSelector("div.modal.verification.popsmsyzm"));
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            log.debug("输入第一个图片验证码");
            String firstPictureCode = "";
            WebElement firstPictureVerify = session.findElementByCssSelector("body > div.modal.verification.popsmsyzm > div.vContext > input");
            firstPictureVerify.sendKeys(firstPictureCode);
            log.debug("验证第一个图片验证码");
            WebElement firstPictureButton = session.findElementByCssSelector("body > div.modal.verification.popsmsyzm > a.blue_btn");
            firstPictureButton.click();
        } catch (MachineException e) {
            e.printStackTrace();
        } finally {
            try {
                FileUtils.copyFile(((TakesScreenshot) session).getScreenshotAs(OutputType.FILE), new File(Thread.currentThread().getId() + "sohu-end-exception.png"));
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (session != null) {
                session.quit();
            }
        }
    }

    public static void TakeScreenShot(PhantomJSDriver driver, WebElement element) {
        File screen = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        BufferedImage img = null;
        try {
            img = ImageIO.read(screen);
        } catch (IOException e) {
            e.printStackTrace();
        }

        File f = new File("element.png");

        Point point = element.getLocation();
        int width = element.getSize().getWidth();
        int height = element.getSize().getHeight();
        BufferedImage dest = img.getSubimage(point.getX(), point.getY(), width, height);
        try {
            ImageIO.write(dest, "png", screen);
            FileUtils.copyFile(screen, f);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
