package MainGame;

import FontMeshCreator.FontType;
import FontMeshCreator.GUIText;
import FontRendering.TextMaster;
import GUI.GUIRenderer;
import GUI.GUITexture;
import GUI.Menu.MainMenu;
import GUIElements.Buttons.AbstractButton;
import GUIElements.Buttons.InterfaceButton;
import GUIElements.Slider;
import GUIElements.UIElement;
import GUIElements.UIGroup;
import InputOutputModule.GameLoader;
import InputOutputModule.GameSaver;
import Physics.*;
import Entities.*;
import Models.TexturedModel;
import MouseHandler.MouseHandler;
import OBJConverter.ModelData;
import OBJConverter.OBJFileLoader;
import RenderEngine.*;
import Models.RawModel;
import Shaders.WaterShader;
import Terrain.Terrain;
import Textures.ModelTexture;
import Textures.TerrainTexture;
import Textures.TerrainTexturePack;
import Toolbox.Maths;
import Toolbox.MousePicker;
import Water.WaterFrameBuffers;
import Water.WaterTile;
import com.sun.tools.javac.Main;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.liquidengine.legui.input.Mouse;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import org.apache.commons.lang3.StringUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.awt.*;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.DoubleBuffer;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class MainGame extends CrazyPutting {
    //Settings
    static final boolean editMode = true;

    //10 units in-engine = 1 meter
    private final int SCALE = GameStaticData.SCALE;
    private final int TERRAIN_SIZE = course.TERRAIN_SIZE;

    //Edit variables
    //0 = place items, 1 = remove items, 66 = debug, -1 is game mode
    int objectType = -1;
    Vector3f terrainPoint;
    final float REMOVE_DISTANCE = SCALE*2;
    final float EDIT_SAND_DISTANCE = SCALE*2;
    int oldLeftMouseButtonState = GLFW_RELEASE;
    int oldRightMouseButtonState = GLFW_RELEASE;
    boolean deleteEditMode = false;

    private final Loader loader = new Loader();
    private List<Entity> entities = new ArrayList<>();
    private List<UIElement> GUIelements = new ArrayList<>();
    private List<UIGroup> GUIgroups = new ArrayList<>();
    private List<WaterTile> waters = new ArrayList<WaterTile>();

    private Light light;
    private Terrain terrain;
    private Camera camera;
    private MasterRenderer masterRenderer;
    private WaterFrameBuffers waterFrameBuffers;
    private GUIRenderer guiRenderer;
    private MousePicker mousePicker;
    private WaterTile mainWaterTile;

    private WaterShader waterShader;
    private WaterRenderer waterRenderer;

    private Trees trees;

    private boolean inputFlag = false;
    private Vector2d neededInput = new Vector2d();

    //TODO try to put it in a better place with better structure
    UIGroup shootGroup = new UIGroup();

    public MainGame(String courseFileName, int solverFlag, double graphicsRate, double physicsStep) {
        this.course = new PuttingCourse(courseFileName);

        DTIME = graphicsRate;

        if (solverFlag == 0)
            this.engine = DetermineSolver.getEulerSolver(course, physicsStep);
        else if (solverFlag == 1) 
            this.engine = DetermineSolver.getVelocityVerletSolver(course, physicsStep);
        else 
            this.engine = DetermineSolver.getRungeKutta4Solver(course, physicsStep);

        DisplayManager.createDisplay();
        GL.createCapabilities();
    }

    public void setUpModels() {
        ModelData ballModelData = OBJFileLoader.loadOBJ("ball");
        RawModel ballModel = loader.loadToVAO(ballModelData.getVertices(), ballModelData.getTextureCoords(), ballModelData.getNormals(), ballModelData.getIndices());
        TexturedModel texturedBall = new TexturedModel(ballModel, new ModelTexture(loader.loadTexture("models/BallTexture")));

        ModelData goalModelData = OBJFileLoader.loadOBJ("goal");
        RawModel goalModel = loader.loadToVAO(goalModelData.getVertices(), goalModelData.getTextureCoords(), goalModelData.getNormals(), goalModelData.getIndices());
        TexturedModel texturedGoal = new TexturedModel(goalModel, new ModelTexture(loader.loadTexture("models/GoalTexture")));

        //Special arrayList just for trees (still declared here since it shouldn't be null)
        trees = new Trees();

        course.ball = new Ball(texturedBall, new Vector3f(25*SCALE, 2*SCALE, 25*SCALE), 0, 0, 0, 1);
        course.goal = new Goal(texturedGoal, new Vector3f(25*SCALE, 2*SCALE, 26*SCALE), 0, 0, 0, 1);

        entities.add(course.ball);
        entities.add(course.goal);

        entities.addAll(trees);
    }

    public void resetPositions() {
        course.setDefaultPositions();
    }

    public void addAxes() {
        //Models and entities
        ModelData dragonModelData = OBJFileLoader.loadOBJ("dragon");
        RawModel dragonModel = loader.loadToVAO(dragonModelData.getVertices(), dragonModelData.getTextureCoords(), dragonModelData.getNormals(), dragonModelData.getIndices());
        TexturedModel texturedDragon = new TexturedModel(dragonModel, new ModelTexture(loader.loadTexture("textures/brick")));

        //Show X-axis
        for(int i=0; i<10; i++){
            TexturedModel XTexturedDragon = new TexturedModel(dragonModel, new ModelTexture(loader.loadTexture("textures/nice_sand")));
            Entity testDragonEntity = new Entity(XTexturedDragon, new Vector3f(i*5*SCALE, 5*SCALE, 0), 0, 0, 0, 1);
            entities.add(testDragonEntity);
        }

        //Show Z-axis
        for(int i=0; i<10; i++){
            TexturedModel ZTexturedDragon = new TexturedModel(dragonModel, new ModelTexture(loader.loadTexture("textures/nice_grass")));
            Entity testDragonEntity = new Entity(ZTexturedDragon, new Vector3f(0, 5*SCALE, 5*SCALE*i), 0, 0, 0, 1);
            entities.add(testDragonEntity);
        }
    }

    public void addTerrain() {
        //Terrain
        TerrainTexture grassTexture = new TerrainTexture(loader.loadTexture("textures/nice_grass"));
        TerrainTexture sandTexture = new TerrainTexture(loader.loadTexture("textures/nice_sand"));

        TerrainTexturePack terrainTexturePack = new TerrainTexturePack(grassTexture, sandTexture);

        //terrain = new Terrain(0, 0, loader, terrainTexturePack, TERRAIN_SIZE);
        terrain = new Terrain(0, 0, loader, course.height, terrainTexturePack, TERRAIN_SIZE);
    }

    public void addWater(){
        waterFrameBuffers = new WaterFrameBuffers();
        waterShader = new WaterShader();
        mainWaterTile = new WaterTile((float) (TERRAIN_SIZE/2.0), (float) (TERRAIN_SIZE/2.0), 0, TERRAIN_SIZE);
        waters.add(mainWaterTile);
    }

    public void addTrees(){
        ModelData treeModelData = OBJFileLoader.loadOBJ("tree");
        RawModel treeModel = loader.loadToVAO(treeModelData.getVertices(), treeModelData.getTextureCoords(), treeModelData.getNormals(), treeModelData.getIndices());
        TexturedModel texturedTree = new TexturedModel(treeModel, new ModelTexture(loader.loadTexture("models/TreeTexture")));

        Tree tree1 = new Tree(texturedTree, new Vector3f(25*SCALE, 2*SCALE, 27*SCALE), 0, 0, 0, 1);
        trees.add(tree1);
    }

    public void initLight() {
        light = new Light(new Vector3f(20000,20000,2000), new Vector3f(1, 1, 1));
    }

    public void initCamera() {
        //Camera
        camera = new Camera(course.ball);
    }

    public void initRenders() {
        masterRenderer = new MasterRenderer(loader);
        guiRenderer = new GUIRenderer(loader);
        waterRenderer = new WaterRenderer(loader, waterShader, masterRenderer.getProjectionMatrix(), waterFrameBuffers);
        TextMaster.init(loader);
    }

    public void initControls() {
        //MousePicker
        mousePicker = new MousePicker(camera, masterRenderer.getProjectionMatrix(), terrain);
    }

    public void setupEditMode(){
        //Handle events related to editing
        GLFW.glfwSetKeyCallback(DisplayManager.getWindow(), (handle, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_1) {
                objectType = 1;
                MouseHandler.disable();
            } else if (key == GLFW_KEY_2){
                objectType = 2;
                MouseHandler.disable();
            } else if (key == GLFW_KEY_ESCAPE){
                objectType = -1;
                MouseHandler.enable();
            } else if (key == GLFW_KEY_F5){
                //Optional TODO use this action before the game starts to load a map (requires API change)
                //GameLoader.loadGameFile("");
                entities.addAll(trees);
                terrain.updateTerrain(loader);
            } else if (key == GLFW_KEY_F10){
                //Optional TODO use this action after editing a map to save it (requires API change)
                //GameSaver.saveGameFile("");
            }
        });
    }

    public void addUI(){
        Slider powerSlider = new Slider(loader, "textures/sliderBar","textures/sliderKnob", new Vector2f(0.6f,-0.4f), new Vector2f(0.3f, 0.2f)) {
            @Override
            public void onClick(InterfaceButton button) {
                MouseHandler.disable();
                getSliderTexture().setPosition(DisplayManager.getNormalizedMouseCoordinates());

                //Calculate value of the slider between 0 and 1 (but not including 0)
                //getBackgroundTexture().getXPosition() returns the middle coordinate of the bar in screen coordinates ([-1, 1]), similarly for the button
                double barCenterPos = Maths.screenCoordinateToPixelX(getBackgroundTexture().getXPosition());
                double knobCenterPos = Maths.screenCoordinateToPixelX(getSliderTexture().getXPosition());
                //600 is a random number that works, don't question the gods
                double barWidth = TERRAIN_SIZE*getBackgroundTexture().getScale().x;
                //Math.min and Math.max ensure the value is always between 0 and 1 (including the edges)
                setValue(Math.min(Math.max((1+((knobCenterPos-barCenterPos)/barWidth))/2, 0.0000001), 1));
            }

            @Override
            public void onStartHover(InterfaceButton button) {
                MouseHandler.disable();
                //button.playHoverAnimation(0.092f);
                //System.out.println("I am the Senate!");
            }

            @Override
            public void onStopHover(InterfaceButton button) {
                MouseHandler.enable();
                //button.resetScale();
                //System.out.println("General Kenobi");
            }

            @Override
            public void whileHovering(InterfaceButton button) {
                //System.out.println("A suprise but I welcome one");
            }
        };

        AbstractButton shootingButton = new AbstractButton(loader, "textures/shootButton", new Vector2f(0.6f,-0.7f), new Vector2f(0.1f, 0.15f)) {
            @Override
            public void onClick(InterfaceButton button) {
                if (currentShotInProcess)
                    return;
                currentShotInProcess = true;
                //Setting the velocity of the ball
                double velocity = powerSlider.getValue() * course.maxVelocity;

                System.out.println("velocity as double: " + velocity);

                //-90 to fix it being rotated compared to the left side instead of ahead
                double angle = (camera.getYaw()-90) * Math.PI / 180.0; //Angle in radians

                //Make velocity vector by splitting the velocity into its x- and y-components
                //TODO
                //System.out.println("first: " + Math.cos(angle) + " second: " + Math.sin(angle) + " third: " + velocity);
                //Set direction
                Vector2d shot = new Vector2d(Math.cos(angle),Math.sin(angle));
                //Set velocity
                shot = shot.multiply(velocity);
                inputFlag = true;
                shotInput = shot;

                //takeShot(shot);
                //course.ball.setVelocity();

                System.out.println("current shot input: " + shotInput);
            }

            @Override
            public void onStartHover(InterfaceButton button) {
                MouseHandler.disable();
                button.playHoverAnimation(0.05f);
            }

            @Override
            public void onStopHover(InterfaceButton button) {
                MouseHandler.enable();
                button.resetScale();
            }

            @Override
            public void whileHovering(InterfaceButton button) {

            }
        };

        shootGroup.addElement(powerSlider);
        shootGroup.addElement(shootingButton);
        GUIgroups.add(shootGroup);

        //GUIs.add(powerText);
    }

    @Override
    //Update screen
    public void requestGraphicsUpdate() {
        //Handle mouse events
        MouseHandler.handleMouseEvents();
        camera.move(terrain);

        if(editMode && objectType!=-1){
            //Update mousePicker
            mousePicker.update();
            terrainPoint = mousePicker.getCurrentTerrainPoint();

            //Handle mouse click (prevents holding the button)
            int newLeftMouseButtonState = glfwGetMouseButton(DisplayManager.getWindow(), GLFW_MOUSE_BUTTON_LEFT);
            if (newLeftMouseButtonState == GLFW_RELEASE && oldLeftMouseButtonState == GLFW_PRESS) {
                deleteEditMode = false;
                handleEditClickAction();
            }
            oldLeftMouseButtonState = newLeftMouseButtonState;

            int newRightMouseButtonState = glfwGetMouseButton(DisplayManager.getWindow(), GLFW_MOUSE_BUTTON_RIGHT);
            if (newRightMouseButtonState == GLFW_RELEASE && oldRightMouseButtonState == GLFW_PRESS) {
                deleteEditMode = true;
                handleEditClickAction();
            }
            oldRightMouseButtonState = newRightMouseButtonState;

            //Handle mouse drags
            if (newLeftMouseButtonState == GLFW_PRESS || newRightMouseButtonState == GLFW_PRESS ) {
                //Update mode
                if(newLeftMouseButtonState==GLFW_PRESS) deleteEditMode = false;
                if(newRightMouseButtonState==GLFW_PRESS) deleteEditMode = true;

                handleEditDragAction();
            }
        }

        //Update mousePicker
        mousePicker.update();
        Vector3f terrainPoint = mousePicker.getCurrentTerrainPoint();

        //Render water part 1
        GL11.glEnable(GL30.GL_CLIP_DISTANCE0);

        //water reflection
        waterFrameBuffers.bindReflectionFrameBuffer();
        float distance = 2*(camera.getPosition().y - mainWaterTile.getHeight());
        camera.setPreventTerrainClipping(false);
        camera.getPosition().y -= distance;
        camera.invertPitch();

        masterRenderer.renderScene(entities, terrain, light, camera, new Vector4f(0, 1, 0, -mainWaterTile.getHeight()+0.2f));

        camera.getPosition().y += distance;
        camera.invertPitch();
        camera.setPreventTerrainClipping(true);
        waterFrameBuffers.unbindCurrentFrameBuffer();

        //water refraction
        waterFrameBuffers.bindRefractionFrameBuffer();
        masterRenderer.renderScene(entities, terrain, light, camera, new Vector4f(0, -1, 0, mainWaterTile.getHeight()+0.2f));
        waterFrameBuffers.unbindCurrentFrameBuffer();

        //Render 3D elements
        masterRenderer.renderScene(entities, terrain, light, camera, new Vector4f(0, 0, 0, 0));

        //Render water part 2
        waterRenderer.render(waters, camera, light);

        //Render 2D elements
        //Render groups
        for(UIGroup group : GUIgroups){
            List<UIElement> groupElements = group.getElements();
            guiRenderer.render(groupElements);
            for(UIElement element : groupElements){
                element.update();
            }
        }

        //Render lonely elements :(
        guiRenderer.render(GUIelements);
        for(UIElement element : GUIelements){
            element.update();
        }

        //Render text
        TextMaster.render();

        DisplayManager.updateDisplay();
        DisplayManager.swapBuffers();
    }

    public void cleanUp() {
        waterFrameBuffers.cleanUp();
        waterShader.cleanUp();
        TextMaster.cleanUp();
        guiRenderer.cleanUp();
        masterRenderer.cleanUp();
        loader.cleanUp();
    }

    @Override 
    protected boolean collectShotData() {
        //System.out.println("enter your shot data(the velocity vector):");
        //Scanner shotScanner = new Scanner(System.in);
        //String[] arguments = shotScanner.nextLine().split(" ");
        //System.out.println("your shot is read");

        shootGroup.show();
        currentShotInProcess = false;
        inputFlag = false;

        while (!inputFlag) {
            requestGraphicsUpdate();
            try {
                Thread.sleep(10);
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("current shot input: " + shotInput);

        shootGroup.hide();
        return true;
        /*if (arguments.length == 1 && arguments[0].equals("stop")) {
            System.out.println("stop condition is recognized");
            shootGroup.show();
            return false;
        }
        if (arguments.length == 2 && StringUtils.isNumeric(arguments[0]) && StringUtils.isNumeric(arguments[1])) {
            shotInput = new Vector2d(arguments[0], arguments[1]);
            System.out.println("input is recognized as valid");
            return true;
        }
        System.out.println("invalid shot input, try again");
        */

        //return collectShotData();
    }

    public void playGame(boolean fileShotsFlag, String shotsFileName) {
        System.out.println(fileShotsFlag);

        setUpModels();
        resetPositions();
        addAxes();
        addTerrain();
        initLight();
        addWater();
        initRenders();
        initCamera();
        initControls();
        
        setInteractiveMod(!fileShotsFlag);
        
        //only call setupEditMode if edit mode should be available
        //obj.setupEditMode();
        if (!fileShotsFlag)
            addUI();
        requestGraphicsUpdate();

        try {
            game();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }

        cleanUp();
    }

    public static void main(String[] args) {
        MainGame obj = new MainGame("./res/courses/course0.txt", 2, 1e-1, 1e-2);    
        obj.playGame(true, "./res/shots/shots.txt");
    }

    private void handleEditClickAction(){
        if(terrainPoint!=null){
            if(objectType == 1){
                if(!deleteEditMode){
                    //Place mode
                    //terrainPoint is the point on the terrain that the user clicked on
                    ModelData treeModelData = OBJFileLoader.loadOBJ("tree");
                    RawModel treeModel = loader.loadToVAO(treeModelData.getVertices(), treeModelData.getTextureCoords(), treeModelData.getNormals(), treeModelData.getIndices());
                    TexturedModel texturedTree = new TexturedModel(treeModel, new ModelTexture(loader.loadTexture("models/TreeTexture")));
                    Tree treeToAdd = new Tree(texturedTree, new Vector3f(terrainPoint), 0, 0, 0, 1);
                    trees.add(treeToAdd);
                    entities.add(treeToAdd);
                } else if(deleteEditMode){
                    //Remove trees within remove distance
                    System.out.println("BEFORE" + trees.size());
                    for(int i=0; i<trees.size(); i++){
                        Entity currentTree = trees.get(i);

                        if(currentTree.getPosition().distance(terrainPoint)<REMOVE_DISTANCE){
                            trees.remove(currentTree);
                            entities.remove(currentTree);
                        }
                    }
                    System.out.println("AFTER" + trees.size());
                }
            } else if(objectType == 2){
                if(!deleteEditMode){
                    //Add sand
                    terrain.setTerrainTypeWithinRadius(terrainPoint.x, terrainPoint.y, terrainPoint.z, 1, EDIT_SAND_DISTANCE);
                    terrain.updateTerrain(loader);

                } else if(deleteEditMode){
                    //Remove sand
                    terrain.setTerrainTypeWithinRadius(terrainPoint.x, terrainPoint.y, terrainPoint.z, 0, EDIT_SAND_DISTANCE);
                }
            } else if(objectType == 66){
                //DEBUG MODE IS ON (order 66)
                System.out.println(terrain.getTerrainTypeAtTerrainPoint(terrainPoint.x, terrainPoint.z));
            }
        }
    }

    private void handleEditDragAction() {
        if (terrainPoint != null) {
            if(objectType == 2){
                //Sand
                if(!deleteEditMode){
                    //Add sand
                    terrain.setTerrainTypeWithinRadius(terrainPoint.x, terrainPoint.y, terrainPoint.z, 1, EDIT_SAND_DISTANCE);
                    terrain.updateTerrain(loader);
                } else if(deleteEditMode){
                    //Remove sand
                    terrain.setTerrainTypeWithinRadius(terrainPoint.x, terrainPoint.y, terrainPoint.z, 0, EDIT_SAND_DISTANCE);
                    terrain.updateTerrain(loader);
                }
            }
        }
    }

    @Override
    //TODO fix
    public void showWinText(){
        FontType font = new FontType(loader.loadTexture("/font/tahoma"), new File("res/font/tahoma.fnt"));
        GUIText winText = new GUIText("You won!", 1, font, new Vector2f(0, 0), 1f, true);
    }
}
