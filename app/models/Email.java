package models;

/**
 * Created by edwardsbean on 15-2-15.
 */
public class Email {
    private String email;
    private String pwd;

    public Email(String email, String pwd) {
        this.email = email;
        this.pwd = pwd;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    @Override
    public String toString() {
        return email + " " + pwd;
    }
}
