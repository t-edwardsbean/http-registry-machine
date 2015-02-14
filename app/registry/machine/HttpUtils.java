package registry.machine;


import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by edwardsbean on 2015/2/9 0009.
 */
public class HttpUtils {
    private static Logger log = LoggerFactory.getLogger(HttpUtils.class);
    private static PoolingHttpClientConnectionManager cm;
    private static CloseableHttpClient httpclient;

    static {
        cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(100);
        httpclient = HttpClients.custom()
                .setConnectionManager(cm)
                .build();
    }

    public static String Get(String url) {
        if (!url.startsWith("http://")) {
            url = "http://" + url;
        }
        HttpContext context = new BasicHttpContext();
        CloseableHttpResponse response = null;
        int statusCode;
        try {
            response = httpclient.execute(new HttpGet(url), context);
            statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                return EntityUtils.toString(entity);
            } else {
                log.warn("code error " + statusCode + "\t" + url);
                return null;
            }
        } catch (Exception e) {
            log.warn("request " + url + " error", e);
            return null;
        } finally {
            try {
                if (response != null) {
                    //ensure the connection is released back to pool
                    EntityUtils.consume(response.getEntity());
                }
            } catch (IOException e) {
                log.warn("close response fail", e);
            }
        }
    }

}
