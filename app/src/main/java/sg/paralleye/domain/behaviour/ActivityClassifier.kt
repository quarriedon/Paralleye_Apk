package sg.paralleye.domain.behaviour

import sg.paralleye.config.ActivityCategory

/** Ch.1 §12, Ch.6: platform-appropriate signals the classifier may use — no camera, mic, or content inspection. */
enum class ForegroundAppCategory { VIDEO_PLAYER, GAME, MESSAGING_OR_BROWSER, OTHER, UNKNOWN }

data class ActivityObservation(
    val keyboardActive: Boolean,
    val foregroundAppCategory: ForegroundAppCategory?,
    val continuousInteractionSeconds: Int,
)

enum class ClassificationConfidence { HIGH, MEDIUM, LOW }

data class ActivityClassificationResult(
    val category: ActivityCategory,
    val confidence: ClassificationConfidence,
    val evidenceSource: String,
)

/**
 * Ch.1 §12, Ch.2 §8-9, Ch.6: activity classification is modular and evidence-based. Where
 * evidence is insufficient the fallback is [ActivityCategory.UNKNOWN] — the system must not
 * fabricate a specific category (Ch.2 §9 "Unknown Activity Principle").
 *
 * Priority order (keyboard state is the strongest, least ambiguous signal available without
 * content inspection; foreground app category is used only when keyboard is inactive) is an
 * engineering choice — the source documents list the available signal types but don't specify
 * a priority order between them.
 */
object ActivityClassifier {
    fun classify(observation: ActivityObservation): ActivityClassificationResult = when {
        observation.keyboardActive -> ActivityClassificationResult(
            category = ActivityCategory.TYPING,
            confidence = ClassificationConfidence.HIGH,
            evidenceSource = "keyboard_active",
        )
        observation.foregroundAppCategory == ForegroundAppCategory.GAME -> ActivityClassificationResult(
            category = ActivityCategory.GAMING,
            confidence = ClassificationConfidence.MEDIUM,
            evidenceSource = "foreground_app_category",
        )
        observation.foregroundAppCategory == ForegroundAppCategory.VIDEO_PLAYER -> ActivityClassificationResult(
            category = ActivityCategory.VIDEO,
            confidence = ClassificationConfidence.MEDIUM,
            evidenceSource = "foreground_app_category",
        )
        observation.foregroundAppCategory == ForegroundAppCategory.MESSAGING_OR_BROWSER &&
            observation.continuousInteractionSeconds > 0 -> ActivityClassificationResult(
            category = ActivityCategory.SCROLLING,
            confidence = ClassificationConfidence.LOW,
            evidenceSource = "foreground_app_category+interaction",
        )
        else -> ActivityClassificationResult(
            category = ActivityCategory.UNKNOWN,
            confidence = ClassificationConfidence.LOW,
            evidenceSource = "insufficient_evidence",
        )
    }
}
