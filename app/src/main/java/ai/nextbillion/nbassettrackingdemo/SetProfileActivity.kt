package ai.nextbillion.nbassettrackingdemo

import ai.nextbillion.assettracking.AssetTracking
import ai.nextbillion.assettracking.assetTrackingIsRunning
import ai.nextbillion.assettracking.entity.AssetTrackingApiExceptionType
import ai.nextbillion.network.AssetApiCallback
import ai.nextbillion.network.AssetException
import ai.nextbillion.network.AssetProfile
import ai.nextbillion.network.create.AssetCreationResponse
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.InputType
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.UUID

class SetProfileActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editIdView: EditText
    private lateinit var editAssetNameView: EditText
    private lateinit var editDescriptionView: EditText
    private lateinit var editAttributesView: EditText
    private lateinit var editAssetIdView: EditText
    private lateinit var lastAssetIdView: EditText

    private lateinit var createNewAssetView: TextView
    private lateinit var bindAssetView: TextView
    private lateinit var progressBar: View

    private lateinit var customId: String
    private lateinit var assetName: String
    private lateinit var assetDescription: String
    private lateinit var assetAttributes: String
    private lateinit var assetId: String

    private var isSplash = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_profile)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        isSplash = intent.getBooleanExtra(Constants.IS_SPLASH_PAGE_KEY, true)
        setActivityView()
        initViewText()
        setListeners()
        checkAssetId()
    }

    private fun setActivityView() {
        editIdView = findViewById(R.id.edit_custom_id)
        editAssetNameView = findViewById(R.id.edit_asset_name)
        editDescriptionView = findViewById(R.id.edit_asset_description)
        editAttributesView = findViewById(R.id.edit_asset_attributes)
        editAssetIdView = findViewById(R.id.edit_asset_id)
        lastAssetIdView = findViewById(R.id.last_used_asset_id)

        createNewAssetView = findViewById(R.id.create_new_asset)
        bindAssetView = findViewById(R.id.bind_asset)

        progressBar = findViewById(R.id.saving_progress)
    }

    private fun initViewText() {
        customId =
            sharedPreferences.getString(Constants.CUSTOM_ID_KEY, UUID.randomUUID().toString())
                .toString()

        assetName = sharedPreferences.getString(
            Constants.ASSET_NAME_KEY,
            getString(R.string.asset_name_example_value)
        ) as String

        assetDescription = sharedPreferences.getString(
            Constants.ASSET_DESCRIPTION_KEY,
            getString(R.string.asset_description_example_value)
        ) as String

        assetAttributes = sharedPreferences.getString(
            Constants.ASSET_ATTRIBUTES_KEY,
            getString(R.string.asset_attribute_example_value)
        ) as String

        assetId = sharedPreferences.getString(Constants.ASSET_ID_KEY, "") as String

        val lastAssetId = sharedPreferences.getString(Constants.LAST_ASSET_ID_KEY, "") as String

        editIdView.setText(customId)
        editAssetNameView.setText(assetName)
        editDescriptionView.setText(assetDescription)
        editAttributesView.setText(assetAttributes)
        lastAssetIdView.setText(lastAssetId)
        lastAssetIdView.inputType = InputType.TYPE_NULL

        if (!assetId.isNullOrEmpty()) {
            editAssetIdView.setText(assetId)
        }
    }

    private fun hideKeyboard() {
        this.currentFocus?.let { view ->
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun setListeners() {
        createNewAssetView.setOnClickListener {
            readAssetInfoFromView()
            if (!checkUserInput()) {
                return@setOnClickListener
            }

            if (assetTrackingIsRunning) {
                Toast.makeText(
                    this@SetProfileActivity,
                    "Asset tracking is ON, please turn off tracking before creating new asset!",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            hideKeyboard()
            progressBar.visibility = View.VISIBLE

            AssetTracking.instance.createNewAsset(
                AssetProfile.Builder().setCustomId(customId).setName(assetName)
                    .setDescription(assetDescription).setAttributes(
                        mapOf("test attribute" to assetAttributes)
                    ).build(), object : AssetApiCallback<AssetCreationResponse> {
                    override fun onSuccess(result: AssetCreationResponse) {
                        progressBar.visibility = View.GONE
                        assetId = result.data.id
                        editAssetIdView.setText(assetId)
                        saveToSharedPreference()
                    }

                    override fun onFailure(exception: AssetException) {
                        progressBar.visibility = View.GONE
                        Toast.makeText(
                            this@SetProfileActivity,
                            "create asset failed: " + exception.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )

        }

        bindAssetView.setOnClickListener {
            assetId = editAssetIdView.text.toString().trim()

            if (assetId.isEmpty()) {
                return@setOnClickListener
            }
            hideKeyboard()
            progressBar.visibility = View.VISIBLE

            AssetTracking.instance.bindAsset(
                this@SetProfileActivity,
                assetId,
                object : AssetApiCallback<Unit> {
                    override fun onSuccess(result: Unit) {
                        progressBar.visibility = View.GONE

                        Toast.makeText(
                            this@SetProfileActivity,
                            String.format(
                                "bind asset successfully with assetId: %s",
                                assetId
                            ),
                            Toast.LENGTH_LONG
                        ).show()

                        sharedPreferences.edit().putString(Constants.LAST_ASSET_ID_KEY, assetId).apply()

                        if (isSplash) {
                            launchMainActivity()
                        } else {
                            finish()
                        }
                    }

                    override fun onFailure(exception: AssetException) {
                        progressBar.visibility = View.GONE

                        val exceptionMessage = exception.message ?: ""

                        if (exceptionMessage.contains(AssetTrackingApiExceptionType.UN_UPLOADED_LOCATION_DATA.exceptionString)) {
                            showForceBindDialog(assetId, exceptionMessage)
                        } else {
                            Toast.makeText(
                                this@SetProfileActivity,
                                "bind asset failed: $exceptionMessage",
                                Toast.LENGTH_LONG
                            ).show()
                        }                    }
                })
        }
    }

    private fun showForceBindDialog(assetId: String, warningMessage: String) {
        val alertDialogBuilder = AlertDialog.Builder(this)

        alertDialogBuilder.setMessage("$warningMessage, do you want to clear local data and force bind to new asset id?")

        alertDialogBuilder.setPositiveButton("Proceed") { dialogInterface: DialogInterface, _: Int ->
            dialogInterface.dismiss() // Close the dialog
            progressBar.visibility = View.VISIBLE

            AssetTracking.instance.forceBindAsset(this, assetId, object : AssetApiCallback<Unit> {
                override fun onSuccess(result: Unit) {
                    progressBar.visibility = View.GONE

                    Toast.makeText(
                        this@SetProfileActivity,
                        String.format(
                            "Force bind new asset successfully with assetId: %s",
                            assetId
                        ),
                        Toast.LENGTH_LONG
                    ).show()

                    sharedPreferences.edit().putString(Constants.LAST_ASSET_ID_KEY, assetId).apply()

                    if (isSplash) {
                        launchMainActivity()
                    } else {
                        finish()
                    }
                }

                override fun onFailure(exception: AssetException) {
                    progressBar.visibility = View.GONE

                    Toast.makeText(
                        this@SetProfileActivity,
                        "bind asset failed: " + exception.message,
                        Toast.LENGTH_LONG
                    ).show()                }
            })
        }

        alertDialogBuilder.setNegativeButton("Cancel") { dialogInterface: DialogInterface, _: Int ->
            dialogInterface.dismiss() // Close the dialog
        }

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun readAssetInfoFromView() {
        customId = editIdView.text.toString().trim()
        assetName = editAssetNameView.text.toString().trim()
        assetDescription = editDescriptionView.text.toString().trim()
        assetAttributes = editAttributesView.text.toString().trim()
    }

    private fun saveToSharedPreference() {
        sharedPreferences.edit().putString(Constants.CUSTOM_ID_KEY, customId).apply()
        sharedPreferences.edit().putString(Constants.ASSET_NAME_KEY, assetName).apply()
        sharedPreferences.edit().putString(Constants.ASSET_DESCRIPTION_KEY, assetDescription)
            .apply()
        sharedPreferences.edit().putString(Constants.ASSET_ATTRIBUTES_KEY, assetAttributes).apply()
        sharedPreferences.edit()
            .putString(Constants.ASSET_ID_KEY, assetId)
            .apply()
    }

    private fun checkAssetId() {
        if (isSplash && !TextUtils.isEmpty(assetId)) {
            launchMainActivity()
        }
    }

    private fun launchMainActivity() {
        startActivity(Intent(this, ExtendedTrackingActivity::class.java))
        finish()
    }

    private fun checkUserInput(): Boolean {
        if (assetName.isEmpty()) {
            Toast.makeText(this, R.string.please_enter_name, Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
}