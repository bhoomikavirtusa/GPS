<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <title>Data Tables experiments</title>
    <link rel="stylesheet" type="text/css" href="<c:url value="/css/shared.css" />" />
    <link rel="stylesheet" type="text/css" href="<c:url value="/dataTables/css/dataTables.css"/>" />
    <link rel="stylesheet" type="text/css" href="<c:url value="/dataTables/css/demo_table.css"/>" />
    <script type="text/javascript" src="<c:url value="/js/jquery-1.5.2.min.js" />"></script>
    <script type="text/javascript" src="<c:url value="/js/jquery.cookie.js" />"></script>
    <script type="text/javascript" src="<c:url value="/dataTables/js/jquery.dataTables.min.js" />"></script>
	<script type="text/javascript" src="<c:url value="/dataTables/js/jquery.dataTables.custom.js" />"></script>
	
	<script type="text/javascript">
	$(document).ready(function() {
		var dataTable = $('#tableId').dataTable( {
			"sDom": '<"top"l>rt<"bottom"ip><"clear">',
			"aoColumns" : [
			    { "sWidth" : "10px", "sClass" : "center", "bSortable" : false, "bSearchable" : false },
			    { "sWidth" : "40%" },  // name
			    { "sWidth" : "60%" },  // description
			    { "sWidth" : "10px" }   // integer
			],
			"aaSorting": [[1, "asc" ]],

			"bAutoWidth": false,
			"bProcessing": true,
			//"bStateSave": true,
			"sPaginationType": "full_numbers",
			"aLengthMenu": [10, 15, 25, 50, 100],  // override default of [10, 25, 50, 100]
			"iDisplayLength": 10 // same as default
		} );

		dataTable.fnSetupWileySearch("tableId", dataTable, true);
	} );
	</script>
	<!-- Alternate way of hiding global filter but using sDom above instead
	<style type="text/css">
		.dataTables_filter { display: none; }
	</style> 
	-->
</head>
<body>

<h3>Data Tables experiments</h3>

<div>
	<select id="tableId_searchColumn">
	</select>
	<input type="text" size="20" id="tableId_searchText" />
</div>

<table id="tableId" class="display">
<thead>
	<tr>
		<th>non-sortable</th>
		<th>name</th>
		<th>description</th>
		<th>integer</th>
	</tr>
</thead>
<tbody>
	<tr>
		<td>blah</td>
		<td>name1</td>
		<td>d1</td>
		<td>10</td>
	</tr>
	<tr>
		<td>blah</td>
		<td>name2</td>
		<td>d2</td>
		<td>9</td>
	</tr>
	<tr>
		<td>blah</td>
		<td>name3</td>
		<td>d3</td>
		<td>8</td>
	</tr>
	<tr>
		<td>blah</td>
		<td>name4</td>
		<td>d4</td>
		<td>7</td>
	</tr>
	<tr>
		<td>blah</td>
		<td>name5</td>
		<td>d5</td>
		<td>6</td>
	</tr>
	<tr>
		<td>blah</td>
		<td>name6</td>
		<td>d6</td>
		<td>5</td>
	</tr>
	<tr>
		<td>blah</td>
		<td>name7</td>
		<td>d7</td>
		<td>4</td>
	</tr>
</tbody>

</table>

</body>
</html>
