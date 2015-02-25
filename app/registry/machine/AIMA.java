package registry.machine;

import akka.actor.ActorRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
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
    private String AIMA_IGNORE = "http://api.f02.cn/http.do?action=addIgnoreList&uid=%s&token=%s&pid=%s&mobiles=";
    private Random random = new Random(2000);

    public AIMA(String uid, String pwd, String pid) {
        this.uid = uid;
        this.pwd = pwd;
        this.pid = pid;
        login();
    }

    private void login() {
        AIMA_LOGIN_URL = String.format(AIMA_LOGIN_URL, uid, pwd);
        log.debug("登录爱玛:" + AIMA_LOGIN_URL);
        RegistryMachineContext.logger.tell("登录爱玛:" + AIMA_LOGIN_URL, ActorRef.noSender());
        String loginResult = HttpUtils.Get(AIMA_LOGIN_URL);
        String[] result = loginResult.split("\\|");
        if (result.length == 2) {
            this.token = result[1];
            log.debug("成功登录爱玛平台，Response:" + loginResult);
            RegistryMachineContext.logger.tell("成功登录爱玛平台，Response:" + loginResult, ActorRef.noSender());
            log.debug("成功登录爱玛平台，Token:" + token);
            AIMA_IGNORE = String.format(AIMA_IGNORE, uid, token, pid);
            RegistryMachineContext.logger.tell("成功登录爱玛平台，Token:" + token, ActorRef.noSender());
            AIMA_GET_PHONE_URL = String.format(AIMA_GET_PHONE_URL, pid, uid) + token;
            AIMA_GET_CODE_URL = String.format(AIMA_GET_CODE_URL, uid, token);
        } else {
            throw new AIMAException("爱玛返回错误Token:" + loginResult);
        }
    }

    public void addIgnoreList(Task task) {
        log.debug("爱码平台未返回或返回错误验证码，加黑无用手机号码");
        LogUtils.log(task, "爱码平台未返回或返回错误验证码，加黑无用手机号码");
        String result = HttpUtils.Get(AIMA_IGNORE + task.getEmail());
        log.debug("加黑结果：" + result);
        LogUtils.log(task, "加黑结果：" + result);
    }

    public String getPhone(Task task) {
        log.debug("请求手机号:" + AIMA_GET_PHONE_URL);
        try {
            Thread.sleep(random.nextInt(2000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        LogUtils.log(task, "请求手机号:" + AIMA_GET_PHONE_URL);
        String getPhoneResult = HttpUtils.Get(AIMA_GET_PHONE_URL);
        int phoneLength = getPhoneResult.split("\\|").length;
        log.debug("检测爱码平台获取的手机号码：" + getPhoneResult);
        LogUtils.log(task, "检测爱码平台获取的手机号码：" + getPhoneResult);
        if (phoneLength == 2) {
            return getPhoneResult.split("\\|")[0];
        } else {
            throw new AIMAException("请求爱码返回手机号码，无法识别返回数据:" + getPhoneResult);
        }
    }

    public String getPhoneCode(Task task, String phone) {
        log.debug("向爱玛平台所要手机验证码:" + AIMA_GET_CODE_URL + phone);
        LogUtils.log(task, "向爱玛平台所要手机验证码:" + AIMA_GET_CODE_URL + phone);
        for (int i = 0; i < 20; i++) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String codeResult = HttpUtils.Get(AIMA_GET_CODE_URL + phone);
            LogUtils.log(task, "爱玛平台返回验证码信息:" + codeResult);
            log.debug("爱玛平台返回验证码信息:{}", codeResult);
            int length = codeResult.split("\\|").length;
            if (length == 2) {
                Pattern p = Pattern.compile("[^0-9]");
                Matcher m = p.matcher(codeResult.split("\\|")[1]);
                String code = m.replaceAll("");
                if (!code.isEmpty()) {
                    log.debug("发现野生的手机验证码一枚:" + code);
                    LogUtils.log(task, "发现野生的手机验证码一枚:" + code);
                    return code;
                }
            } else {
                log.debug("验证码不对，继续等待");
                LogUtils.log(task, "验证码不对，继续等待");
            }
        }
        addIgnoreList(task);
        throw new AIMAException(LogUtils.format(task, "向爱玛平台索要验证码超时"));
    }
}
