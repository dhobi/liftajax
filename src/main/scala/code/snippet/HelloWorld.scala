package code
package snippet

import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmds
import net.liftweb.util.Helpers._

class HelloWorld {

  // replace the contents of the element with id "time" with the date
  def howdy = {

    def timoutFunc = {
      //Timeout after 5 seconds --> have a look at Boot.scala
      Thread.sleep(10000)
      JsCmds.Noop
    }

    def errorFunc = {
      Thread.sleep(2000)
      throw new RuntimeException("Something unexpected happened")
      JsCmds.Alert("You will never see this alert.")
    }

    "#timeoutButton" #> SHtml.ajaxButton("Get a timeout", () => timoutFunc) &
    "#errorButton" #> SHtml.ajaxButton("Get an error", () => errorFunc)
  }
}

