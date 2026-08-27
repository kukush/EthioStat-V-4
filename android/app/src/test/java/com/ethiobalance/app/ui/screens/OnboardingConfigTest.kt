package com.ethiobalance.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingConfigTest {
    @Test
    fun onboardingPageCount_matchesExpectedFiveSlideFlow() {
        assertEquals(5, OnboardingConfig.TOTAL_PAGES)
    }

    @Test
    fun onboardingIsShownForFreshInstallAndHiddenAfterCompletion() {
        assertEquals(true, OnboardingConfig.shouldShowOnboarding(false))
        assertEquals(false, OnboardingConfig.shouldShowOnboarding(true))
    }
}
