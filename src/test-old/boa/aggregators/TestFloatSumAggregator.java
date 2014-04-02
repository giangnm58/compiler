package boa.aggregators;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mrunit.mapreduce.ReduceDriver;
import org.junit.Test;

import boa.io.EmitKey;
import boa.io.EmitValue;

public class TestFloatSumAggregator {
	@Test
	public void testFloatSumAggregatorFloatsCombine() {
		final List<EmitValue> values = new ArrayList<EmitValue>();
		values.add(new EmitValue("1"));
		values.add(new EmitValue("2.0"));
		values.add(new EmitValue(3));
		values.add(new EmitValue(4.0));

		new ReduceDriver<EmitKey, EmitValue, EmitKey, EmitValue>(new FloatSumBoaCombiner()).withInput(new EmitKey("test", 0), values)
				.withOutput(new EmitKey("test", 0), new EmitValue("10.0")).runTest();
	}

	@Test
	public void testFloatSumAggregatorFloatsReduce() {
		final List<EmitValue> values = new ArrayList<EmitValue>();
		values.add(new EmitValue("1"));
		values.add(new EmitValue("2.0"));
		values.add(new EmitValue(3));
		values.add(new EmitValue(4.0));

		new ReduceDriver<EmitKey, EmitValue, Text, NullWritable>(new FloatSumBoaReducer()).withInput(new EmitKey("test", 0), values)
				.withOutput(new Text("test[] = 10.0"), NullWritable.get()).runTest();
	}
}

class FloatSumBoaCombiner extends boa.runtime.BoaCombiner {
	public FloatSumBoaCombiner() {
		super();

		this.tables.put("test", new Table(new boa.aggregators.FloatSumAggregator()));
	}
}

class FloatSumBoaReducer extends boa.runtime.BoaReducer {
	public FloatSumBoaReducer() {
		super();

		this.tables.put("test", new Table(new boa.aggregators.FloatSumAggregator()));
	}
}