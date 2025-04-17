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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.bramstout.mcworldexporter.expression.ExprValue.ExprValueBool;
import nl.bramstout.mcworldexporter.expression.ExprValue.ExprValueFloat;
import nl.bramstout.mcworldexporter.expression.ExprValue.ExprValueInt;
import nl.bramstout.mcworldexporter.expression.ExprValue.ExprValueNull;
import nl.bramstout.mcworldexporter.expression.ExprValue.ExprValueString;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionAdd;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionAddStoreVariable;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionAnd;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionCall;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionConstant;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionConstantArray;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionConstantDict;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionDiv;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionDivStoreVariable;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionEqual;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionGetIndex;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionGetMember;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionGetVariable;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionGreaterThan;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionGreaterThanOrEqual;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionInvert;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionLessThan;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionLessThanOrEqual;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionMult;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionMultStoreVariable;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionMulti;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionNegate;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionNotEqual;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionOr;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionReturn;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionStoreVariable;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionSub;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionSubStoreVariable;
import nl.bramstout.mcworldexporter.expression.Expression.ExpressionTernary;

public class ExprParser {

	private static class CodeIterator{
		
		private String code;
		private int index;
		
		public CodeIterator(String code) {
			this.code = code;
			this.index = 0;
		}
		
		public int getIndex() {
			return index;
		}
		
		public String getCode() {
			return code;
		}
		
		public int peek() {
			if(index >= code.length())
				return 0;
			return code.codePointAt(index);
		}
		
		public int peek(int offset) {
			if((index + offset) < 0 || (index + offset) >= code.length())
				return 0;
			return code.codePointAt(index + offset);
		}
		
		public boolean hasToken(String token) {
			int upperBound = Math.min(code.length(), index + token.length());
			for(int i = index; i < upperBound; ++i) {
				if(code.codePointAt(i) != token.codePointAt(i - index))
					return false;
			}
			if(upperBound < code.length()) {
				// We got the token, but the token in the actual code
				// might still have more characters and therefore actually
				// be a different token, so let's make sure that the next
				// character isn't a token character.
				int nextCodePoint = code.codePointAt(upperBound);
				return !Character.isDigit(nextCodePoint) && !Character.isAlphabetic(nextCodePoint) && nextCodePoint != '_';
			}
			return true;
		}
		
		public void next() {
			next(1);
		}
		
		public void next(int offset) {
			index += offset;
			if(index > code.length())
				index = code.length();
			// Keep moving until we don't have a whitespace anymore.
			while(true) {
				int codePoint = peek();
				if(codePoint == 0 || !Character.isWhitespace(codePoint))
					break;
				index++;
			}
		}
		
		public void skipWhitespaces() {
			// Keep moving until we don't have a whitespace anymore.
			while(true) {
				int codePoint = peek();
				if(codePoint == 0 || !Character.isWhitespace(codePoint))
					break;
				index++;
			}
		}
		
		public String getToken() {
			int start = index;
			int i = start;
			for(i = start; i < code.length(); ++i) {
				int codePoint = code.codePointAt(i);
				if(!Character.isDigit(codePoint) && !Character.isAlphabetic(codePoint) && codePoint != '_')
					break;
			}
			index = i;
			skipWhitespaces();
			return code.substring(start, i);
		}
		
	}
	
	public static Expression parseExpression(CodeIterator code) {
		code.skipWhitespaces();
		int codePoint = code.peek();
		if(codePoint == 0)
			return new ExpressionConstant(new ExprValue(new ExprValueNull()));
		
		Expression expr = parseExpressionPart(code, null, false);
		codePoint = code.peek();
		while(codePoint != 0 && codePoint != ';') {
			expr = parseExpressionPart(code, expr, false);
			codePoint = code.peek();
		}
		return expr;
	}
	
	public static boolean isValidExpression(Expression expr) {
		if(expr == null)
			return false;
		
		if(expr instanceof ExpressionConstant) {
			if(((ExpressionConstant)expr).eval(null).isNull())
				return false;
		}
		
		return true;
	}
	
	public static ExpressionMulti parseMultiExpression(CodeIterator code) {
		int codeStartIndex = code.index;
		Expression[] statements = new Expression[0];
		
		Expression expr = parseExpression(code);
		while(isValidExpression(expr)) {
			statements = Arrays.copyOf(statements, statements.length + 1);
			statements[statements.length-1] = expr;
			expr = parseExpression(code);
		}
		
		return new ExpressionMulti(code.code.substring(codeStartIndex, code.index), statements);
	}
	
	public static ExpressionMulti parseMultiExpression(String code) {
		return parseMultiExpression(new CodeIterator(code));
	}
	
	private static Expression parseSubExpression(CodeIterator code) {
		int codePoint = code.peek();
		if(codePoint == 0)
			return new ExpressionConstant(new ExprValue(new ExprValueNull()));
		
		
		Expression expr = null;
		while(true) {
			expr = parseExpressionPart(code, expr, false);
			codePoint = code.peek();
			
			if(codePoint == '.' || codePoint == '[' || codePoint == '(')
				continue;
			
			break;
		}
		return expr;
	}
	
	private static Expression parseSubExpression2(CodeIterator code) {
		int codePoint = code.peek();
		if(codePoint == 0)
			return new ExpressionConstant(new ExprValue(new ExprValueNull()));
		
		
		Expression expr = null;
		while(true) {
			Expression expr2 = parseExpressionPart(code, expr, true);
			if(expr2 == null)
				break;
			expr = expr2;
			codePoint = code.peek();
			
			if(codePoint == 0)
				break;
		}
		return expr;
	}
	
	private static Expression parseExpressionPart(CodeIterator code, Expression prevExpr, boolean noException) {
		int codePoint = code.peek();
		if(codePoint == '!') {
			code.next();
			codePoint = code.peek();
			if(codePoint == '=' && prevExpr != null) {
				code.next();
				return new ExpressionNotEqual(prevExpr, parseSubExpression(code));
			}
			return new ExpressionInvert(parseSubExpression(code));
		}
		if(codePoint == '-' && (Character.isDigit(code.peek(1)) || code.peek(1) == '.')) {
			// It's a digit
			code.next();
			String token = code.getToken();
			codePoint = code.peek();
			if(codePoint == '.') {
				// It's a float.
				code.next();
				String fraction = code.getToken();
				float val = 0f;
				try {
					val = Float.parseFloat(token + "." + fraction);
				}catch(Exception ex) {}
				return new ExpressionConstant(new ExprValue(new ExprValueFloat(-val)));
			}
			// It's an int
			long val = 0;
			try {
				val = Long.parseLong(token);
			}catch(Exception ex) {}
			return new ExpressionConstant(new ExprValue(new ExprValueInt(-val)));
		}
		if(codePoint == '0' && (code.peek(1) == 'x' || code.peek(1) == 'X')) {
			// Hexidecimal
			code.next(); // skip 0
			code.next(); // skip x
			String token = code.getToken();
			long val = 0;
			try {
				val = Long.parseUnsignedLong(token, 16);
			}catch(Exception ex) {}
			return new ExpressionConstant(new ExprValue(new ExprValueInt(val)));
		}
		if(codePoint == '0' && (code.peek(1) == 'b' || code.peek(1) == 'b')) {
			// Binary
			code.next(); // skip 0
			code.next(); // skip x
			String token = code.getToken();
			long val = 0;
			try {
				val = Long.parseUnsignedLong(token, 2);
			}catch(Exception ex) {}
			return new ExpressionConstant(new ExprValue(new ExprValueInt(val)));
		}
		if(codePoint == '.' && Character.isDigit(code.peek(1))) {
			// Float
			code.next();
			String fraction = code.getToken();
			float val = 0f;
			try {
				val = Float.parseFloat("0." + fraction);
			}catch(Exception ex) {}
			return new ExpressionConstant(new ExprValue(new ExprValueFloat(val)));
		}
		if(Character.isDigit(codePoint)) {
			// Normal number
			String token = code.getToken();
			codePoint = code.peek();
			if(codePoint == '.') {
				// It's a float.
				code.next();
				String fraction = code.getToken();
				float val = 0f;
				try {
					val = Float.parseFloat(token + "." + fraction);
				}catch(Exception ex) {}
				return new ExpressionConstant(new ExprValue(new ExprValueFloat(val)));
			}
			// It's an int
			long val = 0;
			try {
				val = Long.parseLong(token);
			}catch(Exception ex) {}
			return new ExpressionConstant(new ExprValue(new ExprValueInt(val)));
		}
		if(code.hasToken("true")) {
			// True boolean value
			code.next("true".length());
			return new ExpressionConstant(new ExprValue(new ExprValueBool(true)));
		}
		if(code.hasToken("false")) {
			// True boolean value
			code.next("false".length());
			return new ExpressionConstant(new ExprValue(new ExprValueBool(false)));
		}
		if(code.hasToken("null")) {
			// True boolean value
			code.next("null".length());
			return new ExpressionConstant(new ExprValue(new ExprValueNull()));
		}
		if(code.hasToken("return")) {
			code.next("return".length());
			return new ExpressionReturn(parseSubExpression(code));
		}
		if(codePoint == '\'' || codePoint == '"') {
			int terminationChar = codePoint;
			int startChar = code.getIndex();
			code.next();
			while(true) {
				int c = code.peek();
				if(c == '\\') {
					// Skip this char and the next one
					code.index += 2;
					continue;
				}
				if(c == 0 || c == terminationChar) {
					break;
				}
				code.index++;
			}
			startChar = startChar + 1;
			int endChar = code.getIndex();
			String val = "";
			if(endChar > startChar)
				val = code.getCode().substring(startChar, endChar);
			code.next();
			
			// Handle some escaped characters
			if(terminationChar == '\'')
				val = val.replace("\\'", "'");
			else if(terminationChar == '"')
				val = val.replace("\\\"", "\"");
			val = val.replace("\\\n", "\n");
			
			return new ExpressionConstant(new ExprValue(new ExprValueString(val)));
		}
		if(codePoint == '{') {
			Map<String, Expression> val = new HashMap<String, Expression>();
			
			code.next();
			while(true) {
				int c = code.peek();
				if(c == 0 || c == '}') {
					code.next();
					break;
				}
				if(c == ',') {
					code.next();
					continue;
				}
				Expression nameExpr = parseSubExpression2(code);
				String name = "";
				if(nameExpr instanceof ExpressionConstant) {
					ExprValue nameVal = ((ExpressionConstant) nameExpr).eval(null);
					name = nameVal.asString();
				}
				c = code.peek();
				if(c == ':') {
					code.next();
				}
				Expression valExpr = parseSubExpression2(code);
				
				val.put(name, valExpr);
			}
			
			return new ExpressionConstantDict(val);
		}
		if(codePoint == '[' && prevExpr == null) {
			// Array literal
			List<Expression> val = new ArrayList<Expression>();
			
			code.next();
			while(true) {
				int c = code.peek();
				if(c == 0 || c == ']') {
					code.next();
					break;
				}
				if(c == ',') {
					code.next();
					continue;
				}
				
				Expression valExpr = parseSubExpression2(code);
				
				val.add(valExpr);
			}
			
			return new ExpressionConstantArray(val);
		}
		if(codePoint == '[' && prevExpr != null) {
			// Array/Dict access
			code.next(); // [
			Expression keyExpr = parseSubExpression2(code);
			code.next(); // ]
			return new ExpressionGetIndex(prevExpr, keyExpr);
		}
		if(codePoint == '(' && prevExpr != null) {
			// Function call
			List<Expression> val = new ArrayList<Expression>();
			
			code.next();
			while(true) {
				int c = code.peek();
				if(c == 0 || c == ')') {
					code.next();
					break;
				}
				if(c == ',') {
					code.next();
					continue;
				}
				
				Expression valExpr = parseSubExpression2(code);
				
				val.add(valExpr);
			}
			
			return new ExpressionCall(prevExpr, val);
		}
		if(codePoint == '(' && prevExpr == null) {
			// Sub expression
			code.next(); // (
			Expression expr = parseSubExpression2(code);
			code.next(); // )
			return expr;
		}
		if(codePoint == '=' && prevExpr != null) {
			code.next();
			codePoint = code.peek();
			if(codePoint == '=') {
				// Equals
				code.next();
				return new ExpressionEqual(prevExpr, parseSubExpression(code));
			}
			// Assignment
			return new ExpressionStoreVariable(prevExpr, parseSubExpression2(code));
		}
		if(codePoint == '<' && prevExpr != null) {
			code.next();
			codePoint = code.peek();
			if(codePoint == '=') {
				// Less than or equal
				code.next();
				return new ExpressionLessThanOrEqual(prevExpr, parseSubExpression(code));
			}
			// Less than
			return new ExpressionLessThan(prevExpr, parseSubExpression(code));
		}
		if(codePoint == '>' && prevExpr != null) {
			code.next();
			codePoint = code.peek();
			if(codePoint == '=') {
				// Greater than or equal
				code.next();
				return new ExpressionGreaterThanOrEqual(prevExpr, parseSubExpression(code));
			}
			// Greater than
			return new ExpressionGreaterThan(prevExpr, parseSubExpression(code));
		}
		if(codePoint == '&' && prevExpr != null) {
			code.next();
			codePoint = code.peek();
			if(codePoint == '&') {
				// And
				code.next();
				return new ExpressionAnd(prevExpr, parseSubExpression(code));
			}
		}
		if(codePoint == '|' && prevExpr != null) {
			code.next();
			codePoint = code.peek();
			if(codePoint == '|') {
				// And
				code.next();
				return new ExpressionOr(prevExpr, parseSubExpression(code));
			}
		}
		if(codePoint == '?' && prevExpr != null) {
			// Ternary
			code.next(); // ?
			Expression ifTrue = parseSubExpression(code);
			code.next(); // :
			Expression ifFalse = parseSubExpression(code);
			return new ExpressionTernary(prevExpr, ifTrue, ifFalse);
		}
		if(codePoint == '+' && prevExpr != null) {
			code.next();
			codePoint = code.peek();
			if(codePoint == '=') {
				// Asignment
				code.next();
				return new ExpressionAddStoreVariable(prevExpr, parseSubExpression2(code));
			}
			return new ExpressionAdd(prevExpr, parseSubExpression(code));
		}
		if(codePoint == '-' && prevExpr != null) {
			code.next();
			codePoint = code.peek();
			if(codePoint == '=') {
				// Asignment
				code.next();
				return new ExpressionSubStoreVariable(prevExpr, parseSubExpression2(code));
			}
			return new ExpressionSub(prevExpr, parseSubExpression(code));
		}
		if(codePoint == '-' && prevExpr == null) {
			code.next();
			codePoint = code.peek();
			return new ExpressionNegate(parseSubExpression(code));
		}
		if(codePoint == '*' && prevExpr != null) {
			code.next();
			codePoint = code.peek();
			if(codePoint == '=') {
				// Asignment
				code.next();
				return new ExpressionMultStoreVariable(prevExpr, parseSubExpression2(code));
			}
			return new ExpressionMult(prevExpr, parseSubExpression(code));
		}
		if(codePoint == '/' && prevExpr != null) {
			code.next();
			codePoint = code.peek();
			if(codePoint == '=') {
				// Asignment
				code.next();
				return new ExpressionDivStoreVariable(prevExpr, parseSubExpression2(code));
			}
			return new ExpressionDiv(prevExpr, parseSubExpression(code));
		}
		if(codePoint == '.' && prevExpr != null) {
			// Get member
			code.next();
			String memberName = code.getToken();
			return new ExpressionGetMember(prevExpr, memberName);
		}
		if(Character.isLetter(codePoint) || codePoint == '_') {
			// Variable access
			String token = code.getToken();
			return new ExpressionGetVariable(token);
		}
		if(noException)
			return null;
		throw new RuntimeException("Invalid syntax at index " + code.getIndex() + " for code \"" + code.getCode() + "\"");
	}
	
}
