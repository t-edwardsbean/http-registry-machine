package controllers;

import akka.actor.ActorRef;
import akka.actor.Props;
import models.User;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.RandomStringUtils;
import play.Logger;
import play.data.DynamicForm;
import play.data.Form;
import play.libs.F;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.WebSocket;
import registry.machine.*;

import java.io.File;
import java.io.IOException;


public class Application extends Controller {
    public static Logger.ALogger log = Logger.of("application");

    public static Result index() {
        return redirect("index.html");
    }

    public static Result start(int threadNum, int waitTime) {
        log.debug("controller:启动注册机,threadNum:{},waitTime:{}", threadNum, waitTime);
        RegistryMachineContext.isFilter.set(false);
        if (RegistryMachineContext.sleepTime < waitTime) {
            RegistryMachineContext.sleepTime = waitTime;
        }
        if (threadNum > 0 && threadNum < 200) {
            RegistryMachineContext.registryMachine.thread(threadNum);
        } else if (threadNum >= 200) {
            RegistryMachineContext.registryMachine.thread(200);
        } else {
            RegistryMachineContext.registryMachine.thread(15);
        }
        try {
            RegistryMachineContext.start();
        } catch (NullPointerException e) {
            return internalServerError("请刷新浏览器");
        } catch (AIMAException e) {
            return internalServerError("请输入正确的爱玛平台账号密码:" + e.getMessage());
        }
        return ok("ok");
    }

    public static Result filter(int threadNum) {
        log.debug("controller:启动注册机过滤邮箱,threadNum:{}", threadNum);
        RegistryMachineContext.isFilter.set(true);
        if (threadNum > 0 && threadNum <= 200) {
            RegistryMachineContext.registryMachine.thread(threadNum);
        } else if (threadNum > 200) {
            RegistryMachineContext.registryMachine.thread(200);
        } else {
            RegistryMachineContext.registryMachine.thread(30);
        }
        try {
            RegistryMachineContext.start();
        } catch (NullPointerException e) {
            Logger.error("启动失败：",e);
            return internalServerError("请刷新浏览器");
        }
        return ok("ok");
    }


    public static Result download() {
        response().setContentType("application/x-download");
        response().setHeader("Content-disposition", "attachment; filename=result.txt");
        return ok(RegistryMachineContext.result.toString());
    }

    public static Result downloadFilter() {
        response().setContentType("application/x-download");
        response().setHeader("Content-disposition", "attachment; filename=filter.txt");
        StringBuilder result = new StringBuilder();
        for (Task task: RegistryMachineContext.okEmailQueue) {
            result.append(task.getEmail()).append("\r\n");
        }
        return ok(result.toString());
    }

    public static Result status() {
        log.debug("查询注册机状态：{}", RegistryMachineContext.isRunning.get());
        return ok(RegistryMachineContext.isRunning.get() + "");
    }

    public static Result proxyNum() {
        return ok(RegistryMachineContext.proxyQueue.size() + "");
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
    
    public static Result changeUser() {
        DynamicForm requestData = Form.form().bindFromRequest();
        String username = requestData.get("username");
        String password = requestData.get("password");
        log.debug("账号：{},密码：{}", username,password);
        UUAPI.USERNAME = username;
        UUAPI.PASSWORD = password;
        return ok("ok");
    }

    public static Result stop() {
        log.debug("controller:停止注册机");
        try {
            RegistryMachineContext.stop();
        } catch (Exception e) {
        }
        return ok("ok");
    }

    public static Result uuwise() {
        System.out.println("cao");
        log.debug("获取uu账户名称:{}", UUAPI.USERNAME);
        return ok(UUAPI.USERNAME);
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
            if ("email".equals(name)) {
                //清理
                RegistryMachineContext.registryMachine.cleanTask();
                RegistryMachineContext.result = new StringBuilder();
                RegistryMachineContext.okEmailQueue.clear();
            } else if ("proxy".equals(name)) {
                RegistryMachineContext.proxyQueue.clear();
            }
            while (iterator.hasNext()) {
                String line = iterator.nextLine();
                log.debug("{}内容：{}", name, line);
                if ("email".equals(name)) {
                    String emailPwd = RandomStringUtils.randomAlphanumeric(8);
                    Task task = new Task(line, emailPwd);
                    RegistryMachineContext.registryMachine.addTask(task);
                } else if ("proxy".equals(name)) {
                    String[] splits = line.split(":");
                    if (splits.length != 2) {
                        return badRequest("该行文件内容格式不对：" + line);
                    } else {
                        RegistryMachineContext.addProxy(line);
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
