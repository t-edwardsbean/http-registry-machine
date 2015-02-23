package registry.machine;

import akka.actor.ActorRef;
import models.Email;
import models.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by edwardsbean on 15-2-15.
 */
public class LogUtils {
    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private static String TEMPLATE = "%s [%s] %s";

    public static String format(Task task, String msg) {
        return String.format(TEMPLATE, format.format(new Date()), task.getEmail(), msg);
    }

    public static void log(Task task, String msg) {
        RegistryMachineContext.logger.tell(String.format(TEMPLATE, format.format(new Date()), task.getEmail(), msg), ActorRef.noSender());
    }

    public static void log(String msg) {
        RegistryMachineContext.logger.tell(msg, ActorRef.noSender());
    }

    public static void successEmail(Task task) {
        RegistryMachineContext.logger.tell(new Log("email", new Email(task.getEmail(), task.getPassword())), ActorRef.noSender());
    }

    public static void networkException() {
        RegistryMachineContext.logger.tell(new Log("networkError", "代理不可用或者网络超时"), ActorRef.noSender());
    }

    public static void networkException(Exception e) {
        RegistryMachineContext.logger.tell(new Log("networkError", e.getMessage()), ActorRef.noSender());
    }

    public static void emailException() {
        RegistryMachineContext.logger.tell(new Log("emailException", ""), ActorRef.noSender());
    }
}
