package com.hrv.backend.hrv;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Educational reference endpoint — no auth required.
 * Returns structured info about HRV metrics, measurement protocols,
 * breathwork techniques and the Valsalva maneuver.
 */
@RestController
@RequestMapping("/api/hrv/info")
@Tag(name = "HRV Education", description = "Reference info — metrics, breathwork, protocols (public)")
public class HrvInfoController {

    @GetMapping
    @Operation(summary = "Overview of HRV and all available reference topics")
    public Map<String, Object> overview() {
        return Map.of(
            "what_is_hrv", """
                Heart Rate Variability (HRV) is the variation in time between consecutive heartbeats
                (RR intervals). Despite the name it does NOT mean an irregular heart rhythm —
                higher variability is generally a sign of a healthy, adaptable autonomic nervous
                system (ANS). HRV reflects the balance between sympathetic ('fight-or-flight')
                and parasympathetic ('rest-and-digest') branches of the ANS.
                A higher resting RMSSD typically correlates with better recovery, lower stress,
                and greater cardiovascular fitness.""",
            "topics", List.of("metrics", "breathwork", "protocols", "valsalva")
        );
    }

    @GetMapping("/metrics")
    @Operation(summary = "Explanation of all tracked HRV metrics")
    public List<Map<String, String>> metrics() {
        return List.of(
            metric("RMSSD",
                "Root Mean Square of Successive Differences",
                "ms",
                "The square root of the mean of squared differences between successive RR intervals. " +
                "The gold-standard short-term HRV metric. Reflects parasympathetic (vagal) tone. " +
                "Most consumer devices (Oura, WHOOP, Apple Watch) report this.",
                "Typical healthy adult: 20–80 ms. Athletes often 60–100+ ms."),

            metric("SDNN",
                "Standard Deviation of NN intervals",
                "ms",
                "Overall HRV across the full measurement window. Captures both sympathetic and " +
                "parasympathetic activity. Used in 24-hour Holter recordings for cardiac risk.",
                "24h SDNN: <50 ms = poor, 50–100 ms = moderate, >100 ms = good"),

            metric("pNN50",
                "Percentage of successive NN intervals differing by >50 ms",
                "%",
                "Proportion of consecutive RR intervals that differ by more than 50 ms. " +
                "Strong parasympathetic indicator. Correlates closely with RMSSD.",
                "Healthy adults: 5–40%"),

            metric("Mean RR",
                "Mean RR interval",
                "ms",
                "Average time between heartbeats. Inversely related to heart rate. " +
                "RR = 60000 / HR (bpm).",
                "Resting: ~600–1200 ms"),

            metric("Mean HR",
                "Mean Heart Rate",
                "bpm",
                "Average beats per minute during the measurement window.",
                "Resting: 50–90 bpm. Athletes often 40–60 bpm."),

            metric("LF Power",
                "Low-Frequency Power (0.04–0.15 Hz)",
                "ms²",
                "Spectral power in the low-frequency band. Reflects both sympathetic and " +
                "parasympathetic (baroreflex) activity. Rises with mental stress and standing.",
                "Context-dependent. Higher standing than supine."),

            metric("HF Power",
                "High-Frequency Power (0.15–0.40 Hz)",
                "ms²",
                "Spectral power in the high-frequency band — driven by respiratory sinus arrhythmia (RSA). " +
                "Pure parasympathetic / vagal marker. Rises with slow breathing (~6 breaths/min).",
                "Higher = more vagal tone / better recovery."),

            metric("LF/HF Ratio",
                "Sympathovagal balance index",
                "ratio",
                "Ratio of LF to HF power. Historically called 'sympathovagal balance' — " +
                "now considered an oversimplification but still useful as a context marker. " +
                "Rises with stress, drops with relaxation and deep breathing.",
                "Resting: 1–4. Drops toward 1 or below with parasympathetic dominance.")
        );
    }

    @GetMapping("/breathwork")
    @Operation(summary = "Breathwork protocols and their effect on HRV")
    public List<Map<String, Object>> breathwork() {
        return List.of(
            breathProtocol(
                "Resonance / Coherence Breathing",
                "PARASYMPATHETIC_BOOST",
                "Inhale 5s · Exhale 5s (= 6 breaths/min). Synchronises breathing with baroreflex " +
                "oscillation, maximising RSA and HF power. The single most effective technique " +
                "for acute HRV elevation.",
                Map.of("inhale_s", 5, "exhale_s", 5, "rate_bpm", 6, "hold_s", 0),
                "5–20 min daily. Ideal pre-sleep or pre-measurement.",
                "Dramatically raises RMSSD and HF power during and after session."
            ),
            breathProtocol(
                "Box Breathing (4-4-4-4)",
                "STRESS_RESET",
                "Inhale 4s · Hold 4s · Exhale 4s · Hold 4s. Used by Navy SEALs and first " +
                "responders to calm the ANS under pressure. Balances both branches.",
                Map.of("inhale_s", 4, "hold_in_s", 4, "exhale_s", 4, "hold_out_s", 4),
                "3–5 min. Excellent before high-stress events.",
                "Stabilises LF/HF ratio. Moderate RMSSD increase."
            ),
            breathProtocol(
                "4-7-8 Breathing",
                "PARASYMPATHETIC_BOOST",
                "Inhale 4s · Hold 7s · Exhale 8s. Extended exhale activates vagal brake. " +
                "Popularised by Dr Andrew Weil. Strong sleep and anxiety aid.",
                Map.of("inhale_s", 4, "hold_s", 7, "exhale_s", 8),
                "4 cycles max initially. Increase gradually.",
                "Sharp RMSSD rise post-practice. Significant HF power increase."
            ),
            breathProtocol(
                "Wim Hof Method",
                "SYMPATHETIC_ACTIVATION",
                "30–40 deep power breaths → breath retention on empty lungs → recovery breath hold. " +
                "Temporarily alkalises blood, suppresses CO₂, triggers sympathetic surge followed " +
                "by parasympathetic rebound. HRV dips during hyperventilation, spikes in recovery.",
                Map.of("cycles", 3, "power_breaths", 30, "retention_target_s", 60),
                "Morning, empty stomach. Never near water.",
                "Acute HRV drop during retention, then marked rebound. Long-term practitioners " +
                "show elevated baseline RMSSD."
            ),
            breathProtocol(
                "Physiological Sigh",
                "FAST_CALM",
                "Double inhale through nose (first full, second tops up lungs) → long slow exhale " +
                "through mouth. Deflates alveoli, dumps CO₂ rapidly. Researched by Huberman Lab / " +
                "Stanford — fastest single breath for acute stress reduction.",
                Map.of("double_inhale", true, "exhale_extended", true),
                "1–3 repetitions on demand — no sustained practice needed.",
                "Rapid HRV stabilisation within 30 s. Practical for in-meeting use."
            ),
            breathProtocol(
                "Alternate Nostril (Nadi Shodhana)",
                "BALANCE",
                "Close right nostril, inhale left · Close left, exhale right · Inhale right · " +
                "Exhale left = 1 cycle. Balances hemispheric activity, calms the ANS.",
                Map.of("inhale_s", 4, "exhale_s", 4, "sides", "alternating"),
                "5–10 min. Good as morning or pre-session ritual.",
                "Moderate RMSSD increase. Strong LF/HF ratio normalisation."
            ),
            breathProtocol(
                "Valsalva Maneuver",
                "DIAGNOSTIC",
                "Forced exhalation against a closed airway (like blowing against pinched nose). " +
                "Creates 4 distinct ANS phases measurable in HRV. Used clinically to assess " +
                "autonomic function and baroreflex sensitivity. See /api/hrv/info/valsalva.",
                Map.of("exhale_pressure_mmhg", 40, "duration_s", 15),
                "Clinical / research use. Not a daily practice.",
                "Phase II: sharp HR rise + HRV drop. Phase IV: HR overshoot + RMSSD spike."
            )
        );
    }

    @GetMapping("/valsalva")
    @Operation(summary = "Deep-dive: Valsalva maneuver and its 4 HRV phases")
    public Map<String, Object> valsalva() {
        return Map.of(
            "name", "Valsalva Maneuver",
            "description", """
                The Valsalva maneuver is a forced expiratory effort against a closed glottis or
                pinched nose, typically at ~40 mmHg for 15 seconds. It is the most informative
                single test of autonomic cardiovascular regulation and produces four distinct
                haemodynamic phases that are clearly visible in HRV recordings.""",
            "clinical_use", List.of(
                "Autonomic neuropathy assessment (e.g. diabetic)",
                "Baroreflex sensitivity measurement",
                "Cardiac vagal function testing",
                "Distinguishing vasovagal syncope causes",
                "Research: HRV response quantification"
            ),
            "phases", List.of(
                Map.of(
                    "phase", "I — Onset (0–2 s)",
                    "mechanism", "Mechanical compression of the aorta by raised intrathoracic pressure.",
                    "hrv_effect", "Brief BP rise → reflex HR slowing → transient RMSSD spike."
                ),
                Map.of(
                    "phase", "II — Strain (2–15 s)",
                    "mechanism", "Reduced venous return → falling CO and BP → baroreflex-driven tachycardia.",
                    "hrv_effect", "HR rises sharply, RMSSD and HF power fall markedly. LF/HF rises."
                ),
                Map.of(
                    "phase", "III — Release onset (15–16 s)",
                    "mechanism", "Sudden drop in intrathoracic pressure, blood rushes into pulmonary vasculature.",
                    "hrv_effect", "Brief further HR rise and BP drop. HRV at its lowest point."
                ),
                Map.of(
                    "phase", "IV — Overshoot (16–30 s)",
                    "mechanism", "Restored venous return surges CO → BP overshoot above baseline → strong vagal reflex.",
                    "hrv_effect", "HR drops rapidly (the 'overshoot bradycardia'). RMSSD spikes — this is " +
                                  "the key metric: Valsalva Ratio = max Phase IV HR / min Phase II HR. " +
                                  "Normal ≥ 1.21. Values < 1.1 indicate autonomic dysfunction."
                )
            ),
            "how_to_perform", Map.of(
                "equipment", "HRV sensor (chest strap preferred), manometer or resistance device",
                "protocol", List.of(
                    "Rest supine for 5 min — record baseline HRV",
                    "Take a normal breath in",
                    "Exhale forcefully against closed glottis (or pinch nose + close mouth)",
                    "Maintain ~40 mmHg pressure for 15 s",
                    "Release and breathe normally — record 30 s recovery",
                    "Repeat x3 with 1–2 min rest between trials"
                ),
                "measure", "Valsalva Ratio = peak RR (Phase IV) ÷ shortest RR (Phase II)"
            ),
            "app_integration", """
                When logging a Valsalva session in the HRV API, submit readings for each phase
                with the 'notes' field set to 'valsalva_phase_1' through 'valsalva_phase_4'.
                The analytics/trend endpoint can then be filtered by device + notes prefix
                to track Valsalva Ratio longitudinally — a useful autonomic fitness marker."""
        );
    }

    @GetMapping("/protocols")
    @Operation(summary = "Measurement protocols (morning, orthostatic, 5-min)")
    public List<Map<String, Object>> protocols() {
        return List.of(
            Map.of(
                "name", "Morning Baseline (recommended daily)",
                "duration_min", 3,
                "position", "Supine",
                "timing", "Immediately on waking, before coffee or phone",
                "breathing", "Uncontrolled / natural",
                "notes", "Most reproducible. Used by Oura, WHOOP, Garmin for trend tracking."
            ),
            Map.of(
                "name", "5-Minute Standard (research-grade)",
                "duration_min", 5,
                "position", "Seated or supine",
                "timing", "Any time, consistent conditions",
                "breathing", "Uncontrolled or paced at 0.1 Hz (resonance)",
                "notes", "Minimum window for reliable frequency-domain analysis (LF/HF)."
            ),
            Map.of(
                "name", "Orthostatic Test",
                "duration_min", 10,
                "position", "Supine 5 min → Stand 5 min",
                "timing", "Morning",
                "breathing", "Uncontrolled",
                "notes", "The HRV drop on standing reflects autonomic reactivity. " +
                          "Log two separate readings with notes 'orthostatic_supine' and 'orthostatic_standing'. " +
                          "Reduced standing RMSSD = good sympathetic reactivity."
            ),
            Map.of(
                "name", "Post-Breathwork",
                "duration_min", 3,
                "position", "Seated",
                "timing", "Immediately after breathwork session",
                "breathing", "Uncontrolled recovery",
                "notes", "Quantifies the parasympathetic rebound of your practice. " +
                          "Compare with your morning baseline to measure acute effect size."
            )
        );
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Map<String, String> metric(String name, String fullName, String unit,
                                       String description, String reference) {
        return Map.of("name", name, "full_name", fullName, "unit", unit,
                      "description", description, "reference_range", reference);
    }

    private Map<String, Object> breathProtocol(String name, String category,
                                                String description, Object pattern,
                                                String dosage, String hrvEffect) {
        return Map.of("name", name, "category", category, "description", description,
                      "pattern", pattern, "dosage", dosage, "hrv_effect", hrvEffect);
    }
}
