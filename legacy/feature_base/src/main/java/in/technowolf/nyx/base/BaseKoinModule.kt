package `in`.technowolf.nyx.base

import `in`.technowolf.nyx.base.presentation.navigation.NavigationManager
import org.koin.dsl.module

val baseModule = module {
    single { NavigationManager() }
}
