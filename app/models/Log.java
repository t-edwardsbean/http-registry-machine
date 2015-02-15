package models;

/**
 * Created by edwardsbean on 2015/2/14 0014.
 */
public class Log {
    final String type;
    final Object value;

    public String getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public Log(String type, Object value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Log{" +
                "type='" + type + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
