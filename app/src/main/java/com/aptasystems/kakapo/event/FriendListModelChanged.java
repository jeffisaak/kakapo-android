package com.aptasystems.kakapo.event;

public class FriendListModelChanged {

    private int _newItemCount;

    public FriendListModelChanged(int newItemCount) {
        _newItemCount = newItemCount;
    }

    public int getNewItemCount() {
        return _newItemCount;
    }
}
