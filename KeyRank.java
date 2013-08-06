
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * PageRankにKeyGraphを導入する（着手時の提案手法の実装）
 * 入力：テキストファイル（一行ずつ改行）
 * 出力：単語 + \s + rank値
 */

public class KeyRank {

	public static void main(String[] args) throws Exception{

		String filename = "/Users/ikedaayaka/Work/workspace/KeyGraph/TheHistoryOfSheetMusic.txt";//入力
		String noiseFilename = "/Users/ikedaayaka/Work/workspace/KeyGraph/noiselist.txt";//ノイズリスト
		String outFilename = "/Users/ikedaayaka/Work/workspace/KeyGraph/results/musickr.txt";//出力


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


		///////////土台ごとのランクを計算する////////////
		int T = 5;//土台の数
		int inIteration = 20;
		int outIteration = 40;
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

		double[][] rank = new double[T][N];//各土台の単語ごとにrankを保存する
		for(int t=0; t<T; t++){
			double sum = 0.0;
			for(int i=0; i<N; i++){
				rank[t][i] = 1.0/(double)N + Math.random(); //すべて足して１ + random
				sum += rank[t][i];
			}
			for(int i=0; i<N; i++){
				rank[t][i] = (double)rank[t][i]/sum;		
			}
		}

		double[][] r = new double[T][N];//補正値 ページのジャンプしやすさを管理する
		for(int t=0; t<T; t++){ //初期化
			for(int i=0; i<N; i++){
				r[t][i] = 1/(double)N;
			}
		}

		for(int ite=0; ite<outIteration; ite++){//rを更新して収束
			for(int it=0; it<inIteration; it++){//rankを更新して収束
				double[][] newRank = new double[T][N];
				for(int t=0; t<T; t++){
					for(int i=0; i<N; i++){
						for(int j=0; j<N; j++){
							newRank[t][i] += rank[t][j]*link[j][i];
						}	
						newRank[t][i] = (1.0-d)*r[t][i] + d*newRank[t][i];
					}
				}
				rank = newRank;
			}

			//新しいrの計算
			for(int i=0; i<N; i++){
				double sum = 0.0;
				for(int t=0; t<T; t++){
					sum += rank[t][i]*rank[t][i]*rank[t][i];
				}
				for(int t=0; t<T; t++){
					r[t][i] = rank[t][i]*rank[t][i]*rank[t][i]/sum;
				}
			}
			//rの正規化
			for(int t=0; t<T; t++){
				double sum = 0.0;
				for(int i=0; i<N; i++){
					sum += r[t][i];
				}
				for(int i=0; i<N; i++){
					r[t][i] = r[t][i]/sum;
				}
			}
		}


		///////////屋根ランクを計算する////////////
		int yaneIterator = 20;

		double[] yaneRank = new double[N];//土台ごとのrankの和を正規化
		double s = 0.0;
		for(int i=0; i<N; i++){
			for(int t=0; t<T; t++){
				yaneRank[i] += rank[t][i];
			}
			s += yaneRank[i];
		}
		for(int i=0; i<N; i++){
			yaneRank[i] = yaneRank[i]/s;
		}


		//////////キーワードランクを計算する////////////
		KeyRank kr = new KeyRank();
		double[] keyRank = new double[N];
		keyRank = yaneRank;
		for(int ite=0; ite<yaneIterator; ite++){
			keyRank = kr.rankCalc(keyRank, link);//ページランクの計算		
		}


		///////////rank順にファイルに書き出す////////////
		double max = 0.0;
		int maxid = 0;
		int S = keyRank.length;
		int[] sort = new int[S];
		double[] value = new double[S]; 
		for(int i=0; i<S; i++){
			for(int j=0; j<keyRank.length; j++){
				if(max<keyRank[j]){
					max = keyRank[j];
					maxid = j;
				}
			}
			sort[i] = maxid;
			value[i] = max;
			max = 0;
			keyRank[maxid] = 0.0;
		}
		PrintWriter pw = new PrintWriter(new FileWriter(outFilename));
		for(int i=0; i<sort.length; i++){
			//System.out.println(idMap.get(sort[i]) + " " + value[i]);
			pw.println(idMap.get(sort[i]) + " " + value[i]);
		}
		pw.close();
	}


	public double[][] linkCalc(int[][] co){
		int N = co.length;
		double[][] newCo = new double[N][N];
		double[][] link = new double[N][N];

		for(int i=0; i<N; i++){
			double sum=0;
			for(int j=0; j<N; j++){
				if(co[i][j] != 0){
					double ran = Math.random();
					newCo[i][j] = co[i][j] + ran;
				}
				sum += newCo[i][j];
			}
			for(int j=0; j<N; j++){
				link[i][j] = (double)newCo[i][j]/sum;		
			}
		}
		return link;
	}

	public double[] rankCalc(double[] rank, double[][] link){
		int N = rank.length;
		double[] newRank = new double[N];

		double d = 0.85;

		for(int i=0; i<N; i++){
			for(int j=0; j<N; j++){
				newRank[i] += rank[j]*link[j][i];		
			}
			newRank[i] = (1.0-d)/(double)N + d*newRank[i];
		}
		return newRank;
	}

}
