package controllers;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import models.Log;
import org.apache.commons.io.FileUtils;
import play.Logger;
import play.libs.Json;
import registry.machine.RegistryMachineContext;

import java.io.File;

public class MyWebSocketActor extends UntypedActor {
    public static Logger.ALogger log = Logger.of("myWebSocketActor");

    public static Props props(ActorRef out) {
        return Props.create(MyWebSocketActor.class, out);
    }

    private final ActorRef out;
    private File file = new File("result/out.txt");

    public MyWebSocketActor(ActorRef out) {
        this.out = out;
        log.debug("创建WebSocket啦");
        RegistryMachineContext.logger = getSelf();
    }

    public void onReceive(Object message) throws Exception {
        //只输出注册机在运行时的日志
        log.debug("receive message:" + message);
        log.debug("是否输出日志：" + RegistryMachineContext.isRunning.get());
        if (RegistryMachineContext.isRunning.get()) {
            if (message instanceof Log) {
                Log msg = (Log) message;
                if ("email".equals(msg.getType())) {
                    FileUtils.write(file, msg.getValue().toString() + "\r\n", true);
                    RegistryMachineContext.result.append(msg.getValue().toString()).append("\r\n");
                }
                out.tell(Json.toJson(msg).toString(), self());
            } else if (message instanceof String) {
                out.tell(Json.toJson(new Log("log", (String) message)).toString(), self());
            } else {
                unhandled(message);
            }
        }
    }
}