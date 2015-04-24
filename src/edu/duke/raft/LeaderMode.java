package edu.duke.raft;

import java.util.Timer;

public class LeaderMode extends RaftMode {
	private Timer heartbeatTimer;//When timer goes off, must send off heartbeat to other servers
  public void go () {
    synchronized (mLock) {
      int term = mConfig.getCurrentTerm();
      //Send out heartbeats to everyone
      sendHeartbeats();
      System.out.println ("S" + 
			  mID + 
			  "." + 
			  term + 
			  ": switched to leader mode.");
    }
  }

  //LOGIC: If candidate's term is greater, vote for it?
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
    	 int term = mConfig.getCurrentTerm();
    	 if (candidateTerm <= term){
        	  return term;
          }
      	  if (true){//TODO: Need to add condition that candidate’s log is at least as up-to-date as receiver’s log
      		  mConfig.setCurrentTerm(candidateTerm, candidateID);
      		  heartbeatTimer.cancel();
        	  RaftServerImpl.setMode(new FollowerMode());
        	  return 0;
      	  }
      	  else {
      		  mConfig.setCurrentTerm(candidateTerm, 0);
      		  heartbeatTimer.cancel();
      		  RaftServerImpl.setMode(new FollowerMode());
      		  return mConfig.getCurrentTerm();
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
         if (leaderTerm > term){
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
    	sendHeartbeats();
    }
  }
  
  //TODO: Check if this is correct implementation of a heartbeat
  private void sendHeartbeats(){
	  for (int i = 1; i<= mConfig.getNumServers(); i++){
    	  remoteAppendEntries(i, mConfig.getCurrentTerm(), mID, 10, 10, new Entry[0], mCommitIndex);//heartbeat?
      }
	  heartbeatTimer = scheduleTimer(HEARTBEAT_INTERVAL, 1);
  }
}
