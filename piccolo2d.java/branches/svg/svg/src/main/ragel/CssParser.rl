/*
 * Copyright (c) 2008, Piccolo2D project, http://piccolo2d.org
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * None of the name of the Piccolo2D project, the University of Maryland, or the names of its contributors
 * may be used to endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.piccolo2d.svg.css;

import org.piccolo2d.svg.RagelParser;
import java.util.LinkedHashMap;
import java.text.ParseException;

/** <a href="http://research.cs.queensu.ca/~thurston/ragel/">Ragel</a> parser 
 * for <a href="http://www.w3.org/TR/CSS21/grammar.html">CSS</a> - This file is auto-generated by rl2java.sh.
 * <p>
 * DO NOT EDIT MANUALLY!!!
 * </p>
 * See Also:<ul>
 * <li>Another CSS Grammar: http://www.w3.org/TR/CSS21/syndata.html</li>
 * <li>A ragel css grammar: http://labs.ohloh.net/ohcount/browser/ext/ohcount_native/ragel_parsers/css.rl?rev=83e15c94ca8c53994ef07cdb6c7a5ceffe67884c</li>
 * </ul>
 */
class CssParser extends CssParserBase implements RagelParser {
%%{

	machine css;

	#######################################################
	## Define the actions
	#######################################################

	action CLEAR {
		buf.setLength(0);
	}

	action BUF {
		buf.append(data[p]);
	}

	action ELEMENT {
		pushElement(buf);
	}

	action ANYELEMENT {
		pushElement(ANY_ELEMENT);
	}

	action CLASS {
		pushClass(buf);
	}

	action SIMPLEEND {
		finishElement();
	}

	action SELECTOR {
		finishSelector();
	}

	action PROPERTY {
		pushProperty(buf);
	}

	action EXPR {
		pushExpr(buf);
	}

	action RULESET {
		finishRuleSet(css);
	}

	#######################################################
	## Define the grammar
	#######################################################

	comment	= /\/\*[^*]*\*+([^/*][^*]*\*+)*\//;
	
	S = space;
	COMMA = ",";
	PLUS = "+";
	GREATER = ">";	
	IDENT = [_a-zA-Z] @BUF (([_a-zA-Z0-9] | '-') @BUF) *;

	element_name = (IDENT | ("*" @BUF) ) >CLEAR %ELEMENT;

	class = ("." IDENT) >CLEAR %CLASS S*;
	
#	combinator = (PLUS | GREATER | S) >CLEAR @BUF %COMBINATOR;
	combinator = S;

	simple_selector = ((element_name | (class >ANYELEMENT)) S* (class S*)*)  %SIMPLEEND;

#  	simple_selector = (((class S*)+ >ANYELEMENT) | (element_name S* (class S*)* )) %SIMPLEEND;
  
#	selector = simple_selector ( combinator S* simple_selector )*;
	selector = (simple_selector $(sel,1) %(sel,0) (combinator S* simple_selector $(sel,1) %(sel,0) )*) %SELECTOR;

	property = IDENT >CLEAR %PROPERTY;

	value = ( (any - (";" | S | "}") ) @BUF )+;

	expr = value >CLEAR %EXPR;

	declaration = (property S* ':' S* expr)? S*;
	
	ruleset =  selector ( COMMA S* selector )*
    "{" S* declaration (";" S* declaration )* "}" S* %RULESET;
	
	stylesheet = S* ruleset*;
	
	main := stylesheet;
}%%

%% write data;

	final LinkedHashMap parse(final CharSequence data, final LinkedHashMap css) throws ParseException {
		return parse(data.toString().toCharArray(), css);
	}

	// LinkedHashMap<Pattern, Map<CharSequence, CharSequence>>
	final LinkedHashMap parse(final char[] data, LinkedHashMap css) throws ParseException {
		if(css == null)
			css = new LinkedHashMap();
		// high-level buffers
		final StringBuilder buf = new StringBuilder();
		final double[] argv = new double[7];
		int argc = 0;
		boolean absolute = true;
		
		// ragel variables (low level)
		final int pe = data.length;
		final int eof = pe;
		int cs, p = 0;
		int top;

		%% write init;
		%% write exec;

		if ( cs < css_first_final )
			throw new ParseException(new String(data), p);
        return css;
	}
}
