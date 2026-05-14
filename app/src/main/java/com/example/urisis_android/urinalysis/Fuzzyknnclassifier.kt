package com.example.urisis_android.urinalysis

import kotlin.math.pow

/**
 * Enhanced Fuzzy K-Nearest-Neighbours classifier.
 *
 * Reference:
 *   Keller, J.M., Gray, M.R., and Givens, J.A. (1985).
 *   "A Fuzzy K-Nearest Neighbor Algorithm."
 *   IEEE Transactions on Systems, Man, and Cybernetics, SMC-15(4).
 *
 * Two stages:
 *
 *   1. **Soft-membership initialisation** (the "enhanced" part).
 *      Each training point gets a fuzzy membership in every class based
 *      on the labels of its own K nearest neighbours:
 *        u_ij = 0.51 + 0.49 * (n_j / K)   if class j is its own label
 *        u_ij =        0.49 * (n_j / K)   otherwise
 *      where n_j = number of class-j neighbours among the K nearest.
 *
 *      Crisp KNN gives every training point membership 1.0 in its own
 *      class and 0.0 elsewhere. The soft init lets points near class
 *      boundaries report partial overlap, which is closer to the truth
 *      for biological samples sitting between, say, "Yellow" and
 *      "Dark Yellow".
 *
 *   2. **Weighted query membership.**
 *      For a query x, find K nearest training points and compute:
 *        u_j(x) = Σ u_ij * w_i  /  Σ w_i
 *      where w_i = 1 / ||x - x_i||^(2/(m-1))
 *      and m > 1 is the fuzzifier (m=2 is the standard choice).
 */
class FuzzyKnnClassifier(
    private val training: List<TrainingSample>,
    private val k: Int = 5,
    private val initK: Int = 5,
    private val fuzzifier: Float = 2.0f,
) {

    init {
        require(training.isNotEmpty()) { "Training set must not be empty" }
        require(k > 0 && k <= training.size) { "Invalid K=$k for ${training.size} samples" }
        require(fuzzifier > 1.0f) { "Fuzzifier m must be > 1" }
    }

    /**
     * Per-training-sample membership across all classes.
     * Index parallel to [training]. Each inner map sums to ~1.0.
     */
    private val trainingMemberships: List<Map<UrineClass, Float>> =
        computeSoftMemberships()

    /** Classify a query feature vector. Returns memberships for every class. */
    fun classify(query: FeatureVector): ClassificationResult {

        data class Neighbour(val index: Int, val distance: Float)
        val neighbours = training.indices
            .map { Neighbour(it, query.distanceTo(training[it].features)) }
            .sortedBy { it.distance }
            .take(k)

        val totals = mutableMapOf<UrineClass, Float>()
        var weightSum = 0f

        for (n in neighbours) {
            val w = neighbourWeight(n.distance)
            weightSum += w
            val u = trainingMemberships[n.index]
            for ((cls, mem) in u) {
                totals[cls] = (totals[cls] ?: 0f) + mem * w
            }
        }

        val memberships: Map<UrineClass, Float> =
            if (weightSum > 0f) {
                totals.mapValues { (it.value / weightSum).coerceIn(0f, 1f) }
            } else {
                // Query coincided exactly with a training point — degenerate
                // but valid case. Fall back to that point's memberships.
                trainingMemberships[neighbours.first().index]
            }

        val complete: Map<UrineClass, Float> =
            UrineClass.entries.associateWith { memberships[it] ?: 0f }

        return ClassificationResult(
            memberships = complete,
            neighbours = neighbours.map { n ->
                ClassificationResult.Neighbour(
                    sample = training[n.index],
                    distance = n.distance,
                )
            }
        )
    }

    private fun neighbourWeight(distance: Float): Float {
        val exponent = 2.0 / (fuzzifier - 1.0)
        val safeDist = distance.coerceAtLeast(EPSILON)
        return (1.0 / safeDist.toDouble().pow(exponent)).toFloat()
    }

    private fun computeSoftMemberships(): List<Map<UrineClass, Float>> {
        val n = training.size
        val K = initK.coerceAtMost(n - 1).coerceAtLeast(1)

        return training.indices.map { i ->
            val self = training[i]

            data class D(val idx: Int, val d: Float)
            val nearest = (0 until n)
                .filter { it != i }
                .map { D(it, self.features.distanceTo(training[it].features)) }
                .sortedBy { it.d }
                .take(K)

            val counts = mutableMapOf<UrineClass, Int>()
            for (nb in nearest) {
                val cls = training[nb.idx].label
                counts[cls] = (counts[cls] ?: 0) + 1
            }

            UrineClass.entries.associateWith { cls ->
                val nj = counts[cls] ?: 0
                val frac = nj.toFloat() / K.toFloat()
                if (cls == self.label) {
                    0.51f + 0.49f * frac
                } else {
                    0.49f * frac
                }
            }
        }
    }

    companion object {
        private const val EPSILON = 1e-6f
    }
}

/**
 * Output of [FuzzyKnnClassifier.classify].
 *
 * Levels and Flags are reported separately because they're orthogonal:
 *  - exactly one Level is the most likely hydration state
 *  - any combination of Flags can be active (or none)
 */
data class ClassificationResult(
    val memberships: Map<UrineClass, Float>,
    val neighbours: List<Neighbour>,
) {

    val dominantLevel: UrineClass =
        UrineClass.levels.maxBy { memberships[it] ?: 0f }

    val dominantLevelMembership: Float = memberships[dominantLevel] ?: 0f

    val activeFlags: List<UrineClass> =
        UrineClass.flags
            .filter { (memberships[it] ?: 0f) >= FLAG_THRESHOLD }
            .sortedByDescending { memberships[it] ?: 0f }

    data class Neighbour(
        val sample: TrainingSample,
        val distance: Float,
    )

    companion object {
        /**
         * A flag is reported as active when its membership clears 30%.
         * Tuned empirically: in the curated training set, healthy
         * samples land at <0.10 for all flags, while genuinely abnormal
         * samples cross 0.40+.
         */
        const val FLAG_THRESHOLD = 0.30f
    }
}