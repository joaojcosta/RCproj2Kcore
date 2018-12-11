/*		 
 * Copyright (C) 2011-2013 Sebastiano Vigna 
 * Copyright (C) 2013 A P Francisco
 * Copyright (C) 2018 João Costa, ist427055
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses/>.
 *
 */

import it.unimi.dsi.Util;
import it.unimi.dsi.fastutil.ints.AbstractIntComparator;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;
import it.unimi.dsi.webgraph.NodeIterator;

//import it.unimi.dsi.big.webgraph.Edge;
//import it.unimi.dsi.big.webgraph.MutableGraph;
import it.unimi.dsi.logging.ProgressLogger;
import javafx.util.Pair;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.*;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.UnflaggedOption;

/**
  * Compute the algorithm for finding k-cores in linear time on top of Webgraph
  */
public class KCoreAlgorithm {
	private static final Logger LOGGER = LoggerFactory.getLogger( KCoreAlgorithm.class );

	/** The number of connected components. */
	public final int numberOfComponents;

	/** The component of each node. */
	public final int component[];

	protected KCoreAlgorithm( final int numberOfComponents, final int[] component ) {
		this.numberOfComponents = numberOfComponents;
		this.component = component;
    }

	/**
	 * Computes the diameter of a symmetric graph.
	 * 
	 * @param symGraph a symmetric graph.
	 * @param pl a progress logger, or <code>null</code>.
	 * @return an instance of this class containing the computed components.
	 */
	public static KCoreAlgorithm compute( final ImmutableGraph g, final ProgressLogger pl ) {

		LOGGER.info("Computing ...");
		pl.start("Processing V = " + g.numNodes() + ", E = " + g.numArcs());
		pl.expectedUpdates = g.numNodes();
		pl.itemsName = "nodes";

		//---------------  Implementacao do algoritmo k-core ------------------
		//--------- The Cores Algorithm for Simple Undirected Graphs ----------
		int n = g.numNodes(), d, md = -1, i, num;
		int v, u, w, du, pu, pw, start;
		int[] //array temps
		vert = new int[n], //tableVert
		pos  = new int[n], //tableVert
		deg  = new int[n], //tableVert
		bin  = new int[n]; //tableDeg
	
		//The author begin v2 with 1, because they assume that vertex is from 1 to n
		//In Webgraph the begin in 0 to n-1. 

		/**
		* deg[v], indice do deg é o vertice e o valor é grau
		* md fica com o maior grau.
		*/
		//int DEBUG = 100;  
		for(v = 0; v < n; v++){
			
			LazyIntIterator successors = g.successors(v);
			
			if((d = g.outdegree(v)) > md) { md = d; }

			deg[v] = d;
			
			//for v := 1 to n do inc(bin[deg[v]]);
			bin[d] = bin[d] + 1;
			
			pl.update();
		//	System.out.println("[DEBUG] Vertex " + v2 + " : " + d2 + " - " + md2);
		//	if(--DEBUG == 0){ break;}
		}

		//start := 1; 
		//for d:=0 to md do begin
		//	num := bin[d];
		//	bin[d] := start;
		//	inc(start, num);
		//end
		for(d = 0, start = 1; d < md; d++){
			num = bin[d];
			bin[d] = start;  // There are any danger here
			start += num; 
			pl.update();
		}
		//for v:=1 to n do begin
		//	pos[v] := bin[deg[v]];
		//	vert[pos[v]] := v;
		//	inc(bin[deg[v]]);
		//end;
		for(v = 0; v < n; v++){
			pos[v] = bin[deg[v]];
			vert[pos[v]] = v;
			++bin[deg[v]];
			pl.update();
		}
		//for d := md downto 1 do
		//	bin[d] := bin[d-1];
		for(d = md - 1; d > 0; d--){
			bin[d] = bin[d-1];
			pl.update();
		}
		//bin[0] := 1;	
		//bin2[0] = 1;	

		//for i :=1 to n do begin
		//	v := vert[i];
		//	for u in Neighbours(v) do begin
		//		if deg[u] > deg[v] then begin
		//			du := deg[u];
		//			pu := pos[u];
		//			pw := bin[du];
		//			w := vert[pw];
		//			if u <> w then begin
		//				pos[u] := pw;
		//				pos[w] := pu;
		//				vert[pu] := w;
		//				vert[pw] := u;
		//			end
		//			inc(bin[du]);
		//			dec(deg[u]);
		//		end
		//	end
		//end
		bin[0] = 1;
		for(i = 0; i < n; i++ ){
			v = vert[i];
			LazyIntIterator successors = g.successors(v);
			while((u = successors.nextInt()) != -1) {
				//System.out.println("[DEBUR] u2 : " + u2);
				if(deg[u] > deg[v]){
					du = deg[u];
					pu = pos[u];
					pw = bin[du];
					//System.out.println("[DEBUR] pw2 : " + pw2);
					pw = (pw > n - 1) ? n - 1: pw; // ArrayIndexOutOfBoundsException
					w = vert[pw]; 
					if(u != w){
						pos[u] = pw;
						pos[w] = pu;
						vert[pu] = w;
						vert[pw] = u;
					}
					bin[du]++;
					deg[u]++;
				}
				pl.update();
			}
		}

		pl.done();

		return new KCoreAlgorithm( n, vert);
	}

	/**
	 * Returns the size array for this set of connected components.
	 * 
	 * @return the size array for this set of connected components.
	 */
	public int[] computeSizes() {
		final int[] size = new int[ numberOfComponents ];
		for ( int i = component.length; i-- != 0; )
			size[ component[ i ] ]++;
		return size;
	}

	/**
	 * Renumbers by decreasing size the components of this set.
	 * 
	 * <p>After a call to this method, both the internal status of this class and the argument array
	 * are permuted so that the sizes of connected components are decreasing in the component index.
	 * 
	 * @param size the components sizes, as returned by {@link #computeSizes()}.
	 */
	public void sortBySize( final int[] size ) {
		final int[] perm = Util.identity( size.length );
		IntArrays.quickSort( perm, 0, perm.length, new AbstractIntComparator() {
			public int compare( final int x, final int y ) {
				return size[ y ] - size[ x ];
			}
		} );
		final int[] copy = size.clone();
		for ( int i = size.length; i-- != 0; )
			size[ i ] = copy[ perm[ i ] ];
		Util.invertPermutationInPlace( perm );
		for ( int i = component.length; i-- != 0; )
			component[ i ] = perm[ component[ i ] ];
	}
	
	/**
	 * Shows all neighbourhoods of a determined vertex
	 * @param g a symmetric graph.
	 * @param vertex 
	 */
	public void showNeighbourhoods(final ImmutableGraph g, int vertex){

		LazyIntIterator successors = g.successors(vertex);
		int n;

		System.out.print("Vertex " + vertex + " : [ ");

 		while((n = successors.nextInt()) != -1) {
			System.out.print(n + " # ");
		}

 		System.out.println("]");
	}
    


	public static void main( String arg[] ) throws IOException, JSAPException, UnsupportedOperationException {
		SimpleJSAP jsap = new SimpleJSAP( KCoreAlgorithm.class.getName(),
				"Computes the connected components of a symmetric graph of given basename. The resulting data is saved " +
						"in files stemmed from the given basename with extension .scc (a list of binary integers specifying the " +
						"component of each node) and .sccsizes (a list of binary integer specifying the size of each component).",
				new Parameter[] {
						new Switch( "sizes", 's', "sizes", "Compute component sizes." ),
						new Switch( "renumber", 'r', "renumber", "Renumber components in decreasing-size order." ),
						new FlaggedOption( "logInterval", JSAP.LONG_PARSER, Long.toString( ProgressLogger.DEFAULT_LOG_INTERVAL ), JSAP.NOT_REQUIRED, 'l', "log-interval",
								"The minimum time interval between activity logs in milliseconds." ),
						new UnflaggedOption( "basename", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY, "The basename of the graph." ),
						new UnflaggedOption( "resultsBasename", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, JSAP.NOT_GREEDY, "The basename of the resulting files." ),
		}
				);

		JSAPResult jsapResult = jsap.parse( arg );
		if ( jsap.messagePrinted() ) System.exit( 1 );

		final String basename = jsapResult.getString( "basename" );
		final String resultsBasename = jsapResult.getString( "resultsBasename", basename );
		ProgressLogger pl = new ProgressLogger( LOGGER, jsapResult.getLong( "logInterval" ), TimeUnit.MILLISECONDS );

		//final KCoreAlgorithm components = KCoreAlgorithm.compute( ImmutableGraph.loadOffline( basename ), pl );
		
		final KCoreAlgorithm components = KCoreAlgorithm.compute( 
			//ImmutableGraph load(CharSequence);
			ImmutableGraph.load( basename ), 
			pl );

		if ( jsapResult.getBoolean( "sizes" ) || jsapResult.getBoolean( "renumber" ) ) {
			
			final int size[] = components.computeSizes();
			if ( jsapResult.getBoolean( "renumber" ) ) components.sortBySize( size );
			if ( jsapResult.getBoolean( "sizes" ) ) BinIO.storeInts( size, resultsBasename + ".sccsizes" );
		}
		BinIO.storeInts( components.component, resultsBasename + ".scc" );
	}
}