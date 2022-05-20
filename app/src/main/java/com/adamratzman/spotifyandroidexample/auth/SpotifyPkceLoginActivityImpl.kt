package com.adamratzman.spotifyandroidexample.auth

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.adamratzman.spotify.SpotifyClientApi
import com.adamratzman.spotify.SpotifyScope
import com.adamratzman.spotify.auth.pkce.AbstractSpotifyPkceLoginActivity
import com.adamratzman.spotifyandroidexample.BuildConfig
import com.adamratzman.spotifyandroidexample.SpotifyPlaygroundApplication
import com.adamratzman.spotifyandroidexample.activities.ActionHomeActivity
import com.adamratzman.spotifyandroidexample.toast
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal var pkceClassBackTo: Class<out Activity>? = null

class SpotifyPkceLoginActivityImpl : AbstractSpotifyPkceLoginActivity() {
    override val clientId = BuildConfig.SPOTIFY_CLIENT_ID
    override val redirectUri = BuildConfig.SPOTIFY_REDIRECT_URI_PKCE
    override val scopes = SpotifyScope.values().toList()

    override fun onSuccess(api: SpotifyClientApi) {
        val model = (application as SpotifyPlaygroundApplication).model
        model.credentialStore.setSpotifyApi(api)
        Log.d("XXX", "CredentialStore Scopes: $scopes")
        GlobalScope.launch {
            Log.d("XXX", "CredentialsStore Scope hasReadPlaylist:" +
                    "${api.hasScope(SpotifyScope.PLAYLIST_READ_PRIVATE)}")
        }
        val classBackTo = pkceClassBackTo ?: ActionHomeActivity::class.java
        pkceClassBackTo = null
        toast("Authentication via PKCE has completed. Launching ${classBackTo.simpleName}..")
        startActivity(Intent(this, classBackTo))
    }

    override fun onFailure(exception: Exception) {
        exception.printStackTrace()
        pkceClassBackTo = null
        toast("Auth failed: ${exception.message}")
    }
}