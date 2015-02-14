package registry.machine;

import java.io.IOException;

/**
 * Created by edwardsbean on 15-2-13.
 */
public class SohuApplication {
    public static void main(String[] args) throws IOException, InterruptedException {
        String uid = "2509003147";
        String pwd = "1314520";
        String pid = "1219";

        Config config = new Config(uid, pwd, pid);
        RegistryMachine registryMachine = new RegistryMachine();
        registryMachine.setConfig(config);
        registryMachine.thread(1);
        registryMachine.setTaskProcess(new SohuTaskProcess("/home/edwardsbean/software/phantomjs-1.9.2-linux-x86_64/bin/phantomjs"));
        registryMachine.addTask(
//                new Task("wasd1babaxq3", "2692194").setArgs(Arrays.asList("--proxy=127.0.0.1:7070", "--proxy-type=socks5")),
//                new Task("wasd123qxxc3", "2692194"),
//                new Task("azxas1asaz33", "2692194"),
//                new Task("azxas1asdxz33", "2692194"),
//                new Task("azxas1asdxz33", "2692194"),
//                new Task("azxas1asdxz33", "2692194"),
//                new Task("azxas1asdxz33", "2692194"),
                new Task("aazx123", "2692194")
        );
        registryMachine.run();

    }
}
