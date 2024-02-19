package com.example.receiptshare.activities.SignIn

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.receiptshare.R
import com.example.receiptshare.activities.Main.MainActivity
import com.example.receiptshare.data.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * Signs a user into the app using a Google account with FirebaseAuth
 */
class SignInActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SignInActivity"
        private const val RC_SIGN_IN = 1
    }

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var mDatabase: FirebaseDatabase

    private lateinit var mLoginButton: SignInButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        mAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance()
        configureGoogleSignIn()

        bindViews()
    }

    private fun bindViews() {
        mLoginButton = findViewById<SignInButton>(R.id.signin_login_btn)
        mLoginButton.setSize(SignInButton.SIZE_WIDE)
        mLoginButton.setOnClickListener { signIn() }
    }

    /**
     * Adds a user to the Firebase Database if they do not already exist
     * Stores the user's display name, profile image, and uid
     */
    private fun addUserToDatabase() {
        val user = mAuth.currentUser
        val uid = user?.uid

        // Check if user is already in the database
        val usersRef = mDatabase.getReference("users/$uid")
        usersRef.addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        Log.d(TAG, "addUserToDatabase() User is already in database")
                    } else {
                        // User is not in database
                        val u = createUser()
                        usersRef.setValue(u)
                            .addOnCompleteListener {
                                Log.d(TAG, "addUserToDatabase() Added user to database")
                            }
                            .addOnFailureListener {
                                Log.e(TAG, "addUserToDatabase() Failed adding user to database", it)
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(
                        TAG,
                        "addUserToDatabase() Failed checking if user is in database",
                        error.toException()
                    )
                }
            })
    }

    /**
     * Returns an instance of the User data class
     * Values are extracted from the current authenticated Firebase user
     */
    private fun createUser(): User {
        val user = mAuth.currentUser!!

        val name = user.displayName!!
        val uid = user.uid!!
        val profilePictureURL = user.photoUrl!!

        return User(name, uid, profilePictureURL.toString())
    }

    private fun configureGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signIn() {
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")

                    // User profile information is stored in Firebase Database
                    addUserToDatabase()

                    val mainActivity = Intent(this, MainActivity::class.java)
                    startActivity(mainActivity)
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            // GoogleSignInApi.getSignInIntent() was called
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val exception = task.exception
            if (task.isSuccessful) {
                try {
                    // Google sign in was successful, authenticate with Firebase
                    val account = task.getResult(ApiException::class.java)!!
                    Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    Log.w(TAG, "onActivityResult() Google sign in failed", e)
                }
            } else {
                Log.w(TAG, exception.toString())
            }
        }
    }

}