package controllers;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import models.User;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import play.*;
import play.data.Form;
import play.libs.F;
import play.libs.Json;
import play.mvc.*;
import registry.machine.RegistryMachineContext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class Application extends Controller {
    public static Logger.ALogger log = Logger.of("application");
    public static RegistryMachineContext registryMachineContext = new RegistryMachineContext();
    
    public static Result index() {
        return ok("ok");
    }

    public static Result start() {
        RegistryMachineContext.start();
        return ok("ok");
    }

    public static Result stop() {
        RegistryMachineContext.stop();
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

    public static WebSocket<String> socket() {
        return WebSocket.withActor(new F.Function<ActorRef, Props>() {
            public Props apply(ActorRef out) throws Throwable {
                return MyWebSocketActor.props(out);
            }
        });
    }

    public static Result upload(String name) {
        Http.MultipartFormData body = request().body().asMultipartFormData();
        if (body.getFiles().isEmpty()) {
            return badRequest("fail");
        } else {
            File file = body.getFile("file").getFile();
            LineIterator iterator;
            try {
                iterator = FileUtils.lineIterator(file);
            } catch (IOException e) {
                return badRequest("文件错误");
            }
            List<String> lists = new ArrayList<>();
            while (iterator.hasNext()) {
                String line = iterator.nextLine();
                log.debug("{}内容：{}", name, line);
                lists.add(line);
            }
            if ("proxy".equals(name)) {
                registryMachineContext.addProxies(lists);
            } else if ("email".equals(name)) {
                registryMachineContext.addEmails(lists);
            } else {
                return badRequest("文件类型未知");
            }
            return ok("ok");
        }
//        if (file != null) {
//            String fileName = file.getFilename();
//            String contentType = file.getContentType();
//            File saved = file.getFile();
//            System.out.println(fileName);
//            return ok("File uploaded");
//        } else {
//            flash("error", "Missing file");
//            return redirect(routes.Application.index());
//        }
    }
}
