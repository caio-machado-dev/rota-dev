package dev.rotadev.sources.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.rotadev.profile.Confidence;
import dev.rotadev.profile.Evidence;
import dev.rotadev.profile.SkillSignal;

class LinkedInAnalysisTest {

    @Test
    void positionsBecomeExperienceAndRaiseSkillConfidence() {
        var data = new LinkedInData(
                List.of(new LinkedInData.Position("Acme", "Engenheiro Backend", "APIs em Python com FastAPI e PostgreSQL", "Mar 2023", "")),
                List.of("Python", "Kubernetes"),
                List.of(new LinkedInData.Certification("AWS Cloud Practitioner", "Amazon Web Services", "Jan 2025")));

        var result = LinkedInAnalysis.analyze(data);

        assertThat(result.evidences()).extracting(Evidence::kind).containsExactly("EXPERIENCE", "CERTIFICATION");
        assertThat(result.evidences().getFirst().title()).isEqualTo("Engenheiro Backend · Acme");
        assertThat(result.evidences().getFirst().detail()).isEqualTo("Mar 2023 – atual");
        assertThat(result.skills()).filteredOn(s -> s.name().equals("Python")).extracting(SkillSignal::confidence)
                .containsExactly(Confidence.MEDIUM);
        assertThat(result.skills()).filteredOn(s -> s.name().equals("Kubernetes")).extracting(SkillSignal::confidence)
                .containsExactly(Confidence.LOW);
    }
}
