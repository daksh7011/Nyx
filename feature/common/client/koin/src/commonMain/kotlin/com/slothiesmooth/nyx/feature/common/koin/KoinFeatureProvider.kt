package com.slothiesmooth.nyx.feature.common.koin

import androidx.compose.runtime.Composable
import com.slothiesmooth.nyx.feature.common.api.BaseFeatureProvider
import org.koin.compose.KoinIsolatedContext
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * A feature provider with its own isolated Koin graph. Subclasses re-register (in [onProvideDI]) each
 * outer dependency they received in their constructor plus their own repositories/use-cases/ViewModels;
 * screens run inside [withDI] and resolve through [koinFeatureViewModel].
 */
abstract class KoinFeatureProvider : BaseFeatureProvider() {

    protected val koinApp: KoinApplication by lazy {
        koinApplication(createEagerInstances = false) { modules(module { onProvideDI() }) }
    }

    @Composable
    protected fun withDI(context: KoinApplication? = null, content: @Composable () -> Unit) {
        KoinIsolatedContext(context ?: koinApp, content)
    }

    protected open fun Module.onProvideDI() = Unit
}
