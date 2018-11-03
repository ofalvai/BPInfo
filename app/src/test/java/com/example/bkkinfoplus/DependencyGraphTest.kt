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

package com.example.bkkinfoplus

import android.app.Application
import android.content.Context
import com.ofalvai.bpinfo.injection.allModules
import org.junit.Test
import org.koin.dsl.module.module
import org.koin.test.KoinTest
import org.koin.test.checkModules
import org.mockito.Mockito.mock

class DependencyGraphTest: KoinTest {

    @Test
    fun checkKoinModules() {
        val mockModule = module {
            single { mock(Application::class.java) }
            single { mock(Context::class.java) }
        }
        val modules = allModules + mockModule

        checkModules(modules)
    }
}