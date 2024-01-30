package ai.nextbillion.nbassettrackingdemo

import ai.nextbillion.assettracking.AssetTracking
import ai.nextbillion.assettracking.assetTrackingAddCallback
import ai.nextbillion.assettracking.assetTrackingStart
import ai.nextbillion.assettracking.assetTrackingStop
import ai.nextbillion.assettracking.bindAsset
import ai.nextbillion.assettracking.createNewAsset
import ai.nextbillion.assettracking.entity.FakeGpsConfig
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
class SimpleTrackingExample : AppCompatActivity() {
    private lateinit var startTrackingButton: Button
    private lateinit var trackingStatusView: TextView
    private lateinit var assetIdView: TextView

    var permissionsManager: LocationPermissionsManager? = null
    private var assetId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_tracking)
        initView()

        // initialize the Asset Tracking SDK
        initialize(Constants.DEFAULT_API_KEY)

        AssetTracking.instance.setFakeGpsConfig(FakeGpsConfig(allowUseVirtualLocation = true))
        createAsset()
    }

    override fun onDestroy() {
        super.onDestroy()
        // add this to avoid blocking other example, could remove in real usage
        assetTrackingStop()
    }

    private fun initView() {
        startTrackingButton = findViewById(R.id.start_tracking_simple)
        startTrackingButton.setOnClickListener {
            bindAssetAndStartTracking()
        }

        trackingStatusView = findViewById(R.id.tracking_status_simple)
        assetIdView = findViewById(R.id.asset_id_info)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun createAsset() {
        val assetAttributes: Map<String, String> = mapOf("attribute 1" to "test 1", "attribute 2" to "test 2")
        val assetProfile = AssetProfile.Builder().setCustomId(UUID.randomUUID().toString()).setName("testName")
            .setDescription("testDescription").setAttributes(assetAttributes).build()

        createNewAsset(assetProfile, object : AssetApiCallback<AssetCreationResponse> {
            @SuppressLint("SetTextI18n")
            override fun onSuccess(result: AssetCreationResponse) {
                startTrackingButton.isEnabled = true
                assetId = result.data.id
                Toast.makeText(
                    this@SimpleTrackingExample,
                    "create asset successfully with asset id: $assetId",
                    Toast.LENGTH_LONG
                ).show()

                assetIdView.text = "current asset id is: $assetId"
            }

            override fun onFailure(exception: AssetException) {
                Toast.makeText(
                    this@SimpleTrackingExample,
                    "create asset failed with error: " + exception.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun bindAssetAndStartTracking() {
        bindAsset(assetId, object : AssetApiCallback<Unit> {
            @SuppressLint("SetTextI18n")
            override fun onSuccess(result: Unit) {
                Toast.makeText(
                    this@SimpleTrackingExample,
                    String.format(
                        "bind asset successfully with assetId: %s",
                        assetId
                    ),
                    Toast.LENGTH_LONG
                ).show()
                checkPermissionsAndStartTracking()
            }

            override fun onFailure(exception: AssetException) {
                val exceptionMessage = exception.message ?: ""
                Toast.makeText(
                    this@SimpleTrackingExample,
                    "bind asset failed: $exceptionMessage",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun checkPermissionsAndStartTracking() {
        if (LocationPermissionsManager.areAllLocationPermissionGranted(this)) {
            startTracking()
        } else if (!LocationPermissionsManager.isLocationServiceEnabled(this)) {
            showLocationServiceOffDialog()
        } else {
            permissionsManager = LocationPermissionsManager(object : LocationPermissionsListener {
                override fun onExplanationNeeded(permissionsToExplain: List<String>?) {
                    Toast.makeText(
                        this@SimpleTrackingExample, "You need to accept location permissions.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onPermissionResult(granted: Boolean) {
                    if (granted) {
                        startTracking()
                    } else {
                        Toast.makeText(
                            this@SimpleTrackingExample, "You need to accept location permissions.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
            permissionsManager?.requestLocationPermissions(this)
        }
    }

    @SuppressLint("MissingPermission", "SetTextI18n")
    fun startTracking() {
        assetTrackingStart()
        trackingStatusView.text = "Asset Tracking is running"
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