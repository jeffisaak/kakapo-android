package com.aptasystems.kakapo.event;

public class QueuedItemsListModelChanged {

    private int _newItemCount;

    public QueuedItemsListModelChanged(int newItemCount) {
        _newItemCount = newItemCount;
    }

    public int getNewItemCount() {
        return _newItemCount;
    }
}
