package ai.nextbillion.nbassettrackingdemo

import ai.nextbillion.assettracking.AssetTracking
import ai.nextbillion.assettracking.assetTrackingGetDataTrackingConfig
import ai.nextbillion.assettracking.assetTrackingGetLocationConfig
import ai.nextbillion.assettracking.assetTrackingGetNotificationConfig
import ai.nextbillion.assettracking.assetTrackingStart
import ai.nextbillion.assettracking.assetTrackingStop
import ai.nextbillion.assettracking.assetTrackingUpdateDataTrackingConfig
import ai.nextbillion.assettracking.assetTrackingUpdateLocationConfig
import ai.nextbillion.assettracking.assetTrackingUpdateNotificationConfig
import ai.nextbillion.assettracking.bindAsset
import ai.nextbillion.assettracking.createNewAsset
import ai.nextbillion.assettracking.entity.DataTrackingConfig
import ai.nextbillion.assettracking.entity.DefaultConfig
import ai.nextbillion.assettracking.entity.LocationConfig
import ai.nextbillion.assettracking.entity.NotificationConfig
import ai.nextbillion.assettracking.initialize
import ai.nextbillion.assettracking.location.engine.TrackingMode
import ai.nextbillion.assettracking.location.permissions.LocationPermissionsListener
import ai.nextbillion.assettracking.location.permissions.LocationPermissionsManager
import ai.nextbillion.network.AssetApiCallback
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
import com.google.gson.Gson
import java.util.UUID

/**
 * Copyright © NextBillion.ai. All rights reserved.
 * Use of this source code is governed by license that can be found in the LICENSE file.
 */
class UpdateConfiguration : AppCompatActivity() {
    private lateinit var updateLocationConfigButton: Button
    private lateinit var updateNotificationConfigButton: Button
    private lateinit var updateDataTrackingConfigButton: Button
    private lateinit var configurationDetailView: TextView

    private lateinit var defaultConfig: DefaultConfig
    private lateinit var locationConfig: LocationConfig
    private lateinit var notificationConfig: NotificationConfig
    private lateinit var dataTrackingConfig: DataTrackingConfig

    var permissionsManager: LocationPermissionsManager? = null
    private var assetId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_configuration)

        // initialize the Asset Tracking SDK
        initConfigurations()
        createAsset()

        initView()
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
        // add this to avoid blocking other example, could remove in real usage
        assetTrackingStop()
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
        configurationDetailView = findViewById(R.id.configuration_detail)

        updateLocationConfigButton = findViewById(R.id.update_location_config)
        updateLocationConfigButton.setOnClickListener {
            val locationConfigToChange = LocationConfig(
                interval = 20000,
                smallestDisplacement = 20.0f,
                maxWaitTime = 5000,
                fastestInterval = 1500,
                enableStationaryCheck = true
            )
            assetTrackingUpdateLocationConfig(locationConfigToChange)

            locationConfig = assetTrackingGetLocationConfig()

            configurationDetailView.text = "location config: " + Gson().toJson(locationConfig)

        }

        updateNotificationConfigButton = findViewById(R.id.update_notification_config)
        updateNotificationConfigButton.setOnClickListener {
            val notificationConfigToChange = NotificationConfig(
                channelId = "NextBillion.ai",
                channelName = "NextBillion.ai",
            )
            assetTrackingUpdateNotificationConfig(notificationConfigToChange)

            notificationConfig = assetTrackingGetNotificationConfig()

            configurationDetailView.text = "notification config: " + Gson().toJson(notificationConfig)
        }

        updateDataTrackingConfigButton = findViewById(R.id.update_data_tracking_config)
        updateDataTrackingConfigButton.setOnClickListener {

            val dataTrackingConfigToChange = DataTrackingConfig(
                baseUrl = "https://api.nextbillion.io",
                dataUploadingBatchSize = 15,
                dataUploadingBatchWindow = 30,
                dataStorageSize = 5000,
                shouldClearLocalDataWhenCollision = false
            )
            assetTrackingUpdateDataTrackingConfig(dataTrackingConfigToChange)

            dataTrackingConfig = assetTrackingGetDataTrackingConfig()
            configurationDetailView.text = "dataTracking config: " + Gson().toJson(dataTrackingConfig)
        }
    }


    private fun initConfigurations() {
        defaultConfig = DefaultConfig(
            debug = false,
            enhanceService = true,
            repeatInterval = 5L,
            workerEnabled = true,
            crashRestartEnabled = true,
            workOnMainThread = true,
            restartIntent = null
        )

        // You can either choose the specified tracking mode, or use self-defined
        locationConfig = LocationConfig(
            trackingMode = TrackingMode.ACTIVE,
            maxWaitTime = 10000,
            fastestInterval = 1000,
            enableStationaryCheck = false
        )

        notificationConfig = NotificationConfig(channelId = "Custom.ID", channelName = "Custom.Name")

        dataTrackingConfig = DataTrackingConfig(
            baseUrl = "https://api.nextbillion.io",
            dataUploadingBatchSize = 30,
            dataUploadingBatchWindow = 20,
            dataStorageSize = 5000,
            shouldClearLocalDataWhenCollision = true
        )

        AssetTracking {
            setDefaultConfig(defaultConfig)
            setLocationConfig(locationConfig)
            setNotificationConfig(notificationConfig)
            setDataTrackingConfig(dataTrackingConfig)
        }

        initialize(Constants.DEFAULT_API_KEY)
    }

    private fun createAsset() {
        val assetAttributes: Map<String, String> = mapOf("attribute 1" to "test 1", "attribute 2" to "test 2")
        val assetProfile = AssetProfile.Builder().setCustomId(UUID.randomUUID().toString()).setName("testName")
            .setDescription("testDescription").setAttributes(assetAttributes).build()

        createNewAsset(assetProfile, object : AssetApiCallback<AssetCreationResponse> {
            @SuppressLint("SetTextI18n")
            override fun onSuccess(result: AssetCreationResponse) {
                assetId = result.data.id
                Toast.makeText(
                    this@UpdateConfiguration,
                    "create asset successfully with asset id: $assetId",
                    Toast.LENGTH_LONG
                ).show()
                bindAssetAndStartTracking()
            }

            override fun onFailure(exception: Exception) {
                Toast.makeText(
                    this@UpdateConfiguration,
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
                    this@UpdateConfiguration,
                    String.format(
                        "bind asset successfully with assetId: %s",
                        assetId
                    ),
                    Toast.LENGTH_LONG
                ).show()
                updateLocationConfigButton.isEnabled = true
                updateNotificationConfigButton.isEnabled = true
                updateDataTrackingConfigButton.isEnabled = true
                checkPermissionsAndStartTracking()
            }

            override fun onFailure(exception: Exception) {
                val exceptionMessage = exception.message ?: ""
                Toast.makeText(
                    this@UpdateConfiguration,
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
                        this@UpdateConfiguration, "You need to accept location permissions.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onPermissionResult(granted: Boolean) {
                    if (granted) {
                        startTracking()
                    } else {
                        Toast.makeText(
                            this@UpdateConfiguration, "You need to accept location permissions.",
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