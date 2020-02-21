package com.aptasystems.kakapo.event;

public class GroupsListModelChanged {

    private int _newItemCount;

    public GroupsListModelChanged(int newItemCount) {
        _newItemCount = newItemCount;
    }

    public int getNewItemCount() {
        return _newItemCount;
    }
}
