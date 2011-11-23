package de.jreality.plugin.basic;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.jreality.io.JrScene;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.Rn;
import de.jreality.scene.Appearance;
import de.jreality.scene.Camera;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphPath;
import de.jreality.scene.Transformation;
import de.jreality.shader.ShaderUtility;
import de.jtem.beans.ChangeEventMulticaster;
import de.jtem.jrworkspace.plugin.Plugin;
import de.jtem.jrworkspace.plugin.PluginInfo;

public class Scene extends Plugin {
	
	private ChangeListener changeListener;
	
	static JrScene defaultScene() {
		//sceneRoot of the JrScene
		SceneGraphComponent sceneRoot = new SceneGraphComponent("root");
		sceneRoot.setVisible(true);
		Appearance app = new Appearance("root appearance");
		ShaderUtility.createRootAppearance(app);
		sceneRoot.setAppearance(app);
		
		// backdrop
		SceneGraphComponent backdrop = new SceneGraphComponent("backdrop");
		sceneRoot.addChild(backdrop);

		//content
		SceneGraphComponent content = new SceneGraphComponent("content");
		content.setTransformation(new Transformation("content trafo"));
		sceneRoot.addChild(content);
		
		double[] trafoMatrix;
		Transformation trafo;
		
		//avatar
		SceneGraphComponent avatar = new SceneGraphComponent("avatar");
		avatar.setVisible(true);
		trafoMatrix = Rn.identityMatrix(4);
		//trafoMatrix[11] = 16;
		trafo = new Transformation("avatar trafo", trafoMatrix);
		avatar.setTransformation(trafo);
		sceneRoot.addChild(avatar);
		
		//camera
		SceneGraphComponent cameraNode = new SceneGraphComponent("cameraNode");
		cameraNode.setVisible(true);
		trafoMatrix = Rn.identityMatrix(4);
		trafo = new Transformation("camera trafo", trafoMatrix);
		cameraNode.setTransformation(trafo);
		Camera camera = new Camera("camera"); 
		camera.setFar(100.0);
		camera.setFieldOfView(30.0);
		camera.setFocus(3.0);
		camera.setNear(0.1);
		camera.setOnAxis(true);
		camera.setStereo(false);
		cameraNode.setCamera(camera);
		
		avatar.addChild(cameraNode);

		//create JrScene
		JrScene defaultScene = new JrScene(sceneRoot);

		//cameraPath
		SceneGraphPath cameraPath = new SceneGraphPath();
		cameraPath.push(sceneRoot);
		cameraPath.push(avatar);
		cameraPath.push(cameraNode);
		cameraPath.push(camera);
		defaultScene.addPath("cameraPath", cameraPath);
		defaultScene.addPath("microphonePath", cameraPath.popNew());
		
		//avatarPath
		SceneGraphPath avatarPath = new SceneGraphPath();
		avatarPath.push(sceneRoot);
		avatarPath.push(avatar);
		defaultScene.addPath("avatarPath", avatarPath);
		
		MatrixBuilder.euclidean().translate(0,0,16).assignTo(avatar);
		
		//emptyPickPath/content
		SceneGraphPath emptyPickPath = new SceneGraphPath();
		emptyPickPath.push(sceneRoot);
		emptyPickPath.push(content);
		defaultScene.addPath("emptyPickPath", emptyPickPath);
		defaultScene.addPath("contentPath", emptyPickPath);
		
		//backdrop
		SceneGraphPath backdropPath = new SceneGraphPath();
		backdropPath.push(sceneRoot);
		backdropPath.push(backdrop);
		defaultScene.addPath("backdropPath", backdropPath);

		return defaultScene;
	}

	JrScene theScene;
	
	
	public Scene() {
		this(null);
	}
	
	public Scene(JrScene jrscene) {
		if (jrscene != null) theScene = jrscene;
		else theScene = defaultScene();
	}
		
	@Override
	public PluginInfo getPluginInfo() {
		PluginInfo info = new PluginInfo("Scene", "jReality Group");
		info.isDynamic = false;
		return info;
	}

	public SceneGraphPath getCameraPath() {
		return theScene.getPath("cameraPath");
	}
	
	/*
	 * Returns the avatarPath of the ToolSystem. The result is guaranteed to be
	 * non-null and to have the current scene root as its first element.
	 * 
	 * @return The avatarPath for the tool system
	 */
	public SceneGraphPath getAvatarPath() {
		return theScene.getPath("avatarPath");
	}
	
	public SceneGraphPath getEmptyPickPath() {
		return theScene.getPath("emptyPickPath");
	}
	
	public SceneGraphPath getContentPath() {
		return theScene.getPath("contentPath");
	}

	public SceneGraphPath getBackdropPath() {
		return theScene.getPath("backdropPath");
	}
	
	
	public SceneGraphPath getMicrophonePath() {
		return theScene.getPath("microphonePath");
	}

	public void setCameraPath(SceneGraphPath path) {
		theScene.addPath("cameraPath", path);
		fireStateChanged();
	}
	
	public void setBackdropPath(SceneGraphPath path) {
		theScene.addPath("backdropPath", path);
		fireStateChanged();
	}
	
	public void setAvatarPath(SceneGraphPath path) {
		theScene.addPath("avatarPath", path);
		fireStateChanged();
	}
	
	public void setEmptyPickPath(SceneGraphPath path) {
		theScene.addPath("emptyPickPath", path);
		fireStateChanged();
	}
	
	public void setMicrophonePath(SceneGraphPath path) {
		theScene.addPath("microphonePath", path);
		fireStateChanged();
	}
	
	private void fireStateChanged() {
		if (changeListener != null) {
			changeListener.stateChanged(new ChangeEvent(this));
		}
	}
	
	public void addChangeListener(ChangeListener l) {
		changeListener = ChangeEventMulticaster.add(changeListener, l);
	}
	
	public void removeChangeListener(ChangeListener listener) {
		changeListener=ChangeEventMulticaster.remove(changeListener, listener);
	}

	public SceneGraphComponent getSceneRoot() {
		return theScene.getSceneRoot();
	}

	public Appearance getRootAppearance() {
		SceneGraphComponent root = getSceneRoot();
		if (root.getAppearance() == null) root.setAppearance(new Appearance("root app"));
		return getSceneRoot().getAppearance();
	}
	
	public Appearance getContentAppearance() {
		SceneGraphComponent content = getContentComponent();
		if (content.getAppearance() == null) content.setAppearance(new Appearance("content app"));
		return content.getAppearance();
	}

	public SceneGraphComponent getAvatarComponent() {
		return getLastComponent(getAvatarPath());
	}

	public SceneGraphComponent getCameraComponent() {
		return getLastComponent(getCameraPath());
	}

	public SceneGraphComponent getContentComponent() {
		return getLastComponent(getContentPath());
	}

	public SceneGraphComponent getEmptyPickComponent() {
		return getLastComponent(getEmptyPickPath());
	}

	public SceneGraphComponent getBackdropComponent() {
		return getLastComponent(getBackdropPath());
	}

	private SceneGraphComponent getLastComponent(SceneGraphPath path) {
		return path == null ? null : path.getLastComponent();
	}
		
}
