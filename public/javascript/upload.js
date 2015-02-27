var emailBtn = document.getElementById('emailBtn');
var proxyBtn = document.getElementById('proxyBtn');
emailBtn.onclick = uploadEmail;
proxyBtn.onclick = uploadProxy;

function uploadEmail(event) {
    event.preventDefault();
    event.stopPropagation();
    var fileSelect = document.getElementById('email-select');
    $('#successEmail').html("");
    $("#logSection").html("");
    $("#successNum").text(0);
    $("#userExist").text(0);
    $("#networkError").text(0);
    // The rest of the code will go here...
    // Get the selected files from the input.
    var files = fileSelect.files;

    // Create a new FormData object.
    var formData = new FormData();


    // Loop through each of the selected files.
    for (var i = 0; i < files.length; i++) {
        var file = files[i];

        // Add the file to the request.
        formData.append('file', file, file.name);
    }


    // Set up the request.
    var xhr = new XMLHttpRequest();
    // Open the connection.
    xhr.open('POST', 'upload/email', true);
    // Set up a handler for when the request finishes.
    xhr.onload = function () {
        if (xhr.status === 200) {
            // File(s) uploaded.
            alert('设置邮箱账号成功:' + xhr.responseText);
            $("#emailException").text(0);
            $("#successNum").text(0);
            $("#emailException").text(0);
            $("#logSection").text("");
        } else {
            alert('校验邮箱格式失败!:' + + xhr.responseText);
        }
    };
    // Send the Data.
    xhr.send(formData);
}

function uploadProxy(event) {
    event.preventDefault();
    var fileSelect = document.getElementById('proxy-select');

    // The rest of the code will go here...
    // Get the selected files from the input.
    var files = fileSelect.files;

    // Create a new FormData object.
    var formData = new FormData();


    // Loop through each of the selected files.
    for (var i = 0; i < files.length; i++) {
        var file = files[i];

        // Add the file to the request.
        formData.append('file', file, file.name);
    }


    // Set up the request.
    var xhr = new XMLHttpRequest();
    // Open the connection.
    xhr.open('POST', 'upload/proxy', true);
    // Set up a handler for when the request finishes.
    xhr.onload = function () {
        if (xhr.status === 200) {
            // File(s) uploaded.
            alert('设置代理成功:' + xhr.responseText)
        } else {
            alert('代理格式校验失败!:' + + xhr.responseText);
        }
    };
    // Send the Data.
    xhr.send(formData);
}