/**
 * PolyLine3D.java
 *
 * Copyright (c) 2013-2018, F(X)yz
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of F(X)yz, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL F(X)yz BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 

package org.fxyz3d.shapes.primitives;

import java.util.List;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.utils.MeshUtils;

/**
 *
 * @author Sean
 */
public class PolyLine3D extends MeshView {
    
    private List<Point3D> points;
    private static float DEFAULT_WIDTH = 1.0f;
    private float width = DEFAULT_WIDTH;
    private TriangleMesh mesh;
    public static enum LineType {RIBBON, CENTER_RIBBON, TRIANGLE};

    public PolyLine3D() {
        this(null, DEFAULT_WIDTH);
    }

    public PolyLine3D(List<Point3D> points, float width) {
        this(points, width,  LineType.CENTER_RIBBON);
    }

    //Creates a PolyLine3D with the user's choice of mesh style
    public PolyLine3D(List<Point3D> points, float width, LineType lineType ) {
        this.points = points;
        this.width = width;
        mesh  = new TriangleMesh();
        setMesh(mesh);
        switch(lineType) {
            case TRIANGLE: buildTriangleTube(); break;
            case RIBBON:  buildRibbon(); break;
            case CENTER_RIBBON:
            default:
                buildCenterRibbon(); break;
        }

        //Make sure you Cull the Back so that no black shows through
        setCullFace(CullFace.BACK);


    }

    public void setPoints(List<Point3D> pnts){
        points = pnts;
        buildCenterRibbon();
    }

    public void setWidth(float width){
        this.width = width;
        buildCenterRibbon();
    }

    private void buildTriangleTube() {
        //For each data point add three mesh points as an equilateral triangle
        float half = (float) (width / 2.0);
        for(Point3D point: points) {
            //-0.288675f*hw, -0.5f*hw, -0.204124f*hw,
            mesh.getPoints().addAll(point.x - 0.288675f*half, point.y - 0.5f*half, point.z - 0.204124f*half);
            //-0.288675f*hw, 0.5f*hw, -0.204124f*hw, 
            mesh.getPoints().addAll(point.x - 0.288675f*half, point.y + 0.5f*half, point.z - 0.204124f*half);
            //0.57735f*hw, 0f, -0.204124f*hw
            mesh.getPoints().addAll(point.x + 0.57735f*half, point.y + 0.5f*half, point.z - 0.204124f*half);
        }
        //add dummy Texture Coordinate
        mesh.getTexCoords().addAll(0,0); 
        //Beginning End Cap
        mesh.getFaces().addAll(0,0, 1,0, 2,0);
        //Now generate trianglestrips between each point 
        for(int i=3;i<points.size()*3;i+=3) {  //add each triangle tube segment 
            //Vertices wound counter-clockwise which is the default front face of any Triange
            //Triangle Tube Face 1
            mesh.getFaces().addAll(i+2,0, i-2,0, i+1,0); //add secondary Width face
            mesh.getFaces().addAll(i+2,0, i-1,0, i-2,0); //add primary face
            //Triangle Tube Face 2
            mesh.getFaces().addAll(i+2,0, i-3,0, i-1,0); //add secondary Width face
            mesh.getFaces().addAll(i,0, i-3,0, i+2,0); //add primary face
            //Triangle Tube Face 3
            mesh.getFaces().addAll(i,0, i+1,0, i-3,0); //add primary face
            mesh.getFaces().addAll(i+1,0, i-2,0, i-3,0); //add secondary Width face
        }        
        //Final End Cap
        int last = points.size()*3 -1;
        mesh.getFaces().addAll(last,0, last-1,0, last-2,0);
    }
    private void buildRibbon() {
        if(points == null)
            return;
        //add each point. For each point add another point shifted on Z axis by DEFAULT_WIDTH
        //This extra point allows us to build triangles later
        mesh.getPoints().clear();
        for(Point3D point: points) {
            mesh.getPoints().addAll(point.x,point.y,point.z);
            mesh.getPoints().addAll(point.x,point.y,point.z+ width);
        }
        //add dummy Texture Coordinate
        mesh.getTexCoords().setAll(0,0);
        //Now generate trianglestrips for each line segment
        mesh.getFaces().clear();
        for(int i=2;i<points.size()*2;i+=2) {  //add each segment
            //Vertices wound counter-clockwise which is the default front face of any Triange
            //These triangles live on the frontside of the line facing the camera
            mesh.getFaces().addAll(i,0,i-2,0,i+1,0); //add primary face
            mesh.getFaces().addAll(i+1,0,i-2,0,i-1,0); //add secondary Width face
            //Add the same faces but wind them clockwise so that the color looks correct when camera is rotated
            //These triangles live on the backside of the line facing away from initial the camera
            mesh.getFaces().addAll(i+1,0,i-2,0,i,0); //add primary face
            mesh.getFaces().addAll(i-1,0,i-2,0,i+1,0); //add secondary Width face
        }        
    }
    private void buildCenterRibbon() {
        if(points == null)
            return;
        int size = points.size();
        if (size < 2)
            return;

        mesh.getTexCoords().setAll(0,0);


        float[] coords = new float[size * 6];

        int index = 0;

        for (int i = 0; i < size; i++) {

            Point3D p = points.get(i);
            coords[index++] =  p.x;
            coords[index++] =  p.y;
            coords[index++] =  p.z;
        }

        for (int i = size - 1; i > -1; i--) {

            Point3D p = points.get(i);
            coords[index++] =  p.x;
            coords[index++] =  p.y;
            coords[index++] =  p.z;
        }

        coords = MeshUtils.offset(coords, width / 2, 1, false);

        mesh.getPoints().setAll(coords);

        int[] faces = MeshUtils.buildBoundFaces(coords, 0);
        int[] backFaces = MeshUtils.revertFaces(faces);

        mesh.getFaces().setAll(faces);
        mesh.getFaces().addAll(backFaces);

        int[] smoothGroups = new int[mesh.getFaces().size() / 6];
        mesh.getFaceSmoothingGroups().setAll(smoothGroups);
    }

}