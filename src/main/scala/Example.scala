package com.example

import unfiltered.request._
import unfiltered.response._
import unfiltered.Cookie

import org.clapper.avsl.Logger
import com.google.gdata.client.docs.DocsService
import com.google.gdata.data.spreadsheet.SpreadsheetFeed
import collection.JavaConversions._

/** unfiltered plan */
class App extends unfiltered.filter.Plan {
  import QParams._

  val logger = Logger(classOf[App])

  def intent = {
    case GET(Path("/auth")) => {
      val params = docs.DocAuth.fetchToken
      val authURL = docs.DocAuth.helper.createUserAuthorizationUrl(params)
      ResponseCookies(Cookie("secret", params.getOAuthTokenSecret)) ~> Redirect(authURL)
    }
    case GET(Path("/authgranted") & QueryString(query) & Cookies(cookies)) => {
      val params = docs.DocAuth.grabToken(query)
      params.setOAuthTokenSecret(cookies("secret").get.value)
      docs.DocAuth.helper.getAccessToken(params)
      ResponseCookies(Cookie("token", params.getOAuthToken)) ~>
        ResponseCookies(Cookie("secret", params.getOAuthTokenSecret)) ~>
        Redirect("/list")
    }
    case GET(Path("/list") & Cookies(cookies)) => {
      val metafeedUrl = new java.net.URL("https://spreadsheets.google.com/feeds/spreadsheets/private/full")
      val service = new DocsService("open newslist hack")
      service.setOAuthCredentials(docs.DocAuth.params(cookies), docs.DocAuth.signer)
      val feed = service.getFeed(metafeedUrl, classOf[SpreadsheetFeed])
      Ok ~> Html(<ul> { feed.getEntries map { entry => <li>{ entry.getTitle.getPlainText} </li> } } </ul>)
    }
  }
}

/** embedded server */
object Server {
  val logger = Logger(Server.getClass)
  def main(args: Array[String]) {
    val http = unfiltered.jetty.Http(35920) // this will not be necessary in 0.4.0
    http.context("/assets") { _.resources(new java.net.URL(getClass().getResource("/www/css"), ".")) }
      .filter(new App).run({ svr =>
//        unfiltered.util.Browser.open(http.url)
      }, { svr =>
        logger.info("shutting down server")
      })
  }
}
