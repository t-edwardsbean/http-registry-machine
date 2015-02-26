$(function () {
    var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
    var chatSocket = new WS("ws://localhost:9000/socket");
    var receiveEvent = function (event) {
        console.log(event.data);
        var data = JSON.parse(event.data);
        if (data.type === "log") {
            if(data.value === "注册机运行结束" || data.value === "没有邮箱文件，请上传") {
                $('#stopBtn').click();
            }
            if(data.value === "注册机运行结束") {
                $('#outBtn').click();
            }
            if(data.value.indexOf("移除无效代理") !== -1) {
                var num = parseInt($("#proxyNum").text());
                num = num - 1;
                if(num >= 0) {
                    $("#proxyNum").text(num);
                }
            }
            $("#logSection").append(data.value + "<br>")
        } else if (data.type === "email") {
            var num = parseInt($("#successNum").text());
            num = num + 1;
            $("#successNum").text(num);
            $("#successEmail").append("<tr><td>" + data.value.email + "</td><td>" + data.value.pwd + "</td></tr>");
        } else if (data.type === "emailException") {
            var num = parseInt($("#emailException").text());
            num = num + 1;
            $("#emailException").text(num);
        } else if (data.type === "networkError") {
            var num = parseInt($("#networkError").text());
            num = num + 1;
            $("#networkError").text(num);
            $("#logSection").append(data.value + "<br>")
        }
    };
    chatSocket.onmessage = receiveEvent
})