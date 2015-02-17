package controllers;

import akka.actor.ActorRef;
import akka.actor.Props;
import models.User;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import play.*;
import play.data.DynamicForm;
import play.data.Form;
import play.libs.F;
import play.libs.Json;
import play.mvc.*;
import registry.machine.RegistryMachineContext;
import registry.machine.Task;

import java.io.File;
import java.io.IOException;


public class Application extends Controller {
    public static Logger.ALogger log = Logger.of("application");

    public static Result index() {
        return redirect("index.html");
    }

    public static Result start(int threadNum, int waitTime) {
        log.debug("controller:启动注册机,threadNum:{},waitTime:{}", threadNum, waitTime);
        if (!RegistryMachineContext.isRunning) {
            if (RegistryMachineContext.sleepTime < waitTime) {
                RegistryMachineContext.sleepTime = waitTime;
            }
            RegistryMachineContext.registryMachine.thread(threadNum);
            try {
                RegistryMachineContext.start();
            } catch (NullPointerException e) {
                return badRequest("请重启浏览器");
            }
            return ok("ok");
        } else {
            return badRequest("false");
        }
    }

    public static Result status() {
        log.debug("查询注册机状态：{}", RegistryMachineContext.isRunning);
        return ok(RegistryMachineContext.isRunning + "");
    }    
    
    
    public static Result getProxyFile() {
        log.debug("代理文件名：{}", RegistryMachineContext.proxyFileName);
        return ok(RegistryMachineContext.proxyFileName);
    }

    public static Result setProxyFile() {
        DynamicForm requestData = Form.form().bindFromRequest();
        String proxyPath = requestData.get("path");
        log.debug("代理文件名：{}", proxyPath);
        RegistryMachineContext.proxyFileName = proxyPath;
        return ok("ok");
    }
    
    public static Result stop() {
        log.debug("controller:停止注册机");
        RegistryMachineContext.stop();
        return ok("ok");
    }
    
    public static Result aima() {
        log.debug("获取aima账户名称:{}",RegistryMachineContext.AIMAName);
        return ok(RegistryMachineContext.AIMAName);
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
            while (iterator.hasNext()) {
                String line = iterator.nextLine();
                log.debug("{}内容：{}", name, line);
                if ("email".equals(name)) {
                    String[] splits = line.split(",");
                    if (splits.length != 2) {
                        return badRequest("该行文件内容格式不对：" + line);
                    } else {
                        String emailName = splits[0];
                        String emailPwd = splits[1];
                        Task task = new Task(emailName, emailPwd);
                        RegistryMachineContext.registryMachine.addTask(task);
                    }
                } else if ("proxy".equals(name)) {
                    String[] splits = line.split(":");
                    if (splits.length != 2) {
                        return badRequest("该行文件内容格式不对：" + line);
                    } else {
                        RegistryMachineContext.addProxy("--proxy=" + line);
                    }
                } else {
                    return badRequest("该文件到底是proxy还是email");
                }
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
