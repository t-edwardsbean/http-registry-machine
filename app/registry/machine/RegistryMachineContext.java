package registry.machine;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by edwardsbean on 15-2-12.
 */
public class RegistryMachineContext {
    private Queue<String> proxyQueue = new ConcurrentLinkedQueue<>();
    private Queue<String> emailQueue = new ConcurrentLinkedQueue<>();

    public void addProxies(List<String> proxies) {
        proxyQueue.addAll(proxies);
    }

    public void returnProxy(String proxy) {
        proxyQueue.add(proxy);
    }

    public String getProxy() {
        return proxyQueue.poll();
    }

    public void addEmails(List<String> emails) {
        emailQueue.addAll(emails);
    }

    public void returnEmail(String email) {
        emailQueue.add(email);
    }

    public String getEmail() {
        return emailQueue.poll();
    }
    
}
