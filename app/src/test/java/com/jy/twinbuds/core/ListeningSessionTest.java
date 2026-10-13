package com.jy.twinbuds.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class ListeningSessionTest {
    @Test public void successfulFeedbackExpiresOnHeadsetChange() {
        ListeningSession session = new ListeningSession();
        session.start("A+B");
        assertEquals(ListeningSession.Phase.PLAYING, session.phase());
        session.finish("A+B");
        assertTrue(session.report(ListeningSession.Outcome.BOTH_HEARD, "A+B"));
        assertEquals(ListeningSession.Outcome.BOTH_HEARD, session.outcome());
        session.topologyChanged("A+C");
        assertEquals(ListeningSession.Outcome.UNTESTED, session.outcome());
    }

    @Test public void cannotConfirmBeforeSoundFinishes() {
        ListeningSession session = new ListeningSession();
        assertFalse(session.report(ListeningSession.Outcome.BOTH_HEARD, "A+B"));
        session.start("A+B");
        assertFalse(session.report(ListeningSession.Outcome.BOTH_HEARD, "A+B"));
    }

    @Test public void changeDuringPlaybackInvalidatesPendingDialog() {
        ListeningSession session = new ListeningSession();
        session.start("A+B");
        session.finish("A+C");
        assertFalse(session.report(ListeningSession.Outcome.BOTH_HEARD, "A+C"));
        assertEquals(ListeningSession.Phase.IDLE, session.phase());
    }

    @Test public void leavingActivityOrCancellingDoesNotKeepSuccess() {
        ListeningSession session = new ListeningSession();
        session.start("A+B");
        session.finish("A+B");
        assertTrue(session.report(ListeningSession.Outcome.ONE_HEARD, "A+B"));
        session.cancel();
        assertEquals(ListeningSession.Outcome.UNTESTED, session.outcome());
        assertFalse(session.report(ListeningSession.Outcome.BOTH_HEARD, "A+B"));
    }

    @Test public void stablePollDoesNotCancelActiveSession() {
        ListeningSession session = new ListeningSession();
        session.start("A+B");
        session.topologyChanged("A+B");
        assertEquals(ListeningSession.Phase.PLAYING, session.phase());
    }
    @Test public void eachNewTestStartsWithoutPreviousFeedback() {
        ListeningSession session = new ListeningSession();
        session.start("A+B");
        session.finish("A+B");
        session.report(ListeningSession.Outcome.BOTH_HEARD, "A+B");
        session.start("A+B");
        assertEquals(ListeningSession.Outcome.UNTESTED, session.outcome());
    }
}
