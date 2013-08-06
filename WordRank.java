
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * PageRankアルゴリズムによってキーワードの重要度を計算する
 * 入力：テキストファイル（一文ずつ改行）
 * 出力：単語 + \s + rank値
 */

public class WordRank {

	public static void main(String[] args) throws Exception{
		
		String filename = "/Users/ikedaayaka/Work/workspace/KeyGraph/TheHistoryOfSheetMusic.txt";//入力
		String noiseFilename = "/Users/ikedaayaka/Work/workspace/KeyGraph/noiselist.txt";//ノイズリスト
		String outFilename = "/Users/ikedaayaka/Work/workspace/KeyGraph/results/musicwr.txt";//出力

		
		///////////準備 語同士の共起度を計算する////////////

		//文書 < 行ごとの単語リスト >
		LinkedList<LinkedList<Integer>> D = new LinkedList<LinkedList<Integer>>();
		//ノイズリスト(集合)
		HashSet<String> nolist = new HashSet<String>();
		//対応表
		TreeMap<String, Integer> wordMap = new TreeMap<String, Integer>();//単語 : ID
		TreeMap<Integer, String> idMap = new TreeMap<Integer, String>();//ID : 単語


		//ストップワードをノイズリストnolistに格納
		BufferedReader br = new BufferedReader(new FileReader(noiseFilename));
		String line;
		while((line = br.readLine()) != null){
			nolist.add(line);
		}
		br.close();

		//一行ごとにストップワードを除去し、文書Dに追加する
		br = new BufferedReader(new FileReader(filename));
		while((line = br.readLine()) != null){
			String[] wordList = line.split(" ");
			if(wordList.length<=1){
				continue;
			}
			LinkedList<Integer> cleanWordList = new LinkedList<Integer>(); //ノイズでない単語
			for(int i=0; i<wordList.length; i++){
				String word = wordList[i].toLowerCase(); //すべて小文字にする
				////////////////////////
				//
				//   stemming 熟語処理
				//
				Pattern p = Pattern.compile("[\\.|,]$");//ピリオド、カンマの削除
				Matcher m = p.matcher(word);
				word = m.replaceAll("");
				if(!nolist.contains(word) && word!=""){
					Integer id = wordMap.get(word);
					if(id == null){
						id = wordMap.size();
						wordMap.put(word, id);
						idMap.put(id, word);
						System.out.println(id + "\"" + word + "\"");
					}
					cleanWordList.add(id);
				}
			}
			D.add(cleanWordList);
		}
		br.close();

		int N = wordMap.size();//単語の数

		//語同士の共起度（共に出てくる文の数）
		int[][] co = new int[N][N];
		for(int i=0; i<D.size(); i++){//i:各文で
			//一文における語wの出現回数
			int[] ws = new int[N];
			for(int j=0; j<D.get(i).size(); j++){//j:各単語で
				for(int m=0; m<N; m++){//m:単語のID
					if(D.get(i).get(j) == m){
						ws[m]++;
					}
				}
			}
			for(int m=0; m<N; m++){
				for(int n=0; n<N; n++){
					if(m!=n){//自分以外のとき
						co[m][n] += ws[m] * ws[n];
					}
				}
			}
		}
		
		
		///////////ランクを計算する////////////
		int iteration = 50;
		double d = 0.85;//ダンピングファクター
		
		double[][] link = new double[N][N];//リンクごとの重みを保存する 一単語から張られるリンクの総和は１となる
		for(int i=0; i<N; i++){
			double sum=0;
			for(int j=0; j<N; j++){
				sum += co[i][j];
			}
			for(int j=0; j<N; j++){
				link[i][j] = (double)co[i][j]/sum;
			}
		}

		double[] rank = new double[N];//単語ごとのrankを保存する
		for(int i=0; i<N; i++){
			rank[i] = 1.0/(double)N; //すべて足すと１
		}

		//計算中心部
		for(int ite=0; ite<iteration; ite++){
			double[] newRank = new double[N];
			for(int i=0; i<N; i++){
				for(int j=0; j<N; j++){
					newRank[i] += rank[j]*link[j][i];		
				}
				newRank[i] = (1.0-d)/(double)N + d*newRank[i];
			}
			rank = newRank;
		}

		
		///////////rank順にファイルに書き出す////////////
		double max = 0.0;
		int maxid = 0;
		int S = rank.length;
		int[] sort = new int[S];
		double[] value = new double[S]; 
		for(int i=0; i<S; i++){
			for(int j=0; j<rank.length; j++){
				if(max<rank[j]){
					max = rank[j];
					maxid = j;
				}
			}
			sort[i] = maxid;
			value[i] = max;
			max = 0;
			rank[maxid] = 0.0;
		}
		
		PrintWriter pw = new PrintWriter(new FileWriter(outFilename));
		for(int i=0; i<sort.length; i++){
			//System.out.println(idMap.get(sort[i]) + " " + value[i]);
			pw.println(idMap.get(sort[i]) + " " + value[i]);
		}
		pw.close();
		
	}
}