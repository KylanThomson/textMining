/*
   Assignment 4
   Creators: Kylan Thomson, John Kelley, Robert Grey
   Description: This program creates the feature vector by applying
   the following text mining techniques to a set of paragraphs.
        A. Tokenize paragraphs
        B. Remove punctuation and special characters
        C. Remove numbers
        D. Convert upper-case to lower-case
        E. Remove stop words. A set of stop words is provided in the file “stop_words.txt”
        F. Perform stemming. Use the Porter stemming code provided in the file “Porter_Stemmer_X.txt”
        G. Combine stemmed words.
        H. Extract most frequent words.
   Then this program implements a clustering algorithm to group similar paragraphs together.
 */

/*
   Porter stemmer in Java. The original paper is in
       Porter, 1980, An algorithm for suffix stripping, Program, Vol. 14,
       no. 3, pp 130-137,
   See also http://www.tartarus.org/~martin/PorterStemmer
   History:
   Release 1
   Bug 1 (reported by Gonzalo Parra 16/10/99) fixed as marked below.
   The words 'aed', 'eed', 'oed' leave k at 'a' for step 3, and b[k-1]
   is then out outside the bounds of b.
   Release 2
   Similarly,
   Bug 2 (reported by Steve Dyrdahl 22/2/00) fixed as marked below.
   'ion' by itself leaves j = -1 in the test for 'ion' in step 5, and
   b[j] is then outside the bounds of b.
   Release 3
   Considerably revised 4/9/00 in the light of many helpful suggestions
   from Brian Goetz of Quiotix Corporation (brian@quiotix.com).
   Release 4
*/

//import java.io.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Stemmer, implementing the Porter Stemming Algorithm
 *
 * The Stemmer class transforms a word into its root form.  The input
 * word can be provided a character at time (by calling add()), or at once
 * by calling one of the various stem(something) methods.
 */

class Stemmer
{  private char[] b;
    private int i,     /* offset into b */
            i_end, /* offset to end of stemmed word */
            j, k;
    private static final int INC = 50;

    /** Test program for demonstrating the Stemmer.  It reads text from a
     * a list of files, stems each word, and writes the result to standard
     * output. Note that the word stemmed is expected to be in lower case:
     * forcing lower case must be done outside the Stemmer class.
     * Usage: Stemmer file-name file-name ...
     */
    public static void main(String[] args) throws FileNotFoundException {
        String pathName = System.getProperty("user.dir") + "\\src\\paragraphs.txt";
        prepFeatures(pathName);
    }


    /* unit of size whereby b is increased */
    public Stemmer()
    {  b = new char[INC];
        i = 0;
        i_end = 0;
    }

    /**
     * Add a character to the word being stemmed.  When you are finished
     * adding characters, you can call stem(void) to stem the word.
     */

    public void add(char ch)
    {  if (i == b.length)
    {  char[] new_b = new char[i+INC];
        for (int c = 0; c < i; c++) new_b[c] = b[c];
        b = new_b;
    }
        b[i++] = ch;
    }


    /** Adds wLen characters to the word being stemmed contained in a portion
     * of a char[] array. This is like repeated calls of add(char ch), but
     * faster.
     */

    public void add(char[] w, int wLen)
    {  if (i+wLen >= b.length)
    {  char[] new_b = new char[i+wLen+INC];
        for (int c = 0; c < i; c++) new_b[c] = b[c];
        b = new_b;
    }
        for (int c = 0; c < wLen; c++) b[i++] = w[c];
    }

    /**
     * After a word has been stemmed, it can be retrieved by toString(),
     * or a reference to the internal buffer can be retrieved by getResultBuffer
     * and getResultLength (which is generally more efficient.)
     */
    public String toString() { return new String(b,0,i_end); }

    /**
     * Returns the length of the word resulting from the stemming process.
     */
    public int getResultLength() { return i_end; }

    /**
     * Returns a reference to a character buffer containing the results of
     * the stemming process.  You also need to consult getResultLength()
     * to determine the length of the result.
     */
    public char[] getResultBuffer() { return b; }

    /* cons(i) is true <=> b[i] is a consonant. */

    private final boolean cons(int i)
    {  switch (b[i])
    {  case 'a': case 'e': case 'i': case 'o': case 'u': return false;
        case 'y': return (i==0) ? true : !cons(i-1);
        default: return true;
    }
    }

   /* m() measures the number of consonant sequences between 0 and j. if c is
      a consonant sequence and v a vowel sequence, and <..> indicates arbitrary
      presence,
         <c><v>       gives 0
         <c>vc<v>     gives 1
         <c>vcvc<v>   gives 2
         <c>vcvcvc<v> gives 3
         ....
   */

    private final int m()
    {  int n = 0;
        int i = 0;
        while(true)
        {  if (i > j) return n;
            if (! cons(i)) break; i++;
        }
        i++;
        while(true)
        {  while(true)
        {  if (i > j) return n;
            if (cons(i)) break;
            i++;
        }
            i++;
            n++;
            while(true)
            {  if (i > j) return n;
                if (! cons(i)) break;
                i++;
            }
            i++;
        }
    }

    /* vowelinstem() is true <=> 0,...j contains a vowel */

    private final boolean vowelinstem()
    {  int i; for (i = 0; i <= j; i++) if (! cons(i)) return true;
        return false;
    }

    /* doublec(j) is true <=> j,(j-1) contain a double consonant. */

    private final boolean doublec(int j)
    {  if (j < 1) return false;
        if (b[j] != b[j-1]) return false;
        return cons(j);
    }

   /* cvc(i) is true <=> i-2,i-1,i has the form consonant - vowel - consonant
      and also if the second c is not w,x or y. this is used when trying to
      restore an e at the end of a short word. e.g.
         cav(e), lov(e), hop(e), crim(e), but
         snow, box, tray.
   */

    private final boolean cvc(int i)
    {  if (i < 2 || !cons(i) || cons(i-1) || !cons(i-2)) return false;
        {  int ch = b[i];
            if (ch == 'w' || ch == 'x' || ch == 'y') return false;
        }
        return true;
    }

    private final boolean ends(String s)
    {  int l = s.length();
        int o = k-l+1;
        if (o < 0) return false;
        for (int i = 0; i < l; i++) if (b[o+i] != s.charAt(i)) return false;
        j = k-l;
        return true;
    }

   /* setto(s) sets (j+1),...k to the characters in the string s, readjusting
      k. */

    private final void setto(String s)
    {  int l = s.length();
        int o = j+1;
        for (int i = 0; i < l; i++) b[o+i] = s.charAt(i);
        k = j+l;
    }

    /* r(s) is used further down. */

    private final void r(String s) { if (m() > 0) setto(s); }

   /* step1() gets rid of plurals and -ed or -ing. e.g.
          caresses  ->  caress
          ponies    ->  poni
          ties      ->  ti
          caress    ->  caress
          cats      ->  cat
          feed      ->  feed
          agreed    ->  agree
          disabled  ->  disable
          matting   ->  mat
          mating    ->  mate
          meeting   ->  meet
          milling   ->  mill
          messing   ->  mess
          meetings  ->  meet
   */

    private final void step1()
    {  if (b[k] == 's')
    {  if (ends("sses")) k -= 2; else
    if (ends("ies")) setto("i"); else
    if (b[k-1] != 's') k--;
    }
        if (ends("eed")) { if (m() > 0) k--; } else
        if ((ends("ed") || ends("ing")) && vowelinstem())
        {  k = j;
            if (ends("at")) setto("ate"); else
            if (ends("bl")) setto("ble"); else
            if (ends("iz")) setto("ize"); else
            if (doublec(k))
            {  k--;
                {  int ch = b[k];
                    if (ch == 'l' || ch == 's' || ch == 'z') k++;
                }
            }
            else if (m() == 1 && cvc(k)) setto("e");
        }
    }

    /* step2() turns terminal y to i when there is another vowel in the stem. */

    private final void step2() { if (ends("y") && vowelinstem()) b[k] = 'i'; }

   /* step3() maps double suffices to single ones. so -ization ( = -ize plus
      -ation) maps to -ize etc. note that the string before the suffix must give
      m() > 0. */

    private final void step3() { if (k == 0) return; /* For Bug 1 */ switch (b[k-1])
    {
        case 'a': if (ends("ational")) { r("ate"); break; }
            if (ends("tional")) { r("tion"); break; }
            break;
        case 'c': if (ends("enci")) { r("ence"); break; }
            if (ends("anci")) { r("ance"); break; }
            break;
        case 'e': if (ends("izer")) { r("ize"); break; }
            break;
        case 'l': if (ends("bli")) { r("ble"); break; }
            if (ends("alli")) { r("al"); break; }
            if (ends("entli")) { r("ent"); break; }
            if (ends("eli")) { r("e"); break; }
            if (ends("ousli")) { r("ous"); break; }
            break;
        case 'o': if (ends("ization")) { r("ize"); break; }
            if (ends("ation")) { r("ate"); break; }
            if (ends("ator")) { r("ate"); break; }
            break;
        case 's': if (ends("alism")) { r("al"); break; }
            if (ends("iveness")) { r("ive"); break; }
            if (ends("fulness")) { r("ful"); break; }
            if (ends("ousness")) { r("ous"); break; }
            break;
        case 't': if (ends("aliti")) { r("al"); break; }
            if (ends("iviti")) { r("ive"); break; }
            if (ends("biliti")) { r("ble"); break; }
            break;
        case 'g': if (ends("logi")) { r("log"); break; }
    } }

    /* step4() deals with -ic-, -full, -ness etc. similar strategy to step3. */

    private final void step4() { switch (b[k])
    {
        case 'e': if (ends("icate")) { r("ic"); break; }
            if (ends("ative")) { r(""); break; }
            if (ends("alize")) { r("al"); break; }
            break;
        case 'i': if (ends("iciti")) { r("ic"); break; }
            break;
        case 'l': if (ends("ical")) { r("ic"); break; }
            if (ends("ful")) { r(""); break; }
            break;
        case 's': if (ends("ness")) { r(""); break; }
            break;
    } }

    /* step5() takes off -ant, -ence etc., in context <c>vcvc<v>. */

    private final void step5()
    {   if (k == 0) return; /* for Bug 1 */ switch (b[k-1])
    {  case 'a': if (ends("al")) break; return;
        case 'c': if (ends("ance")) break;
            if (ends("ence")) break; return;
        case 'e': if (ends("er")) break; return;
        case 'i': if (ends("ic")) break; return;
        case 'l': if (ends("able")) break;
            if (ends("ible")) break; return;
        case 'n': if (ends("ant")) break;
            if (ends("ement")) break;
            if (ends("ment")) break;
            /* element etc. not stripped before the m */
            if (ends("ent")) break; return;
        case 'o': if (ends("ion") && j >= 0 && (b[j] == 's' || b[j] == 't')) break;
            /* j >= 0 fixes Bug 2 */
            if (ends("ou")) break; return;
        /* takes care of -ous */
        case 's': if (ends("ism")) break; return;
        case 't': if (ends("ate")) break;
            if (ends("iti")) break; return;
        case 'u': if (ends("ous")) break; return;
        case 'v': if (ends("ive")) break; return;
        case 'z': if (ends("ize")) break; return;
        default: return;
    }
        if (m() > 1) k = j;
    }

    /* step6() removes a final -e if m() > 1. */

    private final void step6()
    {  j = k;
        if (b[k] == 'e')
        {  int a = m();
            if (a > 1 || a == 1 && !cvc(k-1)) k--;
        }
        if (b[k] == 'l' && doublec(k) && m() > 1) k--;
    }

    /** Stem the word placed into the Stemmer buffer through calls to add().
     * Returns true if the stemming process resulted in a word different
     * from the input.  You can retrieve the result with
     * getResultLength()/getResultBuffer() or toString().
     */
    public void stem()
    {  k = i - 1;
        if (k > 1) { step1(); step2(); step3(); step4(); step5(); step6(); }
        i_end = k+1; i = 0;
    }

    // Removes stop words
    // Removes numbers
    // Removes punctuation
    public static void prepFeatures(String pathName) throws FileNotFoundException {

        String token = "";
        Scanner inFile1 = new Scanner(new File(System.getProperty("user.dir") + "\\src\\paragraphs.txt")).useDelimiter(" ");
        ArrayList<String> text = new ArrayList<String>();
        ArrayList<String> words = new ArrayList<String>();
        ArrayList<String> stemmedWords = new ArrayList<String>();

        while (inFile1.hasNext()) {
            // find next line
            token = inFile1.next();
            text.add(token);
        }
        inFile1.close();

        for(int i = 0; i < text.size(); i++){
            String temp = text.get(i).toLowerCase();
            // removing punctuation (except for ";" because this is used as a delimiter for paragraphs)
            temp = temp.replaceAll("[?,\".{}!@#$%^&*()|:'`~â€“<>]","");
            // removing numbers
            temp = temp.replaceAll("[0-9]", "");
            temp = temp.replaceAll("]", "");
            //temp = temp.replaceAll("\n", ";");
            if(temp.contains(";")){
                //temp = temp.replaceAll("\n", "");
                String[] arr = temp.split("[^a-zA-Z;]+");
                for ( String ss : arr) {
                    if(ss.length() < 1){
                        continue;
                    }
                    if(ss.length() > 1 && ss.contains(";")){
                        String split1 = ss.substring(0, ss.length() - 1);
                        temp = split1;
                        String split2 = ";";
                        words.add(split2);
                    }
                    // removing stop words
                    else{
                        if(ss.equals("a")) continue;
                        if(ss.equals("able")) continue;
                        if(ss.equals("about")) continue;
                        if(ss.equals("across")) continue;
                        if(ss.equals("all")) continue;
                        if(ss.equals("almost")) continue;
                        if(ss.equals("also")) continue;
                        if(ss.equals("am")) continue;
                        if(ss.equals("among")) continue;
                        if(ss.equals("an")) continue;
                        if(ss.equals("and")) continue;
                        if(ss.equals("any")) continue;
                        if(ss.equals("are")) continue;
                        if(ss.equals("as")) continue;
                        if(ss.equals("at")) continue;
                        if(ss.equals("be")) continue;
                        if(ss.equals("because")) continue;
                        if(ss.equals("been")) continue;
                        if(ss.equals("but")) continue;
                        if(ss.equals("by")) continue;
                        if(ss.equals("can")) continue;
                        if(ss.equals("cannot")) continue;
                        if(ss.equals("could")) continue;
                        if(ss.equals("dear")) continue;
                        if(ss.equals("did")) continue;
                        if(ss.equals("do")) continue;
                        if(ss.equals("does")) continue;
                        if(ss.equals("either")) continue;
                        if(ss.equals("else")) continue;
                        if(ss.equals("ever")) continue;
                        if(ss.equals("every")) continue;
                        if(ss.equals("for")) continue;
                        if(ss.equals("from")) continue;
                        if(ss.equals("get")) continue;
                        if(ss.equals("got")) continue;
                        if(ss.equals("had")) continue;
                        if(ss.equals("has")) continue;
                        if(ss.equals("have")) continue;
                        if(ss.equals("he")) continue;
                        if(ss.equals("her")) continue;
                        if(ss.equals("hers")) continue;
                        if(ss.equals("him")) continue;
                        if(ss.equals("his")) continue;
                        if(ss.equals("how")) continue;
                        if(ss.equals("however")) continue;
                        if(ss.equals("i")) continue;
                        if(ss.equals("if")) continue;
                        if(ss.equals("in")) continue;
                        if(ss.equals("into")) continue;
                        if(ss.equals("is")) continue;
                        if(ss.equals("it")) continue;
                        if(ss.equals("its")) continue;
                        if(ss.equals("just")) continue;
                        if(ss.equals("least")) continue;
                        if(ss.equals("let")) continue;
                        if(ss.equals("like")) continue;
                        if(ss.equals("likely")) continue;
                        if(ss.equals("may")) continue;
                        if(ss.equals("me")) continue;
                        if(ss.equals("might")) continue;
                        if(ss.equals("most")) continue;
                        if(ss.equals("must")) continue;
                        if(ss.equals("my")) continue;
                        if(ss.equals("neither")) continue;
                        if(ss.equals("no")) continue;
                        if(ss.equals("nor")) continue;
                        if(ss.equals("not")) continue;
                        if(ss.equals("of")) continue;
                        if(ss.equals("[of")) continue;
                        if(ss.equals("off")) continue;
                        if(ss.equals("often")) continue;
                        if(ss.equals("on")) continue;
                        if(ss.equals("only")) continue;
                        if(ss.equals("or")) continue;
                        if(ss.equals("other")) continue;
                        if(ss.equals("our")) continue;
                        if(ss.equals("own")) continue;
                        if(ss.equals("rather")) continue;
                        if(ss.equals("said")) continue;
                        if(ss.equals("say")) continue;
                        if(ss.equals("says")) continue;
                        if(ss.equals("she")) continue;
                        if(ss.equals("should")) continue;
                        if(ss.equals("since")) continue;
                        if(ss.equals("so")) continue;
                        if(ss.equals("some")) continue;
                        if(ss.equals("than")) continue;
                        if(ss.equals("that")) continue;
                        if(ss.equals("the")) continue;
                        if(ss.equals("their")) continue;
                        if(ss.equals("them")) continue;
                        if(ss.equals("then")) continue;
                        if(ss.equals("there")) continue;
                        if(ss.equals("these")) continue;
                        if(ss.equals("they")) continue;
                        if(ss.equals("this")) continue;
                        if(ss.equals("tis")) continue;
                        if(ss.equals("to")) continue;
                        if(ss.equals("too")) continue;
                        if(ss.equals("twas")) continue;
                        if(ss.equals("us")) continue;
                        if(ss.equals("wants")) continue;
                        if(ss.equals("was")) continue;
                        if(ss.equals("we")) continue;
                        if(ss.equals("were")) continue;
                        if(ss.equals("what")) continue;
                        if(ss.equals("when")) continue;
                        if(ss.equals("where")) continue;
                        if(ss.equals("which")) continue;
                        if(ss.equals("while")) continue;
                        if(ss.equals("who")) continue;
                        if(ss.equals("whom")) continue;
                        if(ss.equals("why")) continue;
                        if(ss.equals("will")) continue;
                        if(ss.equals("with")) continue;
                        if(ss.equals("would")) continue;
                        if(ss.equals("yet")) continue;
                        if(ss.equals("you")) continue;
                        if(ss.equals("your")) continue;
                        if(ss.length() < 1) continue;
                        if(ss.contains(";") && ss.length() > 1) continue;
                        words.add(ss);
                    }
                }
            }
            if(temp.equals("a")) continue;
            if(temp.equals("able")) continue;
            if(temp.equals("about")) continue;
            if(temp.equals("across")) continue;
            if(temp.equals("all")) continue;
            if(temp.equals("almost")) continue;
            if(temp.equals("also")) continue;
            if(temp.equals("am")) continue;
            if(temp.equals("among")) continue;
            if(temp.equals("an")) continue;
            if(temp.equals("and")) continue;
            if(temp.equals("any")) continue;
            if(temp.equals("are")) continue;
            if(temp.equals("as")) continue;
            if(temp.equals("at")) continue;
            if(temp.equals("be")) continue;
            if(temp.equals("because")) continue;
            if(temp.equals("been")) continue;
            if(temp.equals("but")) continue;
            if(temp.equals("by")) continue;
            if(temp.equals("can")) continue;
            if(temp.equals("cannot")) continue;
            if(temp.equals("could")) continue;
            if(temp.equals("dear")) continue;
            if(temp.equals("did")) continue;
            if(temp.equals("do")) continue;
            if(temp.equals("does")) continue;
            if(temp.equals("either")) continue;
            if(temp.equals("else")) continue;
            if(temp.equals("ever")) continue;
            if(temp.equals("every")) continue;
            if(temp.equals("for")) continue;
            if(temp.equals("from")) continue;
            if(temp.equals("get")) continue;
            if(temp.equals("got")) continue;
            if(temp.equals("had")) continue;
            if(temp.equals("has")) continue;
            if(temp.equals("have")) continue;
            if(temp.equals("he")) continue;
            if(temp.equals("her")) continue;
            if(temp.equals("hers")) continue;
            if(temp.equals("him")) continue;
            if(temp.equals("his")) continue;
            if(temp.equals("how")) continue;
            if(temp.equals("however")) continue;
            if(temp.equals("i")) continue;
            if(temp.equals("if")) continue;
            if(temp.equals("in")) continue;
            if(temp.equals("into")) continue;
            if(temp.equals("is")) continue;
            if(temp.equals("it")) continue;
            if(temp.equals("its")) continue;
            if(temp.equals("just")) continue;
            if(temp.equals("least")) continue;
            if(temp.equals("let")) continue;
            if(temp.equals("like")) continue;
            if(temp.equals("likely")) continue;
            if(temp.equals("may")) continue;
            if(temp.equals("me")) continue;
            if(temp.equals("might")) continue;
            if(temp.equals("most")) continue;
            if(temp.equals("must")) continue;
            if(temp.equals("my")) continue;
            if(temp.equals("neither")) continue;
            if(temp.equals("no")) continue;
            if(temp.equals("nor")) continue;
            if(temp.equals("not")) continue;
            if(temp.equals("of")) continue;
            if(temp.equals("[of")) continue;
            if(temp.equals("off")) continue;
            if(temp.equals("often")) continue;
            if(temp.equals("on")) continue;
            if(temp.equals("only")) continue;
            if(temp.equals("or")) continue;
            if(temp.equals("other")) continue;
            if(temp.equals("our")) continue;
            if(temp.equals("own")) continue;
            if(temp.equals("rather")) continue;
            if(temp.equals("said")) continue;
            if(temp.equals("say")) continue;
            if(temp.equals("says")) continue;
            if(temp.equals("she")) continue;
            if(temp.equals("should")) continue;
            if(temp.equals("since")) continue;
            if(temp.equals("so")) continue;
            if(temp.equals("some")) continue;
            if(temp.equals("than")) continue;
            if(temp.equals("that")) continue;
            if(temp.equals("the")) continue;
            if(temp.equals("their")) continue;
            if(temp.equals("them")) continue;
            if(temp.equals("then")) continue;
            if(temp.equals("there")) continue;
            if(temp.equals("these")) continue;
            if(temp.equals("they")) continue;
            if(temp.equals("this")) continue;
            if(temp.equals("tis")) continue;
            if(temp.equals("to")) continue;
            if(temp.equals("too")) continue;
            if(temp.equals("twas")) continue;
            if(temp.equals("us")) continue;
            if(temp.equals("wants")) continue;
            if(temp.equals("was")) continue;
            if(temp.equals("we")) continue;
            if(temp.equals("were")) continue;
            if(temp.equals("what")) continue;
            if(temp.equals("when")) continue;
            if(temp.equals("where")) continue;
            if(temp.equals("which")) continue;
            if(temp.equals("while")) continue;
            if(temp.equals("who")) continue;
            if(temp.equals("whom")) continue;
            if(temp.equals("why")) continue;
            if(temp.equals("will")) continue;
            if(temp.equals("with")) continue;
            if(temp.equals("would")) continue;
            if(temp.equals("yet")) continue;
            if(temp.equals("you")) continue;
            if(temp.equals("your")) continue;
            if(temp.length() < 1) continue;
            if(temp.contains(";") && temp.length() > 1) continue;
            words.add(temp);
        }
        stemWords(words);
    }

    /**
     * breaks words down to their root word
     * @param words
     */
    public static void stemWords(ArrayList<String> words){
        ArrayList<String> stemmedWords = new ArrayList<String>();
        for(int i = 0; i < words.size(); i++){
            String temp = words.get(i);
            Stemmer s = new Stemmer();
            for(int j = 0; j < temp.length(); j++){
                s.add(temp.charAt(j));
            }
            s.stem();
            String u = s.toString();
            if(u.length() > 0) stemmedWords.add(u);
        }
        paragraphs(words, stemmedWords);
    }
    public static void paragraphs(ArrayList<String> words, ArrayList<String> stemmedWords){
        ArrayList<String> paragraph_1 = new ArrayList<String>();
        ArrayList<String> stemmed_1 = new ArrayList<String>();
        ArrayList<String> paragraph_2 = new ArrayList<String>();
        ArrayList<String> stemmed_2 = new ArrayList<String>();
        ArrayList<String> paragraph_3 = new ArrayList<String>();
        ArrayList<String> stemmed_3 = new ArrayList<String>();
        ArrayList<String> paragraph_4 = new ArrayList<String>();
        ArrayList<String> stemmed_4 = new ArrayList<String>();
        ArrayList<String> paragraph_5 = new ArrayList<String>();
        ArrayList<String> stemmed_5 = new ArrayList<String>();
        ArrayList<String> paragraph_6 = new ArrayList<String>();
        ArrayList<String> stemmed_6 = new ArrayList<String>();
        ArrayList<String> paragraph_7 = new ArrayList<String>();
        ArrayList<String> stemmed_7 = new ArrayList<String>();
        ArrayList<String> paragraph_8 = new ArrayList<String>();
        ArrayList<String> stemmed_8 = new ArrayList<String>();
        ArrayList<String> paragraph_9 = new ArrayList<String>();
        ArrayList<String> stemmed_9 = new ArrayList<String>();
        ArrayList<String> paragraph_10 = new ArrayList<String>();
        ArrayList<String> stemmed_10 = new ArrayList<String>();
        ArrayList<String> paragraph_11 = new ArrayList<String>();
        ArrayList<String> stemmed_11 = new ArrayList<String>();
        ArrayList<String> paragraph_12 = new ArrayList<String>();
        ArrayList<String> stemmed_12 = new ArrayList<String>();
        ArrayList<String> paragraph_13 = new ArrayList<String>();
        ArrayList<String> stemmed_13 = new ArrayList<String>();
        ArrayList<String> paragraph_14 = new ArrayList<String>();
        ArrayList<String> stemmed_14 = new ArrayList<String>();
        ArrayList<String> paragraph_15 = new ArrayList<String>();
        ArrayList<String> stemmed_15 = new ArrayList<String>();
        ArrayList<String> paragraph_16 = new ArrayList<String>();
        ArrayList<String> stemmed_16 = new ArrayList<String>();
        ArrayList<String> allTXT = new ArrayList<String>();
        ArrayList<String> stemmed_allTXT = new ArrayList<String>();
        int count = 1;
        int index = 0;

        /**
         * making arraylists for each paragraph using ";" as a delimiter
         */
        for(int i = 0; i < 100_000; i++){
            if(words.get(i).contains(";")){
                count++;
                if(count > 1){
                    index = i + 1;
                    break;
                }
            }
            paragraph_1.add(words.get(i));
            stemmed_1.add(stemmedWords.get(i));
        }

        for(int i = index; i < 100_000; i++){
            if(words.get(i).contains(";")){
                count++;
                if(count > 2){
                    index = i + 1;
                    break;
                }
            }
            paragraph_2.add(words.get(i));
            stemmed_2.add(stemmedWords.get(i));
        }

        for(int i = index; i < 100_000; i++){
            if(words.get(i).contains(";")){
                count++;
                if(count > 3){
                    index = i + 1;
                    break;
                }
            }
            paragraph_3.add(words.get(i));
            stemmed_3.add(stemmedWords.get(i));
        }

        for(int i = index; i < 100_000; i++){
            if(words.get(i).contains(";")){
                count++;
                if(count > 4){
                    index = i + 1;
                    break;
                }
            }
            paragraph_4.add(words.get(i));
            stemmed_4.add(stemmedWords.get(i));
        }

        for(int i = index; i < 100_000; i++){
            if(words.get(i).contains(";")){
                count++;
                if(count > 5){
                    index = i + 1;
                    break;
                }
            }
            paragraph_5.add(words.get(i));
            stemmed_5.add(stemmedWords.get(i));
        }

        for(int i = index; i < 100_000; i++){
            if(words.get(i).contains(";")){
                count++;
                if(count > 6){
                    index = i + 1;
                    break;
                }
            }
            paragraph_6.add(words.get(i));
            stemmed_6.add(stemmedWords.get(i));
        }

        for(int i = index; i < 100_000; i++){
            if(words.get(i).contains(";")){
                count++;
                if(count > 7){
                    index = i + 1;
                    break;
                }
            }
            paragraph_7.add(words.get(i));
            stemmed_7.add(stemmedWords.get(i));
        }

        for(int i = index; i < 100_000; i++){
            if(words.get(i).contains(";")){
                count++;
                if(count > 8){
                    index = i + 1;
                    break;
                }
            }
            paragraph_8.add(words.get(i));
            stemmed_8.add(stemmedWords.get(i));
        }

        for(int i = index; i < 100_000; i++){
            if(words.get(i).contains(";")){
                count++;
                if(count > 9){
                    index = i + 1;
                    break;
                }
            }
            paragraph_9.add(words.get(i));
            stemmed_9.add(stemmedWords.get(i));
        }

        for(int i = index; i < 100_000; i++){
            if(words.get(i).contains(";")){
                count++;
                if(count > 10){
                    index = i + 1;
                    break;
                }
            }
            paragraph_10.add(words.get(i));
            stemmed_10.add(stemmedWords.get(i));
        }

        for(int i = index; i < 100_000; i++){
            if(words.get(i).contains(";")){
                count++;
                if(count > 11){
                    index = i + 1;
                    break;
                }
            }
            paragraph_11.add(words.get(i));
            stemmed_11.add(stemmedWords.get(i));
        }

        for(int i = index; i < 100_000; i++){
            if(words.get(i).contains(";")){
                count++;
                if(count > 12){
                    index = i + 1;
                    break;
                }
            }
            paragraph_12.add(words.get(i));
            stemmed_12.add(stemmedWords.get(i));
        }

        for(int i = index; i < 100_000; i++){
            if(words.get(i).contains(";")){
                count++;
                if(count > 13){
                    index = i + 1;
                    break;
                }
            }
            paragraph_13.add(words.get(i));
            stemmed_13.add(stemmedWords.get(i));
        }

        for(int i = index; i < 100_000; i++){
            if(words.get(i).contains(";")){
                count++;
                if(count > 14){
                    index = i + 1;
                    break;
                }
            }
            paragraph_14.add(words.get(i));
            stemmed_14.add(stemmedWords.get(i));
        }

        for(int i = index; i < 100_000; i++){
            if(words.get(i).contains(";")){
                count++;
                if(count > 15){
                    index = i + 1;
                    break;
                }
            }
            paragraph_15.add(words.get(i));
            stemmed_15.add(stemmedWords.get(i));
        }

        for(int i = index; i < 100_000; i++){
            if(words.get(i).contains(";")){
                count++;
                if(count > 16){
                    index = i + 1;
                    break;
                }
            }
            paragraph_16.add(words.get(i));
            stemmed_16.add(stemmedWords.get(i));
        }
        for(int i = 0; i < words.size(); i++){
            if(words.get(i).contains(";")) continue;
            allTXT.add(words.get(i));
            stemmed_allTXT.add(stemmedWords.get(i));
        }

        ArrayList<String> all = combineStemmed(allTXT, stemmed_allTXT, 0);
        ArrayList<String> p1 = combineStemmed(paragraph_1, stemmed_1, 1);
        ArrayList<String> p2 = combineStemmed(paragraph_2, stemmed_2, 2);
        ArrayList<String> p3 = combineStemmed(paragraph_3, stemmed_3, 3);
        ArrayList<String> p4 = combineStemmed(paragraph_4, stemmed_4, 4);
        ArrayList<String> p5 = combineStemmed(paragraph_5, stemmed_5, 5);
        ArrayList<String> p6 = combineStemmed(paragraph_6, stemmed_6, 6);
        ArrayList<String> p7 = combineStemmed(paragraph_7, stemmed_7, 7);
        ArrayList<String> p8 = combineStemmed(paragraph_8, stemmed_8, 8);
        ArrayList<String> p9 = combineStemmed(paragraph_9, stemmed_9, 9);
        ArrayList<String> p10 = combineStemmed(paragraph_10, stemmed_10, 10);
        ArrayList<String> p11 = combineStemmed(paragraph_11, stemmed_11, 11);
        ArrayList<String> p12 = combineStemmed(paragraph_12, stemmed_12, 12);
        ArrayList<String> p13 = combineStemmed(paragraph_13, stemmed_13, 13);
        ArrayList<String> p14 = combineStemmed(paragraph_14, stemmed_14, 14);
        ArrayList<String> p15 = combineStemmed(paragraph_15, stemmed_15, 15);
        ArrayList<String> p16 = combineStemmed(paragraph_16, stemmed_16, 16);
        ArrayList<String> kwSet = new ArrayList<String>();
        kwSet.add("Keyword Set");
        for (int i = 1; i < ((all.size() / 2) + 1); i++) {
            kwSet.add(all.get(i-1));
        }
        writeCSV(kwSet);
        writeCSV(featureVector(all, p1, "Paragraph 1"));
        writeCSV(featureVector(all, p2, "Paragraph 2"));
        writeCSV(featureVector(all, p3, "Paragraph 3"));
        writeCSV(featureVector(all, p4, "Paragraph 4"));
        writeCSV(featureVector(all, p5, "Paragraph 5"));
        writeCSV(featureVector(all, p6, "Paragraph 6"));
        writeCSV(featureVector(all, p7, "Paragraph 7"));
        writeCSV(featureVector(all, p8, "Paragraph 8"));
        writeCSV(featureVector(all, p9, "Paragraph 9"));
        writeCSV(featureVector(all, p10, "Paragraph 10"));
        writeCSV(featureVector(all, p11, "Paragraph 11"));
        writeCSV(featureVector(all, p12, "Paragraph 12"));
        writeCSV(featureVector(all, p13, "Paragraph 13"));
        writeCSV(featureVector(all, p14, "Paragraph 14"));
        writeCSV(featureVector(all, p15, "Paragraph 15"));
        writeCSV(featureVector(all, p16, "Paragraph 16"));
    }

    /**
     * Combines words that have common root words
     * @param paragraph
     * @param stemmed
     * @param num
     * @return
     */
    public static ArrayList<String> combineStemmed(ArrayList<String> paragraph, ArrayList<String> stemmed, int num){
        for(int i = 0; i < stemmed.size(); i++){
            String word1 = stemmed.get(i);
            int count = 1;
            for(int j = 0; j < stemmed.size(); j++){
                if(i == j) continue;
                String word2 = stemmed.get(j);
                if(word1.equals(word2)){
                    stemmed.remove(j);
                    if(paragraph.get(i).length() > paragraph.get(j).length()){
                        paragraph.set(i, paragraph.get(j));
                        paragraph.remove(j);
                    } else paragraph.remove(j);
                    count++;
                }
                else continue;
            }
            paragraph.add(Integer.toString(count));
        }
        return paragraph;
    }

    /**
     *  feature vector counts the number of creates a vector that contains
     *  the frequency in which a word in the entire text shows up
     *  in each paragraph
     *
     * @param allText
     * @param p
     * @param c1
     * @return
     */
    public static ArrayList<String> featureVector(ArrayList<String> allText, ArrayList<String> p, String c1) {
        ArrayList<String> kSet = new ArrayList<String>();
        ArrayList<String> pSet = new ArrayList<String>();
        ArrayList<String> frequency = new ArrayList<String>();
        ArrayList<String> vector = new ArrayList<String>();
        vector.add(c1);

        for (int i = 0; i < allText.size() / 2; i++) {
            kSet.add(allText.get(i));
        }
        for( int i = 1; i < kSet.size() + 1; i++){
            vector.add("0");
        }
        for (int i = 0; i < p.size() / 2; i++) {
            pSet.add(p.get(i));
        }
        for (int i = ((p.size() / 2)); i < p.size(); i++){
            frequency.add(p.get(i));
        }
        for(int i = 0; i < pSet.size(); i++){
            for(int j = 0; j < kSet.size(); j++){
                if(pSet.get(i).equals(kSet.get(j))) {
                    vector.set(j + 1, frequency.get(i));
                    break;
                }
            }
        }
        for(int i = 0; i < vector.size(); i++){
            System.out.print(vector.get(i) + ", ");
        }
        System.out.println("");
        System.out.println("");
        return vector;
    }

    /**
     *  Writes to csv file
     *  First row is keyWord set for the entire document
     *  all proceeding rows are frequency vectors for each paragraph
     * @param row
     */
    public static void writeCSV(ArrayList<String> row){
        String filePath = "TDM.csv";
        try{
            FileWriter fw = new FileWriter(filePath, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(bw);

            for(int i = 0; i < row.size(); i++){
                pw.print(row.get(i) + ", ");
            }
            pw.println("");
            pw.flush();
            pw.close();
        }
        catch(Exception e){
            System.out.println("Failed to save file");
        }
    }
}
