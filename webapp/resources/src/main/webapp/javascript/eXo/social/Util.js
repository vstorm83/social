/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
 
/**
 * Util.js
 * Utility class
 * @author	<a href="mailto:hoatlevan@gmail.com">hoatle</a>
 * @since	Oct 20, 2009
 * @copyright	eXo Platform SEA
 */
 
/*
*Social jQuery plugin
*/ 
// Placeholder plugin for HTML 5
;(function($) {
    
	function Placeholder(input) {
    this.input = input;

    // In case of submitting, ignore placeholder value
    $(input[0].form).submit(function() {
        if (input.hasClass('placeholder') && input.val() == input.attr('placeholder')) {
            input.val('');
        }
    });
	}
	
	Placeholder.prototype = {
    show : function(loading) {
        if (this.input.val() === '' || (loading && this.isDefaultPlaceholderValue())) {
            this.input.addClass('placeholder');
            this.input.val(this.input.attr('placeholder'));
        }
    },
    hide : function() {
        if (this.isDefaultPlaceholderValue() && this.input.hasClass('placeholder')) {
            this.input.removeClass('placeholder');
            this.input.val('');
        }
    },
    isDefaultPlaceholderValue : function() {
        return this.input.val() == this.input.attr('placeholder');
    }
	};
	
	var HAS_SUPPORTED = !!('placeholder' in document.createElement('input'));
	
	$.fn.placeholder = function() {
    return HAS_SUPPORTED ? this : this.each(function() {
        var input = $(this);
        var placeholder = new Placeholder(input);
        
        placeholder.show(true);
        
        input.focus(function() {
            placeholder.hide();
        });
        
        input.blur(function() {
            placeholder.show(false);
        });
    });
	}
})(jQuery);

// Autosuggestion plugin.
;(function($) {

    $.fn.autosuggest = function(url, options) {
      var KEYS = {
        ENTER : 13,
        DOWN : 40,
        UP : 38
      },
      DELIMITER = ',',
      DELIMITER_AND_SPACE = ', ';

	    var COLOR = {
	      FOCUS : "#000000",
	      BLUR : "#C7C7C7"
	    };

      var defaults = {
        defaultVal: undefined,
        onSelect: undefined,
        maxHeight: 150,
        multisuggestion : false,
        width: undefined
      };

      options = $.extend(defaults, options);

     return this.each(function() {

	      var input = $(this),
	          results = $('<div />'),
	          currentSelectedItem, posX, posY;

        $(results).addClass('suggestions')
                    .css({
                      'top': (input.position().top + input.height() + 5) + 'px',
                      'left': input.position().left + 'px',
                      'width': options.width || (input.width() + 'px')
                    })
                    .hide();

        // append to target input
	      input.after(results)
	           .keyup(keysActionListener)
	           .blur(function(e) {
	                var resPos = $(results).offset();
	                
	                resPosBottom = resPos.top + $(results).height();
	                resPosRight = resPos.left + $(results).width();
	                
	                if (posY < resPos.top || posY > resPosBottom || posX < resPos.left || posX > resPosRight) {
                    $(results).hide();
	                }

	                if ($(this).val().trim().length == 0) {
	                  $(input).val(options.defaultVal);
	                  $(input).css('color', COLOR.BLUR);
	                }
	           })
	           .focus(function(e) {
	                $(results).css({
	                  'top': (input.position().top + input.height() + 5) + 'px',
	                  'left': input.position().left + 'px'
	                });

	                if ($('div', results).length > 0) {
	                  $(results).show();
	                }
	                
	                if (options.defaultVal && $(this).val() == options.defaultVal) {
	                  $(this).val('');
	                  $(this).css('color', COLOR.FOCUS);
	                } 
	                
	           })
	           .attr('autocomplete', 'off');

	        function buildResults(searchedResults) {
	            var i, iFound = 0;

	            $(results).html('').hide();

              if (searchedResults == null) return;

	            // build list of item over searched result
	            for (i = 0; i < searchedResults.length; i += 1) {
                var item = $('<div />'),
                    text = searchedResults[i];

                $(item).append('<p class="text">' + text + '</p>');

                if (typeof searchedResults[i].extra === 'string') {
                  $(item).append('<p class="extra">' + searchedResults[i].extra + '</p>');
                }

                $(item).addClass('resultItem')
                    .click(function(n) { return function() {
                      selectResultItem(searchedResults[n]);
                    };}(i))
                    .mouseover(function(el) { return function() {
                      changeHover(el);
                    };}(item));

                $(results).append(item);

                iFound += 1;
                if (typeof options.maxResults === 'number' && iFound >= options.maxResults) {
                  break;
                }
	            }

	            if ($('div', results).length > 0) { // if have any element then display the list
                currentSelectedItem = undefined;
                $(results).show().css('height', 'auto');
                if ($(results).height() > options.maxHeight) {
                    $(results).css({'overflow': 'auto', 'height': options.maxHeight + 'px'});
                }
	            }
	        };

	        function reloadData() {
	          var val = input.val();
	          var search_str;
	          
	          if (val.length > 0) val = $.trim(val);
	          
	          if (options.multisuggestion) {
	            search_str = getSearchString(val);
	          } else {
	            search_str = val;
	          }
	          
	          var restUrl = url.replace('input_value', search_str);
	          $.ajax({
	                  type: "GET",
	                  url: restUrl,
	                  complete: function(jqXHR) {
					            if(jqXHR.readyState === 4) {
					              buildResults($.parseJSON(jqXHR.responseText).names);
					            }
	                  }
	          })
	        };

	        function selectResultItem(item) {

	          setValues(item);
	          
	          $(results).html('').hide();
	          if (typeof options.onSelect === 'function') {
	            options.onSelect(item);
	          }
	        };

          function getSearchString(val) {
			      var arr = val.split(DELIMITER);
			      return $.trim(arr[arr.length - 1]);
			    };

	        function changeHover(element) {
            $('div.resultItem', results).removeClass('hover');
            $(element).addClass('hover');
            currentSelectedItem = element;
          };

          function setValues(item) {
            var currentVals = $.trim(input.val());
            var selectedVals;
            
            if (options.multisuggestion) {
	            if(currentVals.indexOf(DELIMITER) >= 0) {
	              selectedVals = currentVals.substr(0, currentVals.lastIndexOf(DELIMITER)) + DELIMITER_AND_SPACE + item;
	              input.val(selectedVals);
	            } else {
	              input.val(item);
	            }
	          } else {
	            input.val(item);
	          }
          };
          
	        function keysActionListener(event) {
	          var keyCode = event.keyCode || event.which;

	          switch (keyCode) {
	            case KEYS.ENTER:
	                if (options.multisuggestion) {
	                   $(currentSelectedItem).trigger('click');
                     return false;
	                }
	                
	                if (currentSelectedItem) {
                    $(currentSelectedItem).trigger('click');
	                } else {
	                  options.onSelect();
	                }

	                return false;
	            case KEYS.DOWN:
	                if (typeof currentSelectedItem === 'undefined') {
	                  currentSelectedItem = $('div.resultItem:first', results).get(0);
	                } else {
	                  currentSelectedItem = $(currentSelectedItem).next().get(0);
	                }

	                changeHover(currentSelectedItem);
	                if (currentSelectedItem) {
	                  $(results).scrollTop(currentSelectedItem.offsetTop);
	                }

	                return false;
	            case KEYS.UP:
	                if (typeof currentSelectedItem === 'undefined') {
	                  currentSelectedItem = $('div.resultItem:last', results).get(0);
	                } else {
	                  currentSelectedItem = $(currentSelectedItem).prev().get(0);
	                }

	                changeHover(currentSelectedItem);
	                if (currentSelectedItem) {
	                  $(results).scrollTop(currentSelectedItem.offsetTop);
	                }

	                return false;
	            default:
	                reloadData.apply(this, [event]);
	          }
	        };

	        $('body').mousemove(function(e) {
            posX = e.pageX;
            posY = e.pageY;
          });
	    });
    }
})(jQuery);


// Tooltip plugin
;(function($) {
    $.fn.toolTip = function(url, settings) {

        var defaultSettings = {
	        className   : 'UserName',
	        color       : 'yellow',
	        onHover     : undefined,
	        timeout     : 300
        };
        
        /* Combining the default settings object with the supplied one */
        settings = $.extend(defaultSettings, settings);

        return this.each(function() {

            var elem = $(this);
            
            // Continue with the next element in case of not effected element
            if(!elem.hasClass(settings.className)) return true;
            
            var scheduleEvent = new eventScheduler();
            var tip = new Tip();

            elem.append(tip.generate()).addClass('UIToolTipContainer');

            elem.addClass(settings.color);
            
            elem.hover(function() {
              reLoadPopup();
	            tip.show();
	            scheduleEvent.clear();
            },function(){
	            scheduleEvent.set(function(){
	              tip.hide();
	            }, settings.timeout);
            });
            
            function reLoadPopup() {
              var hrefValue = elem.attr('href');
              var personId = hrefValue.substr(hrefValue.lastIndexOf("/") + 1);
              
              var restUrl = url.replace('person_Id', personId);
              
              $.ajax({
                      type: "GET",
                      url: restUrl,
                      complete: function(jqXHR) {
                                if(jqXHR.readyState === 4) {
                                  var avatarURL = ($.parseJSON(jqXHR.responseText)).avatarURL;
										              var activityTitle = ($.parseJSON(jqXHR.responseText)).activityTitle;
										              var relationStatus = ($.parseJSON(jqXHR.responseText)).relationshipType;
										              
										              var html = [];
						                      html.push('<div style="float: right; cursor:pointer;">');
						                      html.push('  <div id="ClosePopup" class="ClosePopup" title="Close">[x]</div>');
						                      html.push('</div>');
						                      html.push('<div id="UserAvatar" class="UserAvatar">');
						                      html.push('  <img title="Avatar" alt="Avatar" src="' + avatarURL + '"></img>'); 
						                      html.push('</div>');
						                      html.push('<div id="UserTitle" class="UserTitle">');
						                      html.push('  <span>');
						                      html.push(     activityTitle);
						                      html.push('  </span>');
						                      html.push('</div>');
						                      html.push('<div id="UserAction" class="UserAction">');
						                      html.push('<span>');
										              html.push('</span>');
						                      html.push('</div>');
						                      $('.UIToolTip').html(html.join(''));
                                }
                      }
              })
            };
            
            function buildContent(resp) {
              
            }
        });
    };


    function eventScheduler(){};
    
    eventScheduler.prototype = {
      set : function (func,timeout){
        this.timer = setTimeout(func,timeout);
      },
      clear: function(){
        clearTimeout(this.timer);
      }
    };

    function Tip(){
	    this.shown = false;
    };
    
    Tip.prototype = {
	    generate: function(){
	        return this.tip || (this.tip = $('<span class="UIToolTip"><span class="pointyTipShadow"></span><span class="pointyTip"></span></span>'));
	    },
	    show: function(){
	        if(this.shown) return;
	        
	        this.tip.css('margin-left',-this.tip.outerWidth()/2).fadeIn('fast');
	        this.shown = true;
	    },
	    hide: function(){
	        this.tip.fadeOut();
	        this.shown = false;
	    }
    };
})(jQuery);

// Image cropping plugin
(function($) {
    $.imageCrop = function(object, customOptions) {
        // Rather than requiring a lengthy amount of arguments, pass the
        // plug-in options in an object literal that can be extended over
        // the plug-in's defaults
        var defaultOptions = {
            allowMove : true,
            allowResize : true,
            allowSelect : true,
            aspectRatio : 0,
            displaySizeHint : false,
            minSelect : [0, 0],
            minSize : [0, 0],
            maxSize : [0, 0],
            outlineOpacity : 0.5,
            overlayOpacity : 0.5,
            selectionPosition : [0, 0],
            selectionWidth : 0,
            selectionHeight : 0,

            // Plug-in's event handlers
            onChange : function() {},
            onSelect : function() {}
        };

        // Set options to default
        var options = defaultOptions;

        // And merge them with the custom options
        setOptions(customOptions);

        // Initialize the image layer
        var $image = $(object);

        // Initialize an image holder
        var $holder = $('<div />')
                .css({
                    position : 'relative'
                })
                .width($image.width())
                .height($image.height());

        // Wrap the holder around the image
        $image.wrap($holder)
            .css({
                position : 'absolute'
            });

        // Initialize an overlay layer and place it above the image
        var $overlay = $('<div id="image-crop-overlay" />')
                .css({
                    opacity : options.overlayOpacity,
                    position : 'absolute'
                })
                .width($image.width())
                .height($image.height())
                .insertAfter($image);

        // Initialize a trigger layer and place it above the overlay layer
        var $trigger = $('<div />')
                .css({
                    backgroundColor : '#000000',
                    opacity : 0,
                    position : 'absolute'
                })
                .width($image.width())
                .height($image.height())
                .insertAfter($overlay);

        // Initialize an outline layer and place it above the trigger layer
        var $outline = $('<div id="image-crop-outline" />')
                .css({
                    opacity : options.outlineOpacity,
                    position : 'absolute'
                })
                .insertAfter($trigger);

        // Initialize a selection layer and place it above the outline layer
        var $selection = $('<div />')
                .css({
                    background : 'url(' + $image.attr('src') + ') no-repeat',
                    position : 'absolute'
                })
                .insertAfter($outline);

        // Initialize a background layer of size hint and place it above the
        // selection layer
        var $sizeHintBackground = $('<div id="image-crop-size-hint-background" />')
                .css({
                    opacity : 0.35,
                    position : 'absolute'
                })
                .insertAfter($selection);

        // Initialize a foreground layer of size hint and place it above the
        // background layer
        var $sizeHintForeground = $('<span id="image-crop-size-hint-foreground" />')
                .css({
                    position : 'absolute'
                })
                .insertAfter($sizeHintBackground);

        // Initialize a north/west resize handler and place it above the
        // selection layer
        var $nwResizeHandler = $('<div class="image-crop-resize-handler" id="image-crop-nw-resize-handler" />')
                .css({
                    opacity : 0.5,
                    position : 'absolute'
                })
                .insertAfter($selection);

        // Initialize a north resize handler and place it above the selection
        // layer
//        var $nResizeHandler = $('<div class="image-crop-resize-handler" id="image-crop-n-resize-handler" />')
//                .css({
//                    opacity : 0.5,
//                    position : 'absolute'
//                })
//                .insertAfter($selection);

        // Initialize a north/east resize handler and place it above the
        // selection layer
        var $neResizeHandler = $('<div class="image-crop-resize-handler" id="image-crop-ne-resize-handler" />')
                .css({
                    opacity : 0.5,
                    position : 'absolute'
                })
                .insertAfter($selection);

        // Initialize an west resize handler and place it above the selection
        // layer
//        var $wResizeHandler = $('<div class="image-crop-resize-handler" id="image-crop-w-resize-handler" />')
//                .css({
//                    opacity : 0.5,
//                    position : 'absolute'
//                })
//                .insertAfter($selection);

        // Initialize an east resize handler and place it above the selection
        // layer
//        var $eResizeHandler = $('<div class="image-crop-resize-handler" id="image-crop-e-resize-handler" />')
//                .css({
//                    opacity : 0.5,
//                    position : 'absolute'
//                })
//                .insertAfter($selection);

        // Initialize a south/west resize handler and place it above the
        // selection layer
        var $swResizeHandler = $('<div class="image-crop-resize-handler" id="image-crop-sw-resize-handler" />')
                .css({
                    opacity : 0.5,
                    position : 'absolute'
                })
                .insertAfter($selection);

        // Initialize a south resize handler and place it above the selection
        // layer
//        var $sResizeHandler = $('<div class="image-crop-resize-handler" id="image-crop-s-resize-handler" />')
//                .css({
//                    opacity : 0.5,
//                    position : 'absolute'
//                })
//                .insertAfter($selection);

        // Initialize a south/east resize handler and place it above the
        // selection layer
        var $seResizeHandler = $('<div class="image-crop-resize-handler" id="image-crop-se-resize-handler" />')
                .css({
                    opacity : 0.5,
                    position : 'absolute'
                })
                .insertAfter($selection);

        // Initialize global variables
        var resizeHorizontally = true,
            resizeVertically = true,
            selectionExists,
            selectionOffset = [0, 0],
            selectionOrigin = [0, 0];

        // Verify if the selection size is bigger than the minimum accepted
        // and set the selection existence accordingly
        if (options.selectionWidth > options.minSelect[0] &&
            options.selectionHeight > options.minSelect[1])
            selectionExists = true;
        else
            selectionExists = false;

        // Call the 'updateInterface' function for the first time to
        // initialize the plug-in interface
        updateInterface();

        if (options.allowSelect)
            // Bind an event handler to the 'mousedown' event of the trigger layer
            $trigger.mousedown(setSelection);

        if (options.allowMove)
            // Bind an event handler to the 'mousedown' event of the selection layer
            $selection.mousedown(pickSelection);

        if (options.allowResize)
            // Bind an event handler to the 'mousedown' event of the resize handlers
            $('div.image-crop-resize-handler').mousedown(pickResizeHandler);

        // Merge current options with the custom option
        function setOptions(customOptions) {
            options = $.extend(options, customOptions);
        };

        // Get the current offset of an element
        function getElementOffset(object) {
            var offset = $(object).offset();

            return [offset.left, offset.top];
        };

        // Get the current mouse position relative to the image position
        function getMousePosition(event) {
            var imageOffset = getElementOffset($image);

            var x = event.pageX - imageOffset[0],
                y = event.pageY - imageOffset[1];

            x = (x < 0) ? 0 : (x > $image.width()) ? $image.width() : x;
            y = (y < 0) ? 0 : (y > $image.height()) ? $image.height() : y;

            return [x, y];
        };

        // Return an object containing information about the plug-in state
        function getCropData() {
            return {
                selectionX : options.selectionPosition[0],
                selectionY : options.selectionPosition[1],
                selectionWidth : options.selectionWidth,
                selectionHeight : options.selectionHeight,

                selectionExists : function() {
                    return selectionExists;
                }
            };
        };

        // Update the overlay layer
        function updateOverlayLayer() {
            $overlay.css({
              display : selectionExists ? 'block' : 'none'
            });
        };

        // Update the trigger layer
        function updateTriggerLayer() {
            $trigger.css({
              cursor : options.allowSelect ? 'crosshair' : 'default'
            });
        };

        // Update the selection
        function updateSelection() {
            // Update the outline layer
            $outline.css({
                    cursor : 'default',
                    display : selectionExists ? 'block' : 'none',
                    left : options.selectionPosition[0],
                    top : options.selectionPosition[1]
                })
                .width(options.selectionWidth)
                .height(options.selectionHeight);

            // Update the selection layer
            $selection.css({
                    backgroundPosition : ( - options.selectionPosition[0] - 1) + 'px ' + ( - options.selectionPosition[1] - 1) + 'px',
                    cursor : options.allowMove ? 'move' : 'default',
                    display : selectionExists ? 'block' : 'none',
                    left : options.selectionPosition[0] + 1,
                    top : options.selectionPosition[1] + 1
                })
                .width((options.selectionWidth - 2 > 0) ? (options.selectionWidth - 2) : 0)
                .height((options.selectionHeight - 2 > 0) ? (options.selectionHeight - 2) : 0);
        };

        // Update the size hint
        function updateSizeHint(action) {
            switch (action) {
                case 'fade-out' :
                    // Fade out the size hint
                    $sizeHintBackground.fadeOut('slow');
                    $sizeHintForeground.fadeOut('slow');

                    break;
                default :
                    var display = (selectionExists && options.displaySize) ? 'block' : 'none';

                    // Update the foreground layer
                    $sizeHintForeground.css({
                            cursor : 'default',
                            display : display,
                            left : options.selectionPosition[0] + 4,
                            top : options.selectionPosition[1] + 4
                        })
                        .html(options.selectionWidth + 'x' + options.selectionHeight);

                    // Update the background layer
                    $sizeHintBackground.css({
                            cursor : 'default',
                            display : display,
                            left : options.selectionPosition[0] + 1,
                            top : options.selectionPosition[1] + 1
                        })
                        .width($sizeHintForeground.width() + 6)
                        .height($sizeHintForeground.height() + 6);
            }
        };

        // Update the resize handlers
        function updateResizeHandlers(action) {
            switch (action) {
                case 'hide-all' :
                    $('.image-crop-resize-handler').each(function() {
                        $(this).css({
                                display : 'none'
                            });
                    });

                    break;
                default :
                    var display = (selectionExists && options.allowResize) ? 'block' : 'none';

                    $nwResizeHandler.css({
                            cursor : 'nw-resize',
                            display : display,
                            left : options.selectionPosition[0] - Math.round($nwResizeHandler.width() / 2),
                            top : options.selectionPosition[1] - Math.round($nwResizeHandler.height() / 2)
                        });

//                    $nResizeHandler.css({
//                            cursor : 'n-resize',
//                            display : display,
//                            left : options.selectionPosition[0] + Math.round(options.selectionWidth / 2 - $neResizeHandler.width() / 2) - 1,
//                            top : options.selectionPosition[1] - Math.round($neResizeHandler.height() / 2)
//                        });

                    $neResizeHandler.css({
                            cursor : 'ne-resize',
                            display : display,
                            left : options.selectionPosition[0] + options.selectionWidth - Math.round($neResizeHandler.width() / 2) - 1,
                            top : options.selectionPosition[1] - Math.round($neResizeHandler.height() / 2)
                        });

//                    $wResizeHandler.css({
//                            cursor : 'w-resize',
//                            display : display,
//                            left : options.selectionPosition[0] - Math.round($neResizeHandler.width() / 2),
//                            top : options.selectionPosition[1] + Math.round(options.selectionHeight / 2 - $neResizeHandler.height() / 2) - 1
//                        });

//                    $eResizeHandler.css({
//                            cursor : 'e-resize',
//                            display : display,
//                            left : options.selectionPosition[0] + options.selectionWidth - Math.round($neResizeHandler.width() / 2) - 1,
//                            top : options.selectionPosition[1] + Math.round(options.selectionHeight / 2 - $neResizeHandler.height() / 2) - 1
//                        });

                    $swResizeHandler.css({
                            cursor : 'sw-resize',
                            display : display,
                            left : options.selectionPosition[0] - Math.round($swResizeHandler.width() / 2),
                            top : options.selectionPosition[1] + options.selectionHeight - Math.round($swResizeHandler.height() / 2) - 1
                        });

//                    $sResizeHandler.css({
//                            cursor : 's-resize',
//                            display : display,
//                            left : options.selectionPosition[0] + Math.round(options.selectionWidth / 2 - $seResizeHandler.width() / 2) - 1,
//                            top : options.selectionPosition[1] + options.selectionHeight - Math.round($seResizeHandler.height() / 2) - 1
//                        });

                    $seResizeHandler.css({
                            cursor : 'se-resize',
                            display : display,
                            left : options.selectionPosition[0] + options.selectionWidth - Math.round($seResizeHandler.width() / 2) - 1,
                            top : options.selectionPosition[1] + options.selectionHeight - Math.round($seResizeHandler.height() / 2) - 1
                        });
            }
        };

        // Update the cursor type
        function updateCursor(cursorType) {
            $trigger.css({
                    cursor : cursorType
                });

            $outline.css({
                    cursor : cursorType
                });

            $selection.css({
                    cursor : cursorType
                });

            $sizeHintBackground.css({
                    cursor : cursorType
                });

            $sizeHintForeground.css({
                    cursor : cursorType
                });
        };

        // Update the plug-in interface
        function updateInterface(sender) {
            switch (sender) {
                case 'setSelection' :
                    updateOverlayLayer();
                    updateSelection();
                    updateResizeHandlers('hide-all');

                    break;
                case 'pickSelection' :
                    updateResizeHandlers('hide-all');

                    break;
                case 'pickResizeHandler' :
                    updateSizeHint();
                    updateResizeHandlers('hide-all');

                    break;
                case 'resizeSelection' :
                    updateSelection();
                    updateSizeHint();
                    updateResizeHandlers('hide-all');
                    updateCursor('crosshair');

                    break;
                case 'moveSelection' :
                    updateSelection();
                    updateResizeHandlers('hide-all');
                    updateCursor('move');

                    break;
                case 'releaseSelection' :
                    updateTriggerLayer();
                    updateOverlayLayer();
                    updateSelection();
                    updateSizeHint('fade-out');
                    updateResizeHandlers();

                    break;
                default :
                    updateTriggerLayer();
                    updateOverlayLayer();
                    updateSelection();
                    updateResizeHandlers();
            }
        };

        // Set a new selection
        function setSelection(event) {
            // Prevent the default action of the event
            event.preventDefault();

            // Prevent the event from being notified
            event.stopPropagation();

            // Bind an event handler to the 'mousemove' event
            $(document).mousemove(resizeSelection);

            // Bind an event handler to the 'mouseup' event
            $(document).mouseup(releaseSelection);

            // Notify that a selection exists
            selectionExists = true;

            // Reset the selection size
            options.selectionWidth = 0;
            options.selectionHeight = 0;

            // Get the selection origin
            selectionOrigin = getMousePosition(event);

            // And set its position
            options.selectionPosition[0] = selectionOrigin[0];
            options.selectionPosition[1] = selectionOrigin[1];

            // Update only the needed elements of the plug-in interface
            // by specifying the sender of the current call
            updateInterface('setSelection');
        };

        // Pick the current selection
        function pickSelection(event) {
            // Prevent the default action of the event
            event.preventDefault();

            // Prevent the event from being notified
            event.stopPropagation();

            // Bind an event handler to the 'mousemove' event
            $(document).mousemove(moveSelection);

            // Bind an event handler to the 'mouseup' event
            $(document).mouseup(releaseSelection);

            var mousePosition = getMousePosition(event);

            // Get the selection offset relative to the mouse position
            selectionOffset[0] = mousePosition[0] - options.selectionPosition[0];
            selectionOffset[1] = mousePosition[1] - options.selectionPosition[1];

            // Update only the needed elements of the plug-in interface
            // by specifying the sender of the current call
            updateInterface('pickSelection');
        };

        // Pick one of the resize handlers
        function pickResizeHandler(event) {
            // Prevent the default action of the event
            event.preventDefault();

            // Prevent the event from being notified
            event.stopPropagation();

            switch (event.target.id) {
                case 'image-crop-nw-resize-handler' :
                    selectionOrigin[0] += options.selectionWidth;
                    selectionOrigin[1] += options.selectionHeight;
                    options.selectionPosition[0] = selectionOrigin[0] - options.selectionWidth;
                    options.selectionPosition[1] = selectionOrigin[1] - options.selectionHeight;

                    break;
                case 'image-crop-n-resize-handler' :
                    selectionOrigin[1] += options.selectionHeight;
                    options.selectionPosition[1] = selectionOrigin[1] - options.selectionHeight;

                    resizeHorizontally = false;

                    break;
                case 'image-crop-ne-resize-handler' :
                    selectionOrigin[1] += options.selectionHeight;
                    options.selectionPosition[1] = selectionOrigin[1] - options.selectionHeight;

                    break;
                case 'image-crop-w-resize-handler' :
                    selectionOrigin[0] += options.selectionWidth;
                    options.selectionPosition[0] = selectionOrigin[0] - options.selectionWidth;

                    resizeVertically = false;

                    break;
                case 'image-crop-e-resize-handler' :
                    resizeVertically = false;

                    break;
                case 'image-crop-sw-resize-handler' :
                    selectionOrigin[0] += options.selectionWidth;
                    options.selectionPosition[0] = selectionOrigin[0] - options.selectionWidth;

                    break;
                case 'image-crop-s-resize-handler' :
                    resizeHorizontally = false;

                    break;
            }

            // Bind an event handler to the 'mousemove' event
            $(document).mousemove(resizeSelection);

            // Bind an event handler to the 'mouseup' event
            $(document).mouseup(releaseSelection);

            // Update only the needed elements of the plug-in interface
            // by specifying the sender of the current call
            updateInterface('pickResizeHandler');
        };

        // Resize the current selection
        function resizeSelection(event) {
            // Prevent the default action of the event
            event.preventDefault();

            // Prevent the event from being notified
            event.stopPropagation();

            var mousePosition = getMousePosition(event);

            // Get the selection size
            var height = mousePosition[1] - selectionOrigin[1],
                width = mousePosition[0] - selectionOrigin[0];

            // If the selection size is smaller than the minimum size set it
            // accordingly
            if (Math.abs(width) < options.minSize[0])
                width = (width >= 0) ? options.minSize[0] : - options.minSize[0];

            if (Math.abs(height) < options.minSize[1])
                height = (height >= 0) ? options.minSize[1] : - options.minSize[1];

            // Test if the selection size exceeds the image bounds
            if (selectionOrigin[0] + width < 0 || selectionOrigin[0] + width > $image.width())
                width = - width;

            if (selectionOrigin[1] + height < 0 || selectionOrigin[1] + height > $image.height())
                height = - height;

            if (options.maxSize[0] > options.minSize[0] &&
                options.maxSize[1] > options.minSize[1]) {
                // Test if the selection size is bigger than the maximum size
                if (Math.abs(width) > options.maxSize[0])
                    width = (width >= 0) ? options.maxSize[0] : - options.maxSize[0];

                if (Math.abs(height) > options.maxSize[1])
                    height = (height >= 0) ? options.maxSize[1] : - options.maxSize[1];
            }

            // Set the selection size
            if (resizeHorizontally)
                options.selectionWidth = width;

            if (resizeVertically)
                options.selectionHeight = height;

            // If any aspect ratio is specified
            if (options.aspectRatio) {
                // Calculate the new width and height
                if ((width > 0 && height > 0) || (width < 0 && height < 0))
                    if (resizeHorizontally)
                        height = Math.round(width / options.aspectRatio);
                    else
                        width = Math.round(height * options.aspectRatio);
                else
                    if (resizeHorizontally)
                        height = - Math.round(width / options.aspectRatio);
                    else
                        width = - Math.round(height * options.aspectRatio);

                // Test if the new size exceeds the image bounds
                if (selectionOrigin[0] + width > $image.width()) {
                    width = $image.width() - selectionOrigin[0];
                    height = (height > 0) ? Math.round(width / options.aspectRatio) : - Math.round(width / options.aspectRatio);
                }

                if (selectionOrigin[1] + height < 0) {
                    height = - selectionOrigin[1];
                    width = (width > 0) ? - Math.round(height * options.aspectRatio) : Math.round(height * options.aspectRatio);
                }

                if (selectionOrigin[1] + height > $image.height()) {
                    height = $image.height() - selectionOrigin[1];
                    width = (width > 0) ? Math.round(height * options.aspectRatio) : - Math.round(height * options.aspectRatio);
                }

                // Set the selection size
                options.selectionWidth = width;
                options.selectionHeight = height;
            }

            if (options.selectionWidth < 0) {
                options.selectionWidth = Math.abs(options.selectionWidth);
                options.selectionPosition[0] = selectionOrigin[0] - options.selectionWidth;
            } else
                options.selectionPosition[0] = selectionOrigin[0];

            if (options.selectionHeight < 0) {
                options.selectionHeight = Math.abs(options.selectionHeight);
                options.selectionPosition[1] = selectionOrigin[1] - options.selectionHeight;
            } else
                options.selectionPosition[1] = selectionOrigin[1];

            // Trigger the 'onChange' event when the selection is changed
            options.onChange(getCropData());

            // Update only the needed elements of the plug-in interface
            // by specifying the sender of the current call
            updateInterface('resizeSelection');
        };

        // Move the current selection
        function moveSelection(event) {
            // Prevent the default action of the event
            event.preventDefault();

            // Prevent the event from being notified
            event.stopPropagation();

            var mousePosition = getMousePosition(event);

            // Set the selection position on the x-axis relative to the bounds
            // of the image
            if (mousePosition[0] - selectionOffset[0] > 0)
                if (mousePosition[0] - selectionOffset[0] + options.selectionWidth < $image.width())
                    options.selectionPosition[0] = mousePosition[0] - selectionOffset[0];
                else
                    options.selectionPosition[0] = $image.width() - options.selectionWidth;
            else
                options.selectionPosition[0] = 0;

            // Set the selection position on the y-axis relative to the bounds
            // of the image
            if (mousePosition[1] - selectionOffset[1] > 0)
                if (mousePosition[1] - selectionOffset[1] + options.selectionHeight < $image.height())
                    options.selectionPosition[1] = mousePosition[1] - selectionOffset[1];
                else
                    options.selectionPosition[1] = $image.height() - options.selectionHeight;
            else
                options.selectionPosition[1] = 0;

            // Trigger the 'onChange' event when the selection is changed
            options.onChange(getCropData());

            // Update only the needed elements of the plug-in interface
            // by specifying the sender of the current call
            updateInterface('moveSelection');
        };

        // Release the current selection
        function releaseSelection(event) {
            // Prevent the default action of the event
            event.preventDefault();

            // Prevent the event from being notified
            event.stopPropagation();

            // Unbind the event handler to the 'mousemove' event
            $(document).unbind('mousemove');

            // Unbind the event handler to the 'mouseup' event
            $(document).unbind('mouseup');

            // Update the selection origin
            selectionOrigin[0] = options.selectionPosition[0];
            selectionOrigin[1] = options.selectionPosition[1];

            // Reset the resize constraints
            resizeHorizontally = true;
            resizeVertically = true;

            // Verify if the selection size is bigger than the minimum accepted
            // and set the selection existence accordingly
            if (options.selectionWidth > options.minSelect[0] &&
                options.selectionHeight > options.minSelect[1])
                selectionExists = true;
            else
                selectionExists = false;

            // Trigger the 'onSelect' event when the selection is made
            options.onSelect(getCropData());


            // Update only the needed elements of the plug-in interface
            // by specifying the sender of the current call
            updateInterface('releaseSelection');
        };
    };

    $.fn.imageCrop = function(customOptions) {
        //Iterate over each object
        this.each(function() {
            var currentObject = this,
                image = new Image();

            // And attach imageCrop when the object is loaded
            image.onload = function() {
                $.imageCrop(currentObject, customOptions);
            };

            // Reset the src because cached images don't fire load sometimes
            image.src = currentObject.src;
        });

        // Unless the plug-in is returning an intrinsic value, always have the
        // function return the 'this' keyword to maintain chainability
        return this;
    };
})(jQuery);