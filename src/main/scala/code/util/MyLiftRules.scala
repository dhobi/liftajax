package code.util

import net.liftweb.common.{Empty, Box}
import net.liftweb.http.js.JsCmd

/**
 * "Extend" net.liftweb.http.LiftRules
 */
object MyLiftRules {
  var ajaxTimeoutFailure : Box[() => JsCmd] = Empty

  var ajaxSpecificFailure : List[(Int,() => JsCmd)] = List()
}
