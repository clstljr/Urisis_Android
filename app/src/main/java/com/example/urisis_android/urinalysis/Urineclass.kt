package com.example.urisis_android.urinalysis

/**
 * 8-class taxonomy from the Silver Swan classification table.
 *
 * Levels and Flags coexist: a sample can be both "Significant Dehydration"
 * AND "Abnormal Alkaline" simultaneously. The classifier returns
 * memberships for every class so the UI can surface all relevant labels.
 *
 * Citations from the source table:
 *   [1] Cleveland Clinic (2025)
 *   [2] National Library of Medicine (2025)
 *   [3] Hitzeman et al. (2022)
 *   [4] Rauf & Lestaluhu (2022)
 *   [5] So & Villanueva (2021)
 *   [6] Tamborino et al. (2024)
 */
enum class UrineClass(
    val displayName: String,
    val hydrationStatus: String,
    val clinicalNote: String,
    val isFlag: Boolean,
) {
    LEVEL_1(
        "Level 1",
        "Overhydrated",
        "Polydipsia, Renal Failure",
        isFlag = false,
    ),
    LEVEL_2(
        "Level 2",
        "Well Hydrated",
        "Normal physiological range",
        isFlag = false,
    ),
    LEVEL_3_4(
        "Level 3-4",
        "Minimal Dehydration",
        "Mild fluid deficit",
        isFlag = false,
    ),
    LEVEL_5_6(
        "Level 5-6",
        "Significant Dehydration",
        "Moderate to severe deficit",
        isFlag = false,
    ),
    LEVEL_7_8(
        "Level 7-8",
        "Serious Dehydration",
        "Severe Hypovolemia",
        isFlag = false,
    ),
    FLAG_A(
        "Flag A",
        "Abnormal (Color)",
        "Hematuria, Liver Disease",
        isFlag = true,
    ),
    FLAG_B(
        "Flag B",
        "Abnormal (Alkaline)",
        "UTI, Calcium Stones",
        isFlag = true,
    ),
    FLAG_C(
        "Flag C",
        "Abnormal (Acidic)",
        "Ketoacidosis, Uric Acid Stones",
        isFlag = true,
    );

    companion object {
        val levels: List<UrineClass> get() = entries.filter { !it.isFlag }
        val flags: List<UrineClass> get() = entries.filter { it.isFlag }
    }
}