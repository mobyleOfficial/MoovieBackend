package org.mobyle.di

import io.ktor.server.routing.Route
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.java.KoinJavaComponent.inject

// Fix for Koin `inject` for Ktor 3.0.3 using Koin 4.0.4 in Routes (e.g. in Routing.kt)
inline fun <reified T : Any> Route.injection(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
) =
    lazy {
        GlobalContext.getKoinApplicationOrNull()?.koin?.get<T>(qualifier, parameters) ?:
        inject<T>(T::class.java).value
    }
