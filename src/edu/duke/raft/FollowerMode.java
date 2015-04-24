package edu.duke.raft;

import java.util.Random;
import java.util.Timer;

public class FollowerMode extends RaftMode {
	private int timeout;
	private Timer heartbeatTimer;
	
  public void go () {
    synchronized (mLock) {
      int term = mConfig.getCurrentTerm();
      //Set up timer
      Random myRandom = new Random();
      timeout = ELECTION_TIMEOUT_MIN + myRandom.nextInt(ELECTION_TIMEOUT_MAX - ELECTION_TIMEOUT_MIN);
      heartbeatTimer = scheduleTimer(timeout, 1);
      System.out.println ("S" + 
			  mID + 
			  "." + 
			  term + 
			  ": switched to follower mode.");
    }
  }
  
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
      int term = mConfig.getCurrentTerm ();
      if (candidateTerm < term){
    	  return term;
      }
      else {
    	  if (true){//TODO: Need to add condition that candidate’s log is at least as up-to-date as receiver’s log
    		  mConfig.setCurrentTerm(candidateTerm, candidateID);
    		  if (mConfig.getVotedFor() == candidateID){
    			  heartbeatTimer.cancel();
    	          heartbeatTimer = scheduleTimer(timeout, 1);
    			  return 0;
    		  }
    	  }
    	  return term;
      }
    }
  }
  

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
        if (leaderTerm < term){
      	  return term;
        }
        else {
          heartbeatTimer.cancel();
          heartbeatTimer = scheduleTimer(timeout, 1);
          //TODO: Do a log check, and if consistent, append, if inconsistent, move back entry index
      	  return 0;
        }
    }
  }  

  // @param id of the timer that timed out
  public void handleTimeout (int timerID) {
    synchronized (mLock) {
    	RaftServerImpl.setMode(new CandidateMode());
    }
  }
}

