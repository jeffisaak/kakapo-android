package com.aptasystems.kakapo.event;

public class ScrollToResponse {

    private int _position;

    public ScrollToResponse(int position) {
        _position = position;
    }

    public int getPosition() {
        return _position;
    }
}
