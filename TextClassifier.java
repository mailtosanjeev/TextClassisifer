import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

public class TextClassifier 
{
	Vector<String> words = new Vector<String>();
	HashMap<String,Float> singleWordProbMap = new HashMap<String,Float>();
	HashMap<String,Float> doubleWordProbMap = new HashMap<String,Float>();
	HashMap<String,Float> tripleWordProbMap = new HashMap<String,Float>();
	
	HashMap<String,Tuple<Float,Float>> bigramProbMap = new HashMap<String,Tuple<Float,Float>>();
	HashMap<String,Tuple<Float,Float>> trigramProbMap = new HashMap<String,Tuple<Float,Float>>();
	
	public static void main(String argv[])
	{
		// create a new text classifier
		TextClassifier tc = new TextClassifier();

		// give the file name to the text classifier
		String filename = "c:\\Users\\swesan\\Desktop\\nytimes.txt";
		tc.readFile(filename);

		// get the probabilities for single, double, triple word sequences
		tc.calculateSingleWordProbability(); 
		tc.calculateDoubleWordProbability(); 
		tc.calculateTripleWordProbability(); 
		
		tc.calculateBigramProbability();
		tc.calculateTrigramProbability();
		
		StringBuffer sentence = new StringBuffer();
		for(int i=0; i<133; i++)
		{
			sentence.append(tc.sampleNGram((float)Math.random()));
			sentence.append(" ");
		}
		
		System.out.println(sentence);
		
		float perplexity = tc.calculatePerplexity(sentence.toString());
		
		System.out.println("Perplexity is " + perplexity);
	}

	private float calculatePerplexity(String sentence) 
	{
		String[] sentenceWords = sentence.split("\\s");
		
		String word1 = sentenceWords[0];
		String word2 = sentenceWords[1];
		double prob = 1;
		for(int i=2; i<sentenceWords.length; i++)
		{
			Tuple<Float,Float> probTuple = trigramProbMap.get(word1 + " " + word2 + " " + sentenceWords[i]);
			if(probTuple == null) prob*=1.0/102.0;
			else prob *= probTuple.y - probTuple.x;
			System.out.println("prob is " + prob);
			word1 = word2;
			word2 = sentenceWords[i];
		}
		
		return (float)Math.pow(prob, -1.0/100);
	}

	private String sampleNGram(float prob) 
	{		
		HashMap<String,Tuple<Float,Float>> ngramMap = trigramProbMap;
		Object[] wordSet = ngramMap.keySet().toArray();
		
		for(int i=0; i<ngramMap.size(); i++)
		{
			if(prob>ngramMap.get(wordSet[i]).x && prob<=ngramMap.get(wordSet[i]).y)
			{
				System.out.println("Returning word " + (String)wordSet[i] + " for probability " + prob + " and interval as " + ngramMap.get(wordSet[i]).x + " and " + ngramMap.get(wordSet[i]).y);
				return (String)wordSet[i];
			}
		}
		
		
	    return "";
	}

	/**
	 * calculates the probability of each word in the sequence
	 */
	private void calculateSingleWordProbability() 
	{
		HashMap<String,Integer> fmap = new HashMap<String,Integer>();
		
		for(String word: words)
		{
			if(fmap.containsKey(word))
			{
				fmap.put(word,new Integer(fmap.get(word).intValue()+1));
			}
			else
			{
				fmap.put(word,new Integer(1));
			}
		}
		
		singleWordProbMap = normalizeSequenceProbs(fmap);
	}
	
	/**
	 * calculates the probability of each word in the sequence
	 */
	private void calculateDoubleWordProbability() 
	{
		HashMap<String,Integer> fmap = new HashMap<String,Integer>();
		
		String word1 = words.elementAt(0);
		for(int i=1; i<words.size(); i++)
		{
			String doubleWord = word1 + " " + words.elementAt(i);

			if(fmap.containsKey(doubleWord))
			{
				fmap.put(doubleWord,new Integer(fmap.get(doubleWord).intValue()+1));
			}
			else
			{
				fmap.put(doubleWord,new Integer(1));
			}
			
			word1 = words.elementAt(i);
		}
		
		doubleWordProbMap = normalizeSequenceProbs(fmap);
	}
	
	/**
	 * calculates the probability of each word in the sequence
	 */
	private void calculateTripleWordProbability() 
	{
		HashMap<String,Integer> fmap = new HashMap<String,Integer>();
		
		String word1 = words.elementAt(0);
		String word2 = words.elementAt(1);
		for(int i=2; i<words.size(); i++)
		{
			String tripleWord = word1 + " " + word2 + " " + words.elementAt(i);
			
			if(fmap.containsKey(tripleWord))
			{
				fmap.put(tripleWord,new Integer(fmap.get(tripleWord).intValue()+1));
			}
			else
			{
				fmap.put(tripleWord,new Integer(1));
			}
			
			word1 = words.elementAt(i-1);
			word2 = words.elementAt(i);
		}
		
		tripleWordProbMap = normalizeSequenceProbs(fmap);
	}
	
	private HashMap<String,Float> normalizeSequenceProbs(HashMap<String,Integer> map)
	{
		HashMap<String,Float> pmap = new HashMap<String,Float>();
		
		Object[] wordSet = map.keySet().toArray();
		int sum = 0;
		
		for(int i=0; i<map.size(); i++)
		{
			sum += map.get(wordSet[i]).intValue();
		}
		
		for(int i=0; i<map.size(); i++)
		{
			pmap.put((String)wordSet[i], map.get(wordSet[i]).intValue()*1.0F/sum);
		}
		
		return pmap;
	}
	
	private HashMap<String,Tuple<Float,Float>> normalizeNGramProbs(HashMap<String,Float> map)
	{
		HashMap<String,Tuple<Float,Float>> pmap = new HashMap<String,Tuple<Float,Float>>();
		
		Object[] wordSet = map.keySet().toArray();
		float sum = 0;
		
		for(int i=0; i<map.size(); i++)
		{
			sum += map.get(wordSet[i]).floatValue();
		}
		
		float beginInt = 0.0F;
		for(int i=0; i<map.size(); i++)
		{
			float endInt = beginInt + map.get(wordSet[i]).floatValue()*1.0F/sum;
			pmap.put((String)wordSet[i], new Tuple(beginInt,endInt));
			beginInt = endInt; 
		}
		
		return pmap;
	}
	
	private void calculateBigramProbability()
	{
		//calculate the probability of each bigram
		HashMap<String,Float> map = new HashMap<String,Float>();
		
		String word1 = words.elementAt(0);
		for(int i=1; i<words.size(); i++)
		{
			String doubleWord = word1 + " " + words.elementAt(i);
			map.put(doubleWord,doubleWordProbMap.get(doubleWord)*singleWordProbMap.get(word1));
			word1 = words.elementAt(i);
		}
		
		bigramProbMap = normalizeNGramProbs(map);
	}
	
	private void calculateTrigramProbability()
	{
		//calculate the probability of each trigram
		HashMap<String,Float> map = new HashMap<String,Float>();
		
		String word1 = words.elementAt(0);
		String word2 = words.elementAt(1);
		for(int i=2; i<words.size(); i++)
		{
			String tripleWord = word1 + " " + word2 + " " + words.elementAt(i);
			String doubleWord = word1 + " " + word2;
			map.put(tripleWord,tripleWordProbMap.get(tripleWord)*doubleWordProbMap.get(doubleWord)*singleWordProbMap.get(word1));
			word1 = words.elementAt(i-1);
			word2 = words.elementAt(i);
		}
		
		trigramProbMap = normalizeNGramProbs(map);
	}
	
	/**
	 * Method to read a file with the text to be classified
	 * @param filename - file to be classified
	 */
	private void readFile(String filename) 
	{
		// make sure the file exists otherwise throw an error
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(filename));

			String line = null;
			while ((line = reader.readLine()) != null) 
			{
				String[] wordsOnThisLine = line.split("\\s");	
				
				for(int j=0;j<wordsOnThisLine.length;j++)
				{
					String cleanWord = wordsOnThisLine[j].toLowerCase().replaceAll("\\W", "");
					if(!cleanWord.isEmpty())
						words.add(cleanWord);
				}
			}
			
			System.out.println("The word count is " + words.size());
		}
		catch(FileNotFoundException fnfe)
		{
			System.out.println("File not found.");
		}

		catch(IOException ioe)
		{
			System.out.println("IOError occured");
		}
	}
}
