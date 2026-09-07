/*
 * Function: fnGetDisplayNodes
 * Purpose:  Return an array with the TR nodes used for displaying the table
 * Returns:  array node: TR elements
 *           or
 *           node (if iRow specified)
 * Inputs:   object:oSettings - automatically added by DataTables
 *           int:iRow - optional - if present then the array returned will be the node for
 *             the row with the index 'iRow'
 */
$.fn.dataTableExt.oApi.fnGetDisplayNodes = function ( oSettings, iRow )
{
	var anRows = [];
	if ( oSettings.aiDisplay.length !== 0 )
	{
		if ( typeof iRow != 'undefined' )
		{
			return oSettings.aoData[ oSettings.aiDisplay[iRow] ].nTr;
		}
		else
		{
			for ( var j=oSettings._iDisplayStart ; j<oSettings._iDisplayEnd ; j++ )
			{
				var nRow = oSettings.aoData[ oSettings.aiDisplay[j] ].nTr;
				anRows.push( nRow );
			}
		}
	}
	return anRows;
};

$.fn.dataTableExt.oApi.fnGetHiddenNodes = function ( oSettings )
{
	/* Note the use of a DataTables 'private' function thought the 'oApi' object */
	var anNodes = this.oApi._fnGetTrNodes( oSettings );
	var anDisplay = $('tbody tr', oSettings.nTable);
	
	/* Remove nodes which are being displayed */
	for ( var i=0 ; i<anDisplay.length ; i++ )
	{
		var iIndex = jQuery.inArray( anDisplay[i], anNodes );
		if ( iIndex != -1 )
		{
			anNodes.splice( iIndex, 1 );
		}
	}
	
	/* Fire back the array to the caller */
	return anNodes;
};

$.fn.dataTableExt.oApi.fnGetFilteredNodes = function ( oSettings )
{
	var anRows = [];
	for ( var i=0, iLen=oSettings.aiDisplay.length ; i<iLen ; i++ )
	{
		var nRow = oSettings.aoData[ oSettings.aiDisplay[i] ].nTr;
		anRows.push( nRow );
	}
	return anRows;
};

$.fn.dataTableExt.oApi.fnResetAllFilters = function (oSettings) {
	for (iCol = 0; iCol < oSettings.aoPreSearchCols.length; iCol++) {
		oSettings.aoPreSearchCols[ iCol ].sSearch = '';
	}
	oSettings.oPreviousSearch.sSearch = '';
	// Don't need to call fnDraw() since we expect calling function to call
	//  fnFilter() immediately after which calls fnDraw().
	//oTable.fnDraw();
}

//smarkoff: I'm sure there is a way so I don't have to pass dataTable
//(into itself) but tried "this" and "$(this)" and neither works.
$.fn.dataTableExt.oApi.fnSetupWileySearch = function (oSettings, tableId, dataTable, bStateSave) {
	tableId = "#" + tableId;
	var select = $(tableId + "_searchColumn");
	var textBox = $(tableId + "_searchText");
	
	select.append('<option value="-1">All</option>');
	
	var aoColumns = oSettings['aoColumns'];

	for (var i = 0; i < aoColumns.length; i++) { 
		var col = aoColumns[i];
		var bSearchable = col["bSearchable"];
		// sTitle is always set, even if <th> is used to set the titles instead of sTitle
		var title = col["sTitle"];
 	
		if (bSearchable) {
			select.append('<option value="' + i + '">' + title + '</option>');
		}
	}
	
	var eventFunc = function(objEvent) {
		var index = select.val();
		if (index == -1) index = null;
		//alert("filtering on [" + $(textBoxId).val() + "], column = " + index);
		dataTable.fnResetAllFilters();
		dataTable.fnFilter(textBox.val(), index, false, true, false);
		
		if (bStateSave) {
			// set a cookie that expires when the session expires
			$.cookie(cookieName, select.val() + ":" + textBox.val());
		}
	};
	
	// for some reason bStateSave is undefined
	//var bStateSave = oSettings["bStateSave"];
	//alert("bStateSave = " + oSettings['bStateSave']);
	
	var cookieName = "wileySearch_" + tableId;
	if (bStateSave) {
		var cookie = $.cookie(cookieName);
		if (cookie != null) {
			var array = cookie.split(":");
		    select.val(array[0]);
		    textBox.val(array[1]);
		    eventFunc(null);
		}
	}
	
	textBox.keyup(eventFunc);
	select.change(eventFunc);
};

/*
 * Function: fnGetPositionByColumn
 * Purpose:  Return the index of the row in the aoData object
 * Returns:  the return value will be an integer with the index of the row in the aoData object
 * Inputs:   object:oSettings - automatically added by DataTables
 *           int:iColumn - the column index
 *			 string:sValue the column value 
 */
$.fn.dataTableExt.oApi.fnGetPositionByColumn = function (oSettings, iColumn, sValue) {
	var aColumnData = $.map (oSettings.aoData, function (aRow) {
		return aRow._aData[iColumn];
	});
	var iDataIndex = $.inArray (sValue, aColumnData);
	return iDataIndex;
}

/*
 * Function: fnGetDataForProfile
 * Purpose:  Returns an array of data based on the profile definition
 * Returns:  the return value will be an array of data
 * Inputs:   object:oSettings - automatically added by DataTables
 *           profile 
 */
$.fn.dataTableExt.oApi.fnGetDataForProfile = function (oSettings, _aData, profile) {
	var aData = [];
	for ( var j=0, jLen=profile.length ; j<jLen ; j++ ) {
		aData.push( _aData[profile[j]["sName"]] );
	}
	return aData;
}

/*
 * Function: fnGetAllValues
 * Purpose:  Returns an array of values specific to a column index
 * Returns:  the return value will be an array of data
 * Inputs:   object:oSettings - automatically added by DataTables
 *           profile 
 */
$.fn.dataTableExt.oApi.fnGetAllValues = function (oSettings, iColumn) {
	var aColumnData = $.map (oSettings.aoData, function (aRow) {
		return aRow._aData[iColumn];
	});
	return aColumnData;
}


/*
 * Function: fnAddDataAtPosition (custom code Written for Wiley)
 * Purpose:  Wiley version of Add new row(s) into the table which inserts the row but does not re-sort
 * Returns:  array int: array of indexes (aoData) which have been added (zero length on error)
 * Inputs:   array:mData - the data to be added. The length must match
 *               the original data from the DOM
 *           bool:bRedraw - redraw the table or not - default true
 * Notes:    Warning - the re-filter here will cause the table to redraw
 *             starting at zero
 * Notes:    This function calls the standard add row to table method and then it manipulates the screen
 *           array to Bubble up the newly inserted row to the position specified in iPosition
 */
$.fn.dataTableExt.oApi.fnAddDataAtPosition = function(oSettings, mData, bRedraw, iPosition)
{
	if ( mData.length === 0 )
	{
		return [];
	}

	var aiReturn = [];
	var iTest;
	
	/* Find settings from table node */
//	var oSettings = _fnSettingsFromNode( this[_oExt.iApiIndex] );
	
	oSettings.bFilter = false;
	oSettings.bSort = false;
		
	iTest = this.oApi._fnAddData( oSettings, mData );
		
	var dRow = oSettings.aiDisplayMaster.pop();
	oSettings.aiDisplayMaster.splice( iPosition, 0, dRow );
	if ( iTest == -1 )
	{
		return aiReturn;
	}
	aiReturn.push( iTest );
	
	oSettings._iDisplayEnd = oSettings._iDisplayEnd + 1;
	oSettings.aiDisplay = oSettings.aiDisplayMaster.slice();
		
	oSettings.bSorted = false;
	oSettings.bFiltered = false;
	oSettings.bFilter = false;
	oSettings.bSort = false;
	
	// do not call this method to calculate			
	//_fnCalculateEnd( oSettings );   
	this.oApi._fnDraw( oSettings );   
	this.oApi._fnProcessingDisplay(oSettings, false);  

	return aiReturn;
};

/*
 * Function: fnGetIndexOfDisplayRow  (custom code Written for Wiley)
 * Purpose:  Return an idex to the display arraw where a row contains a value =  to keyValue at location KeyIdx
 * Returns:  Integer index value
 * Inputs:   keyIdx: which column to look in
 *           keyValue which value to look at
 */
$.fn.dataTableExt.oApi.fnGetIndexOfDisplayRow = function(oSettings, keyIdx, keyValue)
{
	// get settings from model first
//	var oSettings = _fnSettingsFromNode( this[_oExt.iApiIndex] );

	iStart = oSettings._iDisplayStart;
	iEnd = oSettings._iDisplayEnd;
	var idx = 0;
	for ( var j=iStart ; j<iEnd ; j++ )
	{
		var aoData = oSettings.aoData[ oSettings.aiDisplay[j] ];
		if( aoData._aData[keyIdx] == keyValue) 
		{
			return j;
		}
		
		idx++;
	}
	
	return iEnd;
	
};

/*
 * Natural Sort algorithm for Javascript - Version 0.6 - Released under MIT license
 * Author: Jim Palmer (based on chunking idea from Dave Koelle)
 * Contributors: Mike Grier (mgrier.com), Clint Priest, Kyle Adams, guillermo
 */
function naturalSort (a, b) {
	var re = /(^-?[0-9]+(\.?[0-9]*)[df]?e?[0-9]?$|^0x[0-9a-f]+$|[0-9]+)/gi,
		sre = /(^[ ]*|[ ]*$)/g,
		dre = /(^([\w ]+,?[\w ]+)?[\w ]+,?[\w ]+\d+:\d+(:\d+)?[\w ]?|^\d{1,4}[\/\-]\d{1,4}[\/\-]\d{1,4}|^\w+, \w+ \d+, \d{4})/,
		hre = /^0x[0-9a-f]+$/i,
		ore = /^0/,
		// convert all to strings and trim()
		x = a.toString().replace(sre, '') || '',
		y = b.toString().replace(sre, '') || '',
		// chunk/tokenize
		xN = x.replace(re, '\0$1\0').replace(/\0$/,'').replace(/^\0/,'').split('\0'),
		yN = y.replace(re, '\0$1\0').replace(/\0$/,'').replace(/^\0/,'').split('\0'),
		// numeric, hex or date detection
		xD = parseInt(x.match(hre)) || (xN.length != 1 && x.match(dre) && Date.parse(x)),
		yD = parseInt(y.match(hre)) || xD && y.match(dre) && Date.parse(y) || null;
	// first try and sort Hex codes or Dates
	if (yD)
		if ( xD < yD ) return -1;
		else if ( xD > yD )	return 1;
	// natural sorting through split numeric strings and default strings
	for(var cLoc=0, numS=Math.max(xN.length, yN.length); cLoc < numS; cLoc++) {
		// find floats not starting with '0', string or 0 if not defined (Clint Priest)
		oFxNcL = !(xN[cLoc] || '').match(ore) && parseFloat(xN[cLoc]) || xN[cLoc] || 0;
		oFyNcL = !(yN[cLoc] || '').match(ore) && parseFloat(yN[cLoc]) || yN[cLoc] || 0;
		// handle numeric vs string comparison - number < string - (Kyle Adams)
		if (isNaN(oFxNcL) !== isNaN(oFyNcL)) return (isNaN(oFxNcL)) ? 1 : -1; 
		// rely on string comparison if different types - i.e. '02' < 2 != '02' < '2'
		else if (typeof oFxNcL !== typeof oFyNcL) {
			oFxNcL += ''; 
			oFyNcL += ''; 
		}
		if (oFxNcL < oFyNcL) return -1;
		if (oFxNcL > oFyNcL) return 1;
	}
	return 0;
}

$.fn.dataTableExt.ofnSearch['title-numeric']  = function(sData) {
	return sData.replace(/\n/g," ").replace( /<.*?>/g, "" );
};

$.fn.dataTableExt.oSort['title-numeric-asc']  = function(a,b) {
    var x = a.match(/title="*(-?[0-9\.]+)"/);
   	x = (x == null) ? "-1" : x[1];
    var y = b.match(/title="*(-?[0-9\.]+)"/);
   	y = (y == null) ? "-1" : y[1];
    x = parseFloat( x );
    y = parseFloat( y );
    return ((x < y) ? -1 : ((x > y) ?  1 : 0));
};
 
$.fn.dataTableExt.oSort['title-numeric-desc'] = function(a,b) {
    var x = a.match(/title="*(-?[0-9\.]+)"/);
   	x = (x == null) ? "-1" : x[1];
    var y = b.match(/title="*(-?[0-9\.]+)"/);
   	y = (y == null) ? "-1" : y[1];
    x = parseFloat( x );
    y = parseFloat( y );
    return ((x < y) ?  1 : ((x > y) ? -1 : 0));
};

$.fn.dataTableExt.oSort['natural-asc']  = function(a,b) {
	return naturalSort(a,b);
};

$.fn.dataTableExt.oSort['natural-desc'] = function(a,b) {
	return naturalSort(a,b) * -1;
};