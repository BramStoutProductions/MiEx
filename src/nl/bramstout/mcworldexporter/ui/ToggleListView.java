/*
 * BSD 3-Clause License
 * 
 * Copyright (c) 2024, Bram Stout Productions
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package nl.bramstout.mcworldexporter.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JPanel;

import nl.bramstout.mcworldexporter.ui.ToggleList.SelectionListener;

public class ToggleListView extends JPanel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private List<Entry<String, Boolean>> items;
	private int itemHeight = 20;
	private int preferredWidth = 100;
	private int dragIndexStart = -1;
	private int dragYStart = -1;
	private boolean dragEnable = false;
	private boolean didDrag = false;
	private List<Entry<String, Boolean>> itemsDragStart;
	private boolean deselectMode = false;
	private boolean selectMode = false;
	private SelectionListener listener;
	
	public ToggleListView() {
		setToolTipText("Click/Drag: Toggle select    Ctrl + Click/Drag: Select    Shift + Click/Drag: Deselect");
		
		items = new ArrayList<Entry<String, Boolean>>();
		itemsDragStart = new ArrayList<Entry<String, Boolean>>();
		
		addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
				
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if(e.getButton() == MouseEvent.BUTTON1) {
					didDrag = false;
					dragEnable = false;
					
					int itemIndex = e.getY() / itemHeight;
					
					if(itemIndex > items.size())
						return;
					
					deselectMode = e.isShiftDown();
					selectMode = e.isControlDown();
					
					dragYStart = e.getY();
					dragIndexStart = Math.min(Math.max(itemIndex, 0), items.size() - 1);
					itemsDragStart.clear();
					for(Entry<String, Boolean> _entry : items) {
						itemsDragStart.add(entry(_entry.getKey(), _entry.getValue().booleanValue()));
					}
					
					if(itemIndex < 0 || itemIndex >= items.size())
						return;
					e.consume();
					Entry<String, Boolean> item = items.get(itemIndex);
					if(deselectMode)
						item.setValue(Boolean.FALSE);
					else if(selectMode)
						item.setValue(Boolean.TRUE);
					else
						item.setValue(Boolean.valueOf(!item.getValue().booleanValue()));
					didDrag = true;
					repaint();
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				dragIndexStart = -1;
				if(e.getButton() == MouseEvent.BUTTON1 && didDrag) {
					if(listener != null)
						listener.onSelectionChanged(items);
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				
			}

			@Override
			public void mouseExited(MouseEvent e) {
				
			}
			
		});
		
		addMouseMotionListener(new MouseMotionListener() {

			@Override
			public void mouseDragged(MouseEvent e) {
				if(dragIndexStart == -1)
					return;
				
				didDrag = true;
				e.consume();
				int itemIndex = e.getY() / itemHeight;
				
				int startIndex = Math.min(itemIndex, dragIndexStart);
				int endIndex = Math.max(itemIndex, dragIndexStart);
				startIndex = Math.min(Math.max(startIndex, 0), items.size() - 1);
				endIndex = Math.min(Math.max(endIndex, 0), items.size() - 1);
				
				if(Math.abs(e.getY() - dragYStart) < (itemHeight / 3)) {
					if(dragEnable) {
						startIndex = -1;
						endIndex = -1;
					}
				}else {
					dragEnable = true;
				}
				
				for(int i = 0; i < items.size(); ++i) {
					Entry<String, Boolean> item = items.get(i);
					Entry<String, Boolean> startItem = itemsDragStart.get(i);
					if(i >= startIndex && i <= endIndex) {
						if(deselectMode)
							item.setValue(Boolean.FALSE);
						else if(selectMode)
							item.setValue(Boolean.TRUE);
						else
							item.setValue(Boolean.valueOf(!startItem.getValue().booleanValue()));
					}else {
						item.setValue(Boolean.valueOf(startItem.getValue().booleanValue()));
					}
				}
				repaint();
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				
			}
			
		});
	}
	
	public List<Entry<String, Boolean>> getItems(){
		return items;
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		
		Rectangle clipBounds = g2.getClipBounds();
		
		g2.setBackground(new Color(230, 230, 230));
		g2.clearRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);
		
		int minItemIndex = clipBounds.y / itemHeight;
		int maxItemIndex = (clipBounds.y + clipBounds.height + itemHeight - 1) / itemHeight;
		
		minItemIndex = Math.min(Math.max(minItemIndex, 0), items.size());
		maxItemIndex = Math.min(Math.max(maxItemIndex, 0), items.size());
		
		int fontHeightOffset = g2.getFontMetrics().getAscent() / 2;
		
		for(int i = minItemIndex; i < maxItemIndex; ++i) {
			Entry<String, Boolean> item = items.get(i);
			
			if(item.getValue().booleanValue()) {
				g2.setColor(new Color(128, 196, 255));
				g2.fillRect(0, i * itemHeight, getWidth(), itemHeight);
			}
			
			g2.setColor(Color.BLACK);
			g2.drawString(item.getKey(), 16, i * itemHeight + itemHeight / 2 + fontHeightOffset);
		}
		
		g2.setColor(new Color(0, 0, 0));
	}
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(preferredWidth, items.size() * itemHeight + itemHeight / 4);
	}
	
	private Entry<String, Boolean> entry(String name, boolean selected){
		return new Entry<String, Boolean>(){

			private String _name = name;
			private boolean _selected = selected;
			
			@Override
			public String getKey() {
				return _name;
			}

			@Override
			public Boolean getValue() {
				return Boolean.valueOf(_selected);
			}

			@Override
			public Boolean setValue(Boolean value) {
				_selected = value.booleanValue();
				return Boolean.valueOf(_selected);
			}
			
		};
	}
	
	public void setItems(List<String> items, boolean selected) {
		this.items.clear();
		for(String item : items) {
			this.items.add(entry(item, selected));
		}
		if(listener != null)
			listener.onSelectionChanged(this.items);
		invalidate();
		repaint();
	}
	
	public void setItemsPreserveSelection(List<String> items) {
		Map<String, Boolean> selection = new HashMap<String, Boolean>();
		for(Entry<String, Boolean> item : this.items) {
			selection.put(item.getKey(), item.getValue());
		}
		this.items.clear();
		for(String item : items) {
			this.items.add(entry(item, selection.getOrDefault(item, Boolean.FALSE).booleanValue()));
		}
		if(listener != null)
			listener.onSelectionChanged(this.items);
		invalidate();
		repaint();
	}
	
	public void deselect(String item) {
		for(Entry<String, Boolean> item2 : items) {
			if(item2.getKey().equals(item)) {
				item2.setValue(Boolean.FALSE);
			}
		}
		if(listener != null)
			listener.onSelectionChanged(items);
		repaint();
	}
	
	public void select(String item) {
		for(Entry<String, Boolean> item2 : items) {
			if(item2.getKey().equals(item)) {
				item2.setValue(Boolean.TRUE);
			}
		}
		if(listener != null)
			listener.onSelectionChanged(items);
		repaint();
	}
	
	public void deselectAll() {
		for(Entry<String, Boolean> item : items) {
			item.setValue(Boolean.FALSE);
		}
		if(listener != null)
			listener.onSelectionChanged(items);
		repaint();
	}
	
	public void selectAll() {
		for(Entry<String, Boolean> item : items) {
			item.setValue(Boolean.TRUE);
		}
		if(listener != null)
			listener.onSelectionChanged(items);
		repaint();
	}
	
	public void setSelection(List<String> selection) {
		Set<String> selSet = new HashSet<String>(selection);
		for(Entry<String, Boolean> item : items) {
			if(selSet.contains(item.getKey())) {
				item.setValue(Boolean.TRUE);
			}else {
				item.setValue(Boolean.FALSE);
			}
		}
		if(listener != null)
			listener.onSelectionChanged(items);
		repaint();
	}
	
	public void addSelectionListener(SelectionListener listener) {
		this.listener = listener;
	}
	
	public List<String> getSelection(){
		List<String> sel = new ArrayList<String>();
		for(Entry<String, Boolean> item : items)
			if(item.getValue().booleanValue())
				sel.add(item.getKey());
		return sel;
	}
	
}
