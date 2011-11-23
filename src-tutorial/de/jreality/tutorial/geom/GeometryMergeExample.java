package de.jreality.tutorial.geom;

import static de.jreality.shader.CommonAttributes.VERTEX_DRAW;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

import de.jreality.geometry.GeometryMergeFactory;
import de.jreality.geometry.IndexedFaceSetFactory;
import de.jreality.geometry.Primitives;
import de.jreality.math.MatrixBuilder;
import de.jreality.plugin.JRViewer;
import de.jreality.scene.Appearance;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.data.Attribute;
import de.jreality.shader.CommonAttributes;
import de.jreality.util.SceneGraphUtility;

/**
 * This example shows how to use the {@link GeometryMergeFactory}, which combines all geometries in a scene graph into
 * a single {@link IndexedFaceSet}.
 * 
 * @author Bernd Gonska
 *
 */
public class GeometryMergeExample {

	public static void main(String[] args) {
		// a little Scene (two boxes and a bangle, transfomation, appearance)
        int i,j,k;
        IndexedFaceSetFactory ifsf = new IndexedFaceSetFactory();
        
        double [][] vertices  = new double[][] {
          {0, 0, 0}, {1, 0, 0}, {1, 1, 0}, {0, 1, 0},
          {0, 0, 1}, {1, 0, 1}, {1, 1, 1}, {0, 1, 1}
        };
        
        int [][] faceIndices = new int [][] {
          {0, 1, 2, 3}, {7, 6, 5, 4}, {0, 1, 5, 4}, 
          {2, 3, 7, 6}, {1, 2, 6, 5}, {3, 0, 4, 7} 
        };
        
        int [][] edgeIndices = new int [][] {
        		{0,1},{1,2},{2,6},{6,7},{7,4},{4,0}
        };
        
        Color[] faceColors = new Color[]{
          Color.BLUE, Color.BLUE, Color.GREEN, Color.GREEN, Color.RED, Color.RED 
        };
        ifsf.setVertexCount( vertices.length );
        ifsf.setVertexCoordinates( vertices );
        ifsf.setFaceCount( faceIndices.length);
        ifsf.setFaceIndices( faceIndices ); 
        ifsf.setFaceColors(faceColors);    
        ifsf.setGenerateFaceNormals( true );
        ifsf.setGenerateEdgesFromFaces(false);
        ifsf.update();
        
        IndexedFaceSet boxx[][][] = new IndexedFaceSet[100][100][100];
		SceneGraphComponent childNode[][][] = new SceneGraphComponent[100][100][100];
		SceneGraphComponent original= new SceneGraphComponent();
        Appearance app= new Appearance();
        app.setAttribute(VERTEX_DRAW, false);
        for (i=0; i<20; i++)
        	for(j=0; j<20; j++ )
        		for(k=0; k<20; k++ )               	
        	{
        		boxx[i][j][k]=ifsf.getIndexedFaceSet();
        		
        		childNode[i][j][k]= new SceneGraphComponent();
        		MatrixBuilder.euclidean().translate((2*i),(2*j),(2*k)).assignTo(childNode[i][j][k]);
        		original.addChild(childNode[i][j][k]);
        		
        		childNode[i][j][k].setGeometry(boxx[i][j][k]);
        	}
		original.setGeometry(boxx[0][0][0]);
		
		//IndexedFaceSet box= Primitives.box(0.1, 0.1, 0.1, false);
        //IndexedFaceSet box2= Primitives.box(0.1, 0.1, 0.1, true);
        //IndexedFaceSet zyl= Primitives.cylinder(20,1,0,.5,5);
        
        //SceneGraphComponent childNode1= new SceneGraphComponent();
        
        //MatrixBuilder.euclidean().translate(0,0,0.1).assignTo(childNode1);
        //SceneGraphComponent childNode2= new SceneGraphComponent();

        //app.setAttribute(CommonAttributes.POLYGON_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, new Color(255,255,0));
        //childNode2.setAppearance(app);
        //original.addChild(childNode1);
        //original.addChild(childNode2);
        //original.setGeometry(box);
        //childNode1.setGeometry(box2);
        //childNode2.setGeometry(zyl);
// the Factory:
        GeometryMergeFactory mergeFact= new GeometryMergeFactory();             
// play with the following 3 optional settings (by default they are true)
        mergeFact.setRespectFaces(false);
        mergeFact.setRespectEdges(false);
        mergeFact.setGenerateVertexNormals(false);                       
// you can set some defaults:
        List defaultAtts= new LinkedList();
        List defaultAttValue= new LinkedList();
        List value= new LinkedList();
        defaultAtts.add(Attribute.COLORS);
        defaultAttValue.add(value);
        value.add(new double[]{0,0,0,1});// remember: only 4d colors
        mergeFact.setDefaultFaceAttributes(defaultAtts,defaultAttValue );
// merge a list of geometrys:
        //IndexedFaceSet[] list= new IndexedFaceSet[]{box2,zyl};
        //IndexedFaceSet result=mergeFact.mergeIndexedFaceSets(list);
// or  a complete tree:
        //IndexedFaceSet result=mergeFact.mergeGeometrySets(original);
        SceneGraphComponent world = SceneGraphUtility.createFullSceneGraphComponent("world");
        SceneGraphComponent merged =  SceneGraphUtility.createFullSceneGraphComponent("merged");
       	//merged.setGeometry(result);
       	//MatrixBuilder.euclidean().translate(2,0,0).assignTo(merged);
       	world.addChildren(merged, original);
        world.setAppearance(app);
       	JRViewer.display(world);
	}

}
