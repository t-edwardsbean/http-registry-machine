package controllers;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import models.Log;
import play.Logger;
import play.libs.Akka;
import play.libs.Json;
import registry.machine.RegistryMachineContext;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class MyWebSocketActor extends UntypedActor {
    public static Logger.ALogger log = Logger.of("myWebSocketActor");

    public static Props props(ActorRef out) {
        return Props.create(MyWebSocketActor.class, out);
    }

    private final ActorRef out;

    public MyWebSocketActor(ActorRef out) {
        this.out = out;
        log.debug("创建WebSocket啦");
    }

    @Override
    public void preStart() throws Exception {
        RegistryMachineContext.logger = getSelf();
    }

    public void onReceive(Object message) throws Exception {
        if (message instanceof Log) {
            log.debug("receive message:" + Json.toJson(message));
            out.tell(Json.toJson(message).toString(), self());
        } else {
            unhandled(message);
        }
    }
}