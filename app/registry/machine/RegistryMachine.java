package registry.machine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Queue;
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
        service = Executors.newFixedThreadPool(num);
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
        init();
        log.debug("启动注册机");
        for (final Task task : queue) {
            service.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        process.process(aima, task);
                    } catch (Exception e) {
                        log.error("process task error", e);
                    } finally {
                        count.incrementAndGet();
                    }
                }
            });
        }
        service.shutdown();
    }

    public void stop() {
        service.shutdownNow();
    }
}
