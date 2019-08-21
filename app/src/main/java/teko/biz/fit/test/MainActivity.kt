package teko.biz.fit.test

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.DataType.AGGREGATE_STEP_COUNT_DELTA
import com.google.android.gms.fitness.data.DataType.TYPE_STEP_COUNT_DELTA
import android.app.Activity
import android.content.Intent
import android.util.Log
import com.google.android.gms.fitness.result.DataReadResponse
import com.google.android.gms.tasks.Task
import androidx.annotation.NonNull
import com.facebook.*
import com.facebook.appevents.AppEventsLogger
import com.facebook.login.LoginResult
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.fitness.request.DataReadRequest
import java.util.*
import java.util.concurrent.TimeUnit
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.data.Field.FIELD_STEPS
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), FacebookCallback<LoginResult> {
    override fun onSuccess(result: LoginResult?) {
        Log.i(LOG_TAG, result?.accessToken?.token ?: "")
    }

    override fun onCancel() {
    }

    override fun onError(error: FacebookException?) {
    }

    var GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 33

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    val callbackManager = CallbackManager.Factory.create();
    override fun onStart() {
        super.onStart()
        val fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .build()

        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                this, // your activity
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                GoogleSignIn.getLastSignedInAccount(this),
                fitnessOptions
            )
        } else {
            subscribe();
        }

        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);


        login_button.setReadPermissions("email");

        // Callback registration
        login_button.registerCallback(callbackManager, this)

        if (AccessToken.isCurrentAccessTokenActive()) {
            Log.i(LOG_TAG, Profile.getCurrentProfile().name)
        } else {
            Log.i(LOG_TAG, "sdasd")
        }
    }

    val LOG_TAG = "TIMTIM"
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.e(LOG_TAG, "requestCode = $resultCode")
        callbackManager.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
                subscribe()
            }
        }
    }

    fun subscribe() {
        GoogleSignIn.getLastSignedInAccount(this)?.let {
            Fitness.getRecordingClient(this, it)
                .subscribe(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .addOnCompleteListener {
                    if (it.isSuccessful()) {
                        readData()
                        Log.i(LOG_TAG, "YES")
                    } else {
                        Log.i(LOG_TAG, it.exception.toString())
                    }
                }
        }
    }

    fun readData() {
        GoogleSignIn.getLastSignedInAccount(this)?.let {
            Fitness.getHistoryClient(this, it)
                .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
                .addOnSuccessListener {
                    val total = (if (it.isEmpty())
                        0
                    else
                        it.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt()).toLong()
                    error("Total ${total}")
                }
                .addOnFailureListener {
                    Log.w(LOG_TAG, "There was a problem getting the step count.", it);
                }
        }

    }
}