package models;

/**
 * Created by shicongyu01_91 on 2015/3/10.
 */
public class Code {
    String code;
    String codeId;

    public Code(String code, String codeId) {
        this.code = code;
        this.codeId = codeId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCodeId() {
        return codeId;
    }

    public void setCodeId(String codeId) {
        this.codeId = codeId;
    }

    @Override
    public String toString() {
        return "Code{" +
                "code='" + code + '\'' +
                ", codeId='" + codeId + '\'' +
                '}';
    }
}
