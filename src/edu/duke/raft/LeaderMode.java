package edu.duke.raft;

import java.util.Timer;

public class LeaderMode extends RaftMode {

	private static final int HEARTBEAT_TIMER_INDEX = 3;
	private static final int RESPONSE_TIMER_INDEX = 4;
	
	private Timer heartbeatTimer;//When timer goes off, must send off heartbeat to other servers
	private Timer leaderResponseWaitingTimer;	// Wait for Node responses here

	public void go () {

		synchronized (mLock) {

			heartbeatTimer = scheduleTimer(HEARTBEAT_INTERVAL, HEARTBEAT_TIMER_INDEX);
			leaderResponseWaitingTimer = scheduleTimer(1, RESPONSE_TIMER_INDEX);
			
			int term = mConfig.getCurrentTerm();
			
			//Send out heartbeats to everyone
			tellFollowersToWriteAndSendHeartbeat();
			
			System.out.println ("S" + 
					mID + 
					"." + 
					term + 
					": switched to leader mode.");

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
			return mConfig.getCurrentTerm();
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
			int term = mConfig.getCurrentTerm();
			if (leaderTerm > term) {
				heartbeatTimer.cancel();
				mConfig.setCurrentTerm(leaderTerm, 0);
				// Could implement commit unrolling here. Reserved for appendEntries in Follower, which will be executed on next Hearbeat of
				// newer Leader to whom we are granting control.
				RaftServerImpl.setMode(new FollowerMode());
			}
			// Speaking to the client, clarify please. We expect the client to know who the leader is, as per demos:
				// http://thesecretlivesofdata.com/raft/
			if (leaderID == mID) {
				// Append to own entries
				int appendResult = mLog.append(entries);
				tellFollowersToWriteAndSendHeartbeat();
				if (appendResult == 0) {
					return mConfig.getCurrentTerm();
				}
				else {
					return 0;
				}
			}
			return mConfig.getCurrentTerm();
			
		}
	}

	// @param id of the timer that timed out
	public void handleTimeout (int timerID) {
		
		synchronized (mLock) {
			
			switch (timerID) {
			
			// Heartbeat
			case 1: 
				tellFollowersToWriteAndSendHeartbeat();
				break;

				// Check for responses
			case 2:
				double majorityCheck = 0.0;
				int successfulResponses = 0;

				int[] responses = RaftResponses.getAppendResponses(mConfig.getCurrentTerm());

				if (responses != null) {

					for (int i = 1; i < responses.length; i++){

						if (responses[i] == 0){
							successfulResponses++;
						}

					}

					majorityCheck = ((double)successfulResponses) / responses.length;

				}

				// Most nodes confirmed! Commit successfully.
				if (majorityCheck > 0.5) {
					
					// TODO: Clarify how to communicate this back to the client. Impression is that we set our data mCommitIndex, which client can read.
					mCommitIndex = mLog.getLastIndex();
					
					// Communicate to nodes that we're good to go!
					tellFollowersToWriteAndSendHeartbeat();
					
				}

				break;

			}
			
		}
		
	}

	private void tellFollowersToWriteAndSendHeartbeat(){
		
//		  protected final void remoteAppendEntries (final int serverID,
//				    final int leaderTerm,
//				    final int leaderID,
//				    final int prevLogIndex,
//				    final int prevLogTerm,
//				    final Entry[] entries,
//				    final int leaderCommit)
		
		for (int i = 1; i<= mConfig.getNumServers(); i++) {
			
			remoteAppendEntries(i, mConfig.getCurrentTerm(), mID, mLog.getLastIndex(), mLog.getLastTerm(), new Entry[0], mCommitIndex);
			
		}
		
		resetTimer();
		
	}
	
	private void resetTimer() {

		heartbeatTimer.cancel();
		heartbeatTimer = scheduleTimer(HEARTBEAT_INTERVAL, HEARTBEAT_TIMER_INDEX);

	}
	
}
