package com.aptasystems.kakapo.service;

import java.io.DataInputStream;
import java.io.IOException;

public interface IAccountSerializer {

    byte[] serializeUserAccountData(long userAccountId) throws IOException;

    AccountData deserializeUserAccountData(DataInputStream dataInputStream)
            throws IOException;
}
