package edu.duke.raft;

import java.util.Random;
import java.util.Timer;

public class CandidateMode extends RaftMode {
	private int timeout;
	private Timer heartbeatTimer; //To check if election has timed out
	private Timer checkTimer; //To check if it has won/if the servers have responded.
  public void go () {
    synchronized (mLock) {
      //Increment of current term
      mConfig.setCurrentTerm(mConfig.getCurrentTerm() + 1, mID);
      int term = mConfig.getCurrentTerm();
      Random myRandom = new Random();
      //Set up check timer
      checkTimer = scheduleTimer(HEARTBEAT_INTERVAL, 2);
      //Set up election timer
      timeout = ELECTION_TIMEOUT_MIN + myRandom.nextInt(ELECTION_TIMEOUT_MAX - ELECTION_TIMEOUT_MIN);
      heartbeatTimer = scheduleTimer(timeout, 1);
      //Ask for votes from everyone
      for (int i = 1; i<= mConfig.getNumServers(); i++){
    	  this.remoteRequestVote(i, term, mID, mLog.getLastIndex(), mLog.getLastTerm());
      }
      
      System.out.println ("S" + 
			  mID + 
			  "." + 
			  term + 
			  ": switched to candidate mode.");
    }
  }

  //LOGIC: If term is greater than my term, then convert to a follower. If additionally, the candidate's log is as good as yours, vote for them.
  // @param candidate’s term
  // @param candidate requesting vote
  // @param index of candidate’s last log entry
  // @param term of candidate’s last log entry
  // @return 0, if server votes for candidate; otherwise, server's
  // current term 
  public int requestVote (int candidateTerm,
			  int candidateID,
			  int lastLogIndex,
			  int lastLogTerm) {
    synchronized (mLock) {
    	if (candidateID == mID && candidateTerm == mConfig.getCurrentTerm()){
    		return 0;
    	}
    	int term = mConfig.getCurrentTerm ();
        if (candidateTerm <= term){
      	  return term;
        }
        else {
      	  if (true){//TODO: Need to add condition that candidate’s log is at least as up-to-date as receiver’s log
      		  mConfig.setCurrentTerm(candidateTerm, candidateID);
      		  heartbeatTimer.cancel();
      		  checkTimer.cancel();
        	  RaftServerImpl.setMode(new FollowerMode());
        	  return 0;
      	  }
      	  else {
      		  mConfig.setCurrentTerm(candidateTerm, 0);
      		  heartbeatTimer.cancel();
      		  checkTimer.cancel();
      		  RaftServerImpl.setMode(new FollowerMode());
      		  return mConfig.getCurrentTerm();
      	  }
        }
    }
  }
  
//LOGIC: Don't ever append entries, but if the requester's term is bigger than yours, revert to follower mode.
  // @param leader’s term
  // @param current leader
  // @param index of log entry before entries to append
  // @param term of log entry before entries to append
  // @param entries to append (in order of 0 to append.length-1)
  // @param index of highest committed entry
  // @return 0, if server appended entries; otherwise, server's
  // current term
  public int appendEntries (int leaderTerm,
			    int leaderID,
			    int prevLogIndex,
			    int prevLogTerm,
			    Entry[] entries,
			    int leaderCommit) {
    synchronized (mLock) {
      int term = mConfig.getCurrentTerm ();
      if (leaderTerm >= term){
    	  checkTimer.cancel();
    	  heartbeatTimer.cancel();
    	  mConfig.setCurrentTerm(leaderTerm, 0);
    	  RaftServerImpl.setMode(new FollowerMode());
      }
      return mConfig.getCurrentTerm();
    }
  }

  // @param id of the timer that timed out
  public void handleTimeout (int timerID) {
    synchronized (mLock) {
    	int[] responses = RaftResponses.getVotes(mConfig.getCurrentTerm());
    	if (timerID == 2){
    		for (int i = 0; i < responses.length; i++){
    			if (responses[i] == -1){
    				remoteRequestVote(i+1, mConfig.getCurrentTerm(), mID, mLog.getLastIndex(), mLog.getLastTerm());
    			}
    		}
    	}
    	int counter = 0;
    	for (int response : responses){
    		if (response == 0){
    			counter ++;
    		}
    	}
    	if (counter >= mConfig.getNumServers()/2){
    		heartbeatTimer.cancel();
    		checkTimer.cancel();
    		RaftServerImpl.setMode(new LeaderMode());
    	}
    	else if (timerID == 1){
    		heartbeatTimer.cancel();
    		checkTimer.cancel();
    		RaftServerImpl.setMode(new CandidateMode());
    	}
    }
  }
}
