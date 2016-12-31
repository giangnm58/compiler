/*
 * Copyright 2015, Anthony Urso, Hridesh Rajan, Robert Dyer,
 *                 and Iowa State University of Science and Technology
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
package boa.aggregators;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer.Context;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import boa.BoaTup;
import boa.functions.BoaCasts;
import boa.io.EmitKey;
import boa.io.EmitValue;

/**
 * The base class for all Boa aggregators.
 * 
 * @author anthonyu
 * @author rdyer
 * @author ankuraga
 */
public abstract class Aggregator {
	private long arg;
	private String mlarg;
	@SuppressWarnings("rawtypes")
	private Context context;
	private EmitKey key;
	private boolean combining;
	private int vectorSize;
	private final static Set<String> inBuiltAggs = new HashSet<String>();

	static {
		inBuiltAggs.add("sum");
		inBuiltAggs.add("top");
		inBuiltAggs.add("max");
		inBuiltAggs.add("min");
		inBuiltAggs.add("collection");
		inBuiltAggs.add("mean");
		inBuiltAggs.add("stDev");
		inBuiltAggs.add("quantile");
		inBuiltAggs.add("kurtosis");
		inBuiltAggs.add("histogram");
		inBuiltAggs.add("graphCSV");
	}

	/**
	 * Construct an Aggregator.
	 * 
	 */
	public Aggregator() {
		// default constructor
	}

	/**
	 * Construct an Aggregator.
	 * 
	 * @param arg
	 *            A long (Boa int) containing the argument to the table
	 * 
	 */
	public Aggregator(final long arg) {
		this();

		this.arg = arg;
	}

	/**
	 * Construct an Aggregator.
	 *
	 * @param arg
	 *            A String containing the argument to the table
	 *
	 */
	public Aggregator(final String arg) {
		this();
		
		this.mlarg = arg;
	}

	/**
	 * Reset this aggregator for a new key.
	 * 
	 * @param key
	 *            The {@link EmitKey} to aggregate for
	 * 
	 */
	public void start(final EmitKey key) {
		this.setKey(key);
	}

	public abstract void aggregate(String data, String metadata) throws IOException, InterruptedException, FinishedException;

	public void aggregate(final String data) throws IOException, InterruptedException, FinishedException {
		this.aggregate(data, null);
	}

	public void aggregate(final long data, final String metadata) throws IOException, InterruptedException, FinishedException {
		this.aggregate(BoaCasts.longToString(data), metadata);
	}

	public void aggregate(final long data) throws IOException, InterruptedException, FinishedException {
		this.aggregate(BoaCasts.longToString(data), null);
	}

	public void aggregate(final double data, final String metadata) throws IOException, InterruptedException, FinishedException {
		this.aggregate(BoaCasts.doubleToString(data), metadata);
	}

	public void aggregate(final double data) throws IOException, InterruptedException, FinishedException {
		this.aggregate(BoaCasts.doubleToString(data), null);
	}

	public void aggregate(final BoaTup data, final String metadata) throws IOException, InterruptedException, FinishedException, IllegalAccessException {	
	}

	public void aggregate(final BoaTup data) throws IOException, InterruptedException, FinishedException, IllegalAccessException {
		this.aggregate(data, null);
	}

	@SuppressWarnings("unchecked")
	protected void collect(final String data, final String metadata) throws IOException, InterruptedException {
		if (this.combining)
			this.getContext().write(this.getKey(), new EmitValue(data, metadata));
		else if (metadata != null)
			this.getContext().write(new Text(this.getKey() + " = " + data + " weight " + metadata), NullWritable.get());
		else
			this.getContext().write(new Text(this.getKey() + " = " + data), NullWritable.get());
	}

	protected void collect(final String data) throws IOException, InterruptedException {
		this.collect(data, null);
	}

	protected void collect(final Object data) throws IOException, InterruptedException {
		this.collect(data.toString(), null);
	}

	@SuppressWarnings("unchecked")
	protected void collect(final long data, final String metadata) throws IOException, InterruptedException {
		this.collect(BoaCasts.longToString(data), metadata);
	}

	protected void collect(final long data) throws IOException, InterruptedException {
		this.collect(BoaCasts.longToString(data), null);
	}

	@SuppressWarnings("unchecked")
	protected void collect(final double data, final String metadata) throws IOException, InterruptedException {
		this.collect(BoaCasts.doubleToString(data), metadata);
	}

	protected void collect(final double data) throws IOException, InterruptedException {
		this.collect(BoaCasts.doubleToString(data), null);
	}

	public void finish() throws IOException, InterruptedException {
		// do nothing by default
	}

	public long getArg() {
		return this.arg;
	}

	public void setContext(@SuppressWarnings("rawtypes") final Context context) {
		this.context = context;
	}

	public boolean isCombining() {
		return this.combining;
	}

	public void setCombining(final boolean combining) {
		this.combining = combining;
	}

	@SuppressWarnings("rawtypes")
	public Context getContext() {
		return this.context;
	}

	public void setKey(final EmitKey key) {
		this.key = key;
	}

	public EmitKey getKey() {
		return this.key;
	}
	
	public int getVectorSize() {
		return this.vectorSize;
	}

	public void setVectorSize(int vectorSize) {
		this.vectorSize = vectorSize;
	}

	public final static boolean isUserDefinedAggregator(String name) {
		return !Aggregator.inBuiltAggs.contains(name);
	}
}
