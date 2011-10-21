package com.example

import unfiltered.request._
import unfiltered.response._
import unfiltered.Cookie

import org.clapper.avsl.Logger
import com.google.gdata.client.docs.DocsService
import collection.JavaConversions._
import com.google.gdata.data.spreadsheet._
import com.google.gdata.data.TextContent
import xml.{Elem, XML}

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
      Ok ~> Html(<ul> { feed.getEntries map { entry => <li>
        <a href={ "/ss/" + entry.getKey }>{ entry.getTitle.getPlainText}</a>
      </li> } } </ul>)
    }
    case GET(Path(Seg("ss" :: key :: Nil)) & Cookies(cookies)) => {
      val service = new DocsService("open newslist hack")
      service.setOAuthCredentials(docs.DocAuth.params(cookies), docs.DocAuth.signer)
      val metafeedUrl = new java.net.URL("https://spreadsheets.google.com/feeds/worksheets/%s/private/full" format (key))
      val feed = service.getFeed(metafeedUrl, classOf[WorksheetFeed])
      Ok ~> Html(<ul> { feed.getEntries map { entry =>
        <li>
          { entry.getTitle.getPlainText }
          { renderSheet(entry, service) }
        </li> } }
      </ul>)
    }
  }
  def renderSheet(sheet: WorksheetEntry, service: DocsService) = {
    val listFeedUrl = sheet.getCellFeedUrl
    val feed = service.getFeed(listFeedUrl, classOf[CellFeed])
//    println((feed.getEntries filter {getCol(_) < 4}).size)
    val interestingCols = feed.getEntries filter {col(_) < 4}
    val rows = interestingCols.groupBy(row)
    for (rowNum <- rows.keys.toSeq.sorted) {

    }

    <ul> { (feed.getEntries filter {x => (col(x) < 4) && (row(x) > 2)}) map  { entry =>
      <li> { entry.getTitle.getPlainText }
        </li>
      <li>{entry.getXmlBlob.getBlob}
        </li><li>
        { linkFrom(inputValue(entry)) } </li>
      <li> {XML.loadString(entry.getXmlBlob.getBlob).text} </li>
    } } </ul>
//    for (ListEntry entry : feed.getEntries()) {
//    System.out.println(entry.getTitle().getPlainText());
//    for (String tag : entry.getCustomElements().getTags()) {
//      System.out.println("  <gsx:" + tag + ">" +
//        entry.getCustomElements().getValue(tag) + "</gsx:" + tag + ">");
//    }
  }
  def col(x: CellEntry) = attributeAsInt(x, "col")
  def row(x: CellEntry) = attributeAsInt(x, "row")
  def attributeAsInt(x: CellEntry, attributeName: String) = {
//      println(XML.loadString(x.getXmlBlob.getBlob).attribute("col").get.head.text.toInt)
    (XML.loadString(x.getXmlBlob.getBlob).attribute(attributeName)).get.head.text.toInt
  }

  def inputValue(x: CellEntry): String = XML.loadString(x.getXmlBlob.getBlob).attribute("inputValue") map (_.head.text) getOrElse("")
  def linkFrom(hyperlinkText: String) = {
    println(hyperlinkText)
    val hyperLink = """=hyperlink\("(.*?)"; "(.*?)"\)""".r
    hyperlinkText match {
      case hyperLink(url, text) => Some((url, text))
      case _ => None
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
