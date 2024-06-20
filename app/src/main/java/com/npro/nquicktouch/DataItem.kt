import kotlinx.serialization.Serializable

@Serializable
data class DataItem(
    val path: String,
    val icon: String,
    val name: String
)

