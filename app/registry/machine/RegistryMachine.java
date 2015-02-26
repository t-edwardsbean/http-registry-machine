package registry.machine;

import akka.actor.ActorRef;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by edwardsbean on 2015/2/9 0009.
 */
public class RegistryMachine {
    private static Logger log = LoggerFactory.getLogger(RegistryMachine.class);
    private Config config;
    private ExecutorService service;
    protected Queue<Task> queue = new LinkedBlockingQueue<>();
    protected TaskProcess process;
    private final AtomicLong count = new AtomicLong(0);
    private Thread asynRun;
    private int threadNum;
    ;

    public void cleanTask() {
        this.queue = new ConcurrentLinkedQueue<Task>();
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    private void init() {
        RegistryMachineContext.aima = new AIMA(config.getUid(), config.getPwd(), config.getPid());
    }

    public void thread(int num) {
        this.threadNum = num;
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
        RegistryMachineContext.isRunning.set(true);
        log.debug("启动注册机：" + RegistryMachineContext.isRunning.get());
        if (queue.isEmpty()) {
            log.debug("没有邮箱文件，请上传");
            RegistryMachineContext.logger.tell("没有邮箱文件，请上传", ActorRef.noSender());
            return;
        }
        init();
        RegistryMachineContext.logger.tell("启动注册机", ActorRef.noSender());
        asynRun = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final Semaphore semaphore = new Semaphore(threadNum);
                    while (!Thread.currentThread().isInterrupted()) {
                        while (!queue.isEmpty()) {
                            final Task task = queue.poll();
                            final String proxy = RegistryMachineContext.proxyQueue.poll();
                            if (proxy != null) {
                                task.getArgs().add(proxy);
                                RegistryMachineContext.proxyQueue.add(proxy);
                            }
                            service.execute(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        semaphore.acquire();
                                        process.process(task);
                                    } catch (MachineNetworkException e) {
                                        //移除无效代理
                                        LogUtils.log("移除无效代理：" + proxy);
                                        RegistryMachineContext.proxyQueue.remove(proxy);
                                        //网络异常，重试
                                        queue.add(task);
                                        log.error("process task error", e);
                                        LogUtils.networkException(e);
                                        LogUtils.log(task, "加入重试队列");
                                    } catch (UnreachableBrowserException | MachineDelayException | AIMAException e) {
                                        queue.add(task);
                                        LogUtils.log(e.getMessage());
                                        LogUtils.log(task, "加入重试队列");
                                    } catch (Exception e) {
                                        log.error("注册机错误", e);
                                        LogUtils.log(e.getMessage());
                                        LogUtils.log(task, "不进行重试");
                                    } finally {
                                        semaphore.release();
                                        count.incrementAndGet();
                                    }
                                }
                            });
                        }
                        //让任务初始化并运行
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            break;
                        }
                        //邮箱池为空，而serviceExecutor任务还在排队等待执行
//                        while (queue.isEmpty() && semaphore.availablePermits() != threadNum && !Thread.currentThread().isInterrupted()) {
//                            try {
//                                Thread.sleep(1000);
//                            } catch (InterruptedException e) {
//                                break;
//                            }
//                        }
                        //全部任务运行完成
                        if (queue.isEmpty() && semaphore.availablePermits() == threadNum) {
                            break;
                        }
                    }
                } finally {
                    LogUtils.log("注册机运行结束");
                }
            }
        });
        asynRun.start();
    }

    public void stop() {
        RegistryMachineContext.isRunning.set(false);
        service.shutdownNow();
        asynRun.interrupt();
    }

    @Override
    public String toString() {
        return "RegistryMachine{" +
                "config=" + config +
                ", service=" + service +
                ", queue=" + queue +
                ", process=" + process +
                ", count=" + count +
                '}';
    }
}
