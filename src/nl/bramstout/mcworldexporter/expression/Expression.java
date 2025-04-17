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

package nl.bramstout.mcworldexporter.expression;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import nl.bramstout.mcworldexporter.expression.ExprValue.ExprValueBool;
import nl.bramstout.mcworldexporter.expression.ExprValue.ExprValueDict;
import nl.bramstout.mcworldexporter.expression.ExprValue.ExprValueFloat;
import nl.bramstout.mcworldexporter.expression.ExprValue.ExprValueInt;
import nl.bramstout.mcworldexporter.expression.ExprValue.ExprValueNull;

public abstract class Expression {

	public abstract ExprValue eval(ExprContext context);
	
	public static class ExpressionMulti extends Expression{
		
		private String code;
		private Expression[] statements;
		
		public ExpressionMulti(String code, Expression[] statements) {
			this.code = code;
			this.statements = statements;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			try {
				ExprValue prevReturnVal = context.returnValue;
				context.returnValue = null;
				ExprValue val = null;
				for(int i = 0; i < statements.length; ++i) {
					val = statements[i].eval(context);
					if(context.returnValue != null) {
						val = context.returnValue;
						break;
					}
				}
				context.returnValue = prevReturnVal;
				if(val == null)
					val = new ExprValue(new ExprValueNull());
				return val;
			}catch(Exception ex) {
				throw new RuntimeException("Exception while evaluating expression \"" + code + "\"", ex);
			}
		}
		
	}
	
	public static class ExpressionConstant extends Expression{
		
		private ExprValue value;
		
		public ExpressionConstant(ExprValue value) {
			this.value = value;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			return value;
		}
		
	}
	
	public static class ExpressionConstantDict extends Expression{
		
		private Map<String, Expression> value;
		
		public ExpressionConstantDict(Map<String, Expression> value) {
			this.value = value;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			ExprValue val = new ExprValue(new ExprValueDict());
			for(Entry<String, Expression> entry : value.entrySet()) {
				val.member(entry.getKey()).set(entry.getValue().eval(context));
			}
			return val;
		}
		
	}
	
	public static class ExpressionConstantArray extends Expression{
		
		private List<Expression> value;
		
		public ExpressionConstantArray(List<Expression> value) {
			this.value = value;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			ExprValue val = new ExprValue(new ExprValueDict());
			for(int i = 0; i < value.size(); ++i) {
				val.member(Integer.toString(i)).set(value.get(i).eval(context));
			}
			return val;
		}
		
	}
	
	public static class ExpressionStoreVariable extends Expression{
		
		private Expression variable;
		private Expression value;
		
		public ExpressionStoreVariable(Expression variable, Expression value) {
			this.variable = variable;
			this.value = value;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			ExprValue val = variable.eval(context);
			val.set(value.eval(context));
			return val;
		}
		
	}
	
	public static class ExpressionAddStoreVariable extends Expression{
		
		private Expression variable;
		private Expression value;
		
		public ExpressionAddStoreVariable(Expression variable, Expression value){
			this.variable = variable;
			this.value = value;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			ExprValue val = variable.eval(context);
			val.set(val.add(value.eval(context)));
			return val;
		}
		
	}
	
	public static class ExpressionSubStoreVariable extends Expression{
		
		private Expression variable;
		private Expression value;
		
		public ExpressionSubStoreVariable(Expression variable, Expression value){
			this.variable = variable;
			this.value = value;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			ExprValue val = variable.eval(context);
			val.set(val.sub(value.eval(context)));
			return val;
		}
		
	}
	
	public static class ExpressionMultStoreVariable extends Expression{
		
		private Expression variable;
		private Expression value;
		
		public ExpressionMultStoreVariable(Expression variable, Expression value){
			this.variable = variable;
			this.value = value;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			ExprValue val = variable.eval(context);
			val.set(val.mult(value.eval(context)));
			return val;
		}
		
	}
	
	public static class ExpressionDivStoreVariable extends Expression{
		
		private Expression variable;
		private Expression value;
		
		public ExpressionDivStoreVariable(Expression variable, Expression value){
			this.variable = variable;
			this.value = value;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			ExprValue val = variable.eval(context);
			val.set(val.div(value.eval(context)));
			return val;
		}
		
	}
	
	public static class ExpressionAdd extends Expression{
		
		private Expression left;
		private Expression right;
		
		public ExpressionAdd(Expression left, Expression right) {
			this.left = left;
			this.right = right;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			return left.eval(context).add(right.eval(context));
		}
		
	}
	
	public static class ExpressionSub extends Expression{
		
		private Expression left;
		private Expression right;
		
		public ExpressionSub(Expression left, Expression right) {
			this.left = left;
			this.right = right;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			return left.eval(context).sub(right.eval(context));
		}
		
	}
	
	public static class ExpressionMult extends Expression{
		
		private Expression left;
		private Expression right;
		
		public ExpressionMult(Expression left, Expression right) {
			this.left = left;
			this.right = right;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			return left.eval(context).mult(right.eval(context));
		}
		
	}
	
	public static class ExpressionDiv extends Expression{
		
		private Expression left;
		private Expression right;
		
		public ExpressionDiv(Expression left, Expression right) {
			this.left = left;
			this.right = right;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			return left.eval(context).div(right.eval(context));
		}
		
	}
	
	public static class ExpressionInvert extends Expression{
		
		private Expression expr;
		
		public ExpressionInvert(Expression expr) {
			this.expr = expr;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			return new ExprValue(new ExprValueBool(!expr.eval(context).asBool()));
		}
		
	}
	
	public static class ExpressionNegate extends Expression{
		
		private Expression expr;
		
		public ExpressionNegate(Expression expr) {
			this.expr = expr;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			ExprValue val = expr.eval(context);
			if(val.getImpl() instanceof ExprValueInt)
				return new ExprValue(new ExprValueInt(-val.asInt()));
			else
				return new ExprValue(new ExprValueFloat(-val.asFloat()));
		}
		
	}
	
	public static class ExpressionEqual extends Expression{
		
		private Expression left;
		private Expression right;
		
		public ExpressionEqual(Expression left, Expression right) {
			this.left = left;
			this.right = right;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			return new ExprValue(new ExprValueBool(left.eval(context).equal(right.eval(context))));
		}
		
	}
	
	public static class ExpressionNotEqual extends Expression{
		
		private Expression left;
		private Expression right;
		
		public ExpressionNotEqual(Expression left, Expression right) {
			this.left = left;
			this.right = right;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			return new ExprValue(new ExprValueBool(!(left.eval(context).equal(right.eval(context)))));
		}
		
	}
	
	public static class ExpressionLessThan extends Expression{
		
		private Expression left;
		private Expression right;
		
		public ExpressionLessThan(Expression left, Expression right) {
			this.left = left;
			this.right = right;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			return new ExprValue(new ExprValueBool(left.eval(context).lessThan(right.eval(context))));
		}
		
	}
	
	public static class ExpressionGreaterThan extends Expression{
		
		private Expression left;
		private Expression right;
		
		public ExpressionGreaterThan(Expression left, Expression right) {
			this.left = left;
			this.right = right;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			return new ExprValue(new ExprValueBool(left.eval(context).greaterThan(right.eval(context))));
		}
		
	}
	
	public static class ExpressionLessThanOrEqual extends Expression{
		
		private Expression left;
		private Expression right;
		
		public ExpressionLessThanOrEqual(Expression left, Expression right) {
			this.left = left;
			this.right = right;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			ExprValue leftValue = left.eval(context);
			ExprValue rightValue = right.eval(context);
			
			return new ExprValue(new ExprValueBool(leftValue.equal(rightValue) || leftValue.lessThan(rightValue)));
		}
		
	}
	
	public static class ExpressionGreaterThanOrEqual extends Expression{
		
		private Expression left;
		private Expression right;
		
		public ExpressionGreaterThanOrEqual(Expression left, Expression right) {
			this.left = left;
			this.right = right;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			ExprValue leftValue = left.eval(context);
			ExprValue rightValue = right.eval(context);
			
			return new ExprValue(new ExprValueBool(leftValue.equal(rightValue) || leftValue.greaterThan(rightValue)));
		}
		
	}
	
	public static class ExpressionAnd extends Expression{
		
		private Expression left;
		private Expression right;
		
		public ExpressionAnd(Expression left, Expression right) {
			this.left = left;
			this.right = right;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			ExprValue leftValue = left.eval(context);
			ExprValue rightValue = right.eval(context);
			
			return new ExprValue(new ExprValueBool(leftValue.asBool() && rightValue.asBool()));
		}
		
	}
	
	public static class ExpressionOr extends Expression{
		
		private Expression left;
		private Expression right;
		
		public ExpressionOr(Expression left, Expression right) {
			this.left = left;
			this.right = right;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			ExprValue leftValue = left.eval(context);
			ExprValue rightValue = right.eval(context);
			
			return new ExprValue(new ExprValueBool(leftValue.asBool() || rightValue.asBool()));
		}
		
	}
	
	public static class ExpressionTernary extends Expression{
		
		private Expression condition;
		private Expression left;
		private Expression right;
		
		public ExpressionTernary(Expression condition, Expression left, Expression right) {
			this.condition = condition;
			this.left = left;
			this.right = right;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			if(condition.eval(context).asBool())
				return left.eval(context);
			return right.eval(context);
		}
		
	}
	
	public static class ExpressionGetVariable extends Expression{
		
		private String name;
		
		public ExpressionGetVariable(String name) {
			this.name = name;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			if(name.equalsIgnoreCase("thisBlock"))
				return context.thisBlock;
			if(name.equalsIgnoreCase("thisEntity"))
				return context.thisBlock;
			if(name.equalsIgnoreCase("global")) {
				return context.globals;
			}
			
			ExprValue val = context.builtins.getOrDefault(name, null);
			if(val != null)
				return val;
			
			val = context.localFunctions.getOrDefault(name, null);
			if(val != null)
				return val;
			
			val = context.variables.getOrDefault(name, null);
			if(val == null) {
				val = new ExprValue(new ExprValueNull());
				context.variables.put(name, val);
			}
			return val;
		}
		
	}
	
	public static class ExpressionGetMember extends Expression{
		
		private Expression parent;
		private String name;
		
		public ExpressionGetMember(Expression parent, String name) {
			this.parent = parent;
			this.name = name;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			ExprValue parentValue = parent.eval(context);
			
			return parentValue.member(name);
		}
		
	}
	
	public static class ExpressionGetIndex extends Expression{
		
		private Expression parent;
		private Expression index;
		
		public ExpressionGetIndex(Expression parent, Expression index) {
			this.parent = parent;
			this.index = index;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			ExprValue parentValue = parent.eval(context);
			
			return parentValue.member(index.eval(context).asString());
		}
		
	}
	
	public static class ExpressionCall extends Expression{
		
		private Expression function;
		private List<Expression> arguments;
		
		public ExpressionCall(Expression function, List<Expression> arguments) {
			this.function = function;
			this.arguments = arguments;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			ExprContext newContext = new ExprContext(context);
			
			for(int i = 0; i < arguments.size(); ++i) {
				newContext.variables.put("arg" + i, arguments.get(i).eval(context));
			}
			return function.eval(context).call(newContext);
		}
		
	}
	
	public static class ExpressionReturn extends Expression{
		
		private Expression value;
		
		public ExpressionReturn(Expression value) {
			this.value = value;
		}
		
		@Override
		public ExprValue eval(ExprContext context) {
			ExprValue returnVal = value.eval(context);
			context.returnValue = returnVal;
			return returnVal;
		}
		
	}
	
}
