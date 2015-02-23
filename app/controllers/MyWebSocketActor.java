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
    }

    @Override
    public void preStart() throws Exception {
        RegistryMachineContext.logger = getSelf();
    }

    public void onReceive(Object message) throws Exception {
        if (RegistryMachineContext.isRunning) {
            if (message instanceof Log) {
                Log msg = (Log) message;
                if ("email".equals(msg.getType())) {
                    FileUtils.write(file, msg.getValue().toString() + "\r\n", true);
                    RegistryMachineContext.result.append(msg.getValue().toString()).append("\r\n");
                }
                log.debug("receive message:" + msg);
                out.tell(Json.toJson(msg).toString(), self());
            } else if (message instanceof String) {
                log.debug("receive message:" + message);
                out.tell(Json.toJson(new Log("log", (String) message)).toString(), self());
            } else {
                unhandled(message);
            }
        }
    }
}