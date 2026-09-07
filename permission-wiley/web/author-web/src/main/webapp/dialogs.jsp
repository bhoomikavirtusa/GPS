<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <title>Dialog examples</title>
    <link rel="stylesheet" type="text/css" href="<c:url value="/css/shared.css" />" />
    <link rel="stylesheet" type="text/css" href="<c:url value="/css/jquery-ui-1.8.13.custom.css" />" />
    <script type="text/javascript" src="<c:url value="/js/jquery-1.8.3.min.js" />"></script>
    <script type="text/javascript" src="<c:url value="/js/jquery-ui-1.8.24.min.js" />"></script>
    <script type="text/javascript" src="<c:url value="/js/jquery.validate.min.js" />"></script>
</head>
<body>

<h3>Dialog examples</h3>

<h4>Example 1: Simple Modal dialog - opened by user clicking on something, closed by user</h4>

<script type="text/javascript">
	$(document).ready(function() {
		var $helpDialog = $("#helpDialog").dialog({autoOpen: false, modal: true});

		$('#helpOpener').click(function() {
			$helpDialog.dialog('open');
			// prevent the default action, e.g., following a link
			return false;
		});
	});
</script>

<div>
	<button id="helpOpener">Open the dialog</button>
</div>

<div id="helpDialog" title="Help" style="display: none">
	Here is some help: blah blah blah.
	<a href="http://www.ibm.com">Link</a>
</div>

<h4>Example 2: Modal dialog with content loaded by ajax call + 2 buttons (added by javascript) - opened by user clicking on something, closed by user</h4>

<script type="text/javascript">
	$(document).ready(function() {
		var $loadDialog = $("<div>").dialog({
			autoOpen: false,
			modal: true,
			open: function() {$(this).load("<c:url value="/index.jsp" />"); },
			title: "Loaded from URL",
			buttons: {
				"Delete": function() {
					$(this).dialog( "close" );
				},
				Cancel: function() {
					$(this).dialog( "close" );
				}
			}
		});

		$('#loadOpener').click(function() {
			$loadDialog.dialog('open');
			// prevent the default action, e.g., following a link
			return false;
		});
	});
</script>

<div>
	<button id="loadOpener">Open the dialog</button>
</div>

<h4>Example 3: A single-part dialog form</h4>

<script type="text/javascript">
	$(document).ready(function() {
		function updateTips(t) {
			tips
				.text(t)
				.addClass("ui-state-highlight");
			setTimeout(function() {
				tips.removeClass("ui-state-highlight", 1500);
			}, 500 );
		}
		
		function checkLength( o, n, min, max ) {
			if (o.val().length > max || o.val().length < min ) {
				o.addClass("ui-state-error" );
				updateTips("Length of " + n + " must be between " +
					min + " and " + max + "." );
				return false;
			} else {
				return true;
			}
		}

		var tips = $(".validateTips");
		var name = $("#name");
		var allFields = $([]).add(name);
		
		var $formDialog = $("#formDialog").dialog({
			autoOpen: false,
			modal: true,
			buttons: {
				"Submit": function() {
					var bValid = true;
					allFields.removeClass("ui-state-error");

					bValid = bValid && checkLength(name, "name", 3, 16);

					if (bValid) {
						/*
						$( "#users tbody" ).append( "<tr>" +
							"<td>" + name.val() + "</td>" + 
							"<td>" + email.val() + "</td>" + 
							"<td>" + password.val() + "</td>" +
						"</tr>" );
						*/
						//$(this).dialog( "close" );
						$("#theForm").submit();
					}
				},
				Cancel: function() {
					$(this).dialog("close");
				}
			}
		});

		$('#formOpener').click(function() {
			$formDialog.dialog('open');
			// prevent the default action, e.g., following a link
			return false;
		});

		$('#formCancel').click(function() {
			$formDialog.dialog('close');
			// prevent the default action, e.g., following a link
			return false;
		});
	});
</script>

<div id="formDialog" title="A Form">
	<form id="theForm">
		<p class="validateTips">All form fields are required.</p>
		
		<table>
			<tr>
				<td><label for="name">Name</label></td>
				<td><input type="text" name="name" id="name" /></td>
			</tr>
			<!--
			<input type="submit" value="Submit" />
			<input type="button" value="Cancel" id="formCancel" />
			-->
		</table>
	</form>
</div>

<div>
	<button id="formOpener">Open the dialog</button>
</div>

<h4>Example 4: A multi-part form where all data is saved after the last part.</h4>

<script type="text/javascript">
	$(document).ready(function() {
		function updateTips(t) {
			tips
				.text(t)
				.addClass("ui-state-highlight");
			setTimeout(function() {
				tips.removeClass("ui-state-highlight", 1500);
			}, 500 );
		}

		function clearTips() {
			tips.text("");
		}
		
		function checkLength( o, n, min, max ) {
			if (o.val().length > max || o.val().length < min ) {
				o.addClass("ui-state-error" );
				updateTips("Length of " + n + " must be between " +
					min + " and " + max + "." );
				return false;
			} else {
				return true;
			}
		}

		var tips = $(".validateTips");
		var box1 = $("#box1");
		var box2 = $("#box2");
		var allFields = $([]).add(box1);
		
		var $page1Dialog = $("#divPage1").dialog({
			autoOpen: false,
			modal: true,
			buttons: {
				"Next": function() {
					var bValid = true;
					allFields.removeClass("ui-state-error");

					bValid = bValid && checkLength(box1, "box1", 3, 16);

					if (bValid) {
						$("#combo_box1").val(box1.val());
						$(this).dialog("close");
						clearTips();
						$page2Dialog.dialog('open');
					}
				},
				Cancel: function() {
					$(this).dialog("close");
				}
			}
		});

		var $page2Dialog = $("#divPage2").dialog({
			autoOpen: false,
			modal: true,
			buttons: {
				"Finish": function() {
					var bValid = true;
					allFields.removeClass("ui-state-error");

					bValid = bValid && checkLength(box2, "box2", 3, 16);

					if (bValid) {
						$("#combo_box2").val(box2.val());
						$(this).dialog("close");
						$("#comboForm").submit();
					}
				},
				Cancel: function() {
					$(this).dialog("close");
				}
			}
		});

		$('#multiFormOpener').click(function() {
			$page1Dialog.dialog('open');
			// prevent the default action, e.g., following a link
			return false;
		});
	});
</script>

<div id="divPage1" title="Page 1">
	<form id="formPage1">
		<p class="validateTips">All form fields are required.</p>
		
		<table>
			<tr>
				<td><label for="box1">Box1</label></td>
				<td><input type="text" name="box1" id="box1" /></td>
			</tr>
		</table>
	</form>
</div>

<div id="divPage2" title="Page 2">
	<form id="formPage2">
		<p class="validateTips">All form fields are required.</p>
		
		<table>
			<tr>
				<td><label for="box2">Box2</label></td>
				<td><input type="text" name="box2" id="box2" /></td>
			</tr>
		</table>
	</form>
</div>

<div>
Combo form (this will normally be hidden)
	<form id="comboForm" action="">
		<table>
		<tr>
			<td><label for="combo_box1">box1</label></td>
			<td><input type="text" name="combo_box1" id="combo_box1" /></td>
		</tr>
		<tr>
			<td><label for="combo_box2">box2</label></td>
			<td><input type="text" name="combo_box2" id="combo_box2" /></td>
		</tr>
		</table>
	</form>
</div>

<div>
	<button id="multiFormOpener">Open the dialog</button>
</div>

<h4>Test JQuery [Form] Validation lib</h4>

<script type="text/javascript">
	$(document).ready(function() {
		$("#formToValidate").validate();
	});
</script>

<style type="text/css">
#formToValidate label.error {
	color: red;
}
#formToValidate input.error {
	background-color: pink;
}
</style>

<form id="formToValidate">
<table>
	<tr>
		<td><label for="description">Description</label></td>
		<td><input type="text" name="description" class="required" /></td>
	</tr>
	<tr>
		<td><label for="integer">Integer</label></td>
		<td><input type="text" name="integer" class="digits" /></td>
	</tr>
	<tr>
		<td><input type="submit" value="Submit" /></td>
	</tr>
</table>
</form>

</body>
</html>
