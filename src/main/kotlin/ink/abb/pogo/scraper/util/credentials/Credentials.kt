package ink.abb.pogo.scraper.util.credentials

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
        JsonSubTypes.Type(value = GoogleCredentials::class, name = "google"),
        JsonSubTypes.Type(value = GoogleAutoCredentials::class, name = "google-auto"),
        JsonSubTypes.Type(value = PtcCredentials::class, name = "ptc")
)
@JsonIgnoreProperties(ignoreUnknown = true)
interface Credentials
