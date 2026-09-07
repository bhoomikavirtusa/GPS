	function addRow() {
		var columns = document.getElementById("availableValues");
		var val = columns.options[columns.selectedIndex].value;
		var text = columns.options[columns.selectedIndex].text;
		var title = columns.options[columns.selectedIndex].title;
		fnAddRow (val, text, title);
	}

	function fnAddRow(val, text, title) {
		if (duplicateRow(val)) {
			// alert ("Value already selected. Please select another value.");
			return false;
		}
	    var table = document.getElementById("selectedValues");
		
	    var rowCount = table.rows.length;  
	    var row = table.insertRow(rowCount);  
	    row.className = (rowCount % 2  == 0) ? "even" : "odd";
	    var cell1 = row.insertCell(0);
	    var element1 = document.createElement("input");
	    element1.type = "hidden";
	    element1.id = "values" + rowCount;
	    element1.name = "values";
	    element1.value = val;
	    cell1.appendChild(element1);
	    element1.checked = true;

	    var element2 = document.createElement("img");
	    // deleteImageUrl must be defined outside this file, before calling - see importAssets.jspx
	    element2.src = deleteImageUrl;  
	    
	    element2.onclick = function () { deleteRow (element1.value); }
	    // the relative path might not be always same relative to jsp position
	    // we try 2 version
	    // will generate stack overflow in IE and will prevent the page to work in Firefox.
	    // element2.onerror = function () { element2.src = "../images/buttons/delete-icon.png"; }
	    cell1.appendChild(element2);
	    
	    var element3 = document.createElement("input");
	    element3.type = "hidden";
	    element3.name = "_values";
	    element3.value = "on";
	    cell1.appendChild(element3);

	    var cell2 = row.insertCell(1);
	    cell2.innerHTML = text;
		
		if (title != null) {
		    var cell3 = row.insertCell(2);
		    cell3.innerHTML = title;
		}
	}  
	
	function duplicateRow(val) {
	    try {
		    var table = document.getElementById("selectedValues");
		    
		    var selectedValue = val;
		    
		    var rowCount = table.rows.length;
			
		    for(var i=0; i<rowCount; i++) {  
		        var row = table.rows[i];
		        var chkbox = row.cells[0].childNodes[0];  
		        if (null != chkbox && selectedValue == chkbox.value) {  
		        	return true;
		        }
		    }
	    } catch(e) {  
	        alert(e);  
	    }  
	    return false;
	}

	function deleteRow(value) {  
	    try {  
		    var table = document.getElementById("selectedValues");  	
		    
		    var rowCount = table.rows.length;
		    for (var i = 0; i < rowCount; i++) {
		        var row = table.rows[i];
		        var chkbox = row.cells[0].childNodes[0];
		        if (null != chkbox && value == chkbox.value) {
		            table.deleteRow(i);
		            rowCount--;
		            i--;
		        } else {
		        	if (i != 0)
		        		row.className = (i % 2  == 0) ? "even" : "odd";
				}
		    }  
	    } catch(e) {
	        alert(e);
	    }
	}
	
	function deleteAllRows() {  
	    try {  
		    var table = document.getElementById("selectedValues");  	
		    
		    var rowCount = table.rows.length;
		    // don't delete the table header
		    for (var i = 1; i < rowCount; i++) {
		    	table.deleteRow(-1); // delete last row
		    }  
	    } catch(e) {
	        alert(e);
	    }
	}
	
	function selectColumn(value) {
		var columns = document.getElementById("availableValues");
		for(var i = 0; i < columns.options.length; i++) {
			if(columns.options[i].value == value) {
				columns.selectedIndex = i;
				addRow();
			}
		}
	}
