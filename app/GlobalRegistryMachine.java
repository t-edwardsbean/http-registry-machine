import play.Application;
import play.Configuration;
import play.GlobalSettings;
import play.Logger;
import registry.machine.RegistryMachineContext;

/**
 * Created by edwardsbean on 15-2-11.
 */
public class GlobalRegistryMachine extends GlobalSettings {
    public static Logger.ALogger log = Logger.of("globalRegistryMachine");

    @Override
    public void beforeStart(Application application) {
        Logger.info("Application start...");
        RegistryMachineContext.phantomjsPath = Configuration.root().getString("phantomjs.path");
    }

    @Override
    public void onStop(Application application) {
        Logger.info("Application shutdown...");
    }
}
