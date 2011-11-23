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

package de.jreality.ui.viewerapp.actions.file;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.beans.Expression;
import java.beans.Statement;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import de.jreality.scene.Viewer;
import de.jreality.ui.viewerapp.FileFilter;
import de.jreality.ui.viewerapp.FileLoaderDialog;
import de.jreality.ui.viewerapp.ViewerSwitch;
import de.jreality.ui.viewerapp.actions.AbstractJrAction;
import de.jtem.beans.DimensionPanel;


/**
 * Exports the scene displayed in a viewer as an image.
 * 
 * @author pinkall
 */
@SuppressWarnings("serial")
public class ExportImage extends AbstractJrAction {

	private ViewerSwitch viewer;
	private DimensionPanel dimPanel;
	private JComponent options;
	private int antialiasing;
	private boolean saveAlpha = true;
	private JCheckBox checkbox;

	
	public ExportImage(String name, ViewerSwitch viewer, Component parentComp) {
		super(name, parentComp);

		if (viewer == null) 
			throw new IllegalArgumentException("Viewer is null!");
		this.viewer = viewer;

		setShortDescription("Export image file");
		setShortCut(KeyEvent.VK_I, 0, true);
	}


	@Override
	public void actionPerformed(ActionEvent e) {

		if (options == null) options = createAccessory();

		// Hack
		Viewer realViewer = viewer.getCurrentViewer();
//		de.jreality.jogl.Viewer joglViewer = (de.jreality.jogl.Viewer) realViewer;
		Dimension d = realViewer.getViewingComponentSize();
		dimPanel.setDimension(d);

		File file = FileLoaderDialog.selectTargetFile(parentComp, options, false, FileFilter.createImageWriterFilters());
		Dimension dim = dimPanel.getDimension();
//		Dimension dim = DimensionDialog.selectDimension(d,frame);
		if (file == null || dim == null) return;

		if (FileFilter.getFileExtension(file) == null) {  //no extension specified
			System.err.println("Please specify a valid file extension.\n" +
			"Export aborted.");
			return;
		}
		
		//render offscreen
		BufferedImage scaledImg = null;
		try {
			Expression expr = new Expression(realViewer, "renderOffscreen", new Object[]{antialiasing*dim.width, antialiasing*dim.height});
//			expr.execute();
			scaledImg = (BufferedImage) expr.getValue();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		int type = saveAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
		BufferedImage img = new BufferedImage(dim.width, dim.height, type);
		Graphics2D g = (Graphics2D) img.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        g.setColor(new Color(255,255,255,0));
        g.fillRect(0,0,dim.width,dim.height);
        g.drawImage(
				scaledImg.getScaledInstance(
						dim.width,
						dim.height,
						BufferedImage.SCALE_SMOOTH
				),
				0,
				0,
				null
		);
		if (saveAlpha) img.coerceData(true);

		//JOGLRenderer.writeBufferedImage(file,img2); :
		try {
			new Statement(Class.forName("de.jreality.util.ImageUtility"), "writeBufferedImage", new Object[]{file, img}).execute();
			System.out.println("Wrote file "+file.getPath());
		} catch (Exception ex) {
			// and now?
			throw new RuntimeException("writing image failed", ex);
		}
	}


	@Override
	public boolean isEnabled() {
		Class<? extends Viewer> viewerType = viewer.getCurrentViewer().getClass();
		try {
			viewerType.getMethod("renderOffscreen", new Class[]{Integer.TYPE, Integer.TYPE});
			return true;
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
//			System.err.println("used viewer doesn't have method renderOffscreen(int w, int h) - " +
//					"this just disables action \"Export Image\"\n");
			//e.printStackTrace();
		}
		return false;
	}

	
	private JComponent createAccessory() {
		
		if (dimPanel == null) {
			dimPanel = new DimensionPanel();
			dimPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Dimension"));
			dimPanel.setToolTipText("Set image dimensions");
		}
		
		Box accessory = Box.createVerticalBox();
		accessory.add(dimPanel);
		accessory.add(Box.createVerticalGlue());
		
		JPanel p = new JPanel();
		ButtonGroup bg = new ButtonGroup();
		JRadioButton button = new JRadioButton(new AbstractAction("none") {
			public void actionPerformed(ActionEvent e) {
				antialiasing = 1;
			}
		});
		bg.add(button);
		p.add(button);
		button = new JRadioButton(new AbstractAction("2x2") {
			public void actionPerformed(ActionEvent e) {
				antialiasing = 2;
			}
		});
		bg.add(button);
		p.add(button);
		button = new JRadioButton(new AbstractAction("4x4") {
			public void actionPerformed(ActionEvent e) {
				antialiasing = 4;
			}
		});
		button.setSelected(true);
		antialiasing = 4;
		bg.add(button);
		p.add(button);
		
		p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Antialiasing factor"));
		p.setToolTipText("<html><body>Choose the factor of dimension scaling<br>" +
				"for \"antialiased\" offscreen rendering</body></html>");
		accessory.add(p);
		checkbox = new JCheckBox(new AbstractAction("save alpha") {
			public void actionPerformed(ActionEvent e) {
				saveAlpha = checkbox.isSelected();
			}
		});
		checkbox.setSelected(saveAlpha);
		Box hbox = Box.createHorizontalBox();
		hbox.add(Box.createHorizontalGlue());
		hbox.add(checkbox);
		hbox.add(Box.createHorizontalGlue());
		accessory.add(hbox);
		
		return accessory;
	}
}