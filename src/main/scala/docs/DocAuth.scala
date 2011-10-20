package docs

import com.google.gdata.client.docs._
import com.google.gdata.client.authn.oauth._
import unfiltered.Cookie

object DocAuth {
  val signer = new OAuthHmacSha1Signer()
  val helper = new GoogleOAuthHelper(signer)

  val CONSUMER_KEY = "448325728603.apps.googleusercontent.com"
  val CONSUMER_SECRET = "KslXJz4DZjLv-7_uYG-XewwD"

  def fetchToken = {
    val oauthParameters = createParams
    oauthParameters.setOAuthCallback("http://127.0.0.1:35920/authgranted")

    helper.getUnauthorizedRequestToken(oauthParameters)

    oauthParameters
  }

  def grabToken(queryString: String) = {
    val oauthParameters = createParams

    helper.getOAuthParametersFromCallback(queryString, oauthParameters)

    oauthParameters
  }

  def params(cookies: Map[String, Option[Cookie]]) = {
    val oAuthParams = createParams
    oAuthParams.setOAuthTokenSecret(cookies("secret").get.value)
    oAuthParams.setOAuthToken(cookies("token").get.value)
    oAuthParams
  }

  def createParams =  {
    val oauthParameters = new GoogleOAuthParameters()
    oauthParameters.setOAuthConsumerKey(CONSUMER_KEY)
    oauthParameters.setOAuthConsumerSecret(CONSUMER_SECRET)
    oauthParameters.setScope("https://spreadsheets.google.com/feeds")
    oauthParameters
  }
}