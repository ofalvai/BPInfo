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

package com.ofalvai.bpinfo.api.bkkfutar;

interface AlertContract {

    String ALERT_ID = "id";

    String ALERT_START = "start";

    String ALERT_END = "end";

    String ALERT_TIMESTAMP = "timestamp";

    String ALERT_ROUTE_IDS = "routeIds";

    String ALERT_URL = "url";

    String ALERT_HEADER = "header";

    String ALERT_HEADER_TRANSLATIONS = "translations";

    String ALERT_DESC = "description";

    String ALERT_DESC_TRANSLATIONS = "translations";
}
