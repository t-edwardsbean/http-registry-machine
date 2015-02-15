package registry.machine;

/**
 * Created by edwardsbean on 2015/2/10 0010.
 */
public abstract class TaskProcess {
    protected String phantomjsPath;

    public TaskProcess(String phantomjsPath) {
        this.phantomjsPath = phantomjsPath;
    }

    public abstract void process(AIMA aima, Task task) throws Exception;
    
}
