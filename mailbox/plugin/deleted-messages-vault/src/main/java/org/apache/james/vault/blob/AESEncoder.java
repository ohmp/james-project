/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.vault.blob;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.james.vault.metadata.Salt;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.subtle.AesGcmJce;

public class AESEncoder {
    private static final byte[] EMPTY_ASSOCIATED_DATA = new byte[0];
    private static final int PBKDF2_ITERATIONS = 65536;
    private static final int KEY_SIZE = 256;
    private static final String SECRET_KEY_FACTORY_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final BigInteger MAX_BYTES = FileUtils.ONE_MB_BI;
    private static SecretKeyFactory secretKeyFactory;

    @Inject
    public AESEncoder() {
        try {
            secretKeyFactory = SecretKeyFactory.getInstance(SECRET_KEY_FACTORY_ALGORITHM);
            AeadConfig.register();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Error while starting AESPayloadCodec", e);
        }
    }

    public InputStream encode(InputStream inputStream, Salt salt) {
        try {
            SecretKey secretKey = deriveKey(salt);
            Aead aead = new AesGcmJce(secretKey.getEncoded());
            return new ByteArrayInputStream(aead.encrypt(IOUtils.toByteArray(inputStream), EMPTY_ASSOCIATED_DATA));
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException("Failed to encrypt", e);
        }
    }

    public InputStream decode(InputStream inputStream, Salt salt) {
        try {
            SecretKey secretKey = deriveKey(salt);
            Aead aead = new AesGcmJce(secretKey.getEncoded());
            return new ByteArrayInputStream(aead.decrypt(IOUtils.toByteArray(inputStream), EMPTY_ASSOCIATED_DATA));
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException("Failed to decrypt", e);
        }
    }

    private SecretKey deriveKey(Salt salt) throws InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(salt.asString().toCharArray(),
            salt.asString().getBytes(StandardCharsets.US_ASCII),
            PBKDF2_ITERATIONS,
            KEY_SIZE);
        return secretKeyFactory.generateSecret(spec);
    }
}
