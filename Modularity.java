

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * モジュラリティ（コミュニティ分割の質を示す）Qが最も高い構造を探し出す
 * 入力：テキストファイル（一行ごとに改行）
 */

public class Modularity {

	public static void main(String[] args) throws Exception{

		String filename = "/Users/ikedaayaka/Work/workspace/KeyGraph/TheHistoryOfSheetMusic.txt";//入力
		String noiseFilename = "/Users/ikedaayaka/Work/workspace/KeyGraph/noiselist.txt";//ノイズリスト


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

		////////////////Modularity//////////////////
		
		int[] k = new int[N];//各単語の次数
		int M = 0; //枝の総数
		for(int i=0; i<N; i++){
			for(int j=0; j<N; j++){
				if(co[i][j] != 0){
					k[i] += 1;
					M += 1;
				}
			}
		}
		M = M/2;//＜ーー 二重に数えているので、/２

		LinkedList<TreeSet<Integer>> com = new LinkedList<TreeSet<Integer>>(); //各Nでの親コミュニティ
		LinkedList<LinkedList<TreeSet<Integer>>> Com = new LinkedList<LinkedList<TreeSet<Integer>>>();//各Nでのmaxコミュニティ
		for(int i=0; i<N ;i++){
			TreeSet<Integer> c = new TreeSet<Integer>();
			c.add(i);
			com.add(c);
		}
		//Comダミー
		TreeSet<Integer> d = new TreeSet<Integer>();
		d.add(88);
		LinkedList<TreeSet<Integer>> dummy = new LinkedList<TreeSet<Integer>>();
		dummy.add(d);
		
		double[] Q = new double[N];//各NでのmaxQ
		double q = 0.0;
		double mq = 0.0;
		int m = 0; //統合前にciとcjをつないでいた枝の値の和
		int k1 = 0, k2 = 0; //次数和
		int a = 0;
		int maxi, maxj;//Qが最大となるi, jを保存

		for(int n=0; n<N-1; n++){
			double maxq = -1*Double.MAX_VALUE;//最も小さい数
			maxi = 0;
			maxj = 0;
			Com.add(dummy);//先頭にダミーを挿入	
			System.out.println("コミュニティ数" + (N-n));
			
			//どの２つのコミュニティを結合すれば最もQが大きくなるか計算する
			for(int i=0; i<com.size(); i++){
				for(int j=i+1; j<com.size(); j++){

					TreeSet<Integer> t1 = com.get(i);
					Iterator<Integer> it1 = t1.iterator();
					while(it1.hasNext()){
						int v1 = it1.next();
						k1 += k[v1];
						TreeSet<Integer> t2 = com.get(j);
						Iterator<Integer> it2 = t2.iterator();
						while(it2.hasNext()){
							int v2 = it2.next();
							if(a==0) k2 += k[v2];
							m+= co[v1][v2];
						}
						a = 99;
					}

					q = mq + (double)m/2/M - (double)k1*k2/(double)(2*M*M);
	
					if(q > maxq){//もしモジュラリティがそれ以前の最大値より大きくなれば
						maxq = q;
						Q[n] = maxq;
						
						maxi = i;
						maxj = j;
						
					}
					q = 0.0;
					m = 0;
					k1 = 0;
					k2 = 0;
				}
			}
			
			//コミュニティ数N-nにおいて、Qを最大にするのは maxi番目、maxj番目のコミュニティを結合したとき
			LinkedList<TreeSet<Integer>> cm = new LinkedList<TreeSet<Integer>>();
			for(TreeSet<Integer> ts: com)cm.add((TreeSet<Integer>) ts.clone());
			Iterator<Integer> it = com.get(maxj).iterator();
			while(it.hasNext()){
				TreeSet<Integer> ts = cm.get(maxi);
				ts.add(it.next());
			}
			cm.remove(maxj);
			Com.set(n, cm);//Comのn番目を、cmで置き換え
			
			//comを、コミュニティ数N-nにおける最良構造（cm）に置き換え
			com.clear();
			for(TreeSet<Integer> ts: cm)com.add((TreeSet<Integer>) ts.clone());
			
			mq = maxq;
		}

		/////////////コミュニティ数0〜Nのうち、もっともモジュラリティQが高い構造を表示する////////
		double maxQ = 0.0;
		int maxq = 0;
		LinkedList<TreeSet<Integer>> maxCom = new LinkedList<TreeSet<Integer>>();
		 
		for(int n=0; n<N-1; n++){
			if(Q[n] > maxQ){
				maxQ = Q[n];
				maxq = n;
			}
		}
		maxCom = Com.get(maxq);

		System.out.println(maxCom);
		System.out.println("----------");
		for(int i=0; i<maxCom.size(); i++){
			Iterator<Integer> ite = maxCom.get(i).iterator();
			while(ite.hasNext()){
				System.out.print(idMap.get(ite.next()) + ", ");
			}
			System.out.println();
		}
		System.out.println("**********");	

	}
}
