package continuous

data class CtConnection(
    val fromNode: CtNode,
    val fromPort: String,
    val toNode: CtNode,
    val toPort: String
)
