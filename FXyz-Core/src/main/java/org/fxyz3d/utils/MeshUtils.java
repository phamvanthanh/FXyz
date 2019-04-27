/**
 * OBJWriter.java
 *
 * Copyright (c) 2013-2016, F(X)yz
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
package org.fxyz3d.utils;

import eu.mihosoft.jcsg.CSG;
import eu.mihosoft.jcsg.Polygon;
import eu.mihosoft.jcsg.PropertyStorage;
import eu.mihosoft.vvecmath.Vector3d;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.ObservableFloatArray;
import javafx.geometry.Point3D;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.TriangleMesh;

/**
 * Loads a CSG from TriangleMesh based on JCSG from Michael Hoffer
 * 
 * @author José Pereda
 */
public class MeshUtils {
    /**
     * Loads a CSG from TriangleMesh.
     * @param mesh
     * @return CSG
     */
    private static final double SMALL = 0.1;

    public static CSG mesh2CSG(MeshView mesh) {
        return mesh2CSG(mesh.getMesh());
    }
    public static CSG mesh2CSG(Mesh mesh) {

        List<Polygon> polygons = new ArrayList<>();
        List<Vector3d> vertices = new ArrayList<>();
        if(mesh instanceof TriangleMesh){
            // Get faces
            ObservableFaceArray faces = ((TriangleMesh)mesh).getFaces();
            int[] f=new int[faces.size()];
            faces.toArray(f);

            // Get vertices
            ObservableFloatArray points = ((TriangleMesh)mesh).getPoints();
            float[] p = new float[points.size()];
            points.toArray(p);

            // convert faces to polygons
            for(int i=0; i<faces.size()/6; i++){
                int i0=f[6*i], i1=f[6*i+2], i2=f[6*i+4];
                vertices.add(Vector3d.xyz(p[3*i0], p[3*i0+1], p[3*i0+2]));
                vertices.add(Vector3d.xyz(p[3*i1], p[3*i1+1], p[3*i1+2]));
                vertices.add(Vector3d.xyz(p[3*i2], p[3*i2+1], p[3*i2+2]));
                polygons.add(Polygon.fromPoints(vertices));
                vertices = new ArrayList<>();
            }
        }

        return CSG.fromPolygons(new PropertyStorage(),polygons);
    }
    
    public static void mesh2STL(String fileName, Mesh mesh) throws IOException{

        if(!(mesh instanceof TriangleMesh)){
            return;
        }
        // Get faces
        ObservableFaceArray faces = ((TriangleMesh)mesh).getFaces();
        int[] f=new int[faces.size()];
        faces.toArray(f);

        // Get vertices
        ObservableFloatArray points = ((TriangleMesh)mesh).getPoints();
        float[] p = new float[points.size()];
        points.toArray(p);

        StringBuilder sb = new StringBuilder();
        sb.append("solid meshFX\n");

        // convert faces to polygons
        for(int i=0; i<faces.size()/6; i++){
            int i0=f[6*i], i1=f[6*i+2], i2=f[6*i+4];
            Point3D pA=new Point3D(p[3*i0], p[3*i0+1], p[3*i0+2]);
            Point3D pB=new Point3D(p[3*i1], p[3*i1+1], p[3*i1+2]);
            Point3D pC=new Point3D(p[3*i2], p[3*i2+1], p[3*i2+2]);
            Point3D pN=pB.subtract(pA).crossProduct(pC.subtract(pA)).normalize();

            sb.append("  facet normal ").append(pN.getX()).append(" ").append(pN.getY()).append(" ").append(pN.getZ()).append("\n");
            sb.append("    outer loop\n");
            sb.append("      vertex ").append(pA.getX()).append(" ").append(pA.getY()).append(" ").append(pA.getZ()).append("\n");
            sb.append("      vertex ").append(pB.getX()).append(" ").append(pB.getY()).append(" ").append(pB.getZ()).append("\n");
            sb.append("      vertex ").append(pC.getX()).append(" ").append(pC.getY()).append(" ").append(pC.getZ()).append("\n");
            sb.append("    endloop\n");
            sb.append("  endfacet\n");
        }

        sb.append("endsolid meshFX\n");

        // write file
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileName), Charset.forName("UTF-8"),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(sb.toString());
        }
    }

    public static int[] buildBoundFaces(float[] coords, int vertexOffset) {
        List<Integer> topVertices = Earcut.earcut(coords, null, 3);
        return buildFaces(topVertices, vertexOffset);
    }

    public static int[] buildFaces(List<Integer> vertices, int vertexOffset) {
        int verSize = vertices.size();
        int[] faces = new int[verSize * 2];
        int fIndex = 0;
        for (Integer v : vertices) {
            faces[fIndex] = v + vertexOffset;
            fIndex++;
            faces[fIndex] = 0;
            fIndex++;
        }
        return faces;
    }

    public static int[] revertFaces(int[] faces) {
        int[] revertFaces = new int[faces.length];
        for (int i = 0; i < faces.length - 5; i += 6) {
            revertFaces[i + 0] = faces[i + 0];
            revertFaces[i + 1] = faces[i + 1];
            revertFaces[i + 2] = faces[i + 4];
            revertFaces[i + 3] = faces[i + 5];
            revertFaces[i + 4] = faces[i + 2];
            revertFaces[i + 5] = faces[i + 3];
        }
        return revertFaces;
    }

    public static float[] offset(float[] coords, float d, int sign, boolean close) {
        if (close)
            return closeOffset(coords, d, sign);
        else
            return openOffset(coords, d, sign);
    }

    public static float[] closeOffset(float[] coords, float t, int sign) {

        int n = coords.length;
        float a, b, A, B, d, a0, b0, cross;
        float[] out = new float[n];

        // Unit vector (a,b) along last (close) edge
        a = coords[0] - coords[n - 3];
        b = coords[1] - coords[n - 2];
        d = (float) (Math.sqrt(a * a + b * b));
        if (d < Double.MIN_VALUE)
            d = 1;
        a0 = a /= d;
        b0 = b /= d;

        // Loop round the polygon, dealing with successive intersections of lines
        for (int i = 0; i < n - 4; i += 3) {
            // Unit vector (A,B) along previous edge
            A = a;
            B = b;
            // Unit vector (a,b) along next edge
            a = coords[i + 3] - coords[i];
            b = coords[i + 4] - coords[i + 1];

            d = (float) (Math.sqrt(a * a + b * b));
            if (d < Double.MIN_VALUE) { // Case two points same coordinates
                a = A;
                b = B;
            } else {
                a /= d;
                b /= d;
            }

            // New vertex
            cross = A * b - a * B;

            if (Math.abs(cross) < SMALL)      // Degenerate cases: 0 or 180 degrees at vertex
            {
                out[i] = coords[i] - t * b * sign;
                out[i + 1] = coords[i + 1] + t * a * sign;
                out[i + 2] = coords[i + 2];
            } else {                         // Usual case
                out[i] = coords[i] + sign * t * (a - A) / cross;
                out[i + 1] = coords[i + 1] + sign * t * (b - B) / cross;
                out[i + 2] = coords[i + 2];
            }
        }

        // Last vertex
        A = a;
        B = b;
        a = a0;
        b = b0;

        cross = A * b - a * B;

        if (Math.abs(cross) < SMALL) {

            out[n - 3] = coords[n - 3] - sign * t * b;
            out[n - 2] = coords[n - 2] + sign * t * a;
            out[n - 1] = coords[n - 1];

        } else { // Usual case

            out[n - 3] = coords[n - 3] + sign * t * (a - A) / cross;
            out[n - 2] = coords[n - 2] + sign * t * (b - B) / cross;
            out[n - 1] = coords[n - 1];
        }

        return out;
    }

    public static float[] openOffset(float[] coords, float t, int sign) {
        int n = coords.length;

        float a, b, A, B, d, cross;
        float[] out = new float[n];

        // Unit vector (a,b) along last edge
        a = coords[3] - coords[0];
        b = coords[4] - coords[1];
        d = (float) (Math.sqrt(a * a + b * b));
        if (d < Double.MIN_VALUE) {
            d = (float) Double.MIN_VALUE;
        }
        a /= d;
        b /= d;

        out[0] = coords[0] - t * b * sign;
        out[1] = coords[1] + t * a * sign;
        out[2] = coords[2];

        //FORWARD
        // Loop round the polygon, dealing with successive intersections of lines
        for (int i = 3; i < n - 4; i += 3) {
            // Unit vector (A,B) along previous edge
            A = a;
            B = b;
            // Unit vector (a,b) along next edge
            a = coords[i + 3] - coords[i];
            b = coords[i + 4] - coords[i + 1];

            d = (float) (Math.sqrt(a * a + b * b));
            if (d < Double.MIN_VALUE) { // Case two points same coordinates
                a = A;
                b = B;
            } else {
                a /= d;
                b /= d;
            }
            // New vertex
            cross = A * b - a * B;

            if (Math.abs(cross) < SMALL)      // Degenerate cases: 0 or 180 degrees at vertex
            {
                out[i] = coords[i] - t * b * sign;
                out[i + 1] = coords[i + 1] + t * a * sign;
                out[i + 2] = coords[i + 2];
            } else                             // Usual case
            {
                out[i] = coords[i] + sign * t * (a - A) / cross;
                out[i + 1] = coords[i + 1] + sign * t * (b - B) / cross;
                out[i + 2] = coords[i + 2];
            }
        }

        // Last vertex
        out[n - 1] = coords[n - 1]; //Z
        out[n - 2] = coords[n - 2] + sign * t * a; //y
        out[n - 3] = coords[n - 3] - sign * t * b; //x

        return out;
    }


    public static int clockwise3D(float[] coords) {
        double sum = 0;
        int length = coords.length;
        for (int i = 3; i < length - 2; i += 3) {
            sum += (coords[i] - coords[i - 3]) * (coords[i + 1] + coords[i - 2]);
        }

        sum += (coords[0] - coords[length - 3]) * (coords[1] + coords[length - 2]);
        if (sum > 0)
            return -1;
        return 1;
    }
}