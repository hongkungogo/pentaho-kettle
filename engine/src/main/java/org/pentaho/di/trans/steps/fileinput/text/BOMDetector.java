/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.di.trans.steps.fileinput.text;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Detector of BOM prefix in file.
 *
 * We don't use BOMInputStream because: 1) it requires commons-io 2.1 or higher, 2) it doesn't support GB18030 charset,
 * 3) it's additional abstraction level that add some microseconds to read
 */
public class BOMDetector {
  public static final BOMMark[] MARKS =
      new BOMMark[] { new BOMMark( "UTF-8", 0xEF, 0xBB, 0xBF ), new BOMMark( "UTF-32BE", 0x00, 0x00, 0xFE, 0xFF ),
        new BOMMark( "UTF-32LE", 0xFF, 0xFE, 0x00, 0x00 ), new BOMMark( "UTF-16BE", 0xFE, 0xFF ), new BOMMark(
            "UTF-16LE", 0xFF, 0xFE ), new BOMMark( "GB18030", 0x84, 0x31, 0x95, 0x33 ), };

  private final InputStream in;
  private int bomSize;
  private String charset;

  public BOMDetector( BufferedInputStream in ) throws IOException {
    this.in = in;
    in.mark( 16 );
    readBOM();
    in.reset();
    in.skip( bomSize );
  }

  void readBOM() throws IOException {
    long bom = readLong();
    for ( BOMMark m : MARKS ) {
      if ( m.matches( bom ) ) {
        bomSize = m.getBytes();
        charset = m.getCharset();
        return;
      }
    }
  }

  public boolean bomExist() {
    return charset != null;
  }

  public String getCharset() {
    return charset;
  }

  public int getBomSize() {
    return bomSize;
  }

  /**
   * Read first 6 bytes for check BOM.
   */
  long readLong() throws IOException {
    long[] b = new long[6];
    for ( int i = 0; i < b.length; i++ ) {
      b[i] = in.read();
      if ( b[i] < 0 ) {
        b[i] = 0; // after EOF
      }
    }
    long r = 0;
    for ( int i = 0; i < b.length; i++ ) {
      r += b[i] << ( i * 8 );
    }
    return r;
  }

  public static class BOMMark {
    private final String charset;
    private final long mark;
    private final long mask;
    private final int bytes;

    public BOMMark( String charset, int... bytes ) {
      this.charset = charset;
      long m = 0;
      for ( int i = 0; i < bytes.length; i++ ) {
        m += ( (long) bytes[i] ) << ( i * 8 );
      }
      mark = m;
      mask = ( 1L << ( bytes.length * 8 ) ) - 1;
      this.bytes = bytes.length;
    }

    public boolean matches( long bytes ) {
      return ( bytes & mask ) == mark;
    }

    public int getBytes() {
      return bytes;
    }

    public String getCharset() {
      return charset;
    }
  }
}
