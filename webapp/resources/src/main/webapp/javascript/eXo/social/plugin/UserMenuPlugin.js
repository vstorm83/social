;(function($, document, window) {
  var template = null;
  var display = true;
  var JsonMap = {};

  // CSS 
  $(document).ready(
    function () {
      var cssCt = '.UIUserInfoMenu {visibility: visible; display:inline; float:none; position: relative; width: 1px; height: 1px;}\n'
                + '.UIUserInfoMenu .Arrow {height: 12px; z-index: 110; position: absolute; width: 20px; left: 42px;}\n'
                + '.UIUserInfoMenu .Top {background: url("/social-resources/skin/ShareImages/ArrowTop.png") no-repeat;  top: -12px;}\n'
                + '.UIUserInfoMenu .Down {background: url("/social-resources/skin/ShareImages/ArrowDown.png") no-repeat;  top: -12px;}\n'
                + '.UIUserInfoMenu .UIInfoContent {position: absolute; left: -45px; top: 22px; background: #fff; font-size:11px; padding:10px; box-shadow: -1px 2px 3px #D5D5D5; border-radius: 8px 8px 8px 8px; min-width:200px; max-width: 230px; z-index: 100;}\n'
                + '.UIInfoContent .Avatar {padding: 2px; width: auto; height: auto; border: solid 1px #e6e6e6; border-radius:5px; float:left; text-align: center;}\n'
                + '.UIInfoContent .RightInfo {margin-left: 80px; line-height: 16px;}\n'
                + '.UIInfoContent .FullName {font-weight: bold; color: #ff8a00; font-size: 11px; white-space:nowrap;}\n'
                + '.UIInfoContent .Status {color: #222222; font-size: 10px;}\n'
                + '.UIInfoContent .RightInfo .More {text-decoration: none; color: #ff8a00 !important; font-size: 10px; white-space:nowrap;}\n'
                + '.UIInfoContent .RightInfo .More:hover {text-decoration: underline;}\n'
                + '.UIInfoContent .LeftRowIcon {margin: 0 5px; padding: 0 3.5px; background: url("/social-resources/skin/ShareImages/ArowRight.png") no-repeat center;}\n'
                + '.UIInfoContent .Invite {font-weight: normal;color: black; font-size: 11px;background: #fff; line-height: 24px; text-align: center; margin: 6px auto; max-width:150px; min-width:100px; border:solid 1px gray; cursor:pointer;}\n'
                + '.UIInfoContent .Invite:hover {background: #ededed;}\n';
      $(document.head).append('<style type="text/css" id="StyleUserInfo">\n' + cssCt + '</style>');
      buildTemplate();
  });

  // building template of menu
  function buildTemplate() {
    if(template === null) {
      template = $('<div class="UIInfoContent"></div>')
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
  // hidden all menus
  function hideAllUserInfo(event) {
    if(display === false) {
      var allMenu = $(document.body).find('div.UIUserInfoMenu');
      allMenu.find('div.UIInfoContent').animate(
        {
          height:'10px',
          width: '10px',
          minWidth: '10px',
          top : '-20px',
          left : '-50px',
          opacity: 0.5
        }, 250, 'linear', function() {
          $(this).css({'opacity': 1, minWidth: '200px'});
          allMenu.html('');
        }
      );
    }
  }

  //
  function UserInfo(jelm) {
    var userInfo = {
        jElmInfo: $(jelm),
        container : $('<div class="UIUserInfoMenu"></div>'),
        userId : "",
        avatarURL : "",
        profileURL : "",
        status : "",
        template : "",
        json : {fullName:"", activityTitle: "", avatarURL: "", relationshipType:""},
        restUrl : "",
        displayInvite : true,
        init : function(event) {
          var portal = eXo.social.portal;
          userInfo.restUrl = 'http://' + window.location.host + portal.context + '/' + portal.rest 
                      + '/social/people' + '/getPeopleInfo/' + userInfo.getUserName() + '.json';

          var json = JsonMap[userInfo.userId];
          if(json == null) {
            $.ajax({
            type: "GET",
            url: userInfo.restUrl,
            complete: function(jqXHR) {
              if(jqXHR.readyState === 4) {
                userInfo.json = $.parseJSON(jqXHR.responseText);
                JsonMap[userInfo.userId] = userInfo.json;
                if(userInfo.getRelationStatus() != 'NoInfo' && userInfo.getRelationStatus() != 'null') {
                  userInfo.buildMenu(event);
                }
              }
            }
           });
          } else {
            userInfo.json = json;
            if(userInfo.getRelationStatus() != 'NoInfo' && userInfo.getRelationStatus() != 'null') {
              userInfo.buildMenu(event);
            }
          }
        },
        getUserName : function() {
          if(userInfo.userId === "") {
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
          if(avatar == null || avatar == '' || avatar == 'null') {
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
            url: userInfo.restUrl + '?updatedType=' + action,
            complete: function(jqXHR) {
              if(jqXHR.readyState === 4) {
                userInfo.json = $.parseJSON(jqXHR.responseText);
                JsonMap[userInfo.userId] = userInfo.json;
              }
            }
           });
          display = false;
          hideAllUserInfo(event);
        },
        buildMenu : function(event) {
          display = true;
          var template = buildTemplate();
          template.find('.FullName').html(userInfo.getFullName());
          template.find('.Status').html(userInfo.getStatus());
          template.find('.Avatar').html('<img style="height:63px; width:63px;" src="' + userInfo.getAvatarURL() + '" />');
          template.find('.More').attr('href', userInfo.getProfileURL());          
          if(userInfo.getRelationStatus() != "NoAction") {
            // calculate action type.
            template.find('.Invite').show().html( function () {
                var relationStatus = userInfo.getRelationStatus();
                var action = '<span action="Invite" title="Invite">Invite</span>';
                //
                if (relationStatus == "pending") { // Viewing is not owner
                  action = '<span action="Accept" title="Accept" style="width: 45%; display: block; float:left;text-align:right;">Accept</span>';
                  action += '<span style="width: 10%; display: block; float:left;"> | </span>';
                  action += '<span action="Deny" title="Deny" style="width: 45%; display: block;float:left;text-align:left;">Deny</span>';
                } else if (relationStatus == "waiting") { // Viewing is owner
                  action = '<span action="Revoke" title="Revoke" style="width: 100%; display: block;">Revoke</span>';
                } else if (relationStatus == "confirmed") { // Had Connection 
                  action = '<span action="Remove" title="Disconnect" style="width: 100%; display: block;">Disconnect</span>';
                } else if (relationStatus == "ignored") { // Connection is removed
                  action = '<span action="Deny" title="Deny" style="width: 100%; display: block;">Deny</span>';
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
            // show menu existing
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

          //template.css({height:'0px', width: '0px'});
          //template.animate({height: h + 'px', width: w + 'px'}, 80);
        }
    };
    return userInfo;
  }
  
  
  $.fn.showUserInfo = function() {
    $(this).on('mouseover', function(event) {
      var userInfo = new UserInfo($(this));
      userInfo.init(event);
      event.stopPropagation();
    }).on('mouseout', function(event) {
      display = false;
      window.setTimeout(function () {hideAllUserInfo(event);}, 500);
      event.stopPropagation();
    });
    return $(this);
  };
  
})(jQuery, document, window);
