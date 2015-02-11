package models;

/**
 * Created by edwardsbean on 15-2-11.
 */
public class User {
    private String name;
    private String message = "hello";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
