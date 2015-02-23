$.ajax({
    url: "aima",
    type: 'get',
    success: function (msg) {
        $('#aimaUser').html(msg);
    }
});

$.ajax({
    url: "proxyFile",
    type: 'get',
    success: function (msg) {
        $('#proxyPath').html(msg);
    }
});

$.ajax({
    url: "status",
    type: 'get',
    success: function (msg) {
        //注册机运行中
        if(msg === "true") {
            $('#startBtn').addClass("disabled");
            $('#stopBtn').removeClass("disabled");
        }
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
        error: function (xhr, ajaxOptions, thrownError) {
            alert(xhr.responseText);
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
            if (msg !== "ok") {
                alert("停止失败")
            }
        }
    });
}

$('#saveBtn').onclick = save;
function save() {
    stop();
    var threadNum = $('#modal-threadNum').val();
    var waitTime = $('#modal-waitTime').val();
    var proxyPath = $('#proxy-select').val();
    $.ajax({
        url: "proxyFile",
        type: 'post',
        data : {
            threadNum: threadNum,
            waitTime: waitTime,
            path: proxyPath
        }
    });
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
