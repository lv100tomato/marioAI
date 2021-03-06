package ch.idsia.scenarios;
import java.util.*;

public final class KeyOfOwnMC {

	    private final int state;
	    private final int cliff;
	    private final int ableToJump;
	    private final int action;

	    public KeyOfOwnMC(int state, int cliff, int ableToJump,int action) {
	        this.state = state;
	        this.cliff = cliff;
	        this.ableToJump = ableToJump;
	        this.action = action;
	    }

	    @Override
	    public boolean equals(Object obj) {
	        if (obj instanceof KeyOfOwnMC) {
	        	KeyOfOwnMC key = (KeyOfOwnMC) obj;
	            return this.state == key.state && this.cliff == key.cliff
	            		&& this.ableToJump == key.ableToJump && this.action == action;
	        } else {
	            return false;
	        }
	    }

	    @Override
	    public int hashCode() {
	        return state + cliff + ableToJump + action;
	    }
	    public int getState(){
	    	return state;
	    }
	    public int getCliff(){
	    	return cliff;
	    }
	    public int getAbleToJump(){
	    	return ableToJump;
	    }
	    public int getAction(){
	    	return action;
	    }
}
