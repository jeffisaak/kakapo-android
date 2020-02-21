package com.aptasystems.kakapo.event;

public class UserAccountListModelChanged {

    private int _newItemCount;

    public UserAccountListModelChanged(int newItemCount) {
        _newItemCount = newItemCount;
    }

    public int getNewItemCount() {
        return _newItemCount;
    }
}
