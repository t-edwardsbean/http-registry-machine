package registry.machine;


import com.sun.jna.Library;
import com.sun.jna.Native;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

public class UUAPI {
    private static Logger log = LoggerFactory.getLogger(UUAPI.class);

    public static String USERNAME = "LIN2509003147";            //UU用户名
    public static String PASSWORD = "yq0206";                        //UU密码
    public static String DLLPATH = "libs/UUWiseHelper";                    //DLL
    public static int SOFTID = 103837;                                    //软件ID 获取方式：http://dll.uuwise.com/index.php?n=ApiDoc.GetSoftIDandKEY
    public static String SOFTKEY = "f8171bc5acc0489ea2387aa6469d3442";    //软件KEY 获取方式：http://dll.uuwise.com/index.php?n=ApiDoc.GetSoftIDandKEY
    public static String DLLVerifyKey = "23C6E062-21DC-4735-AFB1-D96F4BBCE168";    //校验API文件是否被篡改，实际上此值不参与传输，关系软件安全，高手请实现复杂的方法来隐藏此值，防止反编译,获取方式也是在后台获取软件ID和KEY一个地方
    public static boolean checkStatus = false;


    public interface UUDLL extends Library        //载入优优云的静态库
    {
        UUDLL INSTANCE = (UUDLL) Native.loadLibrary(DLLPATH, UUDLL.class);

        public int uu_reportError(int id);

        public int uu_setTimeOut(int nTimeOut);

        public int uu_loginA(String UserName, String passWord);

        public int uu_recognizeByCodeTypeAndBytesA(byte[] picContent, int piclen, int codeType, byte[] returnResult);

        public void uu_getResultA(int nCodeID, String pCodeResult);

        public int uu_getScoreA(String UserName, String passWord);    //查题分

        public int uu_easyRecognizeFileA(int softid, String softkey, String userName, String password, String imagePath, int codeType, byte[] returnResult);//一键识别函数

        public int uu_easyRecognizeBytesA(int softid, String softkey, String username, String pasword, byte[] picContent, int piclen, int codeType, byte[] returnResult);

        public void uu_CheckApiSignA(int softID, String softKey, String guid, String filemd5, String fileCRC, byte[] returnResult); //api校验函数
    }


    public static int getScore() {
        return UUDLL.INSTANCE.uu_getScoreA(USERNAME, PASSWORD);
    }

    public static int reportError(int id) {
        return UUDLL.INSTANCE.uu_reportError(id);
    }

    public static String[] easyDecaptcha(String picPath, int codeType) throws IOException {
        if (!checkStatus) {

            String rs[] = {"-19004", "API校验失败,或未校验"};
            return rs;
        }

        File f = new File(picPath);
        byte[] by = null;
        try {
            by = toByteArray(f);
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] resultBtye = new byte[100];        //为识别结果申请内存空间
        UUDLL.INSTANCE.uu_setTimeOut(60000);
        int codeID = UUDLL.INSTANCE.uu_easyRecognizeBytesA(SOFTID, SOFTKEY, USERNAME, PASSWORD, by, by.length, codeType, resultBtye);
        String resultResult = null;
        try {
            resultResult = new String(resultBtye,RegistryMachineContext.encode);//如果是乱码，这改成UTF-8试试
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        resultResult = resultResult.trim();

        String code;
        //下面这两条是为了防止被破解
        if (resultResult.split("_").length == 2) {
            code = resultResult.split("_")[1];
        }else if(resultResult.indexOf("_") < 0) {
            code = resultResult;
        } else {
            log.warn("checkResult之前校验失败,codeID:{},codeResult:{}", codeID, resultResult);
            code = "校验失败";
        }

        String rs[] = {String.valueOf(codeID), checkResult(resultResult, codeID)};
        return rs;
    }


    public static boolean checkAPI() throws IOException {
        String FILEMD5 = GetFileMD5(DLLPATH + ".dll"); //API文件的MD5值
        String FILECRC = doChecksum(DLLPATH + ".dll");    //API文件的CRC32值
        String GUID = Md5(Long.toString(Math.round(Math.random() * 11111 + 99999)));    //随机值，此值一定要每次运算都变化

        //本地验证结果:
        String okStatus = Md5(SOFTID + (DLLVerifyKey.toUpperCase()) + GUID.toUpperCase() + FILEMD5.toUpperCase() + FILECRC.toUpperCase());

        byte[] CheckResultBtye = new byte[512];
        /**
         * uu_CheckApiSignA用于防止别人替换优优云的API文件
         * 后面对结果再进行校验则是避免被HOOK，从而防止恶意盗码
         * */
        UUDLL.INSTANCE.uu_CheckApiSignA(SOFTID, SOFTKEY.toUpperCase(), GUID.toUpperCase(), FILEMD5.toUpperCase(), FILECRC.toUpperCase(), CheckResultBtye);

        String checkResultResult = new String(CheckResultBtye, "UTF-8");
        checkResultResult = checkResultResult.trim();


        checkStatus = true;
        return checkResultResult.equals(okStatus);
    }


    public static String checkResult(String dllResult, int CodeID) {
        //dll返回的是错误代码
        if (dllResult.indexOf("_") < 0)
            return dllResult;

        log.info("UUAPI，验证码结果为：" + dllResult);
        //对结果进行校验
        String[] re = dllResult.split("_");
        String verify = re[0];
        String code = re[1];
        String localMd5 = null;
        try {
            localMd5 = Md5(SOFTID + DLLVerifyKey + CodeID + code.toUpperCase()).toUpperCase();
            //System.out.println("local checkValue:"+localMd5+"code:"+code);
        } catch (IOException e) {
            // TODO 自动生成的 catch 块
            e.printStackTrace();
        }
        if (localMd5.equals(verify))    //判断本地验证结果和服务器返回的验证结果是否一至，防止API被hook
            return code;
        else
            return "校验失败";
    }

    public static byte[] toByteArray(File imageFile) throws Exception {
        BufferedImage img = ImageIO.read(imageFile);
        ByteArrayOutputStream buf = new ByteArrayOutputStream((int) imageFile.length());
        try {
            ImageIO.write(img, "jpg", buf);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return buf.toByteArray();
    }

    public static byte[] toByteArrayFromFile(String imageFile) throws Exception {
        InputStream is = null;

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            is = new FileInputStream(imageFile);
            byte[] b = new byte[1024];
            int n;
            while ((n = is.read(b)) != -1) {
                out.write(b, 0, n);
            }// end while

        } catch (Exception e) {
            throw new Exception("System error,SendTimingMms.getBytesFromFile", e);
        } finally {

            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                }// end try
            }// end if

        }// end try
        return out.toByteArray();
    }
    //CRC32函数开始

    public static String doChecksum(String fileName) {

        try {

            CheckedInputStream cis = null;
            try {
                // Computer CRC32 checksum
                cis = new CheckedInputStream(
                        new FileInputStream(fileName), new CRC32());

            } catch (FileNotFoundException e) {
                //System.err.println("File not found.");
                //System.exit(1);
            }

            byte[] buf = new byte[128];
            while (cis.read(buf) >= 0) {
            }

            long checksum = cis.getChecksum().getValue();
            cis.close();
            //System.out.println( Integer.toHexString(new Long(checksum).intValue()));
            return Integer.toHexString(new Long(checksum).intValue());

        } catch (IOException e) {
            e.printStackTrace();
            //System.exit(1);
        }

        return null;

    }
    //CRC32函数结束


    //MD5校验函数开始

    /**
     * 获取指定文件的MD5值
     *
     * @param inputFile 文件的相对路径
     */
    public static String GetFileMD5(String inputFile) throws IOException {
        int bufferSize = 256 * 1024;
        FileInputStream fileInputStream = null;
        DigestInputStream digestInputStream = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            fileInputStream = new FileInputStream(inputFile);
            digestInputStream = new DigestInputStream(fileInputStream, messageDigest);
            byte[] buffer = new byte[bufferSize];
            while (digestInputStream.read(buffer) > 0) ;
            messageDigest = digestInputStream.getMessageDigest();
            byte[] resultByteArray = messageDigest.digest();
            return byteArrayToHex(resultByteArray);
        } catch (NoSuchAlgorithmException e) {
            return null;
        } finally {
            try {
                digestInputStream.close();
            } catch (Exception e) {

            }
            try {
                fileInputStream.close();
            } catch (Exception e) {

            }
        }
    }

    public static String Md5(String s) throws IOException {
        try {
            byte[] btInput = s.getBytes();
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            mdInst.update(btInput);
            byte[] md = mdInst.digest();
            return byteArrayToHex(md);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    public static String byteArrayToHex(byte[] byteArray) {
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        char[] resultCharArray = new char[byteArray.length * 2];
        int index = 0;
        for (byte b : byteArray) {
            resultCharArray[index++] = hexDigits[b >>> 4 & 0xf];
            resultCharArray[index++] = hexDigits[b & 0xf];
        }
        return new String(resultCharArray);
    }

    public static Map<String, String> resultType = new HashMap<String, String>();
    public static Map<String, String> errorType = new HashMap<String, String>();

    static {
        resultType.put("-1001", "网络连接失败");
        resultType.put("-1002", "网络传输超时");
        resultType.put("-1003", "文件读取失败");
        resultType.put("-1004", "图像内存流无效");
        resultType.put("-1005", "服务器返回内容错误");
        resultType.put("-1006", "服务器状态错误");
        resultType.put("-1007", "内存分配失败");
        resultType.put("-1008", "没有取到验证码结果，超时");
        resultType.put("-1009", "此时不允许进行该操作（用户没有登录会出现这个错误）");
        resultType.put("-1010", "图片过大，限制1MB");
        resultType.put("-1011", "图片转换为JPG失败，源图片无效？");
        resultType.put("-1012", "获取服务器配置信息失败，是由于网络连接失败造成的");
        resultType.put("-1013", "传入的字符串缓冲区不足");
        resultType.put("-1014", "URL下载失败！");
        resultType.put("-1015", "连续重复的图片次数达到设定值");
        resultType.put("-1016", "找不到窗口标题");
        resultType.put("-1017", "找不到窗口句柄");
        resultType.put("-1020", "切换账户登录超过20次");
        errorType.put("-1020", "切换账户登录超过20次");
        resultType.put("-1021", "1分钟之内查分超过10次");
        errorType.put("-1021", "1分钟之内查分超过10次");
        errorType.put("-1022", "查分时未登录，只有登录之后才能查分");
        resultType.put("-1022", "查分时未登录，只有登录之后才能查分");
        resultType.put("-1023", "文件上传成功，服务器返回的验证码id为0");
        resultType.put("-1024", "上传时codeType为0");
        resultType.put("-1025", "程序捕获到SEH异常");
        resultType.put("-11001", "参数错误");
        resultType.put("-11002", "用户名或密码错误");
        resultType.put("-11003", "帐户被锁定");
        resultType.put("-11004", "用户绑定的了软件，登录的不是绑定的软件");
        resultType.put("-11005", "用户绑定了IP，登录的不是绑定地区的IP");
        resultType.put("-11006", "帐户登录过于频繁");
        resultType.put("-11007", "当前IP被禁止登录（例如连接登录超过5个帐户）");
        resultType.put("-11008", "该IP被限制登录");
        resultType.put("-11009", "余额不足");
        resultType.put("-11010", "帐户异常");
        resultType.put("-11011", "此帐户需要修改密码后才可以登录");
        resultType.put("-11012", "此帐户需要添加密保后才可以登录");
        resultType.put("-11013", "参数不全");
        resultType.put("-11014", "服务器无法使用TEAKEY解密");
        resultType.put("-11015", "软件已被用户屏蔽");
        errorType.put("-1025", "程序捕获到SEH异常");
        errorType.put("-1026", "未调用setSoftInfo函数");
        resultType.put("-1026", "未调用setSoftInfo函数");
        errorType.put("-1101", "无效的异步操作句柄（异步调用函数专有）");
        resultType.put("-1101", "无效的异步操作句柄（异步调用函数专有）");
        errorType.put("-1102", "异步操作尚未完成（异步调用函数专有）");
        resultType.put("-1102", "异步操作尚未完成（异步调用函数专有）");
        resultType.put("-1025", "DLL出现异常(通过配置文件设置dll异常时可生成dump文件)");
        errorType.put("-1025", "DLL出现异常(通过配置文件设置dll异常时可生成dump文件)");
        errorType.put("-1026", "软件未初始化");
        resultType.put("-1026", "软件未初始化");
        resultType.put("-16009", "1分钟之内查分超过10次");
        errorType.put("-16009", "1分钟之内查分超过10次");
        resultType.put("-16002", "查分时未登录，只有登录之后才能查分");
        errorType.put("-16002", "查分时未登录，只有登录之后才能查分");
        resultType.put("-11006", "账户登录过于频繁");
        errorType.put("-11006", "账户登录过于频繁");
        resultType.put("-14009", "密码过于简单");
        errorType.put("-14009", "密码过于简单");
        resultType.put("-17009", "报错过于频繁");
        errorType.put("-17009", "报错过于频繁");
        resultType.put("-17011", "软件报错功能被关闭");
        errorType.put("-17011", "软件报错功能被关闭");
        resultType.put("-19001", "UserAgent内容错误");
        errorType.put("-19001", "UserAgent内容错误");
        resultType.put("-19002", "API版本号错误");
        errorType.put("-19002", "API版本号错误");
        resultType.put("-19003", "TTL错误");
        errorType.put("-19003", "TTL错误");
        resultType.put("-19004", "API文件MD5校验失败");
        errorType.put("-19004", "API文件MD5校验失败");
        resultType.put("-19005", "API文件版本不存在");
        errorType.put("-19005", "API文件版本不存在");
        resultType.put("-19006", "API文件版本被篡改");
        errorType.put("-19006", "API文件版本被篡改");
        resultType.put("-19007", "API文件版本已过期");
        errorType.put("-19007", "API文件版本已过期");
        resultType.put("-19008", "此API版本已被禁用");
        errorType.put("-19008", "此API版本已被禁用");
        resultType.put("-19011", "软件SID参数无效");
        errorType.put("-19011", "软件SID参数无效");
        resultType.put("-19012", "软件ID不存在");
        errorType.put("-19012", "软件ID不存在");
        resultType.put("-19013", "软件未启用");
        errorType.put("-19013", "软件未启用");
        resultType.put("-19014", "软件被禁用");
        errorType.put("-19014", "软件被禁用");
        resultType.put("-19015", "软件状态不正常");
        errorType.put("-19015", "软件状态不正常");
        resultType.put("-19020", "服务器无法使用TEAKEY解密");
        errorType.put("-19020", "服务器无法使用TEAKEY解密");
    }

//    public static String isReturnOK(String[] results) {
//        errorType.get(results[1])
//    }

    public static void main(String[] args) throws Exception {
        //________________初始化接口类需要的参数，或者直接写到UUAPI。java文件里面________________

        boolean status = UUAPI.checkAPI();    //校验API，必须调用一次，校验失败，打码不成功

        if (!status) {
            System.out.print("API文件校验失败，无法使用打码服务");
            return;
        }

        //________________初始化参数结束，上面的操作只需要设置一次________________


        String picPath = "img\\v.png";    //测试图片的位置

        //识别开始
        String result[] = UUAPI.easyDecaptcha(picPath, 3005);//picPath是图片路径,1004是codeType,http://www.uuwise.com/price.html

        System.out.println("this img codeID:" + result[0]);
        System.out.println("return recongize Result:" + result[1]);

    }

}
