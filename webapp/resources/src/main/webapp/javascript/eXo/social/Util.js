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

;(function(window){
   var portal = window.eXo.env.portal,
       DEFAULT_REST_CONTEXT_NAME = 'rest-socialdemo',
       DEFAULT_PORTAL_NAME = 'classic',
       DEFAULT_PORTAL_CONTEXT = "/socialdemo",
       DEFAULT_ACCESSMODE = "public";
       
   window.eXo.social = window.eXo.social || {};
   window.eXo.social.portal = {
     rest : (portal.rest) ? portal.rest : DEFAULT_REST_CONTEXT_NAME,
     portalName : (portal.portalName) ? portal.portalName : DEFAULT_PORTAL_NAME,
     context : (portal.context) ? portal.context : DEFAULT_PORTAL_CONTEXT,
     accessMode: (portal.accessMode) ? portal.accessMode : DEFAULT_ACCESSMODE,
     userName : (portal.userName) ? portal.userName : ''
   };
})(window);

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


// User Profile Popup plugin
;(function($, document, window) {
  var template = null;
  var display = true;

  $.fn.showUserInfo = function(settings) {
  
    var defaultSettings = {
      styleSheetFilePath : "/social-resources/skin/social/webui/UIPopup/DefaultSkin.css",
      hasReload : false
    };
    
    settings = $.extend(defaultSettings, settings);
    
    $( function () {
        $('<style type="text/css">@import url("' + settings.styleSheetFilePath + '")</style>')
          .appendTo("head");
    
        buildTemplate();
    } );
  
    return $(this).on('mouseover', function(event) {
                  var userInfo = new UserInfo($(this));
                  var cache = window.eXo.social.Cache;
                  cache.resetIfNeeded();
                  userInfo.hasReload = settings.hasReload;
                  userInfo.cache = cache;
                  userInfo.init(event);
                  event.stopPropagation();
                }).on('mouseout', function(event) {
                  display = false;
                  window.setTimeout(function () {hideAllUserInfo(event);}, 500);
                  event.stopPropagation();
                });
  };

  // building template of popup
  function buildTemplate() {
    if( !template ) {
      template = $('<div class="UIUserInfoPopupContent"></div>')
        .append('<div class="Arrow"><span></span></div>')
        .append(
          $('<div class="ClearFix"></div>')
            .append('<div class="Avatar"></div>')
            .append(
              $('<div class="RightInfo"></div>')
                .append('<div class="FullName">FullName</div>')
                .append('<div class="Status">Status</div>')
                .append('<div><a class="More" href="javascript:void(0)">More<span class="LeftRowIcon"></span></a></div>')
            )
        )
        .append('<div class="Invite ClearFix">Invite to connect</div>');
    }
    return template;
  }
  
  // hide popup
  function hideAllUserInfo(event) {
    if(display === false) {
      var uiPopup = $(document.body).find('div.UIUserInfoPopup');
      uiPopup.find('div.UIUserInfoPopupContent').animate(
        {
          height:'10px',
          width: '10px',
          minWidth: '10px',
          top : '-20px',
          left : '-50px',
          opacity: 0.5
        }, 250, 'linear', function() {
          $(this).css({'opacity': 1, minWidth: '200px'});
          uiPopup.html('');
        }
      );
    }
  }
  
  window.eXo.social.Cache =  {
    max_size: 500,
    data : {},
    size : 0,
    flush : function () {
	    this.data = {};
	    this.size = 0;
    },
    add : function (query, results) {
	    if(this.size > this.max_size) {
	      this.flush();
	    }
	
	    if(!this.data[query]) {
	      this.size += 1;
	    }
	      
	    this.data[query] = results;
    },
    get : function (query) {
      return this.data[query];
    },
    resetIfNeeded : function() {
      var c_name = "isAlive";
      var c_value = true;
      
      if ( !this.getCookie(c_name) )  {
        this.flush();
        this.setCookie(c_name, c_value, 0.5);
      }
    },
    setCookie : function(c_name, value, exprInMinute) {
	    var c_value=escape(value) + ((exprInMinute==null) ? "" : "; expires=" + this.toExpresTime(exprInMinute) + ";");
	    document.cookie=c_name + "=" + c_value;
	  },
	  getCookie : function(c_name) {
	    var i,x,y,ARRcookies=document.cookie.split(";");
	    for (i=0;i<ARRcookies.length;i++) {
	      x=ARRcookies[i].substr(0,ARRcookies[i].indexOf("="));
	      y=ARRcookies[i].substr(ARRcookies[i].indexOf("=")+1);
	      x=x.replace(/^\s+|\s+$/g,"");
	      if (x==c_name) {
	        return unescape(y);
	      }
	    }
	  },
	  toExpresTime : function(numminutes) {
      var expiryDate = new Date();
      expiryDate.setTime(expiryDate.getTime() + (numminutes * 60 * 1000));
      return expiryDate.toGMTString();
    }
  };
  
  //
  function UserInfo(jelm) {
    var userInfo = {
        jElmInfo: $(jelm),
        container : $('<div class="UIUserInfoPopup"></div>'),
        userId : "",
        avatarURL : "",
        profileURL : "",
        status : "",
        hasReload: false,
        template : "",
        json : {fullName:"", activityTitle: "", avatarURL: "", relationshipType:""},
        restUrl : "",
        cache : {},
        displayInvite : true,
        init : function(event) {
          var portal = eXo.social.portal;
          userInfo.restUrl = 'http://' + window.location.host + portal.context + '/' + portal.rest 
                      + '/social/people' + '/getPeopleInfo/' + userInfo.getUserName() + '.json';
          
          userInfo.json = userInfo.cache.get( userInfo.userId );
          
          if ( !userInfo.json ) { 
		        $.ajax({
		            type: "GET",
		            url: userInfo.restUrl
		          }).complete( function(jqXHR) {
		            if(jqXHR.readyState === 4) {
		              userInfo.json = $.parseJSON(jqXHR.responseText);
		              userInfo.cache.add( userInfo.userId, userInfo.json );
		              if( userInfo.getRelationStatus() != 'NoInfo' ) {
                    userInfo.reBuildPopup(event);
                  } 
		            }
		          });
		      } else {
			      if( userInfo.getRelationStatus() != 'NoInfo' ) {
	            userInfo.reBuildPopup(event);
	          }
	        }
        },
        getUserName : function() {
          if( userInfo.userId.length == 0 ) {
            var userId = userInfo.jElmInfo.attr('rel');
            if(userId == null || userId == "") {
              userId = String(userInfo.getProfileURL());
              userId = userId.substring(userId.lastIndexOf('/')+1);
            }
            userInfo.userId = userId;
          }
          return userInfo.userId;
        },
        getFullName : function() {
          return userInfo.json.fullName;
        },
        getStatus : function () {
          return userInfo.json.activityTitle;
        },
        getProfileURL : function () {
          return userInfo.jElmInfo.attr('href');
        },
        getAvatarURL : function() {
          var avatar = userInfo.json.avatarURL;
          if( !avatar ) {
            avatar = '/social-resources/skin/ShareImages/Avatar.png';
          }
          return avatar;
        },
        getRelationStatus : function() {
          return userInfo.json.relationshipType;
        },
        inviteUser : function(event) {
          var action = $(this).attr('action');

          $.ajax({
            type: "GET",
            url: userInfo.restUrl + '?updatedType=' + action
          }).complete( function(jqXHR) {
            if(jqXHR.readyState === 4) {
              userInfo.json = $.parseJSON(jqXHR.responseText);
              if (userInfo.hasReload) {
                location.reload();
              }
            }
          });
          
          display = false;
          hideAllUserInfo(event);
        },
        reBuildPopup : function(event) {
          display = true;
          var template = buildTemplate();
          template.find('.FullName').html(userInfo.getFullName());
          template.find('.Status').html(userInfo.getStatus());
          template.find('.Avatar').html('<img src="' + userInfo.getAvatarURL() + '" />');
          template.find('.More').attr('href', userInfo.getProfileURL());          
          if(userInfo.getRelationStatus() != "NoAction") {
            // calculate action type.
            template.find('.Invite').show().html( function () {
                var relationStatus = userInfo.getRelationStatus();
                var action = '<span action="Invite" title="Invite">Invite</span>';
                //
                if (relationStatus == "pending") { // Viewing is not owner
                  action = '<span action="Accept" title="Accept" class="Accept">Accept</span>';
                  action += '<span class="VerticalSlash"> | </span>';
                  action += '<span action="Deny" title="Deny" class="Deny">Deny</span>';
                } else if (relationStatus == "waiting") { // Viewing is owner
                  action = '<span action="Revoke" title="Revoke" class="Action">Revoke</span>';
                } else if (relationStatus == "confirmed") { // Had Connection 
                  action = '<span action="Remove" title="Disconnect" class="Action">Disconnect</span>';
                } else if (relationStatus == "ignored") { // Connection is removed
                  action = '<span action="Deny" title="Deny" class="Action">Deny</span>';
                }
                return action;
              }
           ).find('span').off('click').on('click', userInfo.inviteUser);
          } else {
            template.find('.Invite').hide().html('');
          }
          
          var container = userInfo.jElmInfo.next();
          if(container.length == 0 || container.attr('id') != ('InfoMenuOf' + userInfo.userId)) {
            container = userInfo.container;
            container.attr('id', 'InfoMenuOf' + userInfo.userId).append(template);
            container.on('mouseenter', function(event) {
              display = true;
              event.stopPropagation();
            }).on('mouseleave', function(event) {
              display = false;
              window.setTimeout(function () { hideAllUserInfo(event);}, 500);
              event.stopPropagation();
            });
            container.css('float', userInfo.jElmInfo.css('float'));
            container.insertAfter(userInfo.jElmInfo);
          } else {
            // show existing popup
            container.append(template);
          }
          // set default size
          template.css({height:'auto', width: 'auto'});
          var w = template.width();
          var h = template.height();
          
          var Browser = eXo.core.Browser;
          var X = Browser.findMouseRelativeX(container, event, false);
          var Y = Browser.findMouseRelativeY(container, event);
          
          template.find('.Arrow:first').removeClass('Down Top').addClass(function(index, current) {
            var clazz = 'Top';
            template.css('top', -(34 + h - Y) + 'px');
            var delta = $(template).offset().top - $(document).scrollTop();
            var top = (delta < 0);
            if(top){
              $(this).css('top', '-12px');
              template.css('left', (X - 45) + 'px');
              template.css('top', (Y + 12) + 'px');
            } else {
              template.css('left', (X - 52) + 'px');
              $(this).css('top', (h+ 20) + 'px');
              clazz = 'Down';
            }
            
            return clazz;
          });

        }
    };
    
    return userInfo;
  }
  
})(jQuery, document, window);
