package com.rushit.app

import androidx.activity.ComponentActivity
import com.rushit.app.data.auth.google.ForegroundActivityTracker
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ForegroundActivityTrackerTest {
    @Test
    fun `recreation rebinds the resumed host and never retains the destroyed Activity`() {
        val tracker = ForegroundActivityTracker()
        val firstController = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val first = firstController.get()
        tracker.bind(first)
        assertSame(first, tracker.activity)

        firstController.pause()
        assertNull(tracker.activity)
        val secondController = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val second = secondController.get()
        tracker.bind(second)
        assertSame(second, tracker.activity)

        firstController.stop().destroy()
        assertSame("destroying the old Activity must not clear the new host", second, tracker.activity)
        secondController.pause().stop().destroy()
        assertNull(tracker.activity)
    }

    @Test
    fun `backgrounding the app removes the foreground credential host`() {
        val tracker = ForegroundActivityTracker()
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        tracker.bind(controller.get())
        controller.pause()
        assertNull(tracker.activity)
        controller.resume()
        assertSame(controller.get(), tracker.activity)
    }
}
