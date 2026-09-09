MS ANGLE ANGEL - CORRECTED CLAUDE CODE ASSET PACK

Source: Recovered PNG assets from Paralleye presentation material, corrected
for corner-emergence direction to match the approved PARALLEYE Technical
Methodology, Chapter 10 (Adaptive Alert Engine and Ms Angle Angel Behaviour).

CORRECTION APPLIED
The originally recovered assets showed Ms Angle Angel emerging from the
right side of the screen. This was verified by pixel analysis (character
skin-tone and shirt-color pixels were concentrated 67-95% on the right
half of the frame across sampled images). Chapter 10 requires the
opposite: "Ms Angle Angel shall always emerge from the upper-left corner
of the active application screen... shall not appear from the right
side."

All six corner-peel frames below have been horizontally mirrored to
correct this. They now show her emerging from the upper-left corner,
consistent with Chapter 10.

Note on the mirror fix: a horizontal flip is a fast, reliable correction
for on-screen position, but it also reverses any asymmetric details
(hair parting, which hand gestures). If new art is produced later,
render natively in the correct orientation rather than relying on a
mirrored asset long-term.

FILES - SIGNATURE CORNER-PEEL SEQUENCE (use in this order)
1. ms_angle_angel_01_idle_corrected.png       - Screen idle, no character visible
2. ms_angle_angel_02_peek_hand_corrected.png  - Corner curls, hand appears
3. ms_angle_angel_03_peek_face_corrected.png  - Face becomes visible
4. ms_angle_angel_04_peel_corrected.png       - Head and shoulders emerge
5. ms_angle_angel_05_popout_corrected.png     - Upper body fully visible
6. ms_angle_angel_06_ready_corrected.png      - Full character, ready gesture

Implementation note for Claude Code: use these six frames as the visual
reference for the corner-peel entrance animation described in Chapter 10.
Interpolate/animate smoothly between them while preserving character
appearance, proportions, expression, pink clothing and black bob
hairstyle. All frames now emerge from the upper-left corner of the
screen - do not mirror them back or otherwise change emergence side.

OTHER FILE (not related to corner-peel, no correction needed)
ms_angle_angel_posture_interaction_reference.png
  - Standalone "Phone too low / Move it up / Perfect" posture-teaching
    reference. Does not depict the corner-peel entrance, so the
    left/right correction above does not apply to it. Included as-is.
