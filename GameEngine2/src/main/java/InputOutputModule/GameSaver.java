package InputOutputModule;

import java.io.FileWriter;
import java.io.IOException;

public class GameSaver {
    //TODO save the game info when this function is called (goal location, ball location, terrain, etc)
    /*
   TODO make the file path relative, load the goal location, starting location, 'score radius', terrain width, terrain height (check project manual) and
   set these values for the objects themselves (ie set location of items)
    */
    public static void saveGameFile(String fullPath){
//        String terrainInfo = MainGameLoop.terrain.getTerrainInfoAsString();
//        String treeInfo = MainGameLoop.trees.getTreeInfoAsString();

        //TODO link to input
        String gravitationalConstant = "g = 9.81;   // Gravitational acceleration [m/s^2]";
        String massOfBall = "m = 45.93;  // Mass of ball [g]";
        String frictionCoefficient = "mu = 0.131; // Coefficient of friction (rolling ball)\n             // Typical 0.065<=mu<=0.196 ";
        String vMax = "vmax = 3;   // Maximum initial ball speed [m/s] ";
        String goalRadius = "tol = 0.02; // Distance from hole for a successful putt [m] ";
        String startCoordinates2D = "start = (0.0, 0.0); ";
        String goalCoordinates2D = "goal = (0.0, 10.0); ";
        String heightFunction = "height = -0.01*x + 0.003*x^2 + 0.04 * y; ";

        try {
            FileWriter writer = new FileWriter("terrainSaveFile.txt");

            writer.write(gravitationalConstant);
            writer.write("\n");
            writer.write(massOfBall);
            writer.write("\n");
            writer.write(frictionCoefficient);
            writer.write("\n");
            writer.write(vMax);
            writer.write("\n");
            writer.write(goalRadius);
            writer.write("\n");
            writer.write("\n");
            writer.write(startCoordinates2D);
            writer.write("\n");
            writer.write(goalCoordinates2D);
            writer.write("\n");
            writer.write("\n");
            writer.write(heightFunction);

            writer.close();
            System.out.println("Saved");
        } catch (IOException e) {
            System.out.println("Something went wrong with saving the file");
            e.printStackTrace();
        }
    }
}
