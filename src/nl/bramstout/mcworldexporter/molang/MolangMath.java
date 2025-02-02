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

package nl.bramstout.mcworldexporter.molang;

import nl.bramstout.mcworldexporter.Random;
import nl.bramstout.mcworldexporter.molang.MolangValue.MolangFunction;
import nl.bramstout.mcworldexporter.molang.MolangValue.MolangObject;

public class MolangMath extends MolangObject{

	private static class Abs extends MolangScript{
		public Abs() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float value = context.getTempDict().getField("value").asNumber(context);
			return new MolangValue((float) Math.abs(value));
		}
	}
	
	private static class Acos extends MolangScript{
		public Acos() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float value = context.getTempDict().getField("value").asNumber(context);
			return new MolangValue((float) Math.toDegrees(Math.acos(value)));
		}
	}
	
	private static class Asin extends MolangScript{
		public Asin() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float value = context.getTempDict().getField("value").asNumber(context);
			return new MolangValue((float) Math.toDegrees(Math.asin(value)));
		}
	}
	
	private static class Atan extends MolangScript{
		public Atan() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float value = context.getTempDict().getField("value").asNumber(context);
			return new MolangValue((float) Math.toDegrees(Math.atan(value)));
		}
	}
	
	private static class Atan2 extends MolangScript{
		public Atan2() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float x = context.getTempDict().getField("x").asNumber(context);
			float y = context.getTempDict().getField("y").asNumber(context);
			return new MolangValue((float) Math.toDegrees(Math.atan2(x, y)));
		}
	}
	
	private static class Ceil extends MolangScript{
		public Ceil() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float value = context.getTempDict().getField("value").asNumber(context);
			return new MolangValue((float) Math.ceil(value));
		}
	}
	
	private static class Clamp extends MolangScript{
		public Clamp() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float value = context.getTempDict().getField("value").asNumber(context);
			float min = context.getTempDict().getField("min").asNumber(context);
			float max = context.getTempDict().getField("max").asNumber(context);
			return new MolangValue((float) Math.max(Math.min(value, max), min));
		}
	}
	
	private static class Cos extends MolangScript{
		public Cos() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float value = context.getTempDict().getField("value").asNumber(context);
			return new MolangValue((float) Math.cos(Math.toRadians(value)));
		}
	}
	
	private static class DieRoll extends MolangScript{
		Random random;
		public DieRoll(Random random) {super(null); this.random = random;}
		@Override
		public MolangValue eval(MolangContext context) {
			float num = context.getTempDict().getField("num").asNumber(context);
			float low = context.getTempDict().getField("low").asNumber(context);
			float high = context.getTempDict().getField("high").asNumber(context);
			float value = 0f;
			for(float i = 0; i < num; num += 1f)
				value += random.nextFloat() * (high - low) + low;
			return new MolangValue(value);
		}
	}
	
	private static class DieRollInteger extends MolangScript{
		Random random;
		public DieRollInteger(Random random) {super(null); this.random = random;}
		@Override
		public MolangValue eval(MolangContext context) {
			float num = context.getTempDict().getField("num").asNumber(context);
			float low = context.getTempDict().getField("low").asNumber(context);
			float high = context.getTempDict().getField("high").asNumber(context);
			float value = 0f;
			for(float i = 0; i < num; num += 1f)
				value += random.nextInt((int) low, (int) high);
			return new MolangValue(value);
		}
	}
	
	private static class Exp extends MolangScript{
		public Exp() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float value = context.getTempDict().getField("value").asNumber(context);
			return new MolangValue((float) Math.exp(value));
		}
	}
	
	private static class Floor extends MolangScript{
		public Floor() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float value = context.getTempDict().getField("value").asNumber(context);
			return new MolangValue((float) Math.floor(value));
		}
	}
	
	private static class HermiteBlend extends MolangScript{
		public HermiteBlend() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float value = context.getTempDict().getField("value").asNumber(context);
			value = 3f * value * value - 2f * value * value * value;
			return new MolangValue(value);
		}
	}
	
	private static class Lerp extends MolangScript{
		public Lerp() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float start = context.getTempDict().getField("start").asNumber(context);
			float end = context.getTempDict().getField("end").asNumber(context);
			float t = context.getTempDict().getField("0_to_1").asNumber(context);
			return new MolangValue(end * t + start * (1f - t));
		}
	}
	
	private static class LerpRotate extends MolangScript{
		public LerpRotate() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float start = context.getTempDict().getField("start").asNumber(context);
			float end = context.getTempDict().getField("end").asNumber(context);
			float t = context.getTempDict().getField("0_to_1").asNumber(context);
			float d = end - start;
			if(Math.abs(start - end) < Math.abs(d))
				d = start - end;
			return new MolangValue(start + d * t);
		}
	}
	
	private static class Ln extends MolangScript{
		public Ln() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float value = context.getTempDict().getField("value").asNumber(context);
			return new MolangValue((float) Math.log(value));
		}
	}
	
	private static class Max extends MolangScript{
		public Max() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float A = context.getTempDict().getField("A").asNumber(context);
			float B = context.getTempDict().getField("B").asNumber(context);
			return new MolangValue((float) Math.max(A, B));
		}
	}
	
	private static class Min extends MolangScript{
		public Min() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float A = context.getTempDict().getField("A").asNumber(context);
			float B = context.getTempDict().getField("B").asNumber(context);
			return new MolangValue((float) Math.min(A, B));
		}
	}
	
	private static class MinAngle extends MolangScript{
		public MinAngle() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float value = context.getTempDict().getField("value").asNumber(context);
			value += 180f;
			value /= 360f;
			value -= Math.floor(value);
			value *= 360f;
			value -= 180f;
			return new MolangValue(value);
		}
	}
	
	private static class Mod extends MolangScript{
		public Mod() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float value = context.getTempDict().getField("value").asNumber(context);
			float denominator = context.getTempDict().getField("denominator").asNumber(context);
			value /= denominator;
			value -= Math.floor(value);
			value *= denominator;
			return new MolangValue(value);
		}
	}
	
	private static class Pi extends MolangScript{
		public Pi() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			return new MolangValue((float) Math.PI);
		}
	}
	
	private static class Pow extends MolangScript{
		public Pow() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float base = context.getTempDict().getField("base").asNumber(context);
			float exponent = context.getTempDict().getField("exponent").asNumber(context);
			return new MolangValue((float) Math.pow(base, exponent));
		}
	}
	
	private static class RandomFloat extends MolangScript{
		Random random;
		public RandomFloat(Random random) {super(null); this.random = random;}
		@Override
		public MolangValue eval(MolangContext context) {
			float low = context.getTempDict().getField("value").asNumber(context);
			float high = context.getTempDict().getField("value").asNumber(context);
			return new MolangValue((float) ((random.nextFloat() * (high - low)) + low));
		}
	}
	
	private static class RandomInteger extends MolangScript{
		Random random;
		public RandomInteger(Random random) {super(null); this.random = random;}
		@Override
		public MolangValue eval(MolangContext context) {
			float low = context.getTempDict().getField("value").asNumber(context);
			float high = context.getTempDict().getField("value").asNumber(context);
			return new MolangValue((float) Math.floor((random.nextFloat() * (high - low)) + low));
		}
	}
	
	private static class Round extends MolangScript{
		public Round() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float value = context.getTempDict().getField("value").asNumber(context);
			return new MolangValue((float) Math.round(value));
		}
	}
	
	private static class Sin extends MolangScript{
		public Sin() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float value = context.getTempDict().getField("value").asNumber(context);
			return new MolangValue((float) Math.sin(Math.toRadians(value)));
		}
	}
	
	private static class Sqrt extends MolangScript{
		public Sqrt() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float value = context.getTempDict().getField("value").asNumber(context);
			return new MolangValue((float) Math.sqrt(value));
		}
	}
	
	private static class Trunc extends MolangScript{
		public Trunc() {super(null);}
		@Override
		public MolangValue eval(MolangContext context) {
			float value = context.getTempDict().getField("value").asNumber(context);
			if(value >= 0f)
				return new MolangValue((float) Math.floor(value));
			return new MolangValue((float) Math.ceil(value));
		}
	}
	
	public MolangMath(Random random) {
		super();
		
		getFields().put("abs", new MolangValue(new MolangFunction(new Abs(), "value")));
		getFields().put("acos", new MolangValue(new MolangFunction(new Acos(), "value")));
		getFields().put("asin", new MolangValue(new MolangFunction(new Asin(), "value")));
		getFields().put("atan", new MolangValue(new MolangFunction(new Atan(), "value")));
		getFields().put("atan2", new MolangValue(new MolangFunction(new Atan2(), "x", "y")));
		getFields().put("ceil", new MolangValue(new MolangFunction(new Ceil(), "value")));
		getFields().put("clamp", new MolangValue(new MolangFunction(new Clamp(), "value", "min", "max")));
		getFields().put("cos", new MolangValue(new MolangFunction(new Cos(), "value")));
		getFields().put("die_roll", new MolangValue(new MolangFunction(new DieRoll(random), "num", "low", "high")));
		getFields().put("die_roll_integer", new MolangValue(new MolangFunction(new DieRollInteger(random), "num", "low", "high")));
		getFields().put("exp", new MolangValue(new MolangFunction(new Exp(), "value")));
		getFields().put("floor", new MolangValue(new MolangFunction(new Floor(), "value")));
		getFields().put("hermite_blend", new MolangValue(new MolangFunction(new HermiteBlend(), "value")));
		getFields().put("lerp", new MolangValue(new MolangFunction(new Lerp(), "start", "end", "0_to_1")));
		getFields().put("lerprotate", new MolangValue(new MolangFunction(new LerpRotate(), "start", "end", "0_to_1")));
		getFields().put("ln", new MolangValue(new MolangFunction(new Ln(), "value")));
		getFields().put("max", new MolangValue(new MolangFunction(new Max(), "A", "B")));
		getFields().put("min", new MolangValue(new MolangFunction(new Min(), "A", "B")));
		getFields().put("min_angle", new MolangValue(new MolangFunction(new MinAngle(), "value")));
		getFields().put("mod", new MolangValue(new MolangFunction(new Mod(), "value", "denominator")));
		getFields().put("pi", new MolangValue(new MolangFunction(new Pi())));
		getFields().put("pow", new MolangValue(new MolangFunction(new Pow(), "base", "exponent")));
		getFields().put("random", new MolangValue(new MolangFunction(new RandomFloat(random), "low", "high")));
		getFields().put("random_integer", new MolangValue(new MolangFunction(new RandomInteger(random), "low", "high")));
		getFields().put("round", new MolangValue(new MolangFunction(new Round(), "value")));
		getFields().put("sin", new MolangValue(new MolangFunction(new Sin(), "value")));
		getFields().put("sqrt", new MolangValue(new MolangFunction(new Sqrt(), "value")));
		getFields().put("trunc", new MolangValue(new MolangFunction(new Trunc(), "value")));
	}
	
}
