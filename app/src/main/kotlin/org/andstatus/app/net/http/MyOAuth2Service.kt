package org.andstatus.app.net.http

import com.github.scribejava.core.builder.api.DefaultApi20
import com.github.scribejava.core.httpclient.HttpClient
import com.github.scribejava.core.httpclient.HttpClientConfig
import com.github.scribejava.core.model.OAuthConstants
import com.github.scribejava.core.model.OAuthRequest
import com.github.scribejava.core.oauth.AccessTokenRequestParams
import com.github.scribejava.core.oauth.OAuth20Service
import java.io.OutputStream

class MyOAuth2Service(
    api: DefaultApi20?, apiKey: String?, apiSecret: String?, callback: String?, defaultScope: String?,
    responseType: String?, debugStream: OutputStream?, userAgent: String?,
    httpClientConfig: HttpClientConfig?, httpClient: HttpClient?
) : OAuth20Service(
    api, apiKey, apiSecret, callback, defaultScope, responseType, debugStream, userAgent,
    httpClientConfig, httpClient
) {
    override fun createAccessTokenRequest(params: AccessTokenRequestParams?): OAuthRequest {
        return super.createAccessTokenRequest(params).apply {
            // Required as per https://www.rfc-editor.org/rfc/rfc6749#section-4.1.3
            addParameter(OAuthConstants.CLIENT_ID, apiKey)
        }
    }

    override fun createRefreshTokenRequest(refreshToken: String?, scope: String?): OAuthRequest {
        return super.createRefreshTokenRequest(refreshToken, scope).apply {
            // https://www.rfc-editor.org/rfc/rfc6749#section-3.2.1
            // "A client MAY use the "client_id" request parameter to identify itself
            //   when sending requests to the token endpoint..."
            addParameter(OAuthConstants.CLIENT_ID, apiKey)
        }
    }
}
