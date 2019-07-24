package com.sprinthub.example.findme

import android.content.Context
import android.location.Location
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import io.reactivex.Observable

class LocationManager(private val context: Context) : LocationCallback() {

    private val fusedLocationProvider: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }


    private fun locationRequest(): LocationRequest {
        return LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    /**
     * Call to request location updates
     */
    fun locationUpdates(locationSettingsStates: LocationSettingsStates): Observable<Location> {
        return Observable.create { emitter ->

            if (!locationSettingsStates.isLocationPresent) {
                emitter.tryOnError(Throwable("Location not available!"))
            }

            val listener = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    if (!emitter.isDisposed) {
                        locationResult?.lastLocation?.let { emitter.onNext(it) }
                    }

                }
            }

            fusedLocationProvider.requestLocationUpdates(
                locationRequest(),
                listener, null
            )

            emitter.setCancellable { fusedLocationProvider.removeLocationUpdates(listener) }
        }
    }

    /**
     * Call this from Activity before calling [locationUpdates]. Check the response, if error is [ResolvableApiException]
     *  then we must display a dialog for the user to change location settings by calling,
     *  exception.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS)
     */
    fun checkLocationSettings(): Observable<LocationSettingsResponse> {
        return Observable.create { emitter ->
            val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest())

            val client = LocationServices.getSettingsClient(context)
            val locationSettingsTask = client.checkLocationSettings(builder.build())

            locationSettingsTask.addOnSuccessListener { response ->
                if (!emitter.isDisposed) {
                    emitter.onNext(response)
                }
            }

            locationSettingsTask.addOnFailureListener { exception ->
                emitter.tryOnError(exception)
            }
        }
    }

}