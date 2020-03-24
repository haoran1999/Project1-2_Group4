package com.mygdx.game.LevelEditor;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.model.data.ModelData;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.TextureProvider;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.Vector2d;

import java.util.ArrayList;
import java.util.Random;

/*
This will create a level editor in which the user can create a map and save it. It should work as a stand-alone application.
 */
enum EditState {
    Tree,
    Sand,
    Flag,
    Start,
    none
}

public class LevelEditor extends ApplicationAdapter implements InputProcessor, ApplicationListener {
    //Shared 2D stuff
    SpriteBatch batch2D;

    //Used to set what will be rendered
    EditState selectedObjectType = EditState.none;

    //NON-2D-UI
    //size in meter
    final float terrainStepSize = 1;
    final int terrainWidth = 20;
    final int terrainLength = 15;

    //Position attribute - (x, y, z)
    final int POSITION_COMPONENTS = 3;
    //Color attribute - (r, g, b, a), but using Packed
    final int COLOR_COMPONENTS = 1;
    //Total number of components for all attributes
    final int NUM_COMPONENTS = POSITION_COMPONENTS + COLOR_COMPONENTS;
    //The "size" (total number of floats) for a single triangle
//	final int PRIMITIVE_SIZE = 3 * NUM_COMPONENTS;
    //The maximum number of triangles our mesh will hold
    //Size of the terrain / stepSize = the amount of squares, *2=the amount of triangles
    final int MAX_TRIS = (int)((terrainLength * terrainWidth)/terrainStepSize)*2;
    //The maximum number of vertices our mesh will hold
    final int MAX_VERTS = MAX_TRIS * 3;
    //The array which holds all the vertices
    float[] terrainVertices = new float[MAX_VERTS * NUM_COMPONENTS];
    int terrainVertexIndex = 0;

    PerspectiveCamera camera;
    Viewport viewport;
    Environment environment;
    ModelBatch modelBatch;
    ModelBuilder modelBuilder;
    G3dModelLoader modelLoader = new G3dModelLoader(new JsonReader());

    Model ballModel;
    ModelInstance ballInstance;

    Model goalModel;
    ModelInstance goalInstance;

    boolean treesEnabled;
    Model treeModel;
    ArrayList<ModelInstance> treeInstances = new ArrayList<ModelInstance>();

    Mesh ground;
    ShaderProgram groundShader;

    CameraInputController cameraInputController;

    //TODO TMP
    ShapeRenderer shapes;

    @Override
    public void create() {
        //TODO TMP
        shapes = new ShapeRenderer();

//		//Use 1080p
//		Gdx.graphics.setWindowedMode(1920, 1080);

        //Create camera
        camera = new PerspectiveCamera(75, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        //Set initial position and orientation
        camera.position.set(10f, 10f, 10f);
        camera.lookAt(0f, 0f, 0f);

        //Clipping distances
        camera.near = 0.1f;
        camera.far = 300f;

        //Needed for window resizing
        viewport = new FitViewport(800, 480, camera);

        //Model setup
        modelBatch = new ModelBatch();
        modelBuilder = new ModelBuilder();

        //NOTE: when updating the 3D model, export it as fbx, than convert it to g3dj .\fbx-conv-win32 -f -o G3DJ NAME.fbx, than set opacity to 1 for all the materials
        renderBall(0, getTerrainHeight(0, 0), 0);
        renderGoal(5, getTerrainHeight(5, 5),5);

        //Test for the trees
//		enableTrees();
//		addTree(5, getTerrainHeight(5, 5), 5);
//		addTree(15, getTerrainHeight(15, 15), 15);
//		removeTreeWithinRadius(5, getTerrainHeight(5, 5), 5, 1);

        //Set ground shader and mesh
        groundShader = new ShaderProgram(Gdx.files.internal("shader/vertexshader.glsl").readString(), Gdx.files.internal("shader/fragmentshader.glsl").readString());

        ground = new Mesh(true, MAX_VERTS, 0,
                new VertexAttribute(VertexAttributes.Usage.Position, POSITION_COMPONENTS, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, "a_color"));

        //Set camera controller AND INPUT PROCESSOR
        cameraInputController = new CameraInputController(camera);
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(this);
        multiplexer.addProcessor(cameraInputController);
        Gdx.input.setInputProcessor(this);

        //Set lightning
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, .5f, .5f, .5f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
    }

    @Override
    public void render () {
        batch2D = new SpriteBatch();

        //Gdx.gl20.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(.1f, .1f, .1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT|GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(true);

        //Create terrain
        createTerrain(0, 0);

        //this will render the remaining triangles
        flush();

        //Update camera movement
        cameraInputController.update();
        camera.update();

        //Show ball and goal
        modelBatch.begin(camera);
        modelBatch.render(ballInstance, environment);
        modelBatch.render(goalInstance, environment);

        for(ModelInstance treeInstance : treeInstances){
            modelBatch.render(treeInstance, environment);
        }

        modelBatch.end();

        //TODO TMP
        Matrix4 inverseProjectView = camera.invProjectionView;

//        shapes.setProjectionMatrix(camera.combined);
//        shapes.begin(ShapeRenderer.ShapeType.Filled);
//        shapes.circle(tp.x, tp.y, 0.25f, 16);
//        shapes.end();
    }

    public void renderBall(double x, double y, double z){
        ModelData ballModelData = modelLoader.loadModelData(Gdx.files.internal("core/assets/golfBall.g3dj"));
        ballModel = new Model(ballModelData, new TextureProvider.FileTextureProvider());
        ballInstance = new ModelInstance(ballModel, (float) x, (float) y, (float) z);
    }

    public void renderBall(Vector2d location){
        renderBall(location.x, location.y, location.z);
    }

    public void renderGoal(double x, double y, double z){
        ModelData goalModelData = modelLoader.loadModelData(Gdx.files.internal("core/assets/flag.g3dj"));
        goalModel = new Model(goalModelData, new TextureProvider.FileTextureProvider());
        goalInstance = new ModelInstance(goalModel, (float) x, (float) y, (float) z);
    }

    public void renderGoal(Vector2d location){
        renderGoal(location.x, location.y, location.z);
    }

    public void enableTrees(){
        treesEnabled = true;
        ModelData treeModelData = modelLoader.loadModelData(Gdx.files.internal("core/assets/tree.g3dj"));
        treeModel = new Model(treeModelData, new TextureProvider.FileTextureProvider());
    }

    public void addTree(float x, float y, float z){
        ModelInstance tree = new ModelInstance(treeModel, x, y, z);
        treeInstances.add(tree);
    }

    public void removeTree(int treeIndex){
        treeInstances.remove(treeIndex);
    }

    public void removeTreeWithinRadius(float x, float y, float z, float radius){
        for(int i=0; i<treeInstances.size(); i++){
            ModelInstance cTree = treeInstances.get(i);
            Vector3 cTreeLocation = cTree.transform.getTranslation(new Vector3());

            //If the tree is within the given radius, remove it
            if(Math.abs(cTreeLocation.x-x) <= radius && Math.abs(cTreeLocation.y-y) <= radius && Math.abs(cTreeLocation.z-z) <= radius){
                treeInstances.remove(i);
            }
        }
    }

    public void removeTreeAtLocation(float x, float y, float z){
        removeTreeWithinRadius(x, y, z, 0);
    }

    void createTerrain(float xOffset, float yOffset){
        //Go over chunks of terrain and create as many chunks as needed to create the terrain
        for(int x=0; x<terrainWidth/terrainStepSize; x++){
            for(int z=0; z<terrainLength/terrainStepSize; z++){
                float xCoordinate = x*terrainStepSize+xOffset;
                float zCoordinate = z*terrainStepSize+yOffset;
                drawGroundQuad(xCoordinate, zCoordinate);
            }
        }
    }

    static float getTerrainHeight(float x, float z){
        //TODO put the actual function here
        Random random = new Random((long) (x+z));
        random.nextFloat();
        return (random.nextFloat()*3-1);
//		return (float) (.2*x+.02*z-2);
    }

    void drawGroundQuad(float x, float z) {
        //we don't want to hit any index out of bounds exception...
        //so we need to flush the batch if we can't store any more verts
        //4=amounts of indexes used per vertex; 3=amount of vertices per triangle; 2=amount of triangles
        if (terrainVertexIndex == terrainVertices.length-(4*3*2))
            flush();

        //First triangle (bottom left, bottom right, top left)
        //bottom left vertex
        terrainVertices[terrainVertexIndex++] = x;
        terrainVertices[terrainVertexIndex++] = getTerrainHeight(x, z);
        terrainVertices[terrainVertexIndex++] = z;
        if(getTerrainHeight(x, z) > 0){
            terrainVertices[terrainVertexIndex++] = Color.GREEN.toFloatBits();
        } else {
            terrainVertices[terrainVertexIndex++] = Color.BLUE.toFloatBits();
        }

        //bottom right vertex
        terrainVertices[terrainVertexIndex++] = x + terrainStepSize;
        terrainVertices[terrainVertexIndex++] = getTerrainHeight(x + terrainStepSize, z);
        terrainVertices[terrainVertexIndex++] = z;
        if(getTerrainHeight(x + terrainStepSize, z) > 0){
            terrainVertices[terrainVertexIndex++] = Color.GREEN.toFloatBits();
        } else {
            terrainVertices[terrainVertexIndex++] = Color.BLUE.toFloatBits();
        }

        //Top left vertex
        terrainVertices[terrainVertexIndex++] = x;
        terrainVertices[terrainVertexIndex++] = getTerrainHeight(x, z + terrainStepSize);
        terrainVertices[terrainVertexIndex++] = z + terrainStepSize;
        if(getTerrainHeight(x, z + terrainStepSize) > 0){
            terrainVertices[terrainVertexIndex++] = Color.GREEN.toFloatBits();
        } else {
            terrainVertices[terrainVertexIndex++] = Color.BLUE.toFloatBits();
        }

        //Second triangle (bottom right, top left, top right)
        //bottom right
        terrainVertices[terrainVertexIndex++] = x + terrainStepSize;
        terrainVertices[terrainVertexIndex++] = getTerrainHeight(x + terrainStepSize, z);
        terrainVertices[terrainVertexIndex++] = z;
        if(getTerrainHeight(x + terrainStepSize, z) > 0){
            terrainVertices[terrainVertexIndex++] = Color.GREEN.toFloatBits();
        } else {
            terrainVertices[terrainVertexIndex++] = Color.BLUE.toFloatBits();
        }

        //top left vertex
        terrainVertices[terrainVertexIndex++] = x;
        terrainVertices[terrainVertexIndex++] = getTerrainHeight(x, z + terrainStepSize);
        terrainVertices[terrainVertexIndex++] = z + terrainStepSize;
        if(getTerrainHeight(x, z + terrainStepSize) > 0){
            terrainVertices[terrainVertexIndex++] = Color.GREEN.toFloatBits();
        } else {
            terrainVertices[terrainVertexIndex++] = Color.BLUE.toFloatBits();
        }

        //top right vertex
        terrainVertices[terrainVertexIndex++] = x + terrainStepSize;
        terrainVertices[terrainVertexIndex++] = getTerrainHeight(x + terrainStepSize, z + terrainStepSize);
        terrainVertices[terrainVertexIndex++] = z + terrainStepSize;
        if(getTerrainHeight(x + terrainStepSize, z + terrainStepSize) > 0){
            terrainVertices[terrainVertexIndex++] = Color.GREEN.toFloatBits();
        } else {
            terrainVertices[terrainVertexIndex++] = Color.BLUE.toFloatBits();
        }
    }

    //Based of https://github.com/mattdesl/lwjgl-basics/wiki/LibGDX-Meshes-Lesson-1
    void flush() {
        //if we've already flushed
        if (terrainVertexIndex ==0)
            return;

        //sends our vertex data to the mesh
        ground.setVertices(terrainVertices);

        //number of vertices we need to render
        int vertexCount = (terrainVertexIndex /NUM_COMPONENTS);

        //start the shader before setting any uniforms
        groundShader.begin();
        groundShader.setUniformMatrix("u_projTrans", camera.combined);
        ground.render(groundShader, GL20.GL_TRIANGLES, 0, vertexCount);
        groundShader.end();

        //reset index to zero
        terrainVertexIndex = 0;
    }

    @Override
    public void dispose () {
        modelBatch.dispose();
        ballModel.dispose();
        goalModel.dispose();
        if(treesEnabled){
            treeModel.dispose();
        }
    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        System.out.println("TEST");

//        //TODO TMP
//        // ignore if its not left mouse button or first touch pointer
//        if (button != Input.Buttons.LEFT || pointer > 0) return false;
//
//        Vector3 clickVector = new Vector3();
//        clickVector.x = (2.0f*((float)(screenX-0)/(Gdx.graphics.getWidth())))-1.0f;
//        clickVector.y = 1.0f-(2.0f*((float)(screenY-0)/(Gdx.graphics.getHeight())));
//        clickVector.z = (float) (2.0 * 1 - 1.0);
//
//        Matrix4 inverseProjectionMatrix = camera.invProjectionView;
//
//        float[] multiplicationArray = new float[]{clickVector.x, clickVector.y, clickVector.z, 1};
//
//        Matrix4.mul(multiplicationArray, inverseProjectionMatrix.val);
//
//        Vector3 result = new Vector3();
//        result.x = multiplicationArray[0];
//        result.y = multiplicationArray[1];
//        result.z = multiplicationArray[2];
//        float w = multiplicationArray[3];
//        w = (float) (1.0 / w);
//
//        result.x *= w;
//        result.y *= w;
//        result.z *= w;
//
//        System.out.println("ResultX = " + result.x + " ResultY = " + result.y + " ResultZ = " + result.z);
//
////        camera.unproject(tp.set(screenX, screenY, 0));
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }

	public void resize(int width, int height) {
		viewport.update(width, height);
		camera.update();
	}

    public void hide() {

    }

}