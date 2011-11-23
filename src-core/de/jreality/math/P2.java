/**
 *
 * This file is part of jReality. jReality is open source software, made
 * available under a BSD license:
 *
 * Copyright (c) 2003-2006, jReality Group: Charles Gunn, Tim Hoffmann, Markus
 * Schmies, Steffen Weissmann.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * - Neither the name of jReality nor the names of its contributors nor the
 *   names of their associated organizations may be used to endorse or promote
 *   products derived from this software without specific prior written
 *   permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */


package de.jreality.math;

import java.util.logging.Level;

import de.jreality.util.LoggingSystem;

/**
 * Static methods for geometry of the real projective plane RP2.
 * 
 * {@see de.jreality.math.Rn}  for method conventions and discussion of the ubiquitous <i>metricnature</i> parameter.
 * {@see de.jreality.math.Pn}  for other methods applicable in n-dimensional projective space.
 * @author Charles Gunn
 *
 */
final public class P2 {

  private P2() {}

	/**
	 * Calculate the Euclidean perpendicular bisector of the segment from <i>p1</i> to <i>p2</i>.
	 * @param dst
	 * @param p1
	 * @param p2
	 * @return
	 */
	private static double[] perpendicularBisector(double[] dst, double[] p1, double[]p2)	{
		if (p1.length != 3 || p2.length != 3)	{
			throw new IllegalArgumentException("Input points must be homogeneous vectors");
		}
		if (dst == null) dst = new double[3];
		double[] avg = new double[3];
		Rn.add(avg,p1,p2);
		Rn.times(avg, .5, avg);
		double[] line = new double[3];
		lineFromPoints(line, p1, p2);
		dst[0] = -line[1];
		dst[1] = line[0];
		dst[2] = -(dst[0]*avg[0] + dst[1]*avg[1]);
		return dst;
	}
	
	/**
	 * Calculate the perpendicular bisector of the segment <i>p1</i> and <i>p2</i> with metricnature <i>metricnature</i>
	 * @param dst
	 * @param p1
	 * @param p2
	 * @param metric
	 * @return
	 */
	public static double[] perpendicularBisector(double[] dst, double[] p1, double[]p2, int metric)	{
		if (p1.length != 3 || p2.length != 3)	{
			throw new IllegalArgumentException("Input points must be homogeneous vectors");
		}
		if (metric == Pn.EUCLIDEAN) return perpendicularBisector(dst, p1, p2);
		if (dst == null) dst = new double[3];
		double[] midpoint = new double[3];
		Pn.linearInterpolation(midpoint,p1,p2, .5, metric);
		double[] line = lineFromPoints(null, p1, p2);
		double[] polarM = Pn.polarize(null, midpoint, metric);
		double[] pb = pointFromLines(null, polarM, line);
		Pn.polarize(dst, pb, metric);
		if (Rn.innerProduct(dst,p1) < 0)	Rn.times(dst, -1.0, dst);
		return dst;
	}
	
	/**
	 * Calculate the homogeneous coordinates of the point of intersection  of the two  lines <i>l1</i> and <i>l2</i>.
	 * @param point
	 * @param l1
	 * @param l2
	 * @return
	 */
	public static double[] pointFromLines(double[] point, double[] l1, double[] l2)	{
		if (l1.length < 3 || l2.length < 3)	{
			throw new IllegalArgumentException("Input arrays too short");
		}
		if (point == null) point = new double[3];	
		point[0] = l1[1]*l2[2] - l1[2]*l2[1];
		point[1] = l1[2]*l2[0] - l1[0]*l2[2];
		point[2] = l1[0]*l2[1] - l1[1]*l2[0];
		return point;
	}
	
	/**
	 * Calculate the line coordinates of the line connecting the two points <i>p1</i> and <i>p2</i>.
	 * @param point
	 * @param l1
	 * @param l2
	 * @return
	 */
	public static double[] lineFromPoints(double[] line, double[] p1, double[] p2)	{
		return pointFromLines(line, p1, p2);
	}
		
	/**
	 * Returns true if and only if <i>point</i> is  within the polygon determined by the
	 * points contained in the array <i>polygon</i>.
	 * @param polygon
	 * @param point
	 * @return
	 */public static boolean polygonContainsPoint(double[][] polygon, double[] point)	{
		if (point.length != 3)	{
			throw new IllegalArgumentException("Input point must be homogeneous vector");
		}
		double metricn = 0.0;
		int n = polygon.length, j;
		double[] p1 = new double[3];
		double[] p2 = new double[3];
		double[] tmp;
		p1[2] = p2[2] = 1.0;
		p1[0] = polygon[0][0]; p1[1] = polygon[0][1];
		for (int i = 0; i<n; ++i)	{
			j = (i+1) % n;
			p2[0] = polygon[j][0]; p2[1] = polygon[j][1];
			double[] line = lineFromPoints(null, p1, p2);
			double ip = Rn.innerProduct(line, point);
			if (metricn == 0.0) metricn = ip;
			else if (metricn * ip < 0.0) return false;
			tmp = p1;
			p1 = p2;
			p2 = tmp;
			//System.arraycopy(p2,0,p1,0,3);
		}
		return true;
	}
	
	/**
	 * Returns true if and only if the polygon described by the point series <i>polygon</i> is convex.
	 * @param polygon
	 * @return
	 */
	 public static boolean isConvex(double[][] polygon)	{
		int n = polygon.length, j;
		double metricn = 0.0;
		double[][] diffs = new double[n][polygon[0].length];
		for (int i = 0; i<n; ++i)	{
			j = (i+1) % n;
			Rn.subtract(diffs[i], polygon[j], polygon[i]);
			Rn.normalize(diffs[i], diffs[i]);
		}
		double[] p1 = new double[3];
		double[] p2 = new double[3];
		double[] tmp = new double[3];
		p1[2] = p2[2] = 1.0;
		p1[0] = polygon[0][0]; p1[1] = polygon[0][1];
		for (int i = 0; i<n; ++i)	{
			j = (i+1) % n;
			Rn.crossProduct(tmp, diffs[i],diffs[j]);
			if (metricn == 0.0)	metricn = tmp[2];
			else if (metricn * tmp[2] < 0.0) return false;
		}
		
		return true;
	}
	/**
	 * The assumption is that the line is specified in such a way that vertices to be cut away
	 * have a negative inner product with the line coordinates.  The result is a new point array that
	 * defines the polygon obtained by cutting off all points with negative inner product with the
	 * given <i>line</i>.  The polygon is assumed  to be convex.
	 * @param polygon
	 * @param line
	 * @return
	 */
	public static double[][] chopConvexPolygonWithLine(double[][] polygon, double[] line)	{
		if (line.length != 3 )	{
			throw new IllegalArgumentException("Input line must be homogeneous vectors");
		}
		if (polygon == null) return null;
		int n = polygon.length;
		
		double[] center = new double[3];
		Rn.average(center, polygon);
		boolean noNegative = true;
		
		double[] vals = new double[n];
		int count = 0;
		for (int i = 0; i<n; ++i)	{
			vals[i] = Rn.innerProduct(line, polygon[i]);
			if (vals[i] >= 0) count++;
			else noNegative = false;	
			}
		if (count == 0)		{
			LoggingSystem.getLogger(P2.class).log(Level.FINE, "chopConvexPolygonWithLine: nothing left");
			return null;
		} else if (count == n || noNegative)	{
			return polygon;
		}
		double[][] newPolygon = new double[count+2][3];
		double[] tmp = new double[3];
		count = 0;
		for (int i = 0; i<n; ++i)	{
			if (vals[i] >= 0) 	System.arraycopy(polygon[i],0,newPolygon[count++],0,3);
			if (count >= newPolygon.length) break;
			if (vals[i] * vals[(i+1)%n] < 0)	{
				double[] edge = new double[3];
				lineFromPoints(edge, polygon[i], polygon[(i+1)%n]);
				pointFromLines(tmp,edge,line);
				Pn.dehomogenize(newPolygon[count],tmp);
				count++;
			} 
			if (count >= newPolygon.length) break;
		}
		if (count != newPolygon.length) {
			double[][] newPolygon2 = new double[count][];
			System.arraycopy(newPolygon, 0, newPolygon2,0,count);
			return newPolygon2;
		}
		return newPolygon;
	}
	
	/**
	 * Convert the input (x,y,z,w) into (x,y,w).
	 * @param vec3
	 * @param vec4
	 * @return
	 */public static double[] projectP3ToP2(double[] vec3, double[] vec4)	{
		double[] dst;
		if (vec3 == null)	dst = new double[3];
		else dst = vec3;
		dst[0] = vec4[0];
		dst[1] = vec4[1];
		dst[2] = vec4[3];
		return dst;
	}
	
	/**
	 * Convert (x,y,z) into (x,y,0,z)
	 * @param vec4
	 * @param vec3
	 * @return
	 */public static double[] imbedP2InP3(double[] vec4, double[] vec3)	{
		double[] dst;
		if (vec4 == null)	dst = new double[4];
		else dst = vec4;
		dst[0] = vec3[0];
		dst[1] = vec3[1];
		dst[2] = 0.0;
		dst[3] = vec3[2];
		return dst;
	}

}
