package com.aptasystems.kakapo.service;

public class OwnershipInfo {

    private OwnedBy _ownedBy;
    private int _colour;
    private String _avatarLetter;
    private String _reference;

    public OwnershipInfo(OwnedBy ownedBy, int colour, String avatarLetter, String reference) {
        _ownedBy = ownedBy;
        _colour = colour;
        _avatarLetter = avatarLetter;
        _reference = reference;
    }

    public OwnedBy getOwnedBy() {
        return _ownedBy;
    }

    public int getColour() {
        return _colour;
    }

    public String getAvatarLetter() {
        return _avatarLetter;
    }

    public String getReference() {
        return _reference;
    }
}
