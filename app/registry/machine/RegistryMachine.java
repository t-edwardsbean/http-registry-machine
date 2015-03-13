package registry.machine;

import akka.actor.ActorRef;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
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
//        RegistryMachineContext.aima = new AIMA(config.getUid(), config.getPwd(), config.getPid());
    }

    public void thread(int num) {
        this.threadNum = num;
        ThreadFactory nameThreadFactory = new ThreadFactoryBuilder().setNameFormat("TaskProcessorThread-%d").build();
        this.service = Executors.newFixedThreadPool(num, nameThreadFactory);

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
        if (queue.isEmpty() && RegistryMachineContext.okEmailQueue.isEmpty()) {
            log.debug("没有邮箱文件，请上传");
            RegistryMachineContext.logger.tell("没有邮箱文件，请上传", ActorRef.noSender());
            return;
        }
        if (!RegistryMachineContext.okEmailQueue.isEmpty()) {
            queue.clear();
            queue.addAll(RegistryMachineContext.okEmailQueue);
            RegistryMachineContext.okEmailQueue.clear();
        }
        init();
        RegistryMachineContext.logger.tell("启动注册机", ActorRef.noSender());
        asynRun = new Thread(new Runnable() {
            @Override
            public void run() {
                String endException = "";
                try {
                    final Semaphore semaphore = new Semaphore(threadNum);
                    while (!Thread.currentThread().isInterrupted()) {
                        while (!queue.isEmpty()) {
                            final Task task = queue.poll();
//                            final String proxy = RegistryMachineContext.proxyQueue.poll();
                            if (RegistryMachineContext.proxyQueue.isEmpty()) {
                                log.error("代理已用完或为空");
                                LogUtils.log("代理已用完或为空");
                                break;
                            }
                            service.execute(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        semaphore.acquire();
                                        process.process(task);
//                                        RegistryMachineContext.proxyQueue.add(proxy);
                                    } catch (ConnectTimeoutException | SocketTimeoutException | SocketException | MachineException e) {
                                        //移除无效代理
//                                        LogUtils.log(task, "RegistryMachine移除无效代理：" + proxy);
//                                        RegistryMachineContext.proxyQueue.remove(proxy);
                                        //网络异常，重试
                                        if (RegistryMachineContext.proxyQueue.isEmpty()) {
                                            LogUtils.log(task, "RegistryMachine检测没有剩余代理，不重试：");
                                            log.error(task.getEmail() + ",RegistryMachine检测没有剩余代理，不重试");
                                            return;
                                        }
                                        queue.add(task);
                                        log.error(task.getEmail() + ",注册失败，RegistryMachine加入重试队列:", e);
                                        LogUtils.networkException(LogUtils.format(task, e.getMessage()));
                                        LogUtils.log(task, "RegistryMachine加入重试队列");
                                    } catch (Exception e) {
                                        log.error("注册机错误,RegistryMachine不重试", e);
                                        LogUtils.log(task, "RegistryMachine不重试:" + e.getMessage());
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
                        log.info("代理队列：" + RegistryMachineContext.proxyQueue.size() + "　任务队列：" + queue.size() + " 活跃线程：" + (threadNum - semaphore.availablePermits()));
                        //全部任务运行完成
                        if (semaphore.availablePermits() == threadNum) {
                            break;
                        }
                    }
                } finally {
                    if (RegistryMachineContext.isFilter.get()) {
                        LogUtils.log("过滤账号运行结束");
                    } else {
                        if (!queue.isEmpty()) {
                            endException = ",代理用完，剩余" + queue.size() + "未注册";
                        }
                        LogUtils.log("注册机运行结束" + endException);
                        log.info("注册机运行结束" + endException);
                    }
                }
            }
        });
        asynRun.setName("RegistryMachineThread");
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
