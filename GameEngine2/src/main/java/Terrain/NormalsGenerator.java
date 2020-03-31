package Terrain;

import org.joml.Vector3f;

public class NormalsGenerator {
    public static Vector3f[][] generateNormals(float[][] heights){
        Vector3f[][] normals = new Vector3f[heights.length][heights.length];
        for(int z=0;z<normals.length;z++){
            for(int x=0;x<normals[z].length;x++){
                normals[z][x] = calculateNormal(x, z, heights);
            }
        }
        return normals;
    }

    private static Vector3f calculateNormal(int x, int z, float[][] heights){
        float heightL = getHeight(x-1, z, heights);
        float heightR = getHeight(x+1, z, heights);
        float heightD = getHeight(x, z-1, heights);
        float heightU = getHeight(x, z+1, heights);
        Vector3f normal = new Vector3f(heightL-heightR, 2f, heightD - heightU);
        normal.normalize();
        return normal;
    }

    private static float getHeight(int x, int z, float[][] heights){
        x = x < 0 ? 0 : x;
        z = z < 0 ? 0 : z;
        x = x >= heights.length ? heights.length-1 : x;
        z = z >= heights.length ? heights.length-1 : z;
        return heights[z][x];
    }
}