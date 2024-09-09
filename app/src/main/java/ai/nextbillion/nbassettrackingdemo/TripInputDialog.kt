package ai.nextbillion.nbassettrackingdemo

import ai.nextbillion.network.trip.TripProfile
import ai.nextbillion.network.trip.TripUpdateProfile

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout

fun showInputDialog(context: Context, callback: (TripProfile) -> Unit) {
    val layout = LinearLayout(context)
    layout.orientation = LinearLayout.VERTICAL
    val padding = 16
    val customIdInput = EditText(context)
    customIdInput.hint = "Enter Custom ID,If empty, a random ID will be generated"
    customIdInput.setPadding(padding, padding, padding, padding)
    layout.addView(customIdInput)

    val nameInput = EditText(context)
    nameInput.hint = "Enter Name"
    nameInput.setPadding(padding, padding, padding, padding)
    layout.addView(nameInput)

    val descriptionInput = EditText(context)
    descriptionInput.hint = "Enter Description"
    descriptionInput.setPadding(padding, padding, padding, padding)
    layout.addView(descriptionInput)

    // For simplicity, we are not adding input fields for attributes, metaData, and stops
    // You can add them in a similar way if needed
//
    val dialog = AlertDialog.Builder(context)
        .setTitle("Enter Trip Profile Information")
        .setView(layout)
        .setPositiveButton("OK") { _, _ ->
            val name = if (nameInput.text?.toString().isNullOrEmpty()) {
                "test name"
            } else {
                nameInput.text.toString()
            }
            val description = if (descriptionInput.text?.toString().isNullOrEmpty()) {
                "test description"
            } else {
                descriptionInput.text.toString()
            }
            val profile = TripProfile(name = name, description = description)
            callback(profile)
        }
        .setNegativeButton("Cancel", null)
        .create()

    dialog.show()
}

fun showInputUpdateDialog(context: Context, callback: (TripUpdateProfile) -> Unit) {
    val layout = LinearLayout(context)
    layout.orientation = LinearLayout.VERTICAL
    val padding = 16
    val nameInput = EditText(context)
    nameInput.hint = "Enter Name"
    nameInput.setPadding(padding, padding, padding, padding)
    layout.addView(nameInput)

    val descriptionInput = EditText(context)
    descriptionInput.hint = "Enter Description"
    descriptionInput.setPadding(padding, padding, padding, padding)
    layout.addView(descriptionInput)

    // For simplicity, we are not adding input fields for attributes, metaData, and stops
    // You can add them in a similar way if needed
//
    val dialog = AlertDialog.Builder(context)
        .setTitle("Enter Trip Profile Information")
        .setView(layout)
        .setPositiveButton("OK") { _, _ ->
            val name = if (nameInput.text?.toString().isNullOrEmpty()) {
                "test name"
            } else {
                nameInput.text.toString()
            }
            val description = if (descriptionInput.text?.toString().isNullOrEmpty()) {
                "test description"
            } else {
                descriptionInput.text.toString()
            }
            val profile = TripUpdateProfile(name = name, description = description)

            // Set attributes, metaData, and stops here if needed
            callback(profile)
        }
        .setNegativeButton("Cancel", null)
        .create()

    dialog.show()
}