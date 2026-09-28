package com.nannyapp.util

/** Only transport failures may use private offline data; authorization/server errors remain errors. */
fun Resource.Error.canUseOfflineCache(): Boolean = code == null && throwable is java.io.IOException
