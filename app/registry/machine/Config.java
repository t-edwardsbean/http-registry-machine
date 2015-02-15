package registry.machine;

/**
 * Created by edwardsbean on 2015/2/9 0009.
 */
public class Config {
    private String uid;
    private String pwd;
    private String pid;
    private String phantomjsPath;
    public Config(String uid, String pwd, String pid) {
        this.uid = uid;
        this.pwd = pwd;
        this.pid = pid;
    }

    public String getPhantomjsPath() {
        return phantomjsPath;
    }

    public void setPhantomjsPath(String phantomjsPath) {
        this.phantomjsPath = phantomjsPath;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    @Override
    public String toString() {
        return "Config{" +
                "uid='" + uid + '\'' +
                ", pwd='" + pwd + '\'' +
                ", pid='" + pid + '\'' +
                ", phantomjsPath='" + phantomjsPath + '\'' +
                '}';
    }
}
