/*
 * Copyright (c) 2009-2010, Sergey Karakovskiy and Julian Togelius
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Mario AI nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package ch.idsia.agents.controllers;

import java.util.Random;

import ch.idsia.agents.Agent;
import ch.idsia.benchmark.mario.engine.GeneralizerLevelScene;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.engine.sprites.Sprite;
import ch.idsia.benchmark.mario.environments.Environment;

/**
 * Created by IntelliJ IDEA.
 * User: Sergey Karakovskiy, sergey.karakovskiy@gmail.com
 * Date: Apr 8, 2009
 * Time: 4:03:46 AM
 */

public class OwnAgentTest extends BasicMarioAIAgent implements Agent
{
int trueJumpCounter = 0;
int trueSpeedCounter = 0;
boolean jumpWatcher = false;
boolean oldJump = false;

public OwnAgentTest()
{
    super("OwnAgent");
    reset();
}

public void reset()
{
    action = new boolean[Environment.numberOfKeys];
    action[Mario.KEY_RIGHT] = true;
    int trueJumpCounter = 0;
    int trueSpeedCounter = 0;
    boolean jumpWatcher = false;
    boolean oldJump = false;
}

public boolean isObstacle(int r, int c){
	return getReceptiveFieldCellValue(r, c)==GeneralizerLevelScene.BRICK
			|| getReceptiveFieldCellValue(r, c)==GeneralizerLevelScene.BORDER_CANNOT_PASS_THROUGH
			|| getReceptiveFieldCellValue(r, c)==GeneralizerLevelScene.FLOWER_POT_OR_CANNON
			|| getReceptiveFieldCellValue(r, c)==GeneralizerLevelScene.LADDER;
}

public boolean isHole(int r, int c) {
	boolean out = true;
	for(int i = r; i < 19 ;++i) {
		out = out && (getReceptiveFieldCellValue(i, c) == GeneralizerLevelScene.BRICK
				   || getReceptiveFieldCellValue(i, c) == 0);
	}
	return out;
}

public boolean[] getAction()
{
	/*
	if(!isMarioAbleToJump)++trueJumpCounter;
	else trueJumpCounter = 0;
	if(isObstacle(marioEgoRow, marioEgoCol + 1) || isObstacle(marioEgoRow - 1, marioEgoCol + 1) || 
			getEnemiesCellValue(marioEgoRow, marioEgoCol + 2) != Sprite.KIND_NONE
			|| getEnemiesCellValue(marioEgoRow, marioEgoCol + 1) != Sprite.KIND_NONE
			|| (isHole(marioEgoRow, marioEgoCol + 1) && !isHole(marioEgoRow, marioEgoCol))){
		action[Mario.KEY_JUMP] = isMarioAbleToJump || !isMarioOnGround;
	}
	
	if(isHole(marioEgoRow, marioEgoCol + 1) && !isHole(marioEgoRow, marioEgoCol) && trueJumpCounter > 8 ) {
		action[Mario.KEY_RIGHT] = false;
		action[Mario.KEY_LEFT] = true;
		action[Mario.KEY_SPEED] = true;
	}else {
		action[Mario.KEY_RIGHT] = true;
		action[Mario.KEY_LEFT] = false;
		action[Mario.KEY_SPEED] = true;
	}
	
	/**/
	if(trueSpeedCounter>1 || !isMarioAbleToJump) {
		action[Mario.KEY_RIGHT]=false;
	}else {
		action[Mario.KEY_RIGHT]=true;
	}
	if(action[Mario.KEY_RIGHT] == true)trueSpeedCounter+= 6;
	else --trueSpeedCounter;
	
	jumpWatcher = (oldJump != action[Mario.KEY_JUMP]);
	
	if(isObstacle(marioEgoRow, marioEgoCol)) {
		action[Mario.KEY_UP] = true;
		action[Mario.KEY_JUMP] = true;
	}else {
		action[Mario.KEY_UP] = false;
	}
	
	//action[Mario.KEY_UP] = trueJumpCounter > 8;	//MAX JUMP
	
	oldJump = action[Mario.KEY_JUMP];
    return action;
}
}