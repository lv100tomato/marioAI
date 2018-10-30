package ch.idsia.scenarios;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import ch.idsia.benchmark.mario.engine.GlobalOptions;
import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.benchmark.tasks.BasicTask;
import ch.idsia.benchmark.tasks.LearningTask;
import ch.idsia.tools.EvaluationInfo;
import ch.idsia.tools.MarioAIOptions;
import ch.idsia.utils.wox.serial.Easy;
import ch.idsia.agents.Agent;
import ch.idsia.agents.LearningAgent;

public class LearningWithOwnMC implements LearningAgent{
	//private Agent agent;
	private OwnMCAgent agent;
	private String name = "LearningWithOwnMC";
	//目標値(4096.0はステージの右端)
	private float goal = 4096.0f;
	private String args;
	//試行回数
	private int numOfTrial = 50000;
	//コンストラクタ
	public LearningWithOwnMC(String args){
		this.args = args;
		agent = new OwnMCAgent();
	}
	//学習部分
	//1000回学習してその中でもっとも良かったものをリプレイ
	public void learn(){
		for(int i = 0; i < numOfTrial; ++i){
			//目標値までマリオが到達したらshowして終了
			if(run() >= 4096.0f){
				show();
				break;
			}
			if(i % 1000 == 999)
				show();
		}

		try{
			//学習した行動価値関数を書き込み
			File f = new File("MonteCarlo.txt");
			f.createNewFile();
			BufferedWriter bw = new BufferedWriter(new FileWriter(f));
			for(int i = 0; i < Math.pow(2.0, OwnMCAgent.width * 4 + 1); ++i){
				for(int j = 0; j < 2; ++j){
					for(int k = 0;k < 2; ++k){
						for(int t = 0; t < OwnMCAgent.numOfAction; ++t){
							bw.write(String.valueOf(OwnMCAgent.qValue[i][j][k][t]));
							bw.newLine();
						}
					}
				}
			}
		}
		catch(IOException e){
		    System.out.println(e);
		}
	}
	//リプレイ
	public void show(){
		OwnMCAgent.ini();
		MarioAIOptions marioAIOptions = new MarioAIOptions();
		BasicTask basicTask = new BasicTask(marioAIOptions);

		/* ステージ生成 */
		marioAIOptions.setArgs(this.args);
		OwnMCAgent.setMode(true);


	    /* プレイ画面出力するか否か */
	    marioAIOptions.setVisualization(true);
		/* MCAgentをセット */
		marioAIOptions.setAgent(agent);
		basicTask.setOptionsAndReset(marioAIOptions);

		if ( !basicTask.runSingleEpisode(1) ){
			System.out.println("MarioAI: out of computational time"
			+ " per action! Agent disqualified!");
		}

		/* 評価値(距離)をセット */
		EvaluationInfo evaluationInfo = basicTask.getEvaluationInfo();
		//報酬取得
		//float reward = evaluationInfo.distancePassedPhys;
		float reward = agent.reward/**/ - agent.reward2/**/;
		System.out.println("報酬は" + reward);
	}
	//学習
	//画面に表示はしない
	public float run(){
		OwnMCAgent.ini();
		/* MCAgentをプレイさせる */
		MarioAIOptions marioAIOptions = new MarioAIOptions();
		BasicTask basicTask = new BasicTask(marioAIOptions);

		/* ステージ生成 */
		marioAIOptions.setArgs(this.args);
		OwnMCAgent.setMode(false);


	    /* プレイ画面出力するか否か */
	    marioAIOptions.setVisualization(false);
		/* MCAgentをセット */
		marioAIOptions.setAgent(agent);
		basicTask.setOptionsAndReset(marioAIOptions);

		if ( !basicTask.runSingleEpisode(1) ){
			System.out.println("MarioAI: out of computational time"
			+ " per action! Agent disqualified!");
		}

		/* 評価値(距離)をセット */
		EvaluationInfo evaluationInfo = basicTask.getEvaluationInfo();
		//報酬取得
		//float reward = evaluationInfo.distancePassedPhys;
		float reward = agent.reward;
		//reward -= (evaluationInfo.marioStatus == 0) ? 1000 : 0;
		System.out.println(reward);
		System.out.println("      "+(reward/**/ - agent.reward2/**/));
		//ベストスコアが出たら更新
		if(reward > OwnMCAgent.bestScore){
			OwnMCAgent.bestScore = reward;
			OwnMCAgent.best = new ArrayList<Integer>(OwnMCAgent.actions);
		}
		Iterator<KeyOfOwnMC> itr = OwnMCAgent.selected.keySet().iterator();
		//価値関数を更新
		while(itr.hasNext()){
			KeyOfOwnMC key = (KeyOfOwnMC)itr.next();
			OwnMCAgent.sumValue[key.getState()][key.getCliff()][key.getAbleToJump()][key.getAction()]+= reward;
			OwnMCAgent.num[key.getState()][key.getCliff()][key.getAbleToJump()][key.getAction()]++;
			OwnMCAgent.qValue[key.getState()][key.getCliff()][key.getAbleToJump()][key.getAction()] =
					OwnMCAgent.sumValue[key.getState()][key.getCliff()][key.getAbleToJump()][key.getAction()]
							/ (float)OwnMCAgent.num[key.getState()][key.getCliff()][key.getAbleToJump()][key.getAction()];
		}
		//return reward;
		return evaluationInfo.distancePassedPhys;
	}
	//////////////////////////////ここからは必要なし//////////////////////////////
	@Override
	public boolean[] getAction() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void integrateObservation(Environment environment) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void giveIntermediateReward(float intermediateReward) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setObservationDetails(int rfWidth, int rfHeight, int egoRow, int egoCol) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void giveReward(float reward) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void newEpisode() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setLearningTask(LearningTask learningTask) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public Agent getBestAgent() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void init() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setEvaluationQuota(long num) {
		// TODO Auto-generated method stub
		
	}
}