package com.example.urisis_android.urinalysis

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Enhanced Fuzzy K-Nearest-Neighbours classifier.
 *
 * Reference:
 *   Keller, J.M., Gray, M.R., and Givens, J.A. (1985).
 *   "A Fuzzy K-Nearest Neighbor Algorithm."
 *   IEEE Transactions on Systems, Man, and Cybernetics, SMC-15(4).
 *
 * Tuned configuration for the Urisis training set (216 prototypes, 8 hydration
 * classes + abnormality flags):
 *   - k = 7           (query-time neighbours; ~25% of per-class population)
 *   - initK = 5       (soft-membership locality window)
 *   - fuzzifier = 2.0 (standard inverse-square distance weighting)
 *
 * Two stages:
 *
 *   1. **Soft-membership initialisation** (the "enhanced" part).
 *      Each training point gets a fuzzy membership in every class based
 *      on the labels of its own initK nearest neighbours:
 *        u_ij = 0.51 + 0.49 * (n_j / initK)   if class j is its own label
 *        u_ij =        0.49 * (n_j / initK)   otherwise
 *      where n_j = number of class-j neighbours among the initK nearest.
 *
 *      Crisp KNN gives every training point membership 1.0 in its own
 *      class and 0.0 elsewhere. The soft init lets points near class
 *      boundaries report partial overlap, which is closer to the truth
 *      for biological samples sitting between, say, "Yellow" and
 *      "Dark Yellow".
 *
 *   2. **Weighted query membership.**
 *      For a query x, find k nearest training points and compute:
 *        u_j(x) = Σ u_ij * w_i  /  Σ w_i
 *      where w_i = 1 / ||x - x_i||^(2/(m-1))
 *      and m > 1 is the fuzzifier (m=2 → inverse-square distance).
 */
class FuzzyKnnClassifier(
    private val training: List<TrainingSample>,
    private val k: Int = 7,
    private val initK: Int = 5,
    private val fuzzifier: Float = 2.0f,
) {

    init {
        require(training.isNotEmpty()) { "Training set must not be empty" }
        require(k > 0 && k <= training.size) { "Invalid K=$k for ${training.size} samples" }
        require(initK > 0 && initK < training.size) { "Invalid initK=$initK for ${training.size} samples" }
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

    /**
     * Margin-based confidence: gap between top-1 and top-2 hydration levels.
     * A dominant membership of 0.55 with runner-up 0.50 reflects a borderline
     * case (confidence = 0.05), while 0.70 / 0.15 is a strong decision
     * (confidence = 0.55). Far more informative than raw membership alone.
     */
    val confidence: Float = run {
        val sortedMemberships = UrineClass.levels
            .map { memberships[it] ?: 0f }
            .sortedDescending()
        val top = sortedMemberships.getOrElse(0) { 0f }
        val runnerUp = sortedMemberships.getOrElse(1) { 0f }
        (top - runnerUp).coerceIn(0f, 1f)
    }

    val confidenceTier: ConfidenceTier = when {
        confidence >= 0.40f -> ConfidenceTier.HIGH
        confidence >= 0.20f -> ConfidenceTier.MEDIUM
        else                -> ConfidenceTier.LOW
    }

    val activeFlags: List<UrineClass> =
        UrineClass.flags
            .filter { (memberships[it] ?: 0f) >= FLAG_THRESHOLD }
            .sortedByDescending { memberships[it] ?: 0f }

    data class Neighbour(
        val sample: TrainingSample,
        val distance: Float,
    )

    enum class ConfidenceTier { LOW, MEDIUM, HIGH }

    companion object {
        /**
         * A flag is reported as active when its membership clears 40%.
         * Tuned empirically: in the curated training set, healthy samples
         * land at <0.10 for all flags, while genuinely abnormal samples
         * cross 0.40+. Raised from 0.30 to reduce false-positive flags
         * on borderline-healthy queries.
         */
        const val FLAG_THRESHOLD = 0.40f
    }
}

// ──────────────────────────────────────────────────────────────────────────
//  K-Selection Harness
// ──────────────────────────────────────────────────────────────────────────

/**
 * 5-fold cross-validation grid search for the optimal K value.
 *
 * Sweeps k ∈ {3, 5, 7, 9, 11, 15} and reports the configuration that
 * maximises mean accuracy across folds. Run this once on the curated
 * training set; bake the winning K into [FuzzyKnnClassifier] as the
 * default.
 *
 * Usage:
 *   val result = KSelectionHarness.gridSearch(trainingSet)
 *   println(result.summary())
 *   // → "Optimal K=7, accuracy=89.4%, confidence=0.52"
 */
object KSelectionHarness {

    private val K_GRID = listOf(3, 5, 7, 9, 11, 15)
    private const val FOLDS = 5
    private const val RANDOM_SEED = 42L

    data class FoldMetrics(
        val k: Int,
        val accuracy: Float,
        val meanConfidence: Float,
        val meanCorrectConfidence: Float,
    )

    data class GridSearchResult(
        val perK: List<FoldMetrics>,
        val optimal: FoldMetrics,
    ) {
        fun summary(): String = buildString {
            appendLine("EF-KNN K-Selection Grid Search (5-fold CV)")
            appendLine("─".repeat(60))
            appendLine(" K  │ Accuracy │ Mean Conf │ Conf|Correct")
            appendLine("────┼──────────┼───────────┼─────────────")
            for (m in perK) {
                val marker = if (m.k == optimal.k) " ◀" else ""
                appendLine(
                    " %2d │  %5.1f%% │   %.3f   │    %.3f%s".format(
                        m.k,
                        m.accuracy * 100f,
                        m.meanConfidence,
                        m.meanCorrectConfidence,
                        marker,
                    )
                )
            }
            appendLine("─".repeat(60))
            appendLine(
                "Optimal K = ${optimal.k}  " +
                        "(acc=%.1f%%, conf=%.3f)".format(
                            optimal.accuracy * 100f, optimal.meanCorrectConfidence
                        )
            )
        }
    }

    fun gridSearch(
        dataset: List<TrainingSample>,
        initK: Int = 5,
        fuzzifier: Float = 2.0f,
    ): GridSearchResult {
        require(dataset.size >= FOLDS) {
            "Need at least $FOLDS samples for $FOLDS-fold CV, got ${dataset.size}"
        }

        val folds = stratifiedSplit(dataset, FOLDS, RANDOM_SEED)

        val metrics = K_GRID.map { k ->
            evaluateK(folds, k, initK, fuzzifier)
        }

        // Optimal = highest accuracy; tie-break by mean confidence on
        // correctly-classified samples (the "high confidence" objective).
        val optimal = metrics.maxWith(
            compareBy<FoldMetrics> { it.accuracy }
                .thenBy { it.meanCorrectConfidence }
        )

        return GridSearchResult(perK = metrics, optimal = optimal)
    }

    private fun evaluateK(
        folds: List<List<TrainingSample>>,
        k: Int,
        initK: Int,
        fuzzifier: Float,
    ): FoldMetrics {
        var totalCorrect = 0
        var totalSamples = 0
        var confSum = 0f
        var correctConfSum = 0f
        var correctCount = 0

        for (foldIdx in folds.indices) {
            val testFold = folds[foldIdx]
            val trainFold = folds.filterIndexed { i, _ -> i != foldIdx }.flatten()

            // Skip if k would exceed training fold size
            val effectiveK = k.coerceAtMost(trainFold.size - 1)
            val effectiveInitK = initK.coerceAtMost(trainFold.size - 1)

            val classifier = FuzzyKnnClassifier(
                training = trainFold,
                k = effectiveK,
                initK = effectiveInitK,
                fuzzifier = fuzzifier,
            )

            for (sample in testFold) {
                val result = classifier.classify(sample.features)
                val predicted = result.dominantLevel
                val isCorrect = predicted == sample.label

                totalSamples++
                confSum += result.confidence
                if (isCorrect) {
                    totalCorrect++
                    correctCount++
                    correctConfSum += result.confidence
                }
            }
        }

        return FoldMetrics(
            k = k,
            accuracy = totalCorrect.toFloat() / totalSamples,
            meanConfidence = confSum / totalSamples,
            meanCorrectConfidence =
                if (correctCount > 0) correctConfSum / correctCount else 0f,
        )
    }

    /**
     * Stratified k-fold split: each class is divided proportionally across
     * folds, so every fold sees every class. Critical for an 8-class
     * problem with only ~27 samples per class — random splitting risks
     * empty classes in some folds.
     */
    private fun stratifiedSplit(
        dataset: List<TrainingSample>,
        folds: Int,
        seed: Long,
    ): List<List<TrainingSample>> {
        val rng = java.util.Random(seed)
        val byClass = dataset.groupBy { it.label }
        val buckets = MutableList(folds) { mutableListOf<TrainingSample>() }

        for ((_, samples) in byClass) {
            val shuffled = samples.toMutableList().apply {
                // Fisher–Yates with seeded RNG for reproducibility
                for (i in size - 1 downTo 1) {
                    val j = rng.nextInt(i + 1)
                    val tmp = this[i]; this[i] = this[j]; this[j] = tmp
                }
            }
            for ((idx, sample) in shuffled.withIndex()) {
                buckets[idx % folds].add(sample)
            }
        }

        return buckets
    }
}