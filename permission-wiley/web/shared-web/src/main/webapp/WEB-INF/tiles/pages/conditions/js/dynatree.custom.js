	<script type="text/javascript">
//<![CDATA[

	/*
	 * returns true if parent is not null and is not the ROOT element (key = '_1')
	 * can be changed to use the "top" attribute
	 */ 	
	function fnIsTop (node) {
		if (node.parent == null) return true;
		// the root 
		if (node.parent.data.key == '_1')
			return true;
		else
			return false;
	}
	
	/*
	 * post data to server (to save in the session) and refreshes the message
	 * it goes recursively until finds a node that has a message attached to it.
	 * Only then it posts the data and refreshes the message, otherwise no reason to do it
	 */ 
	function fnClickNode (node) {
		var key = node.data.key;
		if (!fnIsTop (node)) {
			fnClickNode (node.parent);
		} else {
			// post data to the server
			fnPostNode (key);
		}
	}
	
	/*
	 * post data to server
	 * gets all the children and children of the children etc and sends the data to the server 
	 * to refresh the session
	 * Also the action will return the new message
	 */ 
	function fnPostNode (key) {
		// get node by key
		var node = $("#conditionTree").dynatree("getTree").selectKey(key);
		var arr = new Array();
		fnSerializeNodes (arr, node, false);
		// add the parent node
		arr.push ({name: "parent", value: key});
		$.ajax({
	    	async: true,
	    	type: 'post',
	    	url: '${postNodeUrl}',
	    	data: arr,
	    	success: function (response) {
	    		// lnagy - the following logic is to deal with the minimum print run requirement 
	    		// (not the most elegant solution, but otherwise can be very complicated)
	    		// it is the case just for common work minimum conditions (the other posts will not return Error)
	    		if (response.indexOf("Error") != -1 && node.data.key == "print_run") {
	    			var minimum = response.substr(6);
	    			alert ("WARNING! The legal department has set the minimum print run to " + minimum + " for this product. The minimum has been changed accordingly.");
	    			var printrunbox = $("#conditionTree").dynatree("getTree").selectKey("print_run_limit_box");
	    			$("#" + printrunbox.data.key).val(minimum);
	    			printrunbox.data.value=minimum;
	    			printrunbox.data.rollupValue = minimum;
	    			printrunbox.render(false,false);
	    		} else {
					node.data.rollupValue = response;
				}
				node.parent.render(false, false);
	    	}
		});
	}
	
	function fnSerializeNodes (arr, node, justLeaves) {
		var obj = {name: node.data.key, value: node.data.value};
		if (!justLeaves || (justLeaves && !node.hasChildren())) {		
			arr[arr.length] = obj;
		}
		if (node.hasChildren()) {
			var children = node.getChildren();
			for (var i=0; i < children.length; i++) {
				var child = children[i];
				fnSerializeNodes (arr, child, justLeaves);
			}
		}	
	}
	
	/*
	 * click radio box event - will deselect all siblings and clear the data for the siblings 
	 * and children of the siblings
	 */		
	function fnClickRadio (key) {
		var node = $("#conditionTree").dynatree("getTree").selectKey(key);
		if (node == null) return;
		// save the data so next time we open is saved
		if ($.exists("#" + key)) {
			node.data.value = $("#" + key).is(':checked') ? 'true' : '';
		}	
		// if we need to handle the mutual exclusive radio buttons at the parent level
		if (!fnIsTop(node)) {
			fnUnclickSiblinRadios(node.parent, key);
			node.parent.render(false, false);
		}
		
		fnClickNode (node);
	}
	
	function fnUnclickSiblinRadios (node, key) {	
		if (node == null) return;
		// if itself, skip
		if (node.data.key == key)
			return;
		// clear the value
		node.data.value = '';
		if (node.hasChildren ()) {
			var children = node.getChildren();
			for (var i=0; i < children.length; i++) {
				var child = children[i];
				fnUnclickSiblinRadios (child, key);
			}
		}	
	}
	
	/*
	 * saves the new value in the DynaTreeNode, so the fnDisplayNode 
	 * will have the data to display when needed
	 */		
	function fnOnBlur (key) {
		// save the data so next time we open is saved
		var node = $("#conditionTree").dynatree("getTree").selectKey(key);
		node.data.value = $("#" + node.data.key).val ();
		
		// smarkoff: Added validation for length of text - db field is 2000 but say 1980 here
		// because the rollupValue is a bit longer than value for the Notes field ("Notes=...")
		var dataTypeCode = node.data.dataTypeCode;
		//alert("dataTypeCode = " + dataTypeCode + ", length = " + node.data.value.length);
		if ((dataTypeCode == 'text' || dataTypeCode == 'textarea') && node.data.value.length > 1980) {
			node.data.value = node.data.value.substring(0, 1980);
			$("#" + node.data.key).val(node.data.value);
			alert("Maximum of 1980 characters allowed - data was truncated.");
		}
		
		fnSelectTopLevelRadio (node);
		// fnClickNode (node);
	}
	
	function fnClickCheckbox (key) {
		var node = $("#conditionTree").dynatree("getTree").selectKey(key);
		if (node == null) return;
	
		// save the data so next time we open is saved
		if ($.exists("#" + key)) {
			node.data.value = $("#" + key).is(':checked') ? 'true' : '';
		}
		debug($("#" + key).is(':checked'));
		///////////////////////////////////////////////////////////////////////
		// TODO - we might want to do it recursively to select children of the children 
		// we have to check specs
		if (node.hasChildren()) {
			fnSetChildren(node, key);
			node.data.addClass = "";
			node.render(false, false);
		}
		
		if (node.parent != null) {
			fnAllSiblingsEqual(node);
		}
		
		fnClickNode (node);
	}	
	
	/*
	 * recursively sets all the child check boxes to checked
	 */
	function fnSetChildren (node, key) {
		var children = node.getChildren();
		for (var i=0; i < children.length; i++) {
			var child = children[i];
			child.data.value = $("#" + key).is(':checked') ? 'true' : '';
			$("#" + child.data.key).prop("checked", $("#" + key).is(':checked'));
			child.data.addClass = "";
			child.render(false, false);
			debug("setChildren: " +  child.data.key + " " + $("#" + child.data.key).is(':checked'));
			// recurse through grand children if necessary
			if(child.hasChildren()) {	
				fnSetChildren(child, key);
			}
		}
	}

	function fnInitialHighlights(node) {
		if (null == node) return;
		if (node.hasChildren()) {
			var children = node.getChildren();
        	for (var x=0; x < children.length; x++) {
        		var child = children[x];
        		if(null != child.parent && child.data.dataTypeCode == 'checkbox') fnAllSiblingsEqual(child);
        		if(child.hasChildren()) {
        			var grandChildren = child.getChildren();
        			fnInitialHighlights(grandChildren[0]);
        		} 
        	}
		} else {
			if (node.data.dataTypeCode == 'checkbox' && null != node.parent ) fnAllSiblingsEqual(node);
		}
	}

	function fnValidateTree (topNode) {
		var children = topNode.getChildren();
    	for (var x=0; x < children.length; x++) {
    		var child = children[x];
    		debug ("fnValidate(): check " + child.data.key);
    		if (!child.data.canSee) {
    			debug ("fnValidate() : skip because is hidden: " + child.data.key);
    			continue;
    		}
    		// if not required, no nothing
    		if (child.data.key == 'other') {
    			debug ("fnValidate() : skip non required fields: other");
    			continue;
    		}
    		// if required, serialize all leaves, and see if any has values (either is checked or value is not null)
    		var arr = new Array();
    		fnSerializeNodes (arr, child, true);
    		var isValid = false;
    		for (var j=0; j < arr.length; j++) {
				var node = $("#conditionTree").dynatree("getTree").selectKey(arr[j].name);
				var dataTypeCode = node.data.dataTypeCode;
				if (dataTypeCode == 'text' || dataTypeCode == 'textarea') {
	    			debug ("		fnValidate(): " + child.data.key + " -> " + arr[j].name + " " + $("#" + node.data.key).val());

	    			var textVal = $("#" + node.data.key).val ();
	    			if (typeof textVal === "undefined" || textVal == '') {
	    				debug ("			text field " + node.data.key + " : undefined - not valid ");
	    			} else {
	    				isValid = true;
	    				break;
	    			}
	    		} else {
	    			debug ("		fnValidate(): " + child.data.key + " -> " + arr[j].name + " " + $("#" + arr[j].name).is(':checked'));
	    			if ($("#" + arr[j].name).is(':checked')) {
	    				isValid = true;
	    				break;
	    			}
	    		}
	    	} 
    		if (!isValid) {
    			alert (child.data.title); 
    			return false;
    		}
    	}
    	return true;
	}


	function fnAllSiblingsEqual(node) {
		if (null == node) return;
		if (null == node.parent)  return true;
		var check = false;
		var lastChild = "";
		var children = node.parent.getChildren();
		for (var i=0; i < children.length; i++) {
			var child = children[i];
			lastChild = child;
			if (child.data.dataTypeCode == 'checkbox' && node.data.dataTypeCode == 'checkbox') {
				if (jQuery.trim(child.data.value) !=  jQuery.trim(node.data.value)) {
					// if one of the child nodes is different then highlight parent and un-check it
					    if ( !fnIsTop(node.parent) ) {
					    	node.parent.data.addClass = "custom1";
					    } else {
					    	node.parent.data.addClass = "";
					    }
						node.parent.data.value = " ";
						node.parent.render(false, false);
		
					if (null != node.parent) {
						fnAllSiblingsEqual(node.parent);
					}
					return;
				}
			}
		}
		
		// if all are the same but they are all unchecked then highlight and
		// un-check parent otherwise do not highlight and check parent
		var parent = node.parent;
		
		if (lastChild.data.value == "true") {
			parent.data.addClass = "";	
			parent.data.value =  'true';
		// uncheck parent only if checkbox
		} else {
			parent.data.addClass = "";
			parent.data.value =  " ";
		}
		
		fnSelectTopLevelRadio (node);
		
		parent.render(false, false);	
		fnAllSiblingsEqual(parent);
	}

	function fnSelectTopLevelRadio (node) {
		var topParent = fnGetTopParent (node);
		// lnagy - if parent is radio, and is first level after top (top level will be the description/condition type), 
		// we check the radio and un-check any other radios 
		// for the new UI
		if (topParent.data.dataTypeCode == 'radio') {
			$("#" + topParent.data.key).prop("checked", true);
			fnClickRadio (topParent.data.key);
		}
	}
	
	function fnGetTopParent (node) {
		// top level will be the description/condition type, so we are looking for the first level after top
		if (fnIsTop(node.parent)) {
			return node;
		} else {
			return fnGetTopParent (node.parent);
		}
	}

	/*
	 * creates the HTML to be displayed based on the node type
	 */
	function fnDisplayNode (node) {
		// ignore the status node
		if (node.data.key == '_statusNode')
			return "Loading";
	
		var data = node.data;
		var nodeSpan = "<span class='" + node.data.addClass + "'>";
		
		// display edits only if canEdit		
		if (typeof data.dataTypeCode != 'undefined' && data.canEdit) {
			if (data.dataTypeCode == 'text') {
				nodeSpan += data.title + " <input type='text' id='" + data.key + "' onBlur='fnOnBlur(\"" + data.key + "\")' name='" + data.key + "' value='" + data.value + "'/></span>";
			}  else  if (data.dataTypeCode == 'textarea') {
				nodeSpan += data.title + " <textarea rows='5' cols='100' id='" + data.key + "' onBlur='fnOnBlur(\"" + data.key + "\")' name='" + data.key + "'>" + data.value + "</textarea></span>";
			}  else if (data.dataTypeCode == 'radio') {
				nodeSpan += "<input type='radio' onclick='fnClickRadio (\"" + data.key + "\")' id='" + data.key + "' name='" + data.key + "' value='true' " + $.checked('true', data.value) + " /> " + data.title + "</span>";
			} else if (node.data.dataTypeCode == 'checkbox') {
				nodeSpan += "<input type='checkbox' onclick='fnClickCheckbox (\"" + data.key + "\")' id='" + data.key + "' name='" + data.key + "' value='true' " + $.checked('true', data.value) + " /> " + data.title + "</span>";
			} else if (node.data.dataTypeCode == '') {
				nodeSpan += data.title + "</span>";
			}
		} else {
			nodeSpan += data.title + "</span>";
		}
		// if we want the nodes that show the message to be DB configurable, we can pass that info thru JSON
		// for now the ones that have no parent will have the message
		// lnagy - hide rollup value
		//if (fnIsTop (node)) {
		//	var rollup = fnCalculateRollup (data.rollupValue);
		//	nodeSpan += "<span class='right' style='text-align:left;width:400px;' id='msg_" + data.key + "' title='" + data.rollupValue + "'>" + rollup + "</span>";
		//}
		return nodeSpan;
	}

	function fnCalculateRollup (rollup) {
		var intRegex = /^\d+$/;
    	if (null != rollup && intRegex.test(rollup)) {
    		rollup = $.formatNumber(rollup, {format:"#,###", locale:"us"});
        	return rollup;
    	} else if (null != rollup && rollup.length > 70) {
			// just display the 100 for now
			//var ptr1 = rollup.indexOf(',',70);
			//if (ptr1 <= 0)
			//	ptr1 = 100;
			var s1 = rollup.substring(0,70) + " ....";
			return s1;
		} else {
			return rollup;
		}
	}
//]]>		
	</script>
