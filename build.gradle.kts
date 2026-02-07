//Copyright (c) [2026] [eason204646]
//[purelyplayer] is licensed under Mulan PSL v2.
//You can use this software according to the terms and conditions of the Mulan
//PSL v2.
//You may obtain a copy of Mulan PSL v2 at:
//         http://license.coscl.org.cn/MulanPSL2
//THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY
//KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
//NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
//See the Mulan PSL v2 for more details.
//
//Mulan Permissive Software License，Version 2
//
//Mulan Permissive Software License，Version 2 (Mulan PSL v2)
//
//January 2020 http://license.coscl.org.cn/MulanPSL2
// 直接在 plugins 块中定义，不使用 libs 别名
plugins {
    id("com.android.application") version "8.7.2" apply false
    // 🚩 修复：将 2.3.0 改为 2.1.0（这是目前的真实最新版）
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    // 🚩 修复：Compose 编译器插件版本必须与 Kotlin 版本严格一致
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
    // 🚩 修复：KSP 版本前缀也必须是 2.1.0
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
}