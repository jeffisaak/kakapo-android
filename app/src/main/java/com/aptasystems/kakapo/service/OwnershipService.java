package com.aptasystems.kakapo.service;

import android.content.Context;

import com.aptasystems.kakapo.KakapoApplication;
import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.adapter.model.AbstractNewsListItem;
import com.aptasystems.kakapo.dao.FriendDAO;
import com.aptasystems.kakapo.dao.UserAccountDAO;
import com.aptasystems.kakapo.entities.Friend;
import com.aptasystems.kakapo.entities.UserAccount;
import com.aptasystems.kakapo.util.PrefsUtil;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.core.content.ContextCompat;
import io.requery.Persistable;
import io.requery.sql.EntityDataStore;

@Singleton
public class OwnershipService {

    @Inject
    Context _context;

    @Inject
    UserAccountDAO _userAccountDAO;

    @Inject
    FriendDAO _friendDAO;

    @Inject
    PrefsUtil _prefsUtil;

    @Inject
    OwnershipService(KakapoApplication application) {
        application.getKakapoComponent().inject(this);
    }

    public OwnershipInfo getOwnership(AbstractNewsListItem newsListItem) {
        UserAccount userAccount = _userAccountDAO.find(_prefsUtil.getCurrentUserAccountId());

        OwnedBy ownedBy;
        int colour;
        String avatarLetter;
        String mixedCaseReference;
        String lowerCaseReference;
        if (newsListItem.isOwnedBy(userAccount.getGuid())) {

            // The news list item is owned by me.
            ownedBy = OwnedBy.Me;
            colour = userAccount.getColour();
            avatarLetter = userAccount.getName().toUpperCase().substring(0, 1);
            mixedCaseReference = _context.getString(R.string.app_text_pronoun_me_mixed_case);
            lowerCaseReference = _context.getString(R.string.app_text_pronoun_me_lower_case);

        } else {

            Friend friend = _friendDAO.find(_prefsUtil.getCurrentUserAccountId(),
                    newsListItem.getOwnerGuid());

            if (friend != null) {

                // The news list item is owned by a friend.
                ownedBy = OwnedBy.Friend;
                colour = friend.getColour();
                avatarLetter = friend.getName().toUpperCase().substring(0, 1);
                mixedCaseReference = friend.getName();
                lowerCaseReference = friend.getName();

            } else {

                // The news list item is owned by a stranger.
                ownedBy = OwnedBy.Stranger;
                colour = ContextCompat.getColor(_context, R.color.strangerAvatarColour);
                avatarLetter = _context.getString(R.string.app_text_avatar_letter_stranger);
                mixedCaseReference = _context.getString(R.string.app_text_pronoun_stranger_mixed_case);
                lowerCaseReference = _context.getString(R.string.app_text_pronoun_stranger_lower_case);
            }
        }

        return new OwnershipInfo(ownedBy, colour, avatarLetter, mixedCaseReference, lowerCaseReference);
    }
}


