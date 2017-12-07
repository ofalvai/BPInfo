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

internal interface RouteContract {
    
    companion object {

        const val ROUTE_ID = "id"

        const val ROUTE_SHORT_NAME = "shortName"

        const val ROUTE_LONG_NAME = "longName"

        const val ROUTE_DESC = "description"

        const val ROUTE_TYPE = "type"

        const val ROUTE_COLOR = "color"

        const val ROUTE_TEXT_COLOR = "textColor"
    }
}