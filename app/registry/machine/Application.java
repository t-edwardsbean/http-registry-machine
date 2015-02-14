package registry.machine;


import java.io.IOException;
import java.util.Arrays;

/**
 * Created by edwardsbean on 2015/2/8 0008.
 */
public class Application {

    public static void main(String[] args) throws IOException, InterruptedException {
        String uid = "2509003147";
        String pwd = "1314520";
        String pid = "1219";

        Config config = new Config(uid, pwd, pid);
        RegistryMachine registryMachine = new RegistryMachine();
        registryMachine.setConfig(config);
        registryMachine.thread(4);
        registryMachine.setTaskProcess(new SinaTaskProcess("/home/edwardsbean/software/phantomjs-1.9.2-linux-x86_64/bin/phantomjs"));
        registryMachine.addTask(
                new Task("wasd1babaxq3", "2692194").setArgs(Arrays.asList("--proxy=127.0.0.1:7070", "--proxy-type=socks5")),
                new Task("wasd123qxxc3", "2692194"),
                new Task("azxas1asaz33", "2692194"),
                new Task("azxas1asdxz33", "2692194"),
                new Task("azxas1asdxz33", "2692194"),
                new Task("azxas1asdxz33", "2692194"),
                new Task("azxas1asdxz33", "2692194"),
                new Task("azxas1asdxz33", "2692194")
        );
        registryMachine.run();

    }
}
