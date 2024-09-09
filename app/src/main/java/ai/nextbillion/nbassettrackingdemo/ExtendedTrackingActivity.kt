package ai.nextbillion.nbassettrackingdemo

import ai.nextbillion.assettracking.*
import ai.nextbillion.assettracking.callback.AssetTrackingCallBack
import ai.nextbillion.assettracking.entity.FakeGpsConfig
import ai.nextbillion.assettracking.entity.LocationConfig
import ai.nextbillion.assettracking.entity.TrackingDisableType
import ai.nextbillion.assettracking.entity.TripStatus
import ai.nextbillion.assettracking.extension.log
import ai.nextbillion.assettracking.location.engine.TrackingMode
import ai.nextbillion.assettracking.location.permissions.LocationPermissionsListener
import ai.nextbillion.assettracking.location.permissions.LocationPermissionsManager
import ai.nextbillion.assettracking.location.permissions.LocationPermissionsManager.Companion.areAllLocationPermissionGranted
import ai.nextbillion.assettracking.location.permissions.LocationPermissionsManager.Companion.isLocationServiceEnabled
import ai.nextbillion.datacollection.NBAssetData
import ai.nextbillion.nbassettrackingdemo.databinding.ActivityExtendedTrackingBinding
import ai.nextbillion.network.AssetApiCallback
import ai.nextbillion.network.AssetException
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach


class ExtendedTrackingActivity : AppCompatActivity(), View.OnClickListener,
    AssetTrackingCallBack {
    private var permissionsManager: LocationPermissionsManager? = null
    private var currentSelectedTrackingMode: MultiTrackingMode = MultiTrackingMode.ACTIVE

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var binding: ActivityExtendedTrackingBinding

    private val mainHandler by lazy {
        Handler(Looper.getMainLooper())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExtendedTrackingBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        supportActionBar?.title = "Asset tracking"

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        initialize(Constants.DEFAULT_API_KEY)
        initView()
        assetTrackingAddCallback(this)
        bindExistingAsset()
    }

    private fun bindExistingAsset() {
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

                    override fun onFailure(exception: AssetException) {
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
        binding.radioButtonActive.isChecked = true
        binding.startTracking.setOnClickListener(this)
        binding.stopTracking.setOnClickListener(this)
        binding.startTrip.setOnClickListener(this)
        binding.stopTrip.setOnClickListener(this)


        binding.radioGroup.setOnCheckedChangeListener { group, checkedId ->
            configSelectedTrackingMode(checkedId)
        }

        binding.editAssetProfile.setOnClickListener {
            if (AssetTracking.instance.isRunning(this)) {
                Toast.makeText(
                    this,
                    "please stop tracking before editing asset profile",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            val intent = Intent(this@ExtendedTrackingActivity, SetProfileActivity::class.java)
            intent.putExtra(Constants.IS_SPLASH_PAGE_KEY, false)
            startActivity(intent)
        }

        binding.mockLocationSb.setOnCheckedChangeListener { _, isChecked ->
            assetTrackingUpdateFakeGpsConfig(FakeGpsConfig(isChecked))
        }

        updateTrackingStatus()
    }

    private fun configSelectedTrackingMode(checkedId: Int) {
        currentSelectedTrackingMode = when (checkedId) {
            R.id.radioButtonActive -> MultiTrackingMode.ACTIVE
            R.id.radioButtonBalanced -> MultiTrackingMode.BALANCED
            R.id.radioButtonPassive -> MultiTrackingMode.PASSIVE
            R.id.radioButtonTimeInterval -> MultiTrackingMode.TIME_INTERVAL
            R.id.radioButtonDistanceInterval -> MultiTrackingMode.DISTANCE_INTERVAL
            else -> MultiTrackingMode.ACTIVE
        }
        binding.timeInterval.visibility =
            if (binding.radioButtonTimeInterval.isChecked) View.VISIBLE else View.GONE
        binding.distanceInterval.visibility =
            if (binding.radioButtonDistanceInterval.isChecked) View.VISIBLE else View.GONE
    }

    @SuppressLint("SetTextI18n")
    override fun onClick(view: View) {
        if (view.id == R.id.start_tracking) {
            checkPermissionsAndStartTracking()
        } else if (view.id == R.id.stop_tracking) {
            assetTrackingStop()
        } else if (view.id == R.id.start_trip) {
            startTrip()
        } else if (view.id == R.id.stop_trip) {
            endTrip()
        }
    }

    private fun startTrip() {

        showInputDialog(this) {
            assetTrackingStartTrip(it, true, object : AssetApiCallback<String> {
                override fun onSuccess(result: String) {
                    Toast.makeText(this@ExtendedTrackingActivity, "start trip successfully", Toast.LENGTH_LONG)
                        .show()
                }

                override fun onFailure(exception: AssetException) {
                    Toast.makeText(
                        this@ExtendedTrackingActivity,
                        "start trip failed: " + exception.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            })

        }
    }

    private fun endTrip() {
        assetTrackingEndTrip(object : AssetApiCallback<String> {
            override fun onSuccess(result: String) {
                Toast.makeText(this@ExtendedTrackingActivity, "end trip successfully", Toast.LENGTH_LONG).show()
            }

            override fun onFailure(exception: AssetException) {
                Toast.makeText(
                    this@ExtendedTrackingActivity,
                    "end trip failed: " + exception.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun updateTrackingStatus() {
        Log.d("asset", "updateTrackingStatus")
        val isTrackingOn = AssetTracking.instance.isRunning(this)
        binding.isStopTracking.text = "Tracking Status: " + if (isTrackingOn) "ON" else "OFF" +
                "\nTrip Status: " + if (assetTrackingIsTripInProgress) "ON" else "OFF"
        if (!isTrackingOn) {
            binding.locationInfo.text = ""
            binding.locationEngineInfo.text = ""
            binding.startTracking.isEnabled = true
            binding.stopTracking.isEnabled = false
            binding.radioGroup.forEach {
                it.isEnabled = true
                binding.timeInterval.isEnabled = true
                binding.distanceInterval.isEnabled = true
            }
        } else {
            binding.startTracking.isEnabled = false
            binding.stopTracking.isEnabled = true
            binding.radioGroup.forEach {
                it.isEnabled = false
                binding.timeInterval.isEnabled = false
                binding.distanceInterval.isEnabled = false
            }
        }

        val isTripInProgress = assetTrackingIsTripInProgress
        if (isTripInProgress) {
            binding.startTrip.isEnabled = false
            binding.stopTrip.isEnabled = true
        } else {
            binding.startTrip.isEnabled = true
            binding.stopTrip.isEnabled = false
        }

    }

    @SuppressLint(*["SetTextI18n", "MissingPermission"])
    fun startTracking() {
        log("MainActivity startTracking")
        if (checkLocationConfig()) {
            assetTrackingStart()
        }
    }

    private fun checkLocationConfig(): Boolean {
        val locationConfig: LocationConfig = when (currentSelectedTrackingMode) {
            MultiTrackingMode.ACTIVE, MultiTrackingMode.BALANCED, MultiTrackingMode.PASSIVE -> {
                LocationConfig(TrackingMode.fromValue(currentSelectedTrackingMode.value)!!)
            }

            MultiTrackingMode.TIME_INTERVAL -> {
                val time = binding.timeInterval.text.toString().toLongOrNull()
                if (time == null) {
                    Toast.makeText(this, "Please input valid time interval", Toast.LENGTH_SHORT)
                        .show()
                    return false
                }
                LocationConfig(interval = time * 1000)
            }

            MultiTrackingMode.DISTANCE_INTERVAL -> {
                val distance = binding.distanceInterval.text.toString().toFloatOrNull()
                if (distance == null) {
                    Toast.makeText(this, "Please input valid distance interval", Toast.LENGTH_SHORT)
                        .show()
                    return false
                }
                LocationConfig(smallestDisplacement = distance)
            }
        }
        AssetTracking.instance.setLocationConfig(locationConfig)
        return true
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
    override fun onLocationSuccess(location: Location) {
        log("onLocationSuccess : " + location.toString())
        binding.locationInfo.text = """
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
        binding.locationInfo.text = exception.message
    }

    override fun onTrackingStart(assetId: String) {
        updateTrackingStatus()
    }

    override fun onTrackingStop(assetId: String, trackingDisableType: TrackingDisableType) {
        Log.d("asset", "onTrackingStop")
        mainHandler.postDelayed({
            updateTrackingStatus()
        }, 1000)
    }


    /**
     * Invoked when the trip status changed.
     * @param tripId the trip id
     * @param status the trip status, see [TripStatus]
     */
    override fun onTripStatusChanged(tripId: String, status: TripStatus) {
        // Handle the trip status change,
        updateTrackingStatus()
    }
}