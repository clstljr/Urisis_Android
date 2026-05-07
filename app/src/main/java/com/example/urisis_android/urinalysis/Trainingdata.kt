package com.example.urisis_android.urinalysis

/**
 * One labelled training point for the Fuzzy KNN classifier.
 */
data class TrainingSample(
    val features: FeatureVector,
    val label: UrineClass,
    val description: String,
)

/**
 * Hand-curated training set derived from the Silver Swan classification
 * table. Each row of the table is expanded into multiple prototypes that
 * cover the colour, pH, and TDS-derived SG range described for that class.
 *
 * Notes on construction:
 *
 *  - "Pale / Transparent" (Level 1) ~ (245, 245, 230)  — low sat, high val.
 *  - "Pale Yellow"       (Level 2) ~ (250, 240, 180).
 *  - "Yellow / Dark Yellow" (Level 3-4) ~ (230, 200, 100) and (210, 180, 80).
 *  - "Amber / Dark Amber"   (Level 5-6) ~ (200, 140, 50) and (170, 110, 30).
 *  - "Deep Amber / Honey"   (Level 7-8) ~ (160, 90, 20).
 *  - Flag A spans red ~ (180, 40, 40), green ~ (60, 150, 60), brown ~ (110, 60, 30).
 *  - Flag B / C samples cover varied SG and varied colour because pH is
 *    the only discriminator — adding mixed-colour prototypes lets the
 *    classifier flag alkaline/acidic across any hydration state.
 */
object TrainingData {

    private fun sample(
        pH: Float, tdsPpm: Float, r: Int, g: Int, b: Int,
        label: UrineClass, description: String,
    ): TrainingSample = TrainingSample(
        features = buildFeatures(pH, tdsPpm, r, g, b),
        label = label,
        description = description,
    )

    val samples: List<TrainingSample> = listOf(

        // ---- Level 1: Overhydrated  (SG < 1.005, pale/transparent) ----
        sample(6.5f, 50f,  245, 245, 230, UrineClass.LEVEL_1, "Pale, transparent, low TDS"),
        sample(7.0f, 80f,  250, 250, 240, UrineClass.LEVEL_1, "Near-water clarity"),
        sample(5.5f, 100f, 240, 240, 225, UrineClass.LEVEL_1, "Pale, slightly acidic"),

        // ---- Level 2: Well Hydrated  (SG 1.005–1.010, pale yellow) ----
        sample(6.5f, 200f, 250, 240, 180, UrineClass.LEVEL_2, "Pale yellow, normal pH"),
        sample(6.0f, 250f, 248, 235, 170, UrineClass.LEVEL_2, "Pale yellow"),
        sample(7.2f, 220f, 245, 235, 175, UrineClass.LEVEL_2, "Pale yellow, mildly alkaline"),

        // ---- Level 3-4: Minimal Dehydration  (SG 1.010–1.020, yellow) ----
        sample(6.5f, 400f, 230, 200, 100, UrineClass.LEVEL_3_4, "Yellow"),
        sample(6.0f, 500f, 220, 195, 95,  UrineClass.LEVEL_3_4, "Yellow / mid-yellow"),
        sample(7.0f, 450f, 210, 180, 80,  UrineClass.LEVEL_3_4, "Dark yellow"),

        // ---- Level 5-6: Significant Dehydration  (SG 1.020–1.030, amber) ----
        sample(6.0f, 700f, 200, 140, 50,  UrineClass.LEVEL_5_6, "Amber"),
        sample(6.5f, 750f, 185, 125, 40,  UrineClass.LEVEL_5_6, "Amber / dark amber"),
        sample(5.8f, 800f, 170, 110, 30,  UrineClass.LEVEL_5_6, "Dark amber"),

        // ---- Level 7-8: Serious Dehydration  (SG > 1.030, deep amber/honey) ----
        sample(6.0f, 950f,  160, 90,  20, UrineClass.LEVEL_7_8, "Deep amber / honey"),
        sample(5.5f, 1050f, 150, 80,  15, UrineClass.LEVEL_7_8, "Honey, concentrated"),
        sample(6.5f, 1100f, 140, 75,  10, UrineClass.LEVEL_7_8, "Deep honey"),

        // ---- Flag A: Abnormal Color  (any SG, any pH, RED/GREEN/BROWN) ----
        sample(6.5f, 400f, 180, 40,  40,  UrineClass.FLAG_A, "Red — possible hematuria"),
        sample(6.0f, 600f, 150, 30,  50,  UrineClass.FLAG_A, "Reddish-brown — old blood"),
        sample(7.0f, 350f, 60,  150, 60,  UrineClass.FLAG_A, "Green — biliverdin / drug"),
        sample(6.5f, 500f, 80,  130, 70,  UrineClass.FLAG_A, "Greenish"),
        sample(6.0f, 600f, 110, 60,  30,  UrineClass.FLAG_A, "Brown — bilirubin / liver"),
        sample(6.5f, 700f, 90,  55,  35,  UrineClass.FLAG_A, "Dark brown"),

        // ---- Flag B: Abnormal Alkaline  (pH > 8.0) ----
        sample(8.5f, 200f, 250, 240, 180, UrineClass.FLAG_B, "Alkaline + pale yellow"),
        sample(9.0f, 500f, 220, 195, 95,  UrineClass.FLAG_B, "Alkaline + yellow"),
        sample(8.8f, 800f, 170, 110, 30,  UrineClass.FLAG_B, "Alkaline + amber"),
        sample(9.5f, 100f, 245, 245, 230, UrineClass.FLAG_B, "Alkaline + pale"),
        sample(8.2f, 950f, 160, 90,  20,  UrineClass.FLAG_B, "Alkaline + honey"),

        // ---- Flag C: Abnormal Acidic  (pH < 4.5) ----
        sample(4.0f, 200f, 250, 240, 180, UrineClass.FLAG_C, "Acidic + pale yellow"),
        sample(3.5f, 500f, 220, 195, 95,  UrineClass.FLAG_C, "Acidic + yellow"),
        sample(4.2f, 800f, 170, 110, 30,  UrineClass.FLAG_C, "Acidic + amber"),
        sample(3.8f, 100f, 245, 245, 230, UrineClass.FLAG_C, "Acidic + pale"),
        sample(4.4f, 950f, 160, 90,  20,  UrineClass.FLAG_C, "Acidic + honey"),
    )
}