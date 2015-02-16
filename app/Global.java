import play.Application;
import play.Configuration;
import play.GlobalSettings;
import play.Logger;
import registry.machine.*;


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
        String phantomjsPath = playConfiguration.getString("phantomjs.path");
        int threadNum = playConfiguration.getInt("phantomjs.thread");
        Config config = new Config(uid, pwd, pid);
        Logger.info("注册机配置：" + config);
        RegistryMachineContext.registryMachine.setConfig(config);
        RegistryMachineContext.registryMachine.thread(threadNum);
        RegistryMachineContext.registryMachine.setTaskProcess(new SinaTaskProcess(phantomjsPath));
//        RegistryMachineContext.registryMachine.addTask(new Task("asdfaz123asc", "2692194"));
    }

    @Override
    public void onStop(Application application) {
        Logger.info("Application shutdown...");
    }
}
