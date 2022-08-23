package de.hechler.patrick.codesprachen.simple.compile;

import de.hechler.patrick.zeugs.check.objects.BigCheckResult;
import de.hechler.patrick.zeugs.check.objects.BigChecker;

public class Test {

	public void testname() throws Exception {
		main(new String[0]);
	}

	public static void main(String[] args) {
		System.out.println(System.getProperty("java.version"));
		BigCheckResult check = BigChecker.tryCheckAll(true, Test.class.getPackage(), Test.class.getClassLoader());
		check.print();
		if (check.wentUnexpected()) {
			check.detailedPrint(System.out);
			throw new Error(check.toString());
		}
	}

}