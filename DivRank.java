

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * 多様性を考慮し、ノードに重みをつける
 * 入力：テキストファイル（一行ごとに改行）
 * 出力：単語 + \t + rank値
 */

public class DivRank {

	public static void main(String[] args) throws Exception{

		String filename = "/Users/ikedaayaka/Work/workspace/KeyGraph/TheHistoryOfSheetMusic.txt";//入力
		String noiseFilename = "/Users/ikedaayaka/Work/workspace/KeyGraph/noiselist.txt";//ノイズリスト
		String outFilename = "/Users/ikedaayaka/Work/workspace/PageRank/results3/drmusic.txt";//出力


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

		////////////////DivRank//////////////////
		double d = 0.85;//ダンピングファクター
		double a = 0.25;//自ノード以外へのジャンプ確率
		int iteration = 100;

		double[][] w = new double[N][N];//エッジの重み
		for(int i=0; i<N; i++){
			double sum=0;
			for(int j=0; j<N; j++){
				sum += co[i][j];
			}
			for(int j=0; j<N; j++){
				w[i][j] = (double)co[i][j]/sum;//正規化	
			}
		}

		int[] deg = new int[N];//次数
		for(int u=0; u<N; u++){
			for(int v=0; v<N; v++){
				if(co[u][v] != 0) deg[u]+= 1;
			}
		}

		/*
		double[] wv = new double[N];//vに向かうEの重みの総和
		for(int v=0; v<N; v++){
			for(int u=0; u<N; u++){
				wv[v] += w[u][v];
			}
		}*/

		double[][] p0 = new double[N][N];//基本の移動確率
		for(int u=0; u<N; u++){
			for(int v=0; v<N; v++){
				if(u==v){//自ノードに遷移
					p0[u][v] = 1-a;
				}else{
					p0[u][v] = (double)w[u][v]/deg[u]*a;
				}
			}
		}

		double r = (double)1/N;
		double[] p = new double[N];//単語ごとのランクを保存する
		for(int u=0; u<N; u++){
			p[u] = (double)1/N;
		}

		//計算
		for(int t=0; t<iteration; t++){
			System.out.println(t);
			double[] newP = new double[N];
			for(int v=0; v<N; v++){
				for(int u=0; u<N; u++){
					double du = 0.0;//正規化のための値
					for(int dv=0; dv<N; dv++){
						du += p0[u][dv]*p[dv];
					}
					newP[v] += ((1-d)*r + (double)d*p0[u][v]*p[v]/du)*p[u];//p(u,v)*pT-1(u)
				}
			}
			p = newP;
		}


		/////////////rank順にファイルに書き出し////////
		double max = 0.0;
		int maxid = 0;
		int L = p.length;
		int[] sort = new int[L];
		double[] value = new double[L]; 
		for(int i=0; i<L; i++){
			for(int j=0; j<p.length; j++){
				if(max<p[j]){
					max = p[j];
					maxid = j;
				}
			}
			sort[i] = maxid;
			value[i] = max;
			max = 0;
			p[maxid] = 0.0;
		}
		
		PrintWriter pw = new PrintWriter(new FileWriter(outFilename));
		for(int i=0; i<sort.length; i++){
			pw.println(idMap.get(sort[i]) + " " + value[i]);
		}

		pw.flush();
		pw.close();
	}
}
