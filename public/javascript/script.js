function start() {
    $('#stopBtn').removeClass("disabled");
    $('#startBtn').addClass("disabled");
    $.ajax({
        url: "start",
        type: 'get',
        success: function (msg) {
            if (msg == "ok") {
                //alert("开始运行");
                //跳转到index
                //location.href = "index";
            } else {
                alert("运行失败")
            }
        }
    });
}

function stop() {
    $('#startBtn').removeClass("disabled");
    $('#stopBtn').addClass("disabled");
    $.ajax({
        url: "stop",
        type: 'get',
        success: function (msg) {
            if (msg == "ok") {
                alert("停止成功");
            } else {
                alert("停止失败")
            }
        }
    });
}

function out() {
    
}