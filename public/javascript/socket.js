$(function() {
    var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
    var chatSocket = new WS("ws://localhost:9000/socket");
    var receiveEvent = function (event) {
        console.log(event.data);
        var data = JSON.parse(event.data);
        if(data.type === "") {
        }else{
            $("#logSection").append(data.value + "<br>")
        }
    };
    chatSocket.onmessage = receiveEvent
})