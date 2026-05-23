package com.example.urisis_android.app

import android.app.Application
import com.example.urisis_android.urinalysis.FuzzyKnnClassifier
import com.example.urisis_android.urinalysis.TrainedModel

/**
 * App-wide singleton bootstrap. Lifecycle:
 *
 *   onCreate() runs once on app launch
 *     │
 *     ├─ Always: instantiate AppModeRepository (cheap)
 *     │
 *     ├─ PRODUCTION mode: load the frozen TrainedModel from res/raw/
 *     │                   and wrap it in a ready-to-use classifier
 *     │
 *     └─ TRAINING mode:   skip model loading entirely. The classifier
 *                         won't exist until you finish a training run
 *                         and rebuild with the new JSON.
 *
 * Access from anywhere via:
 *   (applicationContext as UrinoApplication).classifier
 *
 * In Training Mode that field is null — callers must check, or guard
 * their entry points via AppModeRepository.isTraining() first.
 */
class UrinoApplication : Application() {

    lateinit var appModeRepository: AppModeRepository
        private set

    /** Null in Training Mode (model may not exist yet). */
    var classifier: FuzzyKnnClassifier? = null
        private set

    /** Null in Training Mode. Useful for showing CV accuracy in the About screen. */
    var trainedModelMetadata: TrainedModel.TrainingMetadata? = null
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        appModeRepository = AppModeRepository(this)

        when (appModeRepository.getMode()) {
            AppMode.PRODUCTION -> loadFrozenModel()
            AppMode.TRAINING   -> {
                // Defer model loading. If a trained_model.json happens to be
                // bundled, we can still load it for cross-checking during
                // lab work — but it's not required for Training Mode to
                // function.
                runCatching { loadFrozenModel() }
            }
        }
    }

    private fun loadFrozenModel() {
        val model = TrainedModel.load(this)
        classifier = FuzzyKnnClassifier.fromTrainedModel(model)
        trainedModelMetadata = model.trainingMetadata
    }

    companion object {
        lateinit var instance: UrinoApplication
            private set
    }
}