package com.netkar.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BuildSmokeTest {
    @Test
    void runs_on_java_21_or_newer() {
        assertThat(Runtime.version().feature()).isGreaterThanOrEqualTo(21);
    }
}
