/*
 * Copyright 2017, Robert Dyer, Che Shian Hung
 *                 and Bowling Green State University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package boa.test.functions;

import static org.junit.Assert.assertEquals;
import static boa.functions.BoaAstIntrinsics.parseexpression;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import boa.functions.BoaNormalFormIntrinsics;
import boa.types.Ast.Expression;

/**
 * Test expression reduction.
 *
 * @author rdyer
 * @author cheshianhung
 */
@RunWith(Parameterized.class)
public class TestReduce {
	@Parameters(name = "{index}][{0} = {1}")
	public static Collection<String[]> expressions() {
		return Arrays.asList(new String[][] {
			// literals
			{ "5", "5" },
			{ "8.0", "8.0" },

			{ "+2", "2" },
			{ "+(+2)", "2" },
			{ "+ +2", "2" },

			{ "-8.0", "-8.0" },
			{ "-2", "-2" },
			{ "-(-2)", "2" },
			{ "- -2", "2" },
			{ "- - -2", "-2" },
			{ "- - - -2", "2" },

			{ "+ -2", "-2" },
			{ "- +2", "-2" },
			{ "- + +2", "-2" },
			{ "+ - +2", "-2" },
			{ "+ + -2", "-2" },
			{ "- - +2", "2" },
			{ "+ - -2", "2" },
			{ "- + -2", "2" },
			{ "- + - +2", "2" },
			{ "+ - - +2", "2" },
			{ "+ (- - +2)", "2" },
			{ "+ + - +2", "-2" },

			// add operator
			{ "1 + 2", "3" },
			{ "5 + 2 + 1", "8" },
			{ "5.0 + 2 + 1", "8.0" },
			{ "-1 + -2", "-3" },

			{ "0.1 + 1 + 10 + 100", "111.1" },
			{ "-0.1 + 1 + 10 + 100", "110.9" },
			{ "0.1 + -1 + 10 + 100", "109.1" },
			{ "0.1 + 1 + -10 + 100", "91.1" },
			{ "0.1 + (1 + -10) + 100", "91.1" },
			{ "0.1 + 1 + 10 + -100", "-88.9" },
			{ "-0.1 + -1 + 10 + 100", "108.9" },
			{ "-0.1 + 1 + -10 + 100", "90.9" },
			{ "-0.1 + 1 + 10 + -100", "-89.1" },
			{ "0.1 + -1 + -10 + 100", "89.1" },
			{ "0.1 + -1 + 10 + -100", "-90.9" },
			{ "0.1 + 1 + -10 + -100", "-108.9" },
			{ "-0.1 + -1 + -10 + 100", "88.9" },
			{ "-0.1 + -1 + 10 + -100", "-91.1" },
			{ "-0.1 + 1 + -10 + -100", "-109.1" },
			{ "0.1 + -1 + -10 + -100", "-110.9" },
			{ "-0.1 + -1 + -10 + -100", "-111.1" },

			// subtract operator
			{ "2 - 1", "1" },
			{ "1 - 2 - 3", "-4" },
			{ "2 - -5 - 1", "6" },
			{ "5.0 - 2 - 1", "2.0" },
			{ "5 - (3 - 2)", "4" },
			{ "(5 - 3) - 2", "0" },

			{ "111.1 - 100 - 10 - 1 - 0.1", "0.0" },
			{ "-111.1 - 100 - 10 - 1 - 0.1", "-222.2" },
			{ "111.1 - -100 - 10 - 1 - 0.1", "200.0" },
			{ "111.1 - (-100 - 10) - 1 - 0.1", "220.0" },
			{ "111.1 - -(-100 - 10) - 1 - 0.1", "0.0" },
			{ "111.1 - -(-(-100 - 10) - 1) - 0.1", "220.0" },
			{ "111.1 - -(-(-(-100 - 10) -1) - 0.1)", "2.0" },

			// multiply operator
			{ "2 * 5 * 1", "10" },
			{ "5.0 * 2 * 1", "10.0" },
			{ "999 * 999 * 0", "0" },

			{ "1 * 1 * 1", "1" },
			{ "-1 * 2 * 3", "-6" },
			{ "1 * -2 * 3", "-6" },
			{ "1 * 2 * -3", "-6" },
			{ "-1 * -2 * 3", "6" },
			{ "1 * -2 * -3", "6" },
			{ "-1 * -2 * -3", "-6" },

			// divide operator
			{ "12 / 2 / 3", "2" },
			{ "10.0 / 2 / 1", "5.0" },

			{ "10 / 3", "10 / 3" },
			{ "10.0 / 3.0", "10.0 / 3.0" },
			{ "10.0 / 3", "10.0 / 3" },
			{ "10 / 3.0", "10 / 3.0" },
			{ "5 / -1", "-5" },

			// with variables
			{ "+x", "x" },
			{ "+(+x)", "x" },
			{ "+ + +x", "x" },
			{ "+ + + +x", "x" },

			{ "-x", "-x" },
			{ "- -x", "x" },
			{ "- - -x", "-x" },
			{ "- - - -x", "x" },

			{ "x + 5.0 + 1", "6.0 + x" },
			{ "-x + 5.0 - 1", "4.0 - x" },
			{ "-x + 1 - 5", "-4 - x" },
			{ "x * 5.0 * 1", "5.0 * x" },
			{ "x / 10.0 * 5", "5 * x / 10.0" },

			{ "5.0 + x + 1", "6.0 + x" },
			{ "5.0 - x - 1", "4.0 - x" },
			{ "1 - x - 5", "-4 - x" },
			{ "5.0 * x * 1", "5.0 * x" },

			{ "5.0 + 1 + x", "6.0 + x" },
			{ "5.0 - 1 - x", "4.0 - x" },
			{ " 1 - 5 - x", "-4 - x" },
			{ "5.0 * 1 * x", "5.0 * x" },

			{ "1.0 / x", "1.0 / x"},
			{ "5.0 / x / 5", "1.0 / x" },
			{ "5.0 / 5 / x", "1.0 / x" },

			{ "5.0 * x / 5", "x" },
			{ "x * 5.0 / 5", "x" },

			{ "5 * (1 + x) / 5", "1 + x" },
			{ "5 * (1.0 + x) / 5", "1.0 + x" },
			{ "5.0 * (1 + x) / 5", "1.0 + x" },
			{ "5 * (1 + x) / 5.0", "1.0 + x" },

			{ "5 * x / 5", "x" },
			{ "x * 5 / 5", "x" },

			// identities
			{ "0 + x", "x" },
			{ "x + 0.0", "x" },
			{ "x + (1 - 1.0)", "x" },
			{ "x + (3 - 3)", "x" },
			{ "(3 - 3) + x", "x" },

			{ "0 - x", "-x" },
			{ "x - 0", "x" },
			{ "x - (3 - 3)", "x" },
			{ "(3 - 3) - x", "-x" },

			{ "1 * x", "x" },
			{ "x * 1", "x" },
			{ "x * (3 - 2)", "x" },
			{ "(3 - 2) * x", "x" },
			{ "-1 * x", "-x" },
			{ "x * -1", "-x" },

			{ "x / 1", "x" },
			{ "x / -1", "-x"},
			{ "x / 1 / 1 / 1", "x" },

			{ "x + 1 - 1", "x" },
			{ "1 + x - 1", "x" },
			{ "1 - 1 + x", "x" }, 
			{ "-x + 1 - 1", "-x" },
			{ "1 + -x - 1", "-x" },
			{ "1 - 1 + -x", "-x" },
			{ "x * 2 / 2", "x" },
			{ "2 * x / 2", "x" },
			{ "2 / 2 * x", "x" },
			{ "-x * 2 / 2", "-x" },
			{ "2 * -x / 2", "-x" },
			{ "2 / 2 * -x", "-x" },

			{ "x / x * x", "x" },
			{ "x * x / x", "x" },
			{ "-x * x / x", "-x" },
			{ "x * -x / x", "-x" },
			{ "x * x / -x", "-x" },
			{ "-x * -x / x", "x" },
			{ "-x * x / -x", "x" },
			{ "x * -x / -x", "x" },
			{ "-x * -x / -x", "-x"},

			// elimination
			{ "0 * x", "0" },
			{ "x * 0", "0" },
			{ "x * (1 - 1)", "0" },
			{ "(1 - 1) * x", "0" },
			{ "x - x", "0" },
			{ "x / x", "1" },

			{ "x * 0 * 0", "0" },
			{ "0 * x * 0", "0" },
			{ "0 * 0 * x", "0" },
			{ "-x * 0 * 0", "0" },
			{ "0 * -x * 0", "0" },
			{ "0 * 0 * -x", "0" },
			{ "x * x * 0", "0" },
			{ "x * 0 * x", "0" },
			{ "0 * x * x", "0" },
			{ "x + x - x - x", "0" },
			{ "x - x + x - x", "0" },
			{ "x - x - x + x", "0" },
			{ "-x - x + x + x", "0" },
			{ "-x + x - x + x", "0" },
			{ "-x + x + x - x", "0" },
			{ "x * x / x / x", "1" },
			{ "x / x * x / x", "1" },
			{ "x / x / x * x", "1" },
			{ "-x / x * x / x", "-1" },
			{ "x / -x * x / x", "-1" },
			{ "x / x * -x / x", "-1" },
			{ "x / x * x / -x", "-1" },
			{ "x / -x", "-1" },
			{ "x / -1 / 1 / -1 / -1", "-x"},
			{ "x / -1 / x / 1 / -1", "1"},
			{ "x * -x * x * -x / x / x / -x / -x", "1"},
			{ "x * x * x * -x / x / x / -x / -x", "-1"},
			{ "1 / -x / x / -x * x * x * -x", "-1"},
			{ "1 / x / x / -x * x * x * -x", "1"},

			// with methods 
			{ "foo(x + 3, 2 + 1)", "foo(3 + x, 3)" },
			{ "3 + foo(2 + 1) - 1", "2 + foo(3)" },

			{ "foo(x + 1 + 1, y + 2 + 2)", "foo(2 + x, 4 + y)" },
			{ "foo(1 + x + 1, 2 + y + 2)", "foo(2 + x, 4 + y)" },
			{ "foo(1 + 1 + x, 2 + 2 + y)", "foo(2 + x, 4 + y)" },
			{ "foo(x + 1 - 1, y + 2 - 2)", "foo(x, y)" },
			{ "foo(x - 1 + 1, y - 2 + 2)", "foo(x, y)" },

			{ "foo() + 1 + 1", "2 + foo()" },
			{ "1 + foo() + 1", "2 + foo()" },
			{ "1 + 1 + foo()", "2 + foo()" },
			{ "foo() + 1 - 1", "foo()" },
			{ "foo() - 1 + 1", "foo()" },
			{ "1 + foo() - 1", "foo()" },
			{ "-1 + foo() + 1", "foo()" },
			{ "1 - 1 + foo()", "foo()" },
			{ "-1 + 1 + foo()", "foo()" },

			// complex expressions, multiple operators etc
			{ "-5 * -x", "5 * x" },
			{ "-y * -5 * -x", "-5 * x * y" },
			{ "-2 * -y * -5 * -x", "10 * x * y" },
			{ "5 - 3 + 2", "4" },
			{ "5 - (3 + 2)", "0" },
			{ "5.0 / x / 5 * 10.0 * x", "10.0" },
			{ "-5 * (5 - 10) + 1", "26" },
			{ "-5 * (5 - 5) + 1", "1" },
			{ "-5 / (3 + 2) + 1", "0" },
			{ "(-5 / (3 + 2)) + 1", "0" },
			{ "-5 / ((3 + 2) + 1)", "-5 / 6" },
			{ "-5 / ((3 + 2.0) + 1)", "-5 / 6.0" },
			{ "-6 / -3 + -5 * -3", "17" },
			{ "-6 / -3 + (-5 * -3)", "17" },
			{ "-2 / (-3 + -5) * -3", "-6 / 8" },
			{ "-2 / (-3.0 + -5) * -3", "-6 / 8.0" },
			{ "5.0 + 3.5 - 2 * a.length", "8.5 - 2 * a.length" },
			{ "5.0 + a.length * (3.5 - 2)", "5.0 + 1.5 * a.length" },
			{ "5 + x + 3 + x", "8 + 2 * x" },
			{ "5 + (x + 3) + x", "8 + 2 * x" },
			{ "5 - (x - 3) - x", "8 - 2 * x" },
			{ "5 - 2 * x + 3", "8 - 2 * x" },
			{ "(x - 3) - x", "-3" },
			{ "5.0 + +3.5 - -2 * a.length", "8.5 + 2 * a.length" },
			{ "(5.0 + 3.5) - -2 * a.length", "8.5 + 2 * a.length" },
			{ "5.0 + 3.5 - -2 * a.length + x", "8.5 + 2 * a.length + x" },
			{ "5.0 + 3.5 - (-2 * a.length + x)", "8.5 + 2 * a.length - x" },
			{ "5.0 + 3.5 - -2 * (a.length + x)", "8.5 + 2 * a.length + 2 * x" },
			{ "5 - m(a) + 0", "5 - m(a)" },
			{ "5 - -m(a) - 0", "5 + m(a)" },
			{ "5 + +m(a) - 0", "5 + m(a)" },
			{ "5 + m() + -0", "5 + m()" },
			{ "5 + m(a).x * 0", "5" },
			{ "5 + m(a, b).x / -1", "5 - m(a, b).x" },
			{ "(5 + m(1 + 2, b).x) / -1", "-5 - m(3, b).x" },

			{ "x / 0", "x / 0" },
			{ "x + 2 * 2", "4 + x" },
			{ "x * 2 + 2", "2 + 2 * x" },
			{ "x - 2 * 2", "-4 + x" },
			{ "-x - 2 * 2", "-4 - x" },
			{ "x * 2 - 2", "-2 + 2 * x" },
			{ "-x * 2 - 2", "-2 - 2 * x" },
			{ "x + 6 / 3", "2 + x" },
			{ "x / 3 + 6", "6 + x / 3" },

			{ "1 + -x + 2 + 3", "6 - x" },
			{ "1 + -x + 2 - 4", "-1 - x" },
			{ "1 + -x - 4 + 2", "-1 - x" },
			{ "5 + (x + 3) + x + 2 - 3", "7 + 2 * x" },
			{ "5 + (x + 3) + x - 2 + 3", "9 + 2 * x" },
			{ "5 + (x + 3) + x * 2 - 3", "5 + 3 * x" },
			{ "x + 3 * x + 4 * x + x", "9 * x" },
			{ "x + 3.5 * x + 4.2 * x + x", "9.7 * x" },
			{ "5 + 3 + x + 3.5 * x + 4.2 * x + x", "8 + 9.7 * x" },
			{ "5 + x + 3.5 * x + 4.2 * x + x + 3", "8 + 9.7 * x" },
			{ "x + 3.5 * x + 4.2 * x + x + 5 + 3", "8 + 9.7 * x" },
			{ "0.5 * x + 0.5 * x", "x" },
			{ "-0.5 * x + -0.5 * x", "-x" },
			{ "2 * x + -1 * x", "x" },
			{ "-2 * x + 1 * x", "-x" },
			{ "5 + (x + 3) - x + 2 - 3", "7" },
			{ "5 + (x + 3) + x + 2 * 3", "14 + 2 * x" },
			{ "5 + (x + 3) + x - 2 * 3", "2 + 2 * x" },
			{ "2 + ((8 + x) - x) + 3", "13" },
			{ "2 + 5 + x + -x + x + -2 * x + 3", "10 - x" },
			{ "x + -x + x + -2 * x + 2 + 5 + 3", "10 - x" },
			{ "x + -x + x + 2 * -x + 2 + 5 + 3", "10 - x" },
			{ "x + -x + x + 2 * -x + 2 + 5 + 3 + x", "10" },
			{ "x + -x + x + 2 * -x + 2 + 5 + 3 + x + x", "10 + x" },
			{ "x + -x + x + 2 * -x + 2 + 5 + 3 + x + x + x", "10 + 2 * x" },
			{ "x + -x + x + 2 * -x + 2 + 5 + 3 + x + x + x + 0.5 * -x", "10 + 1.5 * x" },
			{ "x + -x + x + 2 * -x + 2 + 5 + 3 + x + x + x - 0.5 * x", "10 + 1.5 * x" },
			{ "1 - 2 * x - 3 + -2", "-4 - 2 * x" },
			{ "((8 + x) - x)", "8" },
			{ "8 - -x - 2 * x", "8 - x" },
			{ "x * ((-y) / z)", "-x * y / z"},
			{ "x * y + x * y", "2 * x * y"},
			{ "x * y + x * -y + -x * y + x * y", "0"},
			{ "x * y + x * -y + -x * y + -x * y", "-2 * x * y"},
			{ "2 * x * y + x * -y", "x * y"},
			{ "2 * x * y + -x * y", "x * y"},
			{ "2 * x * y + -2 * x * y", "0"},
			{ "2 * x * y + -x * y + 2 * x * -y", "-x * y"},
			{ "x * y * z + x * -y * z + x * y * -z", "-x * y * z"},
			{ "x * -y * z", "-x * y * z"},
			{ "x * y * -z", "-x * y * z"},
			{ "x * -y * -z", "x * y * z"},
			{ "x * -y * -z * -a", "-a * x * y * z"},
			{ "x * y + x * y - x * y + x * y", "2 * x * y"},
			{ "x * y - x * y + x * y + x * y", "2 * x * y"},
			{ "2 * 3 * x + 2 * 4 * x", "14 * x"},
			{ "2 * x * 3 + x * 4 * 2", "14 * x"},
			{ "2 * -x * -y + 3 * x * -y", "-x * y"},
			{ "x * y - x * y - x * y + x * y", "0"},
			{ "x * y + x * y + x * y - x * y", "2 * x * y"},
			{ "x + 2 * 3 + 2 * 3", "12 + x"},
			{ "x * (2 * x)", "2 * x * x"},
			{ "-x * -y * -x", "-x * x * y"},
			{ "-x * y * z + x * -y * z + x * y * -z", "-3 * x * y * z"},
			{ "-2 * x + -3 * x", "-5 * x"},
			{ "2 * x * y + -2 * x * y", "0"},
			{ "3 + 2 * x * y - 5 + -2 * x * y + 9", "7"},

			{ "2 * x * y - x * y - 3 * x * y", "-2 * x * y"},
			{ "x - 3 * y - y", "x - 4 * y"},
			{ "3 * y - x - y", "2 * y - x"},
			{ "3 * y - y - x", "2 * y - x"},
			{ "x + y - y - 2 * y + 3 * x", "4 * x - 2 * y"},
			{ "2.5 * x - 1.3 * x - 3 * x - y - z + 2 * y - y + 2 * z", "z - 1.8 * x"},
			{ "x + 2 * x + 2 * (x * y) + (x * 2) * y", "3 * x + 4 * x * y"},
			{ "x + 2 * x + 2 * (y * x) + (x * 2) * y + y * (x * 2)", "3 * x + 6 * x * y"},
			{ "x * y", "x * y"},
			{ "y * x", "x * y"},
			{ "x - y + x - z - 0.5 * x", "1.5 * x - y - z"},
			{ "x - z + x - y - 0.5 * x", "1.5 * x - y - z"},
			{ "x + 2 * (x + y) - y", "3 * x + y"},
			{ "2 * (x + y)", "2 * x + 2 * y"},

			{ "y / 2 / 2", "y / 4"},
			{ "3 * x / y / 2", "3 * x / (2 * y)"},
			{ "3 * x / y / 2 / z", "3 * x / (2 * y * z)"},
			{ "3 * x / y / 2 * x / z", "3 * x * x / (2 * y * z)"},
			{ "3 * x / y / -2 * x / z", "-3 * x * x / (2 * y * z)"},
			{ "3 * x / y * 6 / -2 * x / z", "-9 * x * x / (y * z)"},
			{ "3 * x / (-2 * x * z) / z", "-3 / (2 * z * z)"},
			{ "3 * x / (-2 / x / y) / z", "-3 * x * x * y / (2 * z)"},
			{ "x / (-2 / x) / z", "-x * x / (2 * z)"},
			{ "a * b / 2", "a * b / 2"},
			{ "x / (1 / x)", "x * x"},
			{ "x / (1 / x / 2 / y)", "2 * x * x * y"},
			{ "x / (1 * x * 2 * y)", "1 / (2 * y)"},
			{ "1 / (x * 2 * y)", "1 / (2 * x * y)"},
			{ "1 / (x / 2 / y)", "2 * y / x"},
			{ "1 / (x / 2 / -y)", "-2 * y / x"},
			{ "1 / (-x / 2 / -y)", "2 * y / x"},
			{ "1 / (-x / -2 / -y)", "-2 * y / x"},
			{ "x / (x / 2 / -y)", "-2 * y"},
			{ "x / (x / -y)", "-y"},
			{ "x / (-x / y)", "-y"},
			{ "x * (2 / y)", "2 * x / y"},
			{ "4 * (2 / y)", "8 / y"},
			{ "x * (2 / y) + 4 * (2 / y)", "2 * x / y + 8 / y"},

			{ "1 / -x", "-1 / x"},
			{ "-1 / x", "-1 / x"},

			{ "3 / b * 2", "6 / b"},
			{ "a / -b", "-a / b"},
			{ "2 / x * y / x", "2 * y / (x * x)"},
			{ "2 / x * (3 / x) * (4 / x)", "24 / (x * x * x)"},
			{ "2 / x * (3 / x) * (-4 / x)", "-24 / (x * x * x)"},

			{ "(2 * x + x * z)/ x", "2 + z"},
			{ "(2 * x + x * -z)/ x", "2 - z"},
			{ "(2 * x + -x * z)/ x", "2 - z"},
			{ "(2 * x - x * z)/ x", "2 - z"},
			{ "(2 * x - -x * z)/ x", "2 + z"},
			{ "(2 * x - -x * z)/ x", "2 + z"},
			{ "(2 * x * y - -x * z * y + x * y) / x / y", "3 + z"},
			{ "(2 * x * y + -x * z * y + x * y) / x / y", "3 - z"},
			{ "(2 * x * y - -x * z * y + 2 * x * y) / x / y", "4 + z"},
			{ "(2 * x * y - 2 * -x * z * y + 2 * x * y) / x / y", "4 + 2 * z"},
			{ "(2 * x * y - 2 * -x * z * y + 2 * x * y) / y", "4 * x + 2 * x * z"},
			{ "(x * x * x - y * y * x * x + x * x) / x / x", "1 + x - y * y"},
			{ "(x * x * x - y * y * x * x + x * x) / (x * x)", "1 + x - y * y"},
			{ "(x * x * x - y * y * x * x + x * x) / x", "x + x * x - x * y * y"},
			{ "(x * a / y + x / y - x * x / y) / x", "a / y + 1 / y - x / y"},
			{ "a / y + 1 / y", "a / y + 1 / y"},
			{ "1 / y + a / y", "a / y + 1 / y"},
			{ "(x * a / y + x / -y - x * x / y) / x", "a / y  - x / y - 1 / y"},

			{ "x / (2 * x + x * z)", "1 / (2 + z)"},
			{ "x / (2 * x + x * -z)", "1 / (2 - z)"},
			{ "x / (2 * x + -x * z)", "1 / (2 - z)"},
			{ "x / (2 * x - x * z)", "1 / (2 - z)"},
			{ "x / (2 * x - -x * z)", "1/ (2 + z)"},
			{ "x / (2 * x - -x * z)", "1 / (2 + z)"},

			{ "(x * a / y + x / -y - x * x / y) / x", "a / y - x / y - 1 / y"},
			{ "2 * (x + y - z) + 3 - 2 * y", "3 + 2 * x - 2 * z"},
			{ "(x * a / y + x / -y - x * x / y) / x", "a / y - x / y - 1 / y"},
			{ "c * (d - b) / ((a - b) * 3)", "c * d / (3 * a - 3 * b) - b * c / (3 * a - 3 * b)"},

			{ "(a + b) * (a + b)", "a * a + 2 * a * b + b * b"},
			{ "(a + b) * (a - b)", "a * a - b * b"},
			{ "(a - b) * (a + b)", "a * a - b * b"},
			{ "(a + b) * (a + b) * (a + b)", "a * a * a + 3 * a * a * b + 3 * a * b * b + b * b * b"},
			{ "(a + 1) * (a + 1)", "1 + 2 * a + a * a"},
			{ "(a + 1) * (a - 1)", "-1 + a * a"},
			{ "(a - 1) * (a + 1)", "-1 + a * a"},
			{ "(a - 1) * (a - 1)", "1 + a * a - 2 * a"},
			{ "(x + y + z) * (x + y)", "x * x + 2 * x * y + x * z + y * y + y * z"},

			{"++a", "++a"},
			{"a[1]", "a[1]"},
			{"a.func()", "a.func()"},
			{"b.func() + a", "a + b.func()"},
			{"b.func() - a", "b.func() - a"},
			{"-(a - b)", "b - a"},
			{"-(a - b + 2) + x - 3", "-5 + b + x - a"},
			{"(-a-b)", "-a - b"},
			{"a + 1", "1 + a"},
			{"b.func() - a - 3", "-3 + b.func() - a"},
			{"a / b / c", "a / (b * c)"},
			{"2 / (b + a)", "2 / (a + b)"},
			{"2 * (b + a)", "2 * a + 2 * b"},
			{"++c + b.func() - a-- - 3", "-3 + ++c + b.func() - a--"},
			{"c * -2 / -(b - a)", "-2 * c / (a - b)"},

			// sorting - all plus
			{"a + b + c", "a + b + c"},
			{"a + c + b", "a + b + c"},
			{"b + a + c", "a + b + c"},
			{"b + c + a", "a + b + c"},
			{"c + a + b", "a + b + c"},
			{"c + b + a", "a + b + c"},

			// sorting - with negative terms
			{"-a + b + c", "b + c - a"},
			{"-a + c + b", "b + c - a"},
			{"a - b + c", "a + c - b"},
			{"a - c + b", "a + b - c"},
			{"a + b - c", "a + b - c"},
			{"a + c - b", "a + c - b"},
			{"-a - b + c", "c - a - b"},
			{"-a - c + b", "b - a - c"},
			{"-a + b - c", "b - a - c"},
			{"-a + c - b", "c - a - b"},

			{"-c + b + a", "a + b - c"},
			{"-c + a + b", "a + b - c"},
			{"c - b + a", "a + c - b"},
			{"c - a + b", "b + c - a"},
			{"c + b - a", "b + c - a"},
			{"c + a - b", "a + c - b"},
			{"-c - b + a", "a - b - c"},
			{"-c - a + b", "b - a - c"},
			{"-c + b - a", "b - a - c"},
			{"-c + a - b", "a - b - c"},

			// sorting - positive mult only
			{"a * b * c", "a * b * c"},
			{"a * c * b", "a * b * c"},
			{"b * a * c", "a * b * c"},
			{"b * c * a", "a * b * c"},
			{"c * a * b", "a * b * c"},
			{"c * b * a", "a * b * c"},

			{"2 * c * b * a", "2 * a * b * c"},
			{"c * 2 * b * a", "2 * a * b * c"},
			{"c * b * 2 * a", "2 * a * b * c"},
			{"c * b * a * 2", "2 * a * b * c"},

			// sorting - mult with negative terms
			{"-a * b * c", "-a * b * c"},
			{"a * -b * c", "-a * b * c"},
			{"a * b * -c", "-a * b * c"},
			{"-a * -b * c", "a * b * c"},
			{"-a * b * -c", "a * b * c"},
			{"a * -b * -c", "a * b * c"},
			{"-a * -b * -c", "-a * b * c"},

			{"-c * b * a", "-a * b * c"},
			{"c * -b * a", "-a * b * c"},
			{"c * b * -a", "-a * b * c"},
			{"-c * -b * a", "a * b * c"},
			{"-c * b * -a", "a * b * c"},
			{"c * -b * -a", "a * b * c"},
			{"-c * -b * -a", "-a * b * c"},

			{"2 * -c * -b * -a", "-2 * a * b * c"},
			{"-c * 2 * -b * -a", "-2 * a * b * c"},
			{"-c * -b * 2 * -a", "-2 * a * b * c"},
			{"-c * -b * -a * 2", "-2 * a * b * c"},
			{"-2 * -c * -b * -a", "2 * a * b * c"},
			{"-c * -2 * -b * -a", "2 * a * b * c"},
			{"-c * -b * -2 * -a", "2 * a * b * c"},
			{"-c * -b * -a * -2", "2 * a * b * c"},

			// sorting - plus mult terms
			{"a * b * c + b + c + a", "a + a * b * c + b + c"},
			{"a * c * b + b + c + a", "a + a * b * c + b + c"},
			{"b * a * c + b + c + a", "a + a * b * c + b + c"},
			{"b * c * a + b + c + a", "a + a * b * c + b + c"},
			{"b * a * c + b + c + a", "a + a * b * c + b + c"},
			{"c * a * b + b + c + a", "a + a * b * c + b + c"},
			{"c * b * a + b + c + a", "a + a * b * c + b + c"},
			{"a + a * b * c + c + b", "a + a * b * c + b + c"},
			{"a + b + a * b * c + c", "a + a * b * c + b + c"},

			{"2 * (c * b * a + b + c + a)", "2 * a + 2 * a * b * c + 2 * b + 2 * c"},

			// sorting - division
			{"a * b * c / d / e / f", "a * b * c / (d * e * f)"},
			{"a * c * b / d / f / e", "a * b * c / (d * e * f)"},
			{"b * a * c / e / d / f", "a * b * c / (d * e * f)"},
			{"b * c * a / e / f / d", "a * b * c / (d * e * f)"},
			{"c * a * b / f / d / e", "a * b * c / (d * e * f)"},
			{"c * b * a / f / e / d", "a * b * c / (d * e * f)"},

			{"-c * b * a / f / e / d", "-a * b * c / (d * e * f)"},
			{"c * -b * a / f / e / d", "-a * b * c / (d * e * f)"},
			{"c * b * -a / f / e / d", "-a * b * c / (d * e * f)"},
			{"c * b * a / -f / e / d", "-a * b * c / (d * e * f)"},
			{"c * b * a / f / -e / d", "-a * b * c / (d * e * f)"},
			{"c * b * a / f / e / -d", "-a * b * c / (d * e * f)"},

			{"f * e * d / c / b / a", "d * e * f / (a * b * c)"},
			{"c * -b * -a / f / -e / -d", "a * b * c / (d * e * f)"},
			{"c * -b * -a / -f / -e / -d", "-a * b * c / (d * e * f)"},

			{"c * b / a * f / e * d", "b * c * d * f/ (a * e)"},

			// ++ / --
			{"++a + a + b", "++a + a + b"},
			{"c + ++a + a + b", "++a + a + b + c"},
			{"c - ++a - a + b", "b + c - ++a - a"},
			{"--a + a + b", "--a + a + b"},
			{"c + --a + a + b", "--a + a + b + c"},
			{"c - --a - a + b", "b + c - --a - a"},
			{"++a * --a + --a * ++a", "2 * ++a * --a"},
			{"x * y + y * x", "2 * x * y"},

			{"2 * a + 3 * (d + a - 3)", "-9 + 5 * a + 3 * d"},
			{"x * y * (a + b) / x / (a + b)", "y"},
			{"x * (1 + y) / (1 + y)", "x"},
			{"x * y / y", "x"},
			{"x * y * (a + b) / (x * (a + b))", "y"},
			{"2 * x / 3", " 2 * x / 3"},
			{"x * (1 / (x + y))", "x / (x + y)"},
			{"1 / x * (1 / (x + y))", "1 / (x * x + x * y)"},
		});
	}

	private Expression e = null;
	private Expression reduced = null;

	public TestReduce(final String e, final String reduced) {
		this.e = parseexpression(e);
		this.reduced = parseexpression(reduced);
	}

	@Test
	public void testReduce() throws Exception {
		assertEquals(reduced, BoaNormalFormIntrinsics.reduce(e));
	}
}