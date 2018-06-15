/*
 * Copyright 2016 Olivér Falvai
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.ofalvai.bpinfo

import org.threeten.bp.format.DateTimeFormatter


object Config {

    object Url {

        const val BACKEND = BuildConfig.BACKEND_URL

        private const val FIREBASE_BACKEND = "https://bpinfo-26f36.firebaseapp.com"

        const val PRIVACY_POLICY = "$FIREBASE_BACKEND/privacy.html"

        const val NOTICES = "$FIREBASE_BACKEND/notices.json"

        const val SOURCE_CODE = "https://github.com/ofalvai/BPInfo"

    }

    object Behavior {
        /**
         * Time after the alert list is considered outdated when the user returns to the activity
         */
        const val REFRESH_THRESHOLD_SEC = 30

        /**
         * Time before an alert is considered "recent".
         */
        const val ALERT_RECENT_THRESHOLD_HOURS = 24

        /**
         * ID of an alert that should open when clicking the test notification
         */
        const val TEST_NOTIFICATION_ALERT_ID = "BKK_bkkinfo-15346"
    }

    @JvmField
    val FORMATTER_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    @JvmField
    val FORMATTER_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d. EEEE ")

    @JvmField
    val FORMATTER_DATE_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("YYYY MMM d. EEEE ")

    const val DATE_SEPARATOR = " ➔ "
}
