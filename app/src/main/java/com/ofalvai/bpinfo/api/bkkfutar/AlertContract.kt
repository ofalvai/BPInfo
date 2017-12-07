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

package com.ofalvai.bpinfo.api.bkkfutar

internal interface AlertContract {

    companion object {

        const val ALERT_ID = "id"

        const val ALERT_START = "start"

        const val ALERT_END = "end"

        const val ALERT_TIMESTAMP = "timestamp"

        const val ALERT_ROUTE_IDS = "routeIds"

        const val ALERT_URL = "url"

        const val ALERT_HEADER = "header"

        const val ALERT_HEADER_TRANSLATIONS = "translations"

        const val ALERT_DESC = "description"

        const val ALERT_DESC_TRANSLATIONS = "translations"
    }
}
