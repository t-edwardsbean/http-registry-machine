package registry.machine;

import akka.actor.ActorRef;
import models.Email;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by edwardsbean on 15-2-12.
 */
public class RegistryMachineContext {
    public static boolean isDebug = false;
    public static String AIMAName;
    public static String YAOYAO;
    public static int sleepTime = 2000;
    public static ActorRef logger;
    public static String proxyFileName = "无";
    public static RegistryMachine registryMachine = new RegistryMachine();
    public static Queue<String> proxyQueue = new ConcurrentLinkedQueue<>();
    public static Queue<String> proxyInUse = new ConcurrentLinkedQueue<>();
    public static Queue<Email> emailQueue = new ConcurrentLinkedQueue<>();
    public static Queue<Task> okEmailQueue = new ConcurrentLinkedQueue<>();
    public static AtomicBoolean isRunning = new AtomicBoolean(false);
    public static StringBuilder result = new StringBuilder();
    public static AIMA aima;
    public static String encode;
    public static AtomicBoolean isFilter = new AtomicBoolean(false);

    public static void addProxy(String proxy) {
        proxyQueue.add(proxy);
    }

    public static void addProxies(List<String> proxies) {
        proxyQueue.addAll(proxies);
    }

    public void returnProxy(String proxy) {
        proxyQueue.add(proxy);
    }

    public String getProxy() {
        return proxyQueue.poll();
    }

    public static void start() {
        registryMachine.run();
    }

    public static void stop() {
        registryMachine.stop();
        isRunning.set(false);
    }

}
