package javadoc.testBug51363;
public class X {
	public void foo() {
		final String happy = "string is a happy place" + "string is a happy place" + "string is a happy place"; //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
	}
}