package com.sprinthub.example.findme

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.api.ResolvableApiException
import com.vanniktech.rxpermission.Permission
import com.vanniktech.rxpermission.RealRxPermission
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        setContentView(R.layout.activity_main)

        val rxPermission = RealRxPermission.getInstance(applicationContext)
        rxPermission.request(Manifest.permission.ACCESS_FINE_LOCATION)
            .subscribe({ permission ->
                when (permission.state()) {
                    Permission.State.GRANTED -> setupLocationUpdatesReporting()
                    Permission.State.DENIED -> Toast.makeText(
                        this,
                        "Permission denied!",
                        Toast.LENGTH_SHORT
                    ).show()
                    Permission.State.DENIED_NOT_SHOWN -> Toast.makeText(
                        this,
                        "Permission denied!",
                        Toast.LENGTH_SHORT
                    ).show()
                    Permission.State.REVOKED_BY_POLICY -> Toast.makeText(
                        this,
                        "Permission revoked!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }, { Timber.w(it) })
            .let { disposables.add(it) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == REQUEST_CHECK_SETTINGS && resultCode == Activity.RESULT_OK) {
            setupLocationUpdatesReporting()
        }
    }

    private fun setupLocationUpdatesReporting() {

        val locationManager = LocationManager(applicationContext)

        locationManager.checkLocationSettings()
            .flatMap { settings -> locationManager.locationUpdates(settings.locationSettingsStates) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ location ->
                textView_location.text = getString(
                    R.string.location_format,
                    location.longitude,
                    location.latitude,
                    System.currentTimeMillis()
                )
            }, { throwable ->
                Timber.w(throwable, "Error occurred")
                if (throwable is ResolvableApiException) {
                    try {
                        throwable.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                    } catch (sendEx: IntentSender.SendIntentException) {
                        Timber.w("Request cancelled")
                    }
                }
            }).let { disposables.add(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!disposables.isDisposed) disposables.dispose()
    }
}


private const val REQUEST_CHECK_SETTINGS = 100