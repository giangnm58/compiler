package boa.test.datagen;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import boa.datagen.util.FileIO;
import boa.evaluator.BoaEvaluator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;


public class TestQueries {

	@Test
	public void testBugFix() {
		String[] args = { "-i", "/Users/roberts/git/compiler/test/known-good/bug-fix.boa", 
				"-d", "/Users/roberts/git/compiler/test/datagen/test_datagen",
				"-o", "/Users/roberts/git/compiler/test/datagen/Bug-fix_output" };
		BoaEvaluator.main(args);
		String expected = "AddedNullCheck[] = 1\n";
		File outputDir = new File("/Users/roberts/git/compiler/test/datagen/Bug-fix_output");
		String actual = getResults(outputDir);// evaluator.getResults();
		try {
			FileUtils.deleteDirectory(outputDir);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertEquals(expected, actual);
	}
	
	@Test
	public void testq20() {
		String[] args = { "-i", "/Users/roberts/git/compiler/test/known-good/q20.boa", 
				"-d","/Users/roberts/git/compiler/test/datagen/test_datagen", 
				"-o","/Users/roberts/git/compiler/test/datagen/q20_output" };
		BoaEvaluator.main(args);
		String expected = "TransientMax[] = 140492550, 3.0\n"
				+ "TransientMean[] = 0.0410958904109589\n"
				+ "TransientMin[] = 140492550, 0.0\n"
				+ "TransientTotal[] = 3\n"
				+ "VolatileMax[] = 140492550, 1.0\n"
				+ "VolatileMean[] = 0.0136986301369863\n"
				+ "VolatileMin[] = 140492550, 0.0\n"
				+ "VolatileTotal[] = 1\n";
		File outputDir = new File("/Users/roberts/git/compiler/test/datagen/q20_output");
		String actual = getResults(outputDir);// evaluator.getResults();
		try {
			FileUtils.deleteDirectory(outputDir);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertEquals(expected, actual);
	}

	public String getResults(File outputDir) {
		for (final File f : outputDir.listFiles()) {
			if (f.getName().startsWith("part")) {
				return FileIO.readFileContents(f);
			}
		}
		return "";
	}
}
