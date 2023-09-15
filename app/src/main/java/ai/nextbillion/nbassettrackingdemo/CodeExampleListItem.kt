package ai.nextbillion.nbassettrackingdemo

import androidx.appcompat.app.AppCompatActivity

/**
 * Copyright © NextBillion.ai. All rights reserved.
 * Use of this source code is governed by license that can be found in the LICENSE file.
 */
data class CodeExampleListItem(
    var name: String = "",
    var description: String = "",
    var activityClass: Class<out AppCompatActivity>? = null
)