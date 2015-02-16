$.ajax({
    url: "aima",
    type: 'get',
    success: function (msg) {
        $('#aimaUser').html(msg);
    }
});

function start() {
    var threadNum = $('#modal-threadNum').val();
    var waitTime = $('#modal-waitTime').val();
    var data = new Object();
    if (threadNum !== "") {
        data["threadNum"] = threadNum;
    }
    if (waitTime !== "") {
        data["waitTime"] = waitTime;
    }
    $.ajax({
        url: "start",
        type: 'get',
        data: data,
        error: function () {
            alert("运行失败")
        },
        success: function (msg) {
            if (msg == "ok") {
                $('#stopBtn').removeClass("disabled");
                $('#startBtn').addClass("disabled");
                //alert("开始运行");
                //跳转到index
                //location.href = "index";
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

$('#saveBtn').onclick = save;
function save() {
    var threadNum = $('#modal-threadNum').val();
    var waitTime = $('#modal-waitTime').val();
    var proxyPath = $('#proxy-select').val();
    if (threadNum !== "") {
        $('#threadNum').html(threadNum);
    }
    if (waitTime !== "") {
        $('#waitTime').html(waitTime);
    }
    if (proxyPath !== "") {
        $('#proxyPath').html(proxyPath);
    }
}
