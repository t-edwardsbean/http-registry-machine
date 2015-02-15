var form = document.getElementsByClassName('proxy-up-form');
var fileSelect = document.getElementById('proxy-select');
var uploadButton = document.getElementById('proxyBtn');

form.onsubmit = upload(event);

function upload(event) {
    event.preventDefault();
    // Update button text.
    uploadButton.innerHTML = 'Uploading...';

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
    xhr.open('POST', 'upload', true);
    // Set up a handler for when the request finishes.
    xhr.onload = function () {
        if (xhr.status === 200) {
            // File(s) uploaded.
            uploadButton.innerHTML = 'Upload';
            alert('upload success:' + xhr.responseText)
        } else {
            alert('An error occurred!:' + + xhr.responseText);
        }
    };
    // Send the Data.
    xhr.send(formData);
}

