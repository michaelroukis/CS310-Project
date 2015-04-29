package edu.duke.raft;

import java.util.Random;
import java.util.Timer;

public class CandidateMode extends RaftMode {
	private int timeout;
	private Timer electionTimer; //To check if election has timed out
	private Timer winCheckTimer; // Check if we won, rather quickly so that we become a Leader in time.
  
	public void go () {

		synchronized (mLock) {

			//Increment of current term
			int term = mConfig.getCurrentTerm() + 1;
			mConfig.setCurrentTerm(term, mID);
			
			//Set up election timer
			Random myRandom = new Random();
			timeout = ELECTION_TIMEOUT_MIN + myRandom.nextInt(ELECTION_TIMEOUT_MAX - ELECTION_TIMEOUT_MIN);
			electionTimer = scheduleTimer(timeout, 1);
			
			// Set up winCheck timer
			winCheckTimer = scheduleTimer(1, 2);
			
			//Ask for votes from everyone
			for (int i = 1; i<= mConfig.getNumServers(); i++){
				
				// Check for self-voting here, not below
				if (i != mID) {
					this.remoteRequestVote(i, term, mID, mLog.getLastIndex(), mLog.getLastTerm());
				}
				
			}

			System.out.println ("S" + 
					mID + 
					"." + 
					term + 
					": switched to candidate mode.");
		}
		
	}

	// Waiting for heartbeat from Leader in append. Otherwise, deny all votes. We are a candidate!
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
    	return mConfig.getCurrentTerm();
    }
    
  }
  
//LOGIC: A majority leader has been established if leaderTerm >= our term. Submit like the Follower you are.
  		// If less than, ignore. Always append failure to respond to ensure leader makes sure we append as future Followers.
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
      
      if (leaderTerm >= term) {
    	  
    	  electionTimer.cancel();
          
          mConfig.setCurrentTerm(leaderTerm, 0);
          RaftServerImpl.setMode(new FollowerMode());
 
      }

      // No appending above. Will message with append failure, prompting leader response when we are a Follower.
      return mConfig.getCurrentTerm();
      
    }
    
  }

  // @param id of the timer that timed out
  public void handleTimeout (int timerID) {
	  
    synchronized (mLock) {
    	
    	switch (timerID) {
    	
    	// Election Timer
    	case 1: 
    		becomeCandidate();
    		break;
    			
    	// WinCheck Timer		
    	case 2: 
    		checkForWin();
    		break;
    			
    	default: // doesn't happen	
    			break;
    	
    	}

    }
    
  }
  
  /**
   * Checks for win from within synchronized block above. Don't call without first surrounding with synchronization.
   */
  private void checkForWin() {

	  double majorityCheck = 0.0;
	  int votesForUs = 0;

	  int[] responses = RaftResponses.getVotes(mConfig.getCurrentTerm());

	  if (responses != null) {

		  for (int i = 1; i < responses.length; i++){

			  if (responses[i] == 0){
				  votesForUs++;
			  }

		  }

		  majorityCheck = ((double)votesForUs) / responses.length;

	  }

	  if (majorityCheck > 0.5) {
		  becomeLeader();
	  }

  }
  
  /**
   * Private helper for becoming Leader. Only call from inside synchronized as done above.
   */
  private void becomeLeader() {
	  
	  cancelTimers();
	  RaftServerImpl.setMode(new LeaderMode());
	  
  }

  /**
   * Private helper for becoming Candidate. Only call from inside synchronized as done above.
   */
  private void becomeCandidate() {

	  cancelTimers();
	  RaftServerImpl.setMode(new CandidateMode());

  }
  
  private void cancelTimers() {
	  
	  electionTimer.cancel();
	  winCheckTimer.cancel();
	  
  }
  
}
