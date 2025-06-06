package datastroke_UAS;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.datastruct.Heap;

// implementasi utama spell checker, kombinasi trie sama algoritma edit distance 
public class SpellCorrector implements ISpellCorrector {

   // trie buat simpan semua kata dictionary 
   private Trie trie = new Trie();
   // hashmap buat track frekuensi kata (berapa kali muncul di dictionary)
   private Map<String, Integer> wordFrequency = new HashMap<>();
   // daftar input yang dianggep invalid, langsung di-reject
   private static final List<String> invalidInput = Arrays.asList("lol", "abcdefghijklmnopqrstuvwxyz,"); 

   // constructor kosong, field udah auto-initialized
   public SpellCorrector() {
   }

   // load dictionary dari file text
   public void useDictionary(String dictionaryFileName) throws IOException {
      try {
         // setup file reader buat baca dictionary
         FileReader fr = new FileReader(dictionaryFileName);
         BufferedReader br = new BufferedReader(fr);
         String currentLine = null;

         // baca file line by line
         while((currentLine = br.readLine()) != null) {
            String cleanWord = currentLine.toLowerCase(); // convert ke lowercase spy konsisten

            // cek apakah line ini single word atau multiple words
            if (!currentLine.contains(" ")) {
               // kalo single word langsung masukin ke frequency map sama trie
               this.wordFrequency.put(cleanWord, this.wordFrequency.getOrDefault(cleanWord, 0) + 1);
               this.trie.add(cleanWord);
            } else {
               // kalo multiple words split dulu baru masukin satu-satu
               String[] splittedString = currentLine.split("\\s");

               for(String singleString : splittedString) {
                  String cleanStr = singleString.toLowerCase();

                  // update frequency counter & masukin ke trie
                  this.wordFrequency.put(cleanStr, this.wordFrequency.getOrDefault(cleanStr, 0) + 1);
                  this.trie.add(cleanStr); 
               }
            }
         }
         // tutup file readers
         fr.close();
         br.close();
      } catch (FileNotFoundException ex) {
         System.err.println("File not found: " + ex.getMessage()); // handle file ga ketemu
         throw ex;
      } catch (IOException ex) {
         System.err.println("IO Exception: " + ex.getMessage()); // handle error IO lainnya
         throw ex;
      }
   }

   // fungsi utama spell checker => kasih saran kata yang mirip
   public String suggestSimilarWord(String inputWord) {
      // validasi input: null, kosong, atau masuk daftar invalid
      if (inputWord == null || inputWord.length() == 0 || invalidInput.contains(inputWord.toLowerCase())) {
         return null;
      }

      String searchWord = inputWord.toLowerCase(); // convert input ke lowercase
      String suggestionResult = null;

      // Outer TreeMap  : Key = edit distance (int),    Value = TreeMap
      // Middle TreeMap : Key = frequency (int),        Value = TreeSet
      // Inner TreeSet  : Berisi kata-kata yang terurut scr alfabetis
      // TreeMap<Integer, TreeMap<Integer, TreeSet<String>>> map = new TreeMap<>(); 

      // jadi heap
      Heap<Integer, String> heap = new Heap<>(wordFrequency.size(), true); // min-heap berdasarkan edit distance

      INode node = this.trie.find(searchWord); // cek dulu apakah kata ada di dictionary
      
      if (node == null) {
         // kata ga ketemu, cari kata yang mirip
         Iterator<String> iterator = this.wordFrequency.keySet().iterator();
         
         // loop semua kata di dictionary
         while(iterator.hasNext()) {

            // ambil kata candidate, hitung edit distance antara input & candidate
            //String candidateWord = iterator.next(); 
            //int dist = this.editDistance(candidateWord, searchWord); 

            // dapetin atau buat treemap buat distance ini
            //TreeMap<Integer, TreeSet<String>> similarWords = map.getOrDefault(dist, new TreeMap<>());

            // dapetin frekuensi kata candidate
            //int freq = this.wordFrequency.get(candidateWord); 

            // dapetin atau buat treeset buat frequency ini
            //TreeSet<String> set = similarWords.getOrDefault(freq, new TreeSet<>());

            // masukin kata ke set
            //set.add(candidateWord);  

            // update nested map structure
            //similarWords.put(freq, set);
            //map.put(dist, similarWords);

            String candidateWord = iterator.next();
            int dist = this.editDistance(candidateWord, searchWord);
            int freq = this.wordFrequency.get(candidateWord);

            // Kombinasikan jarak edit dan -frekuensi supaya min-heap bisa urut: 
            // jarak terendah + frekuensi tertinggi + urutan abjad
            int compositeKey = dist * 1_000_000 - freq; // jarak lebih penting, freq dikurangi (negatif)
            heap.insert(compositeKey, candidateWord);
         }
         // ambil hasil => 1.distance terkecil, 2.frequency tertinggi, 3.kata pertama alphabetically
         //if (!map.isEmpty()) {
            //suggestionResult = map.firstEntry().getValue().lastEntry().getValue().first();
         //}

         // Ambil hasil dari heap:
         if (heap.size() > 0) {
            suggestionResult = heap.removeFirst().getData(); // data = kata
         }

      } else {
         suggestionResult = searchWord;  // kata ketemu di dictionary, return aja 
      }

      return suggestionResult;
   }

   // implementasi edit distance (damerau-levenshtein) buat ukur similarity kata
   private int editDistance(String word1, String word2) {
      
      int n = word1.length();
      int m = word2.length();
      int[][] dp = new int[n + 1][m + 1];

      // nested loop buat isi dp table
      for(int i = 0; i <= n; ++i) {
         for(int j = 0; j <= m; ++j) {
            if (i == 0) {        // base case => convert empty string ke word2[0..j-1] butuh j insertions
               dp[i][j] = j;
            } 
            else if (j == 0) {   // base case => convert word1[0..i-1] ke empty string butuh i deletions
               dp[i][j] = i;
            } 
            else if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
               // karakter sama, ga butuh operasi tambahan
               dp[i][j] = dp[i - 1][j - 1];  
            } 
            else if (i > 1 && j > 1 && 
                      word1.charAt(i - 1) == word2.charAt(j - 2) && 
                      word1.charAt(i - 2) == word2.charAt(j - 1)) {
               // transposition case, tukar posisi dua karakter
               dp[i][j] = 1 + Math.min(Math.min(dp[i - 2][j - 2], dp[i - 1][j]), 
                                       Math.min(dp[i][j - 1], dp[i - 1][j - 1]));
            } 
            else {
               dp[i][j] = 1 + Math.min(dp[i][j - 1], // insert
                              Math.min(dp[i - 1][j], // delete
                                       dp[i - 1][j - 1])); //replace
            }
         }
      }

      return dp[n][m]; // return minimum edit distance
   }
}