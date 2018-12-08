var access_token = null;

window.onload = function setup() {
	var loginBtn = document.getElementById('login');
	loginBtn.onclick = function() {
		window.open('https://www.dropbox.com/oauth2/authorize?client_id=g5si6x4lepay5cg&response_type=code&redirect_uri=http://localhost:8000/auth.html', '_blank', 'width=500,height=450');
	}
}

function auth(code) {
	// console.log(code);
	if (code != null) {
	    var xhr = new XMLHttpRequest();
	    xhr.open('POST', 'https://api.dropboxapi.com/oauth2/token?code='+code+'&grant_type=authorization_code&redirect_uri=http://localhost:8000/auth.html&client_id=g5si6x4lepay5cg&client_secret=<SECRET HERE>');
	    xhr.onload = function() {
	        console.log(xhr.response);  
	        if (xhr.status == 200) {
                var response = JSON.parse(xhr.response);
	        	var loginBtn = document.getElementById('login');
	        	loginBtn.style.display = 'none';
	        	access_token = response.access_token;
	        	authSuccess();
	        }
	    }
	    xhr.send();
	}
}

function authSuccess() {
    var json = {
	    "path": "",
	    "recursive": true,
	    "include_media_info": false,
	    "include_deleted": false,
	    "include_has_explicit_shared_members": false,
	    "include_mounted_folders": true
	}
    var xhr = new XMLHttpRequest();
    xhr.open('POST', 'https://api.dropboxapi.com/2/files/list_folder');
    xhr.setRequestHeader('Authorization', 'Bearer '+access_token);
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.onload = function() {
        console.log(xhr.response);
        if (xhr.status == 200) {
            var response = JSON.parse(xhr.response);
            setTree(response.entries);
            setEmpty();
        }
    }
    xhr.send(JSON.stringify(json));
}

// OLD CODE BELOW

function setTree(entries) {
	var tree = document.querySelector('.tree');
	scan(entries, tree, 147, 148, 149);
	// parseNode(fileListArray, tree, '');
}

// function loadFile() {
// 	var url = window.location.href;
// 	var split = url.split('#');
// 	if (split.length == 2) {
// 		// TODO
// 		var viewer = document.getElementById('viewer');
// 		viewer.src = 'A:/Github/mathnasium/test' + split[1];
// 		// var file = document.getElementById(split[1]);
// 		// file.click();
// 	}
// }

function scan(entries, parent, r,g,b)
{
	for (var i = 0; i < entries.length; i++) {
		console.log(entries[i]);
		var name = entries[i]['name'];
		var path = entries[i]['path_display'];
		var parent = path.replace('/' + name,'');
		if (parent === '')
			parent = 'tree';
		var x = parent.split('/').length - 1;
		console.log(x);
		var parentEle = document.getElementById(parent);
		if (entries[i]['.tag'] == 'folder') {
			addFolder(name, parentEle, path, r + r*x*(.05), g + g*x*(.05), b + b*x*(.05));
		}
		else if (entries[i]['.tag'] == 'file') {
			var newParent = addFile(name, parentEle, path);
		}

	}
};

function addFile(fileName, parent, path){
	// add file ele
	console.log(path);
	var cell = document.createElementNS('http://www.w3.org/1999/xhtml','li');
	cell.classList.add('file');
	cell.id = path; // important for target
	var file = document.createElementNS('http://www.w3.org/1999/xhtml','a');
	file.innerHTML = fileName;
	file.href = '#' + path; // makes it clickable
	parent.appendChild(cell);
	cell.appendChild(file);
	addFileLink(file, path);
}

function addFileLink(file, path) {
	var viewer = document.getElementById('viewer');
	file.onclick = function() {
	    var json = {"path": path, "short_url": false};
	    var xhr = new XMLHttpRequest();
	    // xhr.open('POST', 'https://api.dropboxapi.com/2/files/get_temporary_link');
	    xhr.open('POST', 'https://api.dropboxapi.com/2/sharing/create_shared_link');
	    xhr.setRequestHeader('Authorization', 'Bearer '+access_token);
	    xhr.setRequestHeader('Content-Type', 'application/json');
	    xhr.onload = function() {
	        console.log(xhr.response);
	        if (xhr.status == 200) {
	            var response = JSON.parse(xhr.response);
	            var url = response.url.replace('?dl=0','');
				viewer.src = 'https://docs.google.com/gview?url=' + url + '?dl=1&embedded=true';
	        }
	    }
	    xhr.send(JSON.stringify(json));
	}
}

function addFolder(fileName, parent, path, r,g,b) {
	console.log(path);
	var cell = document.createElementNS('http://www.w3.org/1999/xhtml','li');
	cell.style.backgroundColor = 'rgb(' + r + ',' + g + ',' + b + ')';
	var table = document.createElementNS('http://www.w3.org/1999/xhtml','ol');
	table.id = path;
	var label = document.createElementNS('http://www.w3.org/1999/xhtml','label');
	label.htmlFor = path + ' btn';
	label.innerHTML = fileName;
	var input = document.createElementNS('http://www.w3.org/1999/xhtml','input');
	input.type = 'checkbox';
	input.id = path + 'btn';
	parent.appendChild(cell);
	cell.appendChild(label);
	cell.appendChild(input);
	cell.appendChild(table);
	input.onclick = function() {
		if (input.checked) {
			label.style.backgroundColor = '#a33038';
		}
		else {
			label.style.backgroundColor = '#f42941';
		}
	}
	return table;
}

function setEmpty() {
	var tables = document.querySelectorAll('ol');
	for (var i = 0; i < tables.length; i++) {
		if (tables[i].childNodes.length == 0)
			addEmpty(tables[i]);
	}
}

function addEmpty(parent) {
	var cell = document.createElementNS('http://www.w3.org/1999/xhtml','li');
	cell.classList.add('empty');
	var msg = document.createElementNS('http://www.w3.org/1999/xhtml','a');
	msg.innerHTML = 'empty';
	parent.appendChild(cell);
	cell.appendChild(msg);
}

function compatible(fileName) {
	// TODO
	fileName = fileName.toLowerCase();
	return isIMG(fileName) || isTXT(fileName) || isODT(fileName) ;
}

function isIMG(fileName) {
	var isIMG = /\.(jpe?g|png|gif|bmp)$/i;
	return fileName.match(isIMG);
}

function isTXT(fileName) {
	var isTXT = /\.(pdf|html|css|txt|js|pom)$/i;
	return fileName.match(isTXT);
}

function isODT(fileName) {
	var isODT = /\.(f?odt|f?odp|f?ods|f?odg)$/i;
	return fileName.match(isODT);
}

function isFile(fileName) {
	fileName = fileName.toLowerCase();
	var isFile = /\.[a-z]{1,5}$/i;
	console.log(fileName);
	return fileName.match(isFile);
}