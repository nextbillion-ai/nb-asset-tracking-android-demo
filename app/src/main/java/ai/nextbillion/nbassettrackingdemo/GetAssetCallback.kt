package ai.nextbillion.nbassettrackingdemo

import ai.nextbillion.assettracking.assetTrackingAddCallback
import ai.nextbillion.assettracking.assetTrackingRemoveCallback
import ai.nextbillion.assettracking.assetTrackingStart
import ai.nextbillion.assettracking.assetTrackingStop
import ai.nextbillion.assettracking.bindAsset
import ai.nextbillion.assettracking.callback.AssetTrackingCallBack
import ai.nextbillion.assettracking.createNewAsset
import ai.nextbillion.assettracking.entity.TrackingDisableType
import ai.nextbillion.assettracking.entity.TripStatus
import ai.nextbillion.assettracking.initialize
import ai.nextbillion.assettracking.location.permissions.LocationPermissionsListener
import ai.nextbillion.assettracking.location.permissions.LocationPermissionsManager
import ai.nextbillion.network.AssetApiCallback
import ai.nextbillion.network.AssetException
import ai.nextbillion.network.AssetProfile
import ai.nextbillion.network.create.AssetCreationResponse
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.UUID

/**
 * Copyright © NextBillion.ai. All rights reserved.
 * Use of this source code is governed by license that can be found in the LICENSE file.
 */
class GetAssetCallback : AppCompatActivity(), AssetTrackingCallBack {
    private lateinit var startTrackingButton: Button
    private lateinit var stopTrackingButton: Button
    private lateinit var trackingStatusView: TextView
    private lateinit var locationInfoView: TextView

    var permissionsManager: LocationPermissionsManager? = null
    private var assetId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_get_asset_callback)

        // initialize the Asset Tracking SDK
        initialize(Constants.DEFAULT_API_KEY)
        assetTrackingAddCallback(this)

        createAndBindAsset()
        initView()
    }

    private fun initView() {
        startTrackingButton = findViewById(R.id.callback_start_tracking)
        startTrackingButton.setOnClickListener {
            checkPermissionsAndStartTracking()
        }

        stopTrackingButton = findViewById(R.id.callback_stop_tracking)
        stopTrackingButton.setOnClickListener {
            assetTrackingStop()
        }

        trackingStatusView = findViewById(R.id.callback_is_stop_tracking)
        locationInfoView = findViewById(R.id.callback_location_info)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        assetTrackingRemoveCallback(this)

        // add this to avoid blocking other example, could remove in real usage
        assetTrackingStop()
    }

    @SuppressLint("SetTextI18n")
    override fun onLocationSuccess(location: Location) {
        locationInfoView.text = """
            --------- Location Info --------- 
            Provider: ${location.provider}
            Latitude: ${location.latitude}
            Longitude: ${location.longitude}
            Altitude: ${location.altitude}
            Accuracy: ${location.accuracy}
            speed:${location.speed}
            Bearing: ${location.bearing}
            Time: ${location.time}
            """.trimIndent()
    }

    override fun onLocationFailure(exception: Exception) {
        locationInfoView.text = exception.message
    }

    @SuppressLint("SetTextI18n")
    override fun onTrackingStart(assetId: String) {
        trackingStatusView.text = "Asset Tracking is ON"
    }

    @SuppressLint("SetTextI18n")
    override fun onTrackingStop(assetId: String, trackingDisableType: TrackingDisableType) {
        trackingStatusView.text = "Asset Tracking is OFF"
        locationInfoView.text = ""
    }

    override fun onTripStatusChanged(tripId: String, status: TripStatus) {
    }

    private fun createAndBindAsset() {
        val assetAttributes: Map<String, String> = mapOf("attribute 1" to "test 1", "attribute 2" to "test 2")
        val assetProfile = AssetProfile.Builder().setCustomId(UUID.randomUUID().toString()).setName("testName")
            .setDescription("testDescription").setAttributes(assetAttributes).build()

        createNewAsset(assetProfile, object : AssetApiCallback<AssetCreationResponse> {
            @SuppressLint("SetTextI18n")
            override fun onSuccess(result: AssetCreationResponse) {
                assetId = result.data.id
                Toast.makeText(
                    this@GetAssetCallback,
                    "create asset successfully with asset id: $assetId",
                    Toast.LENGTH_LONG
                ).show()

                onBindAsset()
            }

            override fun onFailure(exception: AssetException) {
                Toast.makeText(
                    this@GetAssetCallback,
                    "create asset failed with error: " + exception.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun onBindAsset() {
        bindAsset(assetId, object : AssetApiCallback<Unit> {
            @SuppressLint("SetTextI18n")
            override fun onSuccess(result: Unit) {
                Toast.makeText(
                    this@GetAssetCallback,
                    String.format(
                        "bind asset successfully with assetId: %s",
                        assetId
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }

            override fun onFailure(exception: AssetException) {
                val exceptionMessage = exception.message ?: ""
                Toast.makeText(
                    this@GetAssetCallback,
                    "bind asset failed: $exceptionMessage",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun checkPermissionsAndStartTracking() {
        if (LocationPermissionsManager.areAllLocationPermissionGranted(this)) {
            assetTrackingStart()
        } else if (!LocationPermissionsManager.isLocationServiceEnabled(this)) {
            showLocationServiceOffDialog()
        } else {
            permissionsManager = LocationPermissionsManager(object : LocationPermissionsListener {
                override fun onExplanationNeeded(permissionsToExplain: List<String>?) {
                    Toast.makeText(
                        this@GetAssetCallback, "You need to accept location permissions.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onPermissionResult(granted: Boolean) {
                    if (granted) {
                        assetTrackingStart()
                    } else {
                        Toast.makeText(
                            this@GetAssetCallback, "You need to accept location permissions.$granted",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
            permissionsManager?.requestLocationPermissions(this)
        }
    }

    private fun showLocationServiceOffDialog() {
        val alertDialogBuilder = AlertDialog.Builder(this)

        alertDialogBuilder.setTitle("Location Services Disabled")
        alertDialogBuilder.setMessage("To enable location services, please go to Settings > Privacy > Location Services.")

        alertDialogBuilder.setPositiveButton("OK") { dialogInterface: DialogInterface, _: Int ->
            dialogInterface.dismiss() // Close the dialog
        }

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }
}