package registry.machine;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by edwardsbean on 2015/3/17 0017.
 */
public class YAOAPI {
    public static List<String> getProxies() {
        List<String> proxies = new ArrayList<>();
        HttpClientContext context = HttpClientContext.create();
        try (CloseableHttpResponse response = HttpUtils.httpclient.execute(new HttpGet("http://www.httpsdaili.com/api.asp?key=" + RegistryMachineContext.YAOYAO + "&getnum=300&filter=1&area=1&proxytype=0"), context)) {
            String result = EntityUtils.toString(response.getEntity());
            String[] all = result.split("\r\n");
            Collections.addAll(proxies, all);
        } catch (Exception e) {
            throw new RuntimeException("获取瑶瑶代理出错",e);
        }
        return proxies;
    }

    public static void main(String[] args) {
        List<String> proxies = getProxies();
        System.out.println(proxies);
    }
}
