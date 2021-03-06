/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.openjsse.com.sun.crypto.provider;

import java.security.*;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

/**
 * KeyGeneratore core implementation and individual key generator
 * implementations. Because of US export regulations, we cannot use
 * subclassing to achieve code sharing between the key generator
 * implementations for our various algorithms. Instead, we have the
 * core implementation in this KeyGeneratorCore class, which is used
 * by the individual implementations. See those further down in this
 * file.
 *
 * @since   1.5
 * @author  Andreas Sterbenz
 */
final class KeyGeneratorCore {

    // algorithm name to use for the generator keys
    private final String name;

    // default key size in bits
    private final int defaultKeySize;

    // current key size in bits
    private int keySize;

    // PRNG to use
    private SecureRandom random;

    /**
     * Construct a new KeyGeneratorCore object with the specified name
     * and defaultKeySize. Initialize to default key size in case the
     * application does not call any of the init() methods.
     */
    KeyGeneratorCore(String name, int defaultKeySize) {
        this.name = name;
        this.defaultKeySize = defaultKeySize;
        implInit(null);
    }

    // implementation for engineInit(), see JCE doc
    // reset keySize to default
    void implInit(SecureRandom random) {
        this.keySize = defaultKeySize;
        this.random = random;
    }

    // implementation for engineInit(), see JCE doc
    // we do not support any parameters
    void implInit(AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidAlgorithmParameterException {
        throw new InvalidAlgorithmParameterException
            (name + " key generation does not take any parameters");
    }

    // implementation for engineInit(), see JCE doc
    // we enforce a general 40 bit minimum key size for security
    void implInit(int keysize, SecureRandom random) {
        if (keysize < 40) {
            throw new InvalidParameterException
                ("Key length must be at least 40 bits");
        }
        this.keySize = keysize;
        this.random = random;
    }

    // implementation for engineInit(), see JCE doc
    // generate the key
    SecretKey implGenerateKey() {
        if (random == null) {
            random = new SecureRandom();
        }
        byte[] b = new byte[(keySize + 7) >> 3];
        random.nextBytes(b);
        return new SecretKeySpec(b, name);
    }

    // nested static class for the ChaCha20 key generator
    public static final class ChaCha20KeyGenerator extends KeyGeneratorSpi {
        private final KeyGeneratorCore core;
        public ChaCha20KeyGenerator() {
            core = new KeyGeneratorCore("ChaCha20", 256);
        }
        @Override
        protected void engineInit(SecureRandom random) {
            core.implInit(random);
        }
        @Override
        protected void engineInit(AlgorithmParameterSpec params,
                SecureRandom random) throws InvalidAlgorithmParameterException {
            core.implInit(params, random);
        }
        @Override
        protected void engineInit(int keySize, SecureRandom random) {
            if (keySize != 256) {
                throw new InvalidParameterException(
                        "Key length for ChaCha20 must be 256 bits");
            }
            core.implInit(keySize, random);
        }
        @Override
        protected SecretKey engineGenerateKey() {
            return core.implGenerateKey();
        }
    }
}
