package com.left.rite

import com.left.rite.databinding.MainBinding
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.Callback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import com.auth0.android.result.UserProfile
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar

class Main : AppCompatActivity() {

    private lateinit var binding: MainBinding
    private lateinit var account: Auth0
    private var cachedCredentials: Credentials? = null
    private var cachedUserProfile: UserProfile? = null

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set up the account object with the Auth0 application details
        account = Auth0(getString(R.string.auth0_clientid), getString(R.string.auth0_domain))
        loginWithBrowser();
    }

    private fun loginWithBrowser() {
        // Setup the WebAuthProvider, using the custom scheme and scope.

        WebAuthProvider.login(account)
            .withScheme(getString(R.string.auth0_scheme))
            .withScope("openid profile email")
            .withAudience("https://${getString(R.string.auth0_domain)}/api/v2/")
            // Launch the authentication passing the callback where the results will be received
            .start(this, object : Callback<Credentials, AuthenticationException> {
                // Called when there is an authentication failure
                override fun onFailure(exception: AuthenticationException) {
                    // Something went wrong!
                }

                // Called when authentication completed successfully
                override fun onSuccess(credentials: Credentials) {
                    cachedCredentials = credentials
                    // Get the access token from the credentials object.
                    // This can be used to call APIs
                    val accessToken = credentials.accessToken
                    print("This is the access token: $accessToken")

                    //        Detector.instance(this)
                    val binding = MainBinding.inflate(layoutInflater)
                    setContentView(binding.root)
                    setSupportActionBar(binding.appBarMain.toolbar)
                    binding.appBarMain.fab.setOnClickListener { view ->
                        Guardian.initiate( this@Main)
                        Snackbar.make(view, "Starting ride tracking.", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show()

                    }

                    val drawerLayout: DrawerLayout = binding.drawerLayout
                    val navView: NavigationView = binding.navView
                    val navController = findNavController(R.id.nav_host_fragment_content_main)
                    // Passing each menu ID as a set of Ids because each
                    // menu should be considered as top level destinations.
                    appBarConfiguration = AppBarConfiguration(
                        setOf(
                            R.id.home, R.id.signals, R.id.settings
                        ), drawerLayout
                    )

//                    val navView: BottomNavigationView = binding.navigation
//                    val navHostFragment =
//                        supportFragmentManager.findFragmentById(R.id.main) as NavHostFragment
//                    val navController = navHostFragment.navController
//                    val appBarConfiguration =
//                        AppBarConfiguration(setOf(R.id.about, R.id.signals, R.id.settings))
                    setupActionBarWithNavController(navController, appBarConfiguration)
                    navView.setupWithNavController(navController)
                }
            })
    }

//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.main, menu)
//        return true
//    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun logout() {
        WebAuthProvider.logout(account)
            .withScheme("demo")
            .start(this, object: Callback<Void?, AuthenticationException> {
                override fun onSuccess(payload: Void?) {
                    // The user has been logged out!
                    print("User has been logged out")
                    // The user has been logged out!
                    cachedCredentials = null
                    cachedUserProfile = null
                }

                override fun onFailure(error: AuthenticationException) {
                    // Something went wrong!
                }
            })
    }
}
