String.prototype.truncate = function(length) {
  if (this.length > length) {
    return this.slice(0, length - 3) + "...";
  } else {
    return this;
  }
};

String.prototype.isEmpty = function() {
	if (this == '' || this == '---')
		return true;
	else
		return false;
};

String.prototype.formatSourceHTML = function () {
	var trunc = this.truncate (30);
	// if it has no entries for disabled source
	if (trunc.indexOf("{") < 0 && trunc.indexOf("}") < 0) {
		return trunc;
	}
	// if just the open bracket is in the text
	if (trunc.indexOf("{") >= 0 && trunc.indexOf("}") < 0) {
		trunc = trunc + "}";
	}
    var result = trunc.replace(/\{/g,"<span class='disabled'>");
    var result = result.replace (/\}/g,"</span>");
    return result;
};

String.prototype.formatSourceText = function () {
    var result = this.replace(/\{/g,"");
    var result = result.replace (/\}/g,"(Inactive)");
    return result;
};

String.prototype.tooltip = function () {
	if (this == 'No Source')
		return 'no ownership information';
	else if (this == 'Not Requested')
		return 	'no permissions request has been created';
	else if (this == 'Request Sent')
		return 	'waiting for reply from source';
	else if (this == 'Amendment Needed')
		return 	'a limitation of liability amendment needs to be created';
	else if (this == 'Amendment Sent')
		return 	'waiting for signed amendment from source';
	else if (this == 'Unpaid')
		return 	'this asset is waiting for a payment request';
	else if (this == 'Contract Insufficient')
		return 	'the contract terms do not meet the set up requirements for this product';
	else if (this == 'Out of Compliance')
		return 	'the contract has expired or the print run has been exceeded';
	else if (this == 'Granted No Restrictions')
		return 	'there are no limitations to the use of this asset in this product';
	else if (this == 'Granted 3rd Party')
		return 	'the asset is usable with some conditions';
	else if (this == 'Cancelled')
		return 	'this asset will not be used in this product';
};

(function( $ ){
	$.fn.serializeJSON=function() {
		var json = {};
		jQuery.map($(this).serializeArray(), function(n, i){
			json[n['name']] = n['value'];
		});
		return json;
	};
})( jQuery );

jQuery.ajaxPrefilter( "script", function( options ) {
    options.cache = true;
} );

jQuery.fn.wait = function (MiliSeconds) {
    $(this).animate({ opacity: '+=0' }, MiliSeconds, function () {
    	$(this).hide ();
    });
    return this;
}
    
showSuccessMessage = function ( msg ) {
	$('#message').show();
	$('#message').css("background", "yellow");
	$('#message').html (msg);
	$('#message').wait (20000);
	
	
};

showErrorMessage = function ( msg ) {
	$('#message').show();
	$('#message').css("background", "red");
	$('#message').html (msg);
	$('#message').wait (20000);
};

isBChecked = function (value1) {
	if (value1)
		return "checked='checked'";
};

bDisplay = function (value1) {
	if (value1)
		return "Yes";
	else
		return "No";
};

jQuery.exists = function (selector) {
	return ($(selector).length > 0);
}

jQuery.checked = function (value1, value2) {
	if (value1 == value2)
		return "checked='checked'";
}

$('textarea[maxlength]').live('keyup blur', function() {
    // Store the maxlength and value of the field.
    var maxlength = $(this).attr('maxlength');
    var val = $(this).val();

    // Trim the field if it has content over the maxlength.
    if (val.length > maxlength) {
        $(this).val(val.slice(0, maxlength));
    }
});

jQuery.initDatePicker = function (selector, selectorInput, dateFormat) {
	// the hidden field is always the bean field (the ones that will be saved). It has to be always in the mm/dd/yy format.
	// if the mapping is done by spring, that is accomplished thru the custom DateEditor
	// otherwise use <fmt:formatDate var="strInvoiceDate" type="date" pattern="MM/dd/yy" value="${license.invoiceDate}"/>		
	$(selector).datepicker({dateFormat: 'mm/dd/yy'});	
	var currentDate = $(selector).datepicker( "getDate" );

	$(selectorInput).datepicker({altField: selector, altFormat: 'mm/dd/yy', dateFormat: dateFormat});
	$(selectorInput).datepicker("setDate", currentDate);
}

debug = function (log_txt) {
    if (typeof window.console != 'undefined') {
        console.log(log_txt);
    } else {
    	alert (log_txt);
    }
}