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

package nl.bramstout.mcworldexporter.model.builtins;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import nl.bramstout.mcworldexporter.Config;
import nl.bramstout.mcworldexporter.model.BlockState;
import nl.bramstout.mcworldexporter.model.builtins.BuiltInBlockState.BuiltInBlockStateHandler;

public class BuiltInBlockStateRegistry {
	
	public static Map<String, Class<? extends BlockState>> builtins = new HashMap<String, Class<? extends BlockState>>();
	
	public static void load(){
		BuiltInBlockState.load();
		
		Map<String, Class<? extends BlockState>> builtins = new HashMap<String, Class<? extends BlockState>>();
		builtins.put("dragon_head", BlockStateSkull.class);
		builtins.put("dragon_wall_head", BlockStateSkull.class);
		
		builtins.put("minecraft:white_banner", BlockStateBanner.class);
		builtins.put("minecraft:orange_banner", BlockStateBanner.class);
		builtins.put("minecraft:magenta_banner", BlockStateBanner.class);
		builtins.put("minecraft:light_blue_banner", BlockStateBanner.class);
		builtins.put("minecraft:yellow_banner", BlockStateBanner.class);
		builtins.put("minecraft:lime_banner", BlockStateBanner.class);
		builtins.put("minecraft:pink_banner", BlockStateBanner.class);
		builtins.put("minecraft:gray_banner", BlockStateBanner.class);
		builtins.put("minecraft:light_gray_banner", BlockStateBanner.class);
		builtins.put("minecraft:cyan_banner", BlockStateBanner.class);
		builtins.put("minecraft:purple_banner", BlockStateBanner.class);
		builtins.put("minecraft:blue_banner", BlockStateBanner.class);
		builtins.put("minecraft:brown_banner", BlockStateBanner.class);
		builtins.put("minecraft:green_banner", BlockStateBanner.class);
		builtins.put("minecraft:red_banner", BlockStateBanner.class);
		builtins.put("minecraft:black_banner", BlockStateBanner.class);
		builtins.put("minecraft:white_wall_banner", BlockStateBanner.class);
		builtins.put("minecraft:orange_wall_banner", BlockStateBanner.class);
		builtins.put("minecraft:magenta_wall_banner", BlockStateBanner.class);
		builtins.put("minecraft:light_blue_wall_banner", BlockStateBanner.class);
		builtins.put("minecraft:yellow_wall_banner", BlockStateBanner.class);
		builtins.put("minecraft:lime_wall_banner", BlockStateBanner.class);
		builtins.put("minecraft:pink_wall_banner", BlockStateBanner.class);
		builtins.put("minecraft:gray_wall_banner", BlockStateBanner.class);
		builtins.put("minecraft:light_gray_wall_banner", BlockStateBanner.class);
		builtins.put("minecraft:cyan_wall_banner", BlockStateBanner.class);
		builtins.put("minecraft:purple_wall_banner", BlockStateBanner.class);
		builtins.put("minecraft:blue_wall_banner", BlockStateBanner.class);
		builtins.put("minecraft:brown_wall_banner", BlockStateBanner.class);
		builtins.put("minecraft:green_wall_banner", BlockStateBanner.class);
		builtins.put("minecraft:red_wall_banner", BlockStateBanner.class);
		builtins.put("minecraft:black_wall_banner", BlockStateBanner.class);
		
		for(String liquidType : Config.liquid)
			builtins.put(liquidType, BlockStateLiquid.class);
		
		BuiltInBlockStateRegistry.builtins = builtins;
	}
	
	public static BlockState newBlockState(String name, int dataVersion) {
		BuiltInBlockStateHandler builtInBlockStateHandler = BuiltInBlockState.getHandler(name);
		if(builtInBlockStateHandler != null) {
			// We have a custom version specified.
			return new BuiltInBlockState(name, dataVersion, builtInBlockStateHandler);
		}
		
		Class<? extends BlockState> classObj = builtins.get(name);
		try {
			return classObj.getConstructor(String.class, int.class).newInstance(name, dataVersion);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
		return null;
	}

}
