/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.tests.compiler.regression;

import java.util.Map;

import junit.framework.Test;

import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;

public class InnerClass15Test extends AbstractRegressionTest {
public InnerClass15Test(String name) {
	super(name);
}
static {
//	TESTS_NUMBERS = new int[] { 2 };
}
public static Test suite() {
	return buildMinimalComplianceTestSuite(testClass(), F_1_5);
}
protected Map getCompilerOptions() {
	Map options = super.getCompilerOptions();
	options.put(CompilerOptions.OPTION_ReportRawTypeReference, CompilerOptions.IGNORE);
	return options;
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=312989
public void test001() {
	this.runNegativeTest(new String[] {
		"X.java",
		"class X {\n" + 
		"	<X> void foo() {\n" + 
		"		class X {}\n" + 
		"	}\n" + 
		"}",
	},
	"----------\n" + 
	"1. WARNING in X.java (at line 2)\n" + 
	"	<X> void foo() {\n" + 
	"	 ^\n" + 
	"The type parameter X is hiding the type X\n" + 
	"----------\n" + 
	"2. WARNING in X.java (at line 3)\n" + 
	"	class X {}\n" + 
	"	      ^\n" + 
	"The nested type X is hiding the type parameter X of the generic method foo() of type X\n" + 
	"----------\n" + 
	"3. ERROR in X.java (at line 3)\n" + 
	"	class X {}\n" + 
	"	      ^\n" + 
	"The nested type X cannot hide an enclosing type\n" + 
	"----------\n");
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=312989
public void test002() {
	this.runNegativeTest(new String[] {
		"X.java",
		"class X<X> {\n" + 
		"	void foo() {\n" + 
		"		class X {}\n" + 
		"	}\n" + 
		"}",
	},
	"----------\n" + 
	"1. WARNING in X.java (at line 1)\n" + 
	"	class X<X> {\n" + 
	"	        ^\n" + 
	"The type parameter X is hiding the type X<X>\n" + 
	"----------\n" + 
	"2. WARNING in X.java (at line 3)\n" + 
	"	class X {}\n" + 
	"	      ^\n" + 
	"The nested type X is hiding the type parameter X of type X<X>\n" + 
	"----------\n" + 
	"3. ERROR in X.java (at line 3)\n" + 
	"	class X {}\n" + 
	"	      ^\n" + 
	"The nested type X cannot hide an enclosing type\n" + 
	"----------\n");
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=312989
// note javac reports an error for this test, but that is
// incorrect, compare and contrast javac behavior with
// test004.
public void test003() {
	this.runNegativeTest(new String[] {
		"Y.java",
		"class Y {\n" +
		"class X {}\n" + 
		"	<X> void foo() {\n" + 
		"		class X {}\n" + 
		"	}\n" + 
		"}",
	},
	"----------\n" + 
	"1. WARNING in Y.java (at line 3)\n" + 
	"	<X> void foo() {\n" + 
	"	 ^\n" + 
	"The type parameter X is hiding the type Y.X\n" + 
	"----------\n" + 
	"2. WARNING in Y.java (at line 4)\n" + 
	"	class X {}\n" + 
	"	      ^\n" + 
	"The nested type X is hiding the type parameter X of the generic method foo() of type Y\n" + 
	"----------\n" + 
	"3. WARNING in Y.java (at line 4)\n" + 
	"	class X {}\n" + 
	"	      ^\n" + 
	"The type X is never used locally\n" + 
	"----------\n");
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=312989
public void test004() {
	this.runNegativeTest(new String[] {
		"Y.java",
		"class Y {\n" +
		"class X {}\n" + 
		"   void foo() {\n" + 
		"		class X {}\n" + 
		"	}\n" + 
		"}",
	},
	"----------\n" + 
	"1. WARNING in Y.java (at line 4)\n" + 
	"	class X {}\n" + 
	"	      ^\n" + 
	"The type X is hiding the type Y.X\n" + 
	"----------\n" + 
	"2. WARNING in Y.java (at line 4)\n" + 
	"	class X {}\n" + 
	"	      ^\n" + 
	"The type X is never used locally\n" + 
	"----------\n");
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=319885
public void test005() {
	this.runNegativeTest(new String[] {
		"p1/GreenBox.java",
		"package p1;\n" +
		"import static p1.BrownBox.*;\n" +
		"public interface GreenBox {\n" +
		"    public static class Cat extends Object {}\n" +
		"}\n",
		"p1/BrownBox.java",
		"package p1;\n" +
		"import static p1.GreenBox.*;\n" +
		"public interface BrownBox {\n" +
		"    public static class BlackCat extends Cat {}\n" +
		"}\n",
	},
	"----------\n" + 
	"1. WARNING in p1\\GreenBox.java (at line 2)\n" + 
	"	import static p1.BrownBox.*;\n" + 
	"	              ^^^^^^^^^^^\n" + 
	"The import p1.BrownBox is never used\n" + 
	"----------\n");
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=319885
public void test006() {
	this.runNegativeTest(new String[] {
		"p1/BrownBox.java",
		"package p1;\n" +
		"import static p1.GreenBox.*;\n" +
		"public interface BrownBox {\n" +
		"    public static class BlackCat extends Cat {}\n" +
		"}\n",
		"p1/GreenBox.java",
		"package p1;\n" +
		"import static p1.BrownBox.*;\n" +
		"public interface GreenBox {\n" +
		"    public static class Cat extends Object {}\n" +
		"}\n",
	},
	"----------\n" + 
	"1. WARNING in p1\\GreenBox.java (at line 2)\n" + 
	"	import static p1.BrownBox.*;\n" + 
	"	              ^^^^^^^^^^^\n" + 
	"The import p1.BrownBox is never used\n" + 
	"----------\n");
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=319885
public void test007() {
	this.runNegativeTest(new String[] {
		"p1/BrownBox.java",
		"package p1;\n" +
		"import static p1.GreenBox.*;\n" +
		"public interface BrownBox {\n" +
		"    public static class BlackCat extends Cat {}\n" +
		"}\n",
		"p1/GreenBox.java",
		"package p1;\n" +
		"import static p1.BrownBox.*;\n" +
		"public interface GreenBox {\n" +
		"    public static class Cat extends java.lang.Object {}\n" +
		"}\n",
	},
	"----------\n" + 
	"1. WARNING in p1\\GreenBox.java (at line 2)\n" + 
	"	import static p1.BrownBox.*;\n" + 
	"	              ^^^^^^^^^^^\n" + 
	"The import p1.BrownBox is never used\n" + 
	"----------\n");
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=319885
public void test008() {
	this.runNegativeTest(new String[] {
		"p1/BrownBox.java",
		"package p1;\n" +
		"import static p1.GreenBox.*;\n" +
		"public interface BrownBox {\n" +
		"    public static class BlackCat extends Cat {}\n" +
		"}\n",
		"p1/GreenBox.java",
		"package p1;\n" +
		"import static p1.BrownBox.*;\n" +
		"public interface GreenBox {\n" +
		"    public static class Cat extends BlackCat {}\n" +
		"}\n",
	},
	"----------\n" + 
	"1. ERROR in p1\\BrownBox.java (at line 4)\n" + 
	"	public static class BlackCat extends Cat {}\n" + 
	"	                    ^^^^^^^^\n" + 
	"The hierarchy of the type BlackCat is inconsistent\n" + 
	"----------\n" + 
	"----------\n" + 
	"1. ERROR in p1\\GreenBox.java (at line 4)\n" + 
	"	public static class Cat extends BlackCat {}\n" + 
	"	                                ^^^^^^^^\n" + 
	"Cycle detected: a cycle exists in the type hierarchy between GreenBox.Cat and BrownBox.BlackCat\n" + 
	"----------\n");
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=319885
public void test009() {
	this.runNegativeTest(new String[] {
		"p1/GreenBox.java",
		"package p1;\n" +
		"import static p1.BrownBox.*;\n" +
		"public interface GreenBox {\n" +
		"    public static class Cat extends BlackCat {}\n" +
		"}\n",
		"p1/BrownBox.java",
		"package p1;\n" +
		"import static p1.GreenBox.*;\n" +
		"public interface BrownBox {\n" +
		"    public static class BlackCat extends Cat {}\n" +
		"}\n",
	},
	"----------\n" + 
	"1. ERROR in p1\\GreenBox.java (at line 4)\n" + 
	"	public static class Cat extends BlackCat {}\n" + 
	"	                    ^^^\n" + 
	"The hierarchy of the type Cat is inconsistent\n" + 
	"----------\n" + 
	"----------\n" + 
	"1. ERROR in p1\\BrownBox.java (at line 4)\n" + 
	"	public static class BlackCat extends Cat {}\n" + 
	"	                                     ^^^\n" + 
	"Cycle detected: a cycle exists in the type hierarchy between BrownBox.BlackCat and GreenBox.Cat\n" + 
	"----------\n");
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=319885
public void test0010() {
	this.runNegativeTest(new String[] {
		"p1/GreenBox.java",
		"package p1;\n" +
		"import static p1.BrownBox.*;\n" +
		"interface SuperInterface {\n" +
		"   public static class Cat extends BlackCat {}\n" +
		"}\n" +
		"public interface GreenBox {\n" +
		"}\n",
		"p1/BrownBox.java",
		"package p1;\n" +
		"import static p1.GreenBox.*;\n" +
		"public interface BrownBox {\n" +
		"    public static class BlackCat extends Cat {}\n" +
		"}\n",
	},
	"----------\n" + 
	"1. ERROR in p1\\GreenBox.java (at line 4)\n" + 
	"	public static class Cat extends BlackCat {}\n" + 
	"	                    ^^^\n" + 
	"The hierarchy of the type Cat is inconsistent\n" + 
	"----------\n" + 
	"----------\n" + 
	"1. ERROR in p1\\BrownBox.java (at line 4)\n" + 
	"	public static class BlackCat extends Cat {}\n" + 
	"	                                     ^^^\n" + 
	"Cat cannot be resolved to a type\n" + 
	"----------\n");
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=319885
public void test0011() {
	this.runNegativeTest(new String[] {
		"p1/GreenBox.java",
		"package p1;\n" +
		"import static p1.BrownBox.*;\n" +
		"interface SuperInterface {\n" +
		"   public static class Cat extends BlackCat {}\n" +
		"}\n" +
		"public interface GreenBox extends SuperInterface {\n" +
		"}\n",
		"p1/BrownBox.java",
		"package p1;\n" +
		"import static p1.GreenBox.*;\n" +
		"public interface BrownBox {\n" +
		"    public static class BlackCat extends Cat {}\n" +
		"}\n",
	},
	"----------\n" + 
	"1. ERROR in p1\\GreenBox.java (at line 4)\n" + 
	"	public static class Cat extends BlackCat {}\n" + 
	"	                    ^^^\n" + 
	"The hierarchy of the type Cat is inconsistent\n" + 
	"----------\n" + 
	"----------\n" + 
	"1. ERROR in p1\\BrownBox.java (at line 4)\n" + 
	"	public static class BlackCat extends Cat {}\n" + 
	"	                                     ^^^\n" + 
	"Cycle detected: a cycle exists in the type hierarchy between BrownBox.BlackCat and SuperInterface.Cat\n" + 
	"----------\n");
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=319885
public void test0012() {
	this.runNegativeTest(new String[] {
		"p1/GreenBox.java",
		"package p1;\n" +
		"import static p1.BrownBox.*;\n" +
		"interface SuperInterface {\n" +
		"   public static class Cat extends BlackCat {}\n" +
		"}\n" +
		"public interface GreenBox extends SuperInterface {\n" +
		"}\n",
		"p1/BrownBox.java",
		"package p1;\n" +
		"import static p1.GreenBox.*;\n" +
		"public interface BrownBox {\n" +
		"    public static class BlackCat extends GreenBox.Cat {}\n" +
		"}\n",
	},
	"----------\n" + 
	"1. ERROR in p1\\GreenBox.java (at line 4)\n" + 
	"	public static class Cat extends BlackCat {}\n" + 
	"	                    ^^^\n" + 
	"The hierarchy of the type Cat is inconsistent\n" + 
	"----------\n" + 
	"----------\n" + 
	"1. ERROR in p1\\BrownBox.java (at line 4)\n" + 
	"	public static class BlackCat extends GreenBox.Cat {}\n" + 
	"	                                     ^^^^^^^^^^^^\n" + 
	"Cycle detected: a cycle exists in the type hierarchy between BrownBox.BlackCat and SuperInterface.Cat\n" + 
	"----------\n");
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=319885
public void test0013() {
	this.runNegativeTest(new String[] {
		"cycle/X.java",
		"package cycle;\n" +
		"class X extends Y {}\n" +
		"class Y extends X {}\n",
	},
	"----------\n" + 
	"1. ERROR in cycle\\X.java (at line 2)\n" + 
	"	class X extends Y {}\n" + 
	"	      ^\n" + 
	"The hierarchy of the type X is inconsistent\n" + 
	"----------\n" + 
	"2. ERROR in cycle\\X.java (at line 3)\n" + 
	"	class Y extends X {}\n" + 
	"	                ^\n" + 
	"Cycle detected: a cycle exists in the type hierarchy between Y and X\n" + 
	"----------\n");
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=319885
public void test0014() {
	this.runNegativeTest(new String[] {
		"cycle/X.java",
		"package cycle;\n" +
		"class X extends Y {}\n" +
		"class Y extends Z {}\n" +
		"class Z extends A {}\n" +
		"class A extends B {}\n" +
		"class B extends C {}\n" +
		"class C extends X {}\n"
	},
	"----------\n" + 
	"1. ERROR in cycle\\X.java (at line 2)\n" + 
	"	class X extends Y {}\n" + 
	"	      ^\n" + 
	"The hierarchy of the type X is inconsistent\n" + 
	"----------\n" + 
	"2. ERROR in cycle\\X.java (at line 3)\n" + 
	"	class Y extends Z {}\n" + 
	"	      ^\n" + 
	"The hierarchy of the type Y is inconsistent\n" + 
	"----------\n" + 
	"3. ERROR in cycle\\X.java (at line 4)\n" + 
	"	class Z extends A {}\n" + 
	"	      ^\n" + 
	"The hierarchy of the type Z is inconsistent\n" + 
	"----------\n" + 
	"4. ERROR in cycle\\X.java (at line 5)\n" + 
	"	class A extends B {}\n" + 
	"	      ^\n" + 
	"The hierarchy of the type A is inconsistent\n" + 
	"----------\n" + 
	"5. ERROR in cycle\\X.java (at line 6)\n" + 
	"	class B extends C {}\n" + 
	"	      ^\n" + 
	"The hierarchy of the type B is inconsistent\n" + 
	"----------\n" + 
	"6. ERROR in cycle\\X.java (at line 7)\n" + 
	"	class C extends X {}\n" + 
	"	                ^\n" + 
	"Cycle detected: a cycle exists in the type hierarchy between C and X\n" + 
	"----------\n");
}
public static Class testClass() {
	return InnerClass15Test.class;
}
}
