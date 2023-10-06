package `in`.technowolf.nyx.encryption

import `in`.technowolf.nyx.encryption.data.dataModule
import `in`.technowolf.nyx.encryption.domain.domainModule
import `in`.technowolf.nyx.encryption.presentation.presentationModule

val featureEncryptionModules = listOf(
    dataModule,
    domainModule,
    presentationModule,
)
