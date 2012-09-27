
var UIImageCropper = {
	init: function(params) {
	  var cropLabel = params.cropLabel || null;
	  var selectionExists;
	  
	  $('input#croppedInfo')[0].type = 'hidden';
	  var cropActionBtn = $("a#cropAction");
	  var onclickAttr = cropActionBtn.attr('onclick');
	
	  cropActionBtn.addClass('DisableButton');     
	  cropActionBtn.attr("onclick", '');     
			
		$('img#profileAvatar').imageCrop({
	    displaySize : true,
	    overlayOpacity : 0.25,
	
	    onSelect : updateForm
	  });
	  
    function updateForm(crop) {
      var x = crop.selectionX;
      var y = crop.selectionY;
      var w = crop.selectionWidth;
      var h = crop.selectionHeight;
      var croppedInfo = [];
      croppedInfo.push("X:" + x, "Y:" + y, "WIDTH:" + w,"HEIGHT:" + h);
      
	    $('input#croppedInfo').val(croppedInfo.join());
	     
	    selectionExists = crop.selectionExists();
        
	    var cropActionButton = $("a#cropAction");
	    if ( selectionExists ) {
	      cropActionButton.removeClass('DisableButton');
	      cropActionButton.attr("onclick", onclickAttr);
	    } else {
	      cropActionButton.addClass('DisableButton');
	      cropActionButton.attr("onclick", '');
	    }
    };
	}
};

_module.UIImageCropper = UIImageCropper;
