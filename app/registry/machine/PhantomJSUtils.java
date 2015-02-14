package registry.machine;

import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by edwardsbean on 15-2-11.
 */
public class PhantomJSUtils {
    public RemoteWebDriver getSession() {
        DesiredCapabilities caps = new DesiredCapabilities();
        caps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, new String[]{"--ignore-ssl-errors=yes"});
        caps.setCapability("phantomjs.page.settings.userAgent", "Mozilla/5.0 (Linux;U;Android 2.2.2;zh-cn;ZTE-C_N880S Build/FRF91) AppleWebkit/531.1(KHTML, like Gecko) Version/4.0 Mobile Safari/531.1");
        caps.setJavascriptEnabled(true);
        caps.setCapability("takesScreenshot", true);
        RemoteWebDriver session = null;
        try {
            session = new RemoteWebDriver(new URL("http://localhost:9999"), caps);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        session.get("https://mail.sina.com.cn/register/regmail.php");
        return session;
    }
}
