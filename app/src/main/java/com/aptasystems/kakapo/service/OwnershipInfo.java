package com.aptasystems.kakapo.service;

public class OwnershipInfo {

    private OwnedBy _ownedBy;
    private int _colour;
    private String _avatarLetter;
    private String _mixedCaseReference;
    private String _lowerCaseReference;

    public OwnershipInfo(OwnedBy ownedBy,
                         int colour,
                         String avatarLetter,
                         String mixedCaseReference,
                         String lowerCaseReference) {
        _ownedBy = ownedBy;
        _colour = colour;
        _avatarLetter = avatarLetter;
        _mixedCaseReference = mixedCaseReference;
        _lowerCaseReference = lowerCaseReference;
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

    public String getReference(boolean mixedCase) {
        return mixedCase ? _mixedCaseReference :  _lowerCaseReference;
    }
}
