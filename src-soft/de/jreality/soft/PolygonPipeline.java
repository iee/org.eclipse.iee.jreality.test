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


package de.jreality.soft;

import java.util.Arrays;

import de.jreality.math.Rn;
import de.jreality.scene.Geometry;
import de.jreality.scene.PointSet;
import de.jreality.scene.data.*;
import de.jreality.scene.data.Attribute;
import de.jreality.scene.data.DataList;
import de.jreality.scene.data.DoubleArray;
import de.jreality.scene.data.DoubleArrayArray;
import de.jreality.scene.data.IntArray;

/**
 * 
 * A render pass will consit of the following:
 * 1. calls to processPolygon for each polygon to render. This will transform the polygon 
 * by using the state of matrix. and give it the current shader.<p>
 * 2. optionally sort the polygon list.<br>
 * 3. shade all Polygons using the shader given by the polygon<br>
 * 4. clip all polygons using clipPolygons()<br>
 * 5. call perspective for all polygons<br>
 * 6. render the polygons.<p>
 * In fact all the calls in 3. to 6. can be done in one pass. However we have to delay the shading to after the traversal,
 * to allow the environment to gather all the lights in the scene, if we want to have lights to act globally. THIS IS NOT 
 * IMPLEMENTED YET.
 * @version 1.0
 * @author <a href="mailto:hoffmann@math.tu-berlin.de">Tim Hoffmann</a>
 *
 */
public class PolygonPipeline
    implements PolygonProcessor, PointProcessor, LineProcessor {

	private int vertexColorLength;
    private DataList vertexColor;
    private boolean vertexColors;
    private final boolean queueOpaque;	
	private final boolean sortOpaque;
    private final boolean triangulate =false;
    private final boolean useTexCoords =true;
    
    private double[] matrix;
    //private boolean itmComputed = false;
    private double[] inverseTransposeMatrix = new double[16];
    int polygonCount = 0;
    Polygon polygons[] = new Polygon[0];
    private int vertexCount = 0;
    double[] vertexData = new double[100 * Polygon.VERTEX_LENGTH];

    PolygonComparator comp = new PolygonComparator();
    private Perspective perspective = new DefaultPerspective();
    private Environment environment;

    private PolygonShader faceShader; // = new DefaultPolygonShader();
    private LineShader lineShader; // = new ConstantPolygonShader(.2,.1,.1);
    //private PolygonShader pointOutlineShader;
    // = new ConstantPolygonShader(.0,.0,.0);
    private PointShader pointShader;
    // = new ConstantPolygonShader(0.8,0.2,0.2);

    private PolygonShader shader;
    private double[] pointVertices = new double[48];
    private static int[] pointIndices = { 0, 3, 6, 9, 12, 15, 18, 21 };
    private static int[][] pointOutlineIndices = { { 0, 24, 27, 3 }, {
            3, 27, 30, 6 }, {
            6, 30, 33, 9 }, {
            9, 33, 36, 12 }, {
            12, 36, 39, 15 }, {
            15, 39, 42, 18 }, {
            18, 42, 45, 21 }, {
            21, 45, 24, 0 }
    };
    private static double[][] pointColors = new double[16][4];
    private DataList pointColorDataList = StorageModel.DOUBLE_ARRAY_ARRAY.createWritableDataList(pointColors);
    private static int[] pointNormals = { 0, 0, 0, 0, 0, 0, 0, 0 };
    private static int[][] pointOutlineNormals = { {0, 3, 6, 0}, {0, 6, 9, 0},
            {0, 9, 12, 0}, {0, 12 ,15 ,0}, {0, 15, 18, 0}, {0, 18, 21, 0},
            {0, 21, 24, 0},{0, 24, 3, 0}};
    //private static double[] zNormal = new double[] { 0, 0, 1 };
    private static double[] zNormal = new double[3*9];
    
    private static int[] lineIndices = { 0, 3, 6, 9 };

    private double pointRadius = 0.025;
    private double outlineFraction = 1 / 3.;
    private double lineWidth = .01;

    private final PolygonRasterizer renderer;
    //	static {
    //	}

    public PolygonPipeline(PolygonRasterizer renderer) {
        this(renderer, false);
    }
    public PolygonPipeline(PolygonRasterizer renderer,boolean sortAll) {
        super();
        this.sortOpaque=sortAll;
        this.queueOpaque=sortAll;
        this.renderer = renderer;
        zNormal[0] = 0;
        zNormal[1] = 0;
        zNormal[2] = 1;
        for (int i = 0; i < 8; i++) {
            int pos = 3 * i;
            pointVertices[pos] = zNormal[pos+3]= Math.cos(2. * i * Math.PI / 8.);
            pointVertices[pos+1] = zNormal[pos+4]= Math.sin(2. * i * Math.PI / 8.);
            zNormal[pos+5]= 0.01;
        }
    }

    public final void clearPipeline() {
        polygonCount = 0;
        vertexCount = 0;
        faceShader = null;
        lineShader = null;
        pointShader = null;
        //pointOutlineShader = null;
    }

    private static final int POLYGON_INCR = 100;
    private final void increasePolygonCapacity() {
        Polygon np[] = new Polygon[polygons.length + POLYGON_INCR];
        System.arraycopy(polygons, 0, np, 0, polygons.length);
        for (int i = polygons.length; i < np.length; i++) {
            np[i] = new Polygon();
        }
        polygons = np;
    }

    //	private final void increaseTransparentPolygonCapacity() {
    //		Polygon np[] = new Polygon[transparentPolygons.length+POLYGON_INCR];
    //		System.arraycopy(transparentPolygons,0,np,0,transparentPolygons.length);
    //		for (int i = transparentPolygons.length; i < np.length; i++) {
    //			np[i] =
    //			 new Polygon();
    //		}
    //		transparentPolygons = np;
    //	}

    private static final int VERTEX_INCR = 1200;
    private final void increaseVertexCapacity(int amount) {
        int a = (amount > VERTEX_INCR ? amount : VERTEX_INCR);
        double nv[] = new double[vertexData.length + Polygon.VERTEX_LENGTH * a];
        System.arraycopy(vertexData, 0, nv, 0, vertexData.length);
        vertexData = nv;
        //System.out.println("vertex capacity "+vertexData.length);
    }

    /* (non-Javadoc)
     * @see de.jreality.soft.PolygonProcessor#processPolygon(double[], int[], double[], int[])
     */
    public final void processPolygon(
        final double[] vd,
        final int[] vertices,
        final double[] nd,
        final int[] normals) {
        if (faceShader == null)
            return;
        shader = faceShader;
        //			process(vd,vertices,nd,normals);
        computeArray(vd, vertices, nd, normals);

        // we do outlining polygons in the renderer at the moment...
//        if (false) {
//            for (int i = 0; i < vertices.length - 1;)
//                processLine(vd, vertices[i++], vertices[i]);
//            processLine(vd, vertices[vertices.length - 1], vertices[0]);
//        }
    }

    final void startGeometry(Geometry geom) {
        if(lineShader!=null)  lineShader.startGeometry(geom);
        if(faceShader!=null)  faceShader.startGeometry(geom);
        if(pointShader!=null) pointShader.startGeometry(geom);
        if(geom instanceof PointSet) {
            vertexColor=((PointSet)geom).getVertexAttributes(Attribute.COLORS);
            vertexColors=vertexColor!=null;
            if(vertexColors)
              vertexColorLength=vertexColor.getStorageModel().getDimensions()[1];
//            System.out.println("PolygonPipeline.startGeometry("+geom.getName()
//              +"): colors: "+vertexColors);
        } else {
            vertexColor=null;
            vertexColors=false;
        }
    }

    public final void processPolygon(
        final DoubleArrayArray vd,
        final IntArray vertices,
        final DoubleArrayArray nd,
        final IntArray normals,
        final DataList texCoords ) {
        if (faceShader == null)
            return;
        shader = faceShader;
        computeArray(vd, vertices, nd, normals, texCoords);
    }

    
    private final void computeArray(
        final double[] vd,
        final int[] vertices,
        final double[] nd,
        final int[] normals) {
        if (2 * vertices.length + 6
            >= (vertexData.length - vertexCount) / Polygon.VERTEX_LENGTH)
            increaseVertexCapacity(vertices.length / 3);

        int vc = vertexCount;
        for (int i = 0; i < vertices.length; i++) {
            int vi = vertices[i];
            VecMat.transform(
                matrix,
                vd[vi++],
                vd[vi++],
                vd[vi],
                vertexData,
                vc + Polygon.SX);
            vertexData[vc + Polygon.SW] = 1;
            int ni = normals[i];
            VecMat.transformNormal(
                inverseTransposeMatrix,
                nd[ni++],
                nd[ni++],
                nd[ni],
                vertexData,
                vc + Polygon.NX);
            vc += Polygon.VERTEX_LENGTH;
            if(vertexColors) {
                DoubleArray color=vertexColor.item(vi/3).toDoubleArray();
                vertexData[vc+Polygon.R]=color.getValueAt(0);
                vertexData[vc+Polygon.G]=color.getValueAt(1);
                vertexData[vc+Polygon.B]=color.getValueAt(2);
                vertexData[vc+Polygon.A]=(vertexColorLength==4
                        ||(vertexColorLength==-1&&color.getLength()>3))?
                                                                        color.getValueAt(3): 1.;
            }
        }
        compute(vertices.length);
    }

    private final void computeArrayNoTransform(
            final double[] vd,
            final int[] vertices,
            final double[] nd,
            final int[] normals) {
        if (2 * vertices.length + 6
                >= (vertexData.length - vertexCount) / Polygon.VERTEX_LENGTH)
            increaseVertexCapacity(vertices.length / 3);

        int vc = vertexCount;
        for (int i = 0; i < vertices.length; i++) {
            int vi = vertices[i];
            
            vertexData[vc + Polygon.SX] = vd[vi++];
            vertexData[vc + Polygon.SY] = vd[vi++];
            vertexData[vc + Polygon.SZ] = vd[vi];
            vertexData[vc + Polygon.SW] = 1;
            int ni = normals[i];
            
            vertexData[vc + Polygon.NX] = nd[ni++];
            vertexData[vc + Polygon.NY] = nd[ni++];
            vertexData[vc + Polygon.NZ] = nd[ni];
            //VecMat.normalize(vertexData, vc + Polygon.NX);
            
            vc += Polygon.VERTEX_LENGTH;
        }
        compute(vertices.length);
    }
    
    private final void computeArray(
        final DoubleArrayArray vd,
        final IntArray vertices,
        final DoubleArrayArray nd,
        final IntArray normals, DataList texCoords) {
        if (2 * vertices.getLength() + 6
            >= (vertexData.length - vertexCount) / Polygon.VERTEX_LENGTH)
            increaseVertexCapacity(vertices.getLength() / 3);

        int vc = vertexCount;
        if(vd.getLengthAt(0)==3)        
        for (int i = 0; i < vertices.getLength(); i++) {
            int vi = vertices.getValueAt(i);
            DoubleArray vertex=vd.item(vi).toDoubleArray();
            VecMat.transform(
                matrix,
                vertex.getValueAt(0),
                vertex.getValueAt(1),
                vertex.getValueAt(2),
                vertexData,
                vc + Polygon.SX);
                vertexData[vc + Polygon.SW] = 1;
            int ni = normals.getValueAt(i);
            DoubleArray normal=nd.item(ni).toDoubleArray();
            VecMat.transformNormal(
                inverseTransposeMatrix,
                normal.getValueAt(0),
                normal.getValueAt(1),
                normal.getValueAt(2),
                vertexData,
                vc + Polygon.NX);
            if(vertexColors) {
                DoubleArray color=vertexColor.item(vi).toDoubleArray();
                vertexData[vc+Polygon.R]=color.getValueAt(0);
                vertexData[vc+Polygon.G]=color.getValueAt(1);
                vertexData[vc+Polygon.B]=color.getValueAt(2);
                vertexData[vc+Polygon.A]=(vertexColorLength==4
                   ||(vertexColorLength==-1&&color.getLength()>3))?
                  color.getValueAt(3): 1.;
            }
            if(useTexCoords && texCoords != null) {
                DoubleArray tc = texCoords.item(vi).toDoubleArray();
                vertexData[vc + Polygon.U] = tc.getValueAt(0);
                vertexData[vc + Polygon.V] = tc.getValueAt(1);
                
            }
            vc += Polygon.VERTEX_LENGTH;
        } else // vertices are 4 tupel
        for (int i = 0; i < vertices.getLength(); i++) {
            int vi = vertices.getValueAt(i);
            DoubleArray vertex=vd.item(vi).toDoubleArray();
            VecMat.transform(
                matrix,
                vertex.getValueAt(0),
                vertex.getValueAt(1),
                vertex.getValueAt(2),
                vertex.getValueAt(3),
                vertexData,
                vc + Polygon.SX);
                if(vertexData[vc + Polygon.SW] != 0) {
                    final double d = vertexData[vc + Polygon.SW];
                    vertexData[vc + Polygon.SX] /= d;    
                    vertexData[vc + Polygon.SY] /= d;    
                    vertexData[vc + Polygon.SZ] /= d;    
                    vertexData[vc + Polygon.SW] = 1;    
                }
                int ni = normals.getValueAt(i);
            DoubleArray normal=nd.item(ni).toDoubleArray();
            VecMat.transformNormal(
                inverseTransposeMatrix,
                normal.getValueAt(0),
                normal.getValueAt(1),
                normal.getValueAt(2),
                //normal.getValueAt(3),
                vertexData,
                vc + Polygon.NX);
            if(vertexColors) {
                DoubleArray color=vertexColor.item(vi).toDoubleArray();
                vertexData[vc+Polygon.R]=color.getValueAt(0);
                vertexData[vc+Polygon.G]=color.getValueAt(1);
                vertexData[vc+Polygon.B]=color.getValueAt(2);
                vertexData[vc+Polygon.A]=(vertexColorLength==4
                   ||(vertexColorLength==-1&&color.getLength()>3))?
                  color.getValueAt(3): 1.;
            }
            if(useTexCoords && texCoords != null) {
                DoubleArray tc = texCoords.item(vi).toDoubleArray();
                vertexData[vc + Polygon.U] = tc.getValueAt(0);
                vertexData[vc + Polygon.V] = tc.getValueAt(1);
                
            }
            vc += Polygon.VERTEX_LENGTH;
        }
        compute(vertices.getLength());
    }

    private final void compute(int verticesLength) {

//        boolean isTransparent = shader.getVertexShader().getTransparency() != 0.;

        if (polygonCount + 1 >= this.polygons.length)
            increasePolygonCapacity();

        Polygon p = polygons[polygonCount++];
        p.length = verticesLength;

        //We store the current shader for later shading			
        p.setShader(shader);

        for (int i = 0; i < verticesLength; i++) {
            p.vertices[i] = vertexCount;
            vertexCount += Polygon.VERTEX_LENGTH;
        }
        
        // sky box shaders need the matrix...
        environment.setMatrix(matrix);
        //****
        shader.shadePolygon(p, vertexData, environment);
        
        
        int numClip = environment.getNumClippingPlanes();
        if(numClip>0) {
            ClippingPlaneSoft[] cp = environment.getClippingPlanes();
            //Intersector.dehomogenize(p,vertexData);
            for(int k =0; k<numClip;k++) {
                double d =VecMat.dot(cp[k].getNormal(),cp[k].getPoint());
                //System.out.println("d "+d);
                int res =Intersector.clipToHalfspace(p,cp[k].getNormal(),-1,d,this);
                if(res == -1){ // clipped out
                    p.setShader(null);
                    vertexCount -= p.length * Polygon.VERTEX_LENGTH;
                    polygonCount--;
                    return;
                }
                
            }
        
        }
        for (int i = 0; i < p.length; i++) {
            perspective.perspective(vertexData, p.vertices[i]);
        }
        
        boolean clippedAway = clipPolygon();
        
        if (!clippedAway) {
            //if (p.length == 0)
            //    System.out.println("ZEROLENGTH!");
            //TODO: check wether we sort before computing this. 
            //boolean isTransparent = (shader.getVertexShader().getTransparency() != 0.)||shader.hasTexture()||shader.interpolateAlpha();
            //shader might have changed after lighting:
            boolean isTransparent = p.getShader().needsSorting(); 
////            isTransparent = false;
            if(triangulate) {
                PolygonShader ps = p.getShader();
                if (polygonCount + 1 >= this.polygons.length)
                    increasePolygonCapacity();
                polygonCount--;
                for(int i = 0,pl =p.length-2; i<pl;i++) {
                    Polygon pi = polygons[polygonCount++];
                    pi.length =3;
                    pi.vertices[0] =p.vertices[0];
                    pi.vertices[1] =p.vertices[i+1];
                    pi.vertices[2] =p.vertices[i+2];
                    pi.setShader(ps);
                    if (queueOpaque || isTransparent) {
                        if(sortOpaque || isTransparent)
                            pi.computeCenterZ(vertexData);
                            //pi.computeMaxZ(vertexData);
                        //return;
                    } else {
                        renderer.renderPolygon(pi, vertexData, ps.isOutline());
                        pi.setShader(null);
                        //TODO:check wether this is save
                        //vertexCount -= p.length * Polygon.VERTEX_LENGTH;
                        polygonCount--;
                    }
                }
                return;
            } else {

            if (queueOpaque || isTransparent) {
				if(sortOpaque || isTransparent)
					p.computeCenterZ(vertexData);
				    //p.computeMaxZ(vertexData);
				return;
            } else {
				renderer.renderPolygon(p, vertexData, p.getShader().isOutline());
            }
            }
        }
        // if the polygon was clipped completely or if the polygon was opaque and therefore 
        //rendered already, we can free its resources: 
		p.setShader(null);
        vertexCount -= p.length * Polygon.VERTEX_LENGTH;
        polygonCount--;
    }

    public int getFreeVertex() {
//        if(vertexCount*Polygon.VERTEX_LENGTH>=vertexData.length) increaseVertexCapacity(1);
        if(vertexCount>=vertexData.length) increaseVertexCapacity(1);
        int vc =vertexCount;
        vertexCount+= Polygon.VERTEX_LENGTH;
        return vc;
    }
    public void freePolygon(int idx) {
        //polygons[idx].setShader(null);
        if(!(idx == polygonCount-1)) {
            Polygon p = polygons[idx];
            polygons[idx] = polygons[polygonCount-1];
            polygons[polygonCount-1] = p;
        }
            polygonCount--;
    }
    public int getFreePolygon() {
        if(polygonCount>=polygons.length-1) increasePolygonCapacity();
        int pc =polygonCount;
        polygonCount++;
        return pc;
    }
    
    /**
     * @param p
     */
    private final boolean clipPolygon() {
        int polPos = polygonCount - 1;
        Polygon p1 = polygons[polPos];
        ClippingBox box = perspective.getFrustum();
        int x0out = 0;
        int x1out = 0;
        int y0out = 0;
        int y1out = 0;
        int z0out = 0;
        int z1out = 0;

        int n = p1.length;
        // TODO check maximal polygon vertex num here:...if applicable...
        
        // after clipping at the clipping plane vertices need not be in order anymore! 
        //for (int v = p1.vertices[0], i = n;
        //i > 0;
        //i--, v += Polygon.VERTEX_LENGTH) {
        for (int v = p1.vertices[0], i = 0;
        i <n;
        i++ ) {
            v = p1.vertices[i];
            double vsw =vertexData[v+Polygon.SW];
            if (vertexData[v + Polygon.SX] < box.x0 * vsw
                )
                x0out++;
            if (vertexData[v + Polygon.SX] > box.x1 * vsw
                )
                x1out++;

            if (vertexData[v + Polygon.SY] < box.y0 * vsw
                )
                y0out++;
            if (vertexData[v + Polygon.SY] > box.y1 * vsw
                )
                y1out++;

            if (vertexData[v + Polygon.SZ] < box.z0 * vsw
                )
                z0out++;
            if (vertexData[v + Polygon.SZ] > box.z1 * vsw
                )
                z1out++;

        }
        if (x0out + x1out + y0out + y1out + z0out + z1out == 0) {
            //shader.shadePolygon(p1, vertexData, environment);
            return false; /*POLY_CLIP_IN*/
        }
        if (x0out == n
            || x1out == n
            || y0out == n
            || y1out == n
            || z0out == n
            || z1out == n) {
            //p1->n = 0;
            //System.out.println("OUT "+(n*Polygon.VERTEX_LENGTH));
            return true; /*POLY_CLIP_OUT*/
        }
        //shader.shadePolygon(p1, vertexData, environment);
        // now clip:
        //if (x0out) CLIP_AND_SWAP(sx, -1., box->x0, p, q, r);
        if (x0out != 0)
            clipToHalfspace(polPos, polygons, Polygon.SX, -1, -box.x0);
        if (x1out != 0)
            clipToHalfspace(polPos, polygons, Polygon.SX, 1, box.x1);

        if (y0out != 0)
            clipToHalfspace(polPos, polygons, Polygon.SY, -1, -box.y0);
        if (y1out != 0)
            clipToHalfspace(polPos, polygons, Polygon.SY, 1, box.y1);

        if (z0out != 0)
            clipToHalfspace(polPos, polygons, Polygon.SZ, -1, -box.z0);
        if (z1out != 0)
            clipToHalfspace(polPos, polygons, Polygon.SZ, 1, box.z1);
        if (p1.length == 0)
            return true;
        else
            return false;
    }

    /**
     * This helper method assumes that the polygon following the one at polPos is free.
     * @param polPos the position of the polygon in thepolygons array
     * @param index The index to compare --- e.g. Polyggn.SX
     * @param sign +/-1 which side of the halfspace.
     * @param k the location of the halfspace --- e.g. ClippingBox.x0
     */
    private void clipToHalfspace(
        int polPos,
        Polygon polygons[],
        int index,
        int sign,
        double k) {

        Polygon p = polygons[polPos];
        if (p.length == 0)
            return;
        //int vc = vertexCount;
        int u = p.vertices[p.length - 1];
        int v = p.vertices[0];
        double tu =
            sign * vertexData[u + index] - k  * vertexData[u+Polygon.SW];
            //sign * vertexData[u + index] / vertexData[u+Polygon.SW] -k;
        double tv = 0;
        //HERE!!!!!
        Polygon newP = polygons[polPos + 1];
        newP.length = 0;
        for (int i = 0; i < p.length; i++, u = v, tu = tv, v = p.vertices[i]) {
            tv =
                sign * vertexData[v + index] - k * vertexData[v+Polygon.SW];
                //sign * vertexData[v + index] / vertexData[v+Polygon.SW] - k;
            if (tu <= 0. ^ tv <= 0.) { // edge crosses plane...
                double t = tu / (tu - tv);

                for (int j = 0; j < Polygon.VERTEX_LENGTH; j++) {
                    vertexData[vertexCount + j] =
                        vertexData[u
                            + j]
                            + t * (vertexData[v + j] - vertexData[u + j]);
                }
                newP.vertices[newP.length++] = vertexCount;
                vertexCount += Polygon.VERTEX_LENGTH;
            }
            if (tv <= 0.) { // vertex v is in ...
                //				for(int j = 0;j<Polygon.VERTEX_LENGTH;j++) {
                //					vertexData[vc+j] = vertexData[v+j];
                //				}
                newP.vertices[newP.length++] = v;
                //				vc+= Polygon.VERTEX_LENGTH;
            }
        }
        // It is left to swap:
        int vp[] = p.vertices;
        p.vertices = newP.vertices;
        p.length = newP.length;
        newP.vertices = vp;

    }

    /**
     * Sets the current matrix for the @link processPolygon method.
     * @param matrix The matrix to set
     */
    public final void setMatrix(final double[] matrix) {
        this.matrix = matrix;
        Rn.transpose(inverseTransposeMatrix,matrix);
        Rn.inverse(inverseTransposeMatrix,inverseTransposeMatrix);
    }

    public final void sortPolygons() {
        //eSystem.out.println("scheduled polys "+polygonCount);
        //it might be better to sort the non transparent polygons too
        // since the setPixel call is one of the most speed relevant. If the
        // polygons are drawn front to back (the reverse oder compared to the 
        // painter's algorithm) the setPixel calls return immediately: 
        Arrays.sort(polygons, 0, polygonCount, comp);
        //Arrays.sort(transparentPolygons,0,transparentPolygonCount,comp);
    }

    /**
     * @return Perspective
     */
    public final Perspective getPerspective() {
        return perspective;
    }

    /**
     * Sets the perspective.
     * @param perspective The perspective to set
     */
    public final void setPerspective(final Perspective perspective) {
        this.perspective = perspective;
    }

    /**
     * @return Environment
     */
    public final Environment getEnvironment() {
        return environment;
    }

    /**
     * Sets the environment.
     * @param environment The environment to set
     */
    public final void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    /**
     * @param renderer
     */
    public final void renderRemaining(final PolygonRasterizer renderer /*,final int xmin, final int xmax, final int ymin, final int ymax*/
    ) {
        //renderer.setWindow(xmin,xmax,ymin,ymax);
        //		for (int i = polygonCount; i > 0;) {
        //			renderer.renderPolygon(polygons[--i],vertexData,polygons[i].getShader().isOutline());
        //			polygons[i].setShader(null);
        //		}
        //		for (int i = 0; i < transparentPolygonCount; i++) {
        //			renderer.renderPolygon(transparentPolygons[i],vertexData,transparentPolygons[i].getShader().isOutline());
        //			transparentPolygons[i].setShader(null);
        //		}
        //System.out.println("polys "+ polygonCount);
        
        // we render the polygons front to back in order to have less to do in setPixel. transparent polys are
        // sorted in reverse order and at the beginning, so we know, that they are rendered last and in
        // back to front order.
//		for (int i = polygonCount; i > 0;) {
//			renderer.renderPolygon(polygons[--i],vertexData,polygons[i].getShader().isOutline());
//			polygons[i].setShader(null);
//		}

        for (int i = 0; i < (polygonCount); i++) {
            Polygon p = polygons[i];
            renderer.renderPolygon(p, vertexData, p.getShader().isOutline());
            p.setShader(null);
            //System.out.println("centerZ "+p.getCenterZ() );
        }

    }

    private double[] point0 = new double[4];
    private double[] normal0 = new double[3];
    private double[] point1 = new double[4];
    private double[] normal1 = new double[3];
    private double[] substMatrix = new double[16];

    /* (non-Javadoc)
     * @see de.jreality.soft.PointProcessor#processPoint(double[], int)
     */
    public final void processPoint(final double[] data, int index, int length) {
        if (pointShader == null)
            return;
        if(length == 4)
            processPoint(data[index], data[index + 1], data[index + 2], data[index + 3]);
        else
            processPoint(data[index], data[index + 1], data[index + 2],1);
    }
    public final void processPoint(final DoubleArrayArray data, int index) {
        DoubleArray da=data.item(index).toDoubleArray();
        double w = 1;
        if(da.size()==4) w = da.getValueAt(3);
        if(vertexColors) {
            DoubleArray color=vertexColor.item(index).toDoubleArray();
            for(int i = 0; i<16;i++) {
            pointColors[i][0]=color.getValueAt(0);
            pointColors[i][1]=color.getValueAt(1);
            pointColors[i][2]=color.getValueAt(2);
            pointColors[i][3]=(vertexColorLength==4
                    ||(vertexColorLength==-1&&color.getLength()>3))?
                                                                    color.getValueAt(3): 1.;
            }
            vertexColor = pointColorDataList;
        }
        processPoint(da.getValueAt(0), da.getValueAt(1), da.getValueAt(2),w);
    }
    public final void processPoint(
        final double x,
        final double y,
        final double z,
        final double w) {
        if (pointShader == null)
            return;
        VecMat.transform(matrix, x, y, z,w, point0, 0);
        VecMat.transformUnNormalized(matrix, 0, 0, pointRadius, normal0, 0);
        double[] mat = matrix;
        double[] tmat = inverseTransposeMatrix;
        matrix = substMatrix;
        double d = 1 - outlineFraction;
        // inner point :
        double l = VecMat.norm(normal0);
        VecMat.assignScale(matrix, l * d);
        matrix[4 * 0 + 3] = point0[0];
        matrix[4 * 1 + 3] = point0[1];
        matrix[4 * 2 + 3] = point0[2]+pointRadius;
        Rn.transpose(inverseTransposeMatrix,matrix);
        Rn.inverse(inverseTransposeMatrix,inverseTransposeMatrix);
        shader = pointShader.getCoreShader();
        computeArray(pointVertices, pointIndices, zNormal, pointNormals);

        // outline :
        if (outlineFraction > 0 /*&& pointShader!= null*/) {
            shader = pointShader.getOutlineShader();
            d = 1 / d;

            for (int i = 0; i < 24; i+=3) {
                pointVertices[24 + i] = pointVertices[i] * d;
                pointVertices[25 + i] = pointVertices[i+1] * d;
                pointVertices[26 + i] = -d;
            }
            for (int i = 0; i < 8; i++) {
                computeArray(
                    pointVertices,
                    pointOutlineIndices[i],
                    zNormal,
                    pointOutlineNormals[i]);
            }
        }

        // reset the matrix:
        matrix = mat;
        inverseTransposeMatrix = tmat;
        
    }

    private static final double[] identity =
        new double[] { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 };
    private double[] line = new double[12];
    private double[] direction = new double[3];
    /* (non-Javadoc)
     * @see de.jreality.soft.LineProcessor#processLine(double[], int, int)
     */
    public final void processLine(DoubleArray from, DoubleArray to) {
        if (lineShader == null)
            return;
        shader = lineShader.getPolygonShader();
        double w = 1;
        if(from.size()==4) w = from.getValueAt(3); 
        VecMat.transform(matrix,
            from.getValueAt(0), from.getValueAt(1), from.getValueAt(2),
            1, point0, 0);
        
        VecMat.transformUnNormalized(matrix, 0, 0, lineWidth, normal0, 0);

        w = 1;
        if(to.size()==4) w = from.getValueAt(3); 
        VecMat.transform(matrix,
            to.getValueAt(0), to.getValueAt(1), to.getValueAt(2),
            1, point1, 0);
        //VecMat.transformUnNormalized(matrix,0,0,.2,normal0,0);

        double[] mat = matrix;
        double[] tmat = inverseTransposeMatrix;
        matrix = identity;
        inverseTransposeMatrix =identity;
        //VecMat.assignIdentity(matrix);

        direction[0] = point1[0] - point0[0];
        direction[1] = point1[1] - point0[1];
        direction[2] = point1[2] - point0[2];
        // TODO make only one call to norm...
        if (VecMat.norm(direction) == 0) {
            matrix = mat;
            inverseTransposeMatrix =tmat;
            return;
        }
        VecMat.normalize(direction);
        double length = VecMat.norm(normal0);
        VecMat.cross(direction, zNormal, normal0);
        VecMat.normalize(normal0);
        normal0[0] *= length;
        normal0[1] *= length;
        normal0[2] *= length;

        VecMat.cross(normal0, direction, normal1);
        VecMat.normalize(normal1);

        line[0] = point0[0] - normal0[0];
        line[1] = point0[1] - normal0[1];
        line[2] = point0[2] + length; //- normal0[2];		

        line[3] = point0[0] + normal0[0];
        line[4] = point0[1] + normal0[1];
        line[5] = point0[2] + length; //+ normal0[2];

        line[6] = point1[0] + normal0[0];
        line[7] = point1[1] + normal0[1];
        line[8] = point1[2] + length; //+ normal0[2];

        line[9]  = point1[0] - normal0[0];
        line[10] = point1[1] - normal0[1];
        line[11] = point1[2] + length; // + normal0[2];

        computeArray(line, lineIndices, normal1, pointNormals);
        matrix = mat;
        inverseTransposeMatrix =tmat;
        
    }

    private double[] normal2 = new double[6];
    private static int[] nIndices = {0,3,3,0};
    private static final double cs = Math.cos(0.2);
    private static final double ss = Math.sin(0.2);
    
    private double test1[] =new double [3];
    private double test2[] =new double [3];
    private DoubleArray tda1 = new DoubleArray(test1);
    private DoubleArray tda2 = new DoubleArray(test2);
    
    private double test3[] =new double [3];
    private double test4[] =new double [3];
    private DoubleArray tda3 = new DoubleArray(test3);
    private DoubleArray tda4 = new DoubleArray(test4);
    private double[] zzNormal = new double[3];
    
    public final void processPseudoTube(DoubleArray from, DoubleArray to) {
        if (lineShader == null)
            return;
        shader = lineShader.getPolygonShader();
        double w = 1;
        if(from.size()==4) w = from.getValueAt(3); 
        VecMat.transform(matrix,
                from.getValueAt(0), from.getValueAt(1), from.getValueAt(2),
                w, point0, 0);
        
        VecMat.transformUnNormalized(matrix, 0, 0, lineWidth, normal0, 0);
        w = 1;
        if(to.size()==4) w = to.getValueAt(3); 
        VecMat.transform(matrix,
                to.getValueAt(0), to.getValueAt(1), to.getValueAt(2),
                w, point1, 0);
       

        double[] mat = matrix;
        double[] tmat = inverseTransposeMatrix;
        matrix = identity;
        inverseTransposeMatrix =identity;
        

        direction[0] = point1[0] - point0[0];
        direction[1] = point1[1] - point0[1];
        direction[2] = point1[2] - point0[2];
        
        //TODO: decide: better but slower:
        zzNormal[0] = point1[0] + point0[0];
        zzNormal[1] = point1[1] + point0[1];
        zzNormal[2] = point1[2] + point0[2];
        VecMat.normalize(zzNormal);

        // TODO make only one call to norm...
        if (VecMat.norm(direction) == 0) {
            matrix = mat;
            inverseTransposeMatrix =tmat;
            
            return;
        }
        VecMat.normalize(direction);
        double length = VecMat.norm(normal0);
        VecMat.cross(direction, zzNormal, normal0);
        VecMat.normalize(normal0);
        VecMat.cross(normal0, direction, normal1);
        
        normal0[0] *= length;
        normal0[1] *= length;
        // allways zero:
        normal0[2] *= length;
        
        
        VecMat.normalize(normal1);
        
        if(VecMat.dot(normal1,zzNormal)<0) {
            normal1[0] *=-1;
            normal1[1] *=-1;
            normal1[2] *=-1;
            System.out.println("flip");
        }

        line[0] = point0[0] - normal0[0];
        line[1] = point0[1] - normal0[1];
        line[2] = point0[2] ;       

        line[3] = point0[0] ;
        line[4] = point0[1] ;
        line[5] = point0[2] + length;
        
        line[6] = point1[0] ;
        line[7] = point1[1] ;
        line[8] = point1[2] + length;

        line[9]  = point1[0] - normal0[0];
        line[10] = point1[1] - normal0[1];
        line[11] = point1[2] ;
        
        normal2[0] = +cs/length*normal0[0] + ss*normal1[0];
        normal2[1] = +cs/length*normal0[1] + ss*normal1[1];
        normal2[2] = ss*normal1[2];
        
        normal2[3] = normal1[0];
        normal2[4] = normal1[1];
        normal2[5] = normal1[2];
        
        
        computeArrayNoTransform(line, lineIndices, normal2, nIndices);
        
        /////
        
        line[0] = point0[0] ;
        line[1] = point0[1] ;
        line[2] = point0[2] + length;     

        line[3] = point0[0] + normal0[0];
        line[4] = point0[1] + normal0[1];
        line[5] = point0[2] ;

        line[6] = point1[0] + normal0[0];
        line[7] = point1[1] + normal0[1];
        line[8] = point1[2] ;

        line[9]  = point1[0] ;
        line[10] = point1[1] ;
        line[11] = point1[2] + length;
        
        normal2[0] = normal1[0];
        normal2[1] = normal1[1];
        normal2[2] = normal1[2];

        normal2[3] = -cs/length*normal0[0] + ss*normal1[0];
        normal2[4] = -cs/length*normal0[1] + ss*normal1[1];
        normal2[5] = ss*normal1[2];
        
        computeArrayNoTransform(line, lineIndices, normal2, nIndices);
        
        /*
        test1[0] = point1[0];
        test1[1] = point1[1];
        test1[2] = point1[2];
        test2[0] = point1[0]+normal1[0];
        test2[1] = point1[1]+normal1[1];
        test2[2] = point1[2]+normal1[2];
        
        
        
        
        test3[0] = point0[0];
        test3[1] = point0[1];
        test3[2] = point0[2];
        test4[0] = point0[0]+normal1[0];
        test4[1] = point0[1]+normal1[1];
        test4[2] = point0[2]+normal1[2];
        
        processLine(tda1,tda2);
        processLine(tda3,tda4);
        
        */
        
        matrix = mat;
        inverseTransposeMatrix =tmat;
        
    }
    
    /**
     * @return PolygonShader
     */
    public PolygonShader getFaceShader() {
        return faceShader;
    }

    /**
     * Sets the faceShader.
     * @param faceShader The faceShader to set
     */
    public void setFaceShader(PolygonShader faceShader) {
        this.faceShader = faceShader;
    }

    /**
     * @return double
     */
    public double getLineWidth() {
        return lineWidth;
    }

    /**
     * @return double
     */
    public double getPointRadius() {
        return pointRadius;
    }

    /**
     * Sets the lineWidth.
     * @param lineWidth The lineWidth to set
     */
    public void setLineWidth(double lineWidth) {
        this.lineWidth = lineWidth;
    }

    /**
     * Sets the pointRadius.
     * @param pointRadius The pointRadius to set
     */
    public void setPointRadius(double pointRadius) {
        this.pointRadius = pointRadius;
    }

    /**
     * @return PolygonShader
     */
    public LineShader getLineShader() {
        return lineShader;
    }

    /**
     * @return PolygonShader
     */
    public PointShader getPointShader() {
        return pointShader;
    }

    /**
     * Sets the lineShader.
     * @param lineShader The lineShader to set
     */
    public void setLineShader(LineShader lineShader) {
        this.lineShader = lineShader;
        if(lineShader != null) lineWidth =lineShader.getLineWidth();
    }

    /**
     * Sets the pointShader.
     * @param pointShader The pointShader to set
     */
    public void setPointShader(PointShader pointShader) {
        this.pointShader = pointShader;
        if(pointShader!= null) {
            pointRadius = pointShader.getPointRadius();
            outlineFraction =pointShader.getOutlineFraction();
        }
    }

    /**
     * @return PolygonShader
     */
//    public PolygonShader getPointOutlineShader() {
//        return pointOutlineShader;
//    }

    /**
     * Sets the pointOutlineShader.
     * @param pointOutlineShader The pointOutlineShader to set
     */
//    public void setPointOutlineShader(PolygonShader pointOutlineShader) {
//        this.pointOutlineShader = pointOutlineShader;
//    }

    /**
     * @return double
     */
    public double getOutlineFraction() {
        return outlineFraction;
    }

    /**
     * Sets the outlineFraction.
     * @param outlineFraction The outlineFraction to set
     */
    public void setOutlineFraction(double outlineFraction) {
        this.outlineFraction = outlineFraction;
    }
    public int copyPolygon(Polygon p) {
        int newP =getFreePolygon();
        Polygon np =polygons[newP];
        np.length =p.length;
        for (int i = 0; i < p.length; i++) {
            np.vertices[i] =p.vertices[i];
            np.setShader(p.getShader());
            np.setCenterZ(p.getCenterZ());
        }
        return newP;
    }
}
