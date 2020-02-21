package com.aptasystems.kakapo.event;

public class ShowResponseLayout {

    private Integer _itemPosition;
    private Long _itemRemoteId;

    public ShowResponseLayout(int itemPosition) {
        _itemPosition = itemPosition;
    }

    public ShowResponseLayout(Integer itemPosition, Long itemRemoteId) {
        _itemPosition = itemPosition;
        _itemRemoteId = itemRemoteId;
    }

    public Integer getItemPosition() {
        return _itemPosition;
    }

    public Long getItemRemoteId() {
        return _itemRemoteId;
    }
}
