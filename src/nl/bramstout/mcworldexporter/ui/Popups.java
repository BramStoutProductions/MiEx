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

import java.awt.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import nl.bramstout.mcworldexporter.MCWorldExporter;

public class Popups {
	
	public static final int ERROR_MESSAGE = JOptionPane.ERROR_MESSAGE;
    public static final int INFORMATION_MESSAGE = JOptionPane.INFORMATION_MESSAGE;
    public static final int WARNING_MESSAGE = JOptionPane.WARNING_MESSAGE;
    public static final int QUESTION_MESSAGE = JOptionPane.QUESTION_MESSAGE;
    public static final int PLAIN_MESSAGE = JOptionPane.PLAIN_MESSAGE;
    public static final int DEFAULT_OPTION = JOptionPane.DEFAULT_OPTION;
    public static final int YES_NO_OPTION = JOptionPane.YES_NO_OPTION;
    public static final int YES_NO_CANCEL_OPTION = JOptionPane.YES_NO_CANCEL_OPTION;
    public static final int OK_CANCEL_OPTION = JOptionPane.OK_CANCEL_OPTION;
    public static final int YES_OPTION = JOptionPane.YES_OPTION;
    public static final int NO_OPTION = JOptionPane.NO_OPTION;
    public static final int CANCEL_OPTION = JOptionPane.CANCEL_OPTION;
    public static final int OK_OPTION = JOptionPane.OK_OPTION;
    public static final int CLOSED_OPTION = JOptionPane.CLOSED_OPTION;
	
    public static void showMessageDialog(Component parentComponent, Object message){
        showMessageDialog(parentComponent, message, UIManager.getString(
                "OptionPane.messageDialogTitle"), JOptionPane.INFORMATION_MESSAGE);
    }
    
	public static void showMessageDialog(Component parentComponent, Object message, String title){
        showMessageDialog(parentComponent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }
	
	public static void showMessageDialog(Component parentComponent, Object message, String title, int messageType){
        showMessageDialog(parentComponent, message, title, messageType, null);
    }
	
	public static void showMessageDialog(Component parentComponent, Object message, String title, int messageType, Icon icon){
		if(MCWorldExporter.cliMode) {
			String messageStr = message.toString();
			if(message instanceof List) {
				messageStr = "";
				for(Object obj : ((List<?>) message)) {
					if(messageStr.equals(""))
						messageStr = obj.toString();
					else
						messageStr = messageStr + ", " + obj.toString();
				}
			}
			System.out.println("[MSG] " + title.replace('\n', ' ') + ": " + messageStr.replace('\n', ' '));
		}else {
			JOptionPane.showMessageDialog(parentComponent, message, title, messageType, icon);
		}
    }
	
	private static Map<String, Object> inputDialogSelections = new HashMap<String, Object>();
	
	/**
	 * In CLI mode, input dialogs won't be shown, but we still need a way to
	 * be able to specify the value. Rather than outputting a prompt
	 * and reading stdin, we use this method to specify what the value should be.
	 * If the selection is set to null, then it's the same as resetting it.
	 * @param title
	 * @param selection
	 */
	public static void setInputDialogSelection(String title, Object selection) {
		if(selection == null)
			inputDialogSelections.remove(title);
		else
			inputDialogSelections.put(title, selection);
	}
	
	public static Object showInputDialog(Component parentComponent,
	        Object message, String title, int messageType, Icon icon,
	        Object[] selectionValues, Object initialSelectionValue){
		if(MCWorldExporter.cliMode) {
			Object selection = inputDialogSelections.getOrDefault(title, null);
			if(selection != null) {
				// Make sure that it's actually a valid selection.
				for(Object selection2 : selectionValues) {
					if(selection2.equals(selection)) {
						return selection2;
					}
				}
			}
			return initialSelectionValue;
		}else {
			return JOptionPane.showInputDialog(parentComponent, message, title, messageType, icon, selectionValues, initialSelectionValue);
		}
	}
	
	public static int showConfirmDialog(Component parentComponent,
	        Object message, String title, int optionType) {
		if(MCWorldExporter.cliMode) {
			Object selection = inputDialogSelections.getOrDefault(title, null);
			if(selection != null && selection instanceof Integer) {
				return ((Integer) selection).intValue();
			}
			return CLOSED_OPTION;
		}else {
			return JOptionPane.showConfirmDialog(parentComponent, message, title, optionType);
		}
	}

}
