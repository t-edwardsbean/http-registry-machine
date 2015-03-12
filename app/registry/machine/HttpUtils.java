package registry.machine;


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by edwardsbean on 2015/2/9 0009.
 */
public class HttpUtils {
    private static Logger log = LoggerFactory.getLogger(HttpUtils.class);
    public static CloseableHttpClient httpclient;

    static {
        SSLContext sslContext = null;
        try {
            sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).useTLS().build();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            e.printStackTrace();
        }
        HttpClientBuilder builder = HttpClientBuilder.create();
        assert sslContext != null;
        SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        builder.setSSLSocketFactory(sslConnectionFactory);
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("https", sslConnectionFactory)
                .register("http", new PlainConnectionSocketFactory())
                .build();

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
        cm.setMaxTotal(200);
        httpclient = HttpClients.custom()
                .setConnectionManager(cm)
                .build();
    }

    public static String Post(String url, HttpEntity entity, String proxy) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(entity);
        CloseableHttpResponse response = httpclient.execute(httpPost);
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                HttpEntity responseEntity= response.getEntity();
                return EntityUtils.toString(responseEntity);
            }else {
                log.warn("code error " + statusCode + "\t" + url);
                return null;
            }
            // do something useful with the response body
            // and ensure it is fully consumed
        } finally {
            EntityUtils.consume(response.getEntity());
            response.close();
        }
    }

//    public static File download(String url,String path) {
//        try {
//            File file = new File(path);
//            file.createNewFile();
//            HttpResponse response = doGet(url);
//            InputStream inputStream = response.getEntity().getContent();
//            byte buff[] = new byte[4096];
//            int counts = 0;
//            while ((counts = inputStream.read(buff)) != -1) {
//                Files.write(buff, file);
//            }
//        } catch (Exception e) {
//
//        }
//    }

    public static HttpResponse doGet(String url) {
        HttpContext context = new BasicHttpContext();
        CloseableHttpResponse response = null;
        try {
            response = httpclient.execute(new HttpGet(url), context);
            return response;
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

    public static String Get(String url) {
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
