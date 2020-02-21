package com.aptasystems.kakapo.event;

public class ContentStreamProgress {

    private long _current;
    private long _max;

    public ContentStreamProgress(long current, long max) {
        _current = current;
        _max = max;
    }

    public float getPercentComplete()
    {
        return (float) _current / (float) _max;
    }

    public long getCurrent() {
        return _current;
    }

    public long getMax() {
        return _max;
    }
}
