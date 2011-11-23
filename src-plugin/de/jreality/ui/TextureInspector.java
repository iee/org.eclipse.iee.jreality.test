package de.jreality.ui;

import static de.jreality.scene.Appearance.INHERITED;
import static de.jreality.shader.CommonAttributes.POLYGON_SHADER;
import static de.jreality.shader.CommonAttributes.TEXTURE_2D;
import static java.awt.FlowLayout.LEFT;
import static javax.swing.JFileChooser.FILES_ONLY;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileSystemView;

import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.plugin.icon.ImageHook;
import de.jreality.scene.Appearance;
import de.jreality.shader.CommonAttributes;
import de.jreality.shader.ImageData;
import de.jreality.shader.Texture2D;
import de.jreality.shader.TextureUtility;
import de.jreality.ui.viewerapp.FileFilter;
import de.jreality.util.Input;
import de.jreality.util.Secure;
import de.jreality.util.SystemProperties;
import de.jtem.jrworkspace.plugin.sidecontainer.widget.ShrinkPanel;

@SuppressWarnings("serial")
public class TextureInspector extends JPanel implements ChangeListener {
	
	private static final double 
		DEFAULT_TEXTURE_SCALE = 20;
	private static final String 
		DEFAULT_TEXTURE = "none";
	
	// maximal value of texture scale
	private double 
		maximalTextureScale = 400.0;
	// ratio of maximal value and minimal value of texture scale
	private double 
		logarithmicRange = 400.0,
		textureRatio = 1.0;
	
	private JButton 
		textureLoadButton = new JButton("Load...", ImageHook.getIcon("folder.png")),
		removeButton = new JButton(ImageHook.getIcon("remove.png"));
	private ButtonGroup 
		textureGroup = new ButtonGroup();
	private JSliderVR
		rotateSlider = new JSliderVR(SwingConstants.HORIZONTAL, -180, 180, 0),
		translateUSlider = new JSliderVR(SwingConstants.HORIZONTAL, 0, 100, 0),
		translateVSlider = new JSliderVR(SwingConstants.HORIZONTAL, 0, 100, 0),
		scaleUSlider = new JSliderVR(SwingConstants.HORIZONTAL, 0, 100, 0),
		scaleVSlider = new JSliderVR(SwingConstants.HORIZONTAL, 0, 100, 0);
	private SpinnerNumberModel
		rotateModel = new SpinnerNumberModel(0.0, -180, 180, 0.1),
		translateUModel = new SpinnerNumberModel(0.0, 0, 1000.0, 0.001),
		translateVModel = new SpinnerNumberModel(0.0, 0, 1000.0, 0.001),
		scaleUModel = new SpinnerNumberModel(1.0, 0, 1000.0, 0.001),
		scaleVModel = new SpinnerNumberModel(1.0, 0, 1000.0, 0.001);
	private JSpinner
		rotateSpinner = new JSpinner(rotateModel),
		translateUSpinner = new JSpinner(translateUModel),
		translateVSpinner = new JSpinner(translateVModel),
		scaleUSpinner = new JSpinner(scaleUModel),
		scaleVSpinner = new JSpinner(scaleVModel);
	private JToggleButton
		scaleLockToggle = new JToggleButton(ImageHook.getIcon("closedChain.gif"));
	private Map<String, String> 
		textureNameToTexture = new HashMap<String, String>();
	private Map<String, AbstractButton> 
		textureNameToButton = new HashMap<String, AbstractButton>();
	private JFileChooser 
		fileChooser = null;

	private Texture2D 
		tex = null;
	private Appearance 
		appearance = null;
	
	private JPanel 
		texPanel = new JPanel();
	private JScrollPane 
		texScroller = new JScrollPane(texPanel);
	private ActionListener 
		texturesListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			updateTexture();
		}
	};

	private int 
		texButtonSize = 60;
	
	private GridBagConstraints 
		c = new GridBagConstraints();

	
	public TextureInspector(int btnSize) {
		this();
		this.texButtonSize = btnSize;
	}
	
	public TextureInspector() {
		setLayout(new ShrinkPanel.MinSizeGridBagLayout());
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(2, 2, 2, 2);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;
		
		texPanel.setLayout(new FlowLayout(LEFT));
		texPanel.setPreferredSize(new Dimension(1, 300)); 
		texScroller.setMinimumSize(new Dimension(100, 150));
		texScroller.setViewportBorder(null);
		c.weighty = 1.0;
		c.weightx = 1.0;
		add(texScroller, c);

		// load button
		textureLoadButton.setToolTipText("Add a new texture");
		textureLoadButton.setMargin(new Insets(0,5,0,5));
		textureLoadButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				loadTexture();
			}
		});
		c.weighty = 0.0;
		c.weightx = 1.0;
		c.gridwidth = GridBagConstraints.RELATIVE;
		add(textureLoadButton, c);
		c.weightx = 0.0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		removeButton.setToolTipText("Remove the current texture");
		removeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeTexture();
			}
		});
		add(removeButton, c);
		
		// scale slider
		scaleUSlider.addChangeListener(this);
		scaleUSpinner.addChangeListener(this);
		scaleVSlider.addChangeListener(this);
		scaleVSpinner.addChangeListener(this);
		translateUSlider.addChangeListener(this);
		translateUSpinner.addChangeListener(this);
		translateVSlider.addChangeListener(this);
		translateVSpinner.addChangeListener(this);
		rotateSlider.addChangeListener(this);
		rotateSpinner.addChangeListener(this);
		
		JPanel scalePanel = new JPanel();
		scalePanel.setBorder(BorderFactory.createTitledBorder("Scale"));
		scalePanel.setLayout(new GridBagLayout());
		c.gridheight = 1;
		c.gridwidth = 1;
		c.weightx = 0.0;
		scalePanel.add(new JLabel("U"), c);
		c.gridwidth = 1;
		c.weightx = 1.0;
		scalePanel.add(scaleUSlider, c);
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.weightx = 0.0;
		scalePanel.add(scaleUSpinner, c);
		c.weightx = 0.0;
		c.gridheight = 2;
		c.gridwidth = GridBagConstraints.REMAINDER;
		scaleLockToggle.setSelectedIcon(ImageHook.getIcon("openChain.gif"));
		scaleLockToggle.setPreferredSize(new Dimension(10, 10));
		scalePanel.add(scaleLockToggle, c);
		c.gridx = 0;
		c.gridy = 1;
		c.gridheight = 1;
		c.gridwidth = 1;
		c.weightx = 0.0;
		scalePanel.add(new JLabel("V"), c);
		c.gridx = 1;
		c.gridwidth = 1;
		c.weightx = 1.0;
		scalePanel.add(scaleVSlider, c);
		c.gridx = 2;
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.weightx = 0.0;
		scalePanel.add(scaleVSpinner, c);
		
		
		c.gridx = GridBagConstraints.RELATIVE;
		c.gridy = GridBagConstraints.RELATIVE;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;
		c.weighty = 0.0;
		add(scalePanel, c);
		
		
		JPanel translatePanel = new JPanel();
		translatePanel.setBorder(BorderFactory.createTitledBorder("Translate"));
		translatePanel.setLayout(new GridBagLayout());
		c.gridheight = 1;
		c.gridwidth = 1;
		c.weightx = 0.0;
		translatePanel.add(new JLabel("U"), c);
		c.gridwidth = 1;
		c.weightx = 1.0;
		translatePanel.add(translateUSlider, c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 0.0;
		translatePanel.add(translateUSpinner, c);
		c.gridwidth = 1;
		c.weightx = 0.0;
		translatePanel.add(new JLabel("V"), c);
		c.gridwidth = 1;
		c.weightx = 1.0;
		translatePanel.add(translateVSlider, c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 0.0;
		translatePanel.add(translateVSpinner, c);
		
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;
		c.weighty = 0.0;
		add(translatePanel, c);
		
		JPanel rotatePanel = new JPanel();
		rotatePanel.setBorder(BorderFactory.createTitledBorder("Rotate"));
		rotatePanel.setLayout(new GridBagLayout());
		c.gridheight = 1;
		c.gridwidth = 1;
		c.weightx = 0.0;
		rotatePanel.add(new JLabel("Angle °"), c);
		c.gridwidth = 1;
		c.weightx = 1.0;
		rotatePanel.add(rotateSlider, c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 0.0;
		rotatePanel.add(rotateSpinner, c);
		
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;
		c.weighty = 0.0;
		add(rotatePanel, c);
		
		makeFileChooser();
		setTexture(DEFAULT_TEXTURE);
		setTextureUScale(DEFAULT_TEXTURE_SCALE);
	}
	
	
	/**
	 * Beware! Ugly code
	 */
	public void stateChanged(ChangeEvent e) {
		Object s = e.getSource();
		if (scaleUSlider == s) {
			double sliderVal = scaleUSlider.getValue() * 0.01;
			double d = Math.exp(Math.log(logarithmicRange) * sliderVal)/logarithmicRange * maximalTextureScale;
			scaleUSpinner.removeChangeListener(this);
			scaleUModel.setValue(d);
			scaleUSpinner.addChangeListener(this);
			if (!scaleLockToggle.isSelected()) {
				scaleVSpinner.removeChangeListener(this);
				scaleVModel.setValue(d * textureRatio);
				scaleVSpinner.addChangeListener(this);
				scaleVSlider.removeChangeListener(this);
				scaleVSlider.setValue((int)(scaleUSlider.getValue() * textureRatio));
				scaleVSlider.addChangeListener(this);
			}
		}
		if (scaleUSpinner == s) {
			double d = getTextureUScale();
			int value = (int)(Math.log(d / maximalTextureScale * logarithmicRange)/Math.log(logarithmicRange)*100);
			scaleUSlider.removeChangeListener(this);
			scaleUSlider.setValue(value);
			scaleUSlider.addChangeListener(this);
			if (!scaleLockToggle.isSelected()) {
				scaleVSpinner.removeChangeListener(this);
				scaleVModel.setValue(d * textureRatio);
				scaleVSpinner.addChangeListener(this);
				scaleVSlider.removeChangeListener(this);
				scaleVSlider.setValue((int)(value * textureRatio));
				scaleVSlider.addChangeListener(this);
			}		
		}
		if (scaleVSlider == s) {
			double sliderVal = scaleVSlider.getValue() * 0.01;
			double d = Math.exp(Math.log(logarithmicRange) * sliderVal)/logarithmicRange * maximalTextureScale;
			scaleVSpinner.removeChangeListener(this);
			scaleVModel.setValue(d);
			scaleVSpinner.addChangeListener(this);
			if (!scaleLockToggle.isSelected()) {
				scaleUSpinner.removeChangeListener(this);
				scaleUModel.setValue(d / textureRatio);
				scaleUSpinner.addChangeListener(this);
				scaleUSlider.removeChangeListener(this);
				scaleUSlider.setValue((int)(scaleVSlider.getValue() / textureRatio));
				scaleUSlider.addChangeListener(this);
			}
		}
		if (scaleVSpinner == s) {
			double d = getTextureVScale();
			int value = (int)(Math.log(d / maximalTextureScale * logarithmicRange)/Math.log(logarithmicRange)*100);
			scaleVSlider.removeChangeListener(this);
			scaleVSlider.setValue(value);
			scaleVSlider.addChangeListener(this);
			if (!scaleLockToggle.isSelected()) {
				scaleUSpinner.removeChangeListener(this);
				scaleUModel.setValue(d / textureRatio);
				scaleUSpinner.addChangeListener(this);
				scaleUSlider.removeChangeListener(this);
				scaleUSlider.setValue((int)(value / textureRatio));
				scaleUSlider.addChangeListener(this);
			}
		}
		if (translateUSlider == s) {
			translateUSpinner.removeChangeListener(this);
			translateUModel.setValue(translateUSlider.getValue() / 100.0);
			translateUSpinner.addChangeListener(this);
		}
		if (translateVSlider == s) {
			translateVSpinner.removeChangeListener(this);
			translateVModel.setValue(translateVSlider.getValue() / 100.0);
			translateVSpinner.addChangeListener(this);
		}
		if (translateUSpinner == s) {
			translateUSlider.removeChangeListener(this);
			translateUSlider.setValue((int)(translateUModel.getNumber().doubleValue() * 100));
			translateUSlider.addChangeListener(this);
		}
		if (translateVSpinner == s) {
			translateVSlider.removeChangeListener(this);
			translateVSlider.setValue((int)(translateVModel.getNumber().doubleValue() * 100));
			translateVSlider.addChangeListener(this);
		}
		if (rotateSlider == s) {
			rotateSlider.removeChangeListener(this);
			rotateModel.setValue(rotateSlider.getValue());
			rotateSlider.addChangeListener(this);
		}
		if (rotateSpinner == s) {
			rotateSlider.removeChangeListener(this);
			rotateSlider.setValue((int)rotateModel.getNumber().doubleValue());
			rotateSlider.addChangeListener(this);
		}
		updateTextureTransform();
	}
	
	
	public void addTexture(String name, String resource) {
		TextureJButton texButton = new TextureJButton(resource);
		texButton.setPreferredSize(new Dimension(texButtonSize, texButtonSize));
		texButton.setActionCommand(name);
		texButton.setToolTipText(resource);
		textureNameToButton.put(name, texButton);
		texButton.addActionListener(texturesListener);
		textureGroup.add(texButton);
		texPanel.add(texButton);
		doLayout();
		updateUI();
	}
	
	
	public String setTextures(Map<String, String> textures) {
		textureNameToTexture = textures;
		List<String> keyList = new LinkedList<String>(textureNameToTexture.keySet());
		Collections.sort(keyList);
		
		for (String name : keyList) {
			String tex = textureNameToTexture.get(name);
			addTexture(name, tex);
		}
		if (keyList.size() > 0) {
			return keyList.get(0);
		} else {
			return "";
		}
	}
	
	
	public Map<String, String> getTextures() {
		return textureNameToTexture;
	}
	
	public double getTextureUScale() {
		return scaleUModel.getNumber().doubleValue();
	}
	
	public void setTextureUScale(double d) {
		scaleUModel.setValue(d);
	}
	
	public double getTextureVScale() {
		return scaleVModel.getNumber().doubleValue();
	}
	
	public void setTextureVScale(double d) {
		scaleVModel.setValue(d);
	}
	
	public double getTextureUTranslation() {
		return translateUModel.getNumber().doubleValue();
	}
	
	public void setTextureUTranslation(double d) {
		translateUModel.setValue(d);
	}
	
	public double getTextureVTranslation() {
		return translateVModel.getNumber().doubleValue();
	}
	
	public void setTextureVTranslation(double d) {
		translateVModel.setValue(d);
	}
	
	public double getTextureRotation() {
		return Math.toRadians(rotateModel.getNumber().doubleValue());
	}
	
	public void setTextureRotation(double r) {
		rotateModel.setValue(Math.toDegrees(r));
	}
	
	private void updateTextureTransform() {
		if (tex != null) {
			tex.setTextureMatrix(getTextureMatrix());
		}
	}

	public String getTexture() {
		ButtonModel bm = textureGroup.getSelection();
		if (bm != null) {
			return textureGroup.getSelection().getActionCommand();
		} else {
			return DEFAULT_TEXTURE;
		}
	}
	
	public void setTexture(String name) {
		AbstractButton bm = textureNameToButton.get(name);
		if (bm != null) {
			bm.doClick();
		}
	}
	
	public Matrix getTextureMatrix() {
		MatrixBuilder mb = MatrixBuilder.euclidean();
		mb.scale(getTextureUScale(), getTextureVScale(), 1.0);
		mb.rotate(getTextureRotation(), 0, 0, 1);
		mb.translate(getTextureUTranslation(), getTextureVTranslation(), 0);
		return mb.getMatrix();
	}
	
	
	private void updateTexture() {
		String texture = getTexture();
		if (appearance != null) {
			String texResource = textureNameToTexture.get(texture);
			if (texResource == null) {
				appearance.setAttribute(
						POLYGON_SHADER + "." + TEXTURE_2D,
						INHERITED,
						Texture2D.class
				);
				textureRatio = 1.0;
			} else {
				try {
					ImageData texData = ImageData.load(Input.getInput(texResource));
					textureRatio = texData.getWidth() / (double)texData.getHeight();
					applyTexture(texData);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				if (!scaleLockToggle.isSelected()) {
					scaleVSpinner.removeChangeListener(this);
					scaleVModel.setValue(getTextureUScale() * textureRatio);
					scaleVSpinner.addChangeListener(this);
					scaleVSlider.removeChangeListener(this);
					scaleVSlider.setValue((int)(scaleUSlider.getValue() * textureRatio));
					scaleVSlider.addChangeListener(this);
				}
				updateTextureTransform();
			}
		}
	}
	
	public void setAppearance(Appearance appearance) {
		this.appearance = appearance;
		updateTexture();
		updateTextureTransform();
	}

	private void makeFileChooser() {
		FileSystemView view = FileSystemView.getFileSystemView();
		String texDir = ".";
		String dataDir = Secure.getProperty(SystemProperties.JREALITY_DATA);
		if (dataDir!= null) texDir = dataDir;
		File defaultDir = new File(texDir);
		fileChooser = new JFileChooser(!defaultDir.exists() ? view.getHomeDirectory() : defaultDir, view);
		fileChooser.setDialogTitle("Select a Texture Image");
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.setFileSelectionMode(FILES_ONLY);
		fileChooser.setFileHidingEnabled(true);
		javax.swing.filechooser.FileFilter[] filters = FileFilter.createImageReaderFilters();
		for (javax.swing.filechooser.FileFilter filter : filters) {
			fileChooser.addChoosableFileFilter(filter);
		}
		fileChooser.setFileFilter(filters[0]);
	}
	
	private void loadTexture() {
		File file = null;
		Window w = SwingUtilities.getWindowAncestor(this);
		if (fileChooser.showOpenDialog(w) == JFileChooser.APPROVE_OPTION) {
			file = fileChooser.getSelectedFile();
		}
		if (file != null) {
			try {
				ImageData img = ImageData.load(Input.getInput(file));
				applyTexture(img);
				setTextureUScale(getTextureUScale());
				String texName = textureNameToTexture.keySet().size() + " " + file.getName();
				textureNameToTexture.put(texName, file.getAbsolutePath());
				addTexture(texName, file.getAbsolutePath());
				textureNameToButton.get(texName).setSelected(true);
				int maxScroll = texScroller.getVerticalScrollBar().getMaximum();
				texScroller.getVerticalScrollBar().setValue(maxScroll);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void removeTexture() {
		String texture = getTexture();
		String texResource = textureNameToTexture.get(texture);
		if (texResource != null) {
			textureNameToTexture.remove(texture);
			textureNameToButton.clear();
			texPanel.removeAll();
			String firstTexture = setTextures(textureNameToTexture);
			AbstractButton bm = textureNameToButton.get(firstTexture);
			bm.doClick();
			updateTexture();
		}
	}
	
	
	private void applyTexture(ImageData imageData) {
		if (imageData != null) {
			tex = TextureUtility.createTexture(
					appearance,
					CommonAttributes.POLYGON_SHADER,
					imageData
			);
		} else {
			TextureUtility.removeTexture(appearance, "polygonShader");
		}
	}
	
	public double getMaximalTextureScale() {
		return maximalTextureScale;
	}

	public void setMaximalTextureScale(double maximalTextureScale) {
		this.maximalTextureScale = maximalTextureScale;
	}

	public double getLogarithmicRange() {
		return logarithmicRange;
	}
	
	public void setLogarithmicRange(double logarithmicRange) {
		this.logarithmicRange = logarithmicRange;
	}
	
	
	@Override
	public void updateUI() {
		super.updateUI();
		if (fileChooser != null) {
			fileChooser.updateUI();
		}
	}
	
	
}
