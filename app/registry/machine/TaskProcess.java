package registry.machine;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by edwardsbean on 2015/2/10 0010.
 */
public abstract class TaskProcess {
    protected String phantomjsPath;
    protected List<String> phantomjsArgs = new ArrayList<>();
    public TaskProcess(String phantomjsPath) {
        this.phantomjsPath = phantomjsPath;
    }

    public abstract void process(AIMA aima, Task task) throws Exception;

    public void screenShot(PhantomJSDriver session,Task task) {
        if (RegistryMachineContext.isDebug) {
            try {
                FileUtils.copyFile(((TakesScreenshot) session).getScreenshotAs(OutputType.FILE), new File("debug/" + task.getEmail() + "-debug.png"));
                Thread.sleep(RegistryMachineContext.sleepTime);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public PhantomJSDriver getSession(Task task) {
        DesiredCapabilities caps = new DesiredCapabilities();
        List<String> args = new ArrayList<String>();
        args.add("--ignore-ssl-errors=yes");
        //启动phantomjs传递的命令行参数
        if (!task.getArgs().isEmpty()) {
            LogUtils.log(task, "使用代理：" + task.getArgs().get(0));
            args.addAll(task.getArgs());
        }
        args.addAll(phantomjsArgs);
        caps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, args.toArray(new String[args.size()]));
        //phantomjs启动后的参数
        caps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_SETTINGS_PREFIX + "userAgent", "Mozilla/5.0 (Linux;U;Android 2.2.2;zh-cn;ZTE-C_N880S Build/FRF91) AppleWebkit/531.1(KHTML, like Gecko) Version/4.0 Mobile Safari/531.1");
        caps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, phantomjsPath);
        caps.setJavascriptEnabled(true);
        caps.setCapability("takesScreenshot", true);
        PhantomJSDriver session = new PhantomJSDriver(caps);
        session.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
        session.manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS);
        return session;
    }
}
