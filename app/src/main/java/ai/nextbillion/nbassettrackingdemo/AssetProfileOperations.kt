package ai.nextbillion.nbassettrackingdemo

import ai.nextbillion.assettracking.bindAsset
import ai.nextbillion.assettracking.createNewAsset
import ai.nextbillion.assettracking.getAssetInfo
import ai.nextbillion.assettracking.initialize
import ai.nextbillion.assettracking.updateAssetInfo
import ai.nextbillion.network.AssetApiCallback
import ai.nextbillion.network.AssetProfile
import ai.nextbillion.network.create.AssetCreationResponse
import ai.nextbillion.network.get.Asset
import ai.nextbillion.network.get.GetAssetResponse
import android.annotation.SuppressLint
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
class AssetProfileOperations : AppCompatActivity() {
    private var assetId = ""
    private var assetName = "testName"
    private var assetDescription = "testDescription"
    private var assetAttributes: Map<String, String> = mapOf("attribute 1" to "test 1", "attribute 2" to "test 2")

    private lateinit var createAssetButton: Button
    private lateinit var bindAssetButton: Button
    private lateinit var updateAssetButton: Button
    private lateinit var getAssetDetailButton: Button
    private lateinit var assetDetailView: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asset_profile_operation)
        initView()

        // initialize the Asset Tracking SDK
        initialize(Constants.DEFAULT_API_KEY)
    }

    private fun initView() {
        createAssetButton = findViewById(R.id.create_asset)
        createAssetButton.setOnClickListener {
            createAsset()
        }

        bindAssetButton = findViewById(R.id.bind_asset_to_device)
        bindAssetButton.setOnClickListener {
            onBindAsset()
        }

        updateAssetButton = findViewById(R.id.update_asset)
        updateAssetButton.setOnClickListener {
            updateAsset()
        }

        getAssetDetailButton = findViewById(R.id.get_asset_detail)
        getAssetDetailButton.setOnClickListener {
            getAssetDetail()
        }

        assetDetailView = findViewById(R.id.asset_detail)
    }

    private fun createAsset() {
        val assetProfile = AssetProfile.Builder().setCustomId(UUID.randomUUID().toString()).setName(assetName)
            .setDescription(assetDescription).setAttributes(assetAttributes).build()

        createNewAsset(assetProfile, object : AssetApiCallback<AssetCreationResponse> {
            @SuppressLint("SetTextI18n")
            override fun onSuccess(result: AssetCreationResponse) {
                assetId = result.data.id
                Toast.makeText(
                    this@AssetProfileOperations,
                    "create asset successfully with asset id: $assetId",
                    Toast.LENGTH_LONG
                ).show()

                val assetJsonString = Gson().toJson(assetProfile)
                assetDetailView.text = "asset profile is: $assetJsonString"
            }

            override fun onFailure(exception: Exception) {
                val exceptionMessage = exception.message ?: ""
                Toast.makeText(
                    this@AssetProfileOperations,
                    "update asset profile failed with error: $exceptionMessage",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun onBindAsset() {
        bindAsset(assetId, object : AssetApiCallback<Unit> {
            override fun onSuccess(result: Unit) {
                Toast.makeText(
                    this@AssetProfileOperations,
                    String.format(
                        "bind asset successfully with assetId: %s",
                        assetId
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }

            override fun onFailure(exception: Exception) {
                val exceptionMessage = exception.message ?: ""
                Toast.makeText(
                    this@AssetProfileOperations,
                    "bind asset failed: $exceptionMessage",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    // Operation of updating asset can only be done after binding to an asset
    private fun updateAsset() {
        assetName = "newName"
        assetDescription = "newDescription"
        val assetProfile = AssetProfile.Builder().setCustomId(assetId).setName(assetName)
            .setDescription(assetDescription).setAttributes(assetAttributes).build()

        updateAssetInfo(assetProfile, object : AssetApiCallback<Unit> {
            @SuppressLint("SetTextI18n")
            override fun onSuccess(result: Unit) {
                Toast.makeText(
                    this@AssetProfileOperations,
                    "update asset profile successfully",
                    Toast.LENGTH_LONG
                ).show()

                val assetJsonString = Gson().toJson(assetProfile)
                assetDetailView.text = "asset profile is: $assetJsonString"
            }

            override fun onFailure(exception: Exception) {
                val exceptionMessage = exception.message ?: ""
                Toast.makeText(
                    this@AssetProfileOperations,
                    "update asset profile failed with error: $exceptionMessage",
                    Toast.LENGTH_LONG
                ).show()
            }

        })
    }

    // User can only get current asset info, and this operation can be done only after binding to an asset id
    private fun getAssetDetail() {
        getAssetInfo(object : AssetApiCallback<GetAssetResponse> {
            @SuppressLint("SetTextI18n")
            override fun onSuccess(result: GetAssetResponse) {
                val asset: Asset = result.data.asset
                val assetJsonString = Gson().toJson(asset)
                assetDetailView.text = "full asset info: $assetJsonString"
            }

            override fun onFailure(exception: Exception) {
                val exceptionMessage = exception.message ?: ""
                Toast.makeText(
                    this@AssetProfileOperations,
                    "bind asset failed: $exceptionMessage",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

}