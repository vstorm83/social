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
 * UIActivity.js
 */
(function ($, _) {  
var UIActivity = {
  COMMENT_BLOCK_BOUND_CLASS_NAME : "commentBox commentBlockBound ",
  COMMENT_BLOCK_BOUND_NONE_CLASS_NAME : " commentBox commentBlockBoundNone",
  DEFAULT_COMMENT_TEXT_AREA_HEIGHT : "28px",
  FOCUS_COMMENT_TEXT_AREA_HEIGHT : "28px",
  FOCUS_COMMENT_TEXT_AREA_COLOR : "#999999",
  DEFAULT_COMMENT_TEXT_AREA_COLOR : "#808080",
  onLoad: function (params) {
    UIActivity.configure(params);
    UIActivity.init();
  },
  configure: function(params) {
    UIActivity.activityId = params.activityId || null;
    UIActivity.inputWriteAComment = params.inputWriteAComment || "";
    UIActivity.commentMinCharactersAllowed = params.commentMinCharactersAllowed || 0;
    UIActivity.commentMaxCharactersAllowed = params.commentMaxCharactersAllowed || 0;
    UIActivity.commentFormDisplayed = params.commentFormDisplayed == "true" ? true : false || false;
    UIActivity.allCommentsDisplayed = params.allCommentsDisplayed = "true" ? true : false || false;
    UIActivity.commentFormFocused = params.commentFormFocused = "true" ? true : false  || false;

    if (UIActivity.activityId == null) {
      alert('err: activityId is null!');
      return;
    }
    UIActivity.commentLinkId = 'CommentLink' + UIActivity.activityId;
    //UIActivity.likeLinkId = 'LikeLink'  + UIActivity.activityId;
    UIActivity.commentFormBlockId = 'CommentFormBlock' + UIActivity.activityId;
    UIActivity.commentTextareId = 'CommentTextarea' + UIActivity.activityId;
    UIActivity.commentButtonId = 'CommentButton' + UIActivity.activityId;
    UIActivity.deleteCommentButtonIds = [];
    UIActivity.contentBoxId = 'ContextBox' + UIActivity.activityId;
    UIActivity.deleteActivityButtonId = 'DeleteActivityButton' + UIActivity.activityId;
    UIActivity.allCommentSize = parseInt(params.allCommentSize);
    UIActivity.commentBlockBoundId = "CommentBlockBound" + UIActivity.activityId;
    UIActivity.commentBlockIds = [];
    UIActivity.activityContextBoxId = "ActivityContextBox" + UIActivity.activityId;
    if (UIActivity.allCommentSize > 0) {
      for (var i = 1; i <= UIActivity.allCommentSize; i++) {
        UIActivity.deleteCommentButtonIds[i - 1] = "DeleteCommentButton" + UIActivity.activityId + i;
        UIActivity.commentBlockIds[i - 1] = "CommentBlock" + UIActivity.activityId + i;
      }
    }
  },
  init: function() {
    UIActivity.commentLinkEl = $("#"+UIActivity.commentLinkId);
    UIActivity.commentFormBlockEl = $("#" + UIActivity.commentFormBlockId);
    UIActivity.commentTextareaEl = $("#" + UIActivity.commentTextareId);
    UIActivity.commentButtonEl = $("#" + UIActivity.commentButtonId).show();
    UIActivity.deleteCommentButtonEls = [];
    UIActivity.contentBoxEl = $(UIActivity.contentBoxId);
    UIActivity.deleteActivityButtonEl = $("#" + UIActivity.deleteActivityButtonId);
    UIActivity.commentBlockBoundEl = $("#" + UIActivity.commentBlockBoundId);
    UIActivity.inputContainer = $("#InputContainer" + UIActivity.activityId);
    UIActivity.commentBlockEls = [];
    UIActivity.activityContextBoxEl = $("#" + UIActivity.activityContextBoxId);
    if(UIActivity.allCommentSize > 0) {
      for(var i=0; i < UIActivity.allCommentSize; i++) {
        UIActivity.deleteCommentButtonEls[i] = $("#" + UIActivity.deleteCommentButtonIds[i]);
        UIActivity.commentBlockEls[i] = $("#" + UIActivity.commentBlockIds[i]);
      }
    }
    
    if (!(UIActivity.commentFormBlockEl && UIActivity.commentTextareaEl && UIActivity.commentButtonEl)) {
      alert('err: init UIActivity!');
    }
    
    var commentLinkEl = $("#" + UIActivity.commentLinkId);
    if (commentLinkEl.length > 0) {
      commentLinkEl.off('click').on('click', function (evt) {
        var currentActivityId = $(this).attr('id').replace('CommentLink', '');
        var inputContainer = $('#InputContainer' + currentActivityId).fadeToggle('fast', function () {
          var thiz = $(this);
          if(thiz.css('display') === 'block') {
            var blockInput = thiz.parents('.uiActivityStream:first').find('.inputContainerShow');
            if(blockInput.length > 0) {
              blockInput.removeClass('inputContainerShow').hide();
            }
            thiz.addClass('inputContainerShow');
            thiz.find('div.replaceTextArea:first').focus();

            var ctTop = ($(window).height()- thiz.height())/2;
            var nTop = thiz.offset().top - ctTop - 20;
            nTop = (nTop > 0) ? nTop : 0;
            
            $('html, body').animate({scrollTop:nTop}, 'slow');
          } else {
            thiz.removeClass('inputContainerShow')
          }
        });
      });
    }
    
	  //
    $('textarea#CommentTextarea' + UIActivity.activityId).exoMentions({
        onDataRequest:function (mode, query, callback) {
          var url = window.location.protocol + '//' + window.location.host + '/' + eXo.social.portal.rest + '/social/people/getprofile/data.json?search='+query;
          $.getJSON(url, function(responseData) {
            responseData = _.filter(responseData, function(item) { 
              return item.name.toLowerCase().indexOf(query.toLowerCase()) > -1;
            });
            callback.call(this, responseData);
          });
        },
        idAction : ('CommentButton'+UIActivity.activityId),
        elasticStyle : {
          maxHeight : '52px',
          minHeight : '22px',
          marginButton: '4px',
          enableMargin: false
        },
        messages : window.eXo.social.I18n.mentions
    });
  
    var actionDeletes = $('a.controllDelete');
    if (actionDeletes.length > 0) {
      actionDeletes.off('click').on('click',
				function(e) {
					$('.currentDeleteActivity:first').removeClass('currentDeleteActivity');
					var jElm = $(this);
					jElm.addClass('currentDeleteActivity');
					var id = jElm.attr('id');
					if(id == null || id.length == 0) {
					  $('#SocialCurrentConfirm').removeAttr('id');
						id = "SocialCurrentConfirm";
						jElm.attr('id', id)
					}
					var confirmText = jElm.attr('data-confirm');
					eXo.social.PopupConfirmation.confirm(id, [{action: UIActivity.removeActivity, label : 'OK'}], 'Confirmation', confirmText, 'Close');
				}
			);
    }
    
    //
    
    function hideMoreBtn() {
      var contentBoxEl = $('#'+UIActivity.contentBoxId);
      var listLiked = $(contentBoxEl).find('.listLiked');
      var moreBtn = listLiked.find('.btn').hide();
      moreBtn.hide();
    }
    
    function reset() {
      var contentBoxEl = $('#'+UIActivity.contentBoxId);
      var listLiked = $(contentBoxEl).find('.listLiked');
      if (listLiked.length == 0) {
        listLiked = $(contentBoxEl).find('.listLikedBox'); 
      }
      var moreBtn = listLiked.find('.btn').hide();
      var listLikedWidth = listLiked.outerWidth();
      
      //
      var elWidth = listLiked.find('a:first-child').outerWidth() + 8;
      var displayedNum = Math.floor(listLikedWidth/elWidth) - 1;
      
      //
      var likedEl = listLiked.find('a');
      var hasMore = (displayedNum < likedEl.length);
      
      likedEl.hide();
      $.each(likedEl, function(idx, el) {
        if (idx < displayedNum) {
          $(el).show();
        }
      });
      
      if (hasMore) {
        moreBtn.show();
      }
    }
    
    hideMoreBtn();
    
    $(window).load(function() {
      reset();
    });
    
    // process with like list
    $(window).resize(function() {
      reset();
    });
    
	},
	loadLikes : function () {
	    var contentBoxEl = $('#'+UIActivity.contentBoxId);
      var listLiked = $(contentBoxEl).find('.listLiked').find('a').show();
      UIActivity.isLoadLike = true;
	},
	removeActivity : function () {
		var jElm = $('.currentDeleteActivity:first');
		var idElm = jElm.attr('id');
		jElm.removeClass('currentDeleteActivity');
		if (idElm.indexOf('Activity') > 0) { // remove activity
			var idActivty = idElm.replace('DeleteActivityButton', '')
			$('#activityContainer' + idActivty).css('overflow', 'hidden').animate(
				{
					height : '1px',
					opacity : '0'
				}, 500,
				function() {
					$(this).removeClass('activityStream');
					window.eval(jElm.attr('data-delete').replace('javascript:', ''));
				});
		} else if (idElm.indexOf('Comment') > 0) { // remove comment
			var idComment = idElm.replace('DeleteCommentButton', '')
			$('#commentContainer' + idComment).css('overflow', 'hidden')
			.animate(
				{
					height : '1px',
					opacity : '0.1'
				}, 300,
				function() {
					$(this).hide();
					window.eval(jElm.attr('data-delete').replace('javascript:', ''));
				}
			);
		}
	},
	
	replyByURL : function(activityId) {
	  $(document).ready( function() {
  	    var actionComment = '#CommentLink' + activityId;
  	    var cmAction = $(actionComment);
  	    if(cmAction.length > 0) {
  	      cmAction.trigger('click');
  	    }
  	  }
	  );
	},

	setPageTitle : function(activityTitle) {
		$(document).attr('title', 'Activity: ' + activityTitle);
	},
	
	loadLikersByURL : function() {
    $(document).ready( function() {
      var contentBoxEl = $('#'+UIActivity.contentBoxId);
      var listLiked = $(contentBoxEl).find('.listLiked');
      listLiked.find('.btn').trigger('click');
      }
    );
  }
};

return UIActivity;
})($, mentions._);
