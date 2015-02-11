package controllers;

import models.User;
import play.*;
import play.data.Form;
import play.libs.Json;
import play.mvc.*;


public class Application extends Controller {
    public static Logger.ALogger log = Logger.of("application");

    public static Result index() {
        return ok("ok");
    }

    //GET，绑定参数并返回JSON
    public static Result hello(String name) {
        User user = new User();
        user.setName(name);
        return ok(Json.toJson(user));
    }
    
    //POST，装箱
    public static Result post() {
        User user = Form.form(User.class).bindFromRequest().get();
        return ok(Json.toJson(user));
    }

}
