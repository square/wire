/*
 * Copyright 2013 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire;

import java.nio.charset.Charset;

// Adapted from android.util.Base64, original copyright notice below:

/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Utilities for encoding and decoding the Base64 representation of
 * binary data.  See RFCs <a
 * href="http://www.ietf.org/rfc/rfc2045.txt">2045</a> and <a
 * href="http://www.ietf.org/rfc/rfc3548.txt">3548</a>.
 */
public final class Base64 {

  private static final Charset UTF_8 = Charset.forName("UTF-8");

  private Base64() {
  }

  public static byte[] decode(String str) {
    byte[] input = str.getBytes(UTF_8);
    Decoder decoder = new Decoder(new byte[input.length * 3 / 4]);
    if (!decoder.process(input, 0, input.length, true)) {
      throw new IllegalArgumentException("bad base-64");
    }
    if (decoder.op == decoder.output.length) {
      return decoder.output;
    }
    byte[] temp = new byte[decoder.op];
    System.arraycopy(decoder.output, 0, temp, 0, decoder.op);
    return temp;
  }

  public static String encode(byte[] input) {
    int outputLen = input.length / 3 * 4;
    if (input.length % 3 > 0) {
      outputLen += 4;
    }
    Encoder encoder = new Encoder(new byte[outputLen]);
    encoder.process(input, 0, input.length, true);
    return new String(encoder.output, UTF_8);
  }

  private static class Decoder {
    private static final int[] DECODE = {
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63,
        52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -2, -1, -1,
        -1,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14,
        15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1,
        -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
        41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    };

    private final byte[] output;
    private int op;

    /** Non-data values in the DECODE arrays. */
    private static final int SKIP = -1;
    private static final int EQUALS = -2;

    private int state;
    private int value;

    public Decoder(byte[] output) {
      this.output = output;
      state = 0;
      value = 0;
    }

    public boolean process(byte[] input, int offset, int len, boolean finish) {
      if (this.state == 6) return false;

      int p = offset;
      len += offset;

      int state = this.state;
      int value = this.value;
      int op = 0;
      final byte[] output = this.output;
      final int[] alphabet = DECODE;

      while (p < len) {
        if (state == 0) {
          while (p + 4 <= len
              && (value = ((alphabet[input[p] & 0xff] << 18)
                  | (alphabet[input[p + 1] & 0xff] << 12)
                  | (alphabet[input[p + 2] & 0xff] << 6)
                  | (alphabet[input[p + 3] & 0xff]))) >= 0) {
            output[op + 2] = (byte) value;
            output[op + 1] = (byte) (value >> 8);
            output[op] = (byte) (value >> 16);
            op += 3;
            p += 4;
          }
          if (p >= len) break;
        }

        int d = alphabet[input[p++] & 0xff];
        switch (state) {
          case 0:
            if (d >= 0) {
              value = d;
              ++state;
            } else if (d != SKIP) {
              this.state = 6;
              return false;
            }
            break;

          case 1:
            if (d >= 0) {
              value = (value << 6) | d;
              ++state;
            } else if (d != SKIP) {
              this.state = 6;
              return false;
            }
            break;

          case 2:
            if (d >= 0) {
              value = (value << 6) | d;
              ++state;
            } else if (d == EQUALS) {
              output[op++] = (byte) (value >> 4);
              state = 4;
            } else if (d != SKIP) {
              this.state = 6;
              return false;
            }
            break;

          case 3:
            if (d >= 0) {
              value = (value << 6) | d;
              output[op + 2] = (byte) value;
              output[op + 1] = (byte) (value >> 8);
              output[op] = (byte) (value >> 16);
              op += 3;
              state = 0;
            } else if (d == EQUALS) {
              output[op + 1] = (byte) (value >> 2);
              output[op] = (byte) (value >> 10);
              op += 2;
              state = 5;
            } else if (d != SKIP) {
              this.state = 6;
              return false;
            }
            break;

          case 4:
            if (d == EQUALS) {
              ++state;
            } else if (d != SKIP) {
              this.state = 6;
              return false;
            }
            break;

          case 5:
            if (d != SKIP) {
              this.state = 6;
              return false;
            }
            break;

          default:
            throw new AssertionError("state = " + state);
        }
      }

      if (!finish) {
        this.state = state;
        this.value = value;
        this.op = op;
        return true;
      }

      switch (state) {
        case 0:
          break;
        case 1:
          this.state = 6;
          return false;
        case 2:
          output[op++] = (byte) (value >> 4);
          break;
        case 3:
          output[op++] = (byte) (value >> 10);
          output[op++] = (byte) (value >> 2);
          break;
        case 4:
          this.state = 6;
          return false;
        case 5:
          break;
        default:
          throw new AssertionError("state = " + state);
      }

      this.state = state;
      this.op = op;
      return true;
    }
  }

  private static class Encoder {
    private static final byte[] ENCODE = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
        'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
        'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
        'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/',
    };

    private final byte[] output;
    private final byte[] tail;
    private int tailLen;

    public Encoder(byte[] output) {
      this.output = output;
      tail = new byte[2];
      tailLen = 0;
    }

    public void process(byte[] input, int offset, int len, boolean finish) {
      final byte[] alphabet = ENCODE;
      final byte[] output = this.output;
      int op = 0;

      int p = offset;
      len += offset;
      int v = -1;

      switch (tailLen) {
        case 0:
          break;

        case 1:
          if (p + 2 <= len) {
            v = ((tail[0] & 0xff) << 16)
                | ((input[p++] & 0xff) << 8)
                | (input[p++] & 0xff);
            tailLen = 0;
          }
          break;

        case 2:
          if (p + 1 <= len) {
            v = ((tail[0] & 0xff) << 16)
                | ((tail[1] & 0xff) << 8)
                | (input[p++] & 0xff);
            tailLen = 0;
          }
          break;

        default:
          throw new AssertionError("tailLen = " + tailLen);
      }

      if (v != -1) {
        output[op++] = alphabet[(v >> 18) & 0x3f];
        output[op++] = alphabet[(v >> 12) & 0x3f];
        output[op++] = alphabet[(v >> 6) & 0x3f];
        output[op++] = alphabet[v & 0x3f];
      }

      while (p + 3 <= len) {
        v = ((input[p] & 0xff) << 16)
            | ((input[p + 1] & 0xff) << 8)
            | (input[p + 2] & 0xff);
        output[op] = alphabet[(v >> 18) & 0x3f];
        output[op + 1] = alphabet[(v >> 12) & 0x3f];
        output[op + 2] = alphabet[(v >> 6) & 0x3f];
        output[op + 3] = alphabet[v & 0x3f];
        p += 3;
        op += 4;
      }

      if (finish) {
        if (p - tailLen == len - 1) {
          int t = 0;
          v = ((tailLen > 0 ? tail[t++] : input[p]) & 0xff) << 4;
          tailLen -= t;
          output[op++] = alphabet[(v >> 6) & 0x3f];
          output[op++] = alphabet[v & 0x3f];
          output[op++] = '=';
          output[op] = '=';
        } else if (p - tailLen == len - 2) {
          int t = 0;
          v = (((tailLen > 1 ? tail[t++] : input[p++]) & 0xff) << 10)
              | (((tailLen > 0 ? tail[t++] : input[p]) & 0xff) << 2);
          tailLen -= t;
          output[op++] = alphabet[(v >> 12) & 0x3f];
          output[op++] = alphabet[(v >> 6) & 0x3f];
          output[op++] = alphabet[v & 0x3f];
          output[op] = '=';
        }
      } else {
        if (p == len - 1) {
          tail[tailLen++] = input[p];
        } else if (p == len - 2) {
          tail[tailLen++] = input[p];
          tail[tailLen++] = input[p + 1];
        }
      }
    }
  }
}
