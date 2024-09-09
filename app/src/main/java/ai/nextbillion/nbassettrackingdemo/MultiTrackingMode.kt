package ai.nextbillion.nbassettrackingdemo

/**
 * @author qiuyu
 * @Date 2023/12/26
 **/
internal enum class MultiTrackingMode(val value:Int) {
    ACTIVE(0),

    BALANCED(1),

    PASSIVE(2),

    TIME_INTERVAL(3),

    DISTANCE_INTERVAL(4),
}