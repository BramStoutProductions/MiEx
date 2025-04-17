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

package nl.bramstout.mcworldexporter.resourcepack;

import nl.bramstout.mcworldexporter.model.Model;
import nl.bramstout.mcworldexporter.nbt.NbtTagCompound;

public abstract class ItemHandler {
	
	public static String DISP_CONTEXT_NONE = "none";
	public static String DISP_CONTEXT_THIRDPERSON_LEFTHAND = "thirdperson_lefthand";
	public static String DISP_CONTEXT_THIRDPERSON_RIGHTHAND = "thirdperson_righthand";
	public static String DISP_CONTEXT_FIRSTPERSON_LEFTHAND = "firstperson_lefthand";
	public static String DISP_CONTEXT_FIRSTPERSON_RIGHTHAND = "firstperson_righthand";
	public static String DISP_CONTEXT_HEAD = "head";
	public static String DISP_CONTEXT_GUI = "gui";
	public static String DISP_CONTEXT_GROUND = "ground";
	public static String DISP_CONTEXT_FIXED = "fixed";
	
	public abstract Model getModel(String name, NbtTagCompound data, String displayContext);
	
}
