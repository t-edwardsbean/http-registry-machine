package registry.machine;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by edwardsbean on 2015/2/10 0010.
 */
public class Task {
    private String email;
    private String password;
    private List<String> args = new ArrayList<>();

    public Task(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public List<String> getArgs() {
        return args;
    }

    public Task setArgs(List<String> args) {
        this.args = args;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Task task = (Task) o;

        if (!email.equals(task.email)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return email.hashCode();
    }

    @Override
    public String toString() {
        return "Task{" +
                "email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", args=" + args +
                '}';
    }
}
