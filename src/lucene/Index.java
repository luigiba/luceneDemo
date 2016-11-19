package lucene;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * create lucene index
 * @author Luigi
 *
 */
public class Index {	
	/**
	 * field names
	 */
	public static final String CONTENT = "content";
	public static final String FILE_NAME = "filename";
	public static final String FILE_PATH = "filepath";

	IndexWriter writer;
	Analyzer analyzer;
	Path directoryWithIndexedFile;

	/**
	 * initialize the index
	 * @param directoryWithIndexedFile
	 * @throws IOException
	 */
	public Index( Path directoryWithIndexedFile ) throws IOException
	{
		this.directoryWithIndexedFile = directoryWithIndexedFile;
		
		//create analyzer and directory that will contains indexed files
		this.analyzer = new StandardAnalyzer( EnglishAnalyzer.getDefaultStopSet() );	
		Directory directory = FSDirectory.open( directoryWithIndexedFile );

		//configure index writer
		IndexWriterConfig config = new IndexWriterConfig( analyzer );
		config.setOpenMode( IndexWriterConfig.OpenMode.CREATE );

		//create index writer
		writer = new IndexWriter( directory, config );

	}

	/**
	 * close index writer
	 * @throws IOException
	 */
	public void closeIndexWriter() throws IOException
	{
		if( writer != null ){
			writer.close();
		}
	}

	/**
	 * close analyzer
	 */
	public void closeAnalyzer()
	{
		if( analyzer != null ){
			analyzer.close();
		}
	}
	
	/**
	 * create index collection
	 * @param directoryToIndex
	 * @param flag
	 * @return false if directoryToIndex is not a directory, true otherwise
	 * @throws IOException
	 */
	public boolean indexCollection( File directoryToIndex ) throws IOException
	{
		if( ! directoryToIndex.isDirectory() ){
			return false;
		}

		File[] listFiles = directoryToIndex.listFiles();
		for( File file : listFiles ){
			writer.addDocument( indexFile( file ) );
		}

		return true;
	}

	/**
	 * index a single file
	 * @param file
	 * @param flag
	 * @return
	 * @throws FileNotFoundException
	 */
	public Document indexFile( File file ) throws FileNotFoundException
	{
		Document doc = new Document();
		FieldType fieldType = getFieldType();

		//index file contents
		Field contentField = new Field( CONTENT, getTextFromFile(file), fieldType );
		//index file name
		Field fileNameField = new Field( FILE_NAME, file.getName(), fieldType );
		//index file path
		Field filePathField = new Field( FILE_PATH, file.getPath(), fieldType);

		doc.add( contentField );
		doc.add( fileNameField );
		doc.add( filePathField );

		return doc;
	}

	/**
	 * 
	 * @return a field type
	 */
	public FieldType getFieldType()
	{
		FieldType fieldType = new FieldType();

		fieldType.setIndexOptions( IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS );
		fieldType.setTokenized(true);
		fieldType.setStored( true );
		fieldType.setStoreTermVectors(true);
		fieldType.setStoreTermVectorPositions(true);
		fieldType.setStoreTermVectorOffsets(true);
		
		return fieldType;
	}

	/**
	 * 
	 * @param file
	 * @return file content
	 * @throws FileNotFoundException
	 */
	public String getTextFromFile( File file ) throws FileNotFoundException
	{
		StringBuilder text = new StringBuilder();

		Scanner input = new Scanner( file );
		while( input.hasNext() ){
			text.append( " " + input.next() );
		}
		input.close();

		return text.toString();
	}


	/**
	 * 
	 * @param field
	 * @param q
	 * @param numberOfResults
	 * @throws ParseException
	 * @throws IOException
	 */
	public void search( String field, String q, int numberOfResults ) throws ParseException, IOException
	{
		//create indexReader
		IndexReader indexReader = DirectoryReader.open( 
				FSDirectory.open( directoryWithIndexedFile ) );
		//create indexSearcher
		IndexSearcher indexSearcher = new IndexSearcher( indexReader );
		
		System.out.println( "Searching for '" + field + "' : '" + q + "' ..." );

		Query query = new QueryParser( field, analyzer ).parse( q );
		TopDocs docs = indexSearcher.search( query, numberOfResults );
		ScoreDoc[] docsRetrieved = docs.scoreDocs;

		System.out.println( "Matching:" );
		if( docsRetrieved == null || docsRetrieved.length == 0 ){
			System.out.println( "NO MATCH" );
			return;
		}

		for( int i = 0; i < docsRetrieved.length; i++ ){
			Document d = indexSearcher.doc( docsRetrieved[ i ].doc );
			System.out.println( getDocString( d ) + "\n" );  
		}

		indexReader.close();
		closeAnalyzer();
	}

	/**
	 * 
	 * @param d
	 * @return
	 */
	public String getDocString( Document d )
	{
		return  "File name: " + d.get(FILE_NAME) + "\n" +
				"File path: " + d.get(FILE_PATH) + "\n" +
				"File content: " + d.get(CONTENT);
	}

	/**
	 * generate index
	 * @param args
	 */
	public static void main(String[] args) {
		try{
			System.out.println( "Creating index (the process can keeps some minutes)..." );
			
			Index index = new Index( Paths.get( "directoryWithIndexedFile" )  );

			index.indexCollection( new File( "directoryToIndex" ) );
			
			System.out.println( "The index has been created\n" );

			index.closeIndexWriter();
			
			index.search( CONTENT, "java -Arduino", 10 );


		}
		catch( Exception e ){
			e.printStackTrace();
		}


	}

}
