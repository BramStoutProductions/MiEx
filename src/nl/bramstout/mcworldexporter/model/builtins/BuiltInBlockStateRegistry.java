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
		/*builtins.put("minecraft:chest", BlockStateChest.class);
		builtins.put("minecraft:trapped_chest", BlockStateChest.class);
		builtins.put("minecraft:ender_chest", BlockStateChest.class);*/
		
		/*builtins.put("minecraft:acacia_sign", BlockStateSign.class);
		builtins.put("minecraft:bamboo_sign", BlockStateSign.class);
		builtins.put("minecraft:birch_sign", BlockStateSign.class);
		builtins.put("minecraft:cherry_sign", BlockStateSign.class);
		builtins.put("minecraft:crimson_sign", BlockStateSign.class);
		builtins.put("minecraft:dark_oak_sign", BlockStateSign.class);
		builtins.put("minecraft:jungle_sign", BlockStateSign.class);
		builtins.put("minecraft:mangrove_sign", BlockStateSign.class);
		builtins.put("minecraft:oak_sign", BlockStateSign.class);
		builtins.put("minecraft:spruce_sign", BlockStateSign.class);
		builtins.put("minecraft:warped_sign", BlockStateSign.class);
		builtins.put("minecraft:acacia_wall_sign", BlockStateSign.class);
		builtins.put("minecraft:bamboo_wall_sign", BlockStateSign.class);
		builtins.put("minecraft:birch_wall_sign", BlockStateSign.class);
		builtins.put("minecraft:cherry_wall_sign", BlockStateSign.class);
		builtins.put("minecraft:crimson_wall_sign", BlockStateSign.class);
		builtins.put("minecraft:dark_oak_wall_sign", BlockStateSign.class);
		builtins.put("minecraft:jungle_wall_sign", BlockStateSign.class);
		builtins.put("minecraft:mangrove_wall_sign", BlockStateSign.class);
		builtins.put("minecraft:oak_wall_sign", BlockStateSign.class);
		builtins.put("minecraft:spruce_wall_sign", BlockStateSign.class);
		builtins.put("minecraft:warped_wall_sign", BlockStateSign.class);*/
		
		/*builtins.put("minecraft:acacia_hanging_sign", BlockStateHangingSign.class);
		builtins.put("minecraft:bamboo_hanging_sign", BlockStateHangingSign.class);
		builtins.put("minecraft:birch_hanging_sign", BlockStateHangingSign.class);
		builtins.put("minecraft:cherry_hanging_sign", BlockStateHangingSign.class);
		builtins.put("minecraft:crimson_hanging_sign", BlockStateHangingSign.class);
		builtins.put("minecraft:dark_oak_hanging_sign", BlockStateHangingSign.class);
		builtins.put("minecraft:jungle_hanging_sign", BlockStateHangingSign.class);
		builtins.put("minecraft:mangrove_hanging_sign", BlockStateHangingSign.class);
		builtins.put("minecraft:oak_hanging_sign", BlockStateHangingSign.class);
		builtins.put("minecraft:spruce_hanging_sign", BlockStateHangingSign.class);
		builtins.put("minecraft:warped_hanging_sign", BlockStateHangingSign.class);
		builtins.put("minecraft:acacia_wall_hanging_sign", BlockStateHangingSign.class);
		builtins.put("minecraft:bamboo_wall_hanging_sign", BlockStateHangingSign.class);
		builtins.put("minecraft:birch_wall_hanging_sign", BlockStateHangingSign.class);
		builtins.put("minecraft:cherry_wall_hanging_sign", BlockStateHangingSign.class);
		builtins.put("minecraft:crimson_wall_hanging_sign", BlockStateHangingSign.class);
		builtins.put("minecraft:dark_oak_wall_hanging_sign", BlockStateHangingSign.class);
		builtins.put("minecraft:jungle_wall_hanging_sign", BlockStateHangingSign.class);
		builtins.put("minecraft:mangrove_wall_hanging_sign", BlockStateHangingSign.class);
		builtins.put("minecraft:oak_wall_hanging_sign", BlockStateHangingSign.class);
		builtins.put("minecraft:spruce_wall_hanging_sign", BlockStateHangingSign.class);
		builtins.put("minecraft:warped_wall_hanging_sign", BlockStateHangingSign.class);*/
		
		/*builtins.put("minecraft:shulker_box", BlockStateShulkerBox.class);
		builtins.put("minecraft:white_shulker_box", BlockStateShulkerBox.class);
		builtins.put("minecraft:orange_shulker_box", BlockStateShulkerBox.class);
		builtins.put("minecraft:magenta_shulker_box", BlockStateShulkerBox.class);
		builtins.put("minecraft:light_blue_shulker_box", BlockStateShulkerBox.class);
		builtins.put("minecraft:yellow_shulker_box", BlockStateShulkerBox.class);
		builtins.put("minecraft:lime_shulker_box", BlockStateShulkerBox.class);
		builtins.put("minecraft:pink_shulker_box", BlockStateShulkerBox.class);
		builtins.put("minecraft:gray_shulker_box", BlockStateShulkerBox.class);
		builtins.put("minecraft:light_gray_shulker_box", BlockStateShulkerBox.class);
		builtins.put("minecraft:cyan_shulker_box", BlockStateShulkerBox.class);
		builtins.put("minecraft:purple_shulker_box", BlockStateShulkerBox.class);
		builtins.put("minecraft:blue_shulker_box", BlockStateShulkerBox.class);
		builtins.put("minecraft:brown_shulker_box", BlockStateShulkerBox.class);
		builtins.put("minecraft:green_shulker_box", BlockStateShulkerBox.class);
		builtins.put("minecraft:red_shulker_box", BlockStateShulkerBox.class);
		builtins.put("minecraft:black_shulker_box", BlockStateShulkerBox.class);*/
		
		//builtins.put("minecraft:end_portal", BlockStateEndPortal.class);
		
		/*builtins.put("minecraft:bed", BlockStateBed.class);
		builtins.put("minecraft:white_bed", BlockStateBed.class);
		builtins.put("minecraft:orange_bed", BlockStateBed.class);
		builtins.put("minecraft:magenta_bed", BlockStateBed.class);
		builtins.put("minecraft:light_blue_bed", BlockStateBed.class);
		builtins.put("minecraft:yellow_bed", BlockStateBed.class);
		builtins.put("minecraft:lime_bed", BlockStateBed.class);
		builtins.put("minecraft:pink_bed", BlockStateBed.class);
		builtins.put("minecraft:gray_bed", BlockStateBed.class);
		builtins.put("minecraft:light_gray_bed", BlockStateBed.class);
		builtins.put("minecraft:cyan_bed", BlockStateBed.class);
		builtins.put("minecraft:purple_bed", BlockStateBed.class);
		builtins.put("minecraft:blue_bed", BlockStateBed.class);
		builtins.put("minecraft:brown_bed", BlockStateBed.class);
		builtins.put("minecraft:green_bed", BlockStateBed.class);
		builtins.put("minecraft:red_bed", BlockStateBed.class);
		builtins.put("minecraft:black_bed", BlockStateBed.class);*/
		
		/*builtins.put("skeleton_skull", BlockStateSkull.class);
		builtins.put("wither_skeleton_skull", BlockStateSkull.class);
		builtins.put("zombie_head", BlockStateSkull.class);
		builtins.put("player_head", BlockStateSkull.class);
		builtins.put("creeper_head", BlockStateSkull.class);
		builtins.put("dragon_head", BlockStateSkull.class);
		builtins.put("skeleton_wall_skull", BlockStateSkull.class);
		builtins.put("wither_skeleton_wall_skull", BlockStateSkull.class);
		builtins.put("zombie_wall_head", BlockStateSkull.class);
		builtins.put("player_wall_head", BlockStateSkull.class);
		builtins.put("creeper_wall_head", BlockStateSkull.class);
		builtins.put("dragon_wall_head", BlockStateSkull.class);*/
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
