package com.slothiesmooth.nyx.shared.composetestsupport

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module

/**
 * Runs a Compose UI test with a fresh Koin started from [module] (defaults to the shared
 * [testInfrastructureModule]), renders [content], then runs [assertions] against the composition.
 * Koin is stopped afterward so tests do not leak global state.
 *
 * Feature UI tests live in `iosTest` (macOS-gated) per the testing strategy; this harness is the
 * seam they call. A test needing the DB passes `module = testInfrastructureModule() + dbModule`
 * where `dbModule` builds `VaultSqlSource` over `createTestSqlDriver(NyxDb.Schema.synchronous())`.
 */
@OptIn(ExperimentalTestApi::class)
fun runFeatureUiTest(
    module: Module = testInfrastructureModule(),
    content: @Composable () -> Unit,
    assertions: ComposeUiTest.() -> Unit,
) = runComposeUiTest {
    startKoin { modules(module) }
    try {
        setContent { content() }
        assertions()
    } finally {
        stopKoin()
    }
}
