/*
Copyright 2011-2013 Frederic Langlet
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
you may obtain a copy of the License at

                http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package kanzi.util.hash;

// Port of SipHash (64 bits) to Java. Implemented with CROUNDS=2, dROUNDS=4.
// SipHash was designed by Jean-Philippe Aumasson and Daniel J. Bernstein.
// See https://131002.net/siphash/

public class SipHash_2_4
{
   private static final long PRIME0 = 0x736f6d6570736575L;
   private static final long PRIME1 = 0x646f72616e646f6dL;
   private static final long PRIME2 = 0x6c7967656e657261L;
   private static final long PRIME3 = 0x7465646279746573L;

   private long v0;
   private long v1;
   private long v2;
   private long v3;

  
   public SipHash_2_4()
   {
   }
  
   
   public SipHash_2_4(byte[] seed)
   {
      this.setSeed(seed);
   }
   
   
   public SipHash_2_4(long k0, long k1)
   {
      this.setSeed(k0, k1);
   }
  
  
   public final void setSeed(byte[] seed) 
   {  
      if (seed == null) 
         throw new NullPointerException("Invalid null seed parameter");

      if (seed.length != 16) 
         throw new IllegalArgumentException("Seed length must be exactly 16");

      this.setSeed(bytesToLong(seed, 0), bytesToLong(seed, 8));
   }

   
   public final void setSeed(long k0, long k1)
   {
      this.v0 = PRIME0 ^ k0;
      this.v1 = PRIME1 ^ k1;
      this.v2 = PRIME2 ^ k0;
      this.v3 = PRIME3 ^ k1;   
   }
  
  
   public long hash(byte[] data)
   {
      return this.hash(data, 0, data.length);
   }
  
   
   public long hash(byte[] data, int offset, int length)
   { 
      final int len8 = length & -8;
      final int end8 = offset + len8;
      int n;
      
      for (n=offset; n<end8; n+=8)
      {
         final long m = bytesToLong(data, n);
         this.v3 ^= m;
         this.sipRound();
         this.sipRound();
         this.v0 ^= m;         
      }       

      long last = (((long) length) & 0xFF) << 56;
      
      for (int i=0; n<length; n++, i+=8)
         last |= (((long) data[n]) << i);
  
      this.v3 ^= last;
      this.sipRound();
      this.sipRound();
      this.v0 ^= last;
      this.v2 ^= 0xFF;
      this.sipRound();
      this.sipRound();
      this.sipRound();
      this.sipRound();
      this.v0 = this.v0 ^ this.v1 ^ this.v2 ^ this.v3;
      return this.v0;     
  }
  
  
   private static long bytesToLong(byte[] buf, int offset)
   {
      return ((((long) buf[offset+7]) & 0xFF) << 56) |
             ((((long) buf[offset+6]) & 0xFF) << 48) |
             ((((long) buf[offset+5]) & 0xFF) << 40) |
             ((((long) buf[offset+4]) & 0xFF) << 32) |
             ((((long) buf[offset+3]) & 0xFF) << 24) |
             ((((long) buf[offset+2]) & 0xFF) << 16) |
             ((((long) buf[offset+1]) & 0xFF) << 8)  |
             ((((long) buf[offset])   & 0xFF));
         
   }


   private void sipRound()
   {
      this.v0 += this.v1;
      this.v1 = (this.v1 << 13) | (this.v1 >>> 51);
      this.v1 ^= this.v0;
      this.v0 = (this.v0 << 32) | (this.v0 >>> 32);
      this.v2 += this.v3;     
      this.v3 = (this.v3 << 16) | (this.v3 >>> 48);
      this.v3 ^= this.v2;
      this.v0 += this.v3;
      this.v3 = (this.v3 << 21) | (this.v3 >>> 43);
      this.v3 ^= this.v0;      
      this.v2 += this.v1;    
      this.v1 = (this.v1 << 17) | (this.v1 >>> 47);
      this.v1 ^= this.v2;     
      this.v2 = (this.v2 << 32) | (this.v2 >>> 32);
   }  
  
}