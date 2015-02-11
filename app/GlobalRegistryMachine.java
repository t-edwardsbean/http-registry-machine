import play.Application;
import play.GlobalSettings;
import play.Logger;

/**
 * Created by edwardsbean on 15-2-11.
 */
public class GlobalRegistryMachine extends GlobalSettings {
    public static Logger.ALogger log = Logger.of("globalRegistryMachine");

    @Override
    public void beforeStart(Application application) {
        Logger.info("Application start...");
    }

    @Override
    public void onStop(Application application) {
        Logger.info("Application shutdown...");
    }
}
