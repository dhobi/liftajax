package code.util

import net.liftweb.common.Full
import net.liftweb.http.js.{JsCmd, JE, AjaxInfo, JsCmds}
import net.liftweb.http.LiftRules

/**
 * Provide own implementation of net.liftweb.http.js.ScriptRenderer
 */
object MyScriptRenderer {

  private def helper = {
    if(MyLiftRules.ajaxSpecificFailure.nonEmpty) {
      """switch(statusCode) {""" +
        (MyLiftRules.ajaxSpecificFailure.map {
          case ((statusCode, jsFunc)) => {
            """case """+statusCode+""":
            """+jsFunc().toJsCmd+"""
          break;
          """
          }
        }).mkString +
        """default:
      liftAjax.lift_defaultFailure();
      }"""
    } else {
      """liftAjax.lift_defaultFailure();"""
    }
  }

  def ajaxScript = JsCmds.Run("""

(function() {

  window.liftAjax = {
    lift_ajaxQueue: [],
    lift_ajaxInProcess: null,
    lift_ajaxShowing: false,
    lift_ajaxRetryCount: """ + (LiftRules.ajaxRetryCount openOr 3) + """,

    lift_ajaxHandler: function(theData, theSuccess, theFailure, responseType){
	  var toSend = {retryCnt: 0};
	  toSend.when = (new Date()).getTime();
	  toSend.theData = theData;
	  toSend.onSuccess = theSuccess;
	  toSend.onFailure = theFailure;
	  toSend.responseType = responseType;

	  liftAjax.lift_ajaxQueue.push(toSend);
	  liftAjax.lift_ajaxQueueSort();
	  liftAjax.lift_doAjaxCycle();
	  return false; // buttons in forms don't trigger the form

    },

    lift_uriSuffix: undefined,

    lift_ajaxQueueSort: function() {
      liftAjax.lift_ajaxQueue.sort(function (a, b) {return a.when - b.when;});
    },

    lift_defaultFailure: function() {
      """ + (LiftRules.ajaxDefaultFailure.map(_().toJsCmd) openOr "") + """
    },
    lift_timeoutFailure: function() {
      """ + (MyLiftRules.ajaxTimeoutFailure.map(_().toJsCmd) openOr "") + """
    },
    lift_specificFailure: function(statusCode) {
      """+helper+"""
    },
    lift_startAjax: function() {
      liftAjax.lift_ajaxShowing = true;
      """ + (LiftRules.ajaxStart.map(_().toJsCmd) openOr "") + """
    },

    lift_endAjax: function() {
      liftAjax.lift_ajaxShowing = false;
      """ + (LiftRules.ajaxEnd.map(_().toJsCmd) openOr "") + """
    },

    lift_testAndShowAjax: function() {
      if (liftAjax.lift_ajaxShowing && liftAjax.lift_ajaxQueue.length == 0 && liftAjax.lift_ajaxInProcess == null) {
        liftAjax.lift_endAjax();
      } else if (!liftAjax.lift_ajaxShowing && (liftAjax.lift_ajaxQueue.length > 0 || liftAjax.lift_ajaxInProcess != null)) {
        liftAjax.lift_startAjax();
      }
    },

    lift_traverseAndCall: function(node, func) {
      if (node.nodeType == 1) func(node);
      var i = 0;
      var cn = node.childNodes;

      for (i = 0; i < cn.length; i++) {
        liftAjax.lift_traverseAndCall(cn.item(i), func);
      }
    },

    lift_successRegisterGC: function() {
      setTimeout("liftAjax.lift_registerGC()", """ + LiftRules.liftGCPollingInterval + """);
    },

    lift_failRegisterGC: function() {
      setTimeout("liftAjax.lift_registerGC()", """ + LiftRules.liftGCFailureRetryTimeout + """);
    },

    lift_registerGC: function() {
      var data = "__lift__GC=_"
      """ + LiftRules.jsArtifacts.ajax(AjaxInfo(JE.JsRaw("data"),
    "POST",
    LiftRules.ajaxPostTimeout,
    false, "script",
    Full("liftAjax.lift_successRegisterGC"), Full("liftAjax.lift_failRegisterGC"))) +
    """
       },

       lift_doAjaxCycle: function() {
         var queue = liftAjax.lift_ajaxQueue;
         if (queue.length > 0) {
           var now = (new Date()).getTime();
           if (liftAjax.lift_ajaxInProcess == null && queue[0].when <= now) {
             var aboutToSend = queue.shift();

             liftAjax.lift_ajaxInProcess = aboutToSend;

             var successFunc = function(data) {
               liftAjax.lift_ajaxInProcess = null;
               if (aboutToSend.onSuccess) {
                 aboutToSend.onSuccess(data);
               }
               liftAjax.lift_doAjaxCycle();
             };

             var failureFunc = function(xhr, textStatus) {
               liftAjax.lift_ajaxInProcess = null;
               var cnt = aboutToSend.retryCnt;
               if (cnt < liftAjax.lift_ajaxRetryCount) {
               aboutToSend.retryCnt = cnt + 1;
                 var now = (new Date()).getTime();
                 aboutToSend.when = now + (1000 * Math.pow(2, cnt));
                 queue.push(aboutToSend);
                 liftAjax.lift_ajaxQueueSort();
               } else {
                 if (aboutToSend.onFailure) {
                   aboutToSend.onFailure();
                 } else {
                   if(textStatus == "timeout" || textStatus == "abort") {
                      liftAjax.lift_timeoutFailure();
                   } else {
                      liftAjax.lift_specificFailure(xhr.status);
                   }
                 }
               }
               liftAjax.lift_doAjaxCycle();
             };

             if (aboutToSend.responseType != undefined &&
                 aboutToSend.responseType != null &&
                 aboutToSend.responseType.toLowerCase() === "json") {
               liftAjax.lift_actualJSONCall(aboutToSend.theData, successFunc, failureFunc);
             } else {
               var theData = aboutToSend.theData;
               if (liftAjax.lift_uriSuffix) {
                 theData += '&' + liftAjax.lift_uriSuffix;
                 liftAjax.lift_uriSuffix = undefined;
               }
               liftAjax.lift_actualAjaxCall(theData, successFunc, failureFunc);
             }
            }
         }

         liftAjax.lift_testAndShowAjax();
         setTimeout("liftAjax.lift_doAjaxCycle();", 200);
       },

       addPageName: function(url) {
         return """ + {
    if (LiftRules.enableLiftGC) {
      "url.replace('" + LiftRules.ajaxPath + "', '" + LiftRules.ajaxPath + "/'+lift_page);"
    } else {
      "url;"
    }
  } + """
    },

    lift_actualAjaxCall: function(data, onSuccess, onFailure) {
      """ +
    LiftRules.jsArtifacts.ajax(AjaxInfo(JE.JsRaw("data"),
      "POST",
      LiftRules.ajaxPostTimeout,
      false, "script",
      Full("onSuccess"), Full("onFailure"))) +
    """
        },

        lift_actualJSONCall: function(data, onSuccess, onFailure) {
    """ +
    LiftRules.jsArtifacts.ajax(AjaxInfo(JE.JsRaw("data"),
      "POST",
      LiftRules.ajaxPostTimeout,
      false, "json",
      Full("onSuccess"), Full("onFailure"))) +
    """
              }
            };

            window.liftUtils = {
              lift_blurIfReturn: function(e) {
                var code;
                if (!e) var e = window.event;
                if (e.keyCode) code = e.keyCode;
                else if (e.which) code = e.which;

                var targ;

                if (e.target) targ = e.target;
                else if (e.srcElement) targ = e.srcElement;
                if (targ.nodeType == 3) // defeat Safari bug
                  targ = targ.parentNode;
                if (code == 13) {targ.blur(); return false;} else {return true;};
              }
            };


          })();
    """ + LiftRules.jsArtifacts.onLoad(new JsCmd() {def toJsCmd = "liftAjax.lift_doAjaxCycle()"}).toJsCmd)

}
