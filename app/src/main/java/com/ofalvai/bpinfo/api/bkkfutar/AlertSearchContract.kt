/*
 * Copyright 2018 Olivér Falvai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ofalvai.bpinfo.api.bkkfutar

interface AlertSearchContract {

    companion object {

        const val BASE_URL = "http://futar.bkk.hu/bkk-utvonaltervezo-api/ws/otp/api/where/"

        const val API_ENDPOINT = "alert-search.json"

        const val DATA = "data"

        const val DATA_ENTRY = "entry"

        const val DATA_ENTRY_ALERT_IDS = "alertIds"

        const val DATA_REFERENCES = "references"

        const val DATA_REFERENCES_ALERTS = "alerts"

        const val DATA_REFERENCES_ROUTES = "routes"

        // Languages and translations throughout the API fields:
        const val LANG_SOME = "someTranslation"
    }

}
