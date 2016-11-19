package lucene;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

/**
 * create lucene index
 * @author Luigi
 *
 */
public class Index2 extends Index{
	/**
	 * field name
	 */
	private static final String[] SEPARATORS = { ".I", ".T", ".A", ".B", ".W" };
	private static final String[] FIELDS = { "identifier number", "title", "author", 
			"affiliation or department", "abstract" };

	IndexSearcher indexSearcher;

	/**
	 * initialize the index
	 * @param directoryWithIndexedFile
	 * @throws IOException
	 */
	public Index2( Path directoryWithIndexedFile ) throws IOException
	{
		super( directoryWithIndexedFile );

	}

	/**
	 * create index collection
	 * @param directoryToIndex
	 * @param flag
	 * @return false if directoryToIndex is not a directory, true otherwise
	 * @throws IOException
	 */
	public boolean indexCollection( File fileToIndex ) throws IOException
	{
		if( ! fileToIndex.isFile() ){
			return false;
		}

		Scanner input = new Scanner( fileToIndex );
		input.useDelimiter( SEPARATORS[ 0 ] );

		while( input.hasNext() ){
			writer.addDocument( indexFile( input.next() ) );
		}

		input.close();

		return true;
	}

	/**
	 * index a single file
	 * @param temp1
	 * @param flag
	 * @return
	 */
	public Document indexFile( String temp1 )
	{
		Document doc = new Document();
		FieldType fieldType = getFieldType();

		String[] text = getTextFromFile( temp1 );
		for( int i = 0; i < FIELDS.length; i++ ){
			Field field = new Field( FIELDS[ i ], text[ i ].replaceAll("\n", ""), fieldType );
			doc.add( field );
		}

		return doc;
	}

	/**
	 * 
	 * @param temp1
	 * @return
	 */
	public String[] getTextFromFile( String temp1 )
	{
		String[] text = new String[ 5 ];
		String[] temp2 = null;

		int i;
		for( i = 1; i < SEPARATORS.length; i++ ){
			temp2 = temp1.split( SEPARATORS[ i ] );
			text[ i - 1 ] = temp2[ 0 ];
			temp1 = temp2[ temp2.length - 1 ];
		}
		text[ i - 1 ] = temp1;


		return text;
	}

	/**
	 * parse cran.qry file into multi queries
	 * @param file
	 * @throws IOException 
	 * @throws ParseException 
	 */
	public void getQueries( File file ) throws ParseException, IOException
	{
		//create indexReader
		IndexReader indexReader = DirectoryReader.open( 
				FSDirectory.open( directoryWithIndexedFile ) );
		//create indexSearcher
		indexSearcher = new IndexSearcher( indexReader );

		Scanner input = new Scanner( file );
		input.useDelimiter( ".I" );

		while( input.hasNext() ){
			String[] q = input.next().split( ".W" );
			//da accodare al file di trac eval
			String str = getQueryResult( FIELDS[ 4 ], q, 10 );
			System.out.println( str );
		}

		indexReader.close();
		closeAnalyzer();
		input.close();
	}

	/**
	 * 
	 * @param field
	 * @param q
	 * @param numberOfResults
	 * @throws ParseException
	 * @throws IOException
	 * 
	 */
	public String getQueryResult( String field, String[] q, int numberOfResults ) throws ParseException, IOException
	{	
		StringBuilder str = new StringBuilder();
		Query query = new QueryParser( field, analyzer ).parse( q[ 1 ] );
		TopDocs docs = indexSearcher.search( query, numberOfResults );
		ScoreDoc[] docsRetrieved = docs.scoreDocs;

		int s = 0;
		for( int i = 0; i < docsRetrieved.length; i++ ){
			str.append( Integer.parseInt( q[ 0 ].trim() ) + 
					" 0 " + 
					docsRetrieved[ i ].doc + 
					"  " + docsRetrieved[ i ].shardIndex + " " + 
					docsRetrieved[ i ].score + 
					" 1 " + 
					"\n" );
			s += docsRetrieved[ i ].score;
		}
		
		System.out.println( s );

		return str.toString();

	}

	/**
	 * 
	 * @param d
	 * @return
	 */
	public String getDocString( Document d )
	{
		StringBuilder str = new StringBuilder();

		for( int i = 0; i < SEPARATORS.length; i++ ){
			str.append( SEPARATORS[ i ] + ": " + d.get( FIELDS[ i ] ) + "\n" );
		}

		return str.toString();
	}

	/**
	 * generate index
	 * @param args 
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try{
			Index2 index = new Index2( Paths.get( "directoryWithIndexedFile2" ) );

			System.out.println( "Creating index (the process can keeps some minutes)..." );

			index.indexCollection( new File( "cran\\cran.all.1400" ) );

			System.out.println( "The index has been created\n" );

			index.closeIndexWriter();

			index.getQueries( new File( "cran\\cran.qry" ) );

		}
		catch( Exception e ){
			e.printStackTrace();
		}




	}

}
