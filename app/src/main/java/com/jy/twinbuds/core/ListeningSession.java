package com.jy.twinbuds.core;

/** 用户反馈仅属于当前这次已完整播放、连接未变化的试听。 */
public final class ListeningSession {
    public enum Phase { IDLE, PLAYING, AWAITING_FEEDBACK, REPORTED }
    public enum Outcome { UNTESTED, BOTH_HEARD, ONE_HEARD, NONE_HEARD }
    private Phase phase = Phase.IDLE;
    private Outcome outcome = Outcome.UNTESTED;
    private String topology = "";

    public void start(String topology) {
        this.topology = topology;
        phase = Phase.PLAYING;
        outcome = Outcome.UNTESTED;
    }

    public void finish(String currentTopology) {
        topologyChanged(currentTopology);
        if (phase == Phase.PLAYING) phase = Phase.AWAITING_FEEDBACK;
    }

    public boolean report(Outcome value, String currentTopology) {
        topologyChanged(currentTopology);
        if (phase != Phase.AWAITING_FEEDBACK || value == null || value == Outcome.UNTESTED) return false;
        outcome = value;
        phase = Phase.REPORTED;
        return true;
    }

    public void topologyChanged(String currentTopology) {
        if (!topology.equals(currentTopology)) cancel();
    }

    public void cancel() {
        phase = Phase.IDLE;
        outcome = Outcome.UNTESTED;
        topology = "";
    }
    public Phase phase() { return phase; }
    public Outcome outcome() { return outcome; }
}
