package registry.machine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by edwardsbean on 2015/2/9 0009.
 */
public class AIMA {
    private static Logger log = LoggerFactory.getLogger(AIMA.class);

    private String uid;
    private String pwd;
    private String pid;
    private String token;
    private String AIMA_LOGIN_URL = "http://api.f02.cn/http.do?action=loginIn&uid=%s&pwd=%s";
    private String AIMA_GET_PHONE_URL = "http://api.f02.cn/http.do?action=getMobilenum&pid=%s&uid=%s&mobile=&size=1&token=";
    private String AIMA_GET_CODE_URL = "http://api.f02.cn/http.do?action=getVcodeAndReleaseMobile&uid=%s&token=%s&mobile=";


    public AIMA(String uid, String pwd, String pid) {
        this.uid = uid;
        this.pwd = pwd;
        this.pid = pid;
        login();
    }

    private void login() {
        AIMA_LOGIN_URL = String.format(AIMA_LOGIN_URL, uid, pwd);
        log.debug("登录爱玛:" + AIMA_LOGIN_URL);
        String loginResult = HttpUtils.Get(AIMA_LOGIN_URL);
        String[] result = loginResult.split("\\|");
        if (result.length == 2) {
            this.token = result[1];
            log.debug("成功登录爱玛平台，Response:" + loginResult);
            log.debug("成功登录爱玛平台，Token:" + token);
            AIMA_GET_PHONE_URL = String.format(AIMA_GET_PHONE_URL, pid, uid) + token;
            AIMA_GET_CODE_URL = String.format(AIMA_GET_CODE_URL, uid, token);
        } else {
            throw new MachineException("爱玛返回错误Token:" + loginResult);
        }
    }

    public String getPhone() {
        log.debug("请求手机号:" + AIMA_GET_PHONE_URL);
        String getPhoneResult = HttpUtils.Get(AIMA_GET_PHONE_URL);
        int phoneLength = getPhoneResult.split("\\|").length;
        log.debug("检测爱玛平台获取的手机号码：" + getPhoneResult);
        if (phoneLength == 2) {
            return getPhoneResult.split("\\|")[0];
        } else {
            throw new MachineException("请求爱玛返回手机号码，无法识别返回数据:" + getPhoneResult);
        }
    }

    public String getPhoneCode(String phone) {
        log.debug("向爱玛平台所要手机验证码:" + AIMA_GET_CODE_URL + phone);
        for (int i = 0; i < 20; i++) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String codeResult = HttpUtils.Get(AIMA_GET_CODE_URL + phone);
            log.debug("爱玛平台返回验证码信息:{}", codeResult);
            int length = codeResult.split("\\|").length;
            if (length == 2) {
                Pattern p = Pattern.compile("[^0-9]");
                Matcher m = p.matcher(codeResult.split("\\|")[1]);
                String code = m.replaceAll("");
                if (!code.isEmpty()) {
                    log.debug("发现野生的手机验证码一枚:" + code);
                    return code;
                }
            } else {
                log.debug("验证码不对，继续等待");
            }
        }
        throw new MachineException("向爱玛平台索要验证码超时");
    }
}
