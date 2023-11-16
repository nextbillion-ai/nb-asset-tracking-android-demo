package ai.nextbillion.nbassettrackingdemo

import ai.nextbillion.assettracking.*
import ai.nextbillion.assettracking.callback.AssetTrackingCallBack
import ai.nextbillion.assettracking.entity.LocationConfig
import ai.nextbillion.assettracking.entity.TrackingDisableType
import ai.nextbillion.assettracking.location.engine.TrackingMode
import ai.nextbillion.assettracking.location.permissions.LocationPermissionsListener
import ai.nextbillion.assettracking.location.permissions.LocationPermissionsManager
import ai.nextbillion.assettracking.location.permissions.LocationPermissionsManager.Companion.areAllLocationPermissionGranted
import ai.nextbillion.assettracking.location.permissions.LocationPermissionsManager.Companion.isBackgroundLocationPermissionGranted
import ai.nextbillion.assettracking.location.permissions.LocationPermissionsManager.Companion.isLocationServiceEnabled
import ai.nextbillion.network.AssetApiCallback
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity


class ExtendedTrackingActivity : AppCompatActivity(), View.OnClickListener,
    AssetTrackingCallBack {
    private lateinit var startTrackingButton: Button
    private lateinit var stopTrackingButton: Button
    private lateinit var radioGroup: RadioGroup
    private lateinit var activeRadioButton: RadioButton
    private lateinit var trackingStatusView: TextView
    private lateinit var locationEngineInfoView: TextView
    private lateinit var locationInfoView: TextView
    private lateinit var editAssetProfileView: TextView


    var permissionsManager: LocationPermissionsManager? = null
    var currentSelectedTrackingMode = TrackingMode.ACTIVE

    private val mainHandler by lazy {
        Handler(Looper.getMainLooper())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_extended_tracking)
        // custom location engine
//        TrackingSDK.setLocationEngine(CustomLocationEngine());
        initialize(Constants.DEFAULT_API_KEY)

        initView()

        assetTrackingAddCallback(this)
        bindExistingAsset()
    }

    private fun bindExistingAsset() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val assetId = sharedPreferences.getString(Constants.LAST_ASSET_ID_KEY, "") as String
        if (assetId.isNotEmpty()) {
            AssetTracking.instance.bindAsset(
                this@ExtendedTrackingActivity,
                assetId,
                object : AssetApiCallback<Unit> {
                    override fun onSuccess(result: Unit) {
                        Toast.makeText(
                            this@ExtendedTrackingActivity,
                            String.format(
                                "bind asset successfully with assetId: %s",
                                assetId
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    override fun onFailure(exception: Exception) {
                        Toast.makeText(
                            this@ExtendedTrackingActivity,
                            "bind asset failed: " + exception.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }

                }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        assetTrackingRemoveCallback(this)
    }

    private fun initView() {
        startTrackingButton = findViewById(R.id.start_tracking)
        stopTrackingButton = findViewById(R.id.stop_tracking)
        startTrackingButton.setOnClickListener(this)
        stopTrackingButton.setOnClickListener(this)

        activeRadioButton = findViewById(R.id.radioButtonActive)
        activeRadioButton.isChecked = true

        radioGroup = findViewById(R.id.radioGroup)
        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            currentSelectedTrackingMode = enumValues<TrackingMode>().find {
                it.value == group.indexOfChild(group.findViewById(checkedId))
            }!!
            AssetTracking.instance.updateLocationConfig(this, LocationConfig(currentSelectedTrackingMode))
        }

        editAssetProfileView = findViewById(R.id.edit_asset_profile)
        editAssetProfileView.setOnClickListener {
            if (AssetTracking.instance.isRunning(this)) {
                Toast.makeText(this, "please stop tracking before editing asset profile", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val intent = Intent(this@ExtendedTrackingActivity, SetProfileActivity::class.java)
            intent.putExtra(Constants.IS_SPLASH_PAGE_KEY, false)
            startActivity(intent)
        }

        trackingStatusView = findViewById(R.id.isStopTracking)
        locationEngineInfoView = findViewById(R.id.locationEngineInfo)
        locationInfoView = findViewById(R.id.locationInfo)
    }

    @SuppressLint("SetTextI18n")
    override fun onClick(view: View) {
        if (view.id == R.id.start_tracking) {
            checkPermissionsAndStartTracking()
        } else if (view.id == R.id.stop_tracking) {
//            TrackingSDK.stopTracking()
            assetTrackingStop()
            updateTrackingStatus()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateTrackingStatus() {
        Log.d("asset", "updateTrackingStatus")
        mainHandler.postDelayed({
            val isTrackingOn = AssetTracking.instance.isRunning(this)
            trackingStatusView.text = "Tracking Status: " + if (isTrackingOn) "ON" else "OFF"
            if (!isTrackingOn) {
                locationInfoView.text = ""
                locationEngineInfoView.text = ""
            }
        }, 1000)

    }

    @SuppressLint(*["SetTextI18n", "MissingPermission"])
    fun startTracking() {
        assetTrackingStart()
        updateTrackingStatus()
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

    private fun checkPermissionsAndStartTracking() {
        if (areAllLocationPermissionGranted(this@ExtendedTrackingActivity)) {
            startTracking()
        } else if (!isLocationServiceEnabled(this@ExtendedTrackingActivity)) {
            showLocationServiceOffDialog()
        } else {
            permissionsManager = LocationPermissionsManager(object : LocationPermissionsListener {
                override fun onExplanationNeeded(permissionsToExplain: List<String>?) {
                    Toast.makeText(
                        this@ExtendedTrackingActivity, "You need to accept location permissions.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onPermissionResult(granted: Boolean) {
                    if (granted) {
                        startTracking()
                    } else {
                        Toast.makeText(
                            this@ExtendedTrackingActivity, "You need to accept location permissions.$granted",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
            permissionsManager?.requestLocationPermissions(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun getTrackingModeString(trackingMode: TrackingMode): String {
        return when (trackingMode.value) {
            0 -> "TRACKING_MODE_ACTIVE"
            1 -> "TRACKING_MODE_BALANCED"
            2 -> "TRACKING_MODE_PASSIVE"
            else -> "Unknown tracking mode"
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onLocationSuccess(result: Location) {
        locationInfoView.text = """
            --------- Location Info --------- 
            Provider: ${result.provider}
            Latitude: ${result.latitude}
            Longitude: ${result.longitude}
            Altitude: ${result.altitude}
            Accuracy: ${result.accuracy}
            speed:${result.speed}
            Bearing: ${result.bearing}
            Time: ${result.time}
            """.trimIndent()
    }

    override fun onLocationFailure(exception: Exception) {
        locationInfoView.text = exception.message
    }

    override fun onTrackingStart(assetId: String) {
        updateTrackingStatus()
    }

    override fun onTrackingStop(assetId: String, trackingDisableType: TrackingDisableType) {
        Log.d("asset", "onTrackingStop")
        updateTrackingStatus()
    }
}