window.onload = function setup() {
	setTree();
}

function setTree() {
	loadFile();	
	var location = ' A:/Github/mathnasium/test';
	var fileListArray = filelist.split(' A:/Github/mathnasium/test/');
	fileListArray.shift(); // removes first empty ele
	var lastEle = fileListArray[fileListArray.length - 1];
	fileListArray[fileListArray.length - 1] = lastEle.slice(0, -1); // remove space in last ele
	console.log(fileListArray);
	var json = arrayToJSON(fileListArray);
	var tree = document.querySelector('.tree');
	scan(json, tree, '', 147, 148, 149);
	// parseNode(fileListArray, tree, '');
}

function loadFile() {
	var url = window.location.href;
	var split = url.split('#');
	if (split.length == 2) {
		// TODO
		var viewer = document.getElementById('viewer');
		viewer.src = 'A:/Github/mathnasium/test' + split[1];
		// var file = document.getElementById(split[1]);
		// file.click();
	}
}

function arrayToJSON(data) {
	var output = {};
	var current;

	for(var a=0; a<data.length; a++) {
	  var s = data[a].split('/');
	  current = output;
	  for(var i=0; i<s.length; i++) {
	    if(s[i] != '') {
	      if(current[s[i]] == null) current[s[i]] = {};
	      current = current[s[i]];
	    }
	  }
	}
	return output;
}

function scan(obj, parent, fullPath, r,g,b)
{
    var k;
    if (obj instanceof Object) {
    	if (Object.keys(obj).length == 0) {
    		addEmpty(parent);
    	}
        for (k in obj){
            if (obj.hasOwnProperty(k)){
            	if (k != null) {
            		var fileName = k.trim(); 
            		if (isFile(fileName)) {
            			if (compatible(fileName)) {
            				addFile(fileName, parent, fullPath + '/' + fileName);
            			}
            		} else {
            			var newParent = addFolder(fileName, parent, fullPath + '/' + fileName, r,g,b);
                		scan(obj[fileName], newParent, fullPath + '/' + fileName, r*1.05,g*1.05,b*1.05);
            		}
            	}
            }
        }
    }

};

function addFile(fileName, parent, fullPath){
	// add file ele
	console.log(fullPath);
	var cell = document.createElementNS('http://www.w3.org/1999/xhtml','li');
	cell.classList.add('file');
	cell.id = fullPath; // important for target
	var file = document.createElementNS('http://www.w3.org/1999/xhtml','a');
	file.href = '#' + fullPath; // important for target
	file.innerHTML = fileName;
	parent.appendChild(cell);
	cell.appendChild(file);
	openFile(file, fullPath);
}

function addFolder(fileName, parent, fullPath, r,g,b) {
	console.log(fullPath);
	var cell = document.createElementNS('http://www.w3.org/1999/xhtml','li');
	cell.style.backgroundColor = 'rgb(' + r + ',' + g + ',' + b + ')';
	var table = document.createElementNS('http://www.w3.org/1999/xhtml','ol');
	var label = document.createElementNS('http://www.w3.org/1999/xhtml','label');
	label.htmlFor = fullPath;
	label.innerHTML = fileName;
	var input = document.createElementNS('http://www.w3.org/1999/xhtml','input');
	input.type = 'checkbox';
	input.id = fullPath;
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

function addEmpty(parent) {
	var cell = document.createElementNS('http://www.w3.org/1999/xhtml','li');
	cell.classList.add('empty');
	var msg = document.createElementNS('http://www.w3.org/1999/xhtml','a');
	msg.innerHTML = 'empty';
	parent.appendChild(cell);
	cell.appendChild(msg);
}

function openFile(file, path) {
	// TODO
	var viewer = document.getElementById('viewer');
	file.onclick = function() {
		viewer.src = 'A:/Github/mathnasium/test' + path;
	}
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