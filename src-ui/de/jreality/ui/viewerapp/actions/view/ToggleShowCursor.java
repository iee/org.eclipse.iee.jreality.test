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


package de.jreality.ui.viewerapp.actions.view;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.KeyStroke;

import de.jreality.scene.Viewer;
import de.jreality.ui.viewerapp.actions.AbstractJrAction;
import de.jreality.util.GuiUtility;


/**
 * Toggles full screen of the ViewerApp's viewer component (where the scene is displayed).<br>
 * There is only one instance of this action.
 * 
 * @author msommer
 */
public class ToggleShowCursor extends AbstractJrAction {

  private Viewer viewer;
  
  public ToggleShowCursor(String name, Viewer viewer) {
    super(name);
    this.viewer = viewer;
    setShortDescription("Toggle show cursor in viewing component");
    setAcceleratorKey(KeyStroke.getKeyStroke(KeyEvent.VK_F9, InputEvent.SHIFT_MASK));
  }

  
  @Override
  public void actionPerformed(ActionEvent e) {
	  if (viewer.hasViewingComponent() && viewer.getViewingComponent() instanceof Component) {
		Component viewingCmp = (Component) viewer.getViewingComponent();
		boolean hide = viewingCmp.getCursor().getType() == Cursor.DEFAULT_CURSOR;
		if (hide) GuiUtility.hideCursor(viewingCmp);
		else GuiUtility.showCursor(viewingCmp);
	}
 }
  
}