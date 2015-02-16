package registry.machine;

import akka.actor.ActorRef;
import org.openqa.selenium.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by edwardsbean on 2015/2/9 0009.
 */
public class RegistryMachine {
    private static Logger log = LoggerFactory.getLogger(RegistryMachine.class);
    private Config config;
    private AIMA aima;
    private ExecutorService service;
    protected ConcurrentLinkedQueue<Task> queue = new ConcurrentLinkedQueue<Task>();
    protected TaskProcess process;
    private final AtomicLong count = new AtomicLong(0);

    public void setConfig(Config config) {
        this.config = config;
    }

    private void init() {
        this.aima = new AIMA(config.getUid(), config.getPwd(), config.getPid());
    }

    public void thread(int num) {
        this.service = Executors.newFixedThreadPool(num);
    }

    public void addTask(Task... tasks) {
        for (Task task : tasks) {
            queue.add(task);
        }
    }

    public void setTaskProcess(TaskProcess process) {
        this.process = process;
    }

    public void run() {
        RegistryMachineContext.isRunning = true;
        init();
        log.debug("启动注册机");
        RegistryMachineContext.logger.tell("启动注册机", ActorRef.noSender());
        if (queue.isEmpty()) {
            LogUtils.log("没有邮箱文件，请上传");
            RegistryMachineContext.isRunning = false;
            return;
        }
        for (final Task task : queue) {
            String proxy = RegistryMachineContext.proxyQueue.poll();
            if (proxy != null) {
                List<String> args = new ArrayList<>();
                args.add(proxy);
                task.setArgs(args);
                RegistryMachineContext.proxyQueue.add(proxy);
            }
            service.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        process.process(aima, task);
                    } catch (NoSuchElementException e) {
                        log.error("process task error", e);
                        LogUtils.networkException();
                    } catch (MachineNetworkException e) {
                        log.error("process task error", e);
                        LogUtils.networkException();
                    } catch (Exception e) {
                        log.error("process task error", e);
                        LogUtils.log(e.getMessage());
                    } finally {
                        count.incrementAndGet();
                    }
                }
            });
        }
        service.shutdown();
    }

    public void stop() {
        RegistryMachineContext.isRunning = false;
        service.shutdownNow();
    }

    @Override
    public String toString() {
        return "RegistryMachine{" +
                "config=" + config +
                ", aima=" + aima +
                ", service=" + service +
                ", queue=" + queue +
                ", process=" + process +
                ", count=" + count +
                '}';
    }
}
