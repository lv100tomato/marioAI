package ch.idsia.scenarios;
import java.util.*;
import java.math.*;

import ch.idsia.agents.controllers.BasicMarioAIAgent;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.engine.sprites.Sprite;
import ch.idsia.benchmark.mario.environments.*;
import ch.idsia.agents.Agent;

public class OwnMCAgent extends BasicMarioAIAgent implements Agent{
	static String name = "MCAgent";
	//前方2マスの縦何マスを取得するか
	public static final int width = 3;
	//取り得る行動の数
	public static final int numOfAction = 12;
	//J：ジャンプ　S：ファイア　R：右　L：左　D：下
	/*enum Action{
		J,
		S,
		R,
		L,
		D,
		JS,
		JR,
		JL,
		JD,
		JSR,
		JSL,
		NONE,
	}*/
	//毎フレームもっとも価値の高い行動をするが、確率epsilonで他の行動を等確率で選択
	public static final float epsilon = 0.005f;
	//もっとも良い選択の再現に使用
	private static int frameCounter = 0;
	//毎エピソードで選択した行動を全フレーム分とっておく
	public static List<Integer> actions;
	//学習中にもっとも良かった行動群
	public static List<Integer> best;
	//学習中にもっとも良かったスコア
	public static float bestScore;
	//マリオの周りの状態とマリオが地面についているか
	private static int state = 0;
	//前1マスに崖があるか 0 : ない 1 : ある
	private static int cliff = 0;
	//マリオがジャンプできるか 0 : できない 1 : できる
	private static int ableToJump = 0;
	//毎フレームで貪欲な選択をするかどうか
	public static boolean mode = false;
	//各エピソードで、ある状態である行動を取ったかどうか KeyOfMCはint4つでstate,cliff,ableToJump,action
	//valueのIntegerはこのMCでは使わない
	public static HashMap<KeyOfOwnMC,Integer> selected;
	//行動価値関数　これを基に行動を決める
	public static float[][][][] qValue;
	//各状態行動対におけるそれまで得た報酬の合計
	public static float[][][][] sumValue;
	//ある状態である行動を取った回数
	public static int[][][][] num;
	
	public static int reward = 0;
	public static int reward2 = 0;
	public static int reward3 = 0;
	
	public static final int nodes = (int)Math.pow(2.0,6 * width + 1);
	
	public static boolean darkness = false;
	
	//マリオの周りの地図を保持
	public static boolean[][][] map;
	//報酬となるコインの位置
	public static boolean[][] reCoin;
	
	//最初のフレームかどうか
	public static boolean first = true;
	//進んでいる方向
	public static boolean[] direction;
	
	public static int oldDistance;
	public static int topDistance;
	public static boolean firstStep = false;
	public static boolean deltaStep = false;
	
	public static void setMode(boolean b){
		mode = b;
	}
	public static void ini(){
		reward = 0;
		reward2 = 0;
		reward3 = 0;
		darkness = false;
		oldDistance = 0;
		topDistance = 0;
		firstStep=false;
		
		frameCounter = 0;
		reward = 0;
		selected.clear();
		actions.clear();
	}
	//使わない
	/*
	public static void setPolicy(){
		for(int i= 0; i < (int)Math.pow(2.0,4 * width + 1); ++i){
			for(int j = 0; j < 2; ++j){
				for(int k = 0; k < 2; ++k){
					float r = (float)(Math.random());
					int idx = 0;
					if(r < epsilon){
						float sum = 0;
						float d = epsilon / (float)numOfAction;
						sum += d;
						while(sum < r){
							sum += d;
							idx++;
						}
					}else{
						float max = -Float.MAX_VALUE;
						for(int t = 0; t < numOfAction; ++t){
							float q = qValue[state][cliff][ableToJump][t];
							if(q > max){
								max = q;
								idx = t;
							}
						}
					}
				}
			}
		}
	}
	*/
	//コンストラクタ
	public OwnMCAgent(){
		super(name);
		//nodes=(int)Math.pow(2.0,6 * width + 1);
		qValue = new float[nodes][2][2][numOfAction];
		sumValue = new float[nodes][2][2][numOfAction];
		num = new int[nodes][2][2][numOfAction];
		selected = new HashMap<KeyOfOwnMC,Integer>();
		for(int i = 0; i < nodes; ++i){
			for(int j = 0; j < 2; ++j){
				for(int k = 0; k < 2; ++k){
					for(int t = 0; t < numOfAction; ++t){
						qValue[i][j][k][t] = 0.0f;
						//一応全パターンは1回は試したいのである程度の値は持たせる
						sumValue[i][j][k][t] = 4096.0f;
						num[i][k][k][t] = 1;
					}
				}
			}
		}
		//System.out.println(OwnMCAgent.qValue[0][0][0][0]);
		actions = new ArrayList<Integer>();
		best = new ArrayList<Integer>();
		
		map = new boolean[19][19][3];
		for(int i=0;i<19;++i) {
			for(int j=0;j<19;++j) {
				map[i][j][0]=false;
				map[i][j][1]=false;
				map[i][j][2]=false;
			}
		}
		reCoin = new boolean[37][37];
		for(int i=0;i<37;++i) {
			for(int j=0;j<37;++j) {
				reCoin[i][j]=false;
			}
		}
		direction = new boolean[4];
		direction[0]=false;//上
		direction[1]=false;//下
		direction[2]=false;//左
		direction[3]=false;//右
	}
	//行動価値関数を取得
	public static float[][][][] getQ(){
		return qValue;
	}
	//行動価値関数を取得
	//学習した後に再現で使う
	public static void setQ(float[][][][] q){
		qValue = q;
	}
	//障害物を検出し、stateの各bitに0,1で格納
	//ここでマリオが得る情報をほとんど決めている
	//ついでにマリオが地面にいるかも取得
	public void detectObstacle(){
		state = 0;
		/*
		for(int j = 0; j < width; ++j){
			if(getEnemiesCellValue(marioEgoRow + j - 1,marioEgoCol + 1) != Sprite.KIND_NONE)
				state += (int)Math.pow(2,j);
		}
		for(int j = 0; j < width; ++j){
			if(getReceptiveFieldCellValue(marioEgoRow + j - 1,marioEgoCol + 1) != 0 && getReceptiveFieldCellValue(marioEgoRow + j - 1,marioEgoCol + 1) != 2)
				state += (int)Math.pow(2,width + j);
		}
		for(int j = 0; j < width; ++j){
			if(getEnemiesCellValue(marioEgoRow + j - 1,marioEgoCol + 2) != Sprite.KIND_NONE)
				state += (int)Math.pow(2, 2 * width + j);
		}
		for(int j = 0; j < width; ++j){
			if(getReceptiveFieldCellValue(marioEgoRow + j - 1,marioEgoCol + 2) != 0 && getReceptiveFieldCellValue(marioEgoRow + j - 1,marioEgoCol + 2) != 2)
				state += (int)Math.pow(2,3 * width + j);
		}
		/**/
		for(int j=0; j<width;++j) {
			for(int k=0; k<1;++k) {
				if(getEnemiesCellValue(marioEgoRow + j - 1,marioEgoCol + k+1) != Sprite.KIND_NONE)
					state += (int)Math.pow(2,width * 6 * k + j);
				if(getReceptiveFieldCellValue(marioEgoRow + j - 1,marioEgoCol + k+1) != 0 && getReceptiveFieldCellValue(marioEgoRow + j - 1,marioEgoCol + k+1) != 2)
					state += (int)Math.pow(2,width *(6*k+1) + j);
				if(reCoin[marioEgoRow + j - 1 + 9][marioEgoCol + k+1 + 9])
					state += (int)Math.pow(2,width *(6*k+2) + j);
				if(getEnemiesCellValue(marioEgoRow + j - 1,marioEgoCol - k-1) != Sprite.KIND_NONE)
					state += (int)Math.pow(2,width *(6*k+3) + j);
				if(getReceptiveFieldCellValue(marioEgoRow + j - 1,marioEgoCol - k-1) != 0 && getReceptiveFieldCellValue(marioEgoRow + j - 1,marioEgoCol - k-1) != 2)
					state += (int)Math.pow(2,width *(6*k+4) + j);
				if(reCoin[marioEgoRow + j - 1 + 9][marioEgoCol - k-1 + 9])
					state += (int)Math.pow(2,width *(6*k+5) + j);
			}
		}
		if(isMarioOnGround)
			state += (int)Math.pow(2, 4 * width);
	}
	//boolをintへ
	public int boolToInt(boolean b){
		return (b) ? 1 : 0;
	}
	//崖検出
	public void detectCliff(){
		
		boolean b = true;
		for(int i = 0; i < 10; ++i){
			if(getReceptiveFieldCellValue(marioEgoRow + i,marioEgoCol + 1) != 0){
				b = false;
				break;
			}
		}
		cliff = (b) ? 1 : 0;
	}
	//ソフトマックス手法
	//使わない
	/*
	public int chooseActionS(){
		float sum = 0.0f;
		int idx = 0;
		for(int i = 0; i < numOfAction; ++i){
			sum += Math.pow(Math.E,qValue[state][cliff][ableToJump][i] / 25f);
		}
		float r = (float)(Math.random());
		float f = 0.0f;
		for(int i = 0; i < numOfAction; ++i){
			f += Math.pow(Math.E,qValue[state][cliff][ableToJump][i] / 25f) / sum;
			if(f > r){
				idx = i;
				break;
			}
		}
		return idx;
	}*/
	//行動価値関数を基に行動選択
	public int chooseAction(){
		float r = (float)(Math.random());
		int idx = 0;
		if(r < epsilon){
			float sum = 0;
			float d = epsilon / (float)numOfAction;
			sum += d;
			while(sum < r){
				sum += d;
				idx++;
			}
		}else{
			float max = -Float.MAX_VALUE;
			for(int i = 0; i < numOfAction; ++i){
				float q = qValue[state][cliff][ableToJump][i];
				if(q > max){
					max = q;
					idx = i;
				}
			}
		}
		return idx;
	}
	//貪欲に行動を選択
	public int chooseActionG(){
		int idx = 0;
		float max = -Float.MAX_VALUE;
		for(int i = 0; i < numOfAction; ++i){
			float q = qValue[state][cliff][ableToJump][i];
			if(q > max){
				max = q;
				idx = i;
			}
		}
		return idx;
	}
	//行動選択前にactionを一旦全部falseにする
	public void clearAction(){
		for(int i = 0; i < Environment.numberOfKeys; ++i){
			action[i] = false;
		}
	}
	//int(0-11)をacitonにする
	public void intToAction(int n){
		if(n == 0 || (n > 4 && n < 11))
			action[Mario.KEY_JUMP] = true;
		if(n == 1 || n == 5 || n == 9 || n == 10)
			action[Mario.KEY_SPEED] = true;
		if(n == 2 || n == 6 || n == 9)
			action[Mario.KEY_RIGHT] = true;
		if(n == 3 || n == 7 || n == 10)
			action[Mario.KEY_LEFT] = true;
		if(n == 4 || n == 8)
			action[Mario.KEY_DOWN] = true;
	}
	public boolean[] getAction(){
		if(distancePassedPhys < (4096.0*timeSpent)/(timeSpent+timeLeft))darkness=true;
		
		deltaStep=firstStep;
		firstStep=firstStep||isMarioOnGround;
		direction = feelDirection(map);
		map = feelMap(map);
		updateCoin();
		
		detectObstacle();
		detectCliff();
		ableToJump = boolToInt(isMarioAbleToJump);
		clearAction();
		int currAction = 0;
		if(!mode){
			currAction = chooseAction();
			actions.add(currAction);
			intToAction(currAction);
			if(!selected.containsKey(new KeyOfOwnMC(state,cliff,ableToJump,currAction)))
				selected.put(new KeyOfOwnMC(state,cliff,ableToJump,currAction),1);	
			else
				selected.put(new KeyOfOwnMC(state,cliff,ableToJump,currAction), selected.get(new KeyOfOwnMC(state,cliff,ableToJump,currAction)) + 1);
		}
		else{
			//currAction = chooseActionG();
			if(frameCounter < best.size())
				currAction = best.get(frameCounter);
			intToAction(currAction);
		}
		frameCounter++;
		if(!darkness) {
		//if(action[Mario.KEY_RIGHT])++reward;
		reward2 = distancePassedCells;
		reward3 = distancePassedPhys;
		if(!firstStep)reward=distancePassedCells;
		/*
		if(direction[3])++reward;
		if(direction[2])--reward;
		/**/
		}
		first = false;
		return action;
	}
	
	public boolean[] feelDirection(boolean[][][] maps) {
		boolean[] output = {false,false,false,false};
		boolean[] zure = {false,false,false,false,false,false,false,false};
		//  -0+b
		//     c
		//- 012
		//0 3 4
		//+ 567
		//ar
		if(!first && firstStep) {
			/*
			int r=0;
			int c=0;
			for(int i=1;i<18;++i) {
				for(int j=1;j<18;++j) {
					if(maps[i][j][1] != (getReceptiveFieldCellValue(i,j) != 0  || getReceptiveFieldCellValue(i,j) == 2)) {
						if(maps[i][j][1] == (getReceptiveFieldCellValue(i-1,j) != 0  || getReceptiveFieldCellValue(i-1,j) == 2))--r;
						if(maps[i][j][1] == (getReceptiveFieldCellValue(i+1,j) != 0  || getReceptiveFieldCellValue(i+1,j) == 2))++r;
						if(maps[i][j][1] == (getReceptiveFieldCellValue(i,j-1) != 0  || getReceptiveFieldCellValue(i,j-1) == 2))--c;
						if(maps[i][j][1] == (getReceptiveFieldCellValue(i,j+1) != 0  || getReceptiveFieldCellValue(i,j+1) == 2))++c;
					}
				}
			}
			output[0]=r>0;
			output[1]=r<0;
			output[2]=c>0;
			output[3]=c<0;
			/**/
			int index = 0;
			int acceptCount=0;
			for(int a=-1;a<2;++a) {
				for(int b=-1;b<2;++b) {
					if(a!=0 || b!=0) {
						boolean fit = true;
						for(int i=1;i<18;++i) {
							for(int j=1;j<18;++j) {
								if(maps[i][j][1]!=(getReceptiveFieldCellValue(i+a,j+b) != 0 && getReceptiveFieldCellValue(i+a,j+b) != 2))fit = false;
							}
						}
						if(fit) {
							zure[index]=true;
							++acceptCount;
						}
						++index;
					}
				}
			}
			if(acceptCount>1) {
				System.err.print("何かがおかしい(");
				for(int i=0;i<8;++i) {
					if(zure[i])System.err.print(i+" ");
				}
				System.err.println(")");
				for(int i=1;i<18;++i) {
					for(int j=1;j<18;++j) {
						System.err.print((maps[i][j][1]?"*":"_")+((getReceptiveFieldCellValue(i,j) != 0 && getReceptiveFieldCellValue(i,j) != 2)?"*":"_"));
						
					}
					System.err.println("");
				}
			}
			else {
				//System.err.println("何もおかしくない");
				if(zure[0] || zure[1] || zure[2])output[1] = true;
				if(zure[0] || zure[3] || zure[5])output[3] = true;
				if(zure[2] || zure[4] || zure[7])output[2] = true;
				if(zure[5] || zure[6] || zure[7])output[0] = true;
			}
		}
		return output;
	}
	
	public boolean[][][] feelMap(boolean[][][] maps) {
		for(int i=0;i<19;++i) {
			for(int j=0;j<19;++j) {
				maps[i][j][0]= getEnemiesCellValue(i,j) != Sprite.KIND_NONE;
				maps[i][j][1]= getReceptiveFieldCellValue(i,j) != 0  && getReceptiveFieldCellValue(i,j) != 2;
			}
		}
		return maps;
	}
	
	public void updateCoin() {
		if(!firstStep)return;
		if(!deltaStep) {
			generateCoin();
			return;
		}
		boolean[][] oldReCoin = new boolean[37][37];
		for(int i=0;i<37;++i) {
			for(int j=0;j<37;++j) {
				oldReCoin[i][j]=reCoin[i][j];
			}
		}
		for(int d=0;d<4;++d) {
			if(direction[d]) {
				for(int i=0;i<37;++i) {
					for(int j=0;j<37;++j) {
						switch(d){
						case 0:
							if(i==0) {
								reCoin[i][j]=false;
							}else {
								reCoin[i][j]=oldReCoin[i-1][j];
							}
							break;
						case 1:
							if(i==36) {
								reCoin[i][j]=false;
							}else {
								reCoin[i][j]=oldReCoin[i+1][j];
							}
							break;
						case 2:
							if(j==0) {
								reCoin[i][j]=false;
							}else {
								reCoin[i][j]=oldReCoin[i][j-1];
							}
							break;
						case 3:
							if(j==36) {
								reCoin[i][j]=false;
							}else {
								reCoin[i][j]=oldReCoin[i][j+1];
							}
							break;
						}
					}
				}
			}
		}
		if(reCoin[18][18]) {
			generateCoin();
			if(!darkness)++reward;
		}
	}
	
	public void generateCoin() {
		//重みづけ
		int[][] weight=new int[19][19];
		for(int i=18;i>=0;--i) {
			for(int j=0;j<19;++j) {
				//地面かどうか
				if(getReceptiveFieldCellValue(i,j) != 0 && getReceptiveFieldCellValue(i,j) != 2)weight[i][j]=4096;
				//下が虚無かどうか
				else if(i==18)weight[i][j]=1024;
				else {
					if(getReceptiveFieldCellValue(i+1,j) != 0 && getReceptiveFieldCellValue(i+1,j) != 2)weight[i][j]=4;
					else {
						int min=8192;
						if(j!=0 )if(min>weight[i+1][j-1])min=weight[i+1][j-1];
						if(min>weight[i+1][j])min=weight[i+1][j];
						if(j!=18)if(min>weight[i+1][j+1])min=weight[i+1][j+1];
						if(min==10)min=40;
						weight[i][j]=min+1;
					}
				}
			}
		}
		
		for(int i=0;i<37;++i) {
			for(int j=0;j<37;++j) {
				reCoin[i][j]=false;
			}
		}
		
		//A*いくぞおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおおお
		boolean[][] aList = new boolean[19][19];
		int[][][] bList = new int[19][19][2];
		//初期化
		for(int i=0;i<19;++i) {
			for(int j=0;j<19;++j) {
				//調べた後かどうか
				aList[i][j]=false;
				//重みの合計
				bList[i][j][0] = -1;
				//前の頂点の方向
				bList[i][j][1] = -1;
				//012
				//3 5
				//678
			}
		}
		bList[9][9][0]=0;
		
		//A*
		//移動するときに通りそうなマスの重みの平均を枝の重みとする
		//マリオの位置(r.c)と(r-1,c)をマリオの占めるスペースとする
		//ゴールは右上端
		//一番右とのy座標が小さいほど軽くする(1マスあたり3)
		while(true) {
			//調べる頂点を選ぶ
			int[] exam = {-1,-1, -1};
			//             x, y,重み
			for(int i=0;i<19;++i) {
				for(int j=0;j<19;++j) {
					//重みが既にわかっている かつ 調べ終わった後でない
					if(bList[i][j][0]!=-1 && !aList[i][j]) {
						//今のマスがより軽ければ置き換え
						if(exam[2] == -1 || exam[2]+(18-exam[1])*3 > bList[i][j][0]+(18-j)*3) {
							exam[0]=i;
							exam[1]=j;
							exam[2]=bList[i][j][0];
						}
					}
				}
			}
			
			//もし選んだ頂点が右上端なら終了
			if(exam[0]==0 && exam[1]==18)break;
			
			//周りのマスに操作
			int w=bList[exam[0]][exam[1]][0];
			for(int i=-1;i<2;++i) {
				for(int j=-1;j<2;++j) {
					//自身のマスは調べない
					if(i!=0 || j!=0) {
						//配列外のマスは調べない
						if(exam[0]+i>=0 && exam[0]+i<19 && exam[1]+j>=0 && exam[1]+j<19) {
							int sumw=0;
							if(i!=1) {
								sumw = w + boxAverageForAStar(weight,exam[0]+i-1,exam[1]+j,exam[0],exam[1]);
							}else{
								sumw = w + boxAverageForAStar(weight,exam[0]+i,exam[1]+j,exam[0]-1,exam[1])/2;
							}
							//既にあるものよりも軽ければ入れ替え
							if(bList[exam[0]+i][exam[1]+j][0] == -1 || bList[exam[0]+i][exam[1]+j][0] > sumw) {
								bList[exam[0]+i][exam[1]+j][0] = sumw;
								bList[exam[0]+i][exam[1]+j][1] = (1-i)*3+1-j;
							}
						}
					}
				}
			}
			//操作済みにする
			aList[exam[0]][exam[1]]=true;
		}
		
		//右上端から辿ってコインを置いていく
		int nowR=0;
		int nowC=18;
		/**/
		boolean[][] route = new boolean[19][19];
		for(int i=0;i<19;++i) {
			for(int j=0;j<19;++j) {
				route[i][j]=false;
			}
		}
		/**/
		while(true) {
			/**/
			route[nowR][nowC]=true;
			/**/
			if(nowR==9 && nowC==9)break;
			reCoin[9+nowR][9+nowC]=true;
			if(bList[nowR][nowC][1]<0)System.err.println("もう後戻りはできない");
			int nextR= nowR + bList[nowR][nowC][1]/3 - 1;
			int nextC= nowC + bList[nowR][nowC][1]%3 - 1;
			nowR = nextR;
			nowC = nextC;
		}
		/*
		for(int i=0;i<19;++i) {
			for(int j=0;j<19;++j) {
				String a = "";
				if(route[i][j])a="0";
				else a=".";
				System.out.print(a);
			}
			System.out.println("");
		}
		System.out.println("");
		/**/
		return;
	}
	
	private int boxAverageForAStar(int[][] weight,int sx,int sy,int ex,int ey){
		if(sx>ex) {
			int swap=sx;
			sx=ex;
			ex=swap;
		}
		if(sy>ey) {
			int swap=sy;
			sy=ey;
			ey=swap;
		}
		
		float sum=0;
		int num=0;
		for(int i=sx;i<=ex;++i) {
			for(int j=sy;j<=ey;++j) {
				++num;
				sum+=safelyPickUp(weight,i,j,1024);
			}
		}
		return (int)(sum/num);
	}
	
	private int safelyPickUp(int[][] numbers,int x,int y,int def) {
		if(x<0 || x>=numbers.length || y<0 || y>=numbers[0].length) {
			return def;
		}else {
			return numbers[x][y];
		}
	}
}
