package com.bose.services.acms.api;

/**
 * API for client-side encryption/decryption of values.
 *
 * This can be used by custom clients, to plug into some native encryption support.
 */
public interface ClientCryptoProvider {
    byte[] encrypt(String input);
    String decrypt(byte[] input);
}
