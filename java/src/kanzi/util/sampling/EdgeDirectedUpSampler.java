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

package kanzi.util.sampling;


// Edge Oriented Interpolation based upsampler.
// Original code by David Schleef: <./original/gstediupsample.c>
// See http://schleef.org/ds/cgak-demo-1 for explanation of the algo and
// http://schleef.org/ds/cgak-demo-1.png for visual examples.
public class EdgeDirectedUpSampler implements UpSampler
{
   private final int width;
   private final int height;
   private final int stride;


   public EdgeDirectedUpSampler(int width, int height)
   {
      this(width, height, width);
   }
   
   
   public EdgeDirectedUpSampler(int width, int height, int stride)
   {
      if (height < 8)
         throw new IllegalArgumentException("The height must be at least 8");

      if (width < 8)
         throw new IllegalArgumentException("The width must be at least 8");

      if (stride < width)
         throw new IllegalArgumentException("The stride must be at least as big as the width");

      if ((height & 7) != 0)
         throw new IllegalArgumentException("The height must be a multiple of 8");

      if ((width & 7) != 0)
         throw new IllegalArgumentException("The width must be a multiple of 8");

      this.height = height;
      this.width = width;
      this.stride = stride;
   }


   @Override
   public void superSampleVertical(int[] input, int[] output)
   { 
      throw new UnsupportedOperationException("Not supported");
   }


   @Override
   public void superSampleHorizontal(int[] input, int[] output)
   {
      throw new UnsupportedOperationException("Not supported");
   }


   private static int reconstructV(int[] buf, int offs, int stride, int a, int b, int c, int d)
   {
      int x;
      x  = buf[offs-3*stride] * a;
      x += buf[offs-2*stride] * b;
      x += buf[offs-1*stride] * c;
      x += buf[offs-0*stride] * d;
      offs++;
      x += buf[offs+0*stride] * d;
      x += buf[offs+1*stride] * c;
      x += buf[offs+2*stride] * b;
      x += buf[offs+3*stride] * a;
      return (x + 0) >> 5;
   }

   
   private static int reconstructH(int[] buf, int offs1, int offs2, int a, int b, int c, int d)
   {
      int x;
      x  = buf[offs1-3] * a;
      x += buf[offs1-2] * b;
      x += buf[offs1-1] * c;
      x += buf[offs1-0] * d;
      x += buf[offs2+0] * d;
      x += buf[offs2+1] * c;
      x += buf[offs2+2] * b;
      x += buf[offs2+3] * a;
      return (x + 0) >> 5;
   }

   
   @Override
   public void superSample(int[] src, int[] dst)
   {
      final int st = this.stride;
      final int sh = this.height;
      final int sw = this.width;
      final int dw = sw << 1;
      final int dw2 = dw + dw;
      int srcOffs = 0;
      int dstOffs = 0;

      // Horizontal
      for (int j=0; j<sh; j++)
      {
         if ((j >= 3) && (j < sh-3))
         {
            for (int i=0; i<sw-1; i++)
            {
               int v;
               int dx  = (-src[srcOffs-st+i] - src[srcOffs-st+i+1] + src[srcOffs+st+i] + src[srcOffs+st+i+1]) << 1;
               int dy  = -src[srcOffs-st+i] - 2*src[srcOffs+i] - src[srcOffs+st+i] + src[srcOffs-st+i+1] + 2*src[srcOffs+i+1] + src[srcOffs+st+i+1];
               int dx2 = -src[srcOffs-st+i] + 2*src[srcOffs+i] - src[srcOffs+st+i] - src[srcOffs-st+i+1] + 2*src[srcOffs+i+1] - src[srcOffs+st+i+1];

               if (dy < 0)
               {
		  dy = -dy;
		  dx = -dx;
               }

               if (Math.abs(dx) <= 4 * Math.abs(dx2))
               {
                  v = (src[srcOffs+i] + src[srcOffs+i+1] + 1) >> 1;
               }
               else if (dx < 0)
               {
                  if (dx < -2*dy)
                    v = reconstructV(src, srcOffs+i, st, 0, 0, 0, 16);
                  else if (dx < -dy)
                    v = reconstructV(src, srcOffs+i, st, 0, 0, 8, 8);
                  else if (2*dx < -dy)
                    v = reconstructV(src, srcOffs+i, st, 0, 4, 8, 4);
                  else if (3*dx < -dy)
                    v = reconstructV(src, srcOffs+i, st, 1, 7, 7, 1);
                  else
                    v = reconstructV(src, srcOffs+i, st, 4, 8, 4, 0);
               }
               else
               {
                 if (dx > 2*dy)
                   v = reconstructV(src, srcOffs+i, -st, 0, 0, 0, 16);
                 else if (dx > dy)
                   v = reconstructV(src, srcOffs+i, -st, 0, 0, 8, 8);
                 else if (2*dx > dy)
                   v = reconstructV(src, srcOffs+i, -st, 0, 4, 8, 4);
                 else if (3*dx > dy)
                   v = reconstructV(src, srcOffs+i, -st, 1, 7, 7, 1);
                 else
                   v = reconstructV(src, srcOffs+i, -st, 4, 8, 4, 0);
               }

               dst[dstOffs+i+i] = src[srcOffs+i];
               //dst[dstOffs+i+i+1] = Math.min(Math.max(v, 0), 255);
               dst[dstOffs+i+i+1] = v;
            }

            dst[dstOffs+dw-2] = src[srcOffs+sw-1];
            dst[dstOffs+dw-1] = src[srcOffs+sw-1];
         }
         else
         {
            final int dstOffs1 = dstOffs;
            final int dstOffs2 = dstOffs + dw;
            final int srcOffs1 = srcOffs;
            
            for (int i=0; i<sw-1; i++)
            {
               dst[dstOffs1+i+i] = src[srcOffs1+i];
               dst[dstOffs1+i+i+1] = (src[srcOffs1+i] + src[srcOffs1+i+1]) >> 1;
               dst[dstOffs2+i+i] = src[srcOffs1+i];
               dst[dstOffs2+i+i+1] = (src[srcOffs1+i] + src[srcOffs1+i+1]) >> 1;
            }

            dst[dstOffs1+dw-2] = src[srcOffs1+sw-1];
            dst[dstOffs1+dw-1] = src[srcOffs1+sw-1];
            dst[dstOffs2+dw-2] = src[srcOffs1+sw-1];
            dst[dstOffs2+dw-1] = src[srcOffs1+sw-1];
         }
         
         srcOffs += st;
         dstOffs += dw2;
      }

      dstOffs = 0;
      
      // Vertical 
      for (int j=0; j<sh-1; j++)
      {
         final int dstOffs1 = dstOffs;
         final int dstOffs2 = dstOffs + dw;
         final int dstOffs3 = dstOffs + dw2;

         for (int i=0; i<dw; i++)
         {
            if ((i>=3) && (i<dw-4))
            {
               int v;
               int dx  = (-dst[dstOffs1+i-1] - dst[dstOffs3+i-1] + dst[dstOffs1+i+1] + dst[dstOffs3+i+1]) << 1;
               int dy  = -dst[dstOffs1+i-1] - 2*dst[dstOffs1+i] - dst[dstOffs1+i+1] + dst[dstOffs3+i-1] + 2*dst[dstOffs3+i] + dst[dstOffs3+i+1];
               int dx2 = -dst[dstOffs1+i-1] + 2*dst[dstOffs1+i] - dst[dstOffs1+i+1] - dst[dstOffs3+i-1] + 2*dst[dstOffs3+i] - dst[dstOffs3+i+1];

               if (dy < 0)
               {
                  dy = -dy;
                  dx = -dx;
               }

               if (Math.abs(dx) <= 4*Math.abs(dx2))
                 v = (dst[dstOffs1+i] + dst[dstOffs3+i]) >> 1;
               else if (dx < 0)
               {
                 if (dx < -2*dy)
                   v = reconstructH(dst, dstOffs1+i, dstOffs3+i, 0, 0, 0, 16);
                 else if (dx < -dy)
                   v = reconstructH(dst, dstOffs1+i, dstOffs3+i, 0, 0, 8, 8);
                 else if (2*dx < -dy)
                   v = reconstructH(dst, dstOffs1+i, dstOffs3+i, 0, 4, 8, 4);
                 else if (3*dx < -dy)
                   v = reconstructH(dst, dstOffs1+i, dstOffs3+i, 1, 7, 7, 1);
                 else
                   v = reconstructH(dst, dstOffs1+i, dstOffs3+i, 4, 8, 4, 0);
               }
               else
               {
                 if (dx > 2*dy)
                   v = reconstructH(dst, dstOffs3+i, dstOffs1+i, 0, 0, 0, 16);
                 else if (dx > dy)
                   v = reconstructH(dst, dstOffs3+i, dstOffs1+i, 0, 0, 8, 8);
                 else if (2*dx > dy)
                   v = reconstructH(dst, dstOffs3+i, dstOffs1+i, 0, 4, 8, 4);
                 else if (3*dx > dy)
                   v = reconstructH(dst, dstOffs3+i, dstOffs1+i, 1, 7, 7, 1);
                 else
                   v = reconstructH(dst, dstOffs3+i, dstOffs1+i, 4, 8, 4, 0);
               }

               //dst[dstOffs2+i] = Math.min(Math.max(v, 0), 255);
               dst[dstOffs2+i] = v;
            }
            else
            {
               dst[dstOffs2+i] = (dst[dstOffs1+i] + dst[dstOffs3+i]) >> 1;
            }
	 }
         
         dstOffs += dw2;         
      }
   
      for (int i=0; i<sw; i++) 
      {
         final int dstOffs1 = dstOffs + i + i;
         final int dstOffs2 = dstOffs + dw + i + i;		            
         dst[dstOffs1+1] = dst[dstOffs1];
         dst[dstOffs2] = dst[dstOffs1];
         dst[dstOffs2+1] = dst[dstOffs1];
      }   
   }
   
   
   @Override
   public boolean supportsScalingFactor(int factor)
   {
      return (factor == 2);
   }   
}