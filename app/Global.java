import com.google.common.io.Files;
import models.Log;
import org.apache.commons.io.FileUtils;
import play.Application;
import play.Configuration;
import play.GlobalSettings;
import play.Logger;
import registry.machine.*;

import java.io.File;
import java.io.IOException;


/**
 * Created by edwardsbean on 15-2-11.
 */
public class Global extends GlobalSettings {

    @Override
    public void onStart(Application application) {
        Configuration playConfiguration = Configuration.root();
        Logger.info("Application start...");
        String uid = playConfiguration.getString("aima.uid");
        RegistryMachineContext.AIMAName = uid;
        String pwd = playConfiguration.getString("aima.pwd");
        String pid = playConfiguration.getString("aima.pid");
        UUAPI.USERNAME = playConfiguration.getString("uu.uid");
        UUAPI.PASSWORD = playConfiguration.getString("uu.pwd");
        boolean isDebug = playConfiguration.getBoolean("phantomjs.debug");
        String yaoyao = playConfiguration.getString("yaoyao.key");
        String phantomjsPath = playConfiguration.getString("phantomjs.path");
        int threadNum = playConfiguration.getInt("phantomjs.thread");
        Config config = new Config(uid, pwd, pid);
        config.setPhantomjsPath(phantomjsPath);
        String encode = playConfiguration.getString("uu.encode");
        String folder=System.getProperty("java.io.tmpdir");
        Logger.info("注册机配置：" + UUAPI.USERNAME + "," + UUAPI.PASSWORD);
        Logger.info("是否调试模式：{}", isDebug);
        Logger.info("临时文件夹：{}", folder);
        Logger.info("UU验证码编码：{}", encode);
        Logger.info("瑶瑶key：{}", yaoyao);
        RegistryMachineContext.encode = encode;
        RegistryMachineContext.isDebug = isDebug;
        RegistryMachineContext.YAOYAO = yaoyao;
        RegistryMachineContext.registryMachine.setConfig(config);
        RegistryMachineContext.registryMachine.thread(threadNum);
        RegistryMachineContext.registryMachine.setTaskProcess(new HttpOldSohuTaskProcess());
        boolean status = false;    //校验API，必须调用一次，校验失败，打码不成功
        File tmp = new File("tmp");
        try {
            FileUtils.deleteDirectory(tmp);
        } catch (IOException e) {
            Logger.info("清理临时文件夹失败：tmp");
        }
        tmp.mkdir();
        File c = new File("lib\\UUWiseHelper.dll");
        Logger.debug("文件是否存在：" + c.exists());
        try {
            status = UUAPI.checkAPI();
        } catch (IOException e) {
            Logger.error("API文件校验失败，无法使用打码服务:" + e.getMessage());
            return;
        }

        if (!status) {
            Logger.error("API文件校验失败，无法使用打码服务");
            return;
        } else {
            Logger.info("校验UUAPI成功");
        }
        Logger.info("初始化代理");
        RegistryMachineContext.addProxies(YAOAPI.getProxies());
        Logger.info("代理池大小：" + RegistryMachineContext.proxyQueue.size());


//        RegistryMachineContext.registryMachine.addTask(new Task("asdfaz123asc", "2692194"));
    }

    @Override
    public void onStop(Application application) {
        Logger.info("Application shutdown...");
    }
}
