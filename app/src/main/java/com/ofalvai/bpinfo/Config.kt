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

import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

object Config {

    const val FUTAR_API_BASE_URL = "http://futar.bkk.hu/bkk-utvonaltervezo-api/ws/otp/api/where/"

    /**
     * Time after the alert list is considered outdated when the user returns to the activity
     */
    const val REFRESH_THRESHOLD_SEC = 30

    /**
     * Time before an alert is considered "recent".
     */
    const val ALERT_RECENT_THRESHOLD_HOURS = 24

    val FORMATTER_TIME: DateTimeFormatter = DateTimeFormat.forPattern("HH:mm")

    val FORMATTER_DATE: DateTimeFormatter = DateTimeFormat.forPattern("MMM d. EEEE ")

    val FORMATTER_DATE_YEAR: DateTimeFormatter = DateTimeFormat.forPattern("YYYY MMM d. EEEE ")

    const val DATE_SEPARATOR = " ➔ "

    const val SOURCE_CODE_URL = "https://github.com/ofalvai/BPInfo"

    const val BACKEND_URL = BuildConfig.BACKEND_URL

    const val PRIVACY_POLICY_URL = BACKEND_URL + "/privacy.html"

    const val BACKEND_NOTICE_PATH = "notices.json"
}
