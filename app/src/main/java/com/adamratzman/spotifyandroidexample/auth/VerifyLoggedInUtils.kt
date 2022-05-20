package com.adamratzman.spotifyandroidexample.auth

import android.app.Activity
import android.util.Log
import com.adamratzman.spotify.SpotifyClientApi
import com.adamratzman.spotify.SpotifyException
import com.adamratzman.spotify.SpotifyScope
import com.adamratzman.spotify.auth.SpotifyDefaultCredentialStore
import com.adamratzman.spotify.auth.implicit.startSpotifyImplicitLoginActivity
import com.adamratzman.spotify.auth.pkce.startSpotifyClientPkceLoginActivity
import com.adamratzman.spotifyandroidexample.data.Model
import kotlinx.coroutines.runBlocking

fun <T> Activity.guardValidSpotifyApi(
    classBackTo: Class<out Activity>,
    alreadyTriedToReauthenticate: Boolean = false,
    block: suspend (api: SpotifyClientApi) -> T
): T? {
    return runBlocking {
        try {
            Log.d("XXX", "Guard(1)")
            val token = Model.credentialStore.spotifyToken
                ?: throw SpotifyException.ReAuthenticationNeededException()
            Log.d("XXX", "Guard(2)")
            val usesPkceAuth = token.refreshToken != null
            val api = (if (usesPkceAuth) Model.credentialStore.getSpotifyClientPkceApi()
            else Model.credentialStore.getSpotifyImplicitGrantApi())
                ?: throw SpotifyException.ReAuthenticationNeededException()
            Log.d("XXX", "Guard(3)")
            Log.d(
                "XXX", "Guard(3) Scope hasReadPlaylist:" +
                        "${api.hasScope(SpotifyScope.PLAYLIST_READ_PRIVATE)}"
            )

            block(api)
        } catch (e: SpotifyException) {
            e.printStackTrace()
            val usesPkceAuth = Model.credentialStore.spotifyRefreshToken != null
            if (usesPkceAuth) {
                Log.d("XXX", "Guard(4)")
                val api = Model.credentialStore.getSpotifyClientPkceApi()!!
                Log.d("XXX", "Guard(5)")
                Log.d(
                    "XXX", "Guard(5) Scope hasReadPlaylist:" +
                            "${api.hasScope(SpotifyScope.PLAYLIST_READ_PRIVATE)}"
                )
                if (!alreadyTriedToReauthenticate) {
                    try {
                        Log.d("XXX", "Guard(6)")
                        api.refreshToken()
                        Model.credentialStore.spotifyToken = api.token
                        Log.d("XXX", "Guard(7)")
                        Log.d(
                            "XXX", "Guard(7) Scope hasReadPlaylist:" +
                                    "${api.hasScope(SpotifyScope.PLAYLIST_READ_PRIVATE)}"
                        )
                        block(api)
                    } catch (e: SpotifyException.ReAuthenticationNeededException) {
                        Log.d("XXX", "Guard(8)")
                        e.printStackTrace()
                        return@runBlocking guardValidSpotifyApi(
                            classBackTo = classBackTo,
                            alreadyTriedToReauthenticate = true,
                            block = block
                        )
                    } catch (e: IllegalArgumentException) {
                        Log.d("XXX", "Guard(9)")
                        e.printStackTrace()
                        return@runBlocking guardValidSpotifyApi(
                            classBackTo = classBackTo,
                            alreadyTriedToReauthenticate = true,
                            block = block
                        )
                    }
                } else {
                    Log.d("XXX", "Guard(10)")
                    pkceClassBackTo = classBackTo
                    startSpotifyClientPkceLoginActivity(SpotifyPkceLoginActivityImpl::class.java)
                    null
                }
            } else {
                Log.d("XXX", "Guard(11)")
                SpotifyDefaultCredentialStore.activityBackOnImplicitAuth = classBackTo
                startSpotifyImplicitLoginActivity(SpotifyImplicitLoginActivityImpl::class.java)
                null
            }
        }
    }
}
